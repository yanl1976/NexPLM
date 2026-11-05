package com.nexplm.platform.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.domain.FileLink;
import com.nexplm.platform.file.domain.FileObject;
import com.nexplm.platform.file.mapper.FileLinkMapper;
import com.nexplm.platform.file.mapper.FileObjectMapper;
import com.nexplm.platform.file.scan.FileScanHook;
import com.nexplm.platform.file.storage.FileStorage;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文件仓服务：内容寻址存储 + 「对象 ↔ 文件」多对多关联。
 * <p>核心约定：</p>
 * <ul>
 *   <li>物理文件按 sha256 唯一，同内容只落一份，重复上传即「秒传」；</li>
 *   <li>{@code file_object.ref_count} 严格等于引用它的 {@code file_link} 条数，
 *       归零时物理文件才被回收；</li>
 *   <li>同一对象同一角色（{@code file_role}）上传新版本时，旧版本 {@code is_latest} 置否，
 *       保证任一时刻每组只有一条最新记录。</li>
 * </ul>
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    /** 文件角色：主文件 / 附件 / 预览 / 派生格式（与数据模型设计第 6 节一致）。 */
    public static final String ROLE_MAIN = "MAIN";
    public static final String ROLE_ATTACHMENT = "ATTACHMENT";
    public static final String ROLE_VIEW = "VIEW";
    public static final String ROLE_DERIVED = "DERIVED";

    private static final Set<String> ALLOWED_ROLES =
            Set.of(ROLE_MAIN, ROLE_ATTACHMENT, ROLE_VIEW, ROLE_DERIVED);

    private static final String DEFAULT_MIME = "application/octet-stream";
    private static final String SCAN_PENDING = "PENDING";
    private static final String SCAN_ERROR = "ERROR";
    private static final int NAME_MAX = 255;
    private static final int MIME_MAX = 128;
    private static final int EXT_MAX = 32;
    private static final int COPY_BUFFER = 64 * 1024;
    private static final int SHA_LENGTH = 64;

    /** 上传结果。{@code dedup=true} 表示命中已有物理文件（秒传）。 */
    public record UploadResult(Long fileId, Long linkId, String sha256, long size, String mime, String ext,
                               String fileName, String fileRole, Integer versionNo, boolean dedup) {
    }

    /** 对象维度下的文件视图（关联信息 + 物理文件元数据）。 */
    public record FileView(Long linkId, Long fileId, String fileName, String fileRole, Integer versionNo,
                           Boolean isLatest, Integer sort, Long size, String mime, String ext,
                           String sha256, OffsetDateTime createdAt) {
    }

    /** 秒传预检结果。 */
    public record PrecheckResult(boolean exists, Long fileId, String mime, Long size) {
    }

    /** 下载地址。{@code direct=true} 为对象存储预签名直链，否则为服务端流式下载路径。 */
    public record DownloadUrl(String url, boolean direct, long expiresIn) {
    }

    /** 打开的文件流，调用方负责关闭。 */
    public record Opened(FileObject file, InputStream stream) implements AutoCloseable {

        @Override
        public void close() {
            try {
                stream.close();
            } catch (IOException ignored) {
                // 关闭失败无需向上传播
            }
        }
    }

    private record Stored(FileObject file, boolean dedup) {
    }

    private final FileObjectMapper fileObjectMapper;
    private final FileLinkMapper fileLinkMapper;
    private final PlmObjectMapper objectMapper;
    private final FileStorage storage;
    private final FileScanHook scanHook;

    public FileService(FileObjectMapper fileObjectMapper, FileLinkMapper fileLinkMapper,
                       PlmObjectMapper objectMapper, FileStorage storage, FileScanHook scanHook) {
        this.fileObjectMapper = fileObjectMapper;
        this.fileLinkMapper = fileLinkMapper;
        this.objectMapper = objectMapper;
        this.storage = storage;
        this.scanHook = scanHook;
    }

    public String storageType() {
        return storage.type();
    }

    /**
     * 上传：落盘（或命中秒传）→ 建物理文件记录 → 若指定了归属对象则绑定关联。
     */
    public UploadResult upload(Long ownerObjectId, String fileRole, Integer sort, String fileName,
                               String mime, InputStream inputStream, Long userId) {
        Path temp = spool(inputStream);
        try {
            return commitFromPath(temp, Files.size(temp), mime, fileName, ownerObjectId, fileRole, sort, userId);
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "读取暂存文件失败：" + ex.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    /**
     * 以已落盘的暂存文件提交为正式文件：算 sha256 → 去重入库 →（可选）绑定对象。
     * <p>分片合并完成后也走这里，保证单文件上传与分片上传的落库口径完全一致。</p>
     */
    public UploadResult commitFromPath(Path temp, long size, String mime, String fileName,
                                       Long ownerObjectId, String fileRole, Integer sort, Long userId) {
        if (size <= 0) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        String role = normalizeRole(fileRole);
        String safeName = safeFileName(fileName);
        if (ownerObjectId != null) {
            requireObject(ownerObjectId);
        }
        String sha = sha256(temp);
        Stored stored = persist(sha, size, normalizeMime(mime), safeName, temp);
        FileObject file = stored.file();
        scan(file, temp);
        Long linkId = null;
        Integer versionNo = null;
        if (ownerObjectId != null) {
            FileLink link = attach(ownerObjectId, file.getId(), safeName, role, sort, userId);
            linkId = link.getId();
            versionNo = link.getVersionNo();
        }
        return new UploadResult(file.getId(), linkId, sha, size, file.getMime(), file.getExt(),
                safeName, role, versionNo, stored.dedup());
    }

    /**
     * 把已有物理文件绑定到对象（秒传后的「贴链接」动作）。
     * 同一对象同一角色会自动递增版本号，并把旧版本标记为非最新。
     */
    @Transactional
    public FileLink attach(Long ownerObjectId, Long fileId, String fileName, String fileRole,
                           Integer sort, Long userId) {
        requireObject(ownerObjectId);
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null) {
            throw BizException.of(ErrorCode.FILE_OBJECT_NOT_FOUND);
        }
        String role = normalizeRole(fileRole);
        String name = safeFileName(fileName);
        int versionNo = fileLinkMapper.nextVersionNo(ownerObjectId, role);
        fileLinkMapper.markNotLatest(ownerObjectId, role);

        FileLink link = new FileLink();
        link.setId(IdWorker.getId());
        link.setFileId(fileId);
        link.setOwnerObjectId(ownerObjectId);
        link.setVersionNo(versionNo);
        link.setFileRole(role);
        link.setFileName(name);
        link.setIsLatest(true);
        link.setSort(sort == null ? 0 : sort);
        link.setCreatedBy(userId);
        link.setCreatedAt(OffsetDateTime.now());
        fileLinkMapper.insert(link);
        fileObjectMapper.addRefCount(fileId, 1);
        return link;
    }

    /**
     * 版本迭代时把某个对象的最新文件链接复制到新版本对象。
     * <p>物理文件不复制，只递增引用计数，新旧版本共享同一份内容。</p>
     */
    @Transactional
    public int copyLatestLinks(Long fromOwnerObjectId, Long toOwnerObjectId, Long userId) {
        List<FileLink> links = fileLinkMapper.selectList(new LambdaQueryWrapper<FileLink>()
                .eq(FileLink::getOwnerObjectId, fromOwnerObjectId)
                .eq(FileLink::getIsLatest, Boolean.TRUE)
                .orderByAsc(FileLink::getFileRole)
                .orderByAsc(FileLink::getSort));
        OffsetDateTime now = OffsetDateTime.now();
        for (FileLink link : links) {
            FileLink copy = new FileLink();
            copy.setId(IdWorker.getId());
            copy.setFileId(link.getFileId());
            copy.setOwnerObjectId(toOwnerObjectId);
            copy.setVersionNo(link.getVersionNo());
            copy.setFileRole(link.getFileRole());
            copy.setFileName(link.getFileName());
            copy.setIsLatest(true);
            copy.setSort(link.getSort());
            copy.setCreatedBy(userId);
            copy.setCreatedAt(now);
            fileLinkMapper.insert(copy);
            fileObjectMapper.addRefCount(link.getFileId(), 1);
        }
        return links.size();
    }

    /** 解除关联；引用计数归零时回收物理文件。 */
    @Transactional
    public void detach(Long linkId) {
        FileLink link = fileLinkMapper.selectById(linkId);
        if (link == null) {
            throw BizException.of(ErrorCode.FILE_LINK_NOT_FOUND);
        }
        fileLinkMapper.deleteById(linkId);
        fileObjectMapper.addRefCount(link.getFileId(), -1);
        if (Boolean.TRUE.equals(link.getIsLatest())) {
            fileLinkMapper.promoteLatest(link.getOwnerObjectId(), link.getFileRole());
        }
        purgeIfUnused(link.getFileId());
    }

    public List<FileView> listByOwner(Long ownerObjectId, boolean latestOnly) {
        if (ownerObjectId == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "ownerObjectId 不能为空");
        }
        List<FileLink> links = fileLinkMapper.selectList(new LambdaQueryWrapper<FileLink>()
                .eq(FileLink::getOwnerObjectId, ownerObjectId)
                .eq(latestOnly, FileLink::getIsLatest, Boolean.TRUE)
                .orderByAsc(FileLink::getFileRole)
                .orderByAsc(FileLink::getSort)
                .orderByDesc(FileLink::getVersionNo));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> fileIds = links.stream().map(FileLink::getFileId).distinct().toList();
        Map<Long, FileObject> files = new HashMap<>();
        for (FileObject file : fileObjectMapper.selectList(new LambdaQueryWrapper<FileObject>()
                .in(FileObject::getId, fileIds))) {
            files.put(file.getId(), file);
        }
        return links.stream().map(link -> toView(link, files.get(link.getFileId()))).toList();
    }

    public FileObject meta(Long fileId) {
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null) {
            throw BizException.of(ErrorCode.FILE_OBJECT_NOT_FOUND);
        }
        return file;
    }

    /** 打开文件流（下载/预览用），带元数据。 */
    public Opened opened(Long fileId) {
        FileObject file = meta(fileId);
        ensureDownloadable(file);
        if (!storage.exists(file.getObjectKey())) {
            throw BizException.of(ErrorCode.FILE_OBJECT_NOT_FOUND, "物理文件缺失：" + file.getObjectKey());
        }
        return new Opened(file, storage.get(file.getObjectKey()));
    }

    /** 下载地址：对象存储给预签名直链，本地存储回退为服务端流式下载路径。 */
    public DownloadUrl downloadUrl(Long fileId, Duration ttl) {
        FileObject file = meta(fileId);
        ensureDownloadable(file);
        long seconds = ttl == null ? 3600L : ttl.toSeconds();
        String url = storage.presignedGetUrl(file.getObjectKey(), Duration.ofSeconds(seconds));
        if (url == null) {
            return new DownloadUrl("/api/v1/files/" + fileId + "/content", false, seconds);
        }
        return new DownloadUrl(url, true, seconds);
    }

    /** 秒传预检：按 sha256 判断物理文件是否已存在。 */
    public PrecheckResult precheck(String sha256) {
        String normalized = sha256 == null ? "" : sha256.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != SHA_LENGTH || !normalized.matches("[0-9a-f]{64}")) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "sha256 必须是 64 位十六进制字符串");
        }
        FileObject file = findBySha(normalized);
        if (file == null) {
            return new PrecheckResult(false, null, null, null);
        }
        return new PrecheckResult(true, file.getId(), file.getMime(), file.getSize());
    }

    // ---------------------------------------------------------------- 内部实现

    private Stored persist(String sha, long size, String mime, String fileName, Path temp) {
        FileObject existing = findBySha(sha);
        if (existing != null) {
            return new Stored(existing, true);
        }
        String objectKey = objectKey(sha);
        if (!storage.exists(objectKey)) {
            try (InputStream in = new BufferedInputStream(Files.newInputStream(temp))) {
                storage.put(objectKey, in, size);
            } catch (IOException ex) {
                throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "写入存储失败：" + ex.getMessage());
            }
        }

        FileObject created = new FileObject();
        created.setId(IdWorker.getId());
        created.setSha256(sha);
        created.setSize(size);
        created.setMime(mime);
        created.setExt(extOf(fileName));
        created.setBucket(storage.type());
        created.setObjectKey(objectKey);
        created.setRefCount(0);
        created.setScanStatus(SCAN_PENDING);
        created.setCreatedAt(OffsetDateTime.now());
        try {
            fileObjectMapper.insert(created);
            return new Stored(created, false);
        } catch (DuplicateKeyException ex) {
            // 并发上传同一内容：以先落库者为准，本次按秒传处理
            FileObject concurrent = findBySha(sha);
            if (concurrent == null) {
                throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "文件元数据写入冲突，请重试");
            }
            return new Stored(concurrent, true);
        }
    }

    private void ensureDownloadable(FileObject file) {
        if (FileScanHook.INFECTED.equals(file.getScanStatus())) {
            throw BizException.of(ErrorCode.FILE_SCAN_INFECTED);
        }
    }

    /**
     * 对新物理文件执行安全扫描并回写状态。同内容秒传不重复扫描；
     * 扫描实现异常时记为 ERROR，允许后续重扫，不阻断上传。
     */
    private void scan(FileObject file, Path temp) {
        String current = file.getScanStatus();
        if (current != null && !SCAN_PENDING.equals(current) && !SCAN_ERROR.equals(current)) {
            return;
        }
        String status;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(temp))) {
            status = scanHook.scan(file, in);
        } catch (IOException ex) {
            log.warn("读取待扫描文件失败：{}", ex.getMessage());
            status = SCAN_ERROR;
        } catch (RuntimeException ex) {
            log.warn("文件安全扫描异常，标记为 ERROR：{}", ex.getMessage());
            status = SCAN_ERROR;
        }
        if (status == null || status.isBlank()) {
            status = FileScanHook.SKIPPED;
        }
        file.setScanStatus(status);
        fileObjectMapper.updateScanStatus(file.getId(), status);
    }

    private void purgeIfUnused(Long fileId) {
        FileObject file = fileObjectMapper.selectById(fileId);
        if (file == null || file.getRefCount() == null || file.getRefCount() > 0) {
            return;
        }
        try {
            storage.delete(file.getObjectKey());
        } catch (RuntimeException ex) {
            // 物理文件删除失败不影响业务；元数据保留，交由离线清理任务处理
            log.warn("物理文件删除失败，保留元数据待清理：key={} 原因={}", file.getObjectKey(), ex.getMessage());
            return;
        }
        fileObjectMapper.deleteById(fileId);
    }

    private FileObject findBySha(String sha) {
        return fileObjectMapper.selectOne(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getSha256, sha).last("limit 1"));
    }

    private FileView toView(FileLink link, FileObject file) {
        return new FileView(link.getId(), link.getFileId(), link.getFileName(), link.getFileRole(),
                link.getVersionNo(), link.getIsLatest(), link.getSort(),
                file == null ? null : file.getSize(),
                file == null ? null : file.getMime(),
                file == null ? null : file.getExt(),
                file == null ? null : file.getSha256(),
                link.getCreatedAt());
    }

    private void requireObject(Long objectId) {
        if (objectId == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "归属对象不能为空");
        }
        if (objectMapper.selectById(objectId) == null) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND);
        }
    }

    /** 角色归一化：空值回落为附件，非法值直接拒绝。分片上传会话也复用该规则。 */
    public static String normalizeRole(String fileRole) {
        if (fileRole == null || fileRole.isBlank()) {
            return ROLE_ATTACHMENT;
        }
        String role = fileRole.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "文件角色不合法，可选：" + String.join("/", ROLE_MAIN, ROLE_ATTACHMENT, ROLE_VIEW, ROLE_DERIVED));
        }
        return role;
    }

    private String normalizeMime(String mime) {
        if (mime == null || mime.isBlank()) {
            return DEFAULT_MIME;
        }
        String value = mime.trim();
        return value.length() > MIME_MAX ? DEFAULT_MIME : value;
    }

    /** 文件名净化：剥离路径、控制字符，长度受表结构限制。 */
    public static String safeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw BizException.of(ErrorCode.FILE_NAME_INVALID, "文件名不能为空");
        }
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\r\\n\\t\\u0000]", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            throw BizException.of(ErrorCode.FILE_NAME_INVALID, "文件名不合法");
        }
        if (name.length() > NAME_MAX) {
            throw BizException.of(ErrorCode.FILE_NAME_INVALID, "文件名过长（最多 " + NAME_MAX + " 个字符）");
        }
        return name;
    }

    private String extOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 1 || dot == fileName.length() - 1) {
            return null;
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ext.matches("[a-z0-9]{1,16}")) {
            return null;
        }
        return ext.length() > EXT_MAX ? ext.substring(0, EXT_MAX) : ext;
    }

    /** 存储键布局，与对象存储规则对齐，便于后续迁移 MinIO。 */
    private String objectKey(String sha) {
        return sha.substring(0, 2) + "/" + sha.substring(2, 4) + "/" + sha;
    }

    private Path spool(InputStream inputStream) {
        if (inputStream == null) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        Path temp = null;
        try {
            temp = Files.createTempFile("plm-upload-", ".tmp");
            Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException ex) {
            if (temp != null) {
                deleteQuietly(temp);
            }
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "上传暂存失败：" + ex.getMessage());
        }
    }

    private String sha256(Path file) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "当前 JVM 不支持 SHA-256");
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[COPY_BUFFER];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "计算文件摘要失败：" + ex.getMessage());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("暂存文件清理失败：{}", path);
        }
    }
}
