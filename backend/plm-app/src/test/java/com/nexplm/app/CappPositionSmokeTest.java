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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 C-1｜工厂结构（工位）与消耗定义。
 *
 * <p>对齐旧系统 {@code PositionManager}（POSITIONITEM 树 + POSITIONLEVEL）与
 * {@code ConsumptionStation}（CSOBJ 消耗站挂在工位下）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CappPositionSmokeTest {

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
    void positionTreeAndConsumptionLifecycle() throws Exception {
        String admin = login();

        // 1. 工厂 → 车间 → 工位，编码为空时按规则 CappPosition 取号（POS-xxxxxx）
        JsonNode plant = call(post("/api/v1/capp/positions"), admin,
                json(Map.of("name", "天水工厂", "positionLevel", "PLANT")));
        assertThat(plant.path("code").asText()).startsWith("POS-");

        JsonNode workshop = call(post("/api/v1/capp/positions"), admin,
                json(Map.of("name", "总装车间", "positionLevel", "WORKSHOP",
                        "parentId", plant.path("id").asText())));
        JsonNode station = call(post("/api/v1/capp/positions"), admin,
                json(Map.of("name", "装配工位1", "positionLevel", "STATION",
                        "parentId", workshop.path("id").asText(),
                        "capacity", 8, "shifts", 2, "logisticsUnit", "LU-A")));

        JsonNode tree = call(get("/api/v1/capp/positions"), admin, null);
        assertThat(tree.size()).isEqualTo(1);
        assertThat(tree.get(0).path("children").size()).isEqualTo(1);
        assertThat(tree.get(0).path("children").get(0).path("children").get(0)
                .path("name").asText()).isEqualTo("装配工位1");

        // 2. 改工位（名称 + 产能）
        call(put("/api/v1/capp/positions/{id}", station.path("id").asText()), admin,
                json(Map.of("name", "装配工位1（改）", "positionLevel", "STATION",
                        "parentId", workshop.path("id").asText(), "capacity", 10)));

        // 3. 消耗定义：挂物料（objectId 反查编码/名称）
        long itemId = createItem(admin, "消耗物料A");
        String itemCode = call(get("/api/v1/items/{id}", itemId), admin, null).path("code").asText();
        call(post("/api/v1/capp/consumptions"), admin, json(Map.of(
                "name", "焊丝", "positionId", station.path("id").asText(),
                "objectId", String.valueOf(itemId), "qty", 1.5, "unit", "kg",
                "allRoute", true, "isPrimary", true)));

        JsonNode list = call(get("/api/v1/capp/consumptions")
                .param("positionId", station.path("id").asText()), admin, null);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).path("itemCode").asText()).isEqualTo(itemCode);
        assertThat(list.get(0).path("qty").asDouble()).isEqualTo(1.5);
        assertThat(list.get(0).path("allRoute").asBoolean()).isTrue();

        // 4. 有消耗定义时不允许删工位
        mockMvc.perform(delete("/api/v1/capp/positions/{id}", station.path("id").asText())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 5. 删消耗后可删工位；有下级时仍拒绝
        call(delete("/api/v1/capp/consumptions/{id}", list.get(0).path("id").asText()), admin, null);
        mockMvc.perform(delete("/api/v1/capp/positions/{id}", workshop.path("id").asText())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        call(delete("/api/v1/capp/positions/{id}", station.path("id").asText()), admin, null);

        JsonNode after = call(get("/api/v1/capp/positions"), admin, null);
        assertThat(after.get(0).path("children").get(0).path("children").size()).isZero();
    }

    // ------------------------------------------------------------ 工具

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
