package com.nexplm.platform.integration.job;

import com.nexplm.platform.integration.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Webhook 投递重试：把到期的 PENDING/FAILED 再发一次，并回收卡住的 SENDING。
 *
 * <p>与 P9-1 集成任务同一套取舍：提交时同步只发一次，剩下的交给后台——
 * 对端不可用时同步重试会把请求线程占满。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.webhook.retry.enabled", havingValue = "true",
        matchIfMissing = true)
public class WebhookRetryJob {

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryJob.class);

    private static final int BATCH_LIMIT = 50;

    private final WebhookService webhookService;
    private final int staleMinutes;

    public WebhookRetryJob(WebhookService webhookService,
                           @Value("${nexplm.webhook.retry.stale-minutes:10}") int staleMinutes) {
        this.webhookService = webhookService;
        this.staleMinutes = staleMinutes;
    }

    @Scheduled(initialDelayString = "PT45S",
            fixedDelayString = "${nexplm.webhook.retry.fixed-delay:PT60S}")
    public void run() {
        try {
            int reclaimed = webhookService.reclaimStaleSending(staleMinutes);
            if (reclaimed > 0) {
                log.info("回收卡住的 Webhook 投递：{} 条", reclaimed);
            }
            int retried = webhookService.retryDue(BATCH_LIMIT);
            if (retried > 0) {
                log.info("Webhook 投递重试完成：{} 条", retried);
            }
        } catch (RuntimeException ex) {
            log.warn("Webhook 投递重试任务执行失败：{}", ex.getMessage());
        }
    }
}
