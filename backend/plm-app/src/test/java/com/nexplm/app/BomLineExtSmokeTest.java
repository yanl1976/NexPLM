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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-CORE-03：BOM 行扩展（蓝本 {@code PSEBOMLINE} 的数量四兄弟 / BOM 类型 / 工序路线 / 数据来源）。
 *
 * <p>覆盖：显式值往返、缺省值（`bomType=DESIGN`、`totalQty` 为空表示按层级累乘）、负数拒绝。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class BomLineExtSmokeTest {

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
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void bomLineCarriesExtendedColumns() throws Exception {
        String admin = login();
        long parentId = createItem(admin, "BOM 父件");
        long childId = createItem(admin, "BOM 子件");

        JsonNode line = call(post("/api/v1/items/{id}/bom", parentId), admin, body(
                "childId", String.valueOf(childId),
                "qty", 3,
                "totalQty", 12,
                "surplusQty", 10,
                "subUseQty", 2,
                "bomType", "quote",
                "itemRoute", "R10",
                "dataSource", "IMPORT"));
        assertThat(line.path("bomType").asText()).isEqualTo("QUOTE");
        assertThat(line.path("totalQty").decimalValue()).isEqualByComparingTo("12");
        assertThat(line.path("surplusQty").decimalValue()).isEqualByComparingTo("10");
        assertThat(line.path("subUseQty").decimalValue()).isEqualByComparingTo("2");
        assertThat(line.path("itemRoute").asText()).isEqualTo("R10");
        assertThat(line.path("dataSource").asText()).isEqualTo("IMPORT");

        // 树接口同样带出扩展列
        JsonNode tree = call(get("/api/v1/items/{id}/bom", parentId), admin, null);
        JsonNode first = tree.get(0);
        assertThat(first.path("bomType").asText()).isEqualTo("QUOTE");
        assertThat(first.path("totalQty").decimalValue()).isEqualByComparingTo("12");

        // 缺省：只给 qty 时 bomType=DESIGN、totalQty 为空（按层级累乘）
        long anotherChild = createItem(admin, "BOM 子件2");
        JsonNode minimal = call(post("/api/v1/items/{id}/bom", parentId), admin, body(
                "childId", String.valueOf(anotherChild),
                "qty", 1));
        assertThat(minimal.path("bomType").asText()).isEqualTo("DESIGN");
        // null 字段按全局 Jackson 配置可能被省略，故用 hasNonNull 而不是 isNull
        assertThat(minimal.hasNonNull("totalQty")).isFalse();

        // 负数用量 → 400（@DecimalMin）
        mockMvc.perform(post("/api/v1/items/{id}/bom", parentId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("childId", String.valueOf(createItem(admin, "BOM 子件3")),
                                "qty", 1, "totalQty", -5)))
                .andExpect(status().isBadRequest());
    }

    /** 多级展开 + 成环拒绝：A→B→C 三层可展开，C→A 应 400。 */
    @Test
    void bomTreeExpandsThreeLevelsAndRejectsCycle() throws Exception {
        String admin = login();
        long a = createItem(admin, "层级A");
        long b = createItem(admin, "层级B");
        long c = createItem(admin, "层级C");
        try {
            call(post("/api/v1/items/{id}/bom", a), admin, body(
                    "childId", String.valueOf(b), "qty", 1));
            call(post("/api/v1/items/{id}/bom", b), admin, body(
                    "childId", String.valueOf(c), "qty", 2));

            JsonNode tree = call(get("/api/v1/items/{id}/bom", a).param("depth", "3"), admin, null);
            assertThat(tree).hasSize(1);
            JsonNode bNode = tree.get(0);
            assertThat(bNode.path("children")).as("第二层应展开").hasSize(1);
            JsonNode cNode = bNode.path("children").get(0);
            assertThat(cNode.path("childId").asText()).isEqualTo(String.valueOf(c));
            assertThat(cNode.path("qty").decimalValue()).isEqualByComparingTo("2");

            // 成环：C 里再放 A
            mockMvc.perform(post("/api/v1/items/{id}/bom", c)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("childId", String.valueOf(a), "qty", 1)))
                    .andExpect(status().isBadRequest());
        } finally {
            jdbcTemplate.update("DELETE FROM bom_line WHERE parent_object_id IN (?, ?, ?)"
                    + " OR child_object_id IN (?, ?, ?)", a, b, c, a, b, c);
            jdbcTemplate.update("DELETE FROM item_master WHERE object_id IN (?, ?, ?)", a, b, c);
            jdbcTemplate.update("DELETE FROM plm_object WHERE id IN (?, ?, ?)", a, b, c);
        }
    }

    /** BOM-01：双向展开判环——快捷方式（不成环）放行，跨级成环拒绝。 */
    @Test
    void bomCycleCheckIsBidirectional() throws Exception {
        String admin = login();
        long a = createItem(admin, "双向A");
        long b = createItem(admin, "双向B");
        long c = createItem(admin, "双向C");
        long x = createItem(admin, "双向X");
        try {
            call(post("/api/v1/items/{id}/bom", a), admin, body("childId", String.valueOf(b), "qty", 1));
            call(post("/api/v1/items/{id}/bom", b), admin, body("childId", String.valueOf(c), "qty", 1));
            call(post("/api/v1/items/{id}/bom", x), admin, body("childId", String.valueOf(a), "qty", 1));

            // A -> C 是快捷方式，不是环
            call(post("/api/v1/items/{id}/bom", a), admin, body("childId", String.valueOf(c), "qty", 1));

            // C -> X 会形成 X -> A -> B -> C -> X
            mockMvc.perform(post("/api/v1/items/{id}/bom", c)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("childId", String.valueOf(x), "qty", 1)))
                    .andExpect(status().isBadRequest());
        } finally {
            jdbcTemplate.update("DELETE FROM bom_line WHERE parent_object_id IN (?, ?, ?, ?)"
                    + " OR child_object_id IN (?, ?, ?, ?)", a, b, c, x, a, b, c, x);
            jdbcTemplate.update("DELETE FROM item_master WHERE object_id IN (?, ?, ?, ?)", a, b, c, x);
            jdbcTemplate.update("DELETE FROM plm_object WHERE id IN (?, ?, ?, ?)", a, b, c, x);
        }
    }

    // ------------------------------------------------------------ 工具

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private long createItem(String token, String name) throws Exception {
        JsonNode created = call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE")));
        return created.path("id").asLong();
    }

    /** 用 Map 保留顺序拼请求体，避免 Map.of 到 10 个键就编译失败。 */
    private String body(Object... pairs) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return json(map);
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
