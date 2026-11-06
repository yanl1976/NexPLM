package com.nexplm.app;

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
 * P8-4 供应商主数据与零件关系冒烟：多源供货 + 首选唯一 + 删除保护。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupplierSmokeTest {

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
    void supplierPartsAndPreferredExclusive() throws Exception {
        String admin = login("admin", "Admin@12345");
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);
        long itemId = insertItem(companyId, "ITEM-A", "阀体");
        long otherItemId = insertItem(companyId, "ITEM-B", "密封圈");

        // 1. 新建两家供应商（SUP- 取号，默认待准入）
        JsonNode supplierA = call(post("/api/v1/sup/suppliers"), admin, json(Map.of(
                "name", "宁波精密铸造有限公司", "shortName", "宁波精密",
                "category", "MATERIAL", "contact", "王工", "phone", "13800000000",
                "paymentTerms", "月结 60 天")));
        long supplierAId = supplierA.path("id").asLong();
        assertThat(supplierA.path("code").asText()).startsWith("SUP-");
        assertThat(supplierA.path("status").asText()).isEqualTo("PENDING");
        assertThat(supplierA.path("currency").asText()).isEqualTo("CNY");
        JsonNode supplierB = call(post("/api/v1/sup/suppliers"), admin, json(Map.of(
                "name", "苏州机械加工厂", "category", "OUTSOURCE", "status", "QUALIFIED")));
        long supplierBId = supplierB.path("id").asLong();

        // 未知类别 → 400
        mockMvc.perform(post("/api/v1/sup/suppliers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "类别写错", "category", "RAW"))))
                .andExpect(status().isBadRequest());

        // 2. A 挂 ITEM-A（首选）
        Map<String, Object> partBody = new HashMap<>();
        partBody.put("objectId", itemId);
        partBody.put("supplyType", "BUY");
        partBody.put("preferred", true);
        partBody.put("leadTimeDays", 15);
        partBody.put("moq", 100);
        partBody.put("price", 12.5);
        JsonNode withPart = call(post("/api/v1/sup/suppliers/{id}/parts", supplierAId), admin,
                json(partBody));
        assertThat(withPart.path("parts")).hasSize(1);
        assertThat(withPart.path("parts").get(0).path("itemCode").asText()).isEqualTo("ITEM-A");
        assertThat(withPart.path("parts").get(0).path("itemName").asText()).isEqualTo("阀体");
        assertThat(withPart.path("parts").get(0).path("preferred").asBoolean()).isTrue();
        long partAId = withPart.path("parts").get(0).path("id").asLong();

        // 3. 同一供应商重复挂同一零件 → 400；不存在的零件 → 400
        mockMvc.perform(post("/api/v1/sup/suppliers/{id}/parts", supplierAId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(partBody)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/suppliers/{id}/parts", supplierAId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("objectId", 999999999L))))
                .andExpect(status().isBadRequest());

        // 4. B 也挂 ITEM-A 且设为首选 → A 的首选被自动取消（同一零件首选唯一）
        JsonNode bParts = call(post("/api/v1/sup/suppliers/{id}/parts", supplierBId), admin,
                json(Map.of("objectId", itemId, "preferred", true, "supplyType", "DOUBLE",
                        "leadTimeDays", 7)));
        assertThat(bParts.path("parts").get(0).path("preferred").asBoolean()).isTrue();
        JsonNode aAfter = call(get("/api/v1/sup/suppliers/{id}", supplierAId), admin, null);
        assertThat(aAfter.path("parts").get(0).path("preferred").asBoolean()).isFalse();

        // 5. 按零件反查：两条 ACTIVE 关系，首选在前
        JsonNode byItem = call(get("/api/v1/sup/parts/by-item?objectId=" + itemId), admin, null);
        assertThat(byItem).hasSize(2);
        assertThat(byItem.get(0).path("preferred").asBoolean()).isTrue();
        assertThat(byItem.get(0).path("supplierId").asLong()).isEqualTo(supplierBId);
        assertThat(byItem.get(1).path("supplierCode").asText())
                .isEqualTo(supplierA.path("code").asText());

        // 6. 改关系（换零件 + 关掉首选 + 停用）
        JsonNode updatedPart = call(put("/api/v1/sup/parts/{id}", partAId), admin,
                json(Map.of("objectId", otherItemId, "preferred", false, "supplyType", "BUY",
                        "status", "INACTIVE", "leadTimeDays", 20)));
        assertThat(updatedPart.path("parts").get(0).path("itemCode").asText()).isEqualTo("ITEM-B");
        assertThat(updatedPart.path("parts").get(0).path("status").asText()).isEqualTo("INACTIVE");
        assertThat(updatedPart.path("parts").get(0).path("leadTimeDays").asInt()).isEqualTo(20);

        // 7. 停用后按零件反查不到（只有 ACTIVE 关系）
        assertThat(call(get("/api/v1/sup/parts/by-item?objectId=" + otherItemId), admin, null))
                .isEmpty();

        // 8. A 已无 ACTIVE 关系 → 可删除；B 仍有 ACTIVE 关系 → 409
        call(delete("/api/v1/sup/suppliers/{id}", supplierAId), admin, null);
        mockMvc.perform(get("/api/v1/sup/suppliers/{id}", supplierAId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/sup/suppliers/{id}", supplierBId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 9. 列表过滤与统计
        JsonNode byStatus = call(get("/api/v1/sup/suppliers?status=QUALIFIED"), admin, null);
        assertThat(byStatus.path("total").asInt()).isEqualTo(1);
        assertThat(byStatus.path("items").get(0).path("partCount").asInt()).isEqualTo(1);
        assertThat(byStatus.path("items").get(0).path("preferredCount").asInt()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/sup/suppliers?category=WRONG")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 10. 关系软删后供应商统计归零
        long partBId = jdbcTemplate.queryForObject(
                "SELECT id FROM sup_part WHERE supplier_id = ? AND deleted = false",
                Long.class, supplierBId);
        call(delete("/api/v1/sup/parts/{id}", partBId), admin, null);
        JsonNode afterRemove = call(get("/api/v1/sup/suppliers/{id}", supplierBId), admin, null);
        assertThat(afterRemove.path("partCount").asInt()).isZero();
        assertThat(afterRemove.path("parts")).isEmpty();
    }

    // ------------------------------------------------------------------ 工具

    private long insertItem(long companyId, String code, String name) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                        lifecycle_state)
                VALUES (?, 'PdmItem', ?, ?, ?, ?, 'Draft')
                """, id, id, code, name, companyId);
        return id;
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
