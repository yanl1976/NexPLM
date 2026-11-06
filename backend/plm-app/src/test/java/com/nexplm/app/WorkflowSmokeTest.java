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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工作流冒烟测试：流程自动部署、发起、初审 → 会签（全票）、驳回回退、发起人撤回。
 *
 * <p>流程模型见 {@code plm-platform/src/main/resources/processes/nexplm-approval.bpmn20.xml}：
 * 发起 → 初审（单人）→ 会签（多实例，全部签核才通过）→ 归档。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorkflowSmokeTest {

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
    void countersignPassesOnlyWhenAllApprove() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long zhangId = createUser(admin, "wf_zhang", "张三");
        long liId = createUser(admin, "wf_li", "李四");
        String zhang = login("wf_zhang", "Passw0rd!");
        String li = login("wf_li", "Passw0rd!");

        long itemId = createItem(admin, "审批零件");

        // 1. 发起：初审 admin，会签 admin + 张三 + 李四
        JsonNode started = start(admin, itemId, adminId, List.of(adminId, zhangId, liId));
        String pid = started.path("processInstanceId").asText();
        assertThat(pid).isNotBlank();
        assertThat(started.path("processKey").asText()).isEqualTo("nexplmApproval");
        JsonNode initialTasks = started.path("activeTasks");
        assertThat(initialTasks.size()).isEqualTo(1);
        assertThat(initialTasks.get(0).path("taskKey").asText()).isEqualTo("initialReview");
        assertThat(initialTasks.get(0).path("assignee").asText()).isEqualTo(String.valueOf(adminId));

        // 2. 待办能带出业务对象上下文
        JsonNode adminTodos = call(get("/api/v1/workflow/tasks?page=1&size=50"), admin, null);
        JsonNode myInitial = findTaskByPid(adminTodos.path("items"), pid);
        assertThat(myInitial).isNotNull();
        assertThat(myInitial.path("objectCode").asText()).isNotBlank();
        assertThat(myInitial.path("objectType").asText()).isEqualTo("PdmItem");

        // 3. 初审通过 → 生成 3 个会签任务
        call(post("/api/v1/workflow/tasks/{taskId}/complete", myInitial.path("taskId").asText()),
                admin, json(Map.of("comment", "初审通过")));
        JsonNode afterInitial = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        JsonNode countersignTasks = afterInitial.path("activeTasks");
        assertThat(countersignTasks.size()).isEqualTo(3);
        assertThat(countersignTasks.findValuesAsText("taskKey")).containsOnly("countersign");

        // 4. 会签未全票前流程不结束：完成 2 个仍在运行
        completeTaskOf(admin, pid, "admin");
        completeTaskOf(zhang, pid, "zhang");
        JsonNode beforeLast = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(beforeLast.path("status").asText()).isEqualTo("RUNNING");
        assertThat(beforeLast.path("activeTasks").size()).isEqualTo(1);

        // 5. 最后一票 → 归档，实例结束
        completeTaskOf(li, pid, "li");
        JsonNode finished = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(finished.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(finished.path("activeTasks").size()).isZero();
        assertThat(finished.path("comments").findValuesAsText("action"))
                .contains("START", "APPROVE");

        // 6. 已结束的实例再次办理 → 404
        mockMvc.perform(post("/api/v1/workflow/tasks/{taskId}/complete",
                        countersignTasks.get(0).path("taskId").asText())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "重复办理"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectRollsBackAndWithdrawTerminates() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long zhangId = createUser(admin, "wf_reject_a", "驳回甲");
        long liId = createUser(admin, "wf_reject_b", "驳回乙");
        long itemId = createItem(admin, "驳回零件");

        // 1. 发起并初审通过，进入会签
        JsonNode started = start(admin, itemId, adminId, List.of(adminId, zhangId, liId));
        String pid = started.path("processInstanceId").asText();
        String initialTaskId = started.path("activeTasks").get(0).path("taskId").asText();
        call(post("/api/v1/workflow/tasks/{taskId}/complete", initialTaskId), admin,
                json(Map.of("comment", "初审通过")));

        JsonNode countersign = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(countersign.path("activeTasks").size()).isEqualTo(3);

        // 2. 驳回：把自己名下的会签任务退回「初审」
        String adminCountersignTask = findTaskOfAssignee(countersign.path("activeTasks"), adminId)
                .path("taskId").asText();
        call(post("/api/v1/workflow/tasks/{taskId}/reject", adminCountersignTask), admin,
                json(Map.of("targetActivityId", "initialReview", "comment", "资料不全，退回补充")));

        JsonNode rejected = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(rejected.path("activeTasks").size()).isEqualTo(1);
        assertThat(rejected.path("activeTasks").get(0).path("taskKey").asText()).isEqualTo("initialReview");
        assertThat(rejected.path("comments").findValuesAsText("action")).contains("REJECT");

        // 3. 驳回不存在的节点 → 400
        String reworkTaskId = rejected.path("activeTasks").get(0).path("taskId").asText();
        mockMvc.perform(post("/api/v1/workflow/tasks/{taskId}/reject", reworkTaskId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("targetActivityId", "notExistsNode", "comment", "非法节点"))))
                .andExpect(status().isBadRequest());

        // 4. 撤回：发起人可撤回，撤回后实例终止且不可重复撤回
        call(post("/api/v1/workflow/instances/{id}/withdraw", pid), admin,
                json(Map.of("reason", "需求取消")));
        JsonNode withdrawn = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(withdrawn.path("status").asText()).isEqualTo("TERMINATED");
        assertThat(withdrawn.path("activeTasks").size()).isZero();

        mockMvc.perform(post("/api/v1/workflow/instances/{id}/withdraw", pid)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "再次撤回"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskOfOtherUserCannotBeCompleted() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long otherId = createUser(admin, "wf_other", "他人");
        String other = login("wf_other", "Passw0rd!");
        long itemId = createItem(admin, "越权零件");

        JsonNode started = start(admin, itemId, adminId, List.of(otherId));
        String taskId = started.path("activeTasks").get(0).path("taskId").asText();

        // 非任务办理人办理 → 403
        mockMvc.perform(post("/api/v1/workflow/tasks/{taskId}/complete", taskId)
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "越权办理"))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ 工具

    private JsonNode start(String token, long itemId, long approverId, List<Long> approverIds) throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("objectType", "PdmItem");
        req.put("objectId", itemId);
        req.put("approverId", approverId);
        req.put("approverIds", approverIds);
        req.put("remark", "冒烟测试发起");
        return call(post("/api/v1/workflow/instances"), token, json(req));
    }

    /** 按用户在当前实例下的活动任务完成（会签多任务场景按 assignee 定位）。 */
    private void completeTaskOf(String token, String pid, String who) throws Exception {
        long userId = currentUserId(token);
        JsonNode detail = call(get("/api/v1/workflow/instances/{id}", pid), token, null);
        JsonNode task = findTaskOfAssignee(detail.path("activeTasks"), userId);
        assertThat(task).as("用户 %s 在实例 %s 下应有待办", who, pid).isNotNull();
        call(post("/api/v1/workflow/tasks/{taskId}/complete", task.path("taskId").asText()), token,
                json(Map.of("comment", who + " 同意")));
    }

    private JsonNode findTaskOfAssignee(JsonNode tasks, long userId) {
        for (JsonNode task : tasks) {
            if (String.valueOf(userId).equals(task.path("assignee").asText())) {
                return task;
            }
        }
        return null;
    }

    private JsonNode findTaskByPid(JsonNode tasks, String pid) {
        for (JsonNode task : tasks) {
            if (pid.equals(task.path("processInstanceId").asText())) {
                return task;
            }
        }
        return null;
    }

    private long createUser(String adminToken, String username, String displayName) throws Exception {
        call(post("/api/v1/system/users"), adminToken, json(Map.of(
                "username", username,
                "password", "Passw0rd!",
                "displayName", displayName,
                "securityLevel", 1,
                "roleIds", new long[]{businessRoleId(adminToken)})));
        JsonNode users = call(get("/api/v1/system/users?page=1&size=100"), adminToken, null).path("items");
        for (JsonNode user : users) {
            if (username.equals(user.path("username").asText())) {
                return user.path("id").asLong();
            }
        }
        throw new IllegalStateException("用户创建失败：" + username);
    }

    private long businessRoleId(String adminToken) throws Exception {
        JsonNode roles = call(get("/api/v1/system/roles"), adminToken, null);
        for (JsonNode role : roles) {
            if ("BUSINESS".equals(role.path("code").asText())) {
                return role.path("id").asLong();
            }
        }
        throw new IllegalStateException("内置 BUSINESS 角色不存在");
    }

    private long createItem(String token, String name) throws Exception {
        JsonNode created = call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA")));
        return created.path("id").asLong();
    }

    private long currentUserId(String token) throws Exception {
        return call(get("/api/v1/auth/me"), token, null).path("id").asLong();
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
        return body(result).path("data");
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
