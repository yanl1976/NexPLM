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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 F-1｜在线用户与强制下线：登录落会话、踢人后令牌立即失效、登录日志落库。
 *
 * <p>取证见 docs/06 EXP-20261107-16：旧系统在线用户是服务端内存会话表 + 按设备踢；
 * 新系统原先是无状态 JWT，做不到「让已签发令牌提前失效」，本用例正是验证这条能力。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SessionSmokeTest {

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
    void loginCreatesSessionAndKickRevokesToken() throws Exception {
        // 1. 登录即落会话（含 IP / UA / 设备类型）
        String token = login("admin", "Admin@12345", "PC");
        Long sessionId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user_session WHERE username = 'admin' AND active = true",
                Long.class);
        assertThat(sessionId).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT device_type FROM sys_user_session WHERE id = ?", String.class, sessionId))
                .isEqualTo("PC");

        // 2. 在线列表：字段对齐旧系统（用户名/设备/在线秒数）
        JsonNode page = call(get("/api/v1/sys/sessions"), token, null);
        assertThat(page.path("total").asLong()).isEqualTo(1);
        JsonNode row = page.path("items").get(0);
        assertThat(row.path("username").asText()).isEqualTo("admin");
        assertThat(row.path("deviceType").asText()).isEqualTo("PC");
        assertThat(row.path("onlineSeconds").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(row.path("id").asLong()).isEqualTo(sessionId);

        // 3. 会话有效：当前令牌可正常访问
        assertThat(call(get("/api/v1/auth/me"), token, null).path("username").asText())
                .isEqualTo("admin");

        // 4. 强制下线 → 该令牌立即 401（这是旧系统"踢人"的等价能力）
        call(post("/api/v1/sys/sessions/{id}/kick", sessionId), token, null);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT end_reason FROM sys_user_session WHERE id = ?", String.class, sessionId))
                .isEqualTo("KICKED");

        // 5. 重新登录后可正常访问；登出同样让令牌失效
        String second = login("admin", "Admin@12345", "MOBILE");
        assertThat(call(get("/api/v1/auth/me"), second, null).path("username").asText())
                .isEqualTo("admin");
        call(post("/api/v1/auth/logout"), second, null);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + second))
                .andExpect(status().isUnauthorized());

        // 6. 全部下线：按设备类型分开（旧系统 OnlineUserType 语义）
        String pcToken = login("admin", "Admin@12345", "PC");
        String mobileToken = login("admin", "Admin@12345", "MOBILE");
        JsonNode kicked = call(post("/api/v1/sys/sessions/kick-all").queryParam("deviceType", "PC"),
                pcToken, null);
        assertThat(kicked.path("kicked").asInt()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + pcToken))
                .andExpect(status().isUnauthorized());
        assertThat(call(get("/api/v1/auth/me"), mobileToken, null).path("username").asText())
                .isEqualTo("admin");

        // 7. 登录日志：成功与失败都落 sys_login_log（原先该表无写入点）
        Long successLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_login_log WHERE username = 'admin' AND result = 'SUCCESS'",
                Long.class);
        assertThat(successLogs).isGreaterThanOrEqualTo(4);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
        assertThat(jdbcTemplate.queryForObject("""
                SELECT fail_reason FROM sys_login_log
                 WHERE username = 'admin' AND result = 'FAILED'
                 ORDER BY id DESC LIMIT 1
                """, String.class)).contains("密码");
    }

    // ------------------------------------------------------------ 工具

    private String login(String username, String password, String deviceType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Device-Type", deviceType)
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
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
