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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 A｜个人工作区（对齐旧系统 000 个人工作区 / 001 常用功能）。
 *
 * <p>覆盖：我的检出（列口径同 {@code CHECKOUTOBJECTVIEW}）、收藏夹（跨对象类型 + 重复收藏幂等）、
 * 最近访问（30 分钟内重复访问只更新时间）、站内消息收件箱、高级查询（plm_object ∪ chg_ecr）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class MyWorkSmokeTest {

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
    void favoritesRecentSearchAndCheckouts() throws Exception {
        String admin = login();
        long itemId = createItem(admin, "个人工作区零件");
        String itemCode = call(get("/api/v1/items/{id}", itemId), admin, null).path("code").asText();

        // 1. 收藏：重复收藏幂等，列表能带出对象编码/名称/类型中文名
        JsonNode first = call(post("/api/v1/my/favorites"), admin,
                json(Map.of("objectType", "PdmItem", "objectId", itemId, "folder", "常用")));
        JsonNode again = call(post("/api/v1/my/favorites"), admin,
                json(Map.of("objectType", "PdmItem", "objectId", itemId)));
        assertThat(again.asLong()).isEqualTo(first.asLong());

        JsonNode favorites = call(get("/api/v1/my/favorites"), admin, null);
        assertThat(favorites.size()).isEqualTo(1);
        assertThat(favorites.get(0).path("code").asText()).isEqualTo(itemCode);
        assertThat(favorites.get(0).path("objectTypeName").asText()).isEqualTo("零部件");
        assertThat(favorites.get(0).path("folder").asText()).isEqualTo("常用");

        // 2. 最近访问：同一对象 30 分钟内重复访问只留一条
        call(post("/api/v1/my/recent"), admin, json(Map.of(
                "objectType", "PdmItem", "objectId", itemId, "accessType", "VIEW")));
        call(post("/api/v1/my/recent"), admin, json(Map.of(
                "objectType", "PdmItem", "objectId", itemId, "accessType", "VIEW")));
        JsonNode recent = call(get("/api/v1/my/recent"), admin, null);
        assertThat(recent.size()).isEqualTo(1);
        assertThat(recent.get(0).path("code").asText()).isEqualTo(itemCode);
        assertThat(recent.get(0).path("accessType").asText()).isEqualTo("VIEW");

        // 3. 高级查询：关键字命中刚建的零部件（plm_object 分支）
        JsonNode search = call(get("/api/v1/my/search").param("keyword", itemCode), admin, null);
        assertThat(search.size()).isEqualTo(1);
        assertThat(search.get(0).path("objectType").asText()).isEqualTo("PdmItem");

        // 4. 我的检出：未检出时为空；检出后按检出时间倒序出现，撤销后清空
        assertThat(call(get("/api/v1/my/checkouts"), admin, null).size()).isZero();
        call(post("/api/v1/items/{id}/checkout", itemId), admin, null);
        JsonNode checkouts = call(get("/api/v1/my/checkouts"), admin, null);
        assertThat(checkouts.size()).isEqualTo(1);
        assertThat(checkouts.get(0).path("objectType").asText()).isEqualTo("PdmItem");
        assertThat(checkouts.get(0).path("objectTypeName").asText()).isEqualTo("零部件");
        assertThat(checkouts.get(0).path("code").asText()).isEqualTo(itemCode);
        assertThat(checkouts.get(0).path("checkedOutAt").isNull()).isFalse();

        call(post("/api/v1/my/checkouts/{id}/cancel", itemId), admin, null);
        assertThat(call(get("/api/v1/my/checkouts"), admin, null).size()).isZero();

        // 5. 站内消息：结构可读（本用例不造通道，列表允许为空）
        assertThat(call(get("/api/v1/my/messages"), admin, null)).isNotNull();
        assertThat(call(get("/api/v1/my/messages/unread-count"), admin, null)
                .path("unread").asInt()).isGreaterThanOrEqualTo(0);

        // 6. 对象类型字典含中文名（前端下拉不写死枚举）
        JsonNode types = call(get("/api/v1/my/object-types"), admin, null);
        assertThat(types.path("types").toString()).contains("PdmItem");
        assertThat(types.path("labels").path("PdmItem").asText()).isEqualTo("零部件");

        // 7. 取消收藏
        mockMvc.perform(delete("/api/v1/my/favorites/{id}", first.asLong())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        assertThat(call(get("/api/v1/my/favorites"), admin, null).size()).isZero();
    }

    // ------------------------------------------------------------ 工具

    private long createItem(String token, String name) throws Exception {
        JsonNode created = call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA")));
        return created.path("id").asLong();
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    /** 路径参数在构造 builder 时给：{@code get("/api/v1/items/{id}", itemId)}。 */
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
