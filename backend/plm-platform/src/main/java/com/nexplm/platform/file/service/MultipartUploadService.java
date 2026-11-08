package com.nexplm.platform.file.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.domain.FileUploadSession;
import com.nexplm.platform.file.mapper.FileUploadChunkMapper;
import com.nexplm.platform.file.mapper.FileUploadSessionMapper;
import com.nexplm.platform.file.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 分片直传（断点续传）：大文件按分片独立上传，全部到齐后合并为内容寻址文件。
 * <p>分片本体不落库，只落存储层（{@code _multipart/{sessionId}/{index}}），
 * 库内记录已收分片索引，客户端可随时查询缺哪些分片继续传。</p>
 */
@Service
public class MultipartUploadService {

    private static final Logger log = LoggerFactory.getLogger(MultipartUploadService.class);

    public static final int MIN_CHUNK_SIZE = 1024 * 1024;
    public static final int MAX_CHUNK_SIZE = 64 * 1024 * 1024;
    public static final int DEFAULT_CHUNK_SIZE = 8 * 1024 * 1024;
    public static final long MAX_TOTAL_SIZE = 4L * 1024 * 1024 * 1024;
    private static final int MAX_CHUNK_COUNT = 10000;
    private static final String CHUNK_PREFIX = "_multipart/";

    public record CreateResult(String uploadId, String fileName, long totalSize, int chunkSize, int chunkTotal) {
    }

    public record SessionView(String uploadId, String fileName, long totalSize, int chunkSize, int chunkTotal,
                              String status, List<Integer> received) {
    }

    private final FileService fileService;
    private final FileStorage storage;
    private final FileUploadSessionMapper sessionMapper;
    private final FileUploadChunkMapper chunkMapper;

    public MultipartUploadService(FileService fileService, FileStorage storage,
                                  FileUploadSessionMapper sessionMapper, FileUploadChunkMapper chunkMapper) {
        this.fileService = fileService;
        this.storage = storage;
        this.sessionMapper = sessionMapper;
        this.chunkMapper = chunkMapper;
    }

