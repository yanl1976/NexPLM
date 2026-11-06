package com.nexplm.platform.integration.job;

import com.nexplm.platform.integration.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 集成重试任务：把到期的 PENDING/FAILED 任务再发一次解，并回收卡住的 RUNNING。
 *
 * <p>为什么重试要后台跑而不是提交时同步重试：对端不可用时同步重试会把请求线程占满，
 * 一次失败要等几十秒，用户侧看起来就是"提交没反应"。同步只发一次，剩下的交给这里。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.integration.retry.enabled", havingValue = "true",
        matchIfMissing = true)
public class ItgRetryJob {

    private static final Logger log = LoggerFactory.getLogger(ItgRetryJob.class);

    private static final int BATCH_LIMIT = 50;

    private final IntegrationService integrationService;
    private final int staleMinutes;

    public ItgRetryJob(IntegrationService integrationService,
                       @Value("${nexplm.integration.retry.stale-minutes:10}") int staleMinutes) {
        this.integrationService = integrationService;
        this.staleMinutes = staleMinutes;
    }

    @Scheduled(initialDelayString = "PT30S",
            fixedDelayString = "${nexplm.integration.retry.fixed-delay:PT60S}")
    public void run() {
        try {
            int reclaimed = integrationService.reclaimStaleRunning(staleMinutes);
            if (reclaimed > 0) {
                log.info("回收卡住的集成任务：{} 条", reclaimed);
            }
            int retried = integrationService.retryDue(BATCH_LIMIT);
            if (retried > 0) {
                log.info("集成任务重试完成：{} 条", retried);
            }
        } catch (RuntimeException ex) {
            log.warn("集成重试任务执行失败：{}", ex.getMessage());
        }
    }
}
