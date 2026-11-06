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
import java.util.ArrayList;
import java.util.HashMap;
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
 * P8-5-1 供应商评级冒烟：维度可配（权重合计 100）+ 打分算总分/等级 + 确认回写 + 快照不随方案变 + 暂停档联动。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupplierRatingSmokeTest {

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
    void ratingSchemeSnapshotAndSuspend() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 供应商（默认待准入）
        JsonNode supplier = call(post("/api/v1/sup/suppliers"), admin,
                json(Map.of("name", "评级测试供应商")));
        long supplierId = supplier.path("id").asLong();
        // 全局 non_null 序列化：等级为空时是"缺字段"而不是 null 节点
        assertThat(supplier.path("grade").isMissingNode()).isTrue();

        // 2. 新建方案：质量 40 / 交付 30 / 价格 20 / 服务 10，A>=90 B>=75 C>=60 D>=0，D 级自动暂停
        JsonNode scheme = call(post("/api/v1/sup/rating-schemes"), admin, json(schemeBody(
                List.of(band("A", 90), band("B", 75), band("C", 60), band("D", 0)), "D",
                List.of(dim("QUALITY", "质量", 40), dim("DELIVERY", "交付", 30),
                        dim("PRICE", "价格", 20), dim("SERVICE", "服务", 10)))));
        long schemeId = scheme.path("id").asLong();
        assertThat(scheme.path("code").asText()).startsWith("RATE-");
        assertThat(scheme.path("dims")).hasSize(4);
        assertThat(scheme.path("gradeRule").get(0).path("grade").asText()).isEqualTo("A");
        assertThat(scheme.path("suspendGrade").asText()).isEqualTo("D");
        Map<String, Long> dimIds = dimIds(scheme.path("dims"));

        // 3. 方案校验：权重合计不是 100 → 400；暂停档不在等级规则内 → 400；等级档位重复 → 400
        mockMvc.perform(post("/api/v1/sup/rating-schemes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(schemeBody(List.of(band("A", 90), band("D", 0)), "D",
                                List.of(dim("QUALITY", "质量", 40), dim("PRICE", "价格", 20))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/rating-schemes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(schemeBody(List.of(band("A", 90), band("D", 0)), "C",
                                List.of(dim("QUALITY", "质量", 60), dim("PRICE", "价格", 40))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/rating-schemes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(schemeBody(List.of(band("A", 90), band("A", 60)), "A",
                                List.of(dim("QUALITY", "质量", 100))))))
                .andExpect(status().isBadRequest());

        // 4. 新建评级草稿：90/80/70/60 → 36+24+14+6 = 80 → B；草稿不回写供应商
        Map<String, Object> ratingBody = ratingBody(supplierId, schemeId, "2026-Q4",
                scoreItems(dimIds, 90, 80, 70, 60));
        JsonNode draft = call(post("/api/v1/sup/ratings"), admin, json(ratingBody));
        long ratingId = draft.path("id").asLong();
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        assertThat(draft.path("totalScore").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(draft.path("grade").asText()).isEqualTo("B");
        assertThat(draft.path("items")).hasSize(4);
        JsonNode supplierAfterDraft = call(get("/api/v1/sup/suppliers/{id}", supplierId), admin,
                null);
        assertThat(supplierAfterDraft.path("grade").isMissingNode()).isTrue();
        assertThat(supplierAfterDraft.path("ratingScore").isMissingNode()).isTrue();

        // 5. 打分校验：漏维度 → 400；超满分 → 400；周期格式错 → 400；同周期重复 → 409
        Map<String, Object> missingDim = ratingBody(supplierId, schemeId, "2027-Q1",
                scoreItems(dimIds, 90, 80, 70, 60));
        missingDim.put("items", scoreItems(dimIds, 90, 80, 70, 60).subList(0, 3));
        mockMvc.perform(post("/api/v1/sup/ratings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(missingDim)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/ratings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ratingBody(supplierId, schemeId, "2027-Q1",
                                scoreItems(dimIds, 120, 80, 70, 60)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/ratings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ratingBody(supplierId, schemeId, "2026-13",
                                scoreItems(dimIds, 90, 80, 70, 60)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/ratings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ratingBody(supplierId, schemeId, "2026-Q4",
                                scoreItems(dimIds, 80, 80, 80, 80)))))
                .andExpect(status().isConflict());

        // 6. 改草稿：全 95 → 95 → A
        JsonNode updated = call(put("/api/v1/sup/ratings/{id}", ratingId), admin,
                json(ratingBody(supplierId, schemeId, "2026-Q4",
                        scoreItems(dimIds, 95, 95, 95, 95))));
        assertThat(updated.path("totalScore").decimalValue()).isEqualByComparingTo("95.00");
        assertThat(updated.path("grade").asText()).isEqualTo("A");

        // 7. 确认：回写供应商等级/分数；状态不被评级改成 QUALIFIED
        JsonNode confirmed = call(post("/api/v1/sup/ratings/{id}/confirm", ratingId), admin, null);
        assertThat(confirmed.path("status").asText()).isEqualTo("CONFIRMED");
        JsonNode supplierAfterConfirm = call(get("/api/v1/sup/suppliers/{id}", supplierId), admin,
                null);
        assertThat(supplierAfterConfirm.path("grade").asText()).isEqualTo("A");
        assertThat(supplierAfterConfirm.path("ratingScore").decimalValue())
                .isEqualByComparingTo("95.00");
        assertThat(supplierAfterConfirm.path("status").asText()).isEqualTo("PENDING");

        // 8. 幂等与留痕：重复确认 → 409；已确认不可改 → 409；已确认不可删 → 409
        mockMvc.perform(post("/api/v1/sup/ratings/{id}/confirm", ratingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/sup/ratings/{id}", ratingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ratingBody(supplierId, schemeId, "2026-Q4",
                                scoreItems(dimIds, 60, 60, 60, 60)))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/sup/ratings/{id}", ratingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 9. 方案改维度（质量 50 / 交付 30 / 价格 10 / 服务 10）→ 历史评级仍是旧快照
        call(put("/api/v1/sup/rating-schemes/{id}", schemeId), admin, json(schemeBody(
                List.of(band("A", 90), band("B", 75), band("C", 60), band("D", 0)), "D",
                List.of(dim("QUALITY", "质量", 50), dim("DELIVERY", "交付", 30),
                        dim("PRICE", "价格", 10), dim("SERVICE", "服务", 10)))));
        JsonNode history = call(get("/api/v1/sup/ratings/{id}", ratingId), admin, null);
        assertThat(history.path("items").get(0).path("weight").asInt()).isEqualTo(40);
        assertThat(history.path("totalScore").decimalValue()).isEqualByComparingTo("95.00");
        Map<String, Long> newDimIds = dimIds(
                call(get("/api/v1/sup/rating-schemes/{id}", schemeId), admin, null).path("dims"));

        // 10. 新周期：全 50 → 50 → D → 确认后供应商被暂停
        JsonNode low = call(post("/api/v1/sup/ratings"), admin, json(ratingBody(supplierId,
                schemeId, "2026-Q3", scoreItems(newDimIds, 50, 50, 50, 50))));
        assertThat(low.path("grade").asText()).isEqualTo("D");
        call(post("/api/v1/sup/ratings/{id}/confirm", low.path("id").asLong()), admin, null);
        JsonNode suspended = call(get("/api/v1/sup/suppliers/{id}", supplierId), admin, null);
        assertThat(suspended.path("status").asText()).isEqualTo("SUSPENDED");
        assertThat(suspended.path("grade").asText()).isEqualTo("D");

        // 11. 列表/过滤/反查：2 条评级；按周期与状态过滤；方案已被引用不能删
        JsonNode all = call(get("/api/v1/sup/ratings?supplierId=" + supplierId), admin, null);
        assertThat(all.path("total").asInt()).isEqualTo(2);
        JsonNode byPeriod = call(
                get("/api/v1/sup/ratings?supplierId=" + supplierId + "&period=2026-Q4"), admin,
                null);
        assertThat(byPeriod.path("total").asInt()).isEqualTo(1);
        assertThat(byPeriod.path("items").get(0).path("status").asText()).isEqualTo("CONFIRMED");
        assertThat(call(get("/api/v1/sup/suppliers/{id}/ratings", supplierId), admin, null))
                .hasSize(2);
        mockMvc.perform(delete("/api/v1/sup/rating-schemes/{id}", schemeId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 12. 草稿可删；方案停用后不能发起新评级
        call(put("/api/v1/sup/rating-schemes/{id}", schemeId), admin, json(schemeBody(
                List.of(band("A", 90), band("D", 0)), "D",
                List.of(dim("QUALITY", "质量", 100)), "INACTIVE")));
        mockMvc.perform(post("/api/v1/sup/ratings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ratingBody(supplierId, schemeId, "2026-Q2",
                                scoreItems(newDimIds, 90, 90, 90, 90)))))
                .andExpect(status().isConflict());
        long draftCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sup_rate WHERE supplier_id = ? AND status = 'DRAFT'",
                Long.class, supplierId);
        assertThat(draftCount).isZero();
    }

    // ------------------------------------------------------------------ 工具

    private Map<String, Object> schemeBody(List<Map<String, Object>> gradeRule,
                                           String suspendGrade,
                                           List<Map<String, Object>> dims) {
        return schemeBody(gradeRule, suspendGrade, dims, "ACTIVE");
    }

    private Map<String, Object> schemeBody(List<Map<String, Object>> gradeRule,
                                           String suspendGrade,
                                           List<Map<String, Object>> dims, String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "供应商季度评级");
        body.put("gradeRule", gradeRule);
        body.put("suspendGrade", suspendGrade);
        body.put("dims", dims);
        body.put("status", status);
        return body;
    }

    private Map<String, Object> band(String grade, int min) {
        return Map.of("grade", grade, "min", min);
    }

    private Map<String, Object> dim(String code, String name, int weight) {
        Map<String, Object> dim = new LinkedHashMap<>();
        dim.put("code", code);
        dim.put("name", name);
        dim.put("weight", weight);
        return dim;
    }

    private Map<String, Object> ratingBody(long supplierId, long schemeId, String period,
                                           List<Map<String, Object>> items) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplierId", supplierId);
        body.put("schemeId", schemeId);
        body.put("period", period);
        body.put("items", items);
        return body;
    }

    /** 按方案维度顺序打分：顺序为 质量/交付/价格/服务。 */
    private List<Map<String, Object>> scoreItems(Map<String, Long> dimIds, int... scores) {
        List<Map<String, Object>> items = new ArrayList<>();
        String[] codes = {"QUALITY", "DELIVERY", "PRICE", "SERVICE"};
        for (int i = 0; i < scores.length && i < codes.length; i++) {
            Long dimId = dimIds.get(codes[i]);
            if (dimId == null) {
                continue;
            }
            items.add(Map.of("dimId", dimId, "score", scores[i]));
        }
        return items;
    }

    private Map<String, Long> dimIds(JsonNode dims) {
        Map<String, Long> result = new HashMap<>();
        for (JsonNode dim : dims) {
            result.put(dim.path("code").asText(), dim.path("id").asLong());
        }
        return result;
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
