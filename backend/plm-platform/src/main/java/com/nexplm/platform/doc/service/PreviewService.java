package com.nexplm.platform.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 文档预览：选择转换器 → （异步）转换 → 产物入文件仓（file_role=VIEW）→ 登记状态。
 *
 * <p>状态语义：NONE 未生成、PENDING 转换中、READY 就绪、FAILED 失败（可重试）、
 * SKIPPED 环境不支持该格式（不视为错误）。</p>
 *
 * <p>转换是<b>异步</b>的：外部 soffice 最长 90 秒，放请求线程里会拖慢响应，也让"只读用户
 * 随手点一下"就能占满容器线程。提交前完成全部需要请求上下文的动作（数据范围校验、源文件
 * 解析、转换器选择），再丢进有界线程池，接口立即返回 PENDING 由前端轮询。</p>
 */
@Service
public class PreviewService {

    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private static final int MESSAGE_MAX = 500;
    /** 转换并发数：外部进程（soffice）吃 CPU/内存，2 路足够，同时充当全局限流。 */
    private static final int CONVERT_THREADS = 2;
    /** 排队上限：满了直接返回「可重试的失败」，不无限堆积、也不阻塞请求线程。 */
    private static final int CONVERT_QUEUE = 8;

    private final DocPreviewMapper previewMapper;
    private final DocMasterMapper docMasterMapper;
    private final ObjectAccessGuard objectAccessGuard;
    private final FileService fileService;
    private final List<PreviewConverter> converters;
    /** PENDING 超过该时长视为卡死（进程重启/线程被中断），允许重新触发并被维护任务回收。 */
    private final int staleMinutes;
    /** 用户级配额：全局池防总量，这一层防单点刷。 */
    private final PreviewRateLimiter rateLimiter;

