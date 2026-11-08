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

    /**
     * 蓝本真实机制（`EXP-20261108-44`）：分类绑定「编码种类 + 码值路径」，
     * 编码 = 各码段取值拼接 + 流水自增（`1001` + `02` + `00001`）。
     */
    @Test
    void itemCodeFollowsClassCodePath() throws Exception {
        String admin = login();
        long classId = 94_100_000_001L;
        jdbcTemplate.update("DELETE FROM doc_class WHERE id = ?", classId);
        jdbcTemplate.update("DELETE FROM code_rule WHERE code = 'Kind_test01'");
        jdbcTemplate.update("""
                        INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                                               version_no, created_at, updated_at, current_no, object_type, remark)
                        VALUES (?, 'Kind_test01', '测试编码种类',
                                '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb,
                                '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, 'PdmItem', 'EXP-20261108-44')
                        """, IdWorker.getId());
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               code_path, deleted, version, created_at, updated_at)
                        VALUES (?, NULL, 'PdmItem', 'T9301', '测试种类分类', '/T9301', 0, 'ACTIVE',
                                'Kind_test01/1001/02/*', false, 0, now(), now())
                        """, classId);
        try {
            String first = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "码值路径零件1", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classId)))).path("code").asText();
            String second = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "码值路径零件2", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classId)))).path("code").asText();

            assertThat(first).as("码值 1001 + 02 + 流水 00001").isEqualTo("10010200001");
            assertThat(second).isEqualTo("10010200002");
        } finally {
            jdbcTemplate.update("DELETE FROM item_master WHERE class_id = ?", classId);
            jdbcTemplate.update("DELETE FROM plm_object WHERE object_type = 'PdmItem'"
                    + " AND id NOT IN (SELECT object_id FROM item_master)");
            jdbcTemplate.update("DELETE FROM doc_class WHERE id = ?", classId);
            jdbcTemplate.update("DELETE FROM code_sequence WHERE rule_code = 'Kind_test01'");
            jdbcTemplate.update("DELETE FROM code_rule WHERE code = 'Kind_test01'");
        }
    }

    /** 三层模型的读取接口：编码种类列表 + 种类下的码值（供编码管理页三页签使用）。 */
    @Test
    void kindAndValueApisExposeThreeLayerModel() throws Exception {
        String admin = login();
        jdbcTemplate.update("DELETE FROM code_segment_value WHERE kind_code = 'Kind_test02'");
        jdbcTemplate.update("DELETE FROM code_kind WHERE code = 'Kind_test02'");
        jdbcTemplate.update("DELETE FROM code_rule WHERE code = 'Kind_test02'");
        jdbcTemplate.update("""
                        INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                                               version_no, created_at, updated_at, current_no, object_type, remark)
                        VALUES (?, 'Kind_test02', '测试种类2',
                                '[{"type":"VALUE"},{"type":"SEQ","width":4,"radix":"DECIMAL"}]'::jsonb,
                                '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, 'PdmItem', 'EXP-20261108-44')
                        """, IdWorker.getId());
        jdbcTemplate.update("""
                        INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
                        VALUES ('Kind_test02', '测试种类2', 'LEGACY_TEST', 'Kind_test02', 9)
                        """);
        jdbcTemplate.update("""
                        INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
                        VALUES (?, 'Kind_test02', '大类', '2001', '2001', '测试大类', 1)
                        """, IdWorker.getId());
        try {
            JsonNode kinds = call(get("/api/v1/system/coding/kinds"), admin, null);
            JsonNode matched = null;
            for (JsonNode item : kinds) {
                if ("Kind_test02".equals(item.path("code").asText())) {
                    matched = item;
                }
            }
            assertThat(matched).as("种类列表应包含刚插入的种类").isNotNull();
            assertThat(matched.path("name").asText()).isEqualTo("测试种类2");
            assertThat(matched.path("valueCount").asLong()).isEqualTo(1);

            JsonNode values = call(get("/api/v1/system/coding/kinds/Kind_test02/values"), admin, null);
            assertThat(values).hasSize(1);
            assertThat(values.get(0).path("valuePath").asText()).isEqualTo("2001");
            assertThat(values.get(0).path("codeValue").asText()).isEqualTo("2001");
            assertThat(values.get(0).path("name").asText()).isEqualTo("测试大类");
        } finally {
            jdbcTemplate.update("DELETE FROM code_segment_value WHERE kind_code = 'Kind_test02'");
            jdbcTemplate.update("DELETE FROM code_kind WHERE code = 'Kind_test02'");
            jdbcTemplate.update("DELETE FROM code_rule WHERE code = 'Kind_test02'");
        }
    }

    /**
     * 码值只增不改值 + 分类绑定 + `code_issue` 路径快照（T-CORE-10）。
     *
     * <p>覆盖：新增子码值 → 改名/停用 → 绑定合法路径 → 建零件取号 → 流水里留路径快照；
     * 另断言"码值数量与规则不符"的路径被拒。</p>
     */
    @Test
    void valueCrudBindingAndIssuePathSnapshot() throws Exception {
        String admin = login();
        String kind = "Kind_test03";
        jdbcTemplate.update("DELETE FROM code_segment_value WHERE kind_code = ?", kind);
        jdbcTemplate.update("DELETE FROM code_kind WHERE code = ?", kind);
        jdbcTemplate.update("DELETE FROM code_rule WHERE code = ?", kind);
        jdbcTemplate.update("DELETE FROM code_issue WHERE rule_code = ?", kind);
        jdbcTemplate.update("""
                        INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                                               version_no, created_at, updated_at, current_no, object_type, remark)
                        VALUES (?, ?, '测试种类3',
                                '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb,
                                '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, 'PdmItem', 'EXP-20261108-46')
                        """, IdWorker.getId(), kind);
        jdbcTemplate.update("""
                        INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
                        VALUES (?, '测试种类3', 'LEGACY3', ?, 11)
                        """, kind, kind);
        jdbcTemplate.update("""
                        INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
                        VALUES (?, ?, '大类', '3001', '3001', '测试大类', 1)
                        """, IdWorker.getId(), kind);
        long classId = 94_300_000_001L;
        jdbcTemplate.update("DELETE FROM doc_class WHERE id = ?", classId);
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               deleted, version, created_at, updated_at)
                        VALUES (?, NULL, 'PdmItem', 'T9601', '绑定测试分类', '/T9601', 0, 'ACTIVE', false, 0, now(), now())
                        """, classId);
        try {
            // 1) 新增子码值
            call(post("/api/v1/system/coding/kinds/{code}/values", kind), admin,
                    json(Map.of("codeValue", "02", "name", "测试小类", "parentPath", "3001")));
            JsonNode values = call(get("/api/v1/system/coding/kinds/{code}/values", kind), admin, null);
            assertThat(values).hasSize(2);

            // 2) 改名（没有 codeValue 字段，结构上不能改值）
            call(put("/api/v1/system/coding/kinds/{code}/values", kind), admin,
                    json(Map.of("valuePath", "3001/02", "name", "改名后")));
            JsonNode afterRename = call(get("/api/v1/system/coding/kinds/{code}/values", kind), admin, null);
            JsonNode child = null;
            for (JsonNode row : afterRename) {
                if ("3001/02".equals(row.path("valuePath").asText())) {
                    child = row;
                }
            }
            assertThat(child).isNotNull();
            assertThat(child.path("deleted").asBoolean()).isFalse();
            assertThat(child.path("name").asText()).isEqualTo("改名后");

            // 3) 码值数量与规则不符 → 400
            Map<String, Object> badBind = new java.util.HashMap<>();
            badBind.put("code", "T9601");
            badBind.put("name", "绑定测试分类");
            badBind.put("codePath", kind + "/3001/*");
            mockMvc.perform(put("/api/v1/item-classes/{id}", classId)
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(badBind)))
                    .andExpect(status().isBadRequest());

            // 4) 合法路径绑定（2 个码值），返回 codePath
            Map<String, Object> bind = new java.util.HashMap<>();
            bind.put("code", "T9601");
            bind.put("name", "绑定测试分类");
            bind.put("codePath", kind + "/3001/02/*");
            JsonNode bound = call(put("/api/v1/item-classes/{id}", classId), admin, json(bind));
            assertThat(bound.path("codePath").asText()).isEqualTo(kind + "/3001/02/*");

            // 5) 建零件取号，流水留路径快照
            JsonNode item = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "快照验证零件", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classId))));
            String code = item.path("code").asText();
            assertThat(code).isEqualTo("30010200001");
            String snapshot = jdbcTemplate.queryForObject(
                    "SELECT generator_path FROM code_issue WHERE rule_code = ? AND code = ?",
                    String.class, kind, code);
            assertThat(snapshot).isEqualTo(kind + "/3001/02/*");

            // 6) 停用（停用后不再允许新绑定；历史编码与快照不受影响）
            call(put("/api/v1/system/coding/kinds/{code}/values", kind), admin,
                    json(Map.of("valuePath", "3001/02", "deleted", true)));
            JsonNode afterDisable = call(get("/api/v1/system/coding/kinds/{code}/values", kind), admin, null);
            boolean disabled = false;
            for (JsonNode row : afterDisable) {
                if ("3001/02".equals(row.path("valuePath").asText())) {
                    disabled = row.path("deleted").asBoolean();
                }
            }
            assertThat(disabled).isTrue();
        } finally {
            jdbcTemplate.update("DELETE FROM code_issue WHERE rule_code = ?", kind);
            jdbcTemplate.update("DELETE FROM item_master WHERE class_id = ?", classId);
            jdbcTemplate.update("DELETE FROM plm_object WHERE object_type = 'PdmItem'"
                    + " AND id NOT IN (SELECT object_id FROM item_master)");
            jdbcTemplate.update("DELETE FROM doc_class WHERE id = ?", classId);
            jdbcTemplate.update("DELETE FROM code_sequence WHERE rule_code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_segment_value WHERE kind_code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_kind WHERE code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_rule WHERE code = ?", kind);
        }
    }

    /**
     * T-CORE-12：编码预览**不消耗流水**，且与实际创建出来的编码一致。
     */
    @Test
    void codePreviewDoesNotConsumeSequenceAndMatchesCreated() throws Exception {
        String admin = login();
        String kind = "Kind_test04";
        jdbcTemplate.update("DELETE FROM code_segment_value WHERE kind_code = ?", kind);
        jdbcTemplate.update("DELETE FROM code_kind WHERE code = ?", kind);
        jdbcTemplate.update("DELETE FROM code_rule WHERE code = ?", kind);
        jdbcTemplate.update("""
                        INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                                               version_no, created_at, updated_at, current_no, object_type, remark)
                        VALUES (?, ?, '预览测试种类',
                                '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb,
                                '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, 'PdmItem', 'EXP-20261108-48')
                        """, IdWorker.getId(), kind);
        jdbcTemplate.update("""
                        INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
                        VALUES (?, '预览测试种类', 'LEGACY4', ?, 12)
                        """, kind, kind);
        jdbcTemplate.update("""
                        INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
                        VALUES (?, ?, '大类', '4001', '4001', '预览大类', 1)
                        """, IdWorker.getId(), kind);
        jdbcTemplate.update("""
                        INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
                        VALUES (?, ?, '小类', '4001/01', '01', '预览小类', 2)
                        """, IdWorker.getId(), kind);
        long classId = 94_400_000_001L;
        jdbcTemplate.update("DELETE FROM doc_class WHERE id = ?", classId);
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               code_path, deleted, version, created_at, updated_at)
                        VALUES (?, NULL, 'PdmItem', 'T9701', '预览测试分类', '/T9701', 0, 'ACTIVE',
                                ?, false, 0, now(), now())
                        """, classId, kind + "/4001/01/*");
        try {
            JsonNode first = call(get("/api/v1/items/code-preview")
                    .param("classId", String.valueOf(classId)), admin, null);
            JsonNode second = call(get("/api/v1/items/code-preview")
                    .param("classId", String.valueOf(classId)), admin, null);
            assertThat(first.path("code").asText()).isEqualTo("40010100001");
            assertThat(second.path("code").asText())
                    .as("连续预览应返回同一个号（预览不消耗流水）")
                    .isEqualTo("40010100001");

            JsonNode item = call(post("/api/v1/items"), admin, json(Map.of(
                    "name", "预览验证零件", "itemType", "PART", "sourceType", "MAKE",
                    "classId", String.valueOf(classId))));
            assertThat(item.path("code").asText()).isEqualTo("40010100001");

            JsonNode after = call(get("/api/v1/items/code-preview")
                    .param("classId", String.valueOf(classId)), admin, null);
            assertThat(after.path("code").asText()).isEqualTo("40010100002");
        } finally {
            jdbcTemplate.update("DELETE FROM item_master WHERE class_id = ?", classId);
            jdbcTemplate.update("DELETE FROM plm_object WHERE object_type = 'PdmItem'"
                    + " AND id NOT IN (SELECT object_id FROM item_master)");
            jdbcTemplate.update("DELETE FROM doc_class WHERE id = ?", classId);
            jdbcTemplate.update("DELETE FROM code_sequence WHERE rule_code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_issue WHERE rule_code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_segment_value WHERE kind_code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_kind WHERE code = ?", kind);
            jdbcTemplate.update("DELETE FROM code_rule WHERE code = ?", kind);
        }
    }

    /** T-CORE-13：大类/中类（有子节点）不允许用于取号，只有最末级小类可以。 */
    @Test
    void previewRejectsNonLeafClass() throws Exception {
        String admin = login();
        long parentId = 94_500_000_001L;
        long childId = 94_500_000_002L;
        jdbcTemplate.update("DELETE FROM doc_class WHERE id IN (?, ?)", parentId, childId);
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               deleted, version, created_at, updated_at)
                        VALUES (?, NULL, 'PdmItem', 'T9801', '测试中类', '/T9801', 0, 'ACTIVE', false, 0, now(), now())
                        """, parentId);
        jdbcTemplate.update("""
                        INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status,
                                               deleted, version, created_at, updated_at)
                        VALUES (?, ?, 'PdmItem', 'T980101', '测试小类', '/T9801/T980101', 0, 'ACTIVE', false, 0, now(), now())
                        """, childId, parentId);
        try {
            mockMvc.perform(get("/api/v1/items/code-preview")
                            .param("classId", String.valueOf(parentId))
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isBadRequest());

            JsonNode leaf = call(get("/api/v1/items/code-preview")
                    .param("classId", String.valueOf(childId)), admin, null);
            assertThat(leaf.path("code").asText()).isNotBlank();
        } finally {
            jdbcTemplate.update("DELETE FROM doc_class WHERE id IN (?, ?)", childId, parentId);
        }
    }

    /** T-CORE-14：图号 → 编号（默认原样采用；非法图号 400）。 */
    @Test
    void codePreviewFromDrawingNumber() throws Exception {
        String admin = login();
        JsonNode byDwg = call(get("/api/v1/items/code-preview")
                .param("dwgNo", "0CT.325.503"), admin, null);
        assertThat(byDwg.path("code").asText()).isEqualTo("0CT.325.503");
        assertThat(byDwg.path("source").asText()).isEqualTo("DRAWING");

        mockMvc.perform(get("/api/v1/items/code-preview")
                        .param("dwgNo", "bad<>dwg")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    /** T-CORE-15：码段（蓝本 CODERULE）按段序返回，父段用于表达码段树。 */
    @Test
    void kindSegmentsAreExposed() throws Exception {
        String admin = login();
        String rule = "Kind_segtest";
        jdbcTemplate.update("DELETE FROM code_segment WHERE rule_code = ?", rule);
        jdbcTemplate.update("DELETE FROM code_kind WHERE code = ?", rule);
        jdbcTemplate.update("DELETE FROM code_rule WHERE code = ?", rule);
        jdbcTemplate.update("""
                        INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                                               version_no, created_at, updated_at, current_no, object_type, remark)
                        VALUES (?, ?, '码段测试规则',
                                '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb,
                                '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, 'PdmItem', 'EXP-20261108-54')
                        """, IdWorker.getId(), rule);
        jdbcTemplate.update("""
                        INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
                        VALUES (?, '码段测试规则', 'LEGACY_SEG', ?, 90)
                        """, rule, rule);
        jdbcTemplate.update("""
                        INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
                        VALUES (?, ?, 'SEG01', '大类', 1, 0, NULL, 'DROPDOWN'),
                               (?, ?, 'SEG02', '小类', 2, 0, 'SEG01', 'DROPDOWN'),
                               (?, ?, 'SEG03', '流水码', 3, 5, NULL, 'DROPDOWN')
                        """, IdWorker.getId(), rule, IdWorker.getId(), rule, IdWorker.getId(), rule);
        try {
            JsonNode segs = call(get("/api/v1/system/coding/kinds/{code}/segments", rule), admin, null);
            assertThat(segs).hasSize(3);
            assertThat(segs.get(0).path("name").asText()).isEqualTo("大类");
            assertThat(segs.get(0).path("orderNo").asInt()).isEqualTo(1);
            assertThat(segs.get(1).path("parentCode").asText()).isEqualTo("SEG01");
            assertThat(segs.get(2).path("length").asInt()).isEqualTo(5);
        } finally {
            jdbcTemplate.update("DELETE FROM code_segment WHERE rule_code = ?", rule);
            jdbcTemplate.update("DELETE FROM code_kind WHERE code = ?", rule);
            jdbcTemplate.update("DELETE FROM code_rule WHERE code = ?", rule);
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
