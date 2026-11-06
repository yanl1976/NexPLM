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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P6-1 工艺路线冒烟：主档 + 工序批量编辑 + 路线串 + 发布唯一性。
 *
 * <p>规则：只有草稿可改/可删；发布要求至少一道工序；发布时同零件其它已发布路线自动归档；
 * 工序号不可重复、工时不可为负。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CappSmokeTest {

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
    void routingLifecycleWithBatchSteps() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "工艺零件");

        JsonNode created = call(post("/api/v1/capp/routings"), admin,
                json(routing("车削工艺", itemId, steps(
                        step(10, "下料", "MACHINING", "下料车间", 0, 5),
                        step(20, "车削", "MACHINING", "机加车间", 30, 25),
                        step(30, "检验", "INSPECTION", "检验室", 0, 3)))));
        long routingId = created.path("id").asLong();
        assertThat(created.path("code").asText()).startsWith("RT-");
        assertThat(created.path("status").asText()).isEqualTo("DRAFT");
        assertThat(created.path("versionNo").asInt()).isEqualTo(1);
        assertThat(created.path("routeString").asText()).isEqualTo("10 下料 → 20 车削 → 30 检验");
        assertThat(created.path("totalMinutes").decimalValue()).isEqualByComparingTo("63");
        assertThat(created.path("steps").size()).isEqualTo(3);
        assertThat(created.path("objectCode").asText()).isNotBlank();

        // 批量编辑：整表替换为 2 道工序
        JsonNode updated = call(put("/api/v1/capp/routings/{id}", routingId), admin,
                json(routing("车削工艺V2", itemId, steps(
                        step(10, "下料", "MACHINING", "下料车间", 0, 5),
                        step(20, "精车", "MACHINING", "机加车间", 20, 15)))));
        assertThat(updated.path("name").asText()).isEqualTo("车削工艺V2");
        assertThat(updated.path("steps").size()).isEqualTo(2);
        assertThat(updated.path("routeString").asText()).isEqualTo("10 下料 → 20 精车");
        assertThat(updated.path("totalMinutes").decimalValue()).isEqualByComparingTo("40");

        // 工序号重复 → 400
        mockMvc.perform(put("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("重复工序号", itemId, steps(
                                step(10, "下料", "MACHINING", null, 0, 5),
                                step(10, "车削", "MACHINING", null, 0, 5))))))
                .andExpect(status().isBadRequest());

        // 未知工序类型 → 400
        mockMvc.perform(put("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("未知工序类型", itemId, steps(
                                step(10, "下料", "FLYING", null, 0, 5))))))
                .andExpect(status().isBadRequest());

        // 发布 → 已发布；再改 409
        JsonNode released = call(post("/api/v1/capp/routings/{id}/release", routingId), admin, null);
        assertThat(released.path("status").asText()).isEqualTo("RELEASED");
        assertThat(released.path("releasedAt").isNull()).isFalse();
        mockMvc.perform(put("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("已发布不许改", itemId, steps(
                                step(10, "下料", "MACHINING", null, 0, 5))))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 列表按零件过滤
        JsonNode list = call(get("/api/v1/capp/routings?objectId=" + itemId), admin, null);
        assertThat(list.path("total").asLong()).isEqualTo(1);
        JsonNode row = list.path("items").get(0);
        assertThat(row.path("stepCount").asInt()).isEqualTo(2);
        assertThat(row.path("totalMinutes").decimalValue()).isEqualByComparingTo("40");
        assertThat(row.path("status").asText()).isEqualTo("RELEASED");
    }

    @Test
    void releaseArchivesPreviousAndCopyKeepsSteps() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "双工艺零件");

        long firstId = call(post("/api/v1/capp/routings"), admin,
                json(routing("旧工艺", itemId, steps(step(10, "下料", "MACHINING", null, 0, 5)))))
                .path("id").asLong();
        call(post("/api/v1/capp/routings/{id}/release", firstId), admin, null);

        long secondId = call(post("/api/v1/capp/routings"), admin,
                json(routing("新工艺", itemId, steps(
                        step(10, "下料", "MACHINING", null, 0, 5),
                        step(20, "铣削", "MACHINING", null, 10, 20)))))
                .path("id").asLong();
        call(post("/api/v1/capp/routings/{id}/release", secondId), admin, null);

        // 同一零件只允许一条已发布路线：旧的自动归档
        assertThat(call(get("/api/v1/capp/routings/{id}", firstId), admin, null)
                .path("status").asText()).isEqualTo("ARCHIVED");
        assertThat(call(get("/api/v1/capp/routings/{id}", secondId), admin, null)
                .path("status").asText()).isEqualTo("RELEASED");

        // 复制为草稿：工序照搬、版本号 +1
        JsonNode copy = call(post("/api/v1/capp/routings/{id}/copy", secondId), admin, null);
        assertThat(copy.path("status").asText()).isEqualTo("DRAFT");
        assertThat(copy.path("steps").size()).isEqualTo(2);
        assertThat(copy.path("versionNo").asInt()).isEqualTo(2);
        assertThat(copy.path("id").asLong()).isNotEqualTo(secondId);

        // 草稿可删；删除后列表只剩 2 条
        call(delete("/api/v1/capp/routings/{id}", copy.path("id").asLong()), admin, null);
        assertThat(call(get("/api/v1/capp/routings?objectId=" + itemId), admin, null)
                .path("total").asLong()).isEqualTo(2);
    }

    /** P6-2：工艺分工（负责人）+ 工序号乱序后一键重建。 */
    @Test
    void resequenceAndCraftOwner() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "分工零件");

        Map<String, Object> body = routing("乱序工艺", itemId, steps(
                step(5, "下料", "MACHINING", null, 0, 2),
                step(7, "车削", "MACHINING", null, 0, 3),
                step(9, "检验", "INSPECTION", null, 0, 1)));
        body.put("ownerId", adminId);
        JsonNode created = call(post("/api/v1/capp/routings"), admin, json(body));
        long routingId = created.path("id").asLong();
        assertThat(created.path("ownerId").asLong()).isEqualTo(adminId);
        assertThat(created.path("ownerName").asText()).isNotBlank();

        JsonNode resequenced = call(post("/api/v1/capp/routings/{id}/resequence", routingId), admin, null);
        assertThat(resequenced.path("steps").size()).isEqualTo(3);
        assertThat(resequenced.path("steps").get(0).path("seqNo").asInt()).isEqualTo(10);
        assertThat(resequenced.path("steps").get(1).path("seqNo").asInt()).isEqualTo(20);
        assertThat(resequenced.path("steps").get(2).path("seqNo").asInt()).isEqualTo(30);
        // 顺序与工序名保持不变
        assertThat(resequenced.path("routeString").asText()).isEqualTo("10 下料 → 20 车削 → 30 检验");

        // 已发布的路线不允许重建序号
        call(post("/api/v1/capp/routings/{id}/release", routingId), admin, null);
        mockMvc.perform(post("/api/v1/capp/routings/{id}/resequence", routingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnknownObjectAndEmptySteps() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "校验零件");

        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("零件不存在", 999999999L,
                                steps(step(10, "下料", "MACHINING", null, 0, 1))))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("没有工序", itemId, List.of()))))
                .andExpect(status().isBadRequest());
    }

    /** P6-3：分类树 + 动态属性筛选 + 保存查询条件。 */
    @Test
    void resourceLibraryWithDynamicAttrsAndSavedQuery() throws Exception {
        String admin = login("admin", "Admin@12345");

        long toolClass = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TOOL", "name", "刀具"))).path("id").asLong();
        long turningClass = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TOOL-TURN", "name", "车刀", "parentId", toolClass))).path("id").asLong();

        JsonNode created = call(post("/api/v1/capp/resources"), admin, json(Map.of(
                "classId", turningClass,
                "code", "R-001",
                "name", "外圆车刀",
                "spec", "25×25",
                "unit", "把",
                "attrs", Map.of("品牌", "山特", "直径", "20"))));
        long resourceId = created.path("id").asLong();
        assertThat(created.path("className").asText()).isEqualTo("车刀");
        assertThat(created.path("attrs").path("品牌").asText()).isEqualTo("山特");

        // 按分类筛选
        assertThat(call(get("/api/v1/capp/resources?classId=" + turningClass), admin, null)
                .path("total").asLong()).isEqualTo(1);
        // 动态属性包含匹配（用 param 传原始 JSON；手写 %XX 在 MockMvc 里不会二次解码）
        assertThat(call(get("/api/v1/capp/resources").param("attrs", "{\"品牌\":\"山特\"}"), admin, null)
                .path("total").asLong()).isEqualTo(1);
        assertThat(call(get("/api/v1/capp/resources").param("attrs", "{\"品牌\":\"他人\"}"), admin, null)
                .path("total").asLong()).isZero();

        // 保存查询条件并按条件执行
        JsonNode saved = call(post("/api/v1/capp/resource-queries"), admin, json(Map.of(
                "name", "山特车刀", "classId", turningClass, "attrs", Map.of("品牌", "山特"))));
        assertThat(call(get("/api/v1/capp/resource-queries/" + saved.path("id").asLong() + "/run"), admin, null)
                .path("total").asLong()).isEqualTo(1);
        // 同名再保存 → 覆盖，不新增
        call(post("/api/v1/capp/resource-queries"), admin, json(Map.of("name", "山特车刀", "classId", turningClass)));
        assertThat(call(get("/api/v1/capp/resource-queries"), admin, null).size()).isEqualTo(1);

        // 分类下有资源时不允许删
        mockMvc.perform(delete("/api/v1/capp/resource-classes/{id}", turningClass)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 资源无引用可删，删完分类才能删
        call(delete("/api/v1/capp/resources/{id}", resourceId), admin, null);
        call(delete("/api/v1/capp/resource-classes/{id}", turningClass), admin, null);
        assertThat(call(get("/api/v1/capp/resources?classId=" + turningClass), admin, null)
                .path("total").asLong()).isZero();
    }

    /** P6-3：工序引用资源后禁止删除资源，引用统计可回溯到路线与工序。 */
    @Test
    void resourceReferenceBlocksDeletion() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "引用资源零件");
        long resourceClass = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TOOL-REF", "name", "引用刀具"))).path("id").asLong();
        long resourceId = call(post("/api/v1/capp/resources"), admin, json(Map.of(
                "classId", resourceClass, "code", "R-REF-01", "name", "切断刀", "unit", "把")))
                .path("id").asLong();

        Map<String, Object> step = step(10, "切断", "MACHINING", "下料车间", 0, 2);
        step.put("resourceId", resourceId);
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("引用资源工艺", itemId, steps(step)))).path("id").asLong();

        JsonNode detail = call(get("/api/v1/capp/routings/{id}", routingId), admin, null);
        assertThat(detail.path("steps").get(0).path("resourceId").asLong()).isEqualTo(resourceId);
        assertThat(detail.path("steps").get(0).path("resourceCode").asText()).isEqualTo("R-REF-01");

        JsonNode references = call(get("/api/v1/capp/resources/" + resourceId + "/references"), admin, null);
        assertThat(references.size()).isEqualTo(1);
        assertThat(references.get(0).path("routingId").asLong()).isEqualTo(routingId);
        assertThat(references.get(0).path("seqNo").asInt()).isEqualTo(10);
        assertThat(call(get("/api/v1/capp/resources/" + resourceId), admin, null)
                .path("referenceCount").asInt()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/capp/resources/{id}", resourceId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 删掉引用它的草稿路线后可删除
        call(delete("/api/v1/capp/routings/{id}", routingId), admin, null);
        call(delete("/api/v1/capp/resources/{id}", resourceId), admin, null);
    }

    // ------------------------------------------------------------------ 工具

    private Map<String, Object> routing(String name, long objectId, List<Map<String, Object>> steps) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("objectId", objectId);
        body.put("applicability", "通用");
        body.put("steps", steps);
        return body;
    }

    @SafeVarargs
    private List<Map<String, Object>> steps(Map<String, Object>... steps) {
        return List.of(steps);
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

    private long currentUserId(String token) throws Exception {
        return call(get("/api/v1/auth/me"), token, null).path("id").asLong();
    }

    private long createItem(String token, String name) throws Exception {
        return call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA")))
                .path("id").asLong();
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
