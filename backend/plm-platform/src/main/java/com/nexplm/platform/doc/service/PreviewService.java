package com.nexplm.platform.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocPreviewDtos;
import com.nexplm.platform.doc.domain.DocPreview;
import com.nexplm.platform.doc.mapper.DocMasterMapper;
import com.nexplm.platform.doc.mapper.DocPreviewMapper;
import com.nexplm.platform.doc.preview.PreviewConverter;
import com.nexplm.platform.doc.preview.PreviewOutput;
import com.nexplm.platform.doc.preview.PreviewUnavailableException;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.object.service.ObjectAccessGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * 文档预览：选择转换器 → 转换 → 产物入文件仓（file_role=VIEW）→ 登记状态。
 * <p>状态语义：READY 就绪、FAILED 失败（可重试）、SKIPPED 环境不支持该格式（不视为错误）。</p>
 */
@Service
public class PreviewService {

    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private static final int MESSAGE_MAX = 500;

    private final DocPreviewMapper previewMapper;
    private final DocMasterMapper docMasterMapper;
    private final ObjectAccessGuard objectAccessGuard;
    private final FileService fileService;
    private final List<PreviewConverter> converters;

    public PreviewService(DocPreviewMapper previewMapper, DocMasterMapper docMasterMapper,
                          ObjectAccessGuard objectAccessGuard,
                          FileService fileService, List<PreviewConverter> converters) {
        this.previewMapper = previewMapper;
        this.docMasterMapper = docMasterMapper;
        this.objectAccessGuard = objectAccessGuard;
        this.fileService = fileService;
        this.converters = converters;
    }

    /** 预览状态（取该文档最近一次记录；无记录返回 NONE）。 */
    public DocPreviewDtos.PreviewStatus status(Long objectId) {
        return status(objectId, null);
    }

    /**
     * 预览状态。
     * <p>一个文档可按源文件存多条记录（唯一键 object_id + source_file_id），
     * 因此指定 {@code fileId} 时取该源文件的记录；不指定才回退到"最近更新的一条"。</p>
     */
    public DocPreviewDtos.PreviewStatus status(Long objectId, Long fileId) {
        objectAccessGuard.requireType(objectId, DocService.OBJECT_TYPE, "文档");
        DocPreview record = latest(objectId, fileId);
        if (record == null) {
            return new DocPreviewDtos.PreviewStatus(null, objectId, null, null, null, "NONE",
                    null, null, null, null, null);
        }
        return toStatus(record);
    }

