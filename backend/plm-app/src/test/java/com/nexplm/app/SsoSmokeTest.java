package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
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

import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9-5 SSO 冒烟：起一个真 OIDC 身份提供方（RS256 签名的 id_token + JWKS + 令牌端点），
 * 验证「跳转地址与 PKCE → 回调换令牌 → 自动建号 → 二次登录复用同一账号」。
 *
 * <p>不 mock 签名器与 HTTP：SSO 最容易出错的地方恰恰是验签、aud/iss、nonce 与 state 校验，
 * mock 掉它们等于只测自己的 if 分支。LDAP 侧本机没有目录服务，只验证配置校验与不可达时的失败语义。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SsoSmokeTest {

    private static final String ISSUER = "https://idp.test";
    private static final String CLIENT_ID = "nexplm-web";
    private static final String KID = "test-kid";
    private static final String SUBJECT = "user-123";
    private static final String USERNAME = "zhangsan";

    private static final EmbeddedPostgres POSTGRES;
    private static final HttpServer IDP;
    private static final KeyPair KEY_PAIR;
    private static final int PORT;
    private static volatile String lastCodeVerifier;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();

            IDP = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            IDP.createContext("/jwks", exchange -> {
                RSAPublicKey key = (RSAPublicKey) KEY_PAIR.getPublic();
                String json = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + KID + "\",\"alg\":\"RS256\","
                        + "\"n\":\"" + base64Url(unsigned(key.getModulus())) + "\","
                        + "\"e\":\"" + base64Url(unsigned(key.getPublicExponent())) + "\"}]}";
                respond(exchange, 200, json);
            });
            IDP.createContext("/token", exchange -> {
                try {
                    Map<String, String> form = parseForm(new String(
                            exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    if (!"authorization_code".equals(form.get("grant_type"))
                            || !CLIENT_ID.equals(form.get("client_id"))
                            || !"s3cret".equals(form.get("client_secret"))) {
                        respond(exchange, 400, "{\"error\":\"invalid_client\"}");
                        return;
                    }
                    lastCodeVerifier = form.get("code_verifier");
                    // 测试约定：授权码形如 nonce-<state 里的 nonce>，令牌端点把它回写进 id_token
                    String code = form.getOrDefault("code", "");
                    String nonce = code.startsWith("nonce-")
                            ? code.substring("nonce-".length()) : null;
                    respond(exchange, 200, "{\"token_type\":\"Bearer\",\"access_token\":\"at-1\","
                            + "\"id_token\":\"" + idToken(nonce) + "\"}");
                } catch (Exception ex) {
                    respond(exchange, 500, "{\"error\":\"server_error\"}");
                }
            });
            IDP.start();
            PORT = IDP.getAddress().getPort();
        } catch (Exception ex) {
            throw new IllegalStateException("启动测试 IdP 失败", ex);
        }
    }

    @AfterAll
    static void stopIdp() {
        IDP.stop(0);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("nexplm.security.bootstrap.admin-password", () -> "Admin@12345");
        registry.add("nexplm.integration.retry.enabled", () -> "false");
        registry.add("nexplm.webhook.retry.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void oidcLoginProvisionsAccountAndReusesIt() throws Exception {
        String admin = login("admin", "Admin@12345");
        long providerId = createOidcProvider(admin, "corp-oidc");

        // 密钥只写不读回：详情抹掉 clientSecret，只说明"已配置"
        JsonNode detail = call(get("/api/v1/sys/sso-providers/{id}", providerId), admin, null);
        assertThat(detail.path("config").has("clientSecret")).isFalse();
        assertThat(detail.path("secretFields").toString()).contains("clientSecret");

        // 登录页拿到的公开信息不含任何 IdP 地址
        JsonNode providers = call(get("/api/v1/auth/sso/providers"), null, null);
        JsonNode oidc = null;
        for (JsonNode item : providers) {
            if ("corp-oidc".equals(item.path("code").asText())) {
                oidc = item;
            }
        }
        assertThat(oidc).isNotNull();
        assertThat(oidc.path("type").asText()).isEqualTo("OIDC");
        assertThat(oidc.has("config")).isFalse();

        // 跳转地址必须带 PKCE 与 state
        JsonNode authorize = call(get("/api/v1/auth/sso/{code}/authorize", "corp-oidc"), null, null);
        String url = authorize.path("url").asText();
        String state = authorize.path("state").asText();
        assertThat(url).contains("client_id=" + CLIENT_ID)
                .contains("code_challenge_method=S256")
                .contains("redirect_uri=");
        assertThat(state).contains(".");

        JsonNode token = call(post("/api/v1/auth/sso/{code}/callback", "corp-oidc"), null,
                json(Map.of("authCode", "nonce-" + nonceOf(state), "state", state)));
        assertThat(token.path("accessToken").asText()).isNotBlank();
        assertThat(token.path("user").path("username").asText()).isEqualTo(USERNAME);
        // PKCE：verifier 真的发给了令牌端点
        assertThat(lastCodeVerifier).isNotBlank();

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT id, auth_provider, status, must_change_pwd FROM sys_user WHERE username = ?",
                USERNAME);
        assertThat(user.get("auth_provider")).isEqualTo("SSO");
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("must_change_pwd")).isEqualTo(false);
        long userId = ((Number) user.get("id")).longValue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user_sso WHERE provider_id = ? AND subject = ?",
                Integer.class, providerId, SUBJECT)).isEqualTo(1);
        // 自动建号按 default_role_code 授最小角色
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id
                 WHERE ur.user_id = ? AND r.code = 'BUSINESS'
                """, Integer.class, userId)).isEqualTo(1);

        // 二次登录：同一 IdP 主体必须复用同一账号，不重复建号
        JsonNode second = call(get("/api/v1/auth/sso/{code}/authorize", "corp-oidc"), null, null);
        call(post("/api/v1/auth/sso/{code}/callback", "corp-oidc"), null,
                json(Map.of("authCode", "nonce-" + nonceOf(second.path("state").asText()),
                        "state", second.path("state").asText())));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user WHERE username = ?", Integer.class, USERNAME))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user_sso WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void rejectsTamperedStateAndInvalidProviderConfig() throws Exception {
        String admin = login("admin", "Admin@12345");
        createOidcProvider(admin, "corp-oidc-b");
        JsonNode authorize = call(get("/api/v1/auth/sso/{code}/authorize", "corp-oidc-b"), null, null);
        String state = authorize.path("state").asText();

        // 篡改签名 → 拒绝
        mockMvc.perform(post("/api/v1/auth/sso/{code}/callback", "corp-oidc-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("authCode", "nonce-x", "state", state + "0"))))
                .andExpect(status().isBadRequest());

        // 类型不匹配：OIDC 配置不能走 LDAP 登录
        mockMvc.perform(post("/api/v1/auth/sso/{code}/ldap-login", "corp-oidc-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "u", "password", "p"))))
                .andExpect(status().isBadRequest());

        // 各类非法配置
        mockMvc.perform(post("/api/v1/sys/sso-providers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "bad-type", "name", "非法类型", "type", "SAML"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sys/sso-providers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "bad-oidc", "name", "缺字段", "type", "OIDC",
                                "config", Map.of("issuer", ISSUER)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sys/sso-providers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "bad-ldap", "name", "非法 LDAP", "type", "LDAP",
                                "config", Map.of("url", "http://127.0.0.1:389",
                                        "attrUsername", "sAMAccountName",
                                        "userFilter", "(sAMAccountName={0})")))))
                .andExpect(status().isBadRequest());
        // 编码重复（必须是合法配置，否则先被配置校验挡成 400，测不到唯一索引）
        mockMvc.perform(post("/api/v1/sys/sso-providers")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(oidcProviderBody("corp-oidc-b"))))
                .andExpect(status().isConflict());
    }

    @Test
    void ldapProviderFailsClosedWhenDirectoryUnreachable() throws Exception {
        String admin = login("admin", "Admin@12345");
        JsonNode created = call(post("/api/v1/sys/sso-providers"), admin, json(Map.of(
                "code", "corp-ad", "name", "域账号", "type", "LDAP",
                "autoProvision", false, "remark", "本机无目录服务，仅验证失败语义",
                "config", Map.of("url", "ldap://127.0.0.1:1", "baseDn", "dc=corp,dc=local",
                        "attrUsername", "sAMAccountName", "userFilter", "(sAMAccountName={0})"))));
        assertThat(created.path("type").asText()).isEqualTo("LDAP");

        // 目录不可达：必须报握手失败，而不是"认证通过"或 500
        mockMvc.perform(post("/api/v1/auth/sso/{code}/ldap-login", "corp-ad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "zhangsan", "password", "whatever"))))
                .andExpect(status().isBadGateway());

        // 空密码必须先被挡住（否则 LDAP 会退化成匿名 bind）
        mockMvc.perform(post("/api/v1/auth/sso/{code}/ldap-login", "corp-ad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "zhangsan", "password", ""))))
                .andExpect(status().isBadRequest());

        // 停用后公开列表不再出现，且登录被拒
        long providerId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_sso_provider WHERE code = 'corp-ad'", Long.class);
        call(delete("/api/v1/sys/sso-providers/{id}", providerId), admin, null);
        JsonNode providers = call(get("/api/v1/auth/sso/providers"), null, null);
        for (JsonNode item : providers) {
            assertThat(item.path("code").asText()).isNotEqualTo("corp-ad");
        }
    }

    /* ------------------------------------------------------------------ 工具 */

    private long createOidcProvider(String admin, String code) throws Exception {
        JsonNode created = call(post("/api/v1/sys/sso-providers"), admin,
                json(oidcProviderBody(code)));
        return created.path("id").asLong();
    }

    private Map<String, Object> oidcProviderBody(String code) {
        return Map.of(
                "code", code, "name", "集团统一身份", "type", "OIDC",
                "autoProvision", true, "defaultRoleCode", "BUSINESS",
                "config", Map.of(
                        "issuer", ISSUER,
                        "authorizationEndpoint", "http://127.0.0.1:" + PORT + "/authorize",
                        "tokenEndpoint", "http://127.0.0.1:" + PORT + "/token",
                        "jwksEndpoint", "http://127.0.0.1:" + PORT + "/jwks",
                        "clientId", CLIENT_ID,
                        "clientSecret", "s3cret",
                        "redirectUri", "http://localhost:5173/login",
                        "usernameClaim", "preferred_username"));
    }

    /** 解出自签 state 里的 nonce（测试专用，生产端只验签不解析）。 */
    private String nonceOf(String state) throws Exception {
        String body = state.substring(0, state.lastIndexOf('.'));
        JsonNode node = objectMapper.readTree(Base64.getUrlDecoder().decode(body));
        return node.path("n").asText();
    }

    private static void respond(HttpExchange exchange, int status, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } catch (Exception ignored) {
            // 测试端点无需处理写失败
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new LinkedHashMap<>();
        if (body == null || body.isBlank()) {
            return form;
        }
        for (String pair : body.split("&")) {
            int index = pair.indexOf('=');
            if (index > 0) {
                form.put(URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    /** 用测试私钥签一个 id_token（RS256）。 */
    private static String idToken(String nonce) throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", KID);
        long now = Instant.now().getEpochSecond();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", ISSUER);
        payload.put("aud", CLIENT_ID);
        payload.put("sub", SUBJECT);
        payload.put("iat", now);
        payload.put("exp", now + 300);
        payload.put("nonce", nonce);
        payload.put("preferred_username", USERNAME);
        payload.put("name", "张三");
        payload.put("email", "zhangsan@example.com");
        String headerPart = base64Url(new ObjectMapper().writeValueAsBytes(header));
        String payloadPart = base64Url(new ObjectMapper().writeValueAsBytes(payload));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(KEY_PAIR.getPrivate());
        signature.update((headerPart + "." + payloadPart).getBytes(StandardCharsets.US_ASCII));
        return headerPart + "." + payloadPart + "." + base64Url(signature.sign());
    }

    private static byte[] unsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload)
            throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        JsonNode root = body(result);
        return root.has("data") ? root.path("data") : root;
    }

    private JsonNode body(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
