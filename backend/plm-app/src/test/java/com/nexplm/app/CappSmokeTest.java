package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P6-1 工艺路线冒烟：主档 + 工序批量编辑 + 路线串 + 发布唯一性。
 *
 * <p>规则：只有草稿可改/可删；发布要求至少一道工序；发布时同零件其它已发布路线自动归档；
 * 工序号不可重复、工时不可为负。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CappSmokeTest {

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
    void routingLifecycleWithBatchSteps() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "工艺零件");

        JsonNode created = call(post("/api/v1/capp/routings"), admin,
                json(routing("车削工艺", itemId, steps(
                        step(10, "下料", "MACHINING", "下料车间", 0, 5),
                        step(20, "车削", "MACHINING", "机加车间", 30, 25),
                        step(30, "检验", "INSPECTION", "检验室", 0, 3)))));
        long routingId = created.path("id").asLong();
        assertThat(created.path("code").asText()).startsWith("RT-");
        assertThat(created.path("status").asText()).isEqualTo("DRAFT");
        assertThat(created.path("versionNo").asInt()).isEqualTo(1);
        assertThat(created.path("routeString").asText()).isEqualTo("10 下料 → 20 车削 → 30 检验");
        assertThat(created.path("totalMinutes").decimalValue()).isEqualByComparingTo("63");
        assertThat(created.path("steps").size()).isEqualTo(3);
        assertThat(created.path("objectCode").asText()).isNotBlank();

        // 批量编辑：整表替换为 2 道工序
        JsonNode updated = call(put("/api/v1/capp/routings/{id}", routingId), admin,
                json(routing("车削工艺V2", itemId, steps(
                        step(10, "下料", "MACHINING", "下料车间", 0, 5),
                        step(20, "精车", "MACHINING", "机加车间", 20, 15)))));
        assertThat(updated.path("name").asText()).isEqualTo("车削工艺V2");
        assertThat(updated.path("steps").size()).isEqualTo(2);
        assertThat(updated.path("routeString").asText()).isEqualTo("10 下料 → 20 精车");
        assertThat(updated.path("totalMinutes").decimalValue()).isEqualByComparingTo("40");

        // 工序号重复 → 400
        mockMvc.perform(put("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("重复工序号", itemId, steps(
                                step(10, "下料", "MACHINING", null, 0, 5),
                                step(10, "车削", "MACHINING", null, 0, 5))))))
                .andExpect(status().isBadRequest());

        // 未知工序类型 → 400
        mockMvc.perform(put("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("未知工序类型", itemId, steps(
                                step(10, "下料", "FLYING", null, 0, 5))))))
                .andExpect(status().isBadRequest());

        // 发布 → 已发布；再改 409
        JsonNode released = call(post("/api/v1/capp/routings/{id}/release", routingId), admin, null);
        assertThat(released.path("status").asText()).isEqualTo("RELEASED");
        assertThat(released.path("releasedAt").isNull()).isFalse();
        mockMvc.perform(put("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("已发布不许改", itemId, steps(
                                step(10, "下料", "MACHINING", null, 0, 5))))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/capp/routings/{id}", routingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 列表按零件过滤
        JsonNode list = call(get("/api/v1/capp/routings?objectId=" + itemId), admin, null);
        assertThat(list.path("total").asLong()).isEqualTo(1);
        JsonNode row = list.path("items").get(0);
        assertThat(row.path("stepCount").asInt()).isEqualTo(2);
        assertThat(row.path("totalMinutes").decimalValue()).isEqualByComparingTo("40");
        assertThat(row.path("status").asText()).isEqualTo("RELEASED");
    }

    @Test
    void releaseArchivesPreviousAndCopyKeepsSteps() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "双工艺零件");

        long firstId = call(post("/api/v1/capp/routings"), admin,
                json(routing("旧工艺", itemId, steps(step(10, "下料", "MACHINING", null, 0, 5)))))
                .path("id").asLong();
        call(post("/api/v1/capp/routings/{id}/release", firstId), admin, null);

        long secondId = call(post("/api/v1/capp/routings"), admin,
                json(routing("新工艺", itemId, steps(
                        step(10, "下料", "MACHINING", null, 0, 5),
                        step(20, "铣削", "MACHINING", null, 10, 20)))))
                .path("id").asLong();
        call(post("/api/v1/capp/routings/{id}/release", secondId), admin, null);

        // 同一零件只允许一条已发布路线：旧的自动归档
        assertThat(call(get("/api/v1/capp/routings/{id}", firstId), admin, null)
                .path("status").asText()).isEqualTo("ARCHIVED");
        assertThat(call(get("/api/v1/capp/routings/{id}", secondId), admin, null)
                .path("status").asText()).isEqualTo("RELEASED");

        // 复制为草稿：工序照搬、版本号 +1
        JsonNode copy = call(post("/api/v1/capp/routings/{id}/copy", secondId), admin, null);
        assertThat(copy.path("status").asText()).isEqualTo("DRAFT");
        assertThat(copy.path("steps").size()).isEqualTo(2);
        assertThat(copy.path("versionNo").asInt()).isEqualTo(2);
        assertThat(copy.path("id").asLong()).isNotEqualTo(secondId);

        // 草稿可删；删除后列表只剩 2 条
        call(delete("/api/v1/capp/routings/{id}", copy.path("id").asLong()), admin, null);
        assertThat(call(get("/api/v1/capp/routings?objectId=" + itemId), admin, null)
                .path("total").asLong()).isEqualTo(2);
    }

    /** P6-2：工艺分工（负责人）+ 工序号乱序后一键重建。 */
    @Test
    void resequenceAndCraftOwner() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "分工零件");

        Map<String, Object> body = routing("乱序工艺", itemId, steps(
                step(5, "下料", "MACHINING", null, 0, 2),
                step(7, "车削", "MACHINING", null, 0, 3),
                step(9, "检验", "INSPECTION", null, 0, 1)));
        body.put("ownerId", adminId);
        JsonNode created = call(post("/api/v1/capp/routings"), admin, json(body));
        long routingId = created.path("id").asLong();
        assertThat(created.path("ownerId").asLong()).isEqualTo(adminId);
        assertThat(created.path("ownerName").asText()).isNotBlank();

        JsonNode resequenced = call(post("/api/v1/capp/routings/{id}/resequence", routingId), admin, null);
        assertThat(resequenced.path("steps").size()).isEqualTo(3);
        assertThat(resequenced.path("steps").get(0).path("seqNo").asInt()).isEqualTo(10);
        assertThat(resequenced.path("steps").get(1).path("seqNo").asInt()).isEqualTo(20);
        assertThat(resequenced.path("steps").get(2).path("seqNo").asInt()).isEqualTo(30);
        // 顺序与工序名保持不变
        assertThat(resequenced.path("routeString").asText()).isEqualTo("10 下料 → 20 车削 → 30 检验");

        // 已发布的路线不允许重建序号
        call(post("/api/v1/capp/routings/{id}/release", routingId), admin, null);
        mockMvc.perform(post("/api/v1/capp/routings/{id}/resequence", routingId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsUnknownObjectAndEmptySteps() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "校验零件");

        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("零件不存在", 999999999L,
                                steps(step(10, "下料", "MACHINING", null, 0, 1))))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("没有工序", itemId, List.of()))))
                .andExpect(status().isBadRequest());
    }

    /** P6-3：分类树 + 动态属性筛选 + 保存查询条件。 */
    @Test
    void resourceLibraryWithDynamicAttrsAndSavedQuery() throws Exception {
        String admin = login("admin", "Admin@12345");

        long toolClass = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TOOL", "name", "刀具"))).path("id").asLong();
        long turningClass = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TOOL-TURN", "name", "车刀", "parentId", toolClass))).path("id").asLong();

        JsonNode created = call(post("/api/v1/capp/resources"), admin, json(Map.of(
                "classId", turningClass,
                "code", "R-001",
                "name", "外圆车刀",
                "spec", "25×25",
                "unit", "把",
                "attrs", Map.of("品牌", "山特", "直径", "20"))));
        long resourceId = created.path("id").asLong();
        assertThat(created.path("className").asText()).isEqualTo("车刀");
        assertThat(created.path("attrs").path("品牌").asText()).isEqualTo("山特");

        // 按分类筛选
        assertThat(call(get("/api/v1/capp/resources?classId=" + turningClass), admin, null)
                .path("total").asLong()).isEqualTo(1);
        // 动态属性包含匹配（用 param 传原始 JSON；手写 %XX 在 MockMvc 里不会二次解码）
        assertThat(call(get("/api/v1/capp/resources").param("attrs", "{\"品牌\":\"山特\"}"), admin, null)
                .path("total").asLong()).isEqualTo(1);
        assertThat(call(get("/api/v1/capp/resources").param("attrs", "{\"品牌\":\"他人\"}"), admin, null)
                .path("total").asLong()).isZero();

        // 保存查询条件并按条件执行
        JsonNode saved = call(post("/api/v1/capp/resource-queries"), admin, json(Map.of(
                "name", "山特车刀", "classId", turningClass, "attrs", Map.of("品牌", "山特"))));
        assertThat(call(get("/api/v1/capp/resource-queries/" + saved.path("id").asLong() + "/run"), admin, null)
                .path("total").asLong()).isEqualTo(1);
        // 同名再保存 → 覆盖，不新增
        call(post("/api/v1/capp/resource-queries"), admin, json(Map.of("name", "山特车刀", "classId", turningClass)));
        assertThat(call(get("/api/v1/capp/resource-queries"), admin, null).size()).isEqualTo(1);

        // 分类下有资源时不允许删
        mockMvc.perform(delete("/api/v1/capp/resource-classes/{id}", turningClass)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 资源无引用可删，删完分类才能删
        call(delete("/api/v1/capp/resources/{id}", resourceId), admin, null);
        call(delete("/api/v1/capp/resource-classes/{id}", turningClass), admin, null);
        assertThat(call(get("/api/v1/capp/resources?classId=" + turningClass), admin, null)
                .path("total").asLong()).isZero();
    }

    /** P6-3：工序引用资源后禁止删除资源，引用统计可回溯到路线与工序。 */
    @Test
    void resourceReferenceBlocksDeletion() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "引用资源零件");
        long resourceClass = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TOOL-REF", "name", "引用刀具"))).path("id").asLong();
        long resourceId = call(post("/api/v1/capp/resources"), admin, json(Map.of(
                "classId", resourceClass, "code", "R-REF-01", "name", "切断刀", "unit", "把")))
                .path("id").asLong();

        Map<String, Object> step = step(10, "切断", "MACHINING", "下料车间", 0, 2);
        step.put("resourceId", resourceId);
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("引用资源工艺", itemId, steps(step)))).path("id").asLong();

        JsonNode detail = call(get("/api/v1/capp/routings/{id}", routingId), admin, null);
        assertThat(detail.path("steps").get(0).path("resourceId").asLong()).isEqualTo(resourceId);
        assertThat(detail.path("steps").get(0).path("resourceCode").asText()).isEqualTo("R-REF-01");

        JsonNode references = call(get("/api/v1/capp/resources/" + resourceId + "/references"), admin, null);
        assertThat(references.size()).isEqualTo(1);
        assertThat(references.get(0).path("routingId").asLong()).isEqualTo(routingId);
        assertThat(references.get(0).path("seqNo").asInt()).isEqualTo(10);
        assertThat(call(get("/api/v1/capp/resources/" + resourceId), admin, null)
                .path("referenceCount").asInt()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/capp/resources/{id}", resourceId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 删掉引用它的草稿路线后可删除
        call(delete("/api/v1/capp/routings/{id}", routingId), admin, null);
        call(delete("/api/v1/capp/resources/{id}", resourceId), admin, null);
    }

    /**
     * P6-4 工艺卡片：模板版本化 + 卡片快照 + 导出 + 重新生成。
     *
     * <p>覆盖的关键规则：草稿模板不能生成卡片、未知变量拒绝、发布归档同族旧版本、
     * 卡片落地双快照（模板改版后历史卡片版式不变）、导出 xlsx 可被 POI 读回、重新生成刷新行。</p>
     */
    @Test
    void cardTemplateVersioningAndExport() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "卡片零件");
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("卡片路线", itemId, steps(
                        step(10, "粗车", "MACHINING", "机加车间", 0.5, 1.5),
                        step(20, "精车", "MACHINING", "机加车间", 0.2, 2.0))))).path("id").asLong();

        Map<String, Object> layout = new HashMap<>();
        layout.put("title", "机械加工工序卡");
        layout.put("headerFields", List.of(
                Map.of("label", "零件编码", "field", "objectCode"),
                Map.of("label", "规格型号", "field", "objectSpec")));
        layout.put("columns", List.of(
                column("seqNo", "工序号", "CENTER", 10),
                column("name", "工序名称", "LEFT", 18),
                column("setupMinutes", "准备工时", "RIGHT", 10),
                column("runMinutes", "单件工时", "RIGHT", 10),
                column("totalMinutes", "工时合计", "RIGHT", 10)));
        layout.put("footerText", "编制：____ 审核：____");

        Map<String, Object> req = new HashMap<>();
        req.put("name", "工序卡模板");
        req.put("cardType", "CRAFT");
        req.put("layout", layout);
        JsonNode template = call(post("/api/v1/capp/card-templates"), admin, json(req));
        long templateId = template.path("id").asLong();
        assertThat(template.path("code").asText()).startsWith("CT-");
        assertThat(template.path("status").asText()).isEqualTo("DRAFT");
        assertThat(template.path("versionNo").asInt()).isEqualTo(1);
        assertThat(template.path("layout").path("columns")).hasSize(5);

        // 草稿模板不能生成卡片
        mockMvc.perform(post("/api/v1/capp/cards")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("templateId", templateId, "routingId", routingId))))
                .andExpect(status().isBadRequest());

        // 未知明细变量被拒绝
        Map<String, Object> badLayout = new HashMap<>(layout);
        badLayout.put("columns", List.of(column("notExist", "未知列", "LEFT", 10)));
        Map<String, Object> badReq = new HashMap<>();
        badReq.put("name", "非法模板");
        badReq.put("layout", badLayout);
        mockMvc.perform(post("/api/v1/capp/card-templates")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badReq)))
                .andExpect(status().isBadRequest());

        JsonNode released = call(post("/api/v1/capp/card-templates/{id}/release", templateId),
                admin, null);
        assertThat(released.path("status").asText()).isEqualTo("RELEASED");

        JsonNode card = call(post("/api/v1/capp/cards"), admin, json(Map.of(
                "templateId", templateId,
                "routingId", routingId,
                "fieldValues", Map.of("objectSpec", "Φ50×200"))));
        long cardId = card.path("id").asLong();
        assertThat(card.path("code").asText()).startsWith("CC-");
        assertThat(card.path("templateVersion").asInt()).isEqualTo(1);
        assertThat(card.path("fieldValues").path("objectCode").asText()).isNotBlank();
        assertThat(card.path("fieldValues").path("objectSpec").asText()).isEqualTo("Φ50×200");
        assertThat(card.path("fieldValues").path("routeString").asText())
                .contains("10 粗车").contains("20 精车");
        // 2 道工序 + 1 行合计
        assertThat(card.path("rows")).hasSize(3);
        JsonNode summary = card.path("rows").get(2);
        assertThat(summary.path("__summary__").asText()).isEqualTo("true");
        assertThat(summary.path("setupMinutes").asText()).isEqualTo("0.70");
        assertThat(summary.path("runMinutes").asText()).isEqualTo("3.50");
        assertThat(summary.path("totalMinutes").asText()).isEqualTo("4.20");
        assertThat(call(get("/api/v1/capp/cards"), admin, null).path("items").get(0)
                .path("rowCount").asInt()).isEqualTo(2);

        // 导出 xlsx：标题 → 表头字段 → 明细表头 → 工序行 → 合计行 → 表尾
        MvcResult exported = mockMvc.perform(get("/api/v1/capp/cards/{id}/export", cardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        byte[] content = exported.getResponse().getContentAsByteArray();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("机械加工工序卡");
            int headRow = findRow(sheet, "工序号");
            assertThat(headRow).isGreaterThan(0);
            assertThat(sheet.getRow(headRow).getCell(1).getStringCellValue()).isEqualTo("工序名称");
            assertThat(sheet.getRow(headRow + 1).getCell(1).getStringCellValue()).isEqualTo("粗车");
            assertThat(sheet.getRow(headRow + 3).getCell(0).getStringCellValue()).isEqualTo("合计");
            assertThat(sheet.getRow(headRow + 3).getCell(4).getStringCellValue()).isEqualTo("4.20");
        }

        // PDF 导出：同一份快照渲染；环境无中文字体时跳过（服务会返回 400 明确提示配置字体路径）
        MvcResult pdfResult = mockMvc.perform(get("/api/v1/capp/cards/{id}/export.pdf", cardId)
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        if (pdfResult.getResponse().getStatus() == 200) {
            byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
            assertThat(pdfBytes.length).isGreaterThan(2000);
            assertThat(new String(pdfBytes, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        } else {
            String message = pdfResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assumeTrue(!message.contains("中文字体"), "PDF 导出失败（非字体原因）：" + message);
        }

        // 改版：另存 v2 并发布 → v1 归档；历史卡片仍是 v1 版式（5 列）
        JsonNode v2 = call(post("/api/v1/capp/card-templates/{id}/new-version", templateId),
                admin, null);
        assertThat(v2.path("versionNo").asInt()).isEqualTo(2);
        assertThat(v2.path("code").asText()).isEqualTo(template.path("code").asText());
        call(post("/api/v1/capp/card-templates/{id}/release", v2.path("id").asLong()), admin, null);
        assertThat(call(get("/api/v1/capp/card-templates/{id}", templateId), admin, null)
                .path("status").asText()).isEqualTo("ARCHIVED");
        JsonNode oldCard = call(get("/api/v1/capp/cards/{id}", cardId), admin, null);
        assertThat(oldCard.path("templateVersion").asInt()).isEqualTo(1);
        assertThat(oldCard.path("layout").path("columns")).hasSize(5);
        assertThat(oldCard.path("rows").get(0).path("totalMinutes").asText()).isEqualTo("2.00");

        // 重新生成：路线工序改动后刷新行，用户手填的表头值保留
        call(put("/api/v1/capp/routings/{id}", routingId), admin,
                json(routing("卡片路线", itemId,
                        steps(step(10, "粗车", "MACHINING", "机加车间", 0.5, 1.5)))));
        JsonNode refreshed = call(post("/api/v1/capp/cards/{id}/regenerate", cardId), admin, null);
        assertThat(refreshed.path("rows")).hasSize(2);
        assertThat(refreshed.path("fieldValues").path("objectSpec").asText()).isEqualTo("Φ50×200");

        call(delete("/api/v1/capp/cards/{id}", cardId), admin, null);
        call(delete("/api/v1/capp/routings/{id}", routingId), admin, null);
    }

    /**
     * P6-6 工时定额：规则匹配优先级 + 单件/批量口径 + 规则改动后重算。
     *
     * <p>覆盖：工作中心+类型 &gt; 类型 &gt; 全局默认的命中顺序；准备工时基准与系数、单件系数；
     * 单件工时合计不含准备工时；批量工时 = 准备合计 + 单件 × 批量；改系数后重算生效；
     * 无规则时按原值且标注 unmatched。</p>
     */
    @Test
    void timeRuleMatchingAndCalc() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "定额零件");
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("定额路线", itemId, steps(
                        step(10, "粗车", "MACHINING", "机加车间", 0.5, 1.5),
                        step(20, "终检", "INSPECTION", "检验站", 0.1, 0.2))))).path("id").asLong();

        long globalRule = call(post("/api/v1/capp/time-rules"), admin, json(timeRule(
                "全局默认", null, null, "0.1", "1", "1", 100))).path("id").asLong();
        long typeRule = call(post("/api/v1/capp/time-rules"), admin, json(timeRule(
                "机加类型", "MACHINING", null, "0.2", "2", "1.5", 50))).path("id").asLong();
        long centerRule = call(post("/api/v1/capp/time-rules"), admin, json(timeRule(
                "机加车间", "MACHINING", "机加车间", "0.3", "1", "2", 100))).path("id").asLong();
        assertThat(call(get("/api/v1/capp/time-rules"), admin, null)).hasSize(3);

        // 工序1 命中「机加车间」（得分 3）：准备 0.3 + 0.5×1.0 = 0.80，单件 1.5×2.0 = 3.00
        // 工序2 命中「全局默认」（得分 0）：准备 0.1 + 0.1×1.0 = 0.20，单件 0.2×1.0 = 0.20
        JsonNode calc = call(get("/api/v1/capp/time-calc?routingId=" + routingId + "&batchQty=10"),
                admin, null);
        assertThat(calc.path("matchedSteps").asInt()).isEqualTo(2);
        assertThat(calc.path("unmatchedSteps").asInt()).isZero();
        assertThat(calc.path("steps").get(0).path("ruleCode").asText()).startsWith("TR-");
        assertThat(calc.path("steps").get(0).path("setupEffective").asDouble()).isCloseTo(0.80, within(0.001));
        assertThat(calc.path("steps").get(0).path("runEffective").asDouble()).isCloseTo(3.00, within(0.001));
        assertThat(calc.path("steps").get(1).path("setupEffective").asDouble()).isCloseTo(0.20, within(0.001));
        assertThat(calc.path("totalSetupEffective").asDouble()).isCloseTo(1.00, within(0.001));
        assertThat(calc.path("singlePieceMinutes").asDouble()).isCloseTo(3.20, within(0.001));
        // 批量 10：1.00 + 3.20×10
        assertThat(calc.path("batchMinutes").asDouble()).isCloseTo(33.00, within(0.001));

        // 改系数后重算：机加车间 runFactor 2.0 → 1.0，单件变 1.5+0.2=1.70，批量 = 1.00 + 17.00
        call(put("/api/v1/capp/time-rules/{id}", centerRule), admin,
                json(timeRule("机加车间", "MACHINING", "机加车间", "0.3", "1", "1", 100)));
        JsonNode recalc = call(get("/api/v1/capp/time-calc?routingId=" + routingId + "&batchQty=10"),
                admin, null);
        assertThat(recalc.path("steps").get(0).path("runEffective").asDouble()).isCloseTo(1.50, within(0.001));
        assertThat(recalc.path("singlePieceMinutes").asDouble()).isCloseTo(1.70, within(0.001));
        assertThat(recalc.path("batchMinutes").asDouble()).isCloseTo(18.00, within(0.001));

        // 系数非法（<= 0）被拒绝
        Map<String, Object> badRule = timeRule("非法系数", null, null, "0", "0", "1", 100);
        mockMvc.perform(post("/api/v1/capp/time-rules")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badRule)))
                .andExpect(status().isBadRequest());

        // 删光规则 → 全部按原值并标注 unmatched
        call(delete("/api/v1/capp/time-rules/{id}", globalRule), admin, null);
        call(delete("/api/v1/capp/time-rules/{id}", typeRule), admin, null);
        call(delete("/api/v1/capp/time-rules/{id}", centerRule), admin, null);
        JsonNode plain = call(get("/api/v1/capp/time-calc?routingId=" + routingId + "&batchQty=10"),
                admin, null);
        assertThat(plain.path("matchedSteps").asInt()).isZero();
        assertThat(plain.path("unmatchedSteps").asInt()).isEqualTo(2);
        assertThat(plain.path("totalSetupEffective").asDouble()).isCloseTo(0.60, within(0.001));
        assertThat(plain.path("singlePieceMinutes").asDouble()).isCloseTo(1.70, within(0.001));
        assertThat(plain.path("batchMinutes").asDouble()).isCloseTo(17.60, within(0.001));

        call(delete("/api/v1/capp/routings/{id}", routingId), admin, null);
    }

    /**
     * P6-2（补齐）工步与工艺过程树。
     *
     * <p>覆盖：工步随工序整表替换提交、工步级资源回填、过程树同源聚合、序号重建时工步跟随迁移
     * （工步绑工序号而非工序行 id，重排后不能变孤儿）、复制路线带工步、同工序工步号重复被拒。</p>
     */
    @Test
    void substepsAndProcessTree() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "工步零件");
        long classId = call(post("/api/v1/capp/resource-classes"), admin,
                json(Map.of("code", "TMP-SUB-CLS", "name", "工步夹具"))).path("id").asLong();
        long resourceId = call(post("/api/v1/capp/resources"), admin, json(Map.of(
                "classId", classId, "code", "TMP-SUB-R1", "name", "三爪卡盘", "unit", "套")))
                .path("id").asLong();

        Map<String, Object> step10 = step(5, "粗车", "MACHINING", "机加车间", 0.3, 1.5);
        step10.put("substeps", List.of(
                substep(1, "装夹", "三爪卡盘装夹找正", resourceId, "0.2", "0.0"),
                substep(2, "粗走刀", "车外圆至 Φ52", null, "0.1", "1.2")));
        Map<String, Object> step20 = step(7, "终检", "INSPECTION", "检验站", 0.1, 0.2);
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("工步路线", itemId, List.of(step10, step20)))).path("id").asLong();

        JsonNode detail = call(get("/api/v1/capp/routings/{id}", routingId), admin, null);
        assertThat(detail.path("steps")).hasSize(2);
        JsonNode substeps = detail.path("steps").get(0).path("substeps");
        assertThat(substeps).hasSize(2);
        assertThat(substeps.get(0).path("name").asText()).isEqualTo("装夹");
        assertThat(substeps.get(0).path("resourceCode").asText()).isEqualTo("TMP-SUB-R1");
        assertThat(substeps.get(0).path("resourceName").asText()).isEqualTo("三爪卡盘");
        assertThat(detail.path("steps").get(1).path("substeps")).hasSize(0);

        // 过程树：与工序表同源，聚合工步数与工步工时之和（0.2+0.0 + 0.1+1.2 = 1.5）
        JsonNode tree = call(get("/api/v1/capp/routings/{id}/process-tree", routingId), admin, null);
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).path("stepSeqNo").asInt()).isEqualTo(5);
        assertThat(tree.get(0).path("substepCount").asInt()).isEqualTo(2);
        assertThat(tree.get(0).path("substepMinutes").asDouble()).isCloseTo(1.5, within(0.001));
        assertThat(tree.get(1).path("substepCount").asInt()).isZero();

        // 同工序内工步号重复 → 400
        Map<String, Object> dupStep = step(5, "粗车", "MACHINING", "机加车间", 0.3, 1.5);
        dupStep.put("substeps", List.of(
                substep(1, "装夹", null, null, "0", "0"),
                substep(1, "重复号", null, null, "0", "0")));
        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(routing("重复工步路线", itemId, List.of(dupStep)))))
                .andExpect(status().isBadRequest());

        // 序号重建：5/7 → 10/20，工步必须跟着工序号迁移
        JsonNode reseq = call(post("/api/v1/capp/routings/{id}/resequence", routingId), admin, null);
        JsonNode stepsAfter = reseq.path("steps");
        assertThat(stepsAfter.get(0).path("seqNo").asInt()).isEqualTo(10);
        assertThat(stepsAfter.get(0).path("name").asText()).isEqualTo("粗车");
        assertThat(stepsAfter.get(0).path("substeps")).hasSize(2);
        assertThat(stepsAfter.get(0).path("substeps").get(1).path("name").asText()).isEqualTo("粗走刀");
        assertThat(stepsAfter.get(1).path("seqNo").asInt()).isEqualTo(20);
        assertThat(stepsAfter.get(1).path("substeps")).hasSize(0);

        // 复制为新草稿也带工步
        JsonNode copied = call(post("/api/v1/capp/routings/{id}/copy", routingId), admin, null);
        assertThat(copied.path("steps").get(0).path("substeps")).hasSize(2);
        assertThat(copied.path("steps").get(0).path("substeps").get(0).path("resourceCode").asText())
                .isEqualTo("TMP-SUB-R1");

        call(delete("/api/v1/capp/routings/{id}", copied.path("id").asLong()), admin, null);
        call(delete("/api/v1/capp/routings/{id}", routingId), admin, null);
        call(delete("/api/v1/capp/resources/{id}", resourceId), admin, null);
        call(delete("/api/v1/capp/resource-classes/{id}", classId), admin, null);
    }

    /**
     * P6-5 工艺成册与齐套性检查。
     *
     * <p>覆盖：齐套检查的 PASS/WARN/FAIL 口径（无路线=FAIL）、成册页序（10/20）、跨零件卡片被拒、
     * 空册不能发布、成册 PDF 导出（封页+目录+卡片分页）。</p>
     */
    @Test
    void volumeAndCompleteness() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "成册零件");
        long otherItemId = createItem(admin, "别的零件");

        // 没有路线 → FAIL，ready=false
        JsonNode empty = call(get("/api/v1/capp/completeness?objectId=" + itemId), admin, null);
        assertThat(empty.path("ready").asBoolean()).isFalse();
        assertThat(empty.path("failCount").asInt()).isEqualTo(2);
        assertThat(empty.path("checks").findValuesAsText("key")).contains("ROUTE_EXISTS", "STEP_EXISTS");

        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("成册路线", itemId, steps(
                        step(10, "粗车", "MACHINING", "机加车间", 0.5, 1.5),
                        step(20, "终检", "INSPECTION", "检验站", 0.2, 0.3))))).path("id").asLong();

        JsonNode draft = call(get("/api/v1/capp/completeness?objectId=" + itemId), admin, null);
        assertThat(draft.path("ready").asBoolean()).isTrue();
        assertThat(draft.path("routeCount").asInt()).isEqualTo(1);
        assertThat(draft.path("releasedRouteCount").asInt()).isZero();
        assertThat(draft.path("stepCount").asInt()).isEqualTo(2);
        assertThat(draft.path("warnCount").asInt()).isGreaterThan(0);
        assertThat(draft.path("checks").findValuesAsText("key")).contains("SUBSTEP_EXISTS", "CARD_EXISTS");

        call(post("/api/v1/capp/routings/{id}/release", routingId), admin, null);

        // 已发布模板 + 两张卡片
        Map<String, Object> layout = new HashMap<>();
        layout.put("title", "成册工序卡");
        layout.put("columns", List.of(
                column("seqNo", "工序号", "CENTER", 10),
                column("name", "工序名称", "LEFT", 18)));
        Map<String, Object> templateReq = new HashMap<>();
        templateReq.put("name", "成册模板");
        templateReq.put("layout", layout);
        long templateId = call(post("/api/v1/capp/card-templates"), admin, json(templateReq))
                .path("id").asLong();
        call(post("/api/v1/capp/card-templates/{id}/release", templateId), admin, null);
        long card1 = call(post("/api/v1/capp/cards"), admin,
                json(Map.of("templateId", templateId, "routingId", routingId))).path("id").asLong();
        long card2 = call(post("/api/v1/capp/cards"), admin,
                json(Map.of("templateId", templateId, "routingId", routingId))).path("id").asLong();

        JsonNode filled = call(get("/api/v1/capp/completeness?objectId=" + itemId), admin, null);
        assertThat(filled.path("cardCount").asInt()).isEqualTo(2);
        assertThat(filled.path("releasedRouteCount").asInt()).isEqualTo(1);

        // 空册不能发布
        Map<String, Object> emptyVolume = new HashMap<>();
        emptyVolume.put("name", "空册");
        emptyVolume.put("objectId", itemId);
        emptyVolume.put("cardIds", List.of());
        long emptyVolumeId = call(post("/api/v1/capp/volumes"), admin, json(emptyVolume))
                .path("id").asLong();
        mockMvc.perform(post("/api/v1/capp/volumes/" + emptyVolumeId + "/release")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        call(delete("/api/v1/capp/volumes/{id}", emptyVolumeId), admin, null);

        // 正常成册：两张卡按传入顺序得到页序 10 / 20
        Map<String, Object> volumeReq = new HashMap<>();
        volumeReq.put("name", "机加工工艺卡片册");
        volumeReq.put("objectId", itemId);
        volumeReq.put("cardIds", List.of(card1, card2));
        JsonNode volume = call(post("/api/v1/capp/volumes"), admin, json(volumeReq));
        long volumeId = volume.path("id").asLong();
        assertThat(volume.path("code").asText()).startsWith("CV-");
        assertThat(volume.path("cards")).hasSize(2);
        assertThat(volume.path("cards").get(0).path("seqNo").asInt()).isEqualTo(10);
        assertThat(volume.path("cards").get(1).path("seqNo").asInt()).isEqualTo(20);
        assertThat(volume.path("cards").get(0).path("rowCount").asInt()).isEqualTo(2);
        assertThat(volume.path("cards").get(0).path("cardCode").asText()).startsWith("CC-");

        // 跨零件卡片被拒
        // 另建一个零件的卡片：先给 otherItem 建路线（否则没有卡片可生成）
        long otherRouting = call(post("/api/v1/capp/routings"), admin,
                json(routing("别零件路线", otherItemId,
                        steps(step(10, "装配", "ASSEMBLY", "装配线", 0.1, 0.4))))).path("id").asLong();
        long otherCard = call(post("/api/v1/capp/cards"), admin,
                json(Map.of("templateId", templateId, "routingId", otherRouting))).path("id").asLong();
        Map<String, Object> badVolume = new HashMap<>();
        badVolume.put("name", "混装册");
        badVolume.put("objectId", itemId);
        badVolume.put("cardIds", List.of(card1, otherCard));
        mockMvc.perform(post("/api/v1/capp/volumes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badVolume)))
                .andExpect(status().isBadRequest());

        // 成册 PDF：封页 + 目录 + 2 张卡
        MvcResult pdf = mockMvc.perform(get("/api/v1/capp/volumes/{id}/export.pdf", volumeId)
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        if (pdf.getResponse().getStatus() == 200) {
            byte[] bytes = pdf.getResponse().getContentAsByteArray();
            assertThat(bytes.length).isGreaterThan(3000);
            assertThat(new String(bytes, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        } else {
            String message = pdf.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assumeTrue(!message.contains("中文字体"), "成册 PDF 导出失败（非字体原因）：" + message);
        }

        call(delete("/api/v1/capp/volumes/{id}", volumeId), admin, null);
        call(delete("/api/v1/capp/cards/{id}", card1), admin, null);
        call(delete("/api/v1/capp/cards/{id}", card2), admin, null);
        call(delete("/api/v1/capp/cards/{id}", otherCard), admin, null);
        call(delete("/api/v1/capp/routings/{id}", otherRouting), admin, null);
        // 已发布路线按业务规则不可删除（只有草稿可删），内嵌库随测试销毁，无需清理
    }

    /**
     * P6-8 质量规划 + P6-9 下发预留。
     *
     * <p>覆盖：控制计划骨架自动生成、未填特性禁止发布、检验项被引用禁止删除、控制计划 Excel 导出、
     * 草稿路线禁止下发、下发载荷结构（operations/totals）、原始 JSON 取数、回执回写。</p>
     */
    @Test
    void qualityPlanAndErpDispatch() throws Exception {
        String admin = login("admin", "Admin@12345");
        long itemId = createItem(admin, "质量零件");
        long routingId = call(post("/api/v1/capp/routings"), admin,
                json(routing("质量路线", itemId, steps(
                        step(10, "粗车", "MACHINING", "机加车间", 0.5, 1.5),
                        step(20, "精车", "MACHINING", "机加车间", 0.2, 1.0))))).path("id").asLong();

        // 草稿路线不能下发
        mockMvc.perform(post("/api/v1/capp/erp-dispatches")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("routingId", routingId))))
                .andExpect(status().isBadRequest());

        // 检验项字典
        Map<String, Object> itemReq = new HashMap<>();
        itemReq.put("name", "外圆直径");
        itemReq.put("category", "DIMENSION");
        itemReq.put("spec", "Φ50 h7");
        itemReq.put("method", "外径千分尺");
        itemReq.put("gauge", "0-25mm 千分尺");
        JsonNode qualityItem = call(post("/api/v1/capp/quality-items"), admin, json(itemReq));
        long itemDictId = qualityItem.path("id").asLong();
        assertThat(qualityItem.path("code").asText()).startsWith("QI-");

        // 空 rows → 按路线工序生成骨架（2 行）
        Map<String, Object> planReq = new HashMap<>();
        planReq.put("name", "质量路线控制计划");
        planReq.put("routingId", routingId);
        JsonNode plan = call(post("/api/v1/capp/quality-plans"), admin, json(planReq));
        long planId = plan.path("id").asLong();
        assertThat(plan.path("code").asText()).startsWith("QP-");
        assertThat(plan.path("rows")).hasSize(2);
        assertThat(plan.path("rows").get(0).path("stepSeqNo").asInt()).isEqualTo(10);
        assertThat(plan.path("rows").get(0).path("stepName").asText()).isEqualTo("粗车");

        // 未填特性不能发布
        mockMvc.perform(post("/api/v1/capp/quality-plans/" + planId + "/release")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 填第一行特性（引用字典），第二行留空也允许保存，但发布仍被拒
        Map<String, Object> row1 = new HashMap<>();
        row1.put("seqNo", 10);
        row1.put("stepSeqNo", 10);
        row1.put("stepName", "粗车");
        row1.put("itemId", itemDictId);
        row1.put("characteristic", "外圆直径");
        row1.put("spec", "Φ50 h7");
        row1.put("method", "外径千分尺");
        row1.put("gauge", "0-25mm 千分尺");
        row1.put("sampleSize", "5");
        row1.put("frequency", "每批");
        row1.put("controlMethod", "首检+巡检");
        row1.put("reactionPlan", "隔离并通知工艺");
        row1.put("critical", true);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("seqNo", 20);
        row2.put("stepSeqNo", 20);
        row2.put("stepName", "精车");
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("name", "质量路线控制计划");
        updateReq.put("routingId", routingId);
        updateReq.put("rows", List.of(row1, row2));
        JsonNode updated = call(put("/api/v1/capp/quality-plans/{id}", planId), admin, json(updateReq));
        assertThat(updated.path("rows").get(0).path("itemCode").asText()).isEqualTo(qualityItem.path("code").asText());
        assertThat(updated.path("rows").get(0).path("critical").asBoolean()).isTrue();
        mockMvc.perform(post("/api/v1/capp/quality-plans/" + planId + "/release")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 补齐第二行后可发布
        row2.put("characteristic", "精车外圆直径");
        updateReq.put("rows", List.of(row1, row2));
        call(put("/api/v1/capp/quality-plans/{id}", planId), admin, json(updateReq));
        JsonNode releasedPlan = call(post("/api/v1/capp/quality-plans/{id}/release", planId), admin, null);
        assertThat(releasedPlan.path("status").asText()).isEqualTo("RELEASED");

        // 已发布 → 不可再改
        mockMvc.perform(put("/api/v1/capp/quality-plans/" + planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateReq)))
                .andExpect(status().isBadRequest());

        // 被引用的检验项不能删
        mockMvc.perform(delete("/api/v1/capp/quality-items/" + itemDictId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 控制计划 Excel
        MvcResult exported = mockMvc.perform(get("/api/v1/capp/quality-plans/{id}/export", planId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        try (Workbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(exported.getResponse().getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("控制计划");
            assertThat(sheet.getRow(2).getCell(3).getStringCellValue()).isEqualTo("受控特性");
            assertThat(sheet.getRow(3).getCell(3).getStringCellValue()).isEqualTo("外圆直径");
            assertThat(sheet.getRow(4).getCell(3).getStringCellValue()).isEqualTo("精车外圆直径");
            assertThat(sheet.getRow(3).getCell(11).getStringCellValue()).isEqualTo("是");
        }

        // 发布路线后可以下发
        call(post("/api/v1/capp/routings/{id}/release", routingId), admin, null);
        JsonNode dispatch = call(post("/api/v1/capp/erp-dispatches"), admin,
                json(Map.of("routingId", routingId)));
        long dispatchId = dispatch.path("id").asLong();
        assertThat(dispatch.path("code").asText()).startsWith("ED-");
        assertThat(dispatch.path("status").asText()).isEqualTo("PENDING");
        assertThat(dispatch.path("payloadSha").asText()).hasSize(64);
        assertThat(dispatch.path("payload").path("payloadVersion").asText()).isEqualTo("1.0");
        assertThat(dispatch.path("payload").path("operations")).hasSize(2);
        assertThat(dispatch.path("payload").path("operations").get(0).path("name").asText())
                .isEqualTo("粗车");
        assertThat(dispatch.path("payload").path("totals").path("totalMinutes").asDouble())
                .isCloseTo(3.2, within(0.001));
        assertThat(dispatch.path("payload").path("routing").path("code").asText())
                .startsWith("RT-");

        // 原始 JSON 取数（P9 适配器入口）
        MvcResult payload = mockMvc.perform(get("/api/v1/capp/erp-dispatches/{id}/payload", dispatchId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        String payloadJson = payload.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(payloadJson).contains("\"operations\"").contains("\"totals\"");

        // 回执
        JsonNode acked = call(post("/api/v1/capp/erp-dispatches/{id}/callback", dispatchId), admin,
                json(Map.of("status", "SENT", "message", "试跑：模拟 ERP 已接收")));
        assertThat(acked.path("status").asText()).isEqualTo("SENT");
        assertThat(acked.path("message").asText()).contains("模拟 ERP 已接收");

        // 载荷是快照而非实时拼装：两次读取的 generatedAt 必须一致（实时重算会生成新时间）
        JsonNode firstRead = call(get("/api/v1/capp/erp-dispatches/{id}", dispatchId), admin, null);
        JsonNode secondRead = call(get("/api/v1/capp/erp-dispatches/{id}", dispatchId), admin, null);
        assertThat(firstRead.path("payload").path("generatedAt").asText())
                .isEqualTo(secondRead.path("payload").path("generatedAt").asText());
        assertThat(secondRead.path("payload").path("operations")).hasSize(2);
    }

    /** P6-7 典型工艺库：另存为典型 → 编辑/发布 → 套用到零件，且与零件路线互不串台。 */
    @Test
    void typicalRoutingSaveAndApply() throws Exception {
        String admin = login("admin", "Admin@12345");
        long sourceItem = createItem(admin, "典型工艺源零件");
        long targetItem = createItem(admin, "典型工艺目标零件");

        // 源零件路线：2 道工序，其中一道带工步
        Map<String, Object> second = step(20, "精车", "MACHINING", "机加车间", 0.2, 1.0);
        second.put("substeps", List.of(substep(1, "装夹", "三爪卡盘", null, "0.1", "0")));
        long sourceId = call(post("/api/v1/capp/routings"), admin,
                json(routing("轴类工艺", sourceItem, steps(
                        step(10, "下料", "MACHINING", "下料车间", 0, 5), second))))
                .path("id").asLong();

        // 另存为典型工艺：不绑零件，工序/工步整份拷入，回到草稿待编辑
        JsonNode typical = call(post("/api/v1/capp/routings/{id}/save-as-typical", sourceId), admin,
                json(Map.of("name", "轴类典型工艺")));
        long typicalId = typical.path("id").asLong();
        assertThat(typical.path("code").asText()).startsWith("RT-");
        assertThat(typical.path("typical").asBoolean()).isTrue();
        assertThat(typical.path("objectId").isMissingNode() || typical.path("objectId").isNull()).isTrue();
        assertThat(typical.path("status").asText()).isEqualTo("DRAFT");
        assertThat(typical.path("steps")).hasSize(2);
        assertThat(typical.path("steps").get(1).path("substeps")).hasSize(1);
        assertThat(typical.path("totalMinutes").decimalValue()).isEqualByComparingTo("6.2");

        // 列表分离：typical=true 才出典型工艺库，零件路线列表里绝不会混进来
        JsonNode typicalList = call(get("/api/v1/capp/routings?typical=true"), admin, null);
        assertThat(typicalList.path("items").findValuesAsText("code"))
                .contains(typical.path("code").asText());
        JsonNode partList = call(get("/api/v1/capp/routings?objectId=" + sourceItem), admin, null);
        assertThat(partList.path("items").findValuesAsText("typical")).containsOnly("false");

        // 典型工艺同公司内名称唯一
        mockMvc.perform(post("/api/v1/capp/routings/{id}/save-as-typical", sourceId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "轴类典型工艺"))))
                .andExpect(status().isConflict());

        // 直接新建典型工艺（不绑零件）
        Map<String, Object> direct = new HashMap<>();
        direct.put("name", "钻孔典型工艺");
        direct.put("typical", true);
        direct.put("steps", steps(step(10, "钻孔", "MACHINING", "机加车间", 0, 2)));
        JsonNode createdTypical = call(post("/api/v1/capp/routings"), admin, json(direct));
        assertThat(createdTypical.path("typical").asBoolean()).isTrue();
        assertThat(createdTypical.path("objectId").isMissingNode()
                || createdTypical.path("objectId").isNull()).isTrue();
        assertThat(createdTypical.path("totalMinutes").decimalValue()).isEqualByComparingTo("2");

        // 零件路线缺零件 → 400（库里 object_id 允许为空，语义由服务端守住）
        Map<String, Object> noObject = new HashMap<>();
        noObject.put("name", "缺零件的路线");
        noObject.put("steps", steps(step(10, "下料", "MACHINING", null, 0, 1)));
        mockMvc.perform(post("/api/v1/capp/routings")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(noObject)))
                .andExpect(status().isBadRequest());

        // 编辑典型工艺（改工时），typical 标记必须与当前一致
        Map<String, Object> typicalEdit = new HashMap<>();
        typicalEdit.put("name", "轴类典型工艺");
        typicalEdit.put("typical", true);
        typicalEdit.put("applicability", "通用");
        typicalEdit.put("steps", steps(step(10, "下料", "MACHINING", "下料车间", 0, 4),
                step(20, "精车", "MACHINING", "机加车间", 0.2, 1.0)));
        JsonNode edited = call(put("/api/v1/capp/routings/{id}", typicalId), admin, json(typicalEdit));
        assertThat(edited.path("totalMinutes").decimalValue()).isEqualByComparingTo("5.2");

        // 典型 ↔ 零件不能直接改开关（各有专属动作）
        Map<String, Object> flip = new HashMap<>(typicalEdit);
        flip.put("typical", false);
        flip.put("objectId", targetItem);
        mockMvc.perform(put("/api/v1/capp/routings/{id}", typicalId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(flip)))
                .andExpect(status().isBadRequest());

        // 典型工艺发布：不参与「同零件唯一投产路线」，不会顶掉源零件路线
        JsonNode released = call(post("/api/v1/capp/routings/{id}/release", typicalId), admin, null);
        assertThat(released.path("status").asText()).isEqualTo("RELEASED");
        assertThat(call(get("/api/v1/capp/routings/{id}", sourceId), admin, null).path("status").asText())
                .isEqualTo("DRAFT");

        // 套用到零件：生成该零件的新草稿路线，工序/工步与典型一致，典型本身不动
        JsonNode applied = call(post("/api/v1/capp/routings/{id}/apply", typicalId), admin,
                json(Map.of("objectId", targetItem, "name", "轴类工艺（套用）")));
        long appliedId = applied.path("id").asLong();
        assertThat(applied.path("objectId").asLong()).isEqualTo(targetItem);
        assertThat(applied.path("objectCode").asText()).isNotBlank();
        assertThat(applied.path("typical").asBoolean()).isFalse();
        assertThat(applied.path("status").asText()).isEqualTo("DRAFT");
        assertThat(applied.path("versionNo").asInt()).isEqualTo(1);
        assertThat(applied.path("name").asText()).isEqualTo("轴类工艺（套用）");
        assertThat(applied.path("steps")).hasSize(2);
        assertThat(applied.path("totalMinutes").decimalValue()).isEqualByComparingTo("5.2");

        // 再次套用：版本号接着该零件的最大值递增
        JsonNode appliedAgain = call(post("/api/v1/capp/routings/{id}/apply", typicalId), admin,
                json(Map.of("objectId", targetItem)));
        assertThat(appliedAgain.path("versionNo").asInt()).isEqualTo(2);
        assertThat(appliedAgain.path("name").asText()).isEqualTo("轴类典型工艺");

        // 零件路线不能「套用」（只有典型工艺可以）
        mockMvc.perform(post("/api/v1/capp/routings/{id}/apply", appliedId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("objectId", sourceItem))))
                .andExpect(status().isBadRequest());

        // 套用后正常走「发布 → 归档」，且典型工艺不受影响
        JsonNode appliedReleased = call(post("/api/v1/capp/routings/{id}/release", appliedId), admin, null);
        assertThat(appliedReleased.path("status").asText()).isEqualTo("RELEASED");
        assertThat(call(get("/api/v1/capp/routings/{id}", typicalId), admin, null)
                .path("status").asText()).isEqualTo("RELEASED");
    }

    // ------------------------------------------------------------------ 工具

    private Map<String, Object> substep(int seqNo, String name, String content, Long resourceId,
                                        String setupMinutes, String runMinutes) {
        Map<String, Object> substep = new HashMap<>();
        substep.put("seqNo", seqNo);
        substep.put("name", name);
        substep.put("content", content);
        substep.put("resourceId", resourceId);
        substep.put("setupMinutes", new java.math.BigDecimal(setupMinutes));
        substep.put("runMinutes", new java.math.BigDecimal(runMinutes));
        return substep;
    }

    private Map<String, Object> timeRule(String name, String opType, String workCenter,
                                         String setupBase, String setupFactor, String runFactor,
                                         int priority) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", name);
        rule.put("opType", opType);
        rule.put("workCenter", workCenter);
        rule.put("setupBaseMinutes", new java.math.BigDecimal(setupBase));
        rule.put("setupFactor", new java.math.BigDecimal(setupFactor));
        rule.put("runFactor", new java.math.BigDecimal(runFactor));
        rule.put("batchQty", 1);
        rule.put("priority", priority);
        rule.put("enabled", true);
        return rule;
    }

    private Map<String, Object> column(String field, String title, String align, int width) {
        Map<String, Object> column = new HashMap<>();
        column.put("field", field);
        column.put("title", title);
        column.put("align", align);
        column.put("width", width);
        return column;
    }

    private int findRow(Sheet sheet, String firstCellValue) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null
                    && firstCellValue.equals(row.getCell(0).getStringCellValue())) {
                return i;
            }
        }
        return -1;
    }

    private Map<String, Object> routing(String name, long objectId, List<Map<String, Object>> steps) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("objectId", objectId);
        body.put("applicability", "通用");
        body.put("steps", steps);
        return body;
    }

    @SafeVarargs
    private List<Map<String, Object>> steps(Map<String, Object>... steps) {
        return List.of(steps);
    }

    private Map<String, Object> step(int seqNo, String name, String opType, String workCenter,
                                     Number setupMinutes, Number runMinutes) {
        Map<String, Object> step = new HashMap<>();
        step.put("seqNo", seqNo);
        step.put("name", name);
        step.put("opType", opType);
        step.put("workCenter", workCenter);
        step.put("setupMinutes", setupMinutes);
        step.put("runMinutes", runMinutes);
        return step;
    }

    private long currentUserId(String token) throws Exception {
        return call(get("/api/v1/auth/me"), token, null).path("id").asLong();
    }

    private long createItem(String token, String name) throws Exception {
        return call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA")))
                .path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
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