    /** 转换线程池：转换要等外部进程（最长 90s），必须离开请求线程；有界队列即资源护栏。 */
    private final ExecutorService converterPool = new ThreadPoolExecutor(
            CONVERT_THREADS, CONVERT_THREADS, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(CONVERT_QUEUE),
            runnable -> {
                Thread thread = new Thread(runnable, "plm-preview-converter");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    /** 在途任务键（objectId:sourceFileId）：单实例去重，与 DB 的 PENDING CAS 双保险。 */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    public PreviewService(DocPreviewMapper previewMapper, DocMasterMapper docMasterMapper,
                          ObjectAccessGuard objectAccessGuard,
                          FileService fileService, List<PreviewConverter> converters,
                          @Value("${nexplm.preview.stale-minutes:5}") int staleMinutes,
                          @Value("${nexplm.preview.rate-limit-per-minute:10}") int rateLimitPerMinute) {
        this.previewMapper = previewMapper;
        this.docMasterMapper = docMasterMapper;
        this.objectAccessGuard = objectAccessGuard;
        this.fileService = fileService;
        this.converters = converters;
        // 0/负数会让正在跑的转换被立即判死，下限锁 1 分钟
        this.staleMinutes = Math.max(1, staleMinutes);
        this.rateLimiter = new PreviewRateLimiter(rateLimitPerMinute);
    }

    @PreDestroy
    void shutdown() {
        converterPool.shutdownNow();
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
     * 触发预览生成/重试。fileId 为空时取主文件（无主文件取排序第一的文件）。
     *
     * <p>同步部分只做三件事：数据范围校验（必须用请求线程的 CurrentUser）、解析源文件、
     * 选择转换器并抢占 PENDING；实际转换丢线程池，接口立即返回 PENDING。</p>
     *
     * <p>刻意<b>不加</b> {@code @Transactional}：每一步都是独立原子的（findOrCreate=单条 insert、
     * mark=单条 update、upload/detach=FileService 自己的事务），外层事务只会把外部进程等待
     * 包成长期占用连接的长事务（EXP-20261108-04 第 5 条）。</p>
     */
    public DocPreviewDtos.PreviewStatus generate(Long objectId, Long fileId, Long userId) {
        objectAccessGuard.requireType(objectId, DocService.OBJECT_TYPE, "文档");
        if (!rateLimiter.tryAcquire(userId, System.currentTimeMillis())) {
            throw BizException.of(ErrorCode.DOC_PREVIEW_RATE_LIMITED,
                    "预览生成过于频繁（每分钟最多 " + rateLimiter.limitPerMinute() + " 次），请稍后重试");
        }
        FileService.FileView source = resolveSource(objectId, fileId);
        String ext = source.ext() == null ? "" : source.ext().toLowerCase(Locale.ROOT);
        DocPreview record = findOrCreate(objectId, source.fileId(), userId);

        PreviewConverter converter = converters.stream()
                .filter(item -> item.supports(ext))
                .findFirst()
                .orElse(null);
        if (converter == null) {
            // 无转换器是纯内存判断，保持同步返回，前端不必轮询
            record.setConverter(null);
            return toStatus(mark(record, "SKIPPED",
                    "暂不支持 ." + (ext.isEmpty() ? "未知" : ext) + " 格式的在线预览，可上传 PDF 或图片附件", null, userId));
        }

        String key = objectId + ":" + source.fileId();
        if (inFlight.contains(key) || !claimPending(record, converter.name(), userId)) {
            // 已有在途任务（或另一实例刚抢到）→ 返回当前状态，不重复起转换
            return toStatus(reload(record.getId()));
        }

        inFlight.add(key);
        try {
            converterPool.execute(() -> runConversion(objectId, record.getId(), source, converter, userId, key));
        } catch (RejectedExecutionException ex) {
            inFlight.remove(key);
            log.warn("预览转换队列已满 objectId={} fileId={}", objectId, source.fileId());
            return toStatus(mark(reload(record.getId()), "FAILED",
                    "预览转换排队已满，请稍后重试", record.getPreviewFileId(), userId));
        }
        return toStatus(reload(record.getId()));
    }

    /**
     * 回收超时未完成的 PENDING（进程重启/线程被中断留下的僵尸记录）→ FAILED（可重试）。
     * <p>只改状态与原因，不动 {@code previewFileId}：上一份可看的预览仍然保留。</p>
     *
     * @return 本轮回收到影响的行数
     */
    public int reapStalePending() {
        OffsetDateTime now = OffsetDateTime.now();
        return previewMapper.update(null, new LambdaUpdateWrapper<DocPreview>()
                .eq(DocPreview::getStatus, "PENDING")
                .lt(DocPreview::getUpdatedAt, now.minusMinutes(staleMinutes))
                .set(DocPreview::getStatus, "FAILED")
                .set(DocPreview::getMessage, "转换超时（进程重启或线程被中断），可重试")
                .set(DocPreview::getUpdatedAt, now));
    }

    /** 清理限流器里的空窗口（由维护任务周期调用，避免 userId 无限累积）。 */
    public void evictRateLimitWindows() {
        rateLimiter.evictIdle();
    }

    // ------------------------------------------------------------ 异步转换

    /**
     * 线程池里执行的实际转换。
     * <p>所有上下文（objectId/userId/源文件/转换器）都在提交前取好，线程内不再碰 CurrentUser。</p>
     */
    private void runConversion(Long objectId, Long recordId, FileService.FileView source,
                               PreviewConverter converter, Long userId, String key) {
        String ext = source.ext() == null ? "" : source.ext().toLowerCase(Locale.ROOT);
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
            DocPreview record = reload(recordId);
            Long previousFileId = record == null ? null : record.getPreviewFileId();
            // 先上传新产物、成功后再解绑旧产物：顺序反了会在上传失败时把「原本能看的预览」清掉
            // （异常走 settle，且外层没有事务可回滚，见 EXP-20261108-04 第 3 条）
            FileService.UploadResult uploaded = fileService.upload(objectId, FileService.ROLE_VIEW, 100,
                    previewName(source.fileName(), output.format()),
                    mimeOf(output.format()),
                    new ByteArrayInputStream(output.content()), userId);
            detachPrevious(objectId, previousFileId);
            DocPreview done = reload(recordId);
            if (done == null) {
                throw new IOException("预览记录已不存在：" + recordId);
            }
            done.setFormat(output.format());
            done.setConverter(converter.name());
            // 预览产物是 VIEW 角色的文件链接，落库后同步子表统计（上传时统计已含 VIEW）
            refreshFileStats(objectId);
            mark(done, "READY", null, uploaded.fileId(), userId, output.pageCount());
        } catch (PreviewUnavailableException ex) {
            settle(recordId, converter, "SKIPPED", ex.getMessage(), userId);
        } catch (Exception ex) {
            log.warn("文档预览转换失败 objectId={} fileId={} converter={}", objectId, source.fileId(),
                    converter.name(), ex);
            settle(recordId, converter, "FAILED",
                    ex.getMessage() == null ? ex.toString() : ex.getMessage(), userId);
        } finally {
            deleteQuietly(workDir);
            inFlight.remove(key);
        }
    }

    /** 转换失败/跳过着陆：保留原产物指针，避免"这次失败把原本能看的预览也清掉"。 */
    private void settle(Long recordId, PreviewConverter converter, String status, String message, Long userId) {
        DocPreview record = reload(recordId);
        if (record == null) {
            return;
        }
        record.setConverter(converter.name());
        mark(record, status, message, record.getPreviewFileId(), userId);
    }

    /**
     * 抢占 PENDING（DB 侧 CAS）：只有"当前不是 PENDING"或"PENDING 已超时"才抢得到。
     * <p>多实例并发触发时只有一个能把状态改成 PENDING，其余直接返回当前状态。</p>
     */
    private boolean claimPending(DocPreview record, String converterName, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        int rows = previewMapper.update(null, new LambdaUpdateWrapper<DocPreview>()
                .eq(DocPreview::getId, record.getId())
                .and(w -> w.ne(DocPreview::getStatus, "PENDING")
                        .or()
                        .lt(DocPreview::getUpdatedAt, now.minusMinutes(staleMinutes)))
                .set(DocPreview::getStatus, "PENDING")
                .set(DocPreview::getMessage, null)
                .set(DocPreview::getConverter, converterName)
                .set(DocPreview::getUpdatedBy, userId)
                .set(DocPreview::getUpdatedAt, now));
        if (rows <= 0) {
            log.debug("预览已在转换中，跳过重复触发 objectId={} fileId={}",
                    record.getObjectId(), record.getSourceFileId());
        }
        return rows > 0;
    }

    private DocPreview reload(Long recordId) {
        return recordId == null ? null : previewMapper.selectById(recordId);
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
        // 新建记录尚未开始转换：用 NONE 而不是 PENDING，否则下面的 PENDING CAS 会把自己挡在门外
        record.setStatus("NONE");
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
