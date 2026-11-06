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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P7-5 工时填报与审批冒烟。
 *
 * <p>规则：一天一任务一条（唯一键）；只有本人能改；不能审批自己的工时；
 * 汇总只把 APPROVED 算实际工时，SUBMITTED 单列待审批；普通成员只能查自己的。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectTimesheetSmokeTest {

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
    void timesheetSubmitApproveAndSummary() throws Exception {
        String admin = login("admin", "Admin@12345");
        long projectId = createProject(admin, "工时填报测试项目");
        long taskId = createTask(admin, projectId, "结构设计");
        long otherTaskId = createTask(admin, projectId, "工艺编制");
        LocalDate today = LocalDate.now();
        LocalDate day1 = today.minusDays(2);
        LocalDate day2 = today.minusDays(1);

        // 1. admin 填报（草稿）
        JsonNode draft = call(post("/api/v1/prj/timesheets"), admin,
                json(timesheet(taskId, day1, "8", "NORMAL", "结构方案")));
        long adminSheetId = draft.path("id").asLong();
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        assertThat(draft.path("taskWbsCode").asText()).isEqualTo("1");

        // 2. 同一天同一任务重复填报 400；未来日期 400
        mockMvc.perform(post("/api/v1/prj/timesheets")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(timesheet(taskId, day1, "4", "NORMAL", null))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/prj/timesheets")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(timesheet(otherTaskId, today.plusDays(1), "8", "NORMAL", null))))
                .andExpect(status().isBadRequest());

        // 3. 草稿可改；提交后 admin 审批自己的被拒（409）
        JsonNode updated = call(put("/api/v1/prj/timesheets/{id}", adminSheetId), admin,
                json(timesheet(taskId, day1, "6.5", "NORMAL", "结构方案（修订）")));
        assertThat(updated.path("hours").asDouble()).isEqualTo(6.5);
        JsonNode submitted = call(post("/api/v1/prj/timesheets/{id}/submit", adminSheetId), admin, null);
        assertThat(submitted.path("status").asText()).isEqualTo("SUBMITTED");
        mockMvc.perform(post("/api/v1/prj/timesheets/{id}/approve", adminSheetId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        // 已提交不允许改
        mockMvc.perform(put("/api/v1/prj/timesheets/{id}", adminSheetId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(timesheet(taskId, day1, "5", "NORMAL", null))))
                .andExpect(status().isConflict());

        // 4. 建一个业务用户（有 prj.project.write），由他填报、admin 审批
        long memberUserId = createMember(admin, "p75_member", "Member@12345", "BUSINESS");
        String member = login("p75_member", "Member@12345");
        JsonNode memberDraft = call(post("/api/v1/prj/timesheets"), member,
                json(timesheet(taskId, day1, "8", "NORMAL", "结构设计")));
        long memberSheetId = memberDraft.path("id").asLong();
        assertThat(memberDraft.path("userId").asLong()).isEqualTo(memberUserId);
        call(post("/api/v1/prj/timesheets/{id}/submit", memberSheetId), member, null);
        // 即使有审批权限，也不能审自己的工时
        mockMvc.perform(post("/api/v1/prj/timesheets/{id}/approve", memberSheetId)
                        .header("Authorization", "Bearer " + member))
                .andExpect(status().isConflict());
        JsonNode approved = call(post("/api/v1/prj/timesheets/{id}/approve", memberSheetId), admin, null);
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("approverName").asText()).isNotEmpty();

        // 5. 成员再填一天并提交，admin 批量通过
        JsonNode memberDay2 = call(post("/api/v1/prj/timesheets"), member,
                json(timesheet(otherTaskId, day2, "3.5", "OVERTIME", "工艺评审")));
        call(post("/api/v1/prj/timesheets/{id}/submit", memberDay2.path("id").asLong()), member, null);
        JsonNode batch = call(post("/api/v1/prj/timesheets/batch-approve"), admin,
                json(Map.of("ids", List.of(memberDay2.path("id").asLong()))));
        assertThat(batch.path("approved").asInt()).isEqualTo(1);

        // 6. 打回后成员可改，状态回落 DRAFT
        JsonNode rejectTarget = call(post("/api/v1/prj/timesheets"), member,
                json(timesheet(otherTaskId, today, "2", "NORMAL", "评审会议")));
        long rejectId = rejectTarget.path("id").asLong();
        call(post("/api/v1/prj/timesheets/{id}/submit", rejectId), member, null);
        JsonNode rejected = call(post("/api/v1/prj/timesheets/{id}/reject", rejectId), admin,
                json(Map.of("reason", "请拆分到具体任务")));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.path("rejectReason").asText()).isEqualTo("请拆分到具体任务");
        JsonNode reopened = call(put("/api/v1/prj/timesheets/{id}", rejectId), member,
                json(timesheet(otherTaskId, today, "1.5", "NORMAL", "评审会议（拆分后）")));
        assertThat(reopened.path("status").asText()).isEqualTo("DRAFT");

        // 7. 汇总：实际工时只算 APPROVED（8 + 3.5），admin 那条仍是待审批 6.5
        JsonNode summary = call(get("/api/v1/prj/projects/{id}/timesheet-summary"
                + "?from=" + day1 + "&to=" + today, projectId), admin, null);
        assertThat(summary.path("approvedHours").asDouble()).isEqualTo(11.5);
        assertThat(summary.path("pendingHours").asDouble()).isEqualTo(6.5);
        assertThat(summary.path("approvedCount").asInt()).isEqualTo(2);
        assertThat(summary.path("byUser")).hasSize(2);
        assertThat(summary.path("byTask")).hasSize(2);
        assertThat(summary.path("byWeek")).hasSize(1);

        // 8. 资源负荷：成员 11.5h / 40h 容量
        JsonNode load = call(get("/api/v1/prj/projects/{id}/resource-load"
                + "?from=" + day1 + "&to=" + today, projectId), admin, null);
        assertThat(load.path("weekCount").asInt()).isEqualTo(1);
        assertThat(load.path("capacityHours").asDouble()).isEqualTo(40.0);
        assertThat(load.path("rows")).hasSize(1);
        assertThat(load.path("rows").get(0).path("approvedHours").asDouble()).isEqualTo(11.5);

        // 9. 无审批权限的用户：只能查自己的工时（传 admin 的 userId 会被强制改成自己），且不能审批
        createMember(admin, "p75_viewer", "Viewer@12345", "AUDITOR");
        String viewer = login("p75_viewer", "Viewer@12345");
        JsonNode viewerDraft = call(post("/api/v1/prj/timesheets"), viewer,
                json(timesheet(taskId, today, "2", "NORMAL", "旁听评审")));
        JsonNode viewerPage = call(get("/api/v1/prj/timesheets?projectId=" + projectId
                + "&userId=1&size=50"), viewer, null);
        assertThat(viewerPage.path("items")).hasSize(1);
        assertThat(viewerPage.path("items").get(0).path("userName").asText()).isEqualTo("p75_viewer");
        mockMvc.perform(post("/api/v1/prj/timesheets/{id}/approve", viewerDraft.path("id").asLong())
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ 工具

    private long createMember(String token, String username, String password, String roleCode)
            throws Exception {
        JsonNode roles = call(get("/api/v1/system/roles"), token, null);
        long roleId = 0;
        for (JsonNode role : roles) {
            if (roleCode.equals(role.path("code").asText())) {
                roleId = role.path("id").asLong();
            }
        }
        assertThat(roleId).isNotZero();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("displayName", username);
        body.put("roleIds", List.of(roleId));
        return call(post("/api/v1/system/users"), token, json(body)).asLong();
    }

    private Map<String, Object> timesheet(long taskId, LocalDate workDate, String hours,
                                          String workType, String remark) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", taskId);
        body.put("workDate", workDate.toString());
        body.put("hours", hours);
        body.put("workType", workType);
        body.put("remark", remark);
        return body;
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
