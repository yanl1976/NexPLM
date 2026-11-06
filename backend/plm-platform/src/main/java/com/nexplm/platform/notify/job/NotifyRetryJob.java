package com.nexplm.platform.notify.job;

import com.nexplm.platform.notify.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 通知重试：到期的 PENDING/FAILED 再发一次，并回收卡住的 SENDING。
 *
 * <p>与 P9-1/P9-4 同一取舍：请求线程里只同步发一次，剩下的交给后台——
 * SMTP 或机器人不可用时同步重试会把接口拖死。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.notify.retry.enabled", havingValue = "true",
        matchIfMissing = true)
public class NotifyRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotifyRetryJob.class);

    private static final int BATCH_LIMIT = 50;

    private final NotifyService notifyService;
    private final int staleMinutes;

    public NotifyRetryJob(NotifyService notifyService,
                          @Value("${nexplm.notify.retry.stale-minutes:10}") int staleMinutes) {
        this.notifyService = notifyService;
        this.staleMinutes = staleMinutes;
    }

    @Scheduled(initialDelayString = "PT50S",
            fixedDelayString = "${nexplm.notify.retry.fixed-delay:PT60S}")
    public void run() {
        try {
            int reclaimed = notifyService.reclaimStaleSending(staleMinutes);
            if (reclaimed > 0) {
                log.info("回收卡住的通知消息：{} 条", reclaimed);
            }
            int retried = notifyService.retryDue(BATCH_LIMIT);
            if (retried > 0) {
                log.info("通知重试完成：{} 条", retried);
            }
        } catch (RuntimeException ex) {
            log.warn("通知重试任务执行失败：{}", ex.getMessage());
        }
    }
}