    /**
     * 生成/重新生成预览。fileId 为空时取主文件（无主文件取排序第一的文件）。
     *
     * <p>刻意<b>不加</b> {@code @Transactional}：转换要等在外部 soffice 进程上（最长 90 秒），
     * 包在事务里会长期占用数据库连接。这里每一步都是独立原子的（findOrCreate=单条 insert、
     * mark=单条 update、upload/detach=FileService 自己的事务），去掉外层事务反而得到
     * 更短的锁持有时间与可重试语义（EXP-20261108-04 第 5 条）。</p>
     */
    public DocPreviewDtos.PreviewStatus generate(Long objectId, Long fileId, Long userId) {
        objectAccessGuard.requireType(objectId, DocService.OBJECT_TYPE, "文档");
        FileService.FileView source = resolveSource(objectId, fileId);
        String ext = source.ext() == null ? "" : source.ext().toLowerCase(Locale.ROOT);
        DocPreview record = findOrCreate(objectId, source.fileId(), userId);
        mark(record, "PENDING", null, null, userId);

        PreviewConverter converter = converters.stream()
                .filter(item -> item.supports(ext))
                .findFirst()
                .orElse(null);
        if (converter == null) {
            record.setConverter(null);
            return toStatus(mark(record, "SKIPPED",
                    "暂不支持 ." + (ext.isEmpty() ? "未知" : ext) + " 格式的在线预览，可上传 PDF 或图片附件", null, userId));
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("plm-preview-");
            Path tempSource = workDir.resolve("source." + (ext.isEmpty() ? "bin" : ext));
            try (FileService.Opened opened = fileService.opened(source.fileId())) {
                Files.copy(opened.stream(), tempSource, StandardCopyOption.REPLACE_EXISTING);
            }
            List<PreviewOutput> outputs = converter.convert(tempSource, ext, workDir);
            if (outputs.isEmpty()) {
                throw new IOException("转换器未返回任何产物");
            }
            PreviewOutput output = outputs.get(0);
            Long previousFileId = record.getPreviewFileId();
            // 先上传新产物、成功后再解绑旧产物。顺序反了会在上传失败时把「原本能看的预览」清掉，
            // 而异常会被下面的 catch 吞掉、外层事务不回滚，用户直接退化为看不了（EXP-20261108-04 第 3 条）。
            FileService.UploadResult uploaded = fileService.upload(objectId, FileService.ROLE_VIEW, 100,
                    previewName(source.fileName(), output.format()),
                    mimeOf(output.format()),
                    new ByteArrayInputStream(output.content()), userId);
            detachPrevious(objectId, previousFileId);
            record.setFormat(output.format());
            record.setConverter(converter.name());
            // 预览产物是 VIEW 角色的文件链接，落库后同步子表统计，
            // 否则详情/列表的文件数会与真实链接数不一致（上传时统计含 VIEW）
            refreshFileStats(objectId);
            return toStatus(mark(record, "READY", null, uploaded.fileId(), userId,
                    output.pageCount()));
        } catch (PreviewUnavailableException ex) {
            return toStatus(mark(record, "SKIPPED", ex.getMessage(), null, userId));
        } catch (Exception ex) {
            log.warn("文档预览转换失败 objectId={} fileId={} converter={}", objectId, source.fileId(),
                    converter.name(), ex);
            // 保留原产物指针：失败时旧预览（未被解绑）仍可继续查看，不能把记录清成"无产物"
            record.setConverter(converter.name());
            return toStatus(mark(record, "FAILED", ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                    record.getPreviewFileId(), userId));
        } finally {
            deleteQuietly(workDir);
        }
    }

    // ------------------------------------------------------------ 内部

