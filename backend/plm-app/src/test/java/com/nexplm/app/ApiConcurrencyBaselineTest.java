package com.nexplm.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P10-4 本地并发基线（<b>不是 DoD 验收</b>）。
 *
 * <p>DoD 的「200 并发 P95 &lt; 1s」必须用 {@code deploy/perf/load-test.mjs} 在目标环境跑——
 * 那里才有 Tomcat 线程池、真实网络往返、独立数据库。本用例在嵌入式 PG 上直接用多线程打 MockMvc，
 * 能发现的只有<b>应用层共享状态问题</b>：误加的全局锁、非线程安全缓存、连接串行化等。
 * 因此预算给得宽（P95 ≤ 2s），只要它退化就说明有并发问题，而不是"这个数就是验收值"。</p>
 *
 * <p>选这两个接口的原因：{@code /ana/dashboard} 是六域聚合（一次请求 18 条 SQL），
 * {@code /workflow/tasks} 是最常见的分页查询；它们都不需要造 1 万行业务数据，
 * 而 BOM 的大数据量场景已由 {@link BomPerformanceTest} 覆盖。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiConcurrencyBaselineTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动测试环境失败", ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("nexplm.security.bootstrap.admin-password", () -> "Admin@12345");
    }

    /** 并发度：本机 CI 友好的规模，压满 200 并发留给目标环境。 */
    private static final int CONCURRENCY = 50;
    private static final int PER_WORKER = 20;
    private static final long BUDGET_P95_MS = 2000L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentReadsStayWithinBaseline() throws Exception {
        String token = login();

        // 预热：JIT 与连接池首次建立不该算进基线
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/ana/dashboard").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        List<Long> durations = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);

        for (int worker = 0; worker < CONCURRENCY; worker++) {
            final int index = worker;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int round = 0; round < PER_WORKER; round++) {
                        // 偶数轮打六域聚合、奇数轮打分页列表，避免单一接口把缓存压成假象
                        String path = (round % 2 == 0)
                                ? "/api/v1/ana/dashboard"
                                : "/api/v1/workflow/tasks?page=1&size=20";
                        long begin = System.nanoTime();
                        MvcResult result = mockMvc.perform(get(path)
                                .header("Authorization", "Bearer " + token)).andReturn();
                        durations.add((System.nanoTime() - begin) / 1_000_000);
                        if (result.getResponse().getStatus() != 200) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (Exception ex) {
                    failures.addAndGet(PER_WORKER);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(180, TimeUnit.SECONDS))
                .withFailMessage("并发基线超时未完成（50×20 次请求超过 180s）")
                .isTrue();
        pool.shutdownNow();

        List<Long> sorted = new ArrayList<>(durations);
        Collections.sort(sorted);
        long p50 = percentile(sorted, 0.50);
        long p95 = percentile(sorted, 0.95);
        long p99 = percentile(sorted, 0.99);

        System.out.printf("[perf-baseline] 请求 %d 次，失败 %d，P50 %dms / P95 %dms / P99 %dms%n",
                sorted.size(), failures.get(), p50, p95, p99);

        assertThat(failures.get()).withFailMessage("并发下出现非 200 响应").isZero();
        assertThat(sorted).hasSize(CONCURRENCY * PER_WORKER);
        assertThat(p95).withFailMessage(
                "P95 %dms 超过本地基线预算 %dms（先查是否引入了全局锁/串行化，"
                        + "再用 deploy/perf/load-test.mjs 在目标环境复测）", p95, BUDGET_P95_MS)
                .isLessThanOrEqualTo(BUDGET_P95_MS);
    }

    // ------------------------------------------------------------------ 工具

    private static long percentile(List<Long> sorted, double ratio) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(sorted.size() * ratio) - 1;
        return sorted.get(Math.min(sorted.size() - 1, Math.max(0, index)));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("accessToken").asText();
    }
}
