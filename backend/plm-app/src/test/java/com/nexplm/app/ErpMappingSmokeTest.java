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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 G：ERP 三段字段映射冒烟。
 *
 * <p>覆盖：模板落库口径（32/9/24 列）、缺口统计、物料试算（嵌套路径 + 缺字段不炸）、
 * BOM 试算（分录数组 {@code [].} + 源数组路径 {@code lines[].}）、
 * 保存后缺口数变化、请购单试算明确拒绝。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErpMappingSmokeTest {

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
    void segmentTemplatesPreviewAndGapAccounting() throws Exception {
        String admin = login("admin", "Admin@12345");

        // ---- 1. 三段概览：方案 A 落库后物料只剩「金蝶内码」一个缺口，BOM 缺口清零
        JsonNode segments = call(get("/api/v1/itg/erp-mapping/segments"), admin);
        assertThat(segments).hasSize(3);
        JsonNode material = findSegment(segments, "Material");
        assertThat(material.path("ruleCount").asInt()).isEqualTo(32);
        assertThat(material.path("mappedCount").asInt()).isEqualTo(30);
        assertThat(material.path("gapCount").asInt()).isEqualTo(1);
        assertThat(material.path("pendingCount").asInt()).isEqualTo(1);
        assertThat(material.path("previewSupported").asBoolean()).isTrue();
        JsonNode bom = findSegment(segments, "Bom");
        assertThat(bom.path("ruleCount").asInt()).isEqualTo(9);
        assertThat(bom.path("gapCount").asInt()).isZero();
        JsonNode requisition = findSegment(segments, "Requisition");
        assertThat(requisition.path("ruleCount").asInt()).isEqualTo(24);
        assertThat(requisition.path("previewSupported").asBoolean()).isFalse();

        // ---- 2. 物料段明细：旧 PDM 列名保真、唯一的缺口是「金蝶内码」
        JsonNode materialDetail = call(get("/api/v1/itg/erp-mapping/Material"), admin);
        assertThat(materialDetail.path("rules")).hasSize(32);
        JsonNode first = materialDetail.path("rules").get(0);
        assertThat(first.path("erpName").asText()).isEqualTo("FMATERIALID");
        assertThat(first.path("pdmName").asText()).isEqualTo("FMATERIALID");
        assertThat(first.path("target").asText()).isEqualTo("Model.FMATERIALID");
        assertThat(first.path("status").asText()).isEqualTo("GAP");
        assertThat(first.path("note").asText()).contains("ERP 分配");
        // 方案 A：客制列已指向 ext_json 下的元数据字段
        assertThat(findRule(materialDetail.path("rules"), "ZYCS").path("sourcePath").asText())
                .isEqualTo("ext_json.F_ZXCV_ZYCS");
        assertThat(findRule(materialDetail.path("rules"), "YUNXUSHENGCHAN")
                .path("sourcePath").asText()).isEqualTo("ext_json.FIsProduce");

        // ---- 3. 物料试算：嵌套路径 + 客制字段（写进 ext_json 后能带进报文）+ 缺口清单
        Map<String, Object> extValues = new java.util.LinkedHashMap<>();
        extValues.put("F_ZXCV_ZYCS", "380V/50Hz");
        extValues.put("FIsProduce", true);
        extValues.put("FErpClsID", "外购");
        extValues.put("F_zxcv_JSCS", "  ");
        JsonNode item = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "ERP 试算件", "itemType", "PART", "specification", "E-01",
                "material", "40Cr", "unit", "PCS", "ext", extValues)));
        long itemId = item.path("id").asLong();
        String itemCode = item.path("code").asText();
        // 客制字段回读：可写也可读（空白值不落库）
        JsonNode itemDetail = call(get("/api/v1/items/{id}", itemId), admin);
        assertThat(itemDetail.path("ext").path("F_ZXCV_ZYCS").asText()).isEqualTo("380V/50Hz");
        assertThat(itemDetail.path("ext").path("FIsProduce").asBoolean()).isTrue();
        assertThat(itemDetail.path("ext").has("F_zxcv_JSCS")).isFalse();
        // 未登记的字段必须拒绝，避免 ext_json 变成垃圾桶
        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法客制字段件", "itemType", "PART",
                                "ext", Map.of("NOT_A_FIELD", "x")))))
                .andExpect(status().isBadRequest());

        JsonNode preview = call(post("/api/v1/itg/erp-mapping/Material/preview"), admin,
                json(Map.of("bizId", String.valueOf(itemId))));
        assertThat(preview.path("bizKey").asText()).startsWith(itemCode);
        JsonNode model = preview.path("payload").path("Model");
        assertThat(model.path("FNumber").asText()).isEqualTo(itemCode);
        assertThat(model.path("FName").asText()).isEqualTo("ERP 试算件");
        assertThat(model.path("FSpecification").asText()).isEqualTo("E-01");
        // 客制字段：摊平在源报文顶层，映射按 ext_json.F_XXX 取到
        assertThat(model.path("F_ZXCV_ZYCS").asText()).isEqualTo("380V/50Hz");
        assertThat(model.path("SubHeadEntity").path("FIsProduce").asBoolean()).isTrue();
        assertThat(model.path("SubHeadEntity").path("FErpClsID").asText()).isEqualTo("外购");
        // 11 个单位列共用 UNIT，全部指向同一份嵌套对象
        assertThat(model.path("SubHeadEntity").path("FBaseUnitId").path("FNumber").asText())
                .isEqualTo("PCS");
        assertThat(preview.path("entryKey").asText()).isEqualTo("entries");
        // 配置层只剩「金蝶内码」没有来源；运行期还有若干客制字段未填值，属"源数据为空"
        assertThat(preview.path("filledCount").asInt() + preview.path("missingCount").asInt())
                .isEqualTo(32);
        assertThat(findMissing(preview.path("missing"), "Model.FMATERIALID").path("source").asText())
                .isEqualTo("FMATERIALID");
        // 已填的客制字段不应该再出现在缺口清单里
        for (JsonNode missingRow : preview.path("missing")) {
            assertThat(missingRow.path("target").asText())
                    .isNotIn("Model.F_ZXCV_ZYCS", "Model.SubHeadEntity.FIsProduce",
                            "Model.SubHeadEntity.FErpClsID");
        }

        // ---- 4. BOM 试算：分录数组 + 父项单位/修改人/子项类型都从既有表列取
        JsonNode parent = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "ERP 试算总成", "itemType", "PRODUCT", "unit", "EA")));
        JsonNode child = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "ERP 试算子件", "itemType", "PART", "unit", "EA")));
        long parentId = parent.path("id").asLong();
        long childId = child.path("id").asLong();
        jdbcTemplate.update("""
                        INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type,
                                              line_no, qty, unit, loss_rate, remark)
                        VALUES (?, ?, ?, 'DESIGN', 10, 2.5, 'EA', 0, '试算行')
                        """, 8_760_000_000_000_001L, parentId, childId);
        JsonNode bomPreview = call(post("/api/v1/itg/erp-mapping/Bom/preview"), admin,
                json(Map.of("bizId", String.valueOf(parentId), "viewType", "DESIGN")));
        JsonNode bomModel = bomPreview.path("payload").path("Model");
        assertThat(bomModel.path("FMATERIALID").path("FNumber").asText())
                .isEqualTo(parent.path("code").asText());
        assertThat(bomModel.path("FUNITID").path("FNumber").asText()).isEqualTo("EA");
        assertThat(bomModel.path("F_zxcv_cjr").asText()).isEqualTo("admin");
        JsonNode entries = bomPreview.path("payload").path("entries");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).path("FMATERIALIDCHILD").path("FNumber").asText())
                .isEqualTo(child.path("code").asText());
        assertThat(entries.get(0).path("FMATERIALTYPE").asText()).isEqualTo("PART");
        assertThat(entries.get(0).path("FNUMERATOR").asDouble()).isEqualTo(2.5);
        assertThat(entries.get(0).path("FMEMO").asText()).isEqualTo("试算行");
        // 父件没填客制项目代号 → 该列进缺口清单（仍是"取不到值"，不是"没配取值路径"）
        assertThat(findMissing(bomPreview.path("missing"), "Model.F_PJQF_XMDA.FNumber")
                .path("source").asText()).isEqualTo("XMDH");

        // ---- 5. 保存：把最后一个缺口（金蝶内码）也补上路径，缺口归零
        List<Map<String, Object>> rules = new ArrayList<>();
        for (JsonNode rule : materialDetail.path("rules")) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("erpName", rule.path("erpName").asText(null));
            row.put("caption", rule.path("caption").asText(null));
            row.put("pdmName", rule.path("pdmName").asText(null));
            row.put("pdmCaption", rule.path("pdmCaption").asText(null));
            row.put("source", rule.path("source").asText());
            String sourcePath = rule.path("sourcePath").asText(null);
            if ("FMATERIALID".equals(rule.path("pdmName").asText()) && sourcePath == null) {
                sourcePath = "code";
            }
            row.put("sourcePath", sourcePath);
            row.put("target", rule.path("target").asText());
            row.put("transform", rule.path("transform").asText(null));
            row.put("canNull", rule.path("canNull").asBoolean(true));
            row.put("sendDbValue", rule.path("sendDbValue").asBoolean(true));
            row.put("status", rule.path("status").asText(null));
            row.put("note", rule.path("note").asText(null));
            rules.add(row);
        }
        JsonNode saved = call(put("/api/v1/itg/erp-mapping/Material"), admin,
                json(Map.of("rules", rules, "remark", "测试保存")));
        assertThat(saved.path("gapCount").asInt()).isZero();
        assertThat(saved.path("mappedCount").asInt()).isEqualTo(31);
        assertThat(saved.path("remark").asText()).isEqualTo("测试保存");

        // ---- 6. 请购单：本系统无实体，试算明确拒绝（不伪造报文）
        mockMvc.perform(post("/api/v1/itg/erp-mapping/Requisition/preview")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bizId", String.valueOf(itemId)))))
                .andExpect(status().isBadRequest());

        // ---- 7. 非法段名 → 400
        mockMvc.perform(get("/api/v1/itg/erp-mapping/Unknown")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ 工具

    private JsonNode findSegment(JsonNode segments, String objectCode) {
        for (JsonNode segment : segments) {
            if (objectCode.equals(segment.path("objectCode").asText())) {
                return segment;
            }
        }
        throw new IllegalStateException("缺少映射段：" + objectCode);
    }

    private JsonNode findRule(JsonNode rules, String pdmName) {
        for (JsonNode rule : rules) {
            if (pdmName.equals(rule.path("pdmName").asText())) {
                return rule;
            }
        }
        throw new IllegalStateException("映射规则里没有旧列名：" + pdmName);
    }

    private JsonNode findMissing(JsonNode missingRows, String target) {
        for (JsonNode row : missingRows) {
            if (target.equals(row.path("target").asText())) {
                return row;
            }
        }
        throw new IllegalStateException("缺口清单里没有：" + target);
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                          String token) throws Exception {
        return call(builder, token, null);
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                          String token, String payload) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
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