    private FileService.FileView resolveSource(Long objectId, Long fileId) {
        List<FileService.FileView> files = fileService.listByOwner(objectId, true).stream()
                .filter(file -> !FileService.ROLE_VIEW.equals(file.fileRole()))
                .toList();
        if (files.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该文档没有可预览的文件，请先上传");
        }
        if (fileId == null) {
            return files.stream()
                    .filter(file -> FileService.ROLE_MAIN.equals(file.fileRole()))
                    .findFirst()
                    .orElse(files.get(0));
        }
        return files.stream()
                .filter(file -> fileId.equals(file.fileId()))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_NOT_FOUND,
                        "文件不属于该文档或不存在：" + fileId));
    }

    private DocPreview latest(Long objectId, Long fileId) {
        List<DocPreview> rows = previewMapper.selectList(new LambdaQueryWrapper<DocPreview>()
                .eq(DocPreview::getObjectId, objectId)
                .eq(fileId != null, DocPreview::getSourceFileId, fileId)
                .orderByDesc(DocPreview::getUpdatedAt)
                .last("limit 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private DocPreview findOrCreate(Long objectId, Long sourceFileId, Long userId) {
        DocPreview existing = previewMapper.selectList(new LambdaQueryWrapper<DocPreview>()
                .eq(DocPreview::getObjectId, objectId)
                .eq(DocPreview::getSourceFileId, sourceFileId)
                .last("limit 1")).stream().findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }
        OffsetDateTime now = OffsetDateTime.now();
        DocPreview record = new DocPreview();
        record.setId(IdWorker.getId());
        record.setObjectId(objectId);
        record.setSourceFileId(sourceFileId);
        record.setFormat("PDF");
        record.setStatus("PENDING");
        record.setCreatedBy(userId);
        record.setCreatedAt(now);
        record.setUpdatedBy(userId);
        record.setUpdatedAt(now);
        previewMapper.insert(record);
        return record;
    }

    private DocPreview mark(DocPreview record, String status, String message, Long previewFileId,
                            Long userId) {
        return mark(record, status, message, previewFileId, userId, null);
    }

    private DocPreview mark(DocPreview record, String status, String message, Long previewFileId,
                            Long userId, Integer pageCount) {
        record.setStatus(status);
        record.setMessage(truncate(message));
        record.setPreviewFileId(previewFileId);
        if (pageCount != null) {
            record.setPageCount(pageCount);
        }
        record.setUpdatedBy(userId);
        record.setUpdatedAt(OffsetDateTime.now());
        previewMapper.updateById(record);
        return record;
    }

    private void detachPrevious(Long objectId, Long previewFileId) {
        if (previewFileId == null) {
            return;
        }
        try {
            fileService.listByOwner(objectId, true).stream()
                    .filter(file -> FileService.ROLE_VIEW.equals(file.fileRole())
                            && previewFileId.equals(file.fileId()))
                    .forEach(file -> fileService.detach(file.linkId()));
        } catch (Exception ex) {
            log.debug("清理旧预览链接失败 objectId={} previewFileId={}", objectId, previewFileId, ex);
        }
    }

    private DocPreviewDtos.PreviewStatus toStatus(DocPreview record) {
        String fileName = null;
        if (record.getPreviewFileId() != null) {
            try {
                fileName = fileService.listByOwner(record.getObjectId(), true).stream()
                        .filter(file -> record.getPreviewFileId().equals(file.fileId()))
                        .map(FileService.FileView::fileName)
                        .findFirst()
                        .orElse(null);
            } catch (Exception ignored) {
                fileName = null;
            }
        }
        return new DocPreviewDtos.PreviewStatus(record.getId(), record.getObjectId(), record.getSourceFileId(),
                record.getPreviewFileId(), record.getFormat(), record.getStatus(), record.getMessage(),
                record.getPageCount(), fileName, record.getConverter(), record.getUpdatedAt());
    }

    private String previewName(String sourceName, String format) {
        String base = sourceName == null ? "document" : sourceName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        String ext = "PDF".equalsIgnoreCase(format) ? "pdf" : format.toLowerCase(Locale.ROOT);
        return FileService.safeFileName("preview-" + base + "." + ext);
    }

    private String mimeOf(String format) {
        if (format == null) {
            return "application/octet-stream";
        }
        return switch (format.toUpperCase(Locale.ROOT)) {
            case "PDF" -> "application/pdf";
            case "PNG" -> "image/png";
            case "JPG", "JPEG" -> "image/jpeg";
            case "GIF" -> "image/gif";
            case "BMP" -> "image/bmp";
            case "WEBP" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 与 DocService.refreshFileStats 同口径：文件数（含 VIEW）+ 主文件（无 MAIN 时取排序第一）。
     * <p>主文件候选必须排除 VIEW 派生物，否则"只有预览产物"的文档会把 doc_master 的主文件
     * 指成 preview-xxx.pdf（EXP-20261108-04 第 1 条）。</p>
     */
    private void refreshFileStats(Long docId) {
        List<FileService.FileView> files = fileService.listByOwner(docId, true);
        FileService.FileView main = files.stream()
                .filter(file -> FileService.ROLE_MAIN.equals(file.fileRole()))
                .findFirst()
                .orElseGet(() -> files.stream()
                        .filter(file -> !FileService.ROLE_VIEW.equals(file.fileRole()))
                        .findFirst()
                        .orElse(null));
        docMasterMapper.updateFileStats(docId, files.size(),
                main == null ? null : main.fileId(), main == null ? null : main.ext());
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MESSAGE_MAX ? message.substring(0, MESSAGE_MAX) : message;
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 临时目录清理失败不影响主流程
                }
            });
        } catch (IOException ignored) {
            // 同上
        }
    }
}
