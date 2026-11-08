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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 图纸关联（DRW-01）：图纸是文档域对象，关联走 `plm_relation(Specification)`。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DrawingSmokeTest {

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
    void drawingLinkLifecycle() throws Exception {
        String admin = login();
        JsonNode item = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "非标自制件-图纸测试", "itemType", "GAUGE", "sourceType", "MAKE")));
        long itemId = item.path("id").asLong();
        long docId = 0L;
        try {
            JsonNode doc = call(post("/api/v1/docs"), admin, json(Map.of(
                    "code", "DRW-TEST-001",
                    "name", "测试图纸",
                    "docType", "DESIGN",
                    "source", "CAD")));
            docId = doc.path("id").asLong();
            assertThat(docId).isGreaterThan(0L);

            call(post("/api/v1/drawings/{docId}/link?itemId={itemId}", docId, itemId), admin, null);

            JsonNode rows = call(get("/api/v1/drawings/items/{itemId}", itemId), admin, null);
            assertThat(rows).hasSize(1);
            JsonNode row = rows.get(0);
            assertThat(row.path("dwg_no").asText()).isEqualTo("DRW-TEST-001");
            assertThat(row.path("doc_id").asLong()).isEqualTo(docId);

            JsonNode items = call(get("/api/v1/drawings/docs/{docId}/items", docId), admin, null);
            assertThat(items).hasSize(1);
            assertThat(items.get(0).path("item_id").asLong()).isEqualTo(itemId);

            long linkId = row.path("link_id").asLong();
            call(delete("/api/v1/drawings/{linkId}", linkId), admin, null);
            JsonNode after = call(get("/api/v1/drawings/items/{itemId}", itemId), admin, null);
            assertThat(after).isEmpty();
        } finally {
            jdbcTemplate.update("DELETE FROM plm_relation WHERE relation_type = 'Specification' AND src_id = ?", itemId);
            if (docId > 0L) {
                jdbcTemplate.update("DELETE FROM doc_master WHERE object_id = ?", docId);
                jdbcTemplate.update("DELETE FROM plm_object WHERE id = ?", docId);
            }
            jdbcTemplate.update("DELETE FROM item_master WHERE object_id = ?", itemId);
            jdbcTemplate.update("DELETE FROM plm_object WHERE id = ?", itemId);
        }
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("HTTP " + result.getResponse().getStatus()
                    + " body=" + result.getResponse().getContentAsString());
        }
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
