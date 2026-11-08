package com.nexplm.platform.doc.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预览触发的按用户滑动窗口限流（进程内）。
 *
 * <p>转换要跑外部进程（最长 90 秒），只读用户也有触发权限（浏览即预览是产品语义），
 * 所以除了线程池的<b>全局</b>并发上限（2 并发 / 8 队列）之外，再加一层<b>用户级</b>配额：
 * 全局池防总量，用户级防单点刷。</p>
 *
 * <p>进程内实现：多副本部署时各副本独立计数（真正的分布式配额需要 Redis，见台账 B16）。
 * 窗口为空时由维护任务 {@link #evictIdle()} 清掉 key，避免 userId 无限累积。</p>
 */
class PreviewRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final int limitPerMinute;
    private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();

    PreviewRateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    /**
     * 尝试占用一次配额。
     *
     * @param userId    触发人；null 视为系统调用，不做用户级限制
     * @param nowMillis 当前时间（毫秒），显式传入便于单测
     * @return true 放行；false 超限
     */
    boolean tryAcquire(Long userId, long nowMillis) {
        if (limitPerMinute <= 0 || userId == null) {
            return true;
        }
        Deque<Long> window = windows.computeIfAbsent(userId, key -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && nowMillis - window.peekFirst() >= WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= limitPerMinute) {
                return false;
            }
            window.addLast(nowMillis);
            return true;
        }
    }

    /** 清掉空窗口（由维护任务周期调用）；非空窗口保留，避免正在计数的用户被重置。 */
    void evictIdle() {
        for (Iterator<Map.Entry<Long, Deque<Long>>> it = windows.entrySet().iterator(); it.hasNext(); ) {
            Deque<Long> window = it.next().getValue();
            synchronized (window) {
                if (window.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    int limitPerMinute() {
        return limitPerMinute;
    }

    /** 当前持有计数的用户数（诊断与单测用）。 */
    int activeWindows() {
        return windows.size();
    }
}
