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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7-4 交付物与齐套性冒烟。
 *
 * <p>规则：交付物对象多态引用、实时回查不存快照；齐套只认「必需 OUTPUT」；
 * 「未发布」只提醒不拦，「已删除/已废弃」才拦提交审核。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectDeliverableSmokeTest {

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
    void deliverablesAndCompleteness() throws Exception {
        String admin = login("admin", "Admin@12345");
        long projectId = createProject(admin, "交付物齐套测试项目");
        long docId = insertPlmObject("PdmDoc", "DOC-P74-1", "设计说明书", "Released");
        long itemId = insertPlmObject("PdmItem", "PN-P74-1", "支架", "Draft");
        long cardId = insertCard("CC-P74-1", "支架工序卡");

        long taskA = createTask(admin, projectId, "编制设计文件");
        long taskB = createTask(admin, projectId, "工艺准备");
        call(post("/api/v1/prj/tasks/{id}/start", taskA), admin, null);
        call(post("/api/v1/prj/tasks/{id}/start", taskB), admin, null);

        // 1. A 挂已发布文档：OK，项目齐套
        JsonNode afterDoc = call(post("/api/v1/prj/tasks/{id}/deliverables", taskA), admin,
                json(deliverable("DOC", docId, "OUTPUT", true)));
        assertThat(afterDoc.path("requiredTotal").asInt()).isEqualTo(1);
        assertThat(afterDoc.path("readyTotal").asInt()).isEqualTo(1);
        assertThat(afterDoc.path("ready").asBoolean()).isTrue();
        JsonNode docRow = afterDoc.path("tasks").get(0).path("items").get(0);
        assertThat(docRow.path("code").asText()).isEqualTo("DOC-P74-1");
        assertThat(docRow.path("state").asText()).isEqualTo("OK");
        assertThat(docRow.path("ready").asBoolean()).isTrue();

        // 2. 重复挂同一对象 400
        mockMvc.perform(post("/api/v1/prj/tasks/{id}/deliverables", taskA)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(deliverable("DOC", docId, "OUTPUT", true))))
                .andExpect(status().isBadRequest());

        // 3. 对象不存在 400
        mockMvc.perform(post("/api/v1/prj/tasks/{id}/deliverables", taskA)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(deliverable("DOC", 123456789L, "OUTPUT", true))))
                .andExpect(status().isBadRequest());

        // 4. B 挂未发布物料 + 工艺卡片：未发布只记为 WARN，不拖累齐套
        JsonNode afterItem = call(post("/api/v1/prj/tasks/{id}/deliverables", taskB), admin,
                json(deliverable("ITEM", itemId, "OUTPUT", true)));
        JsonNode afterCard = call(post("/api/v1/prj/tasks/{id}/deliverables", taskB), admin,
                json(deliverable("CAPP_CARD", cardId, "OUTPUT", true)));
        assertThat(afterCard.path("requiredTotal").asInt()).isEqualTo(3);
        assertThat(afterCard.path("unreleasedTotal").asInt()).isEqualTo(1);
        assertThat(afterCard.path("missingTotal").asInt()).isZero();
        assertThat(afterCard.path("ready").asBoolean()).isTrue();
        JsonNode itemRow = findItem(afterItem, taskB, "PN-P74-1");
        assertThat(itemRow.path("state").asText()).isEqualTo("UNRELEASED");
        assertThat(itemRow.path("ready").asBoolean()).isTrue();
        assertThat(findItem(afterCard, taskB, "CC-P74-1").path("state").asText()).isEqualTo("OK");

        // 4.1 同一文档挂成 INPUT（输入项不参与齐套）
        JsonNode afterInput = call(post("/api/v1/prj/tasks/{id}/deliverables", taskB), admin,
                json(deliverable("DOC", docId, "INPUT", true)));
        assertThat(afterInput.path("requiredTotal").asInt()).isEqualTo(3);
        assertThat(afterInput.path("missingTotal").asInt()).isZero();

        // 5. 文档被删除 → OUTPUT 缺件算 MISSING，项目不齐套；INPUT 缺件只是追溯断链，不计
        jdbcTemplate.update("UPDATE plm_object SET deleted = true WHERE id = ?", docId);
        JsonNode broken = call(get("/api/v1/prj/projects/{id}/deliverables", projectId), admin, null);
        assertThat(broken.path("ready").asBoolean()).isFalse();
        assertThat(broken.path("missingTotal").asInt()).isEqualTo(1);
        assertThat(findItem(broken, taskA, "DOC-P74-1").path("state").asText()).isEqualTo("MISSING");
        assertThat(findItem(broken, taskB, "DOC-P74-1").path("outputType").asText()).isEqualTo("INPUT");
        assertThat(findItem(broken, taskB, "DOC-P74-1").path("state").asText()).isEqualTo("MISSING");

        // 6. 未齐套时提交 A 被拦（409）
        mockMvc.perform(post("/api/v1/prj/tasks/{id}/submit", taskA)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "以为齐了"))))
                .andExpect(status().isConflict());

        // 7. 改成非必需 → 齐套口径放开，可以提交
        long outputId = findItem(broken, taskA, "DOC-P74-1").path("id").asLong();
        JsonNode afterUpdate = call(put("/api/v1/prj/deliverables/{id}", outputId), admin,
                json(Map.of("required", false)));
        assertThat(afterUpdate.path("requiredTotal").asInt()).isEqualTo(2);
        assertThat(afterUpdate.path("ready").asBoolean()).isTrue();
        call(post("/api/v1/prj/tasks/{id}/submit", taskA), admin, json(Map.of("note", "材料已归档")));

        // 8. B 含未发布物料也能提交（WARN 不拦）
        call(post("/api/v1/prj/tasks/{id}/submit", taskB), admin, json(Map.of("note", "草稿版先审")));

        // 9. 移除交付物后计数回落
        long cardOutputId = findItem(afterUpdate, taskB, "CC-P74-1").path("id").asLong();
        JsonNode afterRemove = call(delete("/api/v1/prj/deliverables/{id}", cardOutputId), admin, null);
        assertThat(afterRemove.path("requiredTotal").asInt()).isEqualTo(1);
        assertThat(findNames(afterRemove, taskB)).doesNotContain("CC-P74-1");

        // 10. 候选对象：只列最新版本，按编码模糊查
        JsonNode candidates = call(get("/api/v1/prj/deliverable-candidates?objectType=ITEM&keyword=PN-P74"),
                admin, null);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).path("code").asText()).isEqualTo("PN-P74-1");
        assertThat(candidates.get(0).path("objectType").asText()).isEqualTo("ITEM");
    }

    // ------------------------------------------------------------------ 工具

    private long insertPlmObject(String objectType, String code, String name, String lifecycleState) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                        lifecycle_state)
                VALUES (?, ?, ?, ?, ?, 1, ?)
                """, id, objectType, id, code, name, lifecycleState);
        return id;
    }

    private long insertCard(String code, String title) {
        long id = System.nanoTime();
        long objectId = insertPlmObject("PdmItem", "PN-CARD-" + code, title, "Released");
        jdbcTemplate.update("""
                INSERT INTO capp_card (id, company_id, code, template_id, template_code,
                                       template_version, template_name, routing_id, object_id,
                                       object_code, title)
                VALUES (?, 1, ?, 1, 'CT-000001', 1, '工序卡', 1, ?, 'PN-CARD', ?)
                """, id, code, objectId, title);
        return id;
    }

    private long createTask(String token, long projectId, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", "TASK");
        body.put("planStart", "2026-06-01");
        body.put("planEnd", "2026-06-10");
        body.put("progress", 0);
        JsonNode board = call(post("/api/v1/prj/projects/{id}/tasks", projectId), token, json(body));
        JsonNode tasks = board.path("tasks");
        return tasks.get(tasks.size() - 1).path("id").asLong();
    }

    private Map<String, Object> deliverable(String objectType, long objectId, String outputType,
                                            boolean required) {
        Map<String, Object> body = new HashMap<>();
        body.put("objectType", objectType);
        body.put("objectId", objectId);
        body.put("outputType", outputType);
        body.put("required", required);
        return body;
    }

    private JsonNode findItem(JsonNode board, long taskId, String code) {
        for (JsonNode task : board.path("tasks")) {
            if (task.path("taskId").asLong() != taskId) {
                continue;
            }
            for (JsonNode item : task.path("items")) {
                if (code.equals(item.path("code").asText())) {
                    return item;
                }
            }
        }
        throw new AssertionError("未找到交付物：" + code);
    }

    private java.util.List<String> findNames(JsonNode board, long taskId) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (JsonNode task : board.path("tasks")) {
            if (task.path("taskId").asLong() == taskId) {
                task.path("items").forEach(item -> names.add(item.path("code").asText()));
            }
        }
        return names;
    }

    private long createProject(String token, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", "RD");
        body.put("priority", 2);
        return call(post("/api/v1/prj/projects"), token, json(body)).path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload) throws Exception {
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
