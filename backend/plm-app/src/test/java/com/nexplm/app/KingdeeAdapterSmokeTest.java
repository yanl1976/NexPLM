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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P9-2 金蝶适配器冒烟：对象 → 源报文 → 映射 → 真实发送，四类链路 + 幂等 + 版本键。
 */
@SpringBootTest
@AutoConfigureMockMvc
class KingdeeAdapterSmokeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final HttpServer SERVER;
    private static final int PORT;
    private static final AtomicReference<String> LAST_BODY = new AtomicReference<>("{}");

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SERVER.createContext("/kingdee", exchange -> {
                LAST_BODY.set(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                respond(exchange, 200, "{\"Result\":{\"ResponseStatus\":{\"IsSuccess\":true}}}");
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
        registry.add("nexplm.integration.retry.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void pushMaterialBomDocRouteToKingdee() throws Exception {
        String admin = login("admin", "Admin@12345");
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);

        // 端点 + 物料映射（本地字段 → 金蝶 F 前缀字段）
        JsonNode endpoint = call(post("/api/v1/itg/endpoints"), admin, json(Map.of(
                "code", "KINGDEE-TEST", "name", "金蝶测试账套", "systemCode", "KINGDEE",
                "baseUrl", "http://127.0.0.1:" + PORT + "/kingdee")));
        long endpointId = endpoint.path("id").asLong();
        call(post("/api/v1/itg/mappings"), admin, json(Map.of(
                "endpointId", endpointId, "objectCode", "Material", "direction", "OUT",
                "rules", List.of(
                        rule("code", "FNumber", "UPPER", true),
                        rule("name", "FName", null, true),
                        rule("specification", "FSpecification", null, false),
                        rule("weight", "FWeight", "NUMBER", false)))));

        long itemId = insertItem(companyId, "PN-TEST-1", "支架");
        jdbcTemplate.update("""
                INSERT INTO item_master (object_id, item_type, specification, material, unit,
                                         weight, source_type, std_code)
                VALUES (?, 'PART', '80x60', 'Q235', 'EA', 2.5, 'MAKE', 'GB/T 700')
                """, itemId);

        // 1. 预览：只组装源报文，不落任务
        JsonNode preview = call(post("/api/v1/itg/kingdee/preview"), admin, json(Map.of(
                "bizType", "Material", "bizId", itemId)));
        assertThat(preview.path("bizKey").asText()).isEqualTo("PN-TEST-1@R1");
        assertThat(preview.path("payload").path("specification").asText()).isEqualTo("80x60");
        assertThat(call(get("/api/v1/itg/jobs"), admin, null).path("total").asInt()).isZero();

        // 2. 下发：映射后报文真的发出去
        JsonNode pushed = call(post("/api/v1/itg/kingdee/push"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizId", itemId)));
        assertThat(pushed.path("job").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(pushed.path("job").path("bizKey").asText()).isEqualTo("PN-TEST-1@R1");
        assertThat(pushed.path("job").path("requestPayload").path("FNumber").asText())
                .isEqualTo("PN-TEST-1");
        assertThat(pushed.path("job").path("requestPayload").path("FWeight").isNumber()).isTrue();
        assertThat(LAST_BODY.get()).contains("FNumber").contains("PN-TEST-1");

        // 3. 幂等：同一版本重复下发返回原任务
        JsonNode again = call(post("/api/v1/itg/kingdee/push"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizId", itemId)));
        assertThat(again.path("duplicated").asBoolean()).isTrue();

        // 4. BOM 下发：子件按行带出
        long childId = insertItem(companyId, "PN-TEST-2", "衬套");
        jdbcTemplate.update("""
                INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no,
                                      qty, unit)
                VALUES (?, ?, ?, 'DESIGN', 10, 2, 'EA')
                """, System.nanoTime(), itemId, childId);
        JsonNode bom = call(post("/api/v1/itg/kingdee/push"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Bom", "bizId", itemId)));
        assertThat(bom.path("job").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(bom.path("job").path("bizKey").asText()).isEqualTo("PN-TEST-1@DESIGN@R1");
        assertThat(bom.path("job").path("requestPayload").path("lines")).hasSize(1);

        // 空 BOM 拒绝（不生成空报文）
        mockMvc.perform(post("/api/v1/itg/kingdee/push")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpointId", endpointId, "bizType", "Bom",
                                "bizId", childId))))
                .andExpect(status().isBadRequest());

        // 5. 图文档下发
        long docId = insertDoc(companyId, "DOC-TEST-1", "支架图纸");
        JsonNode doc = call(post("/api/v1/itg/kingdee/push"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Doc", "bizId", docId)));
        assertThat(doc.path("job").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(doc.path("job").path("bizKey").asText()).isEqualTo("DOC-TEST-1@R1");

        // 6. 工艺路线：草稿不能发，发布后才能发
        long routingId = insertRouting(companyId, "RT-TEST-1", itemId, "DRAFT");
        mockMvc.perform(post("/api/v1/itg/kingdee/push")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("endpointId", endpointId, "bizType", "Route",
                                "bizId", routingId))))
                .andExpect(status().isBadRequest());
        jdbcTemplate.update("UPDATE capp_routing SET status = 'RELEASED' WHERE id = ?", routingId);
        JsonNode route = call(post("/api/v1/itg/kingdee/push"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Route", "bizId", routingId)));
        assertThat(route.path("job").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(route.path("job").path("bizKey").asText()).isEqualTo("RT-TEST-1@V1");
        assertThat(route.path("job").path("requestPayload").path("steps")).hasSize(1);

        // 7. 不支持的类型如实报错，不伪造报文
        mockMvc.perform(post("/api/v1/itg/kingdee/preview")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bizType", "Requisition", "bizId", itemId))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/itg/kingdee/preview")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bizType", "Unknown", "bizId", itemId))))
                .andExpect(status().isBadRequest());

        // 8. dry-run：新版本只落 PREVIEW 任务，不发送
        String before = LAST_BODY.get();
        long dryItemId = insertItem(companyId, "PN-TEST-3", "端盖");
        jdbcTemplate.update("""
                INSERT INTO item_master (object_id, item_type, unit, source_type)
                VALUES (?, 'PART', 'EA', 'MAKE')
                """, dryItemId);
        JsonNode dry = call(post("/api/v1/itg/kingdee/push"), admin, json(Map.of(
                "endpointId", endpointId, "bizType", "Material", "bizId", dryItemId,
                "dryRun", true)));
        assertThat(dry.path("duplicated").asBoolean()).isFalse();
        assertThat(dry.path("job").path("status").asText()).isEqualTo("PREVIEW");
        assertThat(dry.path("job").path("requestPayload").path("FNumber").asText())
                .isEqualTo("PN-TEST-3");
        assertThat(LAST_BODY.get()).isEqualTo(before);
    }

    // ------------------------------------------------------------------ 工具

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private long insertItem(long companyId, String code, String name) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                        lifecycle_state)
                VALUES (?, 'PdmItem', ?, ?, ?, ?, 'Draft')
                """, id, id, code, name, companyId);
        return id;
    }

    private long insertDoc(long companyId, String code, String name) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                        lifecycle_state)
                VALUES (?, 'PdmDoc', ?, ?, ?, ?, 'Draft')
                """, id, id, code, name, companyId);
        jdbcTemplate.update("""
                INSERT INTO doc_master (object_id, doc_type, source, file_ext, file_count)
                VALUES (?, 'CAD', 'NORMAL', 'dwg', 1)
                """, id);
        return id;
    }

    private long insertRouting(long companyId, String code, long itemId, String status) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO capp_routing (id, company_id, code, name, object_id, object_type,
                                          version_no, status)
                VALUES (?, ?, ?, '支架加工路线', ?, 'PdmItem', 1, ?)
                """, id, companyId, code, itemId, status);
        jdbcTemplate.update("""
                INSERT INTO capp_routing_step (id, company_id, routing_id, seq_no, name, op_type,
                                               work_center, setup_minutes, run_minutes)
                VALUES (?, ?, ?, 10, '下料', 'MACHINING', '机加车间', 5, 12)
                """, System.nanoTime(), companyId, id);
        return id;
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
