package com.nexplm.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.platform.workflow.service.plugin.WfBusinessPlugin;
import com.nexplm.platform.workflow.service.plugin.WfPluginContext;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 选人检索 + 已走过节点：前端「加签选人」与「驳回选节点」依赖这两个接口。
     * <p>关键点：选人走 {@code wf.task.read} 门槛，普通业务用户（非管理员）也能用。</p>
     */
    @Test
    void actorUsersAndActivitiesFeedRejectPicker() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long pickerId = createUser(admin, "wf_picker", "选人可用");
        String picker = login("wf_picker", "Passw0rd!");

        // 1. 非管理员也能选人（对比 /system/users 需要 system.user.manage）
        JsonNode users = call(get("/api/v1/workflow/actors/users").param("keyword", "wf_picker"), picker, null);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).path("username").asText()).isEqualTo("wf_picker");
        assertThat(users.get(0).path("id").asLong()).isEqualTo(pickerId);

        // 2. 发起并初审通过，历史节点应含初审与会签
        long itemId = createItem(admin, "选人零件");
        JsonNode started = start(admin, itemId, adminId, List.of(adminId, pickerId));
        String pid = started.path("processInstanceId").asText();
        call(post("/api/v1/workflow/tasks/{taskId}/complete",
                        started.path("activeTasks").get(0).path("taskId").asText()), admin,
                json(Map.of("comment", "初审通过")));

        JsonNode activities = call(get("/api/v1/workflow/instances/{id}/activities", pid), admin, null);
        assertThat(activities.findValuesAsText("key")).contains("initialReview", "countersign");

        // 3. 详情补全发起人姓名与意见人姓名，前端不再展示裸 id
        JsonNode detail = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(detail.path("startedByName").asText()).isNotBlank();
        boolean hasCommentUserNames = false;
        for (JsonNode comment : detail.path("comments")) {
            if (!comment.path("userName").asText().isEmpty()) {
                hasCommentUserNames = true;
                break;
            }
        }
        assertThat(hasCommentUserNames).isTrue();

        // 4. 对象维度实例列表（对象详情「审批」页签）
        JsonNode instances = call(get("/api/v1/workflow/instances")
                        .param("objectType", "PdmItem").param("objectId", String.valueOf(itemId)),
                admin, null);
        assertThat(instances.size()).isEqualTo(1);
        assertThat(instances.get(0).path("processInstanceId").asText()).isEqualTo(pid);
        assertThat(instances.get(0).path("status").asText()).isEqualTo("RUNNING");
        assertThat(instances.get(0).path("currentActivities").size()).isGreaterThan(0);
    }

    /** 流程定义管理：列表 / 读 XML / 部署新版本 / 非法 XML 返回 400。 */
    @Test
    void definitionsListXmlAndDeployNewVersion() throws Exception {
        String admin = login("admin", "Admin@12345");

        JsonNode list = call(get("/api/v1/workflow/definitions"), admin, null);
        JsonNode target = null;
        for (JsonNode row : list) {
            if ("nexplmApproval".equals(row.path("key").asText())) {
                target = row;
            }
        }
        assertThat(target).isNotNull();
        int version = target.path("version").asInt();
        assertThat(version).isGreaterThanOrEqualTo(1);

        JsonNode xml = call(get("/api/v1/workflow/definitions/{id}/xml", target.path("id").asText()), admin, null);
        String bpmn = xml.path("xml").asText();
        assertThat(bpmn).contains("nexplmApproval");

        // 同 key 重新部署 → 版本 +1
        JsonNode deployed = call(post("/api/v1/workflow/definitions"), admin,
                json(Map.of("name", "冒烟-流程定义", "bpmnXml", bpmn)));
        assertThat(deployed.path("key").asText()).isEqualTo("nexplmApproval");
        assertThat(deployed.path("version").asInt()).isEqualTo(version + 1);

        // 非法 XML → 400（由引擎解析失败转换而来，不是 500）
        mockMvc.perform(post("/api/v1/workflow/definitions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "坏流程", "bpmnXml", "<not-bpmn/>"))))
                .andExpect(status().isBadRequest());
    }

    /** 业务绑定按 condition_json 选流程：命中的优先于兜底绑定。 */
    @Test
    void bindingConditionSelectsProcess() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long countersignerId = createUser(admin, "wf_cond", "条件会签");

        // 1. 部署一个同构流程，仅换 process id，用于区分命中了哪条绑定
        JsonNode definitions = call(get("/api/v1/workflow/definitions"), admin, null);
        String baseId = null;
        for (JsonNode row : definitions) {
            if ("nexplmApproval".equals(row.path("key").asText())) {
                baseId = row.path("id").asText();
            }
        }
        String xml = call(get("/api/v1/workflow/definitions/{id}/xml", baseId), admin, null).path("xml").asText();
        String altXml = xml.replace("id=\"nexplmApproval\"", "id=\"nexplmApprovalAlt\"");
        JsonNode alt = call(post("/api/v1/workflow/definitions"), admin,
                json(Map.of("name", "冒烟-条件流程", "bpmnXml", altXml)));
        assertThat(alt.path("key").asText()).isEqualTo("nexplmApprovalAlt");

        long bindingId = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO wf_binding (id, object_type, category, condition_json, process_key, name, priority, enabled)
                VALUES (?, 'PdmItem', 'SMOKE_COND', '{"lifecycleState":"Draft"}'::jsonb,
                        'nexplmApprovalAlt', '冒烟-条件绑定', 1, true)
                """, bindingId);
        try {
            long itemId = createItem(admin, "条件零件");
            JsonNode started = start(admin, itemId, adminId, List.of(adminId, countersignerId));
            assertThat(started.path("processKey").asText()).isEqualTo("nexplmApprovalAlt");
        } finally {
            // 条件绑定会影响后续用例（同样是 PdmItem + Draft），必须清理
            jdbcTemplate.update("DELETE FROM wf_binding WHERE id = ?", bindingId);
        }
    }

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

    @Test
    void assigneeResolversExpandRoleStarterAndFormField() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long roleMemberA = createUser(admin, "wf_role_a", "角色甲");
        long roleMemberB = createUser(admin, "wf_role_b", "角色乙");
        long formApprover = createUser(admin, "wf_form_user", "表单审批人");
        long itemId = createItem(admin, "解析零件");

        // 1. 类型清单：8 种解析器已注册
        JsonNode types = call(get("/api/v1/workflow/assignee-types"), admin, null);
        assertThat(types.findValuesAsText("type")).contains(
                "USER", "ROLE", "ORG", "GROUP", "ROLE_GROUP", "STARTER", "FORM_FIELD", "PROJECT_ROLE");

        // 2. 初审=发起人（STARTER）；会签=业务角色成员（ROLE）— 后者用户数不固定，用包含关系断言
        Map<String, Object> byRole = new HashMap<>();
        byRole.put("objectType", "PdmItem");
        byRole.put("objectId", itemId);
        byRole.put("initialAssignees", List.of(Map.of("type", "STARTER")));
        byRole.put("countersignAssignees",
                List.of(Map.of("type", "ROLE", "ids", List.of(businessRoleId(admin)))));
        JsonNode started = call(post("/api/v1/workflow/instances"), admin, json(byRole));
        String pid = started.path("processInstanceId").asText();
        assertThat(started.path("activeTasks").get(0).path("assignee").asText())
                .isEqualTo(String.valueOf(adminId));

        call(post("/api/v1/workflow/tasks/{taskId}/complete",
                        started.path("activeTasks").get(0).path("taskId").asText()),
                admin, json(Map.of("comment", "初审通过")));
        JsonNode countersign = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(countersign.path("activeTasks").size()).isGreaterThanOrEqualTo(2);
        assertThat(countersign.path("activeTasks").findValuesAsText("assignee"))
                .contains(String.valueOf(roleMemberA), String.valueOf(roleMemberB));

        // 3. 表单字段：初审人取流程变量里的用户
        Map<String, Object> byForm = new HashMap<>();
        byForm.put("objectType", "PdmItem");
        byForm.put("objectId", itemId);
        byForm.put("initialAssignees", List.of(Map.of("type", "FORM_FIELD", "expression", "nextApprover")));
        byForm.put("countersignAssignees", List.of(Map.of("type", "USER", "ids", List.of(adminId))));
        byForm.put("variables", Map.of("nextApprover", String.valueOf(formApprover)));
        JsonNode formStarted = call(post("/api/v1/workflow/instances"), admin, json(byForm));
        assertThat(formStarted.path("activeTasks").get(0).path("assignee").asText())
                .isEqualTo(String.valueOf(formApprover));

        // 4. 角色下无人 → 400
        Map<String, Object> emptyRole = new HashMap<>();
        emptyRole.put("objectType", "PdmItem");
        emptyRole.put("objectId", itemId);
        emptyRole.put("approverId", adminId);
        emptyRole.put("countersignAssignees", List.of(Map.of("type", "ROLE", "ids", List.of(999999L))));
        mockMvc.perform(post("/api/v1/workflow/instances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(emptyRole)))
                .andExpect(status().isBadRequest());

        // 5. 项目角色未接入 → 400 且提示明确
        Map<String, Object> projectRole = new HashMap<>();
        projectRole.put("objectType", "PdmItem");
        projectRole.put("objectId", itemId);
        projectRole.put("initialAssignees", List.of(Map.of("type", "PROJECT_ROLE", "ids", List.of(1L))));
        projectRole.put("approverIds", List.of(adminId));
        MvcResult projectRoleResult = mockMvc.perform(post("/api/v1/workflow/instances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(projectRole)))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(projectRoleResult.getResponse().getContentAsString()).contains("未接入");
    }

    @Test
    void addSignerAndDelegateWork() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long signerId = createUser(admin, "wf_signer", "被加签人");
        long ownerId = createUser(admin, "wf_owner", "委托人");
        long delegateId = createUser(admin, "wf_proxy", "受托人");
        String owner = login("wf_owner", "Passw0rd!");
        String proxy = login("wf_proxy", "Passw0rd!");
        long itemId = createItem(admin, "加签零件");

        // 1. 会签 2 人（admin + owner），admin 加签 signer → 3 个任务
        JsonNode started = start(admin, itemId, adminId, List.of(adminId, ownerId));
        String pid = started.path("processInstanceId").asText();
        String initialTaskId = started.path("activeTasks").get(0).path("taskId").asText();
        call(post("/api/v1/workflow/tasks/{taskId}/complete", initialTaskId), admin,
                json(Map.of("comment", "初审通过")));

        JsonNode countersign = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(countersign.path("activeTasks").size()).isEqualTo(2);
        String adminCountersignTask = findTaskOfAssignee(countersign.path("activeTasks"), adminId)
                .path("taskId").asText();
        call(post("/api/v1/workflow/tasks/{taskId}/add-signer", adminCountersignTask), admin,
                json(Map.of("signerId", signerId, "comment", "追加技术审核")));

        JsonNode afterSign = call(get("/api/v1/workflow/instances/{id}", pid), admin, null);
        assertThat(afterSign.path("activeTasks").size()).isEqualTo(3);
        assertThat(afterSign.path("activeTasks").findValuesAsText("assignee"))
                .contains(String.valueOf(signerId));
        assertThat(afterSign.path("comments").findValuesAsText("action")).contains("ADD_SIGNER");

        // 2. 非会签节点（初审）加签 → 409
        JsonNode reviewOnly = start(admin, itemId, adminId, List.of(adminId));
        String reviewTaskId = reviewOnly.path("activeTasks").get(0).path("taskId").asText();
        mockMvc.perform(post("/api/v1/workflow/tasks/{taskId}/add-signer", reviewTaskId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("signerId", signerId, "comment", "初审加签"))))
                .andExpect(status().isConflict());

        // 3. 委托：owner 在生效区间内委托给 proxy
        Map<String, Object> delegateReq = new HashMap<>();
        delegateReq.put("delegateId", delegateId);
        delegateReq.put("startAt", OffsetDateTime.now().minusHours(1).toString());
        delegateReq.put("endAt", OffsetDateTime.now().plusHours(2).toString());
        delegateReq.put("remark", "出差期间代理");
        JsonNode created = call(post("/api/v1/workflow/delegates"), owner, json(delegateReq));
        assertThat(created.path("ownerId").asLong()).isEqualTo(ownerId);
        assertThat(created.path("delegateName").asText()).isEqualTo("受托人");

        // 4. 受托人待办里能看到 owner 的任务，并可代办理（意见带「代 XXX 办理」标记）
        JsonNode ownerFlow = start(admin, itemId, ownerId, List.of(ownerId));
        String ownerPid = ownerFlow.path("processInstanceId").asText();
        JsonNode proxyTodos = call(get("/api/v1/workflow/tasks?page=1&size=100"), proxy, null);
        JsonNode ownerTask = findTaskByPid(proxyTodos.path("items"), ownerPid);
        assertThat(ownerTask).isNotNull();
        assertThat(ownerTask.path("assignee").asText()).isEqualTo(String.valueOf(ownerId));

        call(post("/api/v1/workflow/tasks/{taskId}/complete", ownerTask.path("taskId").asText()), proxy,
                json(Map.of("comment", "代理人同意")));
        JsonNode afterProxy = call(get("/api/v1/workflow/instances/{id}", ownerPid), admin, null);
        assertThat(afterProxy.path("comments").findValuesAsText("comment"))
                .anyMatch(text -> text.contains("[代 委托人 办理]"));

        // 5. 删除委托后列表清空
        call(delete("/api/v1/workflow/delegates/{id}", created.path("id").asLong()), owner, null);
        JsonNode emptyDelegates = call(get("/api/v1/workflow/delegates"), owner, null);
        assertThat(emptyDelegates.size()).isZero();

        // 6. 参数校验：委托给自己 400、时间倒置 400
        Map<String, Object> selfDelegate = new HashMap<>();
        selfDelegate.put("delegateId", ownerId);
        selfDelegate.put("startAt", OffsetDateTime.now().toString());
        selfDelegate.put("endAt", OffsetDateTime.now().plusHours(1).toString());
        mockMvc.perform(post("/api/v1/workflow/delegates")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(selfDelegate)))
                .andExpect(status().isBadRequest());

        Map<String, Object> reversedRange = new HashMap<>();
        reversedRange.put("delegateId", delegateId);
        reversedRange.put("startAt", OffsetDateTime.now().plusHours(2).toString());
        reversedRange.put("endAt", OffsetDateTime.now().plusHours(1).toString());
        mockMvc.perform(post("/api/v1/workflow/delegates")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reversedRange)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doneStartedAndBatchComplete() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long otherId = createUser(admin, "wf_batch", "批量审批人");
        long itemId = createItem(admin, "批量零件");

        JsonNode flowA = start(admin, itemId, adminId, List.of(otherId));
        JsonNode flowB = start(admin, itemId, adminId, List.of(otherId));
        String pidA = flowA.path("processInstanceId").asText();
        String taskA = flowA.path("activeTasks").get(0).path("taskId").asText();
        String taskB = flowB.path("activeTasks").get(0).path("taskId").asText();

        // 1. 批量办理：2 条本人任务成功、1 条不存在失败，互不影响
        Map<String, Object> batchReq = new HashMap<>();
        batchReq.put("taskIds", List.of(taskA, taskB, "not-exists-task"));
        batchReq.put("comment", "批量初审通过");
        JsonNode batch = call(post("/api/v1/workflow/tasks/batch-complete"), admin, json(batchReq));
        assertThat(batch.path("total").asInt()).isEqualTo(3);
        assertThat(batch.path("succeeded").asInt()).isEqualTo(2);
        assertThat(batch.path("failed").asInt()).isEqualTo(1);
        assertThat(batch.path("messages").size()).isEqualTo(1);

        // 2. 已办：能查到刚办理的任务，带业务对象上下文
        JsonNode done = call(get("/api/v1/workflow/tasks/done?page=1&size=50"), admin, null);
        assertThat(done.path("total").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(done.path("items").findValuesAsText("taskId")).contains(taskA, taskB);
        JsonNode doneRow = findTaskByTaskId(done.path("items"), taskA);
        assertThat(doneRow.path("objectType").asText()).isEqualTo("PdmItem");
        assertThat(doneRow.path("endTime").isNull()).isFalse();

        // 3. 我发起：状态 RUNNING 且当前活动节点为「会签」
        JsonNode started = call(get("/api/v1/workflow/instances/started?page=1&size=50"), admin, null);
        JsonNode instanceRow = findInstanceById(started.path("items"), pidA);
        assertThat(instanceRow).isNotNull();
        assertThat(instanceRow.path("status").asText()).isEqualTo("RUNNING");
        assertThat(instanceRow.path("currentActivities").get(0).asText()).isEqualTo("会签");
        assertThat(instanceRow.path("objectCode").asText()).isNotBlank();

        // 4. 批量办理他人任务 → 计入失败而不是整批报错
        JsonNode detail = call(get("/api/v1/workflow/instances/{id}", pidA), admin, null);
        String otherTask = findTaskOfAssignee(detail.path("activeTasks"), otherId).path("taskId").asText();
        Map<String, Object> crossBatch = new HashMap<>();
        crossBatch.put("taskIds", List.of(otherTask));
        crossBatch.put("comment", "越权批量");
        JsonNode crossResult = call(post("/api/v1/workflow/tasks/batch-complete"), admin, json(crossBatch));
        assertThat(crossResult.path("succeeded").asInt()).isZero();
        assertThat(crossResult.path("failed").asInt()).isEqualTo(1);
    }

    @Test
    void lifecycleFollowsWorkflowTransitions() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "生命周期零件");
        assertThat(stateOf(itemId)).isEqualTo("Draft");

        // 1. 发起 → 插件把对象推进到「审批中」
        JsonNode started = start(admin, itemId, adminId, List.of(adminId));
        String pid = started.path("processInstanceId").asText();
        assertThat(stateOf(itemId)).isEqualTo("InWorkflow");

        // 2. 初审 + 会签全部通过 → 「已发布」
        completeTaskOf(admin, pid, "admin");
        completeTaskOf(admin, pid, "admin");
        assertThat(stateOf(itemId)).isEqualTo("Released");

        // 3. 撤回 → 回到「草稿」
        long withdrawItem = createItem(admin, "撤回零件");
        JsonNode withdrawFlow = start(admin, withdrawItem, adminId, List.of(adminId));
        assertThat(stateOf(withdrawItem)).isEqualTo("InWorkflow");
        call(post("/api/v1/workflow/instances/{id}/withdraw",
                        withdrawFlow.path("processInstanceId").asText()),
                admin, json(Map.of("reason", "需求取消")));
        assertThat(stateOf(withdrawItem)).isEqualTo("Draft");
    }

    @Test
    void pluginContextCarriesProcessVariables() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "插件变量零件");
        CapturingPlugin.EVENTS.clear();

        Map<String, Object> req = new HashMap<>();
        req.put("objectType", "PdmItem");
        req.put("objectId", itemId);
        req.put("approverId", adminId);
        req.put("approverIds", List.of(adminId));
        req.put("remark", "变量透传验证");
        req.put("variables", Map.of("bizTag", "T-9001"));
        String pid = call(post("/api/v1/workflow/instances"), admin, json(req))
                .path("processInstanceId").asText();

        // 1. 发起事件：运行时变量可读，且含引擎补的 approver/approvers/initiator
        WfPluginContext started = CapturingPlugin.EVENTS.stream()
                .filter(e -> pid.equals(e.processInstanceId())
                        && WfPluginContext.ACTION_STARTED.equals(e.action()))
                .findFirst().orElseThrow(() -> new AssertionError("未收到 STARTED 插件事件"));
        assertThat(started.variables()).containsEntry("bizTag", "T-9001");
        assertThat(started.variables()).containsKeys("approver", "approvers", "initiator");

        // 2. 走完全流程后归档事件：实例已结束，变量应来自历史（原来这里恒为空 Map）
        completeTaskOf(admin, pid, "admin");
        completeTaskOf(admin, pid, "admin");
        WfPluginContext completed = CapturingPlugin.EVENTS.stream()
                .filter(e -> pid.equals(e.processInstanceId())
                        && WfPluginContext.ACTION_COMPLETED.equals(e.action()))
                .findFirst().orElseThrow(() -> new AssertionError("未收到 COMPLETED 插件事件"));
        assertThat(completed.variables()).containsEntry("bizTag", "T-9001");
        assertThat(completed.variables()).containsKey("initiator");
    }

    @Test
    void signaturesAreRecordedOnApprovalAndReject() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "签名零件");

        // 1. 初审 + 会签各留一条签名
        JsonNode started = start(admin, itemId, adminId, List.of(adminId));
        String pid = started.path("processInstanceId").asText();
        completeTaskOf(admin, pid, "admin");
        completeTaskOf(admin, pid, "admin");

        JsonNode signatures = call(get("/api/v1/workflow/signatures?objectType=PdmItem&objectId=" + itemId),
                admin, null);
        assertThat(signatures.size()).isEqualTo(2);
        assertThat(signatures.findValuesAsText("meaning")).containsOnly("APPROVE");
        assertThat(signatures.findValuesAsText("signerName")).allMatch(name -> !name.isBlank());
        assertThat(signatures.findValuesAsText("verifyResult")).containsOnly("UNKNOWN");
        assertThat(signatures.findValuesAsText("taskKey")).contains("initialReview", "countersign");
        assertThat(signatures.get(0).path("revisionLabel").asText()).isNotBlank();

        // 2. 驳回也留痕，语义为 REJECT
        long rejectItem = createItem(admin, "签名驳回零件");
        JsonNode flow = start(admin, rejectItem, adminId, List.of(adminId));
        String rejectPid = flow.path("processInstanceId").asText();
        completeTaskOf(admin, rejectPid, "admin");
        JsonNode detail = call(get("/api/v1/workflow/instances/{id}", rejectPid), admin, null);
        String countersignTask = findTaskOfAssignee(detail.path("activeTasks"), adminId)
                .path("taskId").asText();
        call(post("/api/v1/workflow/tasks/{taskId}/reject", countersignTask), admin,
                json(Map.of("targetActivityId", "initialReview", "comment", "资料不全")));

        JsonNode rejectSignatures = call(
                get("/api/v1/workflow/signatures?objectType=PdmItem&objectId=" + rejectItem), admin, null);
        assertThat(rejectSignatures.findValuesAsText("meaning")).contains("APPROVE", "REJECT");
    }

    @Test
    void consistencyCheckDetectsAndFixesOrphans() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "巡检零件");

        // 造一条「业务关联在跑、引擎里根本没有」的僵尸记录
        String fakePid = "fake-instance-" + System.nanoTime();
        jdbcTemplate.update("""
                        INSERT INTO wf_instance_link (id, object_type, object_id, process_instance_id,
                                                      process_key, business_key, status, started_by, started_at)
                        VALUES (?, 'PdmItem', ?, ?, 'nexplmApproval', ?, 'RUNNING', ?, now())
                        """,
                IdWorker.getId(), itemId, fakePid, "PdmItem:" + itemId, adminId);

        JsonNode report = call(post("/api/v1/workflow/consistency/check?fix=false"), admin, null);
        assertThat(report.path("issues").findValuesAsText("processInstanceId")).contains(fakePid);
        assertThat(report.path("issues").findValuesAsText("code")).contains("ORPHAN_RUNNING_LINK");

        JsonNode fixed = call(post("/api/v1/workflow/consistency/check?fix=true"), admin, null);
        assertThat(fixed.path("autoFixed").size()).isGreaterThanOrEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM wf_instance_link WHERE process_instance_id = ?", String.class, fakePid))
                .isEqualTo("TERMINATED");

        JsonNode after = call(post("/api/v1/workflow/consistency/check?fix=false"), admin, null);
        assertThat(after.path("issues").findValuesAsText("processInstanceId")).doesNotContain(fakePid);
    }

    @Test
    void remindOverdueTasksIsIdempotent() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "催办零件");
        JsonNode started = start(admin, itemId, adminId, List.of(adminId));
        String taskId = started.path("activeTasks").get(0).path("taskId").asText();

        // 把任务创建时间推到 48 小时前，模拟超期
        jdbcTemplate.update("UPDATE act_ru_task SET create_time_ = now() - interval '48 hours' WHERE id_ = ?",
                taskId);

        JsonNode first = call(post("/api/v1/workflow/tasks/remind?hours=24"), admin, null);
        assertThat(first.path("overdue").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(first.path("reminded").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(first.path("tasks").findValuesAsText("taskId")).contains(taskId);
        assertThat(remindCount(taskId)).isEqualTo(1);

        // 同窗口内重复调用 → 跳过，不重复催办
        JsonNode second = call(post("/api/v1/workflow/tasks/remind?hours=24"), admin, null);
        assertThat(second.path("skipped").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(remindCount(taskId)).isEqualTo(1);

        // 提醒内容可在审批意见里看到
        JsonNode detail = call(get("/api/v1/workflow/instances/{id}",
                started.path("processInstanceId").asText()), admin, null);
        assertThat(detail.path("comments").findValuesAsText("action")).contains("REMIND");
    }

    private Long remindCount(String taskId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wf_comment WHERE task_id = ? AND action = 'REMIND'", Long.class, taskId);
    }

    private String stateOf(long objectId) {
        return jdbcTemplate.queryForObject(
                "SELECT lifecycle_state FROM plm_object WHERE id = ?", String.class, objectId);
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

    private JsonNode findTaskByTaskId(JsonNode tasks, String taskId) {
        for (JsonNode task : tasks) {
            if (taskId.equals(task.path("taskId").asText())) {
                return task;
            }
        }
        return null;
    }

    private JsonNode findInstanceById(JsonNode instances, String pid) {
        for (JsonNode instance : instances) {
            if (pid.equals(instance.path("processInstanceId").asText())) {
                return instance;
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

    // ------------------------------------------------------------ 测试插件

    @TestConfiguration
    static class PluginCaptureConfig {
        @Bean
        CapturingPlugin capturingPlugin() {
            return new CapturingPlugin();
        }
    }

    /** 记录型插件：只把上下文存下来供断言，不做任何业务动作（顺序无关、无副作用）。 */
    static class CapturingPlugin implements WfBusinessPlugin {
        static final List<WfPluginContext> EVENTS = Collections.synchronizedList(new ArrayList<>());

        @Override
        public String name() {
            return "capturing-plugin";
        }

        @Override
        public void onProcessEvent(WfPluginContext context) {
            EVENTS.add(context);
        }
    }
}
