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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-CORE-04：物料类型补齐（蓝本 11 类 → 新系统 8 + 5 = 13 可选）。
 *
 * <p>断言 5 个新增类型（CIS/服务/废料/外协件/手工单）能创建零部件，
 * 且枚举接口能返回它们（前端下拉据此渲染）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemTypeEnumSmokeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final List<String> NEW_TYPES = List.of("CIS", "SERVICE", "SCRAP", "OUTSOURCE", "MANUAL");

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
    void newItemTypesAreUsable() throws Exception {
        String admin = login();

        for (String type : NEW_TYPES) {
            JsonNode created = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "类型验证-" + type, "itemType", type, "sourceType", "MAKE")));
            assertThat(created.path("itemType").asText())
                    .as("新增物料类型 %s 应能创建零部件", type)
                    .isEqualTo(type);
        }

        // 枚举接口要能返回，否则前端下拉选不到
        JsonNode enums = call(get("/api/v1/meta/enums/ItemType"), admin, null);
        List<String> codes = new java.util.ArrayList<>();
        enums.path("items").forEach(item -> codes.add(item.path("code").asText()));
        assertThat(codes).containsAll(NEW_TYPES);
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
