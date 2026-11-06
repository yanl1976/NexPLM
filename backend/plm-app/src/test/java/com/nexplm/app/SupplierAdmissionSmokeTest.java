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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P8-5-2 供应商准入冒烟：检查项口径 + 状态机 + "唯一把供应商变合格的入口"。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupplierAdmissionSmokeTest {

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
    void admissionReviewToQualified() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 供应商默认待准入
        JsonNode supplier = call(post("/api/v1/sup/suppliers"), admin,
                json(Map.of("name", "准入测试供应商")));
        long supplierId = supplier.path("id").asLong();
        assertThat(supplier.path("status").asText()).isEqualTo("PENDING");

        // 2. 发起准入：ADM- 取号，草稿，检查项统计正确
        JsonNode draft = call(post("/api/v1/sup/admissions"), admin, json(body(supplierId,
                "INITIAL", "2026-11-10", "张工", List.of(
                        item("营业执照与经营范围", "PASS", "营业执照副本"),
                        item("质量体系证书", "FAIL", "ISO9001 已过期"),
                        item("样品承认", "NA", null)))));
        long admissionId = draft.path("id").asLong();
        assertThat(draft.path("code").asText()).startsWith("ADM-");
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        assertThat(draft.path("items")).hasSize(3);
        assertThat(draft.path("plannedDate").asText()).isEqualTo("2026-11-10");

        // 3. 参数校验：未知类型 / 日期格式 / 检查项为空 / 供应商不存在
        //    另起一家供应商做参数校验，否则会先撞上"已有未结束评审"（409）
        JsonNode other = call(post("/api/v1/sup/suppliers"), admin,
                json(Map.of("name", "参数校验供应商")));
        long otherId = other.path("id").asLong();
        mockMvc.perform(post("/api/v1/sup/admissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(otherId, "WRONG", null, null,
                                List.of(item("x", "PASS", null))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/admissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(otherId, "INITIAL", "2026-13-01", null,
                                List.of(item("x", "PASS", null))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/admissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(otherId, "INITIAL", null, null, List.of()))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/admissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(999999999L, "INITIAL", null, null,
                                List.of(item("x", "PASS", null))))))
                .andExpect(status().isNotFound());

        // 4. 同一供应商已有未结束评审 → 409
        mockMvc.perform(post("/api/v1/sup/admissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "ANNUAL", null, null,
                                List.of(item("x", "PASS", null))))))
                .andExpect(status().isConflict());

        // 5. 草稿可改：检查项整表替换（保留一条 FAIL）
        JsonNode updated = call(put("/api/v1/sup/admissions/{id}", admissionId), admin,
                json(body(supplierId, "INITIAL", "2026-11-12", "李工", List.of(
                        item("营业执照与经营范围", "PASS", "副本"),
                        item("质量体系证书", "FAIL", "ISO9001 已过期")))));
        assertThat(updated.path("items")).hasSize(2);
        assertThat(updated.path("leader").asText()).isEqualTo("李工");
        assertThat(updated.path("plannedDate").asText()).isEqualTo("2026-11-12");

        // 6. 提交后冻结：再改/再提交/删除都 409
        JsonNode submitted = call(post("/api/v1/sup/admissions/{id}/submit", admissionId), admin,
                null);
        assertThat(submitted.path("status").asText()).isEqualTo("SUBMITTED");
        mockMvc.perform(post("/api/v1/sup/admissions/{id}/submit", admissionId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/sup/admissions/{id}", admissionId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "INITIAL", null, null,
                                List.of(item("x", "PASS", null))))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/sup/admissions/{id}", admissionId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 7. 有 FAIL 不能通过；驳回必须填原因
        mockMvc.perform(post("/api/v1/sup/admissions/{id}/approve", admissionId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/sup/admissions/{id}/reject", admissionId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", ""))))
                .andExpect(status().isBadRequest());
        JsonNode rejected = call(post("/api/v1/sup/admissions/{id}/reject", admissionId), admin,
                json(Map.of("reason", "ISO9001 证书已过期，需换证后重评")));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.path("rejectReason").asText()).contains("ISO9001");
        JsonNode supplierAfterReject = call(get("/api/v1/sup/suppliers/{id}", supplierId), admin,
                null);
        assertThat(supplierAfterReject.path("status").asText()).isEqualTo("PENDING");

        // 8. 驳回后可另起一张：全 PASS → 通过 → 供应商转合格
        JsonNode second = call(post("/api/v1/sup/admissions"), admin, json(body(supplierId,
                "ANNUAL", "2026-11-20", "张工", List.of(
                        item("营业执照与经营范围", "PASS", "副本"),
                        item("质量体系证书", "PASS", "ISO9001 新证"),
                        item("现场审核", "NA", null)))));
        long secondId = second.path("id").asLong();
        call(post("/api/v1/sup/admissions/{id}/submit", secondId), admin, null);
        JsonNode approved = call(post("/api/v1/sup/admissions/{id}/approve", secondId), admin,
                json(Map.of("conclusion", "资料齐全，现场无重大不符合")));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("conclusion").asText()).contains("资料齐全");
        assertThat(approved.path("actualDate").asText()).isNotBlank();
        JsonNode qualified = call(get("/api/v1/sup/suppliers/{id}", supplierId), admin, null);
        assertThat(qualified.path("status").asText()).isEqualTo("QUALIFIED");

        // 9. 重复审批 → 409
        mockMvc.perform(post("/api/v1/sup/admissions/{id}/approve", secondId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 10. 列表/过滤/反查
        JsonNode all = call(get("/api/v1/sup/admissions?supplierId=" + supplierId), admin, null);
        assertThat(all.path("total").asInt()).isEqualTo(2);
        JsonNode byStatus = call(get("/api/v1/sup/admissions?supplierId=" + supplierId
                + "&status=APPROVED"), admin, null);
        assertThat(byStatus.path("total").asInt()).isEqualTo(1);
        assertThat(byStatus.path("items").get(0).path("passCount").asInt()).isEqualTo(2);
        assertThat(call(get("/api/v1/sup/suppliers/{id}/admissions", supplierId), admin, null))
                .hasSize(2);
        mockMvc.perform(get("/api/v1/sup/admissions?type=WRONG")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 11. 已通过后仍可再发起（期满复审），草稿可删
        JsonNode third = call(post("/api/v1/sup/admissions"), admin, json(body(supplierId,
                "SPECIAL", null, null, List.of(item("变更事项确认", "NA", null)))));
        long thirdId = third.path("id").asLong();
        call(delete("/api/v1/sup/admissions/{id}", thirdId), admin, null);
        assertThat(call(get("/api/v1/sup/admissions?supplierId=" + supplierId), admin, null)
                .path("total").asInt()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ 工具

    private Map<String, Object> body(long supplierId, String type, String plannedDate,
                                     String leader, List<Map<String, Object>> items) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplierId", supplierId);
        body.put("type", type);
        body.put("plannedDate", plannedDate);
        body.put("leader", leader);
        body.put("items", items);
        return body;
    }

    private Map<String, Object> item(String checkItem, String result, String evidence) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("checkItem", checkItem);
        item.put("result", result);
        if (evidence != null) {
            item.put("evidence", evidence);
        }
        return item;
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
