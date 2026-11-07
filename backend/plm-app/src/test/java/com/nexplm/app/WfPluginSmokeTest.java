package com.nexplm.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工作流业务插件冒烟：{@code SendNotification}（通知通道）与 {@code ErpPublish}（金蝶下发）。
 *
 * <p>两个插件都在流程提交后（afterCommit）以独立事务执行，因此断言要看它们各自的落库产物：
 * 通知看 {@code sys_notify_message}，下发看 {@code itg_job}——这也正是运维可回溯的两张表。</p>
 *
 * <p>本用例不依赖外部服务：通知通道用 EMAIL 且不配 SMTP（消息落库、投递失败即 FAILED），
 * 金蝶端点指向本机不可达端口（任务落库、发送失败即 FAILED），验证的是「该发的时候发了」。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class WfPluginSmokeTest {

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
        // 本测试类验证多账套：配置指定端点 code，与 createKingdeeEndpoint 的 code 一致
        registry.add("nexplm.workflow.erp-publish.endpoint-code", () -> "kingdee-test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 一条完整审批链路（初审 → 会签 → 归档）应产生：
     * 2 条节点通知（初审、会签各一条）+ 1 条流程通过通知 + 1 条金蝶下发任务。
     */
    @Test
    void notificationAndErpPublishFireOnWorkflowEvents() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "插件零件");
        String itemCode = itemCode(itemId);
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM plm_object WHERE id = ?", Long.class, itemId);

        long channelId = createNotifyChannel(companyId);
        createKingdeeEndpoint(companyId);

        JsonNode started = start(admin, itemId, adminId);
        String pid = started.path("processInstanceId").asText();
        assertThat(pid).isNotBlank();

        // 1. 初审通过：节点级通知立即产生（此时流程仍在运行）
        completeByAdmin(admin, pid);
        assertThat(countMessages(channelId, "wf.task.completed")).isEqualTo(1);
        assertThat(countMessages(channelId, "wf.process.completed")).isZero();
        assertThat(lifecycleState(itemId)).isEqualTo("InWorkflow");

        // 2. 会签通过：流程归档 → 第二条节点通知 + 流程级通知 + 触发金蝶下发
        completeByAdmin(admin, pid);
        assertThat(countMessages(channelId, "wf.task.completed")).isEqualTo(2);
        assertThat(countMessages(channelId, "wf.process.completed")).isEqualTo(1);
        assertThat(lifecycleState(itemId)).isEqualTo("Released");

        List<Map<String, Object>> jobs = jobsOf(itemCode);
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).get("biz_type")).isEqualTo("Material");
        assertThat(String.valueOf(jobs.get(0).get("biz_key"))).startsWith(itemCode).contains("@R");

        // 3. 通知文案是渲染后的快照，含对象编码、名称与结果
        String content = jdbcTemplate.queryForObject("""
                SELECT content FROM sys_notify_message
                 WHERE channel_id = ? AND event_type = 'wf.process.completed'
                """, String.class, channelId);
        assertThat(content).contains(itemCode).contains("插件零件").contains("审批通过");
    }

    /** 没有金蝶端点时不下发：只在集成中心有启用端点才推 ERP，避免"凭空发一份"。 */
    @Test
    void erpPublishSkipsWhenNoKingdeeEndpoint() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "无端点零件");
        String itemCode = itemCode(itemId);
        // 清掉其它用例可能建的端点，保证本用例处于"未配置端点"环境
        jdbcTemplate.update("UPDATE itg_endpoint SET enabled = false WHERE system_code = 'KINGDEE'");

        JsonNode started = start(admin, itemId, adminId);
        String pid = started.path("processInstanceId").asText();
        completeByAdmin(admin, pid);
        completeByAdmin(admin, pid);

        assertThat(lifecycleState(itemId)).isEqualTo("Released");
        assertThat(jobsOf(itemCode)).isEmpty();
    }

    /**
     * 多账套（台账 B8）：配置了 endpoint-code 时只选该端点，即使另一个启用端点 id 更小；
     * 指定端点停用后跳过下发，不静默回退到其它账套。
     */
    @Test
    void erpPublishUsesConfiguredEndpointAndDoesNotFallBack() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "多账套零件");
        String itemCode = itemCode(itemId);
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM plm_object WHERE id = ?", Long.class, itemId);

        // 清掉其它用例残留的端点（软删，避免影响历史 itg_job 的引用）
        jdbcTemplate.update("""
                UPDATE itg_endpoint SET deleted = true, enabled = false
                 WHERE system_code = 'KINGDEE' AND company_id = ?
                """, companyId);
        long configuredId = 900_001L;
        long smallerId = 100L;
        jdbcTemplate.update("""
                INSERT INTO itg_endpoint(id, company_id, code, name, system_code, base_url, timeout_ms, enabled)
                VALUES (?, ?, 'kingdee-test', '指定账套', 'KINGDEE', 'http://127.0.0.1:1/', 500, true)
                """, configuredId, companyId);
        jdbcTemplate.update("""
                INSERT INTO itg_endpoint(id, company_id, code, name, system_code, base_url, timeout_ms, enabled)
                VALUES (?, ?, 'kingdee-other', 'id 更小的备用账套', 'KINGDEE', 'http://127.0.0.1:2/', 500, true)
                """, smallerId, companyId);

        JsonNode started = start(admin, itemId, adminId);
        String pid = started.path("processInstanceId").asText();
        completeByAdmin(admin, pid);
        completeByAdmin(admin, pid);
        List<Map<String, Object>> jobs = jobsOf(itemCode);
        assertThat(jobs).hasSize(1);
        assertThat(((Number) jobs.get(0).get("endpoint_id")).longValue()).isEqualTo(configuredId);

        // 指定端点停用 → 剩下 id 更小的备用端点也不得被选中
        jdbcTemplate.update("UPDATE itg_endpoint SET enabled = false WHERE id = ?", configuredId);
        long secondItemId = createItem(admin, "指定账套不可用零件");
        String secondCode = itemCode(secondItemId);
        JsonNode second = start(admin, secondItemId, adminId);
        String secondPid = second.path("processInstanceId").asText();
        completeByAdmin(admin, secondPid);
        completeByAdmin(admin, secondPid);
        assertThat(jobsOf(secondCode)).isEmpty();

        // 复位，避免影响其它用例
        jdbcTemplate.update("UPDATE itg_endpoint SET deleted = true WHERE id IN (?, ?)",
                configuredId, smallerId);
    }

    // ------------------------------------------------------------ 工具

    private void completeByAdmin(String token, String pid) throws Exception {
        JsonNode detail = call(get("/api/v1/workflow/instances/{id}", pid), token, null);
        JsonNode task = detail.path("activeTasks").get(0);
        assertThat(task).as("实例 %s 应有待办任务", pid).isNotNull();
        call(post("/api/v1/workflow/tasks/{taskId}/complete", task.path("taskId").asText()), token,
                json(Map.of("comment", "同意")));
    }

    private long createNotifyChannel(long companyId) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO sys_notify_channel(id, company_id, code, name, type, enabled, events, config, max_attempts)
                VALUES (?, ?, 'wf-plugin-test', '流程插件测试通道', 'EMAIL', true,
                        CAST(? AS jsonb), CAST('{}' AS jsonb), 1)
                """, id, companyId, "[\"wf.task.completed\",\"wf.process.completed\"]");
        return id;
    }

    private void createKingdeeEndpoint(long companyId) {
        jdbcTemplate.update("""
                INSERT INTO itg_endpoint(id, company_id, code, name, system_code, base_url, timeout_ms, enabled)
                VALUES (?, ?, 'kingdee-test', '金蝶测试端点', 'KINGDEE', 'http://127.0.0.1:1/', 500, true)
                """, IdWorker.getId(), companyId);
    }

    private long countMessages(long channelId, String eventType) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_notify_message WHERE channel_id = ? AND event_type = ?",
                Long.class, channelId, eventType);
        return count == null ? 0 : count;
    }

    private String itemCode(long itemId) {
        return jdbcTemplate.queryForObject("SELECT code FROM plm_object WHERE id = ?", String.class, itemId);
    }

    private List<Map<String, Object>> jobsOf(String itemCode) {
        return jdbcTemplate.queryForList(
                "SELECT biz_type, biz_key, status, endpoint_id FROM itg_job WHERE biz_key LIKE ?", itemCode + "%");
    }

    private String lifecycleState(long itemId) {
        return jdbcTemplate.queryForObject(
                "SELECT lifecycle_state FROM plm_object WHERE id = ?", String.class, itemId);
    }

    private JsonNode start(String token, long itemId, long approverId) throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("objectType", "PdmItem");
        req.put("objectId", itemId);
        req.put("approverId", approverId);
        req.put("approverIds", List.of(approverId));
        req.put("remark", "插件冒烟发起");
        return call(post("/api/v1/workflow/instances"), token, json(req));
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
