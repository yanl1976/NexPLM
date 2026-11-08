package com.nexplm.platform.doc.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预览用户级限流：滑动窗口边界、按用户隔离、关闭开关。
 */
class PreviewRateLimiterTest {

    @Test
    void allowsUpToLimitInsideWindowAndRejectsBeyond() {
        PreviewRateLimiter limiter = new PreviewRateLimiter(3);
        long now = 1_000_000L;

        assertThat(limiter.tryAcquire(1L, now)).isTrue();
        assertThat(limiter.tryAcquire(1L, now + 10)).isTrue();
        assertThat(limiter.tryAcquire(1L, now + 20)).isTrue();
        assertThat(limiter.tryAcquire(1L, now + 30)).as("第 4 次应被拒绝").isFalse();

        assertThat(limiter.tryAcquire(2L, now + 30)).as("另一个用户不受影响").isTrue();

        assertThat(limiter.tryAcquire(1L, now + 60_000)).as("窗口滑过后恢复").isTrue();
    }

    @Test
    void zeroOrNegativeLimitDisablesCheck() {
        assertThat(new PreviewRateLimiter(0).tryAcquire(1L, 0L)).isTrue();
        assertThat(new PreviewRateLimiter(-1).tryAcquire(1L, 0L)).isTrue();
        assertThat(new PreviewRateLimiter(5).tryAcquire(null, 0L)).as("系统调用不做用户级限制").isTrue();
    }

    @Test
    void evictIdleKeepsWindowsWithHits() {
        PreviewRateLimiter limiter = new PreviewRateLimiter(2);
        limiter.tryAcquire(1L, 0L);
        assertThat(limiter.activeWindows()).isEqualTo(1);

        limiter.evictIdle();
        assertThat(limiter.activeWindows()).as("有命中记录的窗口不能被清掉").isEqualTo(1);
        assertThat(limiter.tryAcquire(1L, 1L)).isTrue();
        assertThat(limiter.tryAcquire(1L, 2L)).isFalse();
    }
}
