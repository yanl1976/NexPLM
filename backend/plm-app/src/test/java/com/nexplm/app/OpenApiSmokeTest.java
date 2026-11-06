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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9-3 开放平台冒烟：API Key 认证 + 作用域 + 限流 + 轮换/过期/停用 + 对象与文件接口。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSmokeTest {

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
        registry.add("nexplm.integration.retry.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void apiKeyScopeRateLimitRotateExpire() throws Exception {
        String admin = login("admin", "Admin@12345");
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);
        long itemId = insertItem(companyId, "PN-OPEN-1", "开放接口测试件");

        // 1. 创建客户端：明文 key 只返回一次，库内只有 hash
        JsonNode created = call(post("/api/v1/itg/clients"), admin, json(client("OPEN-1",
                List.of("object.read"), 60, null, true)));
        String apiKey = created.path("apiKey").asText();
        assertThat(apiKey).startsWith("nlk_");
        assertThat(created.path("client").toString()).doesNotContain(apiKey.substring(4));
        String storedHash = jdbcTemplate.queryForObject("""
                SELECT key_hash FROM itg_api_client WHERE code = 'OPEN-1'
                """, String.class);
        assertThat(storedHash).hasSize(64).isNotEqualTo(apiKey);

        // 2. whoami 与对象查询
        JsonNode who = call(get("/api/v1/open/whoami"), apiKey, null);
        assertThat(who.path("clientCode").asText()).isEqualTo("OPEN-1");
        assertThat(who.path("scopes")).hasSize(1);
        JsonNode objects = call(get("/api/v1/open/objects?objectType=PdmItem"), apiKey, null);
        assertThat(objects.path("total").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(objects.path("items").toString()).contains("PN-OPEN-1");
        JsonNode detail = call(get("/api/v1/open/objects/" + itemId), apiKey, null);
        assertThat(detail.path("code").asText()).isEqualTo("PN-OPEN-1");

        // 3. 无 key / 错 key → 401
        mockMvc.perform(get("/api/v1/open/objects"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/open/objects")
                        .header("X-API-Key", "nlk_wrongkey000000"))
                .andExpect(status().isUnauthorized());

        // 4. 作用域不足：只给了 object.read，文件接口 403
        mockMvc.perform(get("/api/v1/open/files/1/content").header("X-API-Key", apiKey))
                .andExpect(status().isForbidden());

        // 5. 补授 file.read/file.write 后上传与下载可用
        long clientId = created.path("client").path("id").asLong();
        call(put("/api/v1/itg/clients/{id}", clientId), admin, json(client("OPEN-1",
                List.of("object.read", "file.read", "file.write"), 60, null, true)));
        MockMultipartFile file = new MockMultipartFile("file", "open.txt",
                "text/plain", "hello-open-api".getBytes(StandardCharsets.UTF_8));
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/open/files")
                        .file(file).header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andReturn();
        long fileId = body(uploadResult).path("data").path("fileId").asLong();
        MvcResult download = mockMvc.perform(get("/api/v1/open/files/{id}/content", fileId)
                        .header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(download.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("hello-open-api");

        // 6. 未知作用域 400
        mockMvc.perform(post("/api/v1/itg/clients")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(client("OPEN-BAD", List.of("object.write"), 60, null, true))))
                .andExpect(status().isBadRequest());

        // 7. 限流：独立客户端配额 2，第 3 次 429
        JsonNode limited = call(post("/api/v1/itg/clients"), admin, json(client("OPEN-2",
                List.of("object.read"), 2, null, true)));
        String limitedKey = limited.path("apiKey").asText();
        call(get("/api/v1/open/whoami"), limitedKey, null);
        call(get("/api/v1/open/whoami"), limitedKey, null);
        mockMvc.perform(get("/api/v1/open/whoami").header("X-API-Key", limitedKey))
                .andExpect(status().isTooManyRequests());

        // 8. 轮换：旧 key 立即失效，新 key 可用
        JsonNode rotated = call(post("/api/v1/itg/clients/{id}/rotate", clientId), admin, null);
        String newKey = rotated.path("apiKey").asText();
        assertThat(newKey).isNotEqualTo(apiKey);
        mockMvc.perform(get("/api/v1/open/whoami").header("X-API-Key", apiKey))
                .andExpect(status().isUnauthorized());
        call(get("/api/v1/open/whoami"), newKey, null);

        // 9. 过期与停用
        JsonNode expired = call(post("/api/v1/itg/clients"), admin, json(client("OPEN-EXPIRED",
                List.of("object.read"), 60, "2020-01-01", true)));
        mockMvc.perform(get("/api/v1/open/whoami").header("X-API-Key",
                        expired.path("apiKey").asText()))
                .andExpect(status().isUnauthorized());
        long expiredId = expired.path("client").path("id").asLong();
        call(put("/api/v1/itg/clients/{id}", expiredId), admin, json(client("OPEN-EXPIRED",
                List.of("object.read"), 60, "2099-12-31", false)));
        mockMvc.perform(get("/api/v1/open/whoami").header("X-API-Key",
                        expired.path("apiKey").asText()))
                .andExpect(status().isUnauthorized());

        // 10. 站内登录态不受影响：admin JWT 仍可访问管理接口
        assertThat(call(get("/api/v1/itg/clients"), admin, null).path("total").asInt())
                .isGreaterThanOrEqualTo(3);
    }

    // ------------------------------------------------------------------ 工具

    private long insertItem(long companyId, String code, String name) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                        lifecycle_state)
                VALUES (?, 'PdmItem', ?, ?, ?, ?, 'Draft')
                """, id, id, code, name, companyId);
        return id;
    }

    private Map<String, Object> client(String code, List<String> scopes, int rateLimit,
                                       String expiresAt, boolean enabled) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("name", "客户端 " + code);
        body.put("scopes", scopes);
        body.put("rateLimitPerMin", rateLimit);
        body.put("enabled", enabled);
        if (expiresAt != null) {
            body.put("expiresAt", expiresAt);
        }
        return body;
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    /** token 既可以是站内 JWT，也可以是 API Key（按前缀区分，方便两套接口共用断言）。 */
    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload)
            throws Exception {
        if (token.startsWith("nlk_")) {
            builder.header("X-API-Key", token);
        } else {
            builder.header("Authorization", "Bearer " + token);
        }
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
