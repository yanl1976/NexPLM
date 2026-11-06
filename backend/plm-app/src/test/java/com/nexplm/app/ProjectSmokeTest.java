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
 * P7-1 项目与 EPS 冒烟：EPS 目录树 + 立项 + 状态机（草稿 → 执行中 → 暂停 → 结项）+ 成员。
 *
 * <p>规则：新建即草稿；状态只能经 activate/pause/close 迁移，保存接口不给改状态；
 * 只有草稿可删；已结项不可改；EPS 有子节点或项目时禁删。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectSmokeTest {

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
    void epsTreeAndProjectLifecycle() throws Exception {
        String admin = login("admin", "Admin@12345");
        long userId = currentUserId(admin);

        // 1. EPS：根 + 子节点，物化路径可查子树
        JsonNode root = call(post("/api/v1/prj/eps"), admin, json(Map.of(
                "code", "EPS-RD", "name", "研发中心", "sortNo", 10)));
        long rootId = root.path("id").asLong();
        assertThat(root.path("path").asText()).isEqualTo("/" + rootId + "/");
        assertThat(root.path("children")).isEmpty();

        JsonNode child = call(post("/api/v1/prj/eps"), admin, json(Map.of(
                "code", "EPS-RD-1", "name", "电传研发部", "parentId", rootId)));
        long childId = child.path("id").asLong();
        assertThat(child.path("path").asText()).isEqualTo("/" + rootId + "/" + childId + "/");

        JsonNode tree = call(get("/api/v1/prj/eps"), admin, null);
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).path("children")).hasSize(1);
        assertThat(tree.get(0).path("children").get(0).path("name").asText()).isEqualTo("电传研发部");

        // 2. 立项：状态草稿、编码 PJ-、项目经理回填
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("name", "某型号电传系统研制");
        createBody.put("epsId", childId);
        createBody.put("managerId", userId);
        createBody.put("type", "RD");
        createBody.put("priority", 1);
        createBody.put("planStart", "2026-03-01");
        createBody.put("planEnd", "2026-12-31");
        createBody.put("remark", "示范项目");
        createBody.put("members", List.of(Map.of("userId", userId, "role", "MANAGER")));
        JsonNode project = call(post("/api/v1/prj/projects"), admin, json(createBody));
        long projectId = project.path("id").asLong();
        assertThat(project.path("code").asText()).startsWith("PJ-");
        assertThat(project.path("status").asText()).isEqualTo("DRAFT");
        assertThat(project.path("epsName").asText()).isEqualTo("电传研发部");
        assertThat(project.path("managerName").asText()).isNotBlank();
        assertThat(project.path("progress").asInt()).isZero();
        assertThat(project.path("members")).hasSize(1);
        assertThat(project.path("members").get(0).path("role").asText()).isEqualTo("MANAGER");

        // 3. 校验：计划结束早于开始 400、进度越界 400、未知类型 400
        Map<String, Object> badDate = new HashMap<>(createBody);
        badDate.put("name", "计划日期反了");
        badDate.put("planStart", "2026-12-01");
        badDate.put("planEnd", "2026-01-01");
        mockMvc.perform(post("/api/v1/prj/projects")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badDate)))
                .andExpect(status().isBadRequest());

        Map<String, Object> badProgress = new HashMap<>(createBody);
        badProgress.put("name", "进度越界");
        badProgress.put("progress", 200);
        mockMvc.perform(post("/api/v1/prj/projects")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badProgress)))
                .andExpect(status().isBadRequest());

        Map<String, Object> badType = new HashMap<>(createBody);
        badType.put("name", "未知类型");
        badType.put("type", "FLYING");
        mockMvc.perform(post("/api/v1/prj/projects")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badType)))
                .andExpect(status().isBadRequest());

        // 4. 列表：按 EPS 子树筛选命中，按状态筛选互斥
        JsonNode bySubtree = call(get("/api/v1/prj/projects?epsId=" + rootId + "&includeSubtree=true"), admin, null);
        assertThat(bySubtree.path("total").asLong()).isEqualTo(1);
        JsonNode activeOnly = call(get("/api/v1/prj/projects?status=ACTIVE"), admin, null);
        assertThat(activeOnly.path("total").asLong()).isZero();
        // 关键字：中文走 param()（URL 模板里直接写中文会被 MockMvc 二次编码）
        JsonNode byKeyword = call(get("/api/v1/prj/projects").param("keyword", "电传系统"), admin, null);
        assertThat(byKeyword.path("total").asLong()).isEqualTo(1);

        // 5. 状态机：草稿 → 执行中（补实际开始日）→ 暂停 → 结项（补实际结束日）
        JsonNode activated = call(post("/api/v1/prj/projects/{id}/activate", projectId), admin, null);
        assertThat(activated.path("status").asText()).isEqualTo("ACTIVE");
        assertThat(activated.path("actualStart").asText()).isNotBlank();
        // 重复启动 → 409
        mockMvc.perform(post("/api/v1/prj/projects/{id}/activate", projectId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 执行中仍可改主档（只有结项后禁改）
        Map<String, Object> updateBody = new HashMap<>(createBody);
        updateBody.put("name", "某型号电传系统研制（一期）");
        updateBody.put("progress", 30);
        JsonNode updated = call(put("/api/v1/prj/projects/{id}", projectId), admin, json(updateBody));
        assertThat(updated.path("name").asText()).isEqualTo("某型号电传系统研制（一期）");
        assertThat(updated.path("progress").asInt()).isEqualTo(30);

        JsonNode paused = call(post("/api/v1/prj/projects/{id}/pause", projectId), admin, null);
        assertThat(paused.path("status").asText()).isEqualTo("PAUSED");

        JsonNode closed = call(post("/api/v1/prj/projects/{id}/close", projectId), admin, null);
        assertThat(closed.path("status").asText()).isEqualTo("CLOSED");
        assertThat(closed.path("actualEnd").asText()).isNotBlank();

        // 结项后：不可改、不可删
        mockMvc.perform(put("/api/v1/prj/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/prj/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 6. 草稿可删；删除后列表只剩已结项那条
        Map<String, Object> draftBody = new HashMap<>(createBody);
        draftBody.put("name", "临时草稿项目");
        long draftId = call(post("/api/v1/prj/projects"), admin, json(draftBody)).path("id").asLong();
        call(delete("/api/v1/prj/projects/{id}", draftId), admin, null);
        assertThat(call(get("/api/v1/prj/projects"), admin, null).path("total").asLong()).isEqualTo(1);

        // 7. EPS 删除保护：有子节点 409 / 有项目 409（子节点下挂着已结项项目）
        mockMvc.perform(delete("/api/v1/prj/eps/{id}", rootId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/prj/eps/{id}", childId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 8. 成员整表替换：清空后详情成员为空
        JsonNode cleared = call(put("/api/v1/prj/projects/{id}/members", projectId), admin, "[]");
        assertThat(cleared.path("members")).isEmpty();
    }

    // ------------------------------------------------------------------ 工具

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
