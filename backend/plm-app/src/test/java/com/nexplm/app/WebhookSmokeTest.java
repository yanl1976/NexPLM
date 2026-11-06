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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9-4 Webhook 冒烟：真实 HTTP 端点 + 真签名，覆盖「订阅 → 事件 → 签名投递 → 失败重试 → 重放」。
 *
 * <p>用真实 HTTP 而不是 mock：这一层的价值就是"对端到底收到了什么"，
 * mock 掉发送器就只剩下自己跟自己的状态机对齐。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookSmokeTest {

    /** 对端收到的一次投递（原始报文 + 相关请求头）。 */
    private record Received(String body, String event, String delivery, String signature) {
    }

    private static final EmbeddedPostgres POSTGRES;
    private static final HttpServer SERVER;
    private static final int PORT;
    private static final List<Received> RECEIVED = Collections.synchronizedList(new ArrayList<>());

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/hook/ok", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
                RECEIVED.add(new Received(body,
                        exchange.getRequestHeaders().getFirst("X-NexPLM-Event"),
                        exchange.getRequestHeaders().getFirst("X-NexPLM-Delivery"),
                        exchange.getRequestHeaders().getFirst("X-NexPLM-Signature")));
                respond(exchange, 200, "{\"received\":true}");
            });
            SERVER.createContext("/hook/fail", exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));
            SERVER.start();
            PORT = SERVER.getAddress().getPort();
        } catch (Exception ex) {
            throw new IllegalStateException("启动 Webhook 测试环境失败", ex);
        }
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("nexplm.security.bootstrap.admin-password", () -> "Admin@12345");
        // 本用例手动驱动重试与重放，关掉后台任务避免与断言抢状态
        registry.add("nexplm.integration.retry.enabled", () -> "false");
        registry.add("nexplm.webhook.retry.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearReceived() {
        RECEIVED.clear();
    }

    /** 文档发布 → 签名投递；未被订阅的事件不发；地址/事件/编码校验生效。 */
    @Test
    void docReleaseDeliversSignedWebhookAndFiltersEvents() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 非 http 地址拒绝（防止 file:// 读服务端本地文件）
        mockMvc.perform(post("/api/v1/itg/webhooks")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "WH-BAD-URL", "name", "非法地址",
                                "url", "file:///c:/secret.txt", "events", List.of("doc.released")))))
                .andExpect(status().isBadRequest());
        // 未知事件类型拒绝
        mockMvc.perform(post("/api/v1/itg/webhooks")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "WH-BAD-EVENT", "name", "未知事件",
                                "url", "http://127.0.0.1:" + PORT + "/hook/ok",
                                "events", List.of("doc.unknown")))))
                .andExpect(status().isBadRequest());

        JsonNode created = call(post("/api/v1/itg/webhooks"), admin, json(Map.of(
                "code", "WH-DOC", "name", "文档发布订阅",
                "url", "http://127.0.0.1:" + PORT + "/hook/ok",
                "events", List.of("doc.released"), "secret", "s3cret")));
        long webhookId = created.path("webhook").path("id").asLong();
        assertThat(created.path("secret").asText()).isEqualTo("s3cret");

        // 详情不回传密钥
        JsonNode detail = call(get("/api/v1/itg/webhooks/{id}", webhookId), admin, null);
        assertThat(detail.has("secret")).isFalse();
        assertThat(detail.path("url").asText()).contains("/hook/ok");

        // 编码重复 409
        mockMvc.perform(post("/api/v1/itg/webhooks")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "WH-DOC", "name", "重复编码",
                                "url", "http://127.0.0.1:" + PORT + "/hook/ok",
                                "events", List.of("doc.released")))))
                .andExpect(status().isConflict());

        // 只订阅「任务完成」的订阅不应收到文档发布
        long taskHookId = call(post("/api/v1/itg/webhooks"), admin, json(Map.of(
                "code", "WH-TASK", "name", "任务完成订阅",
                "url", "http://127.0.0.1:" + PORT + "/hook/ok",
                "events", List.of("task.completed")))).path("webhook").path("id").asLong();

        long docId = call(post("/api/v1/docs"), admin,
                json(Map.of("name", "签名验证文档", "docType", "DESIGN"))).path("id").asLong();
        JsonNode released = call(post("/api/v1/objects/{id}/transitions", docId), admin,
                json(Map.of("action", "release", "reason", "Webhook 联调")));
        assertThat(released.path("lifecycleState").asText()).isEqualTo("Released");

        JsonNode deliveries = call(get("/api/v1/itg/webhooks/{id}/deliveries", webhookId), admin, null);
        assertThat(deliveries.path("total").asLong()).isEqualTo(1);
        JsonNode delivery = deliveries.path("items").get(0);
        assertThat(delivery.path("status").asText()).isEqualTo("SENT");
        assertThat(delivery.path("eventType").asText()).isEqualTo("doc.released");
        assertThat(delivery.path("httpStatus").asInt()).isEqualTo(200);

        Received received = receivedFor(delivery.path("id").asText());
        assertThat(received.event()).isEqualTo("doc.released");
        assertThat(received.signature()).isEqualTo("sha256=" + hmacHex("s3cret", received.body()));
        JsonNode payload = objectMapper.readTree(received.body());
        assertThat(payload.path("event").asText()).isEqualTo("doc.released");
        assertThat(payload.path("data").path("code").asText()).startsWith("DOC-");
        assertThat(payload.path("data").path("lifecycleState").asText()).isEqualTo("Released");
        assertThat(payload.path("data").path("name").asText()).isEqualTo("签名验证文档");

        JsonNode taskDeliveries = call(get("/api/v1/itg/webhooks/{id}/deliveries", taskHookId),
                admin, null);
        assertThat(taskDeliveries.path("total").asLong()).isZero();
    }

    /** 对端 500 → 落 FAILED 待重试；改好地址后重放成功；已成功的投递不能重放。 */
    @Test
    void failedDeliveryRetriesAndReplays() throws Exception {
        String admin = login("admin", "Admin@12345");

        long webhookId = call(post("/api/v1/itg/webhooks"), admin, json(Map.of(
                "code", "WH-FAIL", "name", "失败重试验证",
                "url", "http://127.0.0.1:" + PORT + "/hook/fail",
                "events", List.of("doc.released"), "maxAttempts", 3))).path("webhook").path("id").asLong();

        long docId = call(post("/api/v1/docs"), admin,
                json(Map.of("name", "投递失败文档", "docType", "DESIGN"))).path("id").asLong();
        call(post("/api/v1/objects/{id}/transitions", docId), admin,
                json(Map.of("action", "release")));

        JsonNode deliveries = call(get("/api/v1/itg/webhooks/{id}/deliveries", webhookId), admin, null);
        JsonNode delivery = deliveries.path("items").get(0);
        assertThat(delivery.path("status").asText()).isEqualTo("FAILED");
        assertThat(delivery.path("attempt").asInt()).isEqualTo(1);
        assertThat(delivery.path("nextRetryAt").isNull()).isFalse();
        assertThat(delivery.path("errorMsg").asText()).contains("500");
        long deliveryId = delivery.path("id").asLong();

        // 修好对端地址（secret 不传表示保持不变）
        call(put("/api/v1/itg/webhooks/{id}", webhookId), admin, json(Map.of(
                "code", "WH-FAIL", "name", "失败重试验证",
                "url", "http://127.0.0.1:" + PORT + "/hook/ok",
                "events", List.of("doc.released"))));

        JsonNode replayed = call(post("/api/v1/itg/webhooks/deliveries/{id}/replay", deliveryId),
                admin, null);
        assertThat(replayed.path("status").asText()).isEqualTo("SENT");
        assertThat(replayed.path("attempt").asInt()).isEqualTo(1);
        // 重放用的是原报文快照，不是重新取源数据
        Received replayedBody = receivedFor(String.valueOf(deliveryId));
        assertThat(objectMapper.readTree(replayedBody.body()).path("data").path("code").asText())
                .startsWith("DOC-");

        // 已成功的不允许再重放
        mockMvc.perform(post("/api/v1/itg/webhooks/deliveries/{id}/replay", deliveryId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    /** 任务完成（审核通过）也会推送，载荷带项目与 WBS。 */
    @Test
    void taskCompletionDeliversWebhook() throws Exception {
        String admin = login("admin", "Admin@12345");

        long webhookId = call(post("/api/v1/itg/webhooks"), admin, json(Map.of(
                "code", "WH-TASK-OK", "name", "任务完成订阅",
                "url", "http://127.0.0.1:" + PORT + "/hook/ok",
                "events", List.of("task.completed")))).path("webhook").path("id").asLong();

        JsonNode project = call(post("/api/v1/prj/projects"), admin, json(Map.of(
                "name", "Webhook 任务项目", "type", "RD", "priority", 2)));
        long projectId = project.path("id").asLong();

        Map<String, Object> taskBody = new LinkedHashMap<>();
        taskBody.put("name", "事件验证任务");
        taskBody.put("type", "TASK");
        taskBody.put("planStart", "2026-11-01");
        taskBody.put("planEnd", "2026-11-10");
        JsonNode board = call(post("/api/v1/prj/projects/{id}/tasks", projectId), admin,
                json(taskBody));
        JsonNode tasks = board.path("tasks");
        long taskId = tasks.get(tasks.size() - 1).path("id").asLong();

        call(post("/api/v1/prj/tasks/{id}/start", taskId), admin, null);
        call(post("/api/v1/prj/tasks/{id}/submit", taskId), admin, json(Map.of("note", "完成")));
        call(post("/api/v1/prj/tasks/{id}/approve", taskId), admin, null);

        JsonNode deliveries = call(get("/api/v1/itg/webhooks/{id}/deliveries", webhookId), admin, null);
        assertThat(deliveries.path("total").asLong()).isEqualTo(1);
        JsonNode delivery = deliveries.path("items").get(0);
        assertThat(delivery.path("status").asText()).isEqualTo("SENT");

        JsonNode payload = objectMapper.readTree(receivedFor(delivery.path("id").asText()).body());
        assertThat(payload.path("event").asText()).isEqualTo("task.completed");
        assertThat(payload.path("data").path("taskId").asLong()).isEqualTo(taskId);
        assertThat(payload.path("data").path("projectId").asLong()).isEqualTo(projectId);
        assertThat(payload.path("data").path("wbsCode").asText()).isNotBlank();
        assertThat(payload.path("data").path("status").asText()).isEqualTo("DONE");
    }

    /* ------------------------------------------------------------------ 工具 */

    /** 按投递 id 找对端收到的那次回调（多个订阅并存时只认本条投递）。 */
    private Received receivedFor(String deliveryId) {
        return RECEIVED.stream().filter(item -> deliveryId.equals(item.delivery())).findFirst()
                .orElseThrow(() -> new AssertionError("对端未收到投递：" + deliveryId));
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
            // 测试端点不需要处理写失败
        }
    }

    private static String hmacHex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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
