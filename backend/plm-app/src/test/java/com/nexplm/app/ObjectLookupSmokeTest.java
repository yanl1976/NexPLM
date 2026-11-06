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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P10-3 扫码查对象冒烟：编码精确匹配、大小写不敏感、同一编码多类型、变更单兜底、查不到返回空。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ObjectLookupSmokeTest {

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
    void lookupByCodeExactly() throws Exception {
        String admin = login("admin", "Admin@12345");
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);

        insertObject(93_000_000_001L, "PdmItem", "PN-920001", "扫码零件", companyId, "Released");
        insertObject(93_000_000_002L, "PdmDoc", "DOC-920001", "扫码文档", companyId, "Draft");
        // 同一编码同时存在于物料与文档：编码规则各自独立，扫码要能都列出来让用户选
        insertObject(93_000_000_003L, "PdmItem", "SAME-001", "同码零件", companyId, "Draft");
        insertObject(93_000_000_004L, "PdmDoc", "SAME-001", "同码文档", companyId, "Released");
        jdbcTemplate.update("""
                        INSERT INTO chg_ecr (id, company_id, code, title, change_type, priority,
                                             status, requester_id)
                        VALUES (?, ?, ?, ?, 'DESIGN', 'HIGH', 'EXECUTING', ?)
                        """,
                93_000_000_011L, companyId, "ECR-920001", "扫码变更", 1L);

        // ---- 1. 精确匹配 + 返回最小字段
        JsonNode item = call("/api/v1/objects/lookup?code=PN-920001", admin);
        assertThat(item).hasSize(1);
        assertThat(item.get(0).path("objectType").asText()).isEqualTo("PdmItem");
        assertThat(item.get(0).path("code").asText()).isEqualTo("PN-920001");
        assertThat(item.get(0).path("name").asText()).isEqualTo("扫码零件");
        assertThat(item.get(0).path("revision").asInt()).isEqualTo(1);
        assertThat(item.get(0).path("lifecycleState").asText()).isEqualTo("Released");

        // ---- 2. 大小写不敏感（标签上手抄常写成大写）
        JsonNode lower = call("/api/v1/objects/lookup?code=pn-920001", admin);
        assertThat(lower).hasSize(1);
        assertThat(lower.get(0).path("code").asText()).isEqualTo("PN-920001");

        // ---- 3. 精确而不是模糊：PN-9200 不应命中 PN-920001
        JsonNode partial = call("/api/v1/objects/lookup?code=PN-9200", admin);
        assertThat(partial).isEmpty();

        // ---- 4. 同一编码两种对象类型都返回
        JsonNode same = call("/api/v1/objects/lookup?code=SAME-001", admin);
        assertThat(same).hasSize(2);

        // ---- 5. 变更单不在 plm_object，走兜底查询
        JsonNode ecr = call("/api/v1/objects/lookup?code=ECR-920001", admin);
        assertThat(ecr).hasSize(1);
        assertThat(ecr.get(0).path("objectType").asText()).isEqualTo("ChgEcr");
        assertThat(ecr.get(0).path("name").asText()).isEqualTo("扫码变更");
        assertThat(ecr.get(0).path("lifecycleState").asText()).isEqualTo("EXECUTING");
        // 变更单没有版本号：字段可能是 null，也可能被全局 NON_NULL 策略省略，两种都算"没有"
        assertThat(ecr.get(0).hasNonNull("revision")).isFalse();

        // ---- 6. 查不到是正常业务结果：200 + 空数组，不是 404
        JsonNode missing = call("/api/v1/objects/lookup?code=NOPE-9999", admin);
        assertThat(missing).isEmpty();

        // ---- 7. 空白编码 → 400（用 param 传真实空格，避免 uri 模板里的 %20 不解码）
        mockMvc.perform(get("/api/v1/objects/lookup").param("code", "   ")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ 工具

    private void insertObject(long id, String objectType, String code, String name, Long companyId,
                              String state) {
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                                lifecycle_state, is_latest, deleted)
                        VALUES (?, ?, ?, ?, ?, ?, ?, true, false)
                        """,
                id, objectType, id, code, name, companyId, state);
    }

    private JsonNode call(String url, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.has("data") ? root.path("data") : root;
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("accessToken").asText();
    }
}
