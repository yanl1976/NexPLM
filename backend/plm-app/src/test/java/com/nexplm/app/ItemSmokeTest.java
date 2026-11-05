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
        assertThat(product.path("code").asText()).isEqualTo("PN-000001");
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
        assertThat(part.path("code").asText()).isEqualTo("PN-000002");
        assertThat(part.path("material").asText()).isEqualTo("40Cr");
        assertThat(part.path("unit").asText()).isEqualTo("PCS");
        assertThat(part.path("weight").asDouble()).isEqualTo(1.25);

        // 3. 标准件：外购 + 标准号
        JsonNode standard = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "六角螺栓 M8×30", "itemType", "STANDARD", "sourceType", "BUY",
                "stdCode", "GB/T 5783")));
        long standardId = standard.path("id").asLong();
        assertThat(standard.path("code").asText()).isEqualTo("PN-000003");
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

        // 5. 列表与筛选
        JsonNode all = call(get("/api/v1/items?page=1&size=20"), admin, null);
        assertThat(all.path("total").asLong()).isEqualTo(3);
        JsonNode parts = call(get("/api/v1/items?page=1&size=20&itemType=PART"), admin, null);
        assertThat(parts.path("total").asLong()).isEqualTo(1);
        assertThat(parts.path("items").get(0).path("code").asText()).isEqualTo("PN-000002");
        JsonNode bought = call(get("/api/v1/items?page=1&size=20&sourceType=BUY"), admin, null);
        assertThat(bought.path("total").asLong()).isEqualTo(1);
        JsonNode bySpec = call(get("/api/v1/items?page=1&size=20&keyword=M3"), admin, null);
        assertThat(bySpec.path("total").asLong()).isEqualTo(1);

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
        JsonNode afterDelete = call(get("/api/v1/items?page=1&size=20"), admin, null);
        assertThat(afterDelete.path("total").asLong()).isEqualTo(2);
        mockMvc.perform(get("/api/v1/items/{id}", standardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());

        // 8. 详情可读（产品）
        JsonNode productDetail = call(get("/api/v1/items/{id}", productId), admin, null);
        assertThat(productDetail.path("itemType").asText()).isEqualTo("PRODUCT");
        assertThat(productDetail.path("specification").asText()).isEqualTo("ZD-100");
        assertThat(productDetail.path("isLatest").asBoolean()).isTrue();
    }

    // ------------------------------------------------------------ 工具

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
