package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7-6 项目模板冒烟：模板骨架 → 编码生成 → 从模板立项（偏移换日期 / 目录物化路径）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectTemplateSmokeTest {

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

    @Test
    void templateTasksFoldersAndInstantiate() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 建模板（PT-000001）
        JsonNode created = call(post("/api/v1/prj/templates"), admin,
                json(template("研发标准模板", "RD")));
        long templateId = created.path("id").asLong();
        assertThat(created.path("code").asText()).isEqualTo("PT-000001");

        // 2. 任务骨架：2 条一级 + 1 条二级（编码按数组顺序生成）
        List<Map<String, Object>> tasks = List.of(
                taskRow(null, "需求分析", 0, 5),
                taskRow(null, "方案设计", 5, 5),
                taskRow("2", "结构设计", 5, 3));
        JsonNode afterTasks = call(put("/api/v1/prj/templates/{id}/tasks", templateId), admin,
                json(Map.of("tasks", tasks)));
        List<String> codes = new ArrayList<>();
        afterTasks.path("tasks").forEach(node -> codes.add(node.path("wbsCode").asText()));
        assertThat(codes).containsExactly("1", "2", "2.1");
        JsonNode child = afterTasks.path("tasks").get(2);
        assertThat(child.path("startOffset").asInt()).isEqualTo(5);
        assertThat(child.path("durationDays").asInt()).isEqualTo(3);
        assertThat(child.path("parentId").asLong())
                .isEqualTo(afterTasks.path("tasks").get(1).path("id").asLong());

        // 3. 父编码不存在 400
        mockMvc.perform(put("/api/v1/prj/templates/{id}/tasks", templateId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tasks", List.of(taskRow("9", "孤儿任务", 0, 1))))))
                .andExpect(status().isBadRequest());

        // 4. 目录骨架：01-设计文档 / 01-设计文档/图纸 / 02-评审记录
        List<Map<String, Object>> folders = List.of(
                folder("/", "01-设计文档"),
                folder("/01-设计文档/", "图纸"),
                folder("/", "02-评审记录"));
        JsonNode afterFolders = call(put("/api/v1/prj/templates/{id}/folders", templateId), admin,
                json(Map.of("folders", folders)));
        assertThat(afterFolders.path("folders")).hasSize(3);

        // 5. 模板列表带数量
        JsonNode page = call(get("/api/v1/prj/templates?page=1&size=10"), admin, null);
        assertThat(page.path("items").get(0).path("taskCount").asInt()).isEqualTo(3);
        assertThat(page.path("items").get(0).path("folderCount").asInt()).isEqualTo(3);

        // 6. 从模板立项：偏移换真实日期（2026-12-01 开工）
        JsonNode project = call(post("/api/v1/prj/projects/from-template"), admin, json(Map.of(
                "templateId", templateId,
                "name", "模板立项测试项目",
                "planStart", "2026-12-01",
                "priority", 2)));
        long projectId = project.path("id").asLong();
        assertThat(project.path("code").asText()).startsWith("PJ-");
        assertThat(project.path("planStart").asText()).isEqualTo("2026-12-01");
        // 最长链：offset 5 + 工期 5 → 12-10
        assertThat(project.path("planEnd").asText()).isEqualTo("2026-12-10");

        JsonNode board = call(get("/api/v1/prj/projects/{id}/tasks", projectId), admin, null);
        JsonNode rootTasks = board.path("tasks");
        assertThat(rootTasks).hasSize(2);
        assertThat(rootTasks.get(0).path("planStart").asText()).isEqualTo("2026-12-01");
        assertThat(rootTasks.get(0).path("planEnd").asText()).isEqualTo("2026-12-05");
        assertThat(rootTasks.get(1).path("planStart").asText()).isEqualTo("2026-12-06");
        assertThat(rootTasks.get(1).path("planEnd").asText()).isEqualTo("2026-12-10");
        assertThat(rootTasks.get(1).path("children")).hasSize(1);
        JsonNode sub = rootTasks.get(1).path("children").get(0);
        assertThat(sub.path("wbsCode").asText()).isEqualTo("2.1");
        assertThat(sub.path("planStart").asText()).isEqualTo("2026-12-06");
        assertThat(sub.path("planEnd").asText()).isEqualTo("2026-12-08");
        assertThat(sub.path("status").asText()).isEqualTo("NOT_STARTED");

        // 7. 目录物化路径：图纸挂在 01-设计文档 下
        List<Map<String, Object>> folderRows = jdbcRows(projectId);
        assertThat(folderRows).hasSize(3);
        Map<String, Object> design = folderRows.stream()
                .filter(row -> "01-设计文档".equals(row.get("name"))).findFirst().orElseThrow();
        Map<String, Object> drawing = folderRows.stream()
                .filter(row -> "图纸".equals(row.get("name"))).findFirst().orElseThrow();
        assertThat(drawing.get("parent_id")).isEqualTo(design.get("id"));
        assertThat(String.valueOf(drawing.get("path"))).startsWith(String.valueOf(design.get("path")));

        // 8. 停用模板后不能再立项
        call(put("/api/v1/prj/templates/{id}", templateId), admin,
                json(template("研发标准模板", "RD", false)));
        mockMvc.perform(post("/api/v1/prj/projects/from-template")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("templateId", templateId, "name", "停用后立项",
                                "planStart", "2026-12-01"))))
                .andExpect(status().isBadRequest());

        // 9. 删除模板后详情 404
        call(delete("/api/v1/prj/templates/{id}", templateId), admin, null);
        mockMvc.perform(get("/api/v1/prj/templates/{id}", templateId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ 工具

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private List<Map<String, Object>> jdbcRows(long projectId) {
        return jdbcTemplate.queryForList("""
                SELECT id, parent_id, path, name FROM prj_project_folder
                 WHERE project_id = ? AND deleted = false ORDER BY sort_no, id
                """, projectId);
    }

    private Map<String, Object> template(String name, String type) {
        return template(name, type, true);
    }

    private Map<String, Object> template(String name, String type, boolean enabled) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("enabled", enabled);
        return body;
    }

    private Map<String, Object> taskRow(String parentWbsCode, String name, int startOffset,
                                        int durationDays) {
        Map<String, Object> body = new HashMap<>();
        body.put("parentWbsCode", parentWbsCode);
        body.put("name", name);
        body.put("type", "TASK");
        body.put("startOffset", startOffset);
        body.put("durationDays", durationDays);
        return body;
    }

    private Map<String, Object> folder(String parentPath, String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("parentPath", parentPath);
        body.put("name", name);
        return body;
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
