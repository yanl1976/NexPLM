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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P10-2 自定义看板冒烟：白名单数据集、布局校验、预览数据、编码唯一、软删。
 *
 * <p>重点不是"能存能取"，而是<b>非法输入一律拒绝</b>：数据集必须来自服务端白名单
 * （这是本功能唯一的安全边界），组件类型/宽度/条数/取前 N 都要在服务端收口。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class BoardSmokeTest {

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
    void boardCrudValidationAndPreview() throws Exception {
        String admin = login("admin", "Admin@12345");
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);

        // 样本：2 个文档（1 草稿 + 1 已发布）、1 条执行中变更
        insertObject(92_000_000_001L, "PdmDoc", "DOC-910001", "草稿文档", companyId, "Draft");
        insertObject(92_000_000_002L, "PdmDoc", "DOC-910002", "已发布文档", companyId, "Released");
        jdbcTemplate.update("""
                        INSERT INTO chg_ecr (id, company_id, code, title, change_type, priority,
                                             status, requester_id)
                        VALUES (?, ?, ?, ?, 'DESIGN', 'HIGH', 'EXECUTING', ?)
                        """,
                92_000_000_011L, companyId, "ECR-910001", "执行中变更", 1L);

        // ---- 1. 数据集字典来自服务端白名单
        JsonNode datasets = call(get("/api/v1/ana/boards/datasets"), admin, null);
        assertThat(datasets).isNotEmpty();
        assertThat(datasets.findValuesAsText("code")).contains("doc.byState", "change.byStatus");

        // ---- 2. 新建：组件 id 缺失由后端补齐，order 按提交顺序重排，标题缺省取数据集名
        JsonNode created = call(post("/api/v1/ana/boards"), admin, json(Map.of(
                "code", "BOARD-001",
                "name", "研发概览",
                "description", "文档与变更",
                "shared", true,
                "components", List.of(
                        Map.of("type", "bar", "dataset", "doc.byState", "span", 2, "top", 10),
                        Map.of("type", "metric", "dataset", "change.byStatus",
                                "title", "变更总数", "span", 1)))));
        long boardId = created.path("id").asLong();
        assertThat(created.path("code").asText()).isEqualTo("BOARD-001");
        assertThat(created.path("shared").asBoolean()).isTrue();
        assertThat(created.path("components")).hasSize(2);
        JsonNode first = created.path("components").get(0);
        assertThat(first.path("id").asText()).isNotBlank();
        assertThat(first.path("order").asInt()).isZero();
        assertThat(first.path("title").asText()).isEqualTo("文档按状态");
        assertThat(first.path("top").asInt()).isEqualTo(10);
        assertThat(created.path("components").get(1).path("order").asInt()).isEqualTo(1);
        assertThat(created.path("components").get(1).path("title").asText()).isEqualTo("变更总数");

        // ---- 3. 预览：按组件返回各自数据集结果，total 为分组计数之和
        JsonNode data = call(get("/api/v1/ana/boards/{id}/data", boardId), admin, null);
        assertThat(data.path("components")).hasSize(2);
        JsonNode docBlock = data.path("components").get(0);
        assertThat(docBlock.path("domain").asText()).isEqualTo("doc");
        assertThat(docBlock.path("rows")).hasSize(2);
        assertThat(docBlock.path("total").asLong()).isEqualTo(2);
        JsonNode changeBlock = data.path("components").get(1);
        assertThat(changeBlock.path("domain").asText()).isEqualTo("change");
        assertThat(changeBlock.path("total").asLong()).isEqualTo(1);
        assertThat(changeBlock.path("rows").get(0).path("key").asText()).isEqualTo("EXECUTING");

        // ---- 4. 列表能看到组件数
        JsonNode page = call(get("/api/v1/ana/boards?keyword=BOARD-001"), admin, null);
        assertThat(page.path("total").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("componentCount").asInt()).isEqualTo(2);

        // ---- 5. 校验：未知数据集 / 空组件 / 非法类型 / 越界 span 与 top 一律 400
        expectBadRequest(post("/api/v1/ana/boards"), admin, Map.of(
                "code", "BOARD-BAD-1", "name", "未知数据集",
                "components", List.of(Map.of("type", "bar", "dataset", "raw.sql"))));
        expectBadRequest(post("/api/v1/ana/boards"), admin, Map.of(
                "code", "BOARD-BAD-2", "name", "空组件", "components", List.of()));
        expectBadRequest(post("/api/v1/ana/boards"), admin, Map.of(
                "code", "BOARD-BAD-3", "name", "非法类型",
                "components", List.of(Map.of("type", "pie", "dataset", "doc.byState"))));
        expectBadRequest(post("/api/v1/ana/boards"), admin, Map.of(
                "code", "BOARD-BAD-4", "name", "越界宽度",
                "components", List.of(Map.of("type", "bar", "dataset", "doc.byState", "span", 5))));
        expectBadRequest(post("/api/v1/ana/boards"), admin, Map.of(
                "code", "BOARD-BAD-5", "name", "越界条数",
                "components", List.of(Map.of("type", "bar", "dataset", "doc.byState", "top", 1000))));

        // ---- 6. 编码唯一：同编码再建一次 409
        mockMvc.perform(post("/api/v1/ana/boards")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "BOARD-001", "name", "重复",
                                "components", List.of(
                                        Map.of("type", "bar", "dataset", "doc.byState"))))))
                .andExpect(status().isConflict());

        // ---- 7. 修改：整体覆盖布局（组件减少为 1 个）
        JsonNode updated = call(put("/api/v1/ana/boards/{id}", boardId), admin, json(Map.of(
                "code", "BOARD-001", "name", "研发概览 v2",
                "components", List.of(
                        Map.of("type", "table", "dataset", "doc.byState", "title", "文档状态明细")))));
        assertThat(updated.path("name").asText()).isEqualTo("研发概览 v2");
        assertThat(updated.path("components")).hasSize(1);
        assertThat(updated.path("components").get(0).path("type").asText()).isEqualTo("table");

        // ---- 8. 软删后列表为空、详情 404、预览 404
        call(delete("/api/v1/ana/boards/{id}", boardId), admin, null);
        JsonNode afterDelete = call(get("/api/v1/ana/boards?keyword=BOARD-001"), admin, null);
        assertThat(afterDelete.path("total").asInt()).isZero();
        mockMvc.perform(get("/api/v1/ana/boards/{id}", boardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ana/boards/{id}/data", boardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ 工具

    private void insertObject(long id, String objectType, String code, String name, Long companyId,
                              String state) {
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                                lifecycle_state, is_latest, deleted)
                        VALUES (?, ?, ?, ?, ?, ?, ?, true, false)
                        """,
                id, objectType, id, code, name, companyId, state);
    }

    private void expectBadRequest(MockHttpServletRequestBuilder builder, String token,
                                  Object payload) throws Exception {
        mockMvc.perform(builder.header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isBadRequest());
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
