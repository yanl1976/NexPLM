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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P8-3-2 体系审核冒烟：计划 → 检查表 → 开始 → 不符合项 → 转质量问题 → 整改关闭 → 关闭审核。
 */
@SpringBootTest
@AutoConfigureMockMvc
class QltAuditSmokeTest {

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
    void auditPlanToIssueClosedLoop() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);

        // 1. 新建审核计划（AU- 取号）
        JsonNode plan = call(post("/api/v1/qlt/audit-plans"), admin, json(Map.of(
                "title", "2026 年度内部质量体系审核",
                "auditType", "INTERNAL",
                "standard", "IATF16949:2016",
                "scope", "生产部/质量部/采购部",
                "plannedDate", LocalDate.now().toString(),
                "auditorId", adminId,
                "auditee", "生产部")));
        long planId = plan.path("id").asLong();
        assertThat(plan.path("code").asText()).startsWith("AU-");
        assertThat(plan.path("status").asText()).isEqualTo("PLANNED");
        assertThat(plan.path("auditorName").asText()).isNotBlank();

        // 2. 检查表为空不能开始审核
        mockMvc.perform(post("/api/v1/qlt/audit-plans/{id}/start", planId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 3. 检查表整表替换（1 OK / 1 NG / 1 待检）；编号重复 → 400
        JsonNode withItems = call(put("/api/v1/qlt/audit-plans/{id}/items", planId), admin, json(Map.of(
                "items", List.of(
                        Map.of("itemNo", "1", "clause", "8.5.1", "question", "是否按控制计划作业",
                                "result", "OK", "evidence", "抽查 3 份作业记录"),
                        Map.of("itemNo", "2", "clause", "8.7", "question", "不合格品是否隔离标识",
                                "result", "NG", "evidence", "现场 2 箱未标识"),
                        Map.of("itemNo", "3", "clause", "9.2", "question", "内审是否按计划执行")))));
        assertThat(withItems.path("items")).hasSize(3);
        assertThat(withItems.path("stats").path("okCount").asInt()).isEqualTo(1);
        assertThat(withItems.path("stats").path("ngCount").asInt()).isEqualTo(1);
        assertThat(withItems.path("stats").path("pendingCount").asInt()).isEqualTo(1);
        mockMvc.perform(put("/api/v1/qlt/audit-plans/{id}/items", planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("items", List.of(
                                Map.of("itemNo", "A", "question", "第一条"),
                                Map.of("itemNo", "a", "question", "重复编号"))))))
                .andExpect(status().isBadRequest());

        // 4. 开始审核 → IN_PROGRESS + 实际日期
        JsonNode started = call(post("/api/v1/qlt/audit-plans/{id}/start", planId), admin, null);
        assertThat(started.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(started.path("actualDate").asText()).isEqualTo(LocalDate.now().toString());

        // 5. 登记不符合项（关联检查项 2）；未知严重度 → 400
        long ngItemId = withItems.path("items").get(1).path("id").asLong();
        JsonNode withFinding = call(post("/api/v1/qlt/audit-plans/{id}/findings", planId), admin,
                json(Map.of("title", "不合格品未隔离标识", "description", "现场 2 箱未挂标识牌",
                        "severity", "MAJOR", "dueDate", LocalDate.now().plusDays(7).toString(),
                        "itemId", ngItemId)));
        assertThat(withFinding.path("findings")).hasSize(1);
        JsonNode finding = withFinding.path("findings").get(0);
        long findingId = finding.path("id").asLong();
        assertThat(finding.path("code").asText()).startsWith("FD-");
        assertThat(finding.path("itemNo").asText()).isEqualTo("2");
        assertThat(finding.path("status").asText()).isEqualTo("OPEN");
        mockMvc.perform(post("/api/v1/qlt/audit-plans/{id}/findings", planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "严重度写错", "severity", "BLOCKER"))))
                .andExpect(status().isBadRequest());

        // 6. 未转问题可改；改完仍是 OPEN
        JsonNode updated = call(put("/api/v1/qlt/audit-findings/{id}", findingId), admin,
                json(Map.of("title", "不合格品未隔离标识（含库房）", "severity", "MAJOR",
                        "dueDate", LocalDate.now().plusDays(10).toString())));
        assertThat(updated.path("findings").get(0).path("title").asText()).contains("库房");

        // 7. 有未关闭不符合项 → 不能关闭审核
        mockMvc.perform(post("/api/v1/qlt/audit-plans/{id}/close", planId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 8. 不符合项转质量问题：source=AUDIT、objectCode=审核计划编码
        JsonNode toIssue = call(post("/api/v1/qlt/audit-findings/{id}/to-issue", findingId), admin,
                null);
        JsonNode converted = toIssue.path("findings").get(0);
        long issueId = converted.path("issueId").asLong();
        String issueCode = converted.path("issueCode").asText();
        assertThat(issueCode).startsWith("QI-");
        Map<String, Object> issueRow = jdbcTemplate.queryForMap(
                "SELECT source, object_type, object_code, severity FROM qlt_issue WHERE id = ?", issueId);
        assertThat(issueRow.get("source")).isEqualTo("AUDIT");
        assertThat(issueRow.get("object_type")).isEqualTo("QltAuditPlan");
        assertThat(issueRow.get("object_code")).isEqualTo(plan.path("code").asText());
        assertThat(issueRow.get("severity")).isEqualTo("MAJOR");

        // 重复转问题 / 转问题后改或删 → 409
        mockMvc.perform(post("/api/v1/qlt/audit-findings/{id}/to-issue", findingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/qlt/audit-findings/{id}", findingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "转问题后还想改"))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/qlt/audit-findings/{id}", findingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 9. 质量问题未关闭时，不符合项不能关闭
        mockMvc.perform(post("/api/v1/qlt/audit-findings/{id}/close", findingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 10. 走 P8-1 闭环把问题关掉（分派 → 措施 → 完成 → 提交 → 验证通过）
        call(post("/api/v1/qlt/issues/{id}/assign", issueId), admin,
                json(Map.of("ownerId", adminId)));
        JsonNode withAction = call(post("/api/v1/qlt/issues/{id}/actions", issueId), admin,
                json(Map.of("actionType", "CORRECTIVE", "content", "补齐标识并纳入日常巡检")));
        long actionId = withAction.path("actions").get(0).path("id").asLong();
        call(post("/api/v1/qlt/issues/{id}/actions/{actionId}/done?done=true", issueId, actionId),
                admin, null);
        call(post("/api/v1/qlt/issues/{id}/submit", issueId), admin, null);
        call(post("/api/v1/qlt/issues/{id}/verify", issueId), admin,
                json(Map.of("passed", true, "note", "现场复查合格")));

        // 11. 问题关闭后，不符合项才能关闭
        JsonNode findingClosed = call(post("/api/v1/qlt/audit-findings/{id}/close", findingId),
                admin, null);
        assertThat(findingClosed.path("findings").get(0).path("status").asText()).isEqualTo("CLOSED");
        assertThat(findingClosed.path("stats").path("openFindingCount").asInt()).isZero();

        // 12. 关闭审核 → CLOSED；关闭后不能再改计划/检查表/登记不符合项
        JsonNode closed = call(post("/api/v1/qlt/audit-plans/{id}/close", planId), admin, null);
        assertThat(closed.path("status").asText()).isEqualTo("CLOSED");
        mockMvc.perform(put("/api/v1/qlt/audit-plans/{id}", planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "关闭后改名"))))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/qlt/audit-plans/{id}/items", planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("items", List.of(Map.of("itemNo", "1",
                                "question", "关闭后还想改"))))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/qlt/audit-plans/{id}/findings", planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "关闭后补登"))))
                .andExpect(status().isConflict());

        // 13. 列表与详情统计
        JsonNode page = call(get("/api/v1/qlt/audit-plans?status=CLOSED"), admin, null);
        assertThat(page.path("total").asInt()).isEqualTo(1);
        JsonNode detail = call(get("/api/v1/qlt/audit-plans/{id}", planId), admin, null);
        assertThat(detail.path("stats").path("itemTotal").asInt()).isEqualTo(3);
        assertThat(detail.path("stats").path("findingTotal").asInt()).isEqualTo(1);
        assertThat(detail.path("findings").get(0).path("issueCode").asText()).isEqualTo(issueCode);

        // 14. 未转问题的草稿计划可删；已开始/已关闭的不可删
        JsonNode draft = call(post("/api/v1/qlt/audit-plans"), admin,
                json(Map.of("title", "临时计划", "auditType", "PROCESS")));
        call(delete("/api/v1/qlt/audit-plans/{id}", draft.path("id").asLong()), admin, null);
        mockMvc.perform(delete("/api/v1/qlt/audit-plans/{id}", planId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ 工具

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
