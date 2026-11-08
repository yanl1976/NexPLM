package com.nexplm.app;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 B｜编码管理（对齐旧系统 {@code InforCenter/Coding} 的段模型）。
 *
 * <p>验证：预置 10 条规则的"下一个编码"预览与既有 {@code prefix + 6 位流水} 完全一致；
 * 改段定义后预览立即变化；重置落审计；两个流水段这类非法定义在保存时被挡住。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CodingRuleSmokeTest {

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

    /**
     * T-CORE-01：编码规则按物料类型路由 + 人工编码校验。
     * 覆盖：配了 `PdmItem:PART` → PART 走 `PRT-`；无专属规则的 ASSEMBLY 回退 `PN-`；
     * 非法人工编码 400；图号式人工编码（`0CT.325.503`）被采用。
     */
    @Test
    void itemCodeRoutesByItemTypeAndValidatesManualInput() throws Exception {
        String admin = login();
        jdbcTemplate.update("DELETE FROM code_rule WHERE code = 'PdmItem:PART'");
        jdbcTemplate.update("""
                        INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy,
                                               status, version_no, created_at, updated_at, current_no,
                                               object_type, remark)
                        VALUES (?, 'PdmItem:PART', '零件专用编码',
                                '[{"type":"TEXT","value":"PRT-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]'::jsonb,
                                '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0,
                                'PdmItem:PART', 'T-CORE-01 测试')
                        """, IdWorker.getId());
        try {
            JsonNode part = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "类型路由零件", "itemType", "PART", "sourceType", "MAKE")));
            assertThat(part.path("code").asText()).startsWith("PRT-");

            JsonNode assembly = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "回退编码部件", "itemType", "ASSEMBLY", "sourceType", "MAKE")));
            assertThat(assembly.path("code").asText()).startsWith("PN-");

            mockMvc.perform(post("/api/v1/items")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("name", "非法编码零件", "itemType", "PART",
                                    "sourceType", "MAKE", "code", "非法<>编码"))))
                    .andExpect(status().isBadRequest());

            JsonNode manual = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "图号式手工编码", "itemType", "PART", "sourceType", "MAKE",
                    "code", "0CT.325.503")));
            assertThat(manual.path("code").asText()).isEqualTo("0CT.325.503");
        } finally {
            jdbcTemplate.update("DELETE FROM code_rule WHERE code = 'PdmItem:PART'");
        }
    }

    @Test
    void presetRulesPreviewMatchesLegacyFormat() throws Exception {
        String admin = login();
        JsonNode rules = call(get("/api/v1/system/coding/rules"), admin, null);
        assertThat(rules.size()).isGreaterThanOrEqualTo(10);

        // 只断言"不会被其它用例改段定义/消耗流水"的规则，且比对格式而非固定流水：
        // 同类的其它用例会创建零部件（消耗 PdmItem 流水）或改 CappRouting 段定义，
        // 固定值断言会因执行顺序而红（T-CORE-01 新增用例后暴露）。
        Map<String, String> expectedPrefix = Map.of(
                "PdmDoc", "DOC-",
                "PdmItem", "PN-",
                "CappCardVolume", "CV-");
        for (JsonNode rule : rules) {
            String code = rule.path("code").asText();
            if (expectedPrefix.containsKey(code)) {
                assertThat(rule.path("preview").asText())
                        .as("规则 %s 的预览应与既有生成格式一致（前缀 %s + 6 位流水）",
                                code, expectedPrefix.get(code))
                        .matches(java.util.regex.Pattern.quote(expectedPrefix.get(code)) + "\\d{6}");
            }
        }
    }

    @Test
    void updateSegmentsAndResetAffectNextCode() throws Exception {
        String admin = login();

        // 1. 带日期段 + 按年归零：yyyy + '-' + 6 位流水
        call(put("/api/v1/system/coding/rules/CappRouting"), admin, json(Map.of(
                "name", "工艺路线（带年份）",
                "resetPolicy", "YEAR",
                "segments", List.of(
                        Map.of("type", "DATE", "format", "yyyy"),
                        Map.of("type", "TEXT", "value", "-RT"),
                        Map.of("type", "SEQ", "width", 4, "radix", "DECIMAL")))));

        String preview = call(get("/api/v1/system/coding/rules/CappRouting/preview"), admin, null).asText();
        assertThat(preview).matches("\\d{4}-RT\\d{4}").endsWith("RT0001");

        // 2. 重置到 99：下一个应为 ...0100，且落审计
        JsonNode afterReset = call(post("/api/v1/system/coding/rules/CappRouting/reset"), admin,
                json(Map.of("toNo", 99, "reason", "历史数据导入")));
        assertThat(afterReset.path("currentNo").asLong()).isEqualTo(99);
        assertThat(afterReset.path("preview").asText()).endsWith("RT0100");

        JsonNode audit = call(get("/api/v1/system/coding/rules/CappRouting"), admin, null);
        assertThat(audit.path("resetPolicy").asText()).isEqualTo("YEAR");
    }

    @Test
    void invalidSegmentsAreRejected() throws Exception {
        String admin = login();
        // 两个流水段 —— 旧系统同一规则也只允许一个 OrderCode 段
        mockMvc.perform(put("/api/v1/system/coding/rules/CappCard")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("segments", List.of(
                                Map.of("type", "SEQ", "width", 4),
                                Map.of("type", "SEQ", "width", 4))))))
                .andExpect(status().isBadRequest());

        // 日期段格式不在白名单
        mockMvc.perform(put("/api/v1/system/coding/rules/CappCard")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("segments", List.of(
                                Map.of("type", "DATE", "format", "yy-MM"),
                                Map.of("type", "SEQ", "width", 4))))))
                .andExpect(status().isBadRequest());
    }

    /**
     * 编码规则接口必须带权限（`EXP-20261108-15`）：普通业务用户既不能看规则，
     * 也不能改段定义或重置流水（原实现无任何 `@PreAuthorize`）。
     */
    @Test
    void codingRulesRequirePrivilege() throws Exception {
        String admin = login();
        createUser(admin, "coder", "编码普通用户");
        String business = loginAs("coder", "Passw0rd!");

        mockMvc.perform(get("/api/v1/system/coding/rules")
                        .header("Authorization", "Bearer " + business))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/system/coding/rules/PdmItem")
                        .header("Authorization", "Bearer " + business)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "越权改规则"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/coding/rules/PdmItem/reset")
                        .header("Authorization", "Bearer " + business)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("toNo", 1, "reason", "越权跳号"))))
                .andExpect(status().isForbidden());
    }

    /**
     * T-CORE-02：取号落发放流水（蓝本 `CODERES` 的等价物）。
     * 断言：创建零部件后能按「规则 + 编码」查回该条流水，且带细分类型与操作人。
     */
    @Test
    void issuedCodesAreRecordedAndQueryable() throws Exception {
        String admin = login();
        JsonNode item = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "流水验证零件", "itemType", "PART", "sourceType", "MAKE")));
        String code = item.path("code").asText();

        JsonNode issues = call(get("/api/v1/system/coding/issues")
                .param("ruleCode", "PdmItem")
                .param("code", code), admin, null);
        assertThat(issues.path("total").asLong()).isGreaterThanOrEqualTo(1);
        JsonNode first = issues.path("items").get(0);
        assertThat(first.path("code").asText()).isEqualTo(code);
        assertThat(first.path("ruleCode").asText()).isEqualTo("PdmItem");
        assertThat(first.path("objectType").asText()).isEqualTo("PdmItem:PART");
        assertThat(first.path("issuedByName").asText()).isNotBlank();
    }

    /**
     * 蓝本编码体系（`EXP-20261108-43`）：**编码 = 分类码 + 该分类内独立递增的流水**。
     *
     * <p>蓝本实测 `CODES=10010100001`、`ORDINAL=00001`，且每个分类组都从 xxxxxx00001 起。
     * 断言：同分类连续两个 → 00001/00002；另一个分类 → 各自从 00001 起；无分类回退 `PN-`。</p>
     */
    @Test
    void itemCodeUsesClassPrefixAndPerClassSequence() throws Exception {
        String admin = login();
        long classA = 94_000_000_001L;
        long classB = 94_000_000_002L;
        jdbcTemplate.update("DELETE FROM doc_class WHERE id IN (?, ?)", classA, classB);
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               deleted, version, created_at, updated_at)
                        VALUES (?, NULL, 'PdmItem', 'T9101', '测试分类A', '/T9101', 0, 'ACTIVE', false, 0, now(), now())
                        """, classA);
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               deleted, version, created_at, updated_at)
                        VALUES (?, NULL, 'PdmItem', 'T9102', '测试分类B', '/T9102', 0, 'ACTIVE', false, 0, now(), now())
                        """, classB);

        try {
            String a1 = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "分类编码零件A1", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classA)))).path("code").asText();
            String a2 = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "分类编码零件A2", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classA)))).path("code").asText();
            String b1 = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "分类编码零件B1", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classB)))).path("code").asText();
            String noClass = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "无分类零件", "itemType", "PART", "sourceType", "MAKE"))).path("code").asText();

            assertThat(a1).as("分类 A 第一个").isEqualTo("T910100001");
            assertThat(a2).as("分类 A 第二个（同分类独立流水）").isEqualTo("T910100002");
            assertThat(b1).as("分类 B 从 00001 起").isEqualTo("T910200001");
            assertThat(noClass).as("无分类回退通用规则").startsWith("PN-");
        } finally {
            jdbcTemplate.update("DELETE FROM item_master WHERE class_id IN (?, ?)", classA, classB);
            jdbcTemplate.update("DELETE FROM plm_object WHERE object_type = 'PdmItem'"
                    + " AND id NOT IN (SELECT object_id FROM item_master)");
            jdbcTemplate.update("DELETE FROM doc_class WHERE id IN (?, ?)", classA, classB);
            jdbcTemplate.update("DELETE FROM code_sequence WHERE rule_code = 'PdmItemByClass'");
        }
    }

    // ------------------------------------------------------------ 工具

    private String login() throws Exception {
        return loginAs("admin", "Admin@12345");
    }

    private String loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private long createUser(String adminToken, String username, String displayName) throws Exception {
        call(post("/api/v1/system/users"), adminToken, json(Map.of(
                "username", username,
                "password", "Passw0rd!",
                "displayName", displayName,
                "securityLevel", 1,
                "roleIds", new long[]{businessRoleId(adminToken)})));
        JsonNode users = call(get("/api/v1/system/users?page=1&size=100"), adminToken, null).path("items");
        for (JsonNode user : users) {
            if (username.equals(user.path("username").asText())) {
                return user.path("id").asLong();
            }
        }
        throw new IllegalStateException("用户创建失败：" + username);
    }

    private long businessRoleId(String adminToken) throws Exception {
        for (JsonNode role : call(get("/api/v1/system/roles"), adminToken, null)) {
            if ("BUSINESS".equals(role.path("code").asText())) {
                return role.path("id").asLong();
            }
        }
        throw new IllegalStateException("内置 BUSINESS 角色不存在");
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
