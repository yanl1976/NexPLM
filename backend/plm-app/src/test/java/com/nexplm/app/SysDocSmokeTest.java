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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P8-3-1 体系文件冒烟：按模板生成 → 审批发布（P8 DoD）→ 修订再发布 → 作废。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysDocSmokeTest {

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
    void templateGenerateApproveRevise() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 文件夹两级：QP（程序文件）→ QP/WI（作业指导书）
        JsonNode root = call(post("/api/v1/qlt/sys-doc-folders"), admin,
                json(Map.of("code", "QP", "name", "程序文件")));
        long rootId = root.path("id").asLong();
        assertThat(root.path("path").asText()).isEqualTo("/QP");
        JsonNode child = call(post("/api/v1/qlt/sys-doc-folders"), admin,
                json(Map.of("code", "WI", "name", "作业指导书", "parentId", rootId)));
        long childId = child.path("id").asLong();
        assertThat(child.path("path").asText()).isEqualTo("/QP/WI");

        // 文件夹编码不可改
        mockMvc.perform(put("/api/v1/qlt/sys-doc-folders/{id}", rootId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "QP2", "name", "程序文件"))))
                .andExpect(status().isBadRequest());

        // 2. 模板：3 个章节，其中"适用范围"与"职责"必填
        JsonNode template = call(post("/api/v1/qlt/sys-doc-templates"), admin, json(Map.of(
                "name", "程序文件模板",
                "docLevel", "PROCEDURE",
                "folderId", childId,
                "defaultReviewMonths", 12,
                "sections", List.of(
                        Map.of("key", "S1", "title", "目的", "required", true),
                        Map.of("key", "S2", "title", "适用范围", "required", true),
                        Map.of("key", "S3", "title", "相关记录", "required", false)))));
        long templateId = template.path("id").asLong();
        assertThat(template.path("code").asText()).startsWith("TPL-");
        assertThat(template.path("sections")).hasSize(3);
        assertThat(template.path("folderName").asText()).isEqualTo("作业指导书");

        // 章节编号重复 → 400
        mockMvc.perform(post("/api/v1/qlt/sys-doc-templates")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "坏模板", "sections", List.of(
                                Map.of("key", "S1", "title", "A"),
                                Map.of("key", "s1", "title", "B"))))))
                .andExpect(status().isBadRequest());

        // 3. 按模板生成：层级/文件夹/评审周期/章节骨架都来自模板
        JsonNode doc = call(post("/api/v1/qlt/sys-docs/from-template"), admin, json(Map.of(
                "templateId", templateId, "code", "QP-01", "name", "不合格品控制程序")));
        long docId = doc.path("id").asLong();
        assertThat(doc.path("status").asText()).isEqualTo("DRAFT");
        assertThat(doc.path("versionNo").asInt()).isEqualTo(1);
        assertThat(doc.path("docLevel").asText()).isEqualTo("PROCEDURE");
        assertThat(doc.path("folderId").asLong()).isEqualTo(childId);
        assertThat(doc.path("reviewMonths").asInt()).isEqualTo(12);
        assertThat(doc.path("sections")).hasSize(3);
        assertThat(doc.path("sections").get(0).path("required").asBoolean()).isTrue();
        assertThat(doc.path("versions")).isEmpty();

        // 文件编号重复 → 400
        mockMvc.perform(post("/api/v1/qlt/sys-docs/from-template")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("templateId", templateId, "code", "QP-01",
                                "name", "重复编号"))))
                .andExpect(status().isBadRequest());

        // 4. 必填章节没内容 → 提交审批 400
        mockMvc.perform(post("/api/v1/qlt/sys-docs/{id}/submit", docId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 5. 补内容 → 提交 → SUBMITTED；提交后不能改
        call(put("/api/v1/qlt/sys-docs/{id}", docId), admin, json(Map.of(
                "code", "QP-01", "name", "不合格品控制程序", "docLevel", "PROCEDURE",
                "folderId", childId, "reviewMonths", 12,
                "sections", List.of(
                        section("S1", "目的", "规范不合格品处置流程", true),
                        section("S2", "适用范围", "适用于本公司所有不合格品", true),
                        section("S3", "相关记录", "", false)))));
        JsonNode submitted = call(post("/api/v1/qlt/sys-docs/{id}/submit", docId), admin, null);
        assertThat(submitted.path("status").asText()).isEqualTo("SUBMITTED");
        mockMvc.perform(put("/api/v1/qlt/sys-docs/{id}", docId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "QP-01", "name", "改个名"))))
                .andExpect(status().isConflict());

        // 6. 驳回必须写原因；驳回退回草稿
        mockMvc.perform(post("/api/v1/qlt/sys-docs/{id}/approve", docId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("passed", false))))
                .andExpect(status().isBadRequest());
        JsonNode rejected = call(post("/api/v1/qlt/sys-docs/{id}/approve", docId), admin,
                json(Map.of("passed", false, "note", "职责章节缺失")));
        assertThat(rejected.path("status").asText()).isEqualTo("DRAFT");
        assertThat(rejected.path("approveNote").asText()).contains("职责");

        // 7. 再次提交 → 审批通过 → 发布：版本快照 + 生效日 + 下次评审日
        call(post("/api/v1/qlt/sys-docs/{id}/submit", docId), admin, null);
        JsonNode released = call(post("/api/v1/qlt/sys-docs/{id}/approve", docId), admin,
                json(Map.of("passed", true, "note", "符合要求")));
        assertThat(released.path("status").asText()).isEqualTo("RELEASED");
        assertThat(released.path("effectiveDate").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(released.path("nextReviewDate").asText())
                .isEqualTo(LocalDate.now().plusMonths(12).toString());
        assertThat(released.path("versions")).hasSize(1);
        assertThat(released.path("versions").get(0).path("versionNo").asInt()).isEqualTo(1);
        assertThat(released.path("versions").get(0).path("changeNote").asText()).isEqualTo("首次发布");

        // 8. 修订 → 草稿 v2；改内容后重新发布 → 版本累积
        JsonNode revised = call(post("/api/v1/qlt/sys-docs/{id}/revise", docId), admin,
                json(Map.of("changeNote", "补充让步接收条款")));
        assertThat(revised.path("status").asText()).isEqualTo("DRAFT");
        assertThat(revised.path("versionNo").asInt()).isEqualTo(2);
        // 序列化配置为 non_null，字段为空时是"缺字段"而不是 null 节点
        assertThat(revised.path("effectiveDate").asText("")).isEmpty();
        call(put("/api/v1/qlt/sys-docs/{id}", docId), admin, json(Map.of(
                "code", "QP-01", "name", "不合格品控制程序", "docLevel", "PROCEDURE",
                "folderId", childId, "reviewMonths", 12,
                "sections", List.of(
                        section("S1", "目的", "规范不合格品处置流程（含让步接收）", true),
                        section("S2", "适用范围", "适用于本公司所有不合格品", true),
                        section("S3", "相关记录", "《不合格品处置单》", false)))));
        call(post("/api/v1/qlt/sys-docs/{id}/submit", docId), admin, null);
        JsonNode v2 = call(post("/api/v1/qlt/sys-docs/{id}/approve", docId), admin,
                json(Map.of("passed", true, "note", "同意改版")));
        assertThat(v2.path("versions")).hasSize(2);
        assertThat(v2.path("versions").get(0).path("versionNo").asInt()).isEqualTo(2);
        assertThat(v2.path("versions").get(0).path("changeNote").asText()).contains("让步接收");

        // 9. 列表：按文件夹子树过滤（挂在 QP/WI 下的文件用根 QP 也能查到）
        JsonNode byRoot = call(get("/api/v1/qlt/sys-docs?folderId=" + rootId), admin, null);
        assertThat(byRoot.path("total").asInt()).isEqualTo(1);
        JsonNode byStatus = call(get("/api/v1/qlt/sys-docs?status=RELEASED"), admin, null);
        assertThat(byStatus.path("total").asInt()).isEqualTo(1);

        // 10. 到期评审：把下次评审日改成 10 天前 → 命中；刚发布时（12 个月后）不命中
        assertThat(call(get("/api/v1/qlt/sys-docs/due-review?days=30"), admin, null)).isEmpty();
        jdbcTemplate.update("UPDATE sys_doc SET next_review_date = CURRENT_DATE - 10 WHERE id = ?",
                docId);
        JsonNode due = call(get("/api/v1/qlt/sys-docs/due-review?days=30"), admin, null);
        assertThat(due).hasSize(1);
        assertThat(due.get(0).path("dueReview").asBoolean()).isTrue();
        JsonNode listed = call(get("/api/v1/qlt/sys-docs?dueReviewOnly=true"), admin, null);
        assertThat(listed.path("total").asInt()).isEqualTo(1);

        // 11. 作废 → OBSOLETE；作废后不能再修订；有文件的文件夹不能删
        JsonNode obsolete = call(post("/api/v1/qlt/sys-docs/{id}/obsolete", docId), admin, null);
        assertThat(obsolete.path("status").asText()).isEqualTo("OBSOLETE");
        assertThat(obsolete.path("obsoleteAt").isNull()).isFalse();
        mockMvc.perform(post("/api/v1/qlt/sys-docs/{id}/revise", docId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("changeNote", "作废后还想改"))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/qlt/sys-doc-folders/{id}", childId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 12. 删除模板：已生成的文件与版本历史不受影响
        call(delete("/api/v1/qlt/sys-doc-templates/{id}", templateId), admin, null);
        JsonNode afterDelete = call(get("/api/v1/qlt/sys-docs/{id}", docId), admin, null);
        assertThat(afterDelete.path("sections")).hasSize(3);
        assertThat(afterDelete.path("versions")).hasSize(2);
        assertThat(call(get("/api/v1/qlt/sys-doc-templates"), admin, null)).isEmpty();
    }

    // ------------------------------------------------------------------ 工具

    private static Map<String, Object> section(String key, String title, String content,
                                               boolean required) {
        return Map.of("key", key, "title", title, "content", content, "required", required);
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
