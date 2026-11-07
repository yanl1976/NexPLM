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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 E-1｜供应商协同回复（多轮往来 / 逐条确认 / 已读 / 重要 / 生效失效）。
 *
 * <p>取证与取舍见 docs/06 EXP-20261107-15：旧系统「回复」是再建一条发放单、父子无结构关联，
 * 本批改为独立回复表，因此这里重点验证四件事：多轮回复可累加、逐条确认落到明细行、
 * 失效由服务端拦截、草稿不能提前回复。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupCoordinationSmokeTest {

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
    void coordinationReplyReadImportantAndExpiry() throws Exception {
        String admin = login();
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);
        long itemId = insertItem(companyId, "ITEM-C1", "协同支架");
        long secondItemId = insertItem(companyId, "ITEM-C2", "协同衬套");

        JsonNode supplier = call(post("/api/v1/sup/suppliers"), admin,
                json(Map.of("name", "协同回复测试供应商")));
        long supplierId = supplier.path("id").asLong();

        // 1. 失效单：失效日期早于今天 → 发出被服务端拒绝（旧系统只有前端 JS 判断）
        long expiredId = call(post("/api/v1/sup/issuances"), admin,
                json(save(supplierId, "已过期的发放", List.of(item(itemId)),
                        LocalDate.now().minusDays(3)))).path("id").asLong();
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/send", expiredId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 2. 正常单：发出后可回复
        long issuanceId = call(post("/api/v1/sup/issuances"), admin,
                json(save(supplierId, "协同发放", List.of(item(itemId), item(secondItemId)),
                        LocalDate.now().plusDays(30)))).path("id").asLong();
        JsonNode sent = call(post("/api/v1/sup/issuances/{id}/send", issuanceId), admin, null);
        assertThat(sent.path("status").asText()).isEqualTo("SENT");
        assertThat(sent.path("expireDate").asText()).isEqualTo(LocalDate.now().plusDays(30).toString());
        // 逐条确认用的是「发放清单行 id」，不是零件 plm_object id
        String firstRowId = sent.path("items").get(0).path("id").asText();
        String secondRowId = sent.path("items").get(1).path("id").asText();

        // 3. 草稿不能提前回复（另建一张草稿单验证状态机）
        long draftId = call(post("/api/v1/sup/issuances"), admin,
                json(save(supplierId, "草稿单", List.of(item(itemId)), null))).path("id").asLong();
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/replies", draftId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("replyType", "ACK"))))
                .andExpect(status().isConflict());

        // 4. 回复 ACK：内容留空时落默认文案
        JsonNode ack = call(post("/api/v1/sup/issuances/{id}/replies", issuanceId), admin,
                json(Map.of("replyType", "ACK")));
        assertThat(ack.path("replies")).hasSize(1);
        assertThat(ack.path("replies").get(0).path("replyType").asText()).isEqualTo("ACK");
        assertThat(ack.path("replies").get(0).path("content").asText()).isEqualTo("已收阅");
        assertThat(ack.path("replies").get(0).path("repliedByName").asText()).isNotBlank();

        // 5. 图纸确认 + 逐条确认：replyState 落到明细行
        JsonNode confirm = call(post("/api/v1/sup/issuances/{id}/replies", issuanceId), admin,
                json(Map.of("replyType", "CONFIRM", "content", "图纸已核对",
                        "confirmedItemIds", List.of(firstRowId))));
        assertThat(confirm.path("replies")).hasSize(2);
        String firstState = confirm.path("items").get(0).path("replyState").asText();
        String secondState = confirm.path("items").get(1).path("replyState").asText();
        assertThat(firstState).isEqualTo("CONFIRMED");
        assertThat(secondState).isEqualTo("PENDING");

        // 6. 承诺交期必须带日期；带日期则回显
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/replies", issuanceId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("replyType", "COMMIT_DATE"))))
                .andExpect(status().isBadRequest());
        JsonNode commit = call(post("/api/v1/sup/issuances/{id}/replies", issuanceId), admin,
                json(Map.of("replyType", "COMMIT_DATE", "content", "下月 15 日交货",
                        "promisedDate", LocalDate.now().plusDays(20).toString())));
        assertThat(commit.path("replies").get(2).path("promisedDate").asText())
                .isEqualTo(LocalDate.now().plusDays(20).toString());

        // 7. 无法满足：驳回落到明细行
        JsonNode reject = call(post("/api/v1/sup/issuances/{id}/replies", issuanceId), admin,
                json(Map.of("replyType", "REJECT", "content", "该版本已停产",
                        "rejectedItemIds", List.of(secondRowId))));
        assertThat(reject.path("items").get(1).path("replyState").asText()).isEqualTo("REJECTED");

        // 8. 非法回复类型 → 400
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/replies", issuanceId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("replyType", "WHATEVER"))))
                .andExpect(status().isBadRequest());

        // 9. 已读 / 重要 / 附件拉取标记
        call(post("/api/v1/sup/issuances/{id}/read", issuanceId), admin, null);
        call(post("/api/v1/sup/issuances/{id}/important?important=true", issuanceId), admin, null);
        String itemRowId = call(get("/api/v1/sup/issuances/{id}", issuanceId), admin, null)
                .path("items").get(0).path("id").asText();
        call(post("/api/v1/sup/issuances/{id}/items/{itemId}/pull", issuanceId, itemRowId),
                admin, null);
        JsonNode detail = call(get("/api/v1/sup/issuances/{id}", issuanceId), admin, null);
        assertThat(detail.path("readAt").isNull()).isFalse();
        assertThat(detail.path("important").asBoolean()).isTrue();
        assertThat(detail.path("items").get(0).path("pullState").asText()).isEqualTo("PULL");

        // 10. 列表回带往来条数
        JsonNode bySupplier = call(get("/api/v1/sup/suppliers/{id}/issuances", supplierId),
                admin, null);
        JsonNode target = null;
        for (JsonNode row : bySupplier) {
            if (row.path("id").asLong() == issuanceId) {
                target = row;
            }
        }
        assertThat(target).isNotNull();
        assertThat(target.path("replyCount").asInt()).isEqualTo(4);
    }

    // ------------------------------------------------------------ 工具

    private Map<String, Object> save(long supplierId, String title, List<Map<String, Object>> items,
                                     LocalDate expireDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("supplierId", supplierId);
        body.put("title", title);
        body.put("type", "DESIGN");
        body.put("items", items);
        if (expireDate != null) {
            // 生效日永远早于失效日（服务端会校验窗口合法性，这里不制造非法窗口）
            body.put("effectiveDate", expireDate.minusDays(30).toString());
            body.put("expireDate", expireDate.toString());
        }
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
