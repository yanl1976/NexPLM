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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P8-1 质量问题闭环冒烟：一个质量问题走完两轮整改并关闭（P8 DoD）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class QualityIssueSmokeTest {

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
    void twoRoundsThenClose() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 1. 登记（带期限已过 → overdue）
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("title", "客户投诉：焊缝气孔");
        createBody.put("description", "批次 A 焊缝出现气孔");
        createBody.put("source", "CUSTOMER");
        createBody.put("severity", "CRITICAL");
        createBody.put("dueDate", yesterday.toString());
        JsonNode created = call(post("/api/v1/qlt/issues"), admin, json(createBody));
        long issueId = created.path("issue").path("id").asLong();
        assertThat(created.path("issue").path("code").asText()).startsWith("QI-");
        assertThat(created.path("issue").path("status").asText()).isEqualTo("OPEN");
        assertThat(created.path("issue").path("overdue").asBoolean()).isTrue();

        // 2. 未分派就提交验证 → 409
        mockMvc.perform(post("/api/v1/qlt/issues/{id}/submit", issueId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 3. 分派 → ASSIGNED
        JsonNode assigned = call(post("/api/v1/qlt/issues/{id}/assign", issueId), admin,
                json(Map.of("ownerId", adminId)));
        assertThat(assigned.path("issue").path("status").asText()).isEqualTo("ASSIGNED");

        // 4. 第 1 轮：临时遏制措施 → IN_PROGRESS
        JsonNode round1 = call(post("/api/v1/qlt/issues/{id}/actions", issueId), admin,
                json(Map.of("actionType", "CONTAINMENT", "content", "全检并隔离库存")));
        assertThat(round1.path("issue").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(round1.path("issue").path("roundNo").asInt()).isEqualTo(1);
        assertThat(round1.path("actions")).hasSize(1);
        long action1 = round1.path("actions").get(0).path("id").asLong();

        // 5. 措施未完成就提交 → 409
        mockMvc.perform(post("/api/v1/qlt/issues/{id}/submit", issueId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 6. 完成措施 → 提交验证 → VERIFYING
        call(post("/api/v1/qlt/issues/{id}/actions/{actionId}/done?done=true", issueId, action1),
                admin, null);
        JsonNode verifying = call(post("/api/v1/qlt/issues/{id}/submit", issueId), admin, null);
        assertThat(verifying.path("issue").path("status").asText()).isEqualTo("VERIFYING");

        // 7. 验证不通过 → 退回整改并开启第 2 轮
        JsonNode rejected = call(post("/api/v1/qlt/issues/{id}/verify", issueId), admin,
                json(Map.of("passed", false, "note", "未触及根因，气孔仍会复现")));
        assertThat(rejected.path("issue").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(rejected.path("issue").path("roundNo").asInt()).isEqualTo(2);
        assertThat(rejected.path("issue").path("verifyNote").asText()).contains("根因");

        // 8. 第 2 轮：根因 + 纠正措施（都挂 roundNo=2）
        JsonNode round2 = call(post("/api/v1/qlt/issues/{id}/actions", issueId), admin,
                json(Map.of("actionType", "ROOT_CAUSE", "content", "保护气体流量不足")));
        assertThat(round2.path("actions")).hasSize(2);
        assertThat(round2.path("actions").get(1).path("roundNo").asInt()).isEqualTo(2);
        long action2 = round2.path("actions").get(1).path("id").asLong();
        call(post("/api/v1/qlt/issues/{id}/actions/{actionId}/done?done=true", issueId, action2),
                admin, null);
        call(post("/api/v1/qlt/issues/{id}/submit", issueId), admin, null);

        // 9. 验证通过 → 关闭
        JsonNode closed = call(post("/api/v1/qlt/issues/{id}/verify", issueId), admin,
                json(Map.of("passed", true, "note", "复检合格")));
        assertThat(closed.path("issue").path("status").asText()).isEqualTo("CLOSED");
        assertThat(closed.path("issue").path("closedAt").isNull()).isFalse();
        assertThat(closed.path("issue").path("overdue").asBoolean()).isFalse();
        assertThat(closed.path("actions")).hasSize(2);

        // 10. 关闭后不能再加措施/再验证
        mockMvc.perform(post("/api/v1/qlt/issues/{id}/actions", issueId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "关闭后补一条"))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/qlt/issues/{id}/verify", issueId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("passed", true, "note", "重复验证"))))
                .andExpect(status().isConflict());

        // 11. 列表：按状态过滤能查到；overdue 过滤不含已关闭
        JsonNode closedPage = call(get("/api/v1/qlt/issues?status=CLOSED&page=1&size=10"), admin, null);
        assertThat(closedPage.path("total").asInt()).isEqualTo(1);
        JsonNode overduePage = call(get("/api/v1/qlt/issues?overdue=true&page=1&size=10"), admin, null);
        assertThat(overduePage.path("total").asInt()).isZero();
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
