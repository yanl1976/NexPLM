package com.nexplm.platform.file.job;

import com.nexplm.platform.file.service.MultipartUploadService;
import com.nexplm.platform.file.storage.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文件仓维护任务：回收孤儿物理文件、清理超时未完成的分片会话。
 * <p>孤儿来源：上传时物理文件已落盘但元数据写入失败、或历史版本被删后引用计数归零。
 * 清理带冷静期（默认 24 小时）并二次校验引用，避免误删正在被使用的文件。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.file.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class FileMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(FileMaintenanceJob.class);

    private static final int BATCH_LIMIT = 200;

    private record Orphan(Long id, String objectKey) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final FileStorage storage;
    private final MultipartUploadService multipartUploadService;
    private final int retainHours;

    public FileMaintenanceJob(JdbcTemplate jdbcTemplate, FileStorage storage,
                              MultipartUploadService multipartUploadService,
                              @Value("${nexplm.file.maintenance.retain-hours:24}") int retainHours) {
        this.jdbcTemplate = jdbcTemplate;
        this.storage = storage;
        this.multipartUploadService = multipartUploadService;
        this.retainHours = retainHours;
    }

    /** 回收无任何关联且超过冷静期的物理文件。 */
    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT1H")
    public void purgeOrphanFiles() {
        List<Orphan> orphans;
        try {
            orphans = jdbcTemplate.query("""
                    SELECT f.id, f.object_key FROM file_object f
                     WHERE f.ref_count <= 0
                       AND f.created_at < now() - make_interval(hours => ?)
                       AND NOT EXISTS (SELECT 1 FROM file_link l WHERE l.file_id = f.id)
                     ORDER BY f.created_at
                     LIMIT ?
                    """, (rs, rowNum) -> new Orphan(rs.getLong("id"), rs.getString("object_key")),
                    retainHours, BATCH_LIMIT);
        } catch (RuntimeException ex) {
            log.warn("孤儿文件扫描失败：{}", ex.getMessage());
            return;
        }
        int purged = 0;
        for (Orphan orphan : orphans) {
            try {
                Integer linked = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM file_link WHERE file_id = ?", Integer.class, orphan.id());
                if (linked != null && linked > 0) {
                    jdbcTemplate.update("UPDATE file_object SET ref_count = ? WHERE id = ?", linked, orphan.id());
                    continue;
                }
                storage.delete(orphan.objectKey());
                jdbcTemplate.update("DELETE FROM file_object WHERE id = ? AND ref_count <= 0", orphan.id());
                purged++;
            } catch (RuntimeException ex) {
                log.warn("孤儿文件清理失败：id={} 原因={}", orphan.id(), ex.getMessage());
            }
        }
        if (purged > 0) {
            log.info("孤儿文件清理完成：{} 个", purged);
        }
    }

    /** 中断长时间无进展的分片上传会话并清理分片。 */
    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT1H")
    public void abortStaleUploads() {
        List<String> stale;
        try {
            stale = jdbcTemplate.queryForList("""
                    SELECT id FROM file_upload_session
                     WHERE status = 'UPLOADING' AND updated_at < now() - make_interval(hours => ?)
                     LIMIT ?
                    """, String.class, retainHours, BATCH_LIMIT);
        } catch (RuntimeException ex) {
            log.warn("过期上传会话扫描失败：{}", ex.getMessage());
            return;
        }
        int aborted = 0;
        for (String uploadId : stale) {
            try {
                multipartUploadService.abort(uploadId);
                aborted++;
            } catch (RuntimeException ex) {
                log.warn("过期上传会话清理失败：{} 原因={}", uploadId, ex.getMessage());
            }
        }
        if (aborted > 0) {
            log.info("过期上传会话清理完成：{} 个", aborted);
        }
    }
}
