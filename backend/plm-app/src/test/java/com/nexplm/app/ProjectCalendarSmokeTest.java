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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7-7 日历与拖拽排期冒烟。
 *
 * <p>口径：日历只返回与区间相交的已排期任务；拖拽改期按 planStart 差值级联平移子树，
 * 不动祖先区间、不动依赖。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectCalendarSmokeTest {

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
    void calendarFilterAndDragReschedule() throws Exception {
        String admin = login("admin", "Admin@12345");
        long projectId = createProject(admin);

        // 1. 父任务 A(12-01~12-10) + 子任务 B(12-02~12-05) + 独立任务 C(12-20~12-22)
        long taskA = createTask(admin, projectId, null, "总体设计", "2026-12-01", "2026-12-10");
        long taskB = createTask(admin, projectId, taskA, "结构设计", "2026-12-02", "2026-12-05");
        createTask(admin, projectId, null, "验收评审", "2026-12-20", "2026-12-22");
        // 未排期任务不进日历
        createTask(admin, projectId, null, "待排期任务", null, null);

        // 2. 整月 3 条
        JsonNode whole = call(get("/api/v1/prj/projects/{id}/calendar?from=2026-12-01&to=2026-12-31",
                projectId), admin, null);
        assertThat(whole.path("tasks")).hasSize(3);
        assertThat(whole.path("from").asText()).isEqualTo("2026-12-01");

        // 3. 小区间只返回相交任务（C 不相交）
        JsonNode small = call(get("/api/v1/prj/projects/{id}/calendar?from=2026-12-03&to=2026-12-04",
                projectId), admin, null);
        assertThat(small.path("tasks")).hasSize(2);

        // 4. 拖 B 后移 5 天 → B 12-07~12-10；父任务 A 不动（不改祖先）
        JsonNode afterB = call(post("/api/v1/prj/tasks/{id}/reschedule", taskB), admin,
                json(Map.of("planStart", "2026-12-07", "planEnd", "2026-12-10")));
        JsonNode nodeA = afterB.path("tasks").get(0);
        assertThat(nodeA.path("id").asLong()).isEqualTo(taskA);
        assertThat(nodeA.path("planStart").asText()).isEqualTo("2026-12-01");
        assertThat(nodeA.path("planEnd").asText()).isEqualTo("2026-12-10");
        JsonNode nodeB = nodeA.path("children").get(0);
        assertThat(nodeB.path("planStart").asText()).isEqualTo("2026-12-07");
        assertThat(nodeB.path("planEnd").asText()).isEqualTo("2026-12-10");
        assertThat(nodeB.path("durationDays").asInt()).isEqualTo(4);

        // 5. 拖父任务 A 后移 3 天 → A 12-04~12-13，子任务 B 级联 +3 → 12-10~12-13
        JsonNode afterA = call(post("/api/v1/prj/tasks/{id}/reschedule", taskA), admin,
                json(Map.of("planStart", "2026-12-04", "planEnd", "2026-12-13")));
        JsonNode parent = afterA.path("tasks").get(0);
        assertThat(parent.path("planStart").asText()).isEqualTo("2026-12-04");
        assertThat(parent.path("planEnd").asText()).isEqualTo("2026-12-13");
        JsonNode child = parent.path("children").get(0);
        assertThat(child.path("planStart").asText()).isEqualTo("2026-12-10");
        assertThat(child.path("planEnd").asText()).isEqualTo("2026-12-13");

        // 6. 结束早于开始 400
        mockMvc.perform(post("/api/v1/prj/tasks/{id}/reschedule", taskA)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("planStart", "2026-12-10", "planEnd", "2026-12-01"))))
                .andExpect(status().isBadRequest());

        // 7. 拉长不移动：delta=0 时不平移子树
        JsonNode stretched = call(post("/api/v1/prj/tasks/{id}/reschedule", taskA), admin,
                json(Map.of("planStart", "2026-12-04", "planEnd", "2026-12-20")));
        assertThat(stretched.path("tasks").get(0).path("planEnd").asText()).isEqualTo("2026-12-20");
        assertThat(stretched.path("tasks").get(0).path("children").get(0).path("planEnd").asText())
                .isEqualTo("2026-12-13");
    }

    // ------------------------------------------------------------------ 工具

    private long createProject(String token) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "日历排期测试项目");
        body.put("type", "RD");
        body.put("priority", 3);
        return call(post("/api/v1/prj/projects"), token, json(body)).path("id").asLong();
    }

    private long createTask(String token, long projectId, Long parentId, String name,
                            String planStart, String planEnd) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("parentId", parentId);
        body.put("type", "TASK");
        body.put("planStart", planStart);
        body.put("planEnd", planEnd);
        body.put("progress", 0);
        JsonNode board = call(post("/api/v1/prj/projects/{id}/tasks", projectId), token, json(body));
        return lastTaskId(board.path("tasks"), name);
    }

    /** 在任务树里按名称找 id（新建任务总是被追加为最后一个兄弟）。 */
    private long lastTaskId(JsonNode nodes, String name) {
        for (JsonNode node : nodes) {
            if (name.equals(node.path("name").asText()) && node.path("children").isEmpty()) {
                return node.path("id").asLong();
            }
            long found = lastTaskId(node.path("children"), name);
            if (found > 0) {
                return found;
            }
        }
        return 0;
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
