package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BOM 性能门禁：10,000 行结构下，树展开、BOM 展开、BOM 汇总均在 2s 内返回。
 * <p>构造 4 层「总成 → 20 个部件 → 各 25 个子部件 → 各 20 个零件」，共 10,520 行 BOM。
 * 造数直接走 SQL（上万次 HTTP 会淹没被测耗时），计时用 MockMvc 走真实接口与序列化全链路。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class BomPerformanceTest {

    private static final EmbeddedPostgres POSTGRES;

    /** 结构规模：1 + 20 + 500 + 10000 = 10,521 个零部件、10,520 行 BOM。 */
    private static final int LEVEL2_COUNT = 20;
    private static final int LEVEL3_PER_PARENT = 25;
    private static final int LEVEL4_PER_PARENT = 20;
    private static final int NODE_TOTAL = 1 + LEVEL2_COUNT + LEVEL2_COUNT * LEVEL3_PER_PARENT
            + LEVEL2_COUNT * LEVEL3_PER_PARENT * LEVEL4_PER_PARENT;
    private static final long ID_BASE = 9_000_000L;
    private static final long LINE_ID_BASE = 9_500_000L;
    private static final long ROOT_ID = ID_BASE + 1;
    /** 计划要求：1 万行展开 < 2s。 */
    private static final long BUDGET_MS = 2000L;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tenThousandLineTreeWithinBudget() throws Exception {
        String token = login();
        seedStructure();

        long lineCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM bom_line", Long.class);
        assertThat(lineCount).isEqualTo(10_520L);

        // 首次请求包含 JIT 与执行计划预热，先跑一次不计时
        call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=10", ROOT_ID), token, null);

        long treeMs = elapsed(() -> call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=10", ROOT_ID), token, null));
        long summaryMs = elapsed(() -> call(
                get("/api/v1/items/{id}/bom/summary?viewType=DESIGN&depth=10", ROOT_ID), token, null));
        long rollupMs = elapsed(() -> call(
                get("/api/v1/items/{id}/bom/rollup?viewType=DESIGN&depth=10&leafOnly=true", ROOT_ID), token, null));
        System.out.printf("[BOM-PERF] lines=10520 tree=%dms summary=%dms rollup=%dms%n", treeMs, summaryMs, rollupMs);

        assertThat(treeMs).as("树展开耗时 %d ms", treeMs).isLessThan(BUDGET_MS);
        assertThat(summaryMs).as("BOM 展开耗时 %d ms", summaryMs).isLessThan(BUDGET_MS);
        assertThat(rollupMs).as("BOM 汇总耗时 %d ms", rollupMs).isLessThan(BUDGET_MS);

        // 结果正确性：叶子层 10,000 个零件各出现一次，汇总后仍是 10,000 行
        JsonNode rollup = call(get("/api/v1/items/{id}/bom/rollup?viewType=DESIGN&depth=10&leafOnly=true", ROOT_ID),
                token, null);
        assertThat(rollup.size()).isEqualTo(10_000);
        assertThat(rollup.get(0).path("totalQty").asDouble()).isEqualTo(1.0);
    }

    /** 用 SQL 批量造 4 层结构，避免上万次接口调用干扰计时。 */
    private void seedStructure() {
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, lifecycle_state)
                SELECT CAST(? AS bigint) + g, 'PdmItem', CAST(? AS bigint) + g,
                       'PERF-' || lpad(g::text, 6, '0'), '性能件' || g, 1, 'Draft'
                  FROM generate_series(1, ?) g
                """, ID_BASE, ID_BASE, NODE_TOTAL);

        jdbcTemplate.update("""
                INSERT INTO item_master (object_id, item_type, unit)
                SELECT CAST(? AS bigint) + g, CASE WHEN g = 1 THEN 'PRODUCT' ELSE 'PART' END, 'EA'
                  FROM generate_series(1, ?) g
                """, ID_BASE, NODE_TOTAL);

        // 第 2 层：根下 20 个部件
        jdbcTemplate.update("""
                INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit, loss_rate)
                SELECT CAST(? AS bigint) + (g - 1), CAST(? AS bigint) + 1, CAST(? AS bigint) + g,
                       'DESIGN', (g - 1) * 10, 1, 'EA', 0
                  FROM generate_series(2, ?) g
                """, LINE_ID_BASE, ID_BASE, ID_BASE, 1 + LEVEL2_COUNT);

        int level3Start = 2 + LEVEL2_COUNT;
        int level3End = level3Start + LEVEL2_COUNT * LEVEL3_PER_PARENT - 1;
        // 第 3 层：每个部件挂 25 个子部件
        jdbcTemplate.update("""
                INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit, loss_rate)
                SELECT CAST(? AS bigint) + (g - 1),
                       CAST(? AS bigint) + (2 + ((g - ?) / ?)),
                       CAST(? AS bigint) + g,
                       'DESIGN', (((g - ?) % ?) + 1) * 10, 1, 'EA', 0
                  FROM generate_series(?, ?) g
                """, LINE_ID_BASE, ID_BASE, level3Start, LEVEL3_PER_PARENT, ID_BASE,
                level3Start, LEVEL3_PER_PARENT, level3Start, level3End);

        int level4Start = level3End + 1;
        int level4End = level4Start + LEVEL2_COUNT * LEVEL3_PER_PARENT * LEVEL4_PER_PARENT - 1;
        // 第 4 层：每个子部件挂 20 个零件
        jdbcTemplate.update("""
                INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit, loss_rate)
                SELECT CAST(? AS bigint) + (g - 1),
                       CAST(? AS bigint) + (? + ((g - ?) / ?)),
                       CAST(? AS bigint) + g,
                       'DESIGN', (((g - ?) % ?) + 1) * 10, 1, 'EA', 0
                  FROM generate_series(?, ?) g
                """, LINE_ID_BASE, ID_BASE, level3Start, level4Start, LEVEL4_PER_PARENT, ID_BASE,
                level4Start, LEVEL4_PER_PARENT, level4Start, level4End);
    }

    private long elapsed(ThrowingRunnable action) throws Exception {
        long start = System.nanoTime();
        action.run();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return body(result).path("data");
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
