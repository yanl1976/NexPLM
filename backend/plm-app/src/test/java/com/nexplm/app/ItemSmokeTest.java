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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 零部件冒烟测试：自动取号、类型/供应方式校验、列表筛选、详情、更新、删除保护。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemSmokeTest {

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
    void itemCrudWithTypeFiltersAndDeleteGuard() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 自动取号 + 初始状态
        JsonNode product = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "主传动总成", "itemType", "PRODUCT", "specification", "ZD-100")));
        long productId = product.path("id").asLong();
        String productCode = product.path("code").asText();
        assertThat(productCode).matches("PN-\\d{6}");
        int productSeq = Integer.parseInt(productCode.substring(3));
        assertThat(product.path("lifecycleState").asText()).isEqualTo("Draft");
        assertThat(product.path("revisionLabel").asText()).isEqualTo("A.1");
        assertThat(product.path("unit").asText()).isEqualTo("EA");
        assertThat(product.path("sourceType").asText()).isEqualTo("MAKE");

        // 2. 零件：材料/单位/重量逐字段回读
        Map<String, Object> partReq = new HashMap<>();
        partReq.put("name", "主动齿轮");
        partReq.put("itemType", "PART");
        partReq.put("specification", "M3 Z=24");
        partReq.put("material", "40Cr");
        partReq.put("unit", "PCS");
        partReq.put("weight", 1.25);
        JsonNode part = call(post("/api/v1/items"), admin, json(partReq));
        long partId = part.path("id").asLong();
        assertThat(part.path("code").asText()).isEqualTo(String.format("PN-%06d", productSeq + 1));
        assertThat(part.path("material").asText()).isEqualTo("40Cr");
        assertThat(part.path("unit").asText()).isEqualTo("PCS");
        assertThat(part.path("weight").asDouble()).isEqualTo(1.25);

        // 3. 标准件：外购 + 标准号
        JsonNode standard = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "六角螺栓 M8×30", "itemType", "STANDARD", "sourceType", "BUY",
                "stdCode", "GB/T 5783")));
        long standardId = standard.path("id").asLong();
        assertThat(standard.path("code").asText()).isEqualTo(String.format("PN-%06d", productSeq + 2));
        assertThat(standard.path("sourceType").asText()).isEqualTo("BUY");
        assertThat(standard.path("stdCode").asText()).isEqualTo("GB/T 5783");

        // 4. 非法类型 / 非法供应方式
        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法类型件", "itemType", "UNKNOWN"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法供应方式", "itemType", "PART", "sourceType", "RENT"))))
                .andExpect(status().isBadRequest());

        // 5. 列表与筛选（同一测试类内多个用例共享库，按相对数量与包含关系断言）
        JsonNode all = call(get("/api/v1/items?page=1&size=200"), admin, null);
        assertThat(all.path("total").asLong()).isGreaterThanOrEqualTo(3);
        JsonNode parts = call(get("/api/v1/items?page=1&size=200&itemType=PART"), admin, null);
        assertThat(parts.path("items").findValuesAsText("code")).contains(part.path("code").asText());
        JsonNode bought = call(get("/api/v1/items?page=1&size=200&sourceType=BUY"), admin, null);
        assertThat(bought.path("items").findValuesAsText("code")).contains(standard.path("code").asText());
        JsonNode bySpec = call(get("/api/v1/items?page=1&size=200&keyword=M3"), admin, null);
        assertThat(bySpec.path("items").findValuesAsText("code")).contains(part.path("code").asText());

        // 6. 更新：名称与重量变化，版本不变
        JsonNode updated = call(put("/api/v1/items/{id}", partId), admin, json(Map.of(
                "name", "主动齿轮(改)", "itemType", "PART", "material", "20CrMnTi",
                "unit", "PCS", "weight", 1.3)));
        assertThat(updated.path("name").asText()).isEqualTo("主动齿轮(改)");
        assertThat(updated.path("material").asText()).isEqualTo("20CrMnTi");
        assertThat(updated.path("revisionLabel").asText()).isEqualTo("A.1");

        // 7. 删除：无 BOM 引用可删；删除后列表不再包含
        mockMvc.perform(delete("/api/v1/items/{id}", standardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        JsonNode afterDelete = call(get("/api/v1/items?page=1&size=200"), admin, null);
        assertThat(afterDelete.path("items").findValuesAsText("code"))
                .doesNotContain(standard.path("code").asText());
        mockMvc.perform(get("/api/v1/items/{id}", standardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());

        // 8. 详情可读（产品）
        JsonNode productDetail = call(get("/api/v1/items/{id}", productId), admin, null);
        assertThat(productDetail.path("itemType").asText()).isEqualTo("PRODUCT");
        assertThat(productDetail.path("specification").asText()).isEqualTo("ZD-100");
        assertThat(productDetail.path("isLatest").asBoolean()).isTrue();
    }

    @Test
    void bomTreeWithValidationAndDeleteGuard() throws Exception {
        String admin = login("admin", "Admin@12345");
        long productId = createItem(admin, "BOM 总成", "PRODUCT", "MAKE");
        long assemblyId = createItem(admin, "BOM 部件", "ASSEMBLY", "MAKE");
        long partId = createItem(admin, "BOM 零件", "PART", "MAKE");
        long boltId = createItem(admin, "BOM 螺栓", "STANDARD", "BUY");

        // 1. 搭 3 层级 BOM：总成 → 部件 → (零件 4 件/损耗 2%、螺栓 8 件)
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", productId), admin,
                json(Map.of("childId", assemblyId, "qty", 1)));
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", assemblyId), admin,
                json(Map.of("childId", partId, "qty", 4, "lossRate", 2, "refDesignator", "P1")));
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", assemblyId), admin,
                json(Map.of("childId", boltId, "qty", 8)));

        // 2. 树结构：层级、用量、损耗率、位号、hasChildren
        JsonNode tree = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=3", productId), admin, null);
        assertThat(tree.size()).isEqualTo(1);
        JsonNode assembly = tree.get(0);
        assertThat(assembly.path("childId").asLong()).isEqualTo(assemblyId);
        assertThat(assembly.path("qty").asDouble()).isEqualTo(1.0);
        assertThat(assembly.path("hasChildren").asBoolean()).isTrue();
        assertThat(assembly.path("children").size()).isEqualTo(2);
        JsonNode partNode = assembly.path("children").get(0);
        assertThat(partNode.path("childId").asLong()).isEqualTo(partId);
        assertThat(partNode.path("qty").asDouble()).isEqualTo(4.0);
        assertThat(partNode.path("lossRate").asDouble()).isEqualTo(2.0);
        assertThat(partNode.path("refDesignator").asText()).isEqualTo("P1");
        assertThat(partNode.path("hasChildren").asBoolean()).isFalse();
        long partLineId = partNode.path("lineId").asLong();

        // 3. 重复子件 → 409
        mockMvc.perform(post("/api/v1/items/{id}/bom", productId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", assemblyId, "qty", 2))))
                .andExpect(status().isConflict());

        // 4. 自引用与循环引用 → 400
        mockMvc.perform(post("/api/v1/items/{id}/bom", productId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", productId, "qty", 1))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/items/{id}/bom", partId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", productId, "qty", 1))))
                .andExpect(status().isBadRequest());

        // 5. 用量为 0 → 400（Bean Validation）
        mockMvc.perform(post("/api/v1/items/{id}/bom", assemblyId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", boltId, "qty", 0))))
                .andExpect(status().isBadRequest());

        // 6. 被引用查询
        JsonNode whereUsed = call(get("/api/v1/items/{id}/bom/where-used", partId), admin, null);
        assertThat(whereUsed.size()).isEqualTo(1);
        assertThat(whereUsed.get(0).path("parentId").asLong()).isEqualTo(assemblyId);

        // 7. 结构校验：无问题
        JsonNode validate = call(get("/api/v1/items/{id}/bom/validate?viewType=DESIGN", productId), admin, null);
        assertThat(validate.path("issues").size()).isZero();

        // 8. 修改用量
        JsonNode updated = call(put("/api/v1/items/{id}/bom/lines/{lineId}", assemblyId, partLineId), admin,
                json(Map.of("qty", 5, "unit", "PCS", "lossRate", 3, "refDesignator", "P1,P2")));
        assertThat(updated.path("qty").asDouble()).isEqualTo(5.0);
        assertThat(updated.path("unit").asText()).isEqualTo("PCS");

        // 9. 被 BOM 引用时不能删除零部件
        mockMvc.perform(delete("/api/v1/items/{id}", partId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 10. 删除 BOM 行后可删除零部件
        mockMvc.perform(delete("/api/v1/items/{id}/bom/lines/{lineId}", assemblyId, partLineId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        JsonNode afterRemove = call(get("/api/v1/items/{id}/bom?viewType=DESIGN", assemblyId), admin, null);
        assertThat(afterRemove.size()).isEqualTo(1);
        mockMvc.perform(delete("/api/v1/items/{id}", partId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        // 11. 非法视图 400
        mockMvc.perform(get("/api/v1/items/{id}/bom?viewType=NOT_EXIST", productId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 工具

    private long createItem(String token, String name, String itemType, String sourceType) throws Exception {
        JsonNode created = call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", itemType, "sourceType", sourceType, "unit", "EA")));
        return created.path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                          String token, String payload) throws Exception {
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
