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
 * 批次 E-2｜站内消息收件人化：供应商事件进本人收件箱、已读按收件人、可批量已读。
 *
 * <p>修复的缺陷（docs/06 EXP-20261107-15 第 10 条）：原实现按 company_id 过滤 + 消息行级
 * read_at，全公司共用一个收件箱。这里验证三件事：① 事件能进收件箱；② 收件箱行只属于收件人；
 * ③ 已读/全部已读不改动别人。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotifyInboxSmokeTest {

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
    void supplierEventsLandInRecipientInbox() throws Exception {
        String admin = login();
        long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);

        // 0. 初始应为空
        assertThat(unread(admin)).isZero();

        long itemId = insertItem(companyId, "ITEM-N1", "通知测试件");
        long supplierId = call(post("/api/v1/sup/suppliers"), admin,
                json(Map.of("name", "通知测试供应商"))).path("id").asLong();
        long issuanceId = call(post("/api/v1/sup/issuances"), admin,
                json(save(supplierId, "通知发放", List.of(item(itemId))))).path("id").asLong();
        String code = call(get("/api/v1/sup/issuances/{id}", issuanceId), admin, null)
                .path("code").asText();

        // 1. 发出 → 一条站内消息进本人收件箱
        call(post("/api/v1/sup/issuances/{id}/send", issuanceId), admin, null);
        assertThat(unread(admin)).isEqualTo(1);
        JsonNode inbox = call(get("/api/v1/my/messages"), admin, null);
        assertThat(inbox).hasSize(1);
        assertThat(inbox.get(0).path("title").asText()).contains("供应商数据已发放").contains(code);
        assertThat(inbox.get(0).path("eventType").asText()).isEqualTo("supplier.issuance.sent");
        assertThat(inbox.get(0).path("readAt").isMissingNode()
                || inbox.get(0).path("readAt").isNull()).isTrue();

        // 2. 收件箱行的 user_id 必须是本人（收件人维度，而不是公司维度）
        Long inboxUserId = jdbcTemplate.queryForObject("""
                SELECT b.user_id FROM sys_notify_inbox b
                  JOIN sys_notify_message m ON m.id = b.message_id
                 WHERE m.event_key LIKE 'supplier.issuance.sent:%'
                 LIMIT 1
                """, Long.class);
        assertThat(inboxUserId).isEqualTo(adminId);
        Long internalChannels = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_notify_channel WHERE type = 'INTERNAL' AND deleted = false",
                Long.class);
        assertThat(internalChannels).isEqualTo(1);

        // 3. 协同回复 → 第二条消息
        call(post("/api/v1/sup/issuances/{id}/replies", issuanceId), admin,
                json(Map.of("replyType", "CONFIRM", "content", "图纸确认完毕")));
        assertThat(unread(admin)).isEqualTo(2);
        JsonNode twoMessages = call(get("/api/v1/my/messages").queryParam("unreadOnly", "true"),
                admin, null);
        assertThat(twoMessages).hasSize(2);
        boolean hasReply = false;
        for (JsonNode row : twoMessages) {
            if ("supplier.reply.created".equals(row.path("eventType").asText())) {
                hasReply = true;
                assertThat(row.path("content").asText()).contains("图纸确认完毕");
            }
        }
        assertThat(hasReply).isTrue();

        // 4. 标记单条已读 → 未读减一（按收件人收件箱行更新）
        long firstMessageId = twoMessages.get(0).path("id").asLong();
        call(post("/api/v1/my/messages/{id}/read", firstMessageId), admin, null);
        assertThat(unread(admin)).isEqualTo(1);

        // 5. 全部已读 → 未读归零，未读筛选为空
        JsonNode marked = call(post("/api/v1/my/messages/read-all"), admin, null);
        assertThat(marked.path("updated").asInt()).isEqualTo(1);
        assertThat(unread(admin)).isZero();
        assertThat(call(get("/api/v1/my/messages").queryParam("unreadOnly", "true"), admin, null))
                .isEmpty();
    }

    // ------------------------------------------------------------ 工具

    private int unread(String token) throws Exception {
        return call(get("/api/v1/my/messages/unread-count"), token, null).path("unread").asInt();
    }

    private Map<String, Object> save(long supplierId, String title, List<Map<String, Object>> items) {
        Map<String, Object> body = new HashMap<>();
        body.put("supplierId", supplierId);
        body.put("title", title);
        body.put("type", "DESIGN");
        body.put("items", items);
        return body;
    }

    private Map<String, Object> item(long objectId) {
        return Map.of("objectId", objectId, "objectType", "PdmItem");
    }

    private long insertItem(long companyId, String code, String name) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                                lifecycle_state)
                        VALUES (?, 'PdmItem', ?, ?, ?, ?, 'Draft')
                        """, id, id, code, name, companyId);
        return id;
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
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
