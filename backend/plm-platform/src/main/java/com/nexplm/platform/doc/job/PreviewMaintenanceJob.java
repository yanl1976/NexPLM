package com.nexplm.platform.doc.job;

import com.nexplm.platform.doc.service.PreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预览维护：回收超时未完成的转换，并清理限流窗口。
 *
 * <p>为什么需要：转换是异步的（{@code PreviewService.generate} 提交后立即返回 PENDING），
 * 若进程在转换中途重启、或线程池被关闭打断，记录会永久停在 PENDING，前端一直显示"转换中"。
 * 这里按 {@code nexplm.preview.stale-minutes}（默认 5 分钟）把僵尸记录转成 FAILED（可重试）。</p>
 *
 * <p>与 {@code FileMaintenanceJob}/{@code NotifyRetryJob} 同一取舍：失败只记 warn，不向调度器抛。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.preview.maintenance.enabled", havingValue = "true",
        matchIfMissing = true)
public class PreviewMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(PreviewMaintenanceJob.class);

    private final PreviewService previewService;

    public PreviewMaintenanceJob(PreviewService previewService) {
        this.previewService = previewService;
    }

    @Scheduled(initialDelayString = "PT1M",
            fixedDelayString = "${nexplm.preview.maintenance.fixed-delay:PT60S}")
    public void run() {
        try {
            int reaped = previewService.reapStalePending();
            if (reaped > 0) {
                log.info("回收超时未完成的预览转换：{} 条", reaped);
            }
            previewService.evictRateLimitWindows();
        } catch (RuntimeException ex) {
            log.warn("预览维护任务执行失败：{}", ex.getMessage());
        }
    }
}
