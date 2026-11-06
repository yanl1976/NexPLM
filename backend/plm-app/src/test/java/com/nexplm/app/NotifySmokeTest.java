package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9-6 通知通道冒烟：企业微信/钉钉走真实 HTTP 端点（钉钉含加签），
 * 邮件通道在 SMTP 占位符为空时必须"明确失败"而不是静默成功。
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotifySmokeTest {

    private record Received(String path, String query, String body) {
    }

    private static final EmbeddedPostgres POSTGRES;
    private static final HttpServer ROBOT;
    private static final int PORT;
    private static final List<Received> RECEIVED = Collections.synchronizedList(new ArrayList<>());

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            ROBOT = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var handler = (com.sun.net.httpserver.HttpHandler) exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
                RECEIVED.add(new Received(exchange.getRequestURI().getPath(),
                        exchange.getRequestURI().getQuery(), body));
                respond(exchange, 200, "{\"errcode\":0,\"errmsg\":\"ok\"}");
            };
            ROBOT.createContext("/wecom", handler);
            ROBOT.createContext("/ding", handler);
            ROBOT.start();
            PORT = ROBOT.getAddress().getPort();
        } catch (Exception ex) {
            throw new IllegalStateException("启动机器人测试端点失败", ex);
        }
    }

    @AfterAll
    static void stopRobot() {
        ROBOT.stop(0);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("nexplm.security.bootstrap.admin-password", () -> "Admin@12345");
        registry.add("nexplm.integration.retry.enabled", () -> "false");
        registry.add("nexplm.webhook.retry.enabled", () -> "false");
        registry.add("nexplm.notify.retry.enabled", () -> "false");
        // SMTP 占位符保持为空：验证"没配就明确失败"
        registry.add("nexplm.notify.email.host", () -> "");
        registry.add("nexplm.notify.email.from", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearReceived() {
        RECEIVED.clear();
    }

    /** 企业微信通道：测试发送 → 真实端点收到 text 报文；文档发布事件按订阅分发。 */
    @Test
    void wecomChannelSendsAndReceivesDocRelease() throws Exception {
        String admin = login();
        long channelId = createChannel(admin, "notify-wecom", "企业微信群", "WECOM",
                List.of("doc.released"), Map.of(
                        "webhook", "http://127.0.0.1:" + PORT + "/wecom?key=abc"));

        JsonNode test = call(post("/api/v1/sys/notify-channels/{id}/test", channelId), admin, null);
        assertThat(test.path("status").asText()).isEqualTo("SENT");
        assertThat(test.path("response").asText()).contains("errcode");
        assertThat(RECEIVED).hasSize(1);
        JsonNode payload = objectMapper.readTree(RECEIVED.get(0).body());
        assertThat(payload.path("msgtype").asText()).isEqualTo("text");
        assertThat(payload.path("text").path("content").asText()).contains("通知通道测试");

        long docId = call(post("/api/v1/docs"), admin,
                json(Map.of("name", "通知验证文档", "docType", "DESIGN"))).path("id").asLong();
        call(post("/api/v1/objects/{id}/transitions", docId), admin, json(Map.of("action", "release")));

        JsonNode messages = call(get("/api/v1/sys/notify-channels/{id}/messages", channelId), admin, null);
        assertThat(messages.path("items").size()).isEqualTo(2);
        JsonNode latest = messages.path("items").get(0);
        assertThat(latest.path("eventType").asText()).isEqualTo("doc.released");
        assertThat(latest.path("status").asText()).isEqualTo("SENT");

        JsonNode detail = call(get("/api/v1/sys/notify-channels/messages/{id}",
                latest.path("id").asText()), admin, null);
        assertThat(detail.path("title").asText()).contains("文档发布");
        assertThat(detail.path("content").asText()).contains("通知验证文档");
        // webhook URL 里带 key，属凭据：详情不回传
        JsonNode channel = call(get("/api/v1/sys/notify-channels/{id}", channelId), admin, null);
        assertThat(channel.path("config").has("webhook")).isFalse();
        assertThat(channel.path("secretFields").toString()).contains("webhook");
    }

    /** 钉钉通道：配了 secret 必须带 timestamp+sign，签名要能被验证。 */
    @Test
    void dingtalkChannelSignsRequests() throws Exception {
        String admin = login();
        String secret = "SECtest123456";
        long channelId = createChannel(admin, "notify-ding", "钉钉群", "DINGTALK",
                List.of("task.completed"),
                Map.of("webhook", "http://127.0.0.1:" + PORT + "/ding?access_token=t1",
                        "secret", secret));

        JsonNode test = call(post("/api/v1/sys/notify-channels/{id}/test", channelId), admin, null);
        assertThat(test.path("status").asText()).isEqualTo("SENT");

        assertThat(RECEIVED).hasSize(1);
        Map<String, String> query = parseQuery(RECEIVED.get(0).query());
        assertThat(query).containsKeys("timestamp", "sign");
        String expected = sign(query.get("timestamp"), secret);
        assertThat(query.get("sign")).isEqualTo(expected);
    }

    /** 邮件通道：SMTP 占位符为空 → 明确失败并落 FAILED，可重发；配置校验照常生效。 */
    @Test
    void emailChannelFailsClosedWithoutSmtpPlaceholders() throws Exception {
        String admin = login();
        long channelId = createChannel(admin, "notify-mail", "运维邮件", "EMAIL",
                List.of("task.completed"), Map.of("to", "ops@example.com"));

        JsonNode test = call(post("/api/v1/sys/notify-channels/{id}/test", channelId), admin, null);
        assertThat(test.path("status").asText()).isEqualTo("FAILED");
        assertThat(test.path("errorMsg").asText()).contains("SMTP 未配置");
        assertThat(test.path("nextRetryAt").isNull()).isFalse();

        JsonNode resent = call(post("/api/v1/sys/notify-channels/messages/{id}/resend",
                test.path("id").asText()), admin, null);
        assertThat(resent.path("status").asText()).isEqualTo("FAILED");
        assertThat(resent.path("attempt").asInt()).isEqualTo(1);

        // 没填收件人 / 未知类型 / 未知事件 / 重复编码
        mockMvc.perform(post("/api/v1/sys/notify-channels")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "bad-mail", "name", "缺收件人", "type", "EMAIL",
                                "events", List.of("task.completed"), "config", Map.of()))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sys/notify-channels")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "bad-type", "name", "未知类型", "type", "SMS",
                                "events", List.of("task.completed"), "config", Map.of()))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sys/notify-channels")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "bad-event", "name", "未知事件", "type", "WECOM",
                                "events", List.of("doc.unknown"),
                                "config", Map.of("webhook", "http://127.0.0.1:" + PORT + "/wecom")))))
                .andExpect(status().isBadRequest());
        // 重复编码：先自建一条，再提交同编码（避免依赖其它用例的执行顺序）
        createChannel(admin, "notify-dup", "重复源", "WECOM", List.of("task.completed"),
                Map.of("webhook", "http://127.0.0.1:" + PORT + "/wecom"));
        mockMvc.perform(post("/api/v1/sys/notify-channels")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "notify-dup", "name", "重复", "type", "WECOM",
                                "events", List.of("doc.released"),
                                "config", Map.of("webhook", "http://127.0.0.1:" + PORT + "/wecom")))))
                .andExpect(status().isConflict());
    }

    /* ------------------------------------------------------------------ 工具 */

    private long createChannel(String admin, String code, String name, String type,
                               List<String> events, Map<String, Object> config) throws Exception {
        return call(post("/api/v1/sys/notify-channels"), admin, json(Map.of(
                "code", code, "name", name, "type", type, "events", events, "config", config)))
                .path("id").asLong();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int index = pair.indexOf('=');
            if (index > 0) {
                result.put(URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    /** 与服务端同一口径：base64(HMAC-SHA256(timestamp + "\n" + secret, secret))。 */
    private static String sign(String timestamp, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "\n" + secret).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
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
