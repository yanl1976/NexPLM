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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P10-1 统计看板冒烟：六域指标卡口径、超期判定、月度趋势补零、状态下钻与分页、非法域拒绝。
 *
 * <p>为什么直接用 SQL 造样本：被测对象就是"跨六域的聚合口径"，用六个域的创建接口造数据
 * 会把测试变成六套 CRUD 的联调（且任务依赖项目、工艺依赖零件，链路长且易碎），
 * 反而看不清聚合本身是否正确。这里只给每个域最小合法行，断言集中在卡片数字与下钻结果。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardSmokeTest {

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void overviewCountsDrilldownAndTrend() throws Exception {
        String admin = login("admin", "Admin@12345");
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // ---- 六域样本：文档 2（1 草稿 + 1 已发布）、零部件 1（已发布）、变更 1（执行中且超期）、
        //      工艺 1（已发布）、任务 2（1 完成 + 1 进行中且超期）、质量 2（1 未关闭且超期 + 1 已关闭）
        insertObject(91_000_000_001L, "PdmDoc", "DOC-900001", "草稿文档", companyId, "Draft", null);
        insertObject(91_000_000_002L, "PdmDoc", "DOC-900002", "已发布文档", companyId, "Released",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
        insertObject(91_000_000_003L, "PdmItem", "PN-900001", "已发布零件", companyId, "Released",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

        jdbcTemplate.update("""
                        INSERT INTO chg_ecr (id, company_id, code, title, change_type, priority, status,
                                             requester_id, target_date)
                        VALUES (?, ?, ?, ?, 'DESIGN', 'HIGH', 'EXECUTING', ?, ?)
                        """,
                91_000_000_011L, companyId, "ECR-900001", "超期变更", 1L, yesterday);

        jdbcTemplate.update("""
                        INSERT INTO capp_routing (id, company_id, code, name, object_id, status)
                        VALUES (?, ?, ?, ?, ?, 'RELEASED')
                        """,
                91_000_000_021L, companyId, "RT-900001", "已发布路线", 91_000_000_003L);

        jdbcTemplate.update("""
                        INSERT INTO prj_task (id, company_id, project_id, wbs_code, name, status, plan_end)
                        VALUES (?, ?, ?, ?, ?, 'DONE', ?)
                        """,
                91_000_000_031L, companyId, 1L, "9.1", "已完成任务", yesterday);
        jdbcTemplate.update("""
                        INSERT INTO prj_task (id, company_id, project_id, wbs_code, name, status, plan_end)
                        VALUES (?, ?, ?, ?, ?, 'IN_PROGRESS', ?)
                        """,
                91_000_000_032L, companyId, 1L, "9.2", "超期任务", yesterday);

        jdbcTemplate.update("""
                        INSERT INTO qlt_issue (id, company_id, code, title, severity, status, due_date)
                        VALUES (?, ?, ?, ?, 'MAJOR', 'OPEN', ?)
                        """,
                91_000_000_041L, companyId, "QI-900001", "超期质量问题", yesterday);

        // ---- 1. 总览：六张卡 + 各域口径
        JsonNode overview = call(get("/api/v1/ana/dashboard"), admin, null);
        assertThat(overview.path("domains")).hasSize(6);
        assertThat(overview.path("cards")).hasSize(6);

        JsonNode doc = card(overview, "doc");
        assertThat(doc.path("total").asLong()).isEqualTo(2);
        assertThat(doc.path("done").asLong()).isEqualTo(1);
        assertThat(doc.path("active").asLong()).isEqualTo(1);
        assertThat(doc.path("doneRate").asInt()).isEqualTo(50);
        assertThat(doc.path("overdue").asLong()).isZero();
        assertThat(doc.path("byStatus").findValuesAsText("key")).contains("Draft", "Released");

        JsonNode item = card(overview, "item");
        assertThat(item.path("total").asLong()).isEqualTo(1);
        assertThat(item.path("doneRate").asInt()).isEqualTo(100);

        JsonNode change = card(overview, "change");
        assertThat(change.path("active").asLong()).isEqualTo(1);
        assertThat(change.path("overdue").asLong()).isEqualTo(1);
        assertThat(change.path("done").asLong()).isZero();

        JsonNode capp = card(overview, "capp");
        assertThat(capp.path("total").asLong()).isEqualTo(1);
        assertThat(capp.path("done").asLong()).isEqualTo(1);

        JsonNode task = card(overview, "task");
        assertThat(task.path("total").asLong()).isEqualTo(2);
        assertThat(task.path("done").asLong()).isEqualTo(1);
        assertThat(task.path("active").asLong()).isEqualTo(1);
        assertThat(task.path("overdue").asLong()).isEqualTo(1);

        JsonNode quality = card(overview, "quality");
        assertThat(quality.path("total").asLong()).isEqualTo(1);
        assertThat(quality.path("active").asLong()).isEqualTo(1);
        assertThat(quality.path("overdue").asLong()).isEqualTo(1);

        // ---- 2. 下钻：按状态筛选只回该状态的行，字段六域统一
        JsonNode drafts = call(get("/api/v1/ana/dashboard/doc/items?status=Draft"), admin, null);
        assertThat(drafts.path("total").asInt()).isEqualTo(1);
        JsonNode draftRow = drafts.path("items").get(0);
        assertThat(draftRow.path("code").asText()).isEqualTo("DOC-900001");
        assertThat(draftRow.path("status").asText()).isEqualTo("Draft");

        JsonNode taskDrill = call(get("/api/v1/ana/dashboard/task/items"), admin, null);
        assertThat(taskDrill.path("total").asInt()).isEqualTo(2);
        assertThat(taskDrill.path("items").get(0).path("code").asText()).isNotBlank();

        // 分页：size=1 → 1 行但 total 仍是 2
        JsonNode paged = call(get("/api/v1/ana/dashboard/task/items?page=1&size=1"), admin, null);
        assertThat(paged.path("items")).hasSize(1);
        assertThat(paged.path("total").asInt()).isEqualTo(2);
        assertThat(paged.path("size").asInt()).isEqualTo(1);

        // ---- 3. 非法域 → 400
        mockMvc.perform(get("/api/v1/ana/dashboard/unknown/items")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // ---- 4. 趋势：期间内逐月补齐，每月都带全六个域键（无数据补 0）
        JsonNode trendOverview = call(get("/api/v1/ana/dashboard?from=2026-01-01&to=2026-03-31"),
                admin, null);
        JsonNode trend = trendOverview.path("trend");
        assertThat(trend).hasSize(3);
        assertThat(trend.get(0).path("month").asText()).isEqualTo("2026-01");
        assertThat(trend.get(2).path("month").asText()).isEqualTo("2026-03");
        assertThat(trend.get(0).path("domains").size()).isEqualTo(6);
        assertThat(trend.get(0).path("total").asLong()).isZero();

        // 期间外的数据不影响期间内新增数：今天造的数据落在 2026-01~03 之外
        JsonNode januaryDoc = card(trendOverview, "doc");
        assertThat(januaryDoc.path("addedInPeriod").asLong()).isZero();
        assertThat(januaryDoc.path("total").asLong()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ 工具

    private void insertObject(long id, String objectType, String code, String name, Long companyId,
                              String state, java.sql.Timestamp releasedAt) {
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                                lifecycle_state, is_latest, deleted, released_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, true, false, ?)
                        """,
                id, objectType, id, code, name, companyId, state, releasedAt);
    }

    private JsonNode card(JsonNode overview, String domain) {
        for (JsonNode node : overview.path("cards")) {
            if (domain.equals(node.path("domain").asText())) {
                return node;
            }
        }
        throw new AssertionError("看板缺少域：" + domain);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload)
            throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus())
                .withFailMessage("HTTP %s body=%s", result.getResponse().getStatus(),
                        result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(200);
        JsonNode root = body(result);
        return root.has("data") ? root.path("data") : root;
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
