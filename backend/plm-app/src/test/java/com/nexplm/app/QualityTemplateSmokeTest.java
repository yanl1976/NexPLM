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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P8-2 8D 模板与看板冒烟：模板 → 应用到问题 → 同轮防重 → 打回后新一轮可再应用 → 看板口径。
 */
@SpringBootTest
@AutoConfigureMockMvc
class QualityTemplateSmokeTest {

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
    void applyTemplatePerRound() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);

        // 1. 建模板 + 整表替换步骤（取号 DT-）
        JsonNode template = call(post("/api/v1/qlt/templates"), admin,
                json(Map.of("name", "标准 8D", "templateType", "8D")));
        long templateId = template.path("id").asLong();
        assertThat(template.path("code").asText()).startsWith("DT-");
        JsonNode withItems = call(put("/api/v1/qlt/templates/{id}/items", templateId), admin, json(Map.of(
                "items", List.of(
                        Map.of("itemCode", "D3", "name", "临时遏制措施", "actionType", "CONTAINMENT",
                                "prompt", "先止血的措施："),
                        Map.of("itemCode", "D4", "name", "根因分析", "actionType", "ROOT_CAUSE",
                                "prompt", "5Why 结论：")))));
        assertThat(withItems.path("items")).hasSize(2);
        assertThat(withItems.path("items").get(0).path("itemCode").asText()).isEqualTo("D3");

        // 步骤编号重复 → 400
        mockMvc.perform(put("/api/v1/qlt/templates/{id}/items", templateId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("items", List.of(
                                Map.of("itemCode", "D1", "name", "A"),
                                Map.of("itemCode", "d1", "name", "B"))))))
                .andExpect(status().isBadRequest());

        // 2. 停用的模板不能应用
        JsonNode disabled = call(post("/api/v1/qlt/templates"), admin,
                json(Map.of("name", "停用模板", "enabled", false)));
        long disabledId = disabled.path("id").asLong();
        call(put("/api/v1/qlt/templates/{id}/items", disabledId), admin, json(Map.of(
                "items", List.of(Map.of("itemCode", "D1", "name", "成立小组")))));

        JsonNode issue = call(post("/api/v1/qlt/issues"), admin,
                json(Map.of("title", "焊缝气孔", "severity", "MAJOR", "ownerId", adminId)));
        long issueId = issue.path("issue").path("id").asLong();

        mockMvc.perform(post("/api/v1/qlt/issues/{id}/apply-template", issueId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("templateId", disabledId))))
                .andExpect(status().isBadRequest());

        // 3. 应用模板：生成 2 条措施，状态推进到整改中，记录模板编码
        JsonNode applied = call(post("/api/v1/qlt/issues/{id}/apply-template", issueId), admin,
                json(Map.of("templateId", templateId)));
        assertThat(applied.path("actions")).hasSize(2);
        assertThat(applied.path("issue").path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(applied.path("issue").path("templateCode").asText())
                .isEqualTo(template.path("code").asText());
        assertThat(applied.path("actions").get(0).path("content").asText()).startsWith("[D3]");
        assertThat(applied.path("actions").get(0).path("status").asText()).isEqualTo("PENDING");

        // 4. 同一轮重复应用同一模板 → 409
        mockMvc.perform(post("/api/v1/qlt/issues/{id}/apply-template", issueId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("templateId", templateId))))
                .andExpect(status().isConflict());

        // 5. 完成本轮措施 → 提交 → 验证不通过 → 第 2 轮可再次应用（轮次变了）
        for (JsonNode action : applied.path("actions")) {
            call(post("/api/v1/qlt/issues/{id}/actions/{actionId}/done?done=true", issueId,
                    action.path("id").asLong()), admin, null);
        }
        call(post("/api/v1/qlt/issues/{id}/submit", issueId), admin, null);
        JsonNode rejected = call(post("/api/v1/qlt/issues/{id}/verify", issueId), admin,
                json(Map.of("passed", false, "note", "根因未查清")));
        assertThat(rejected.path("issue").path("roundNo").asInt()).isEqualTo(2);

        JsonNode second = call(post("/api/v1/qlt/issues/{id}/apply-template", issueId), admin,
                json(Map.of("templateId", templateId)));
        assertThat(second.path("actions")).hasSize(4);

        // 6. 已关闭的问题不能应用模板
        JsonNode ready = second;
        for (JsonNode action : ready.path("actions")) {
            if ("PENDING".equals(action.path("status").asText())) {
                call(post("/api/v1/qlt/issues/{id}/actions/{actionId}/done?done=true", issueId,
                        action.path("id").asLong()), admin, null);
            }
        }
        call(post("/api/v1/qlt/issues/{id}/submit", issueId), admin, null);
        JsonNode closed = call(post("/api/v1/qlt/issues/{id}/verify", issueId), admin,
                json(Map.of("passed", true, "note", "复检合格")));
        assertThat(closed.path("issue").path("status").asText()).isEqualTo("CLOSED");
        mockMvc.perform(post("/api/v1/qlt/issues/{id}/apply-template", issueId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("templateId", templateId))))
                .andExpect(status().isConflict());

        // 7. 看板：关闭率为全量快照（1/1=100），趋势按月、责任人负荷只算未关闭
        JsonNode board = call(get("/api/v1/qlt/dashboard"), admin, null);
        assertThat(board.path("total").asInt()).isEqualTo(1);
        assertThat(board.path("closed").asInt()).isEqualTo(1);
        assertThat(board.path("open").asInt()).isZero();
        assertThat(board.path("closedRate").asInt()).isEqualTo(100);
        assertThat(board.path("byStatus")).isNotEmpty();
        assertThat(board.path("trend")).isNotEmpty();
        assertThat(board.path("topOwners")).isEmpty();

        // 8. 删除模板：列表查不到、详情 404，但已生成的措施保留
        call(delete("/api/v1/qlt/templates/{id}", templateId), admin, null);
        mockMvc.perform(get("/api/v1/qlt/templates/{id}", templateId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        JsonNode page = call(get("/api/v1/qlt/templates?page=1&size=20"), admin, null);
        assertThat(page.path("total").asInt()).isEqualTo(1);
        Integer actionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM qlt_issue_action WHERE issue_id = ? AND deleted = false",
                Integer.class, issueId);
        assertThat(actionCount).isEqualTo(4);
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