    /**
     * 创建上传会话。归属对象的存在性延迟到合并时校验（对象可能在会话期间被改/删）。
     */
    public CreateResult create(String fileName, long totalSize, Integer chunkSize, String mime,
                               Long ownerObjectId, String fileRole, Integer sort, Long userId) {
        if (totalSize <= 0) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        if (totalSize > MAX_TOTAL_SIZE) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "单个文件不能超过 4GB");
        }
        int size = (chunkSize == null || chunkSize <= 0) ? DEFAULT_CHUNK_SIZE : chunkSize;
        if (size < MIN_CHUNK_SIZE || size > MAX_CHUNK_SIZE) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "分片大小需在 " + MIN_CHUNK_SIZE + " ~ " + MAX_CHUNK_SIZE + " 字节之间");
        }
        int chunkTotal = (int) ((totalSize + size - 1) / size);
        if (chunkTotal > MAX_CHUNK_COUNT) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "分片数量过多，请增大分片大小");
        }

        // 会话创建时就校验扩展名：避免客户端传完 4GB 才在合并阶段被拒（等保整改 ①）
        fileService.ensureUploadAllowed(fileName);

        FileUploadSession session = new FileUploadSession();
        session.setId(UUID.randomUUID().toString().replace("-", ""));
        session.setFileName(FileService.safeFileName(fileName));
        session.setTotalSize(totalSize);
        session.setChunkSize(size);
        session.setChunkTotal(chunkTotal);
        session.setMime(mime);
        session.setOwnerObjectId(ownerObjectId);
        session.setFileRole(FileService.normalizeRole(fileRole));
        session.setSort(sort == null ? 0 : sort);
        session.setStatus(FileUploadSession.STATUS_UPLOADING);
        session.setCreatedBy(userId);
        session.setCreatedAt(OffsetDateTime.now());
        session.setUpdatedAt(OffsetDateTime.now());
        sessionMapper.insert(session);
        return new CreateResult(session.getId(), session.getFileName(), totalSize, size, chunkTotal);
    }

    /** 上传单个分片。重复上传同一序号为幂等覆盖，便于失败重试。 */
    public void saveChunk(String uploadId, int index, InputStream inputStream, long size) {
        FileUploadSession session = require(uploadId);
        if (!FileUploadSession.STATUS_UPLOADING.equals(session.getStatus())) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_SESSION_NOT_FOUND, "上传会话已结束：" + uploadId);
        }
        if (size <= 0) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_CHUNK_INVALID, "分片内容为空");
        }
        if (index < 0 || index >= session.getChunkTotal()) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_CHUNK_INVALID, "分片序号越界：" + index);
        }
        if (size > session.getChunkSize()) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_CHUNK_INVALID, "分片大小超出约定：" + size);
        }
        storage.put(chunkKey(uploadId, index), inputStream, size);
        chunkMapper.upsert(uploadId, index, (int) size);
        sessionMapper.touch(uploadId);
    }

    /** 合并全部分片并提交为正式文件。 */
    public FileService.UploadResult complete(String uploadId, Long userId) {
        FileUploadSession session = require(uploadId);
        if (!FileUploadSession.STATUS_UPLOADING.equals(session.getStatus())) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_SESSION_NOT_FOUND, "上传会话已结束：" + uploadId);
        }
        int received = chunkMapper.countChunks(uploadId);
        if (received != session.getChunkTotal()) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_CHUNK_INVALID,
                    "分片未上传完整：" + received + "/" + session.getChunkTotal());
        }

        Path temp = null;
        try {
            temp = Files.createTempFile("plm-merge-", ".tmp");
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp))) {
                for (int index = 0; index < session.getChunkTotal(); index++) {
                    try (InputStream in = storage.get(chunkKey(uploadId, index))) {
                        in.transferTo(out);
                    }
                }
            }
            long size = Files.size(temp);
            if (size != session.getTotalSize()) {
                throw BizException.of(ErrorCode.FILE_UPLOAD_CHUNK_INVALID,
                        "合并后大小与声明不符：实际 " + size + "，声明 " + session.getTotalSize());
            }
            FileService.UploadResult result = fileService.commitFromPath(temp, size, session.getMime(),
                    session.getFileName(), session.getOwnerObjectId(), session.getFileRole(),
                    session.getSort(), userId);
            sessionMapper.updateStatus(uploadId, FileUploadSession.STATUS_MERGED);
            cleanupChunks(uploadId, session.getChunkTotal());
            chunkMapper.deleteAll(uploadId);
            return result;
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "分片合并失败：" + ex.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    public void abort(String uploadId) {
        FileUploadSession session = require(uploadId);
        if (FileUploadSession.STATUS_MERGED.equals(session.getStatus())) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_SESSION_NOT_FOUND, "会话已完成，无法取消：" + uploadId);
        }
        sessionMapper.updateStatus(uploadId, FileUploadSession.STATUS_ABORTED);
        cleanupChunks(uploadId, session.getChunkTotal());
        chunkMapper.deleteAll(uploadId);
    }

    /** 会话状态与已收分片索引（客户端据此续传缺失分片）。 */
    public SessionView view(String uploadId) {
        FileUploadSession session = require(uploadId);
        return new SessionView(session.getId(), session.getFileName(), session.getTotalSize(),
                session.getChunkSize(), session.getChunkTotal(), session.getStatus(),
                chunkMapper.listIndexes(uploadId));
    }

    private FileUploadSession require(String uploadId) {
        if (uploadId == null || uploadId.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "上传会话标识不能为空");
        }
        FileUploadSession session = sessionMapper.selectById(uploadId);
        if (session == null) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_SESSION_NOT_FOUND, "上传会话不存在：" + uploadId);
        }
        return session;
    }

    private String chunkKey(String uploadId, int index) {
        return CHUNK_PREFIX + uploadId + "/" + index;
    }

    private void cleanupChunks(String uploadId, int chunkTotal) {
        for (int index = 0; index < chunkTotal; index++) {
            try {
                storage.delete(chunkKey(uploadId, index));
            } catch (RuntimeException ex) {
                log.warn("分片清理失败：{}#{} {}", uploadId, index, ex.getMessage());
            }
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("合并暂存文件清理失败：{}", path);
        }
    }
}
