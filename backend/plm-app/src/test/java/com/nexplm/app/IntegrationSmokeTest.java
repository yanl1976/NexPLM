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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9-1 集成框架冒烟：用 JDK 内嵌 HTTP 端点验证「映射 → 幂等提交 → 发送 → 重试 → 死信 → 重放」。
 *
 * <p>用真实 HTTP 而不是 mock：这一层的价值恰恰在于"真的发出去了/真的失败了"，
 * mock 掉发送器就只剩下状态机自娱自乐。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class IntegrationSmokeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final HttpServer SERVER;
    private static final int PORT;
    private static final AtomicInteger FLAKY_HITS = new AtomicInteger();
    private static final AtomicInteger OK_HITS = new AtomicInteger();

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/ok", exchange -> {
                OK_HITS.incrementAndGet();
                respond(exchange, 200, "{\"result\":\"ok\"}");
            });
            SERVER.createContext("/fail", exchange -> respond(exchange, 500, "{\"error\":\"boom\"}"));
            SERVER.createContext("/flaky", exchange -> {
                int hit = FLAKY_HITS.incrementAndGet();
                respond(exchange, hit <= 1 ? 500 : 200, "{\"try\":" + hit + "}");
            });
            SERVER.start();
            PORT = SERVER.getAddress().getPort();
        } catch (IOException ex) {
            throw new IllegalStateException("启动测试环境失败", ex);
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
        // 后台重试任务关掉，本用例手动驱动，避免与断言抢状态
        registry.add("nexplm.integration.retry.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void mappingIdempotentRetryDeadLetterReplay() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 建端点（认证信息只写不读）
        JsonNode endpoint = call(post("/api/v1/itg/endpoints"), admin, json(Map.of(
                "code", "ERP-TEST", "name", "测试 ERP", "systemCode", "KINGDEE",
                "baseUrl", "http://127.0.0.1:" + PORT + "/ok",
                "authType", "BEARER", "authConfig", Map.of("token", "secret-token"),
                "headers", Map.of("X-Tenant", "T001"), "timeoutMs", 5000)));
        long endpointId = endpoint.path("id").asLong();
        assertThat(endpoint.path("authConfigured").asBoolean()).isTrue();
        assertThat(endpoint.toString()).doesNotContain("secret-token");

        // 2. 参数校验：非 http 地址 / 未知认证 / 超时越界 / 重复编码
        mockMvc.perform(post("/api/v1/itg/endpoints")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(endpointBody("BAD-SCHEME", "file:///etc/passwd", "NONE"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/itg/endpoints")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(endpointBody("BAD-AUTH", "http://127.0.0.1:" + PORT + "/ok",
                                "WECHAT"))))
                .andExpect(status().isBadRequest());
        Map<String, Object> tooLong = endpointBody("BAD-TIMEOUT",
                "http://127.0.0.1:" + PORT + "/ok", "NONE");
        tooLong.put("timeoutMs", 999999);
        mockMvc.perform(post("/api/v1/itg/endpoints")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(tooLong)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/itg/endpoints")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(endpointBody("ERP-TEST", "http://127.0.0.1:" + PORT + "/ok",
                                "NONE"))))
                .andExpect(status().isConflict());

        // 3. 建映射：code→Number(UPPER)、name→Name(必填)、qty→Qty(NUMBER)
        JsonNode mapping = call(post("/api/v1/itg/mappings"), admin, json(Map.of(
                "endpointId", endpointId, "objectCode", "Material", "direction", "OUT",
                "rules", List.of(
                        rule("code", "Number", "UPPER", true),
                        rule("name", "Name", null, true),
                        rule("qty", "Qty", "NUMBER", false)))));
        long mappingId = mapping.path("id").asLong();
        assertThat(mapping.path("rules")).hasSize(3);
        mockMvc.perform(post("/api/v1/itg/mappings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpointId", endpointId, "objectCode", "Material",
                                "direction", "OUT",
                                "rules", List.of(rule("code", "Number", null, false))))))
                .andExpect(status().isConflict());

        // 4. 提交任务：映射生效（大写 + 数值）后真的发出去
        JsonNode submitted = call(post("/api/v1/itg/jobs"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizKey", "M-001",
                "payload", Map.of("code", "m-001", "name", "支架", "qty", "12"))));
        JsonNode job = submitted.path("job");
        long jobId = job.path("id").asLong();
        assertThat(job.path("jobNo").asText()).startsWith("ITG-");
        assertThat(job.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(job.path("httpStatus").asInt()).isEqualTo(200);
        assertThat(job.path("requestPayload").path("Number").asText()).isEqualTo("M-001");
        assertThat(job.path("requestPayload").path("Qty").isNumber()).isTrue();
        assertThat(job.path("logs")).hasSize(1);
        assertThat(OK_HITS.get()).isEqualTo(1);

        // 5. 幂等：同 (端点, 业务类型, 业务键) 再提交 → 返回原任务且不重发
        JsonNode again = call(post("/api/v1/itg/jobs"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizKey", "M-001",
                "payload", Map.of("code", "m-999", "name", "另一个", "qty", "99"))));
        assertThat(again.path("duplicated").asBoolean()).isTrue();
        assertThat(again.path("job").path("id").asLong()).isEqualTo(jobId);
        assertThat(again.path("job").path("requestPayload").path("Number").asText())
                .isEqualTo("M-001");
        assertThat(OK_HITS.get()).isEqualTo(1);

        // 6. dry-run：只生成报文快照，永不发送
        JsonNode preview = call(post("/api/v1/itg/jobs"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizKey", "M-002",
                "payload", Map.of("code", "m-002", "name", "衬套"), "dryRun", true)));
        assertThat(preview.path("job").path("status").asText()).isEqualTo("PREVIEW");
        assertThat(preview.path("job").path("requestPayload").path("Number").asText())
                .isEqualTo("M-002");
        assertThat(OK_HITS.get()).isEqualTo(1);

        // 7. 必填字段缺失：映射阶段就拒绝，不产生半成品报文
        mockMvc.perform(post("/api/v1/itg/jobs")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpointId", endpointId, "bizType", "Material",
                                "bizKey", "M-003", "payload", Map.of("code", "m-003")))))
                .andExpect(status().isBadRequest());

        // 8. 失败重试 → 死信 → 重放
        JsonNode failEndpoint = call(post("/api/v1/itg/endpoints"), admin,
                json(endpointBody("ERP-FAIL", "http://127.0.0.1:" + PORT + "/fail", "NONE")));
        long failEndpointId = failEndpoint.path("id").asLong();
        JsonNode failed = call(post("/api/v1/itg/jobs"), admin, json(Map.of(
                "endpointId", failEndpointId, "bizType", "Bom", "bizKey", "B-001",
                "payload", Map.of("any", "value"), "maxAttempt", 2)));
        assertThat(failed.path("job").path("status").asText()).isEqualTo("FAILED");
        assertThat(failed.path("job").path("attempt").asInt()).isEqualTo(1);
        assertThat(failed.path("job").path("nextRetryAt").isNull()).isFalse();
        long failedJobId = failed.path("job").path("id").asLong();

        JsonNode dead = call(post("/api/v1/itg/jobs/{id}/dispatch", failedJobId), admin, null);
        assertThat(dead.path("status").asText()).isEqualTo("DEAD");
        assertThat(dead.path("attempt").asInt()).isEqualTo(2);
        assertThat(dead.path("logs")).hasSize(2);
        mockMvc.perform(post("/api/v1/itg/jobs/{id}/dispatch", failedJobId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 重放：仍失败（对端还是 500），但重试次数归零重新计数
        JsonNode replayed = call(post("/api/v1/itg/jobs/{id}/replay", failedJobId), admin, null);
        assertThat(replayed.path("status").asText()).isEqualTo("FAILED");
        assertThat(replayed.path("attempt").asInt()).isEqualTo(1);
        assertThat(replayed.path("logs")).hasSize(3);

        // 9. 偶发失败：第二次成功
        JsonNode flakyEndpoint = call(post("/api/v1/itg/endpoints"), admin,
                json(endpointBody("ERP-FLAKY", "http://127.0.0.1:" + PORT + "/flaky", "NONE")));
        JsonNode flaky = call(post("/api/v1/itg/jobs"), admin, json(Map.of(
                "endpointId", flakyEndpoint.path("id").asLong(), "bizType", "Route",
                "bizKey", "R-001", "payload", Map.of("any", "value"))));
        assertThat(flaky.path("job").path("status").asText()).isEqualTo("FAILED");
        JsonNode recovered = call(post("/api/v1/itg/jobs/{id}/dispatch",
                flaky.path("job").path("id").asLong()), admin, null);
        assertThat(recovered.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(recovered.path("httpStatus").asInt()).isEqualTo(200);

        // 10. 列表与过滤、详情日志、端点删除后历史任务仍可查
        JsonNode jobs = call(get("/api/v1/itg/jobs?status=SUCCESS"), admin, null);
        assertThat(jobs.path("total").asInt()).isGreaterThanOrEqualTo(2);
        JsonNode detail = call(get("/api/v1/itg/jobs/{id}", failedJobId), admin, null);
        assertThat(detail.path("bizType").asText()).isEqualTo("Bom");
        assertThat(detail.path("logs")).hasSize(3);
        mockMvc.perform(get("/api/v1/itg/jobs?status=WRONG")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        call(delete("/api/v1/itg/endpoints/{id}", failEndpointId), admin, null);
        JsonNode afterDelete = call(get("/api/v1/itg/jobs/{id}", failedJobId), admin, null);
        assertThat(afterDelete.path("endpointCode").asText()).isEqualTo("ERP-FAIL");
        assertThat(call(get("/api/v1/itg/mappings?endpointId=" + endpointId), admin, null)
                .path("total").asInt()).isEqualTo(1);

        // 11. 映射更新立即生效（无需重启）
        Map<String, Object> updateMapping = new LinkedHashMap<>();
        updateMapping.put("endpointId", endpointId);
        updateMapping.put("objectCode", "Material");
        updateMapping.put("direction", "OUT");
        updateMapping.put("rules", List.of(rule("code", "MaterialCode", "LOWER", true)));
        call(put("/api/v1/itg/mappings/{id}", mappingId), admin, json(updateMapping));
        JsonNode remapped = call(post("/api/v1/itg/jobs"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizKey", "M-004",
                "payload", Map.of("code", "M-004"))));
        assertThat(remapped.path("job").path("requestPayload").path("MaterialCode").asText())
                .isEqualTo("m-004");
        assertThat(remapped.path("job").path("requestPayload").has("Number")).isFalse();
    }

    // ------------------------------------------------------------------ 工具

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        // 读完请求体再回，避免发送端写一半被重置
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private Map<String, Object> endpointBody(String code, String baseUrl, String authType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("name", "端点 " + code);
        body.put("systemCode", "OTHER");
        body.put("baseUrl", baseUrl);
        body.put("authType", authType);
        return body;
    }

    private Map<String, Object> rule(String source, String target, String transform,
                                     boolean required) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("source", source);
        rule.put("target", target);
        if (transform != null) {
            rule.put("transform", transform);
        }
        rule.put("required", required);
        return rule;
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
