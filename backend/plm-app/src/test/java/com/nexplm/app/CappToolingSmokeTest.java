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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 C-3/C-4｜工装管理（台账 / 申请单 / 借用占用 / 工序挂工装）。
 *
 * <p>对齐旧系统 {@code CAPP/Tooling}：{@code ToolingListView}（台账）、
 * {@code ToolingApplyListView} + {@code CreateToolingAfterToolingApplyPublishedPage}（申请单批准后建台账）、
 * {@code WorlflowPlaceOnResourcePage}（工装挂到工序）、{@code ToolingImpactAnalysis}（引用分析）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CappToolingSmokeTest {

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
    void toolingLedgerApplyBorrowAndStepReference() throws Exception {
        String admin = login();

        // 1. 台账：编码留空按规则 CappTooling 取号（TL-xxxxxx）
        long toolingId = call(post("/api/v1/capp/toolings"), admin, json(Map.of(
                "name", "缸体夹具", "toolingType", "FIXTURE", "spec", "CB-100",
                "location", "A 库 3 号架", "purchaseDate", "2026-01-10"))).asLong();
        String toolingCode = call(get("/api/v1/capp/toolings"), admin, null)
                .get(0).path("code").asText();
        assertThat(toolingCode).startsWith("TL-");
        assertThat(call(get("/api/v1/capp/toolings"), admin, null).get(0).path("status").asText())
                .isEqualTo("IDLE");

        // 2. 改台账（类型 + 状态）
        call(put("/api/v1/capp/toolings/{id}", toolingId), admin, json(Map.of(
                "name", "缸体夹具（改）", "toolingType", "GAUGE", "status", "MAINTAIN")));
        JsonNode listed = call(get("/api/v1/capp/toolings").param("toolingType", "GAUGE"), admin, null);
        assertThat(listed.size()).isEqualTo(1);
        assertThat(listed.get(0).path("name").asText()).isEqualTo("缸体夹具（改）");
        assertThat(listed.get(0).path("status").asText()).isEqualTo("MAINTAIN");
        assertThat(call(get("/api/v1/capp/toolings").param("toolingType", "MOLD"), admin, null).size())
                .isZero();

        // 3. 申请单：草稿 → 提交 → 批准，批准时自动生成台账
        long applyId = call(post("/api/v1/capp/tooling-applies"), admin, json(Map.of(
                "toolingType", "MOLD", "toolingName", "注塑模具", "spec", "MJ-9",
                "reason", "新品试制"))).asLong();
        assertThat(call(get("/api/v1/capp/tooling-applies"), admin, null)
                .get(0).path("status").asText()).isEqualTo("DRAFT");
        // 草稿不能直接批准
        mockMvc.perform(post("/api/v1/capp/tooling-applies/{id}/approve", applyId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approved", true))))
                .andExpect(status().isBadRequest());
        call(post("/api/v1/capp/tooling-applies/{id}/submit", applyId), admin, null);
        JsonNode approved = call(post("/api/v1/capp/tooling-applies/{id}/approve", applyId), admin,
                json(Map.of("approved", true, "comment", "同意开模")));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        long applyToolingId = approved.path("toolingId").asLong();
        assertThat(applyToolingId).isPositive();
        JsonNode applyRow = call(get("/api/v1/capp/tooling-applies"), admin, null).get(0);
        assertThat(applyRow.path("tooling_code").asText()).startsWith("TL-");
        assertThat(call(get("/api/v1/capp/toolings"), admin, null).size()).isEqualTo(2);

        // 4. 借出 → 台账 IN_USE → 借用中禁止删除 → 归还回 IDLE
        long borrowId = call(post("/api/v1/capp/tooling-borrows"), admin, json(Map.of(
                "toolingId", applyToolingId, "remark", "试模"))).asLong();
        mockMvc.perform(delete("/api/v1/capp/toolings/{id}", applyToolingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        JsonNode borrowRows = call(get("/api/v1/capp/tooling-borrows"), admin, null);
        assertThat(borrowRows.size()).isEqualTo(1);
        // 未归还时 returned_at 为空（全局 non_null 序列化下该键可能直接缺失，用 hasNonNull 判定）
        assertThat(borrowRows.get(0).hasNonNull("returned_at")).isFalse();
        call(post("/api/v1/capp/tooling-borrows/{id}/return", borrowId), admin, null);
        assertThat(call(get("/api/v1/capp/tooling-borrows"), admin, null)
                .get(0).hasNonNull("returned_at")).isTrue();

        // 5. 工序挂工装：详情回带工装编码，影响分析查得到引用
        long itemId = createItem(admin, "工装引用零件");
        Map<String, Object> step = step(10, "镗孔", "MACHINING", "机加车间", 0.5, 2);
        step.put("toolingId", toolingId);
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("工装引用工艺", itemId, List.of(step)))).path("id").asLong();
        JsonNode routingDetail = call(get("/api/v1/capp/routings/{id}", routingId), admin, null);
        assertThat(routingDetail.path("steps").get(0).path("toolingId").asLong()).isEqualTo(toolingId);
        assertThat(routingDetail.path("steps").get(0).path("toolingCode").asText()).isEqualTo(toolingCode);
        assertThat(call(get("/api/v1/capp/routings/{id}/process-tree", routingId), admin, null)
                .get(0).path("toolingCode").asText()).isEqualTo(toolingCode);

        JsonNode usages = call(get("/api/v1/capp/toolings/" + toolingId + "/usages"), admin, null);
        assertThat(usages.size()).isEqualTo(1);
        assertThat(usages.get(0).path("routing_code").asText()).startsWith("RT-");
        assertThat(usages.get(0).path("step_no").asInt()).isEqualTo(10);

        // 6. 被工序引用 → 禁删
        mockMvc.perform(delete("/api/v1/capp/toolings/{id}", toolingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 7. 报废工装不能挂到工序上
        call(put("/api/v1/capp/toolings/{id}", toolingId), admin, json(Map.of(
                "name", "缸体夹具（改）", "toolingType", "GAUGE", "status", "SCRAP")));
        Map<String, Object> scrapStep = step(10, "镗孔", "MACHINING", "机加车间", 0.5, 2);
        scrapStep.put("toolingId", toolingId);
        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("报废工装工艺", itemId, List.of(scrapStep)))))
                .andExpect(status().isBadRequest());

        // 8. 解除引用（草稿路线去掉工装）后可删除
        Map<String, Object> freeStep = step(10, "镗孔", "MACHINING", "机加车间", 0.5, 2);
        call(put("/api/v1/capp/routings/{id}", routingId), admin,
                json(routing("工装引用工艺", itemId, List.of(freeStep))));
        assertThat(call(get("/api/v1/capp/toolings/" + toolingId + "/usages"), admin, null).size()).isZero();
        call(delete("/api/v1/capp/toolings/{id}", toolingId), admin, null);
        assertThat(call(get("/api/v1/capp/toolings"), admin, null).size()).isEqualTo(1);
    }

    // ------------------------------------------------------------ 工具

    private Map<String, Object> routing(String name, long objectId, List<Map<String, Object>> steps) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("objectId", objectId);
        body.put("steps", steps);
        return body;
    }

    private Map<String, Object> step(int seqNo, String name, String opType, String workCenter,
                                     Number setupMinutes, Number runMinutes) {
        Map<String, Object> step = new HashMap<>();
        step.put("seqNo", seqNo);
        step.put("name", name);
        step.put("opType", opType);
        step.put("workCenter", workCenter);
        step.put("setupMinutes", setupMinutes);
        step.put("runMinutes", runMinutes);
        return step;
    }

    private long createItem(String token, String name) throws Exception {
        return call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA")))
                .path("id").asLong();
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
