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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7-8 项目门户三视图冒烟：我的项目 / 我管理的项目（进度 vs 时间进度、逾期任务、待审工时）/ 公司组合。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectPortalSmokeTest {

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
    void portalThreeViews() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 两个项目：P1 计划已在昨天到期（时间进度 100%，实际进度低 → 落后），P2 跨到明年
        long p1 = createProject(admin, "门户测试-落后项目", "2026-09-01", "2026-11-05");
        long p2 = createProject(admin, "门户测试-正常项目", "2026-11-01", "2027-01-31");

        // 2. P1 两个任务：一个逾期未完成（工期长）、一个已完成（工期短）→ 汇总进度仍很低
        long t1 = createTask(admin, p1, "逾期任务", "2026-09-01", "2026-10-15", 0, "NOT_STARTED");
        createTask(admin, p1, "已完成任务", "2026-10-14", "2026-11-05", 100, "DONE");
        createTask(admin, p2, "正常任务", "2026-11-01", "2026-11-30", 0, "NOT_STARTED");

        // 3. 工时：提交一条待审（P1 的待审工时数应为 1）
        LocalDate yesterday = LocalDate.now().minusDays(1);
        JsonNode sheet = call(post("/api/v1/prj/timesheets"), admin, json(Map.of(
                "taskId", t1, "workDate", yesterday.toString(), "hours", 8, "workType", "NORMAL")));
        call(post("/api/v1/prj/timesheets/{id}/submit", sheet.path("id").asLong()), admin, null);

        // 4. 我管理的项目
        JsonNode managed = call(get("/api/v1/prj/portal/managed-projects"), admin, null);
        assertThat(managed).hasSize(2);
        JsonNode row = findBy(managed, "code", "PJ-");
        JsonNode p1Row = findById(managed, p1);
        assertThat(p1Row.path("taskCount").asInt()).isEqualTo(2);
        assertThat(p1Row.path("doneTaskCount").asInt()).isEqualTo(1);
        assertThat(p1Row.path("overdueTaskCount").asInt()).isEqualTo(1);
        assertThat(p1Row.path("pendingTimesheetCount").asInt()).isEqualTo(1);
        assertThat(p1Row.path("timeProgress").isNumber()).isTrue();
        assertThat(p1Row.path("deviation").asInt()).isLessThan(0);
        assertThat(row).isNotNull();

        // 5. 公司组合：总数 2、有逾期项目、风险清单包含 P1
        JsonNode portfolio = call(get("/api/v1/prj/portal/portfolio"), admin, null);
        assertThat(portfolio.path("totalProjects").asInt()).isEqualTo(2);
        assertThat(portfolio.path("activeProjects").asInt()).isEqualTo(2);
        assertThat(portfolio.path("overdueProjects").asInt()).isEqualTo(1);
        assertThat(portfolio.path("byStatus").isArray()).isTrue();
        assertThat(portfolio.path("risks")).isNotEmpty();
        boolean containsP1 = false;
        for (JsonNode risk : portfolio.path("risks")) {
            if (risk.path("id").asLong() == p1) {
                containsP1 = true;
                assertThat(risk.path("overdueTaskCount").asInt()).isEqualTo(1);
            }
        }
        assertThat(containsP1).isTrue();

        // 6. 我的项目：管理员是这两个项目的经理
        JsonNode mine = call(get("/api/v1/prj/portal/my-projects"), admin, null);
        assertThat(mine).hasSize(2);
        JsonNode mineP1 = findById(mine, p1);
        assertThat(mineP1.path("manager").asBoolean()).isTrue();
        assertThat(mineP1.path("status").asText()).isEqualTo("DRAFT");
    }

    // ------------------------------------------------------------------ 工具

    private JsonNode findById(JsonNode array, long id) {
        for (JsonNode node : array) {
            if (node.path("id").asLong() == id) {
                return node;
            }
        }
        return null;
    }

    private JsonNode findBy(JsonNode array, String field, String prefix) {
        for (JsonNode node : array) {
            if (node.path(field).asText().startsWith(prefix)) {
                return node;
            }
        }
        return null;
    }

    private long createProject(String token, String name, String planStart, String planEnd)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", "RD");
        body.put("priority", 3);
        body.put("planStart", planStart);
        body.put("planEnd", planEnd);
        return call(post("/api/v1/prj/projects"), token, json(body)).path("id").asLong();
    }

    private long createTask(String token, long projectId, String name, String planStart,
                            String planEnd, int progress, String status) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", "TASK");
        body.put("planStart", planStart);
        body.put("planEnd", planEnd);
        body.put("progress", progress);
        body.put("status", status);
        JsonNode board = call(post("/api/v1/prj/projects/{id}/tasks", projectId), token, json(body));
        for (JsonNode node : board.path("tasks")) {
            if (name.equals(node.path("name").asText())) {
                return node.path("id").asLong();
            }
        }
        throw new IllegalStateException("任务未创建：" + name);
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
