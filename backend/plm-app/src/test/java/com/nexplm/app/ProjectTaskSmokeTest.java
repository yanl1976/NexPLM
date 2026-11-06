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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7-2 WBS 任务与依赖冒烟：层级编码 + 项目进度汇总 + 依赖防环 + 编码重排。
 *
 * <p>规则：WBS 编码服务端生成；项目进度=叶子任务按工期加权；有子任务不可删；
 * 依赖重复/成环 400；重排用两阶段写，避免互换编码撞唯一索引。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectTaskSmokeTest {

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
    void wbsTreeProgressRollupAndDependency() throws Exception {
        String admin = login("admin", "Admin@12345");
        long projectId = createProject(admin, "WBS 任务测试项目");

        // 1. 两条一级任务 + 一条二级任务，WBS 编码 1 / 2 / 2.1
        JsonNode afterFirst = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin,
                json(task("需求分析", null, "2026-01-01", "2026-01-10", 100)));
        JsonNode first = afterFirst.path("tasks").get(0);
        long firstId = first.path("id").asLong();
        assertThat(first.path("wbsCode").asText()).isEqualTo("1");
        assertThat(first.path("status").asText()).isEqualTo("DONE");
        assertThat(first.path("durationDays").asInt()).isEqualTo(10);

        JsonNode afterSecond = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin,
                json(task("设计", null, "2026-01-11", "2026-01-31", 0)));
        long secondId = afterSecond.path("tasks").get(1).path("id").asLong();
        assertThat(afterSecond.path("tasks").get(1).path("wbsCode").asText()).isEqualTo("2");

        Map<String, Object> childBody = task("结构设计", secondId, "2026-01-11", "2026-01-20", 50);
        JsonNode afterChild = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin, json(childBody));
        JsonNode parentNode = afterChild.path("tasks").get(1);
        assertThat(parentNode.path("children")).hasSize(1);
        JsonNode child = parentNode.path("children").get(0);
        long childId = child.path("id").asLong();
        assertThat(child.path("wbsCode").asText()).isEqualTo("2.1");
        assertThat(child.path("status").asText()).isEqualTo("IN_PROGRESS");

        // 2. 进度汇总：叶子 = 需求分析(10 天/100%) + 结构设计(10 天/50%) → 75%
        assertThat(afterChild.path("projectProgress").asInt()).isEqualTo(75);
        assertThat(call(get("/api/v1/prj/projects/{id}", projectId), admin, null).path("progress").asInt())
                .isEqualTo(75);

        // 3. 子任务做完 → 项目 100%
        Map<String, Object> childDone = task("结构设计", secondId, "2026-01-11", "2026-01-20", 100);
        childDone.put("status", "DONE");
        JsonNode afterDone = call(put("/api/v1/prj/tasks/{id}", childId), admin, json(childDone));
        assertThat(afterDone.path("projectProgress").asInt()).isEqualTo(100);

        // 4. 校验：计划日期反了 400、进度越界 400、未知类型 400
        Map<String, Object> badDate = task("日期反了", null, "2026-02-10", "2026-02-01", 0);
        mockMvc.perform(post("/api/v1/prj/projects/{id}/tasks", projectId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badDate)))
                .andExpect(status().isBadRequest());
        Map<String, Object> badProgress = task("进度越界", null, null, null, 150);
        mockMvc.perform(post("/api/v1/prj/projects/{id}/tasks", projectId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badProgress)))
                .andExpect(status().isBadRequest());
        Map<String, Object> badType = task("未知类型", null, null, null, 0);
        badType.put("type", "FLYING");
        mockMvc.perform(post("/api/v1/prj/projects/{id}/tasks", projectId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badType)))
                .andExpect(status().isBadRequest());

        // 5. 依赖：需求分析 → 结构设计（FS）；反向再加会成环 → 400；重复添加 → 400
        JsonNode withDep = call(post("/api/v1/prj/projects/{id}/task-dependencies", projectId), admin,
                json(Map.of("predTaskId", firstId, "succTaskId", childId, "depType", "FS", "lagDays", 2)));
        assertThat(withDep.path("dependencies")).hasSize(1);
        long depId = withDep.path("dependencies").get(0).path("id").asLong();
        assertThat(withDep.path("dependencies").get(0).path("predName").asText()).contains("需求分析");

        mockMvc.perform(post("/api/v1/prj/projects/{id}/task-dependencies", projectId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("predTaskId", childId, "succTaskId", firstId))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/prj/projects/{id}/task-dependencies", projectId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("predTaskId", firstId, "succTaskId", childId))))
                .andExpect(status().isBadRequest());

        // 6. 编码重排：插一条排序号为 1 的任务，重排后它排到第一位
        Map<String, Object> early = task("立项评审", null, "2025-12-20", "2025-12-25", 100);
        early.put("sortNo", 1);
        early.put("type", "MILESTONE");
        JsonNode afterEarly = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin, json(early));
        assertThat(afterEarly.path("tasks")).hasSize(3);
        long earlyId = afterEarly.path("tasks").get(2).path("id").asLong();

        JsonNode resequenced = call(post("/api/v1/prj/projects/{id}/tasks/resequence", projectId), admin, null);
        JsonNode tasks = resequenced.path("tasks");
        assertThat(tasks.get(0).path("name").asText()).isEqualTo("立项评审");
        assertThat(tasks.get(0).path("wbsCode").asText()).isEqualTo("1");
        assertThat(tasks.get(1).path("wbsCode").asText()).isEqualTo("2");
        assertThat(tasks.get(2).path("wbsCode").asText()).isEqualTo("3");
        assertThat(tasks.get(2).path("children").get(0).path("wbsCode").asText()).isEqualTo("3.1");

        // 7. 删除守卫：有子任务的不能删；删叶子连带清掉它的依赖
        mockMvc.perform(delete("/api/v1/prj/tasks/{id}", secondId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        JsonNode afterDelete = call(delete("/api/v1/prj/tasks/{id}", firstId), admin, null);
        assertThat(afterDelete.path("dependencies")).isEmpty();
        assertThat(afterDelete.path("tasks")).hasSize(2);

        // 8. 依赖删除接口（上一步删任务时已连带清掉原依赖，这里新建一条再删）
        JsonNode freshDep = call(post("/api/v1/prj/projects/{id}/task-dependencies", projectId), admin,
                json(Map.of("predTaskId", earlyId, "succTaskId", secondId)));
        long freshDepId = freshDep.path("dependencies").get(0).path("id").asLong();
        JsonNode afterDepRemoved = call(delete("/api/v1/prj/task-dependencies/{id}", freshDepId), admin, null);
        assertThat(afterDepRemoved.path("dependencies")).isEmpty();
        assertThat(depId).isPositive();
    }

    /** P7-3 任务执行闭环：派工 → 接收 → 开始 → 提交 → 打回 → 再提交 → 通过；我的任务按未完成过滤。 */
    @Test
    void taskExecutionLoop() throws Exception {
        String admin = login("admin", "Admin@12345");
        long userId = call(get("/api/v1/auth/me"), admin, null).path("id").asLong();
        long projectId = createProject(admin, "执行闭环测试项目");

        JsonNode board = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin,
                json(task("编制工艺", null, "2026-05-01", "2026-05-10", 0)));
        long taskId = board.path("tasks").get(0).path("id").asLong();
        assertThat(board.path("tasks").get(0).path("status").asText()).isEqualTo("NOT_STARTED");

        // 创建时执行人默认落到创建人，派工只是「改派」
        assertThat(board.path("tasks").get(0).path("ownerId").asLong()).isEqualTo(userId);

        // 派工 → 接收 → 开始
        JsonNode assigned = call(post("/api/v1/prj/tasks/{id}/assign", taskId), admin,
                json(Map.of("ownerId", userId)));
        JsonNode assignedTask = assigned.path("tasks").get(0);
        assertThat(assignedTask.path("ownerName").asText()).isNotBlank();
        assertThat(assignedTask.path("assignedAt").asText()).isNotBlank();

        JsonNode accepted = call(post("/api/v1/prj/tasks/{id}/accept", taskId), admin, null);
        assertThat(accepted.path("tasks").get(0).path("acceptedAt").asText()).isNotBlank();

        JsonNode started = call(post("/api/v1/prj/tasks/{id}/start", taskId), admin, null);
        assertThat(started.path("tasks").get(0).path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(started.path("tasks").get(0).path("actualStart").asText()).isNotBlank();

        // 提交 → 打回（记录原因）→ 再提交 → 通过
        JsonNode submitted = call(post("/api/v1/prj/tasks/{id}/submit", taskId), admin,
                json(Map.of("note", "工艺已编制完成，含 5 道工序")));
        assertThat(submitted.path("tasks").get(0).path("status").asText()).isEqualTo("SUBMITTED");
        assertThat(submitted.path("tasks").get(0).path("submitNote").asText()).contains("5 道工序");

        JsonNode rejected = call(post("/api/v1/prj/tasks/{id}/reject", taskId), admin,
                json(Map.of("reason", "缺少热处理工序")));
        JsonNode rejectedTask = rejected.path("tasks").get(0);
        assertThat(rejectedTask.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(rejectedTask.path("rejectReason").asText()).isEqualTo("缺少热处理工序");

        call(post("/api/v1/prj/tasks/{id}/submit", taskId), admin, json(Map.of("note", "已补热处理")));
        JsonNode approved = call(post("/api/v1/prj/tasks/{id}/approve", taskId), admin, null);
        JsonNode approvedTask = approved.path("tasks").get(0);
        assertThat(approvedTask.path("status").asText()).isEqualTo("DONE");
        assertThat(approvedTask.path("progress").asInt()).isEqualTo(100);
        assertThat(approvedTask.path("approvedAt").asText()).isNotBlank();
        assertThat(approvedTask.path("actualEnd").asText()).isNotBlank();
        assertThat(approved.path("projectProgress").asInt()).isEqualTo(100);

        // 重复通过 / 对未开始的剧情报错
        mockMvc.perform(post("/api/v1/prj/tasks/{id}/approve", taskId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        JsonNode second = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin,
                json(task("编写作业指导书", null, "2026-05-11", "2026-05-20", 0)));
        long secondId = second.path("tasks").get(1).path("id").asLong();
        mockMvc.perform(post("/api/v1/prj/tasks/{id}/submit", secondId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "还没开始就提交"))))
                .andExpect(status().isBadRequest());

        // 我的任务：默认只查未完成（已通过的「编制工艺」不应出现）
        call(post("/api/v1/prj/tasks/{id}/assign", secondId), admin, json(Map.of("ownerId", userId)));
        JsonNode myTasks = call(get("/api/v1/prj/my-tasks"), admin, null);
        assertThat(myTasks.findValuesAsText("name")).contains("编写作业指导书").doesNotContain("编制工艺");
        assertThat(call(get("/api/v1/prj/my-tasks?status=DONE"), admin, null)
                .findValuesAsText("name")).contains("编制工艺");
    }

    // ------------------------------------------------------------------ 工具

    private Map<String, Object> task(String name, Long parentId, String planStart, String planEnd,
                                     int progress) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("parentId", parentId);
        body.put("type", "TASK");
        body.put("planStart", planStart);
        body.put("planEnd", planEnd);
        body.put("progress", progress);
        return body;
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
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
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
