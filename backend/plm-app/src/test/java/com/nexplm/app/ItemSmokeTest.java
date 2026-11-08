package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 零部件冒烟测试：自动取号、类型/供应方式校验、列表筛选、详情、更新、删除保护。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemSmokeTest {

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
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void itemCrudWithTypeFiltersAndDeleteGuard() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 自动取号 + 初始状态
        JsonNode product = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "主传动总成", "itemType", "PRODUCT", "specification", "ZD-100")));
        long productId = product.path("id").asLong();
        String productCode = product.path("code").asText();
        assertThat(productCode).matches("PN-\\d{6}");
        int productSeq = Integer.parseInt(productCode.substring(3));
        assertThat(product.path("lifecycleState").asText()).isEqualTo("Draft");
        assertThat(product.path("revisionLabel").asText()).isEqualTo("A.1");
        assertThat(product.path("unit").asText()).isEqualTo("EA");
        assertThat(product.path("sourceType").asText()).isEqualTo("MAKE");

        // 2. 零件：材料/单位/重量逐字段回读
        Map<String, Object> partReq = new HashMap<>();
        partReq.put("name", "主动齿轮");
        partReq.put("itemType", "PART");
        partReq.put("specification", "M3 Z=24");
        partReq.put("material", "40Cr");
        partReq.put("unit", "PCS");
        partReq.put("weight", 1.25);
        JsonNode part = call(post("/api/v1/items"), admin, json(partReq));
        long partId = part.path("id").asLong();
        assertThat(part.path("code").asText()).isEqualTo(String.format("PN-%06d", productSeq + 1));
        assertThat(part.path("material").asText()).isEqualTo("40Cr");
        assertThat(part.path("unit").asText()).isEqualTo("PCS");
        assertThat(part.path("weight").asDouble()).isEqualTo(1.25);

        // 3. 标准件：外购 + 标准号
        JsonNode standard = call(post("/api/v1/items"), admin, json(Map.of(
                "name", "六角螺栓 M8×30", "itemType", "STANDARD", "sourceType", "BUY",
                "stdCode", "GB/T 5783")));
        long standardId = standard.path("id").asLong();
        assertThat(standard.path("code").asText()).isEqualTo(String.format("PN-%06d", productSeq + 2));
        assertThat(standard.path("sourceType").asText()).isEqualTo("BUY");
        assertThat(standard.path("stdCode").asText()).isEqualTo("GB/T 5783");

        // 4. 非法类型 / 非法供应方式
        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法类型件", "itemType", "UNKNOWN"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/items")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法供应方式", "itemType", "PART", "sourceType", "RENT"))))
                .andExpect(status().isBadRequest());

        // 5. 列表与筛选（同一测试类内多个用例共享库，按相对数量与包含关系断言）
        JsonNode all = call(get("/api/v1/items?page=1&size=200"), admin, null);
        assertThat(all.path("total").asLong()).isGreaterThanOrEqualTo(3);
        JsonNode parts = call(get("/api/v1/items?page=1&size=200&itemType=PART"), admin, null);
        assertThat(parts.path("items").findValuesAsText("code")).contains(part.path("code").asText());
        JsonNode bought = call(get("/api/v1/items?page=1&size=200&sourceType=BUY"), admin, null);
        assertThat(bought.path("items").findValuesAsText("code")).contains(standard.path("code").asText());
        JsonNode bySpec = call(get("/api/v1/items?page=1&size=200&keyword=M3"), admin, null);
        assertThat(bySpec.path("items").findValuesAsText("code")).contains(part.path("code").asText());

        // 6. 更新：名称与重量变化，版本不变
        JsonNode updated = call(put("/api/v1/items/{id}", partId), admin, json(Map.of(
                "name", "主动齿轮(改)", "itemType", "PART", "material", "20CrMnTi",
                "unit", "PCS", "weight", 1.3)));
        assertThat(updated.path("name").asText()).isEqualTo("主动齿轮(改)");
        assertThat(updated.path("material").asText()).isEqualTo("20CrMnTi");
        assertThat(updated.path("revisionLabel").asText()).isEqualTo("A.1");

        // 7. 删除：无 BOM 引用可删；删除后列表不再包含
        mockMvc.perform(delete("/api/v1/items/{id}", standardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        JsonNode afterDelete = call(get("/api/v1/items?page=1&size=200"), admin, null);
        assertThat(afterDelete.path("items").findValuesAsText("code"))
                .doesNotContain(standard.path("code").asText());
        mockMvc.perform(get("/api/v1/items/{id}", standardId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());

        // 8. 详情可读（产品）
        JsonNode productDetail = call(get("/api/v1/items/{id}", productId), admin, null);
        assertThat(productDetail.path("itemType").asText()).isEqualTo("PRODUCT");
        assertThat(productDetail.path("specification").asText()).isEqualTo("ZD-100");
        assertThat(productDetail.path("isLatest").asBoolean()).isTrue();
    }

    @Test
    void batchCreateUpdateTransferOwnerAndVersionCompare() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 批量新建：2 行合法 + 1 行缺名称 → 逐条独立，部分成功
        JsonNode created = call(post("/api/v1/items/batch-create"), admin, json(Map.of("items", List.of(
                Map.of("name", "批量件甲", "itemType", "PART", "specification", "B-01", "unit", "PCS"),
                Map.of("name", "批量件乙", "itemType", "PART", "material", "45#"),
                Map.of("itemType", "PART")))));
        assertThat(created.path("total").asInt()).isEqualTo(3);
        assertThat(created.path("done").asInt()).isEqualTo(2);
        assertThat(created.path("skipped").asInt()).isEqualTo(1);
        assertThat(created.path("messages").toString()).contains("名称不能为空");

        JsonNode batchItems = call(get("/api/v1/items?page=1&size=200&keyword=批量件"), admin, null).path("items");
        long itemA = idByName(batchItems, "批量件甲");
        long itemB = idByName(batchItems, "批量件乙");
        assertThat(itemA).isPositive();
        assertThat(itemB).isPositive();

        // 2. 批量改属性：material 生效、规格型号保持不变、设计属性块按字段合并
        JsonNode updated = call(put("/api/v1/items/batch-update"), admin, json(Map.of(
                "ids", List.of(itemA, itemB),
                "patch", Map.of("material", "Q235", "designAttr", Map.of("provider", "宝钢")))));
        assertThat(updated.path("done").asInt()).isEqualTo(2);
        JsonNode detailA = call(get("/api/v1/items/{id}", itemA), admin, null);
        assertThat(detailA.path("material").asText()).isEqualTo("Q235");
        assertThat(detailA.path("specification").asText()).isEqualTo("B-01");
        assertThat(detailA.path("designAttr").path("provider").asText()).isEqualTo("宝钢");

        // 3. 转交所有权：同公司内成功，重复转交被跳过提示
        long newOwner = createUser(admin, "batch_owner", "批量转交测试");
        JsonNode transferred = call(post("/api/v1/items/transfer-owner"), admin, json(Map.of(
                "ids", List.of(itemA), "targetUserId", newOwner)));
        assertThat(transferred.path("done").asInt()).isEqualTo(1);
        assertThat(call(get("/api/v1/items/{id}", itemA), admin, null).path("ownerId").asLong())
                .isEqualTo(newOwner);
        JsonNode again = call(post("/api/v1/items/transfer-owner"), admin, json(Map.of(
                "ids", List.of(itemA), "targetUserId", newOwner)));
        assertThat(again.path("done").asInt()).isZero();
        assertThat(again.path("skipped").asInt()).isEqualTo(1);
        assertThat(again.path("messages").toString()).contains("已经是");

        // 4. 版本比较：A.1 检出→检入得到 A.2，改 A.2 材料后逐字段差异应命中 material
        long compareItem = createItem(admin, "版本比较件", "PART", "MAKE");
        call(post("/api/v1/items/{id}/checkout", compareItem), admin, null);
        JsonNode second = call(post("/api/v1/items/{id}/checkin", compareItem), admin, null);
        long secondId = second.path("id").asLong();
        assertThat(second.path("revisionLabel").asText()).isEqualTo("A.2");
        call(put("/api/v1/items/{id}", secondId), admin, json(Map.of(
                "name", "版本比较件", "itemType", "PART", "material", "40Cr")));

        JsonNode compare = call(get("/api/v1/items/" + compareItem + "/versions/compare?baseId="
                + compareItem + "&targetId=" + secondId), admin, null);
        assertThat(compare.path("baseLabel").asText()).isEqualTo("A.1");
        assertThat(compare.path("targetLabel").asText()).isEqualTo("A.2");
        assertThat(compare.path("totalCount").asInt()).isGreaterThan(20);
        assertThat(compare.path("changedCount").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode materialRow = findRow(compare.path("rows"), "material");
        assertThat(materialRow.path("changed").asBoolean()).isTrue();
        assertThat(materialRow.path("targetValue").asText()).isEqualTo("40Cr");
        assertThat(materialRow.path("group").asText()).isEqualTo("基本属性");

        // 5. 比较的边界：同版本、跨零部件都拒绝
        mockMvc.perform(get("/api/v1/items/{id}/versions/compare", compareItem)
                        .param("baseId", String.valueOf(compareItem))
                        .param("targetId", String.valueOf(compareItem))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        long otherItem = createItem(admin, "无关件", "PART", "MAKE");
        mockMvc.perform(get("/api/v1/items/{id}/versions/compare", compareItem)
                        .param("baseId", String.valueOf(compareItem))
                        .param("targetId", String.valueOf(otherItem))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bomTreeWithValidationAndDeleteGuard() throws Exception {
        String admin = login("admin", "Admin@12345");
        long productId = createItem(admin, "BOM 总成", "PRODUCT", "MAKE");
        long assemblyId = createItem(admin, "BOM 部件", "ASSEMBLY", "MAKE");
        long partId = createItem(admin, "BOM 零件", "PART", "MAKE");
        long boltId = createItem(admin, "BOM 螺栓", "STANDARD", "BUY");

        // 1. 搭 3 层级 BOM：总成 → 部件 → (零件 4 件/损耗 2%、螺栓 8 件)
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", productId), admin,
                json(Map.of("childId", assemblyId, "qty", 1)));
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", assemblyId), admin,
                json(Map.of("childId", partId, "qty", 4, "lossRate", 2, "refDesignator", "P1")));
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", assemblyId), admin,
                json(Map.of("childId", boltId, "qty", 8)));

        // 2. 树结构：层级、用量、损耗率、位号、hasChildren
        JsonNode tree = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=3", productId), admin, null);
        assertThat(tree.size()).isEqualTo(1);
        JsonNode assembly = tree.get(0);
        assertThat(assembly.path("childId").asLong()).isEqualTo(assemblyId);
        assertThat(assembly.path("qty").asDouble()).isEqualTo(1.0);
        assertThat(assembly.path("hasChildren").asBoolean()).isTrue();
        assertThat(assembly.path("children").size()).isEqualTo(2);
        JsonNode partNode = assembly.path("children").get(0);
        assertThat(partNode.path("childId").asLong()).isEqualTo(partId);
        assertThat(partNode.path("qty").asDouble()).isEqualTo(4.0);
        assertThat(partNode.path("lossRate").asDouble()).isEqualTo(2.0);
        assertThat(partNode.path("refDesignator").asText()).isEqualTo("P1");
        assertThat(partNode.path("hasChildren").asBoolean()).isFalse();
        long partLineId = partNode.path("lineId").asLong();

        // 3. 重复子件 → 409
        mockMvc.perform(post("/api/v1/items/{id}/bom", productId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", assemblyId, "qty", 2))))
                .andExpect(status().isConflict());

        // 4. 自引用与循环引用 → 400
        mockMvc.perform(post("/api/v1/items/{id}/bom", productId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", productId, "qty", 1))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/items/{id}/bom", partId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", productId, "qty", 1))))
                .andExpect(status().isBadRequest());

        // 5. 用量为 0 → 400（Bean Validation）
        mockMvc.perform(post("/api/v1/items/{id}/bom", assemblyId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("childId", boltId, "qty", 0))))
                .andExpect(status().isBadRequest());

        // 6. 被引用查询
        JsonNode whereUsed = call(get("/api/v1/items/{id}/bom/where-used", partId), admin, null);
        assertThat(whereUsed.size()).isEqualTo(1);
        assertThat(whereUsed.get(0).path("parentId").asLong()).isEqualTo(assemblyId);

        // 7. 结构校验：无问题
        JsonNode validate = call(get("/api/v1/items/{id}/bom/validate?viewType=DESIGN", productId), admin, null);
        assertThat(validate.path("issues").size()).isZero();

        // 8. 修改用量
        JsonNode updated = call(put("/api/v1/items/{id}/bom/lines/{lineId}", assemblyId, partLineId), admin,
                json(Map.of("qty", 5, "unit", "PCS", "lossRate", 3, "refDesignator", "P1,P2")));
        assertThat(updated.path("qty").asDouble()).isEqualTo(5.0);
        assertThat(updated.path("unit").asText()).isEqualTo("PCS");

        // 9. 被 BOM 引用时不能删除零部件
        mockMvc.perform(delete("/api/v1/items/{id}", partId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 10. 删除 BOM 行后可删除零部件
        mockMvc.perform(delete("/api/v1/items/{id}/bom/lines/{lineId}", assemblyId, partLineId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        JsonNode afterRemove = call(get("/api/v1/items/{id}/bom?viewType=DESIGN", assemblyId), admin, null);
        assertThat(afterRemove.size()).isEqualTo(1);
        mockMvc.perform(delete("/api/v1/items/{id}", partId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());

        // 11. 非法视图 400
        mockMvc.perform(get("/api/v1/items/{id}/bom?viewType=NOT_EXIST", productId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bomSummaryAndRollupWithLossRateAndItemMasterUnit() throws Exception {
        String admin = login("admin", "Admin@12345");
        long rootId = createItem(admin, "汇总总成", "PRODUCT", "MAKE");
        long subId = createItem(admin, "汇总部件", "ASSEMBLY", "MAKE");
        long leafId = createItem(admin, "汇总零件", "PART", "MAKE");

        // 根 → 部件 2 件；部件 → 零件 3 件（损耗 10%）；根 → 零件 1 件（同一子件两处出现）
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", subId, "qty", 2)));
        JsonNode subToLeaf = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", subId), admin,
                json(Map.of("childId", leafId, "qty", 3, "lossRate", 10)));
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", leafId, "qty", 1)));
        // 行单位改成 KG 并设置有效期：单位按子件主数据（EA）合并，有效期用于 effectAt 过滤
        long leafLineId = subToLeaf.path("lineId").asLong();
        call(put("/api/v1/items/{id}/bom/lines/{lineId}", subId, leafLineId), admin,
                json(Map.of("qty", 3, "unit", "KG", "lossRate", 10,
                        "effectFrom", "2026-01-01", "effectTo", "2026-12-31")));

        // 1. 展开：用量逐层累乘，有效用量再叠加本层损耗率
        JsonNode summary = call(get("/api/v1/items/{id}/bom/summary?viewType=DESIGN", rootId), admin, null);
        assertThat(summary.size()).isEqualTo(3);
        double rootToLeafQty = 0;
        for (JsonNode row : summary) {
            if (row.path("level").asInt() == 1 && row.path("childId").asLong() == leafId) {
                rootToLeafQty = row.path("qty").asDouble();
            }
            if (row.path("level").asInt() == 2) {
                assertThat(row.path("childId").asLong()).isEqualTo(leafId);
                assertThat(row.path("qty").asDouble()).isEqualTo(6.0);
                assertThat(row.path("lossRate").asDouble()).isEqualTo(10.0);
                assertThat(row.path("effectiveQty").asDouble()).isEqualTo(6.6);
                // 行级单位是快照值，展开按下标行的实际单位展示
                assertThat(row.path("unit").asText()).isEqualTo("KG");
                // 层级路径：根行位置 10 → 下级位置 10
                assertThat(row.path("path").asText()).isEqualTo("10.10");
                assertThat(row.path("effectFrom").asText()).isEqualTo("2026-01-01");
                assertThat(row.path("effectTo").asText()).isEqualTo("2026-12-31");
            }
        }
        assertThat(rootToLeafQty).isEqualTo(1.0);

        // 2. 汇总：同一子件多处出现合并累计，单位取子件主数据（EA）
        JsonNode rollup = call(get("/api/v1/items/{id}/bom/rollup?viewType=DESIGN", rootId), admin, null);
        assertThat(rollup.size()).isEqualTo(2);
        boolean leafFound = false;
        for (JsonNode row : rollup) {
            if (row.path("childId").asLong() == leafId) {
                leafFound = true;
                assertThat(row.path("occurrences").asInt()).isEqualTo(2);
                assertThat(row.path("totalQty").asDouble()).isEqualTo(7.0);
                assertThat(row.path("totalEffectiveQty").asDouble()).isEqualTo(7.6);
                assertThat(row.path("unit").asText()).isEqualTo("EA");
            }
        }
        assertThat(leafFound).isTrue();

        // 3. 有效性过滤：行有效期 2026-01-01~2026-12-31，区间内可见、区间外不可见
        JsonNode inRange = call(get("/api/v1/items/{id}/bom/summary?viewType=DESIGN&effectAt=2026-06-01", rootId),
                admin, null);
        assertThat(inRange.size()).isEqualTo(3);
        JsonNode outRange = call(get("/api/v1/items/{id}/bom/summary?viewType=DESIGN&effectAt=2027-01-01", rootId),
                admin, null);
        assertThat(outRange.size()).isEqualTo(2);
        assertThat(outRange.findValuesAsText("level")).containsOnly("1");
        JsonNode treeOut = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&effectAt=2027-01-01", rootId),
                admin, null);
        assertThat(treeOut.size()).isEqualTo(2);

        // 4. 汇总只统计叶子件（对应旧系统元件汇总口径）
        JsonNode leafRollup = call(get("/api/v1/items/{id}/bom/rollup?viewType=DESIGN&leafOnly=true", rootId),
                admin, null);
        assertThat(leafRollup.size()).isEqualTo(1);
        assertThat(leafRollup.get(0).path("childId").asLong()).isEqualTo(leafId);

        // 5. 层数上限：depth=1 只展开一层
        JsonNode shallow = call(get("/api/v1/items/{id}/bom/summary?viewType=DESIGN&depth=1", rootId),
                admin, null);
        assertThat(shallow.size()).isEqualTo(2);
        assertThat(shallow.findValuesAsText("level")).containsOnly("1");

        // 6. 有效期起止倒置 → 400
        mockMvc.perform(put("/api/v1/items/{id}/bom/lines/{lineId}", subId, leafLineId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("qty", 3, "effectFrom", "2027-01-01", "effectTo", "2026-01-01"))))
                .andExpect(status().isBadRequest());

        // 7. 非法视图 400
        mockMvc.perform(get("/api/v1/items/{id}/bom/rollup?viewType=NOT_EXIST", rootId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bomAlternatesCrudWithGuards() throws Exception {
        String admin = login("admin", "Admin@12345");
        long rootId = createItem(admin, "替代总成", "PRODUCT", "MAKE");
        long partA = createItem(admin, "替代零件A", "PART", "MAKE");
        long partB = createItem(admin, "替代零件B", "PART", "MAKE");
        long partC = createItem(admin, "替代零件C", "PART", "MAKE");
        long loneId = createItem(admin, "未入BOM零件", "PART", "MAKE");

        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", partA, "qty", 1)));

        // 1. 未出现在任何 BOM 中的零部件不能维护替代件
        mockMvc.perform(post("/api/v1/items/{id}/alternates", loneId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alternateId", partB))))
                .andExpect(status().isBadRequest());

        // 2. 不能把自身作为备选件
        mockMvc.perform(post("/api/v1/items/{id}/alternates", partA)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alternateId", partA))))
                .andExpect(status().isBadRequest());

        // 3. 新增全局双向替代
        JsonNode globalAlt = call(post("/api/v1/items/{id}/alternates", partA), admin,
                json(Map.of("alternateId", partB, "isGlobal", true, "isTwoWay", true, "priorityLevel", "1")));
        assertThat(globalAlt.path("alternateId").asLong()).isEqualTo(partB);
        assertThat(globalAlt.path("code").asText()).isNotBlank();
        assertThat(globalAlt.path("isGlobal").asBoolean()).isTrue();
        assertThat(globalAlt.path("isTwoWay").asBoolean()).isTrue();
        long globalRowId = globalAlt.path("id").asLong();

        // 4. 同一备选件重复添加 → 409
        mockMvc.perform(post("/api/v1/items/{id}/alternates", partA)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alternateId", partB))))
                .andExpect(status().isConflict());

        // 5. 全局替代在所有视图都可见
        assertThat(call(get("/api/v1/items/{id}/alternates?viewType=PROCESS", partA), admin, null).size())
                .isEqualTo(1);

        // 6. 修改优先级/排序/双向
        JsonNode updatedAlt = call(put("/api/v1/items/{id}/alternates/{alternateRowId}", partA, globalRowId),
                admin, json(Map.of("priorityLevel", "2", "lineNo", 30, "isTwoWay", false)));
        assertThat(updatedAlt.path("priorityLevel").asText()).isEqualTo("2");
        assertThat(updatedAlt.path("lineNo").asInt()).isEqualTo(30);
        assertThat(updatedAlt.path("isTwoWay").asBoolean()).isFalse();

        // 7. 局部替代只在指定视图可见
        JsonNode localAlt = call(post("/api/v1/items/{id}/alternates", partA), admin,
                json(Map.of("alternateId", partC, "viewType", "PROCESS", "isGlobal", false)));
        assertThat(localAlt.path("viewType").asText()).isEqualTo("PROCESS");
        assertThat(call(get("/api/v1/items/{id}/alternates?viewType=DESIGN", partA), admin, null).size())
                .isEqualTo(1);
        assertThat(call(get("/api/v1/items/{id}/alternates?viewType=PROCESS", partA), admin, null).size())
                .isEqualTo(2);
        assertThat(call(get("/api/v1/items/{id}/alternates", partA), admin, null).size())
                .isEqualTo(2);

        // 8. 替代行归属校验：换一个零部件访问该行 → 404
        mockMvc.perform(delete("/api/v1/items/{id}/alternates/{alternateRowId}", rootId,
                        localAlt.path("id").asLong())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());

        // 9. 删除后列表减少；且替代关系不影响 BOM 展开
        mockMvc.perform(delete("/api/v1/items/{id}/alternates/{alternateRowId}", partA, globalRowId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        assertThat(call(get("/api/v1/items/{id}/alternates", partA), admin, null).size()).isEqualTo(1);
        assertThat(call(get("/api/v1/items/{id}/bom/summary?viewType=DESIGN", rootId), admin, null).size())
                .isEqualTo(1);
    }

    @Test
    void bomBaselineFreezeAndDiff() throws Exception {
        String admin = login("admin", "Admin@12345");
        long rootId = createItem(admin, "基线总成", "PRODUCT", "MAKE");
        long subId = createItem(admin, "基线部件", "ASSEMBLY", "MAKE");
        long leafId = createItem(admin, "基线零件", "PART", "MAKE");
        long boltId = createItem(admin, "基线螺栓", "STANDARD", "BUY");
        long emptyId = createItem(admin, "无结构件", "PART", "MAKE");

        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", subId, "qty", 1)));
        JsonNode leafLine = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", subId), admin,
                json(Map.of("childId", leafId, "qty", 2)));
        long leafLineId = leafLine.path("lineId").asLong();

        // 1. 冻结基线：总成 → 部件 → 零件 共 2 行
        JsonNode baseline = call(post("/api/v1/items/{id}/baselines", rootId), admin,
                json(Map.of("code", "BL-001", "name", "初始结构", "viewType", "DESIGN")));
        assertThat(baseline.path("code").asText()).isEqualTo("BL-001");
        assertThat(baseline.path("lineCount").asInt()).isEqualTo(2);
        assertThat(baseline.path("frozenAt").asText()).isNotBlank();
        long baselineId = baseline.path("id").asLong();

        JsonNode lines = call(get("/api/v1/items/{id}/baselines/{baselineId}/lines", rootId, baselineId),
                admin, null);
        assertThat(lines.size()).isEqualTo(2);
        assertThat(lines.findValuesAsText("path")).containsExactly("10", "10.10");

        // 2. 未改动时差异为空
        JsonNode noDiff = call(get("/api/v1/items/{id}/baselines/{baselineId}/diff", rootId, baselineId),
                admin, null);
        assertThat(noDiff.path("added").asInt()).isZero();
        assertThat(noDiff.path("removed").asInt()).isZero();
        assertThat(noDiff.path("changed").asInt()).isZero();
        assertThat(noDiff.path("rows").size()).isZero();

        // 3. 用量变更 → QTY_CHANGED
        call(put("/api/v1/items/{id}/bom/lines/{lineId}", subId, leafLineId), admin,
                json(Map.of("qty", 3)));
        JsonNode qtyDiff = call(get("/api/v1/items/{id}/baselines/{baselineId}/diff", rootId, baselineId),
                admin, null);
        assertThat(qtyDiff.path("changed").asInt()).isEqualTo(1);
        JsonNode qtyRow = qtyDiff.path("rows").get(0);
        assertThat(qtyRow.path("changeType").asText()).isEqualTo("QTY_CHANGED");
        assertThat(qtyRow.path("path").asText()).isEqualTo("10.10");
        assertThat(qtyRow.path("baselineQty").asDouble()).isEqualTo(2.0);
        assertThat(qtyRow.path("currentQty").asDouble()).isEqualTo(3.0);

        // 4. 新增子件 → ADDED；删除子件 → REMOVED
        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", subId), admin,
                json(Map.of("childId", boltId, "qty", 8)));
        mockMvc.perform(delete("/api/v1/items/{id}/bom/lines/{lineId}", subId, leafLineId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        JsonNode mixedDiff = call(get("/api/v1/items/{id}/baselines/{baselineId}/diff", rootId, baselineId),
                admin, null);
        assertThat(mixedDiff.path("added").asInt()).isEqualTo(1);
        assertThat(mixedDiff.path("removed").asInt()).isEqualTo(1);
        assertThat(mixedDiff.path("rows").findValuesAsText("changeType"))
                .containsExactlyInAnyOrder("ADDED", "REMOVED");

        // 5. 基线编码重复 → 409；无结构不能创建基线 → 400
        mockMvc.perform(post("/api/v1/items/{id}/baselines", rootId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "BL-001"))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/items/{id}/baselines", emptyId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "BL-EMPTY"))))
                .andExpect(status().isBadRequest());

        // 6. 列表与跨件归属
        assertThat(call(get("/api/v1/items/{id}/baselines?viewType=DESIGN", rootId), admin, null).size())
                .isEqualTo(1);
        mockMvc.perform(delete("/api/v1/items/{id}/baselines/{baselineId}", emptyId, baselineId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());

        // 7. 删除基线后列表为空
        mockMvc.perform(delete("/api/v1/items/{id}/baselines/{baselineId}", rootId, baselineId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        assertThat(call(get("/api/v1/items/{id}/baselines", rootId), admin, null).size()).isZero();
    }

    @Test
    void bomExcelExportAndImport() throws Exception {
        String admin = login("admin", "Admin@12345");
        long rootId = createItem(admin, "Excel 总成", "PRODUCT", "MAKE");
        long subId = createItem(admin, "Excel 部件", "ASSEMBLY", "MAKE");
        long leafId = createItem(admin, "Excel 零件", "PART", "MAKE");
        String subCode = call(get("/api/v1/items/{id}", subId), admin, null).path("code").asText();
        String leafCode = call(get("/api/v1/items/{id}", leafId), admin, null).path("code").asText();

        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", subId, "qty", 1)));

        // 1. 导出：表头 + 1 行，层级号从 1 开始
        MvcResult exportResult = mockMvc.perform(
                        get("/api/v1/items/{id}/bom/export?viewType=DESIGN", rootId)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        byte[] exported = exportResult.getResponse().getContentAsByteArray();
        assertThat(exported).isNotEmpty();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(exported))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo(subCode);
        }

        // 2. 预演：1 行更新 + 1 行新增 + 1 行编码不存在
        byte[] dirtyFile = buildBomWorkbook(new Object[][]{
                {"1", subCode, 2},
                {"2", leafCode, 3},
                {"3", "NOT-EXIST-CODE", 1}
        });
        JsonNode dirtyPreview = multipartCall("/api/v1/items/{id}/bom/import/preview", rootId, admin,
                "bom.xlsx", dirtyFile, "DESIGN");
        assertThat(dirtyPreview.path("total").asInt()).isEqualTo(3);
        assertThat(dirtyPreview.path("invalid").asInt()).as(dirtyPreview.toString()).isEqualTo(1);
        assertThat(dirtyPreview.path("updateCount").asInt()).isEqualTo(1);
        assertThat(dirtyPreview.path("createCount").asInt()).isEqualTo(1);
        assertThat(dirtyPreview.path("errors").size()).isEqualTo(1);
        assertThat(dirtyPreview.path("errors").get(0).asText()).contains("不存在");

        // 3. 有错误的文件执行导入 → 400
        mockMvc.perform(multipart("/api/v1/items/{id}/bom/import", rootId)
                        .file(new MockMultipartFile("file", "bom.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", dirtyFile))
                        .param("viewType", "DESIGN")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 4. 修正后执行导入：新增 1 行、更新 1 行
        byte[] goodFile = buildBomWorkbook(new Object[][]{
                {"1", subCode, 2},
                {"2", leafCode, 3}
        });
        JsonNode imported = multipartCall("/api/v1/items/{id}/bom/import", rootId, admin,
                "bom.xlsx", goodFile, "DESIGN");
        assertThat(imported.path("created").asInt()).isEqualTo(1);
        assertThat(imported.path("updated").asInt()).isEqualTo(1);
        assertThat(imported.path("skipped").asInt()).isZero();

        // 5. 结构生效：根下 2 个子件，数量分别为 2 / 3
        JsonNode tree = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=2", rootId), admin, null);
        assertThat(tree.size()).isEqualTo(2);
        assertThat(tree.get(0).path("childId").asLong()).isEqualTo(subId);
        assertThat(tree.get(0).path("qty").asDouble()).isEqualTo(2.0);
        assertThat(tree.get(1).path("childId").asLong()).isEqualTo(leafId);
        assertThat(tree.get(1).path("qty").asDouble()).isEqualTo(3.0);

        // 6. 重复导入同样的文件 → 全部不变
        JsonNode again = multipartCall("/api/v1/items/{id}/bom/import/preview", rootId, admin,
                "bom.xlsx", goodFile, "DESIGN");
        assertThat(again.path("unchangedCount").asInt()).isEqualTo(2);
        assertThat(again.path("createCount").asInt()).isZero();
        assertThat(again.path("updateCount").asInt()).isZero();
    }

    @Test
    void bomBatchUpdateQtyAndReplace() throws Exception {
        String admin = login("admin", "Admin@12345");
        long rootId = createItem(admin, "批量总成", "PRODUCT", "MAKE");
        long subId = createItem(admin, "批量部件", "ASSEMBLY", "MAKE");
        long leafA = createItem(admin, "批量零件A", "PART", "MAKE");
        long leafB = createItem(admin, "批量零件B", "PART", "MAKE");
        long leafC = createItem(admin, "批量零件C", "PART", "MAKE");
        long otherRoot = createItem(admin, "无关总成", "PRODUCT", "MAKE");

        call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", subId, "qty", 1)));
        JsonNode lineA = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", subId), admin,
                json(Map.of("childId", leafA, "qty", 2)));
        JsonNode lineB = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", subId), admin,
                json(Map.of("childId", leafB, "qty", 3)));
        long lineAId = lineA.path("lineId").asLong();
        long lineBId = lineB.path("lineId").asLong();

        // 1. 批量改用量：两行同时改成 5 件 / 损耗 4%
        Map<String, Object> qtyReq = new HashMap<>();
        qtyReq.put("lineIds", List.of(lineAId, lineBId));
        qtyReq.put("qty", 5);
        qtyReq.put("lossRate", 4);
        JsonNode qtyResult = call(post("/api/v1/items/{id}/bom/batch/update-qty", rootId), admin, json(qtyReq));
        assertThat(qtyResult.path("updated").asInt()).isEqualTo(2);
        assertThat(qtyResult.path("skipped").asInt()).isZero();
        JsonNode subTree = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=2", subId), admin, null);
        assertThat(subTree.get(0).path("qty").asDouble()).isEqualTo(5.0);
        assertThat(subTree.get(0).path("lossRate").asDouble()).isEqualTo(4.0);

        // 2. 越权：用另一棵树修改这些行 → 全部跳过
        JsonNode denied = call(post("/api/v1/items/{id}/bom/batch/update-qty", otherRoot), admin, json(qtyReq));
        assertThat(denied.path("updated").asInt()).isZero();
        assertThat(denied.path("skipped").asInt()).isEqualTo(2);

        // 3. 批量替换：把 A 全部换成 C
        JsonNode replaced = call(post("/api/v1/items/{id}/bom/batch/replace", rootId), admin,
                json(Map.of("fromChildId", leafA, "toChildId", leafC, "viewType", "DESIGN")));
        assertThat(replaced.path("updated").asInt()).isEqualTo(1);
        JsonNode afterReplace = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=2", subId), admin, null);
        assertThat(afterReplace.findValuesAsText("code")).doesNotContain(
                call(get("/api/v1/items/{id}", leafA), admin, null).path("code").asText());
        assertThat(afterReplace.get(0).path("childId").asLong()).isEqualTo(leafC);

        // 4. 替换成已存在的兄弟子件 → 跳过并给出原因
        JsonNode conflict = call(post("/api/v1/items/{id}/bom/batch/replace", rootId), admin,
                json(Map.of("fromChildId", leafC, "toChildId", leafB, "viewType", "DESIGN")));
        assertThat(conflict.path("updated").asInt()).isZero();
        assertThat(conflict.path("skipped").asInt()).isEqualTo(1);
        assertThat(conflict.path("messages").get(0).asText()).contains("已存在");

        // 5. 替换目标与被替换件相同 → 400
        mockMvc.perform(post("/api/v1/items/{id}/bom/batch/replace", rootId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fromChildId", leafB, "toChildId", leafB))))
                .andExpect(status().isBadRequest());

        // 6. 批量改用量不给任何字段 → 400
        Map<String, Object> emptyReq = new HashMap<>();
        emptyReq.put("lineIds", List.of(lineAId));
        mockMvc.perform(post("/api/v1/items/{id}/bom/batch/update-qty", rootId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(emptyReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bomMoveReorderAndDuplicateGuard() throws Exception {
        String admin = login("admin", "Admin@12345");
        long rootId = createItem(admin, "排序总成", "PRODUCT", "MAKE");
        long subId = createItem(admin, "排序部件", "ASSEMBLY", "MAKE");
        long leafA = createItem(admin, "排序零件A", "PART", "MAKE");
        long leafB = createItem(admin, "排序零件B", "PART", "MAKE");

        JsonNode lineA = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", leafA, "qty", 1)));
        JsonNode lineB = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", rootId), admin,
                json(Map.of("childId", leafB, "qty", 1)));

        // 1. 新增时序号递增，A 在前
        JsonNode tree = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=1", rootId), admin, null);
        assertThat(tree.get(0).path("childId").asLong()).isEqualTo(leafA);
        assertThat(tree.get(1).path("childId").asLong()).isEqualTo(leafB);

        // 2. 交换序号（与前端「上移/下移」实现一致：两次 move 互换 lineNo）
        int lineNoA = lineA.path("lineNo").asInt();
        int lineNoB = lineB.path("lineNo").asInt();
        assertThat(lineNoA).isLessThan(lineNoB);
        call(post("/api/v1/items/{id}/bom/lines/{lineId}/move", rootId, lineA.path("lineId").asLong()), admin,
                json(Map.of("newLineNo", lineNoB, "viewType", "DESIGN")));
        call(post("/api/v1/items/{id}/bom/lines/{lineId}/move", rootId, lineB.path("lineId").asLong()), admin,
                json(Map.of("newLineNo", lineNoA, "viewType", "DESIGN")));

        tree = call(get("/api/v1/items/{id}/bom?viewType=DESIGN&depth=1", rootId), admin, null);
        assertThat(tree.get(0).path("childId").asLong()).isEqualTo(leafB);
        assertThat(tree.get(1).path("childId").asLong()).isEqualTo(leafA);

        // 3. 换父件时目标父件下已有同一子件 → 409 并给出提示
        long lineAInSub = call(post("/api/v1/items/{id}/bom?viewType=DESIGN", subId), admin,
                json(Map.of("childId", leafA, "qty", 2))).path("lineId").asLong();
        MvcResult conflict = mockMvc.perform(post("/api/v1/items/{id}/bom/lines/{lineId}/move", subId, lineAInSub)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("newParentId", rootId, "viewType", "DESIGN"))))
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(conflict.getResponse().getContentAsString()).contains("已存在");
    }

    /**
     * 批次 H·T-H-01：零部件分类（复用 doc_class，entity_code=PdmItem）。
     * 覆盖：示例树可读 → 建分类/属性 → 归类（必填校验）→ 按分类检索（含/不含子分类）
     * → 属性过滤 → 删除保护 → 取消归类后可删。
     */
    @Test
    void itemClassifyTreeAssignAndSearch() throws Exception {
        String admin = login("admin", "Admin@12345");

        // 1. 示例分类树（V71 种子）：PdmItem 下应有 MECH，且与文档分类互不干扰
        JsonNode tree = call(get("/api/v1/item-classes"), admin, null);
        JsonNode mech = null;
        for (JsonNode node : tree) {
            if ("MECH".equals(node.path("code").asText())) {
                mech = node;
            }
        }
        assertThat(mech).as("PdmItem 示例分类 MECH 应已由 V71 建好").isNotNull();
        long mechId = mech.path("id").asLong();

        // 2. 建子分类 + 定义属性（material 必填、surface 可选）
        String suffix = String.valueOf(System.nanoTime() % 100000);
        JsonNode created = call(post("/api/v1/item-classes"), admin, json(Map.of(
                "parentId", mechId, "code", "MECH-T" + suffix, "name", "测试分类" + suffix)));
        long classId = created.path("id").asLong();
        assertThat(created.path("path").asText()).startsWith("/MECH/");

        call(put("/api/v1/item-classes/{id}/attributes", classId), admin, json(Map.of("items", List.of(
                Map.of("code", "material", "name", "材料", "valueType", "STRING", "required", true),
                Map.of("code", "surface", "name", "表面处理", "valueType", "STRING", "required", false)))));
        JsonNode attrs = call(get("/api/v1/item-classes/{id}/attributes", classId), admin, null);
        // 生效属性 = 自身 2 个 + 祖先可继承（MECH 未定义属性，故正好 2 个）
        assertThat(attrs.findValuesAsText("code")).contains("material", "surface");

        // 3. 归类：必填属性缺失应 400，补齐后成功
        long itemId = createItem(admin, "分类测试零件" + suffix, "PART", "MAKE");
        mockMvc.perform(put("/api/v1/items/{id}/classify", itemId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("classId", classId, "attrs", Map.of()))))
                .andExpect(status().isBadRequest());
        JsonNode assigned = call(put("/api/v1/items/{id}/classify", itemId), admin,
                json(Map.of("classId", classId, "attrs", Map.of("material", "40Cr"))));
        assertThat(assigned.path("classId").asLong()).isEqualTo(classId);
        assertThat(assigned.path("attrs").path("material").asText()).isEqualTo("40Cr");

        // 4. 回读
        JsonNode info = call(get("/api/v1/items/{id}/classify", itemId), admin, null);
        assertThat(info.path("classCode").asText()).isEqualTo("MECH-T" + suffix);
        assertThat(info.path("attrs").path("material").asText()).isEqualTo("40Cr");

        // 5. 按分类检索：含子分类能查到，不含子分类查不到
        String code = call(get("/api/v1/items/{id}", itemId), admin, null).path("code").asText();
        JsonNode withChildren = call(get("/api/v1/items?page=1&size=200&classId=" + mechId
                + "&includeChildren=true"), admin, null);
        assertThat(withChildren.path("items").findValuesAsText("code")).contains(code);
        JsonNode withoutChildren = call(get("/api/v1/items?page=1&size=200&classId=" + mechId
                + "&includeChildren=false"), admin, null);
        assertThat(withoutChildren.path("items").findValuesAsText("code")).doesNotContain(code);

        // 6. 属性过滤（ext_json.classAttrs）
        JsonNode matched = call(get("/api/v1/items?page=1&size=200&classId=" + classId
                + "&attr.material=40Cr"), admin, null);
        assertThat(matched.path("items").findValuesAsText("code")).contains(code);
        JsonNode unmatched = call(get("/api/v1/items?page=1&size=200&classId=" + classId
                + "&attr.material=99Cr"), admin, null);
        assertThat(unmatched.path("items").findValuesAsText("code")).doesNotContain(code);

        // 7. 分类下仍有零件 → 删除保护
        MvcResult conflict = mockMvc.perform(delete("/api/v1/item-classes/{id}", classId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(conflict.getResponse().getContentAsString()).contains("零部件");

        // 8. 取消归类后属性值一并清空，分类可删
        Map<String, Object> cancelReq = new HashMap<>();
        cancelReq.put("classId", null);
        cancelReq.put("attrs", Map.of());
        JsonNode cleared = call(put("/api/v1/items/{id}/classify", itemId), admin, json(cancelReq));
        // 取消归类后 classId 为 null（全局 Jackson 配置为 non_null，字段可能整个不返回）
        assertThat(cleared.path("classId").isNull() || cleared.path("classId").isMissingNode()).isTrue();
        assertThat(cleared.path("attrs").isEmpty()).isTrue();
        mockMvc.perform(delete("/api/v1/item-classes/{id}", classId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ 工具

    private JsonNode multipartCall(String path, long itemId, String token, String fileName,
                                   byte[] content, String viewType) throws Exception {
        MvcResult result = mockMvc.perform(multipart(path, itemId)
                        .file(new MockMultipartFile("file", fileName,
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content))
                        .param("viewType", viewType)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data");
    }

    private byte[] buildBomWorkbook(Object[][] dataRows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BOM");
            String[] headers = {"层级号", "编码", "名称", "规格型号", "单位", "数量", "损耗率(%)", "有效用量",
                    "生效日期", "失效日期"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = dataRows[r];
                // 每行为 {层级号, 编码, 数量}，按表头列位写入（数量在第 6 列）
                row.createCell(0).setCellValue(values[0] == null ? "" : values[0].toString());
                row.createCell(1).setCellValue(values[1] == null ? "" : values[1].toString());
                row.createCell(5).setCellValue(values[2] instanceof Number number ? number.doubleValue() : 0);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** 按名称在列表结果里取 id（批量新建不返回逐条 id，走查询更贴近真实调用）。 */
    private long idByName(JsonNode items, String name) {
        for (JsonNode item : items) {
            if (name.equals(item.path("name").asText())) {
                return item.path("id").asLong();
            }
        }
        return -1L;
    }

    private JsonNode findRow(JsonNode rows, String field) {
        for (JsonNode row : rows) {
            if (field.equals(row.path("field").asText())) {
                return row;
            }
        }
        throw new IllegalStateException("差异行里没有字段：" + field);
    }

    private long createUser(String token, String username, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "Passw0rd!",
                                "displayName", displayName, "securityLevel", 1,
                                "roleIds", new long[]{businessRoleId(token)}))))
                .andExpect(status().isOk())
                .andReturn();
        return Long.parseLong(body(result).path("data").asText());
    }

    /**
     * 台账 B14 ①（T-H-01a）：分类关联编码 → 按编码自动匹配分类。
     * 覆盖：关联规则后建议命中 → 已归类不再建议 → 同一规则不能挂两个分类（409）
     * → 跨对象类型规则拒绝（400）→ 未关联规则的分类不给建议。
     */
    @Test
    void classifySuggestedByLinkedCodeRule() throws Exception {
        String admin = login("admin", "Admin@12345");
        // 清掉可能残留的关联，保证本用例从"未关联"开始
        jdbcTemplate.update(
                "UPDATE doc_class SET code_rule_code = NULL WHERE entity_code = 'PdmItem' AND code_rule_code IS NOT NULL");

        // 1. 建分类并关联编码规则 PdmItem（V56 预置，object_type=PdmItem）
        JsonNode clazz = call(post("/api/v1/item-classes"), admin, json(Map.of(
                "code", "AUTO-CODE", "name", "编码自动归类", "codeRuleCode", "PdmItem")));
        long classId = clazz.path("id").asLong();
        assertThat(clazz.path("codeRuleCode").asText()).isEqualTo("PdmItem");

        // 2. 新建零件（自动取号 PN-xxxxxx）→ 建议命中该分类
        long itemId = createItem(admin, "编码自动归类零件", "PART", "MAKE");
        JsonNode suggestion = call(get("/api/v1/items/{id}/classify/suggestion", itemId), admin, null);
        assertThat(suggestion.path("matched").asBoolean()).isTrue();
        assertThat(suggestion.path("classId").asLong()).isEqualTo(classId);
        assertThat(suggestion.path("codeRuleCode").asText()).isEqualTo("PdmItem");

        // 3. 归类之后不再给建议（蓝本语义：已有分类不覆盖）
        call(put("/api/v1/items/{id}/classify", itemId), admin, json(Map.of("classId", classId)));
        JsonNode after = call(get("/api/v1/items/{id}/classify/suggestion", itemId), admin, null);
        assertThat(after.path("matched").asBoolean()).isFalse();
        assertThat(after.path("reason").asText()).contains("已有分类");

        // 4. 同一规则不能再挂第二个分类
        mockMvc.perform(post("/api/v1/item-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "AUTO-CODE2", "name", "冲突分类",
                                "codeRuleCode", "PdmItem")))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 5. 文档类型规则不能挂到零件分类上
        mockMvc.perform(post("/api/v1/item-classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "AUTO-CODE3", "name", "错类型分类",
                                "codeRuleCode", "PdmDoc")))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 6. 未关联规则的分类：建议为空并说明原因
        jdbcTemplate.update("UPDATE doc_class SET code_rule_code = NULL WHERE id = ?", classId);
        long anotherItem = createItem(admin, "无关联规则零件", "PART", "MAKE");
        JsonNode noRule = call(get("/api/v1/items/{id}/classify/suggestion", anotherItem), admin, null);
        assertThat(noRule.path("matched").asBoolean()).isFalse();
        assertThat(noRule.path("reason").asText()).contains("未关联分类");
    }

    private long businessRoleId(String token) throws Exception {
        // 注意：/system/roles 返回数组本身（不是分页对象），别按 items 取
        for (JsonNode role : call(get("/api/v1/system/roles"), token, null)) {
            if ("BUSINESS".equals(role.path("code").asText())) {
                return role.path("id").asLong();
            }
        }
        throw new IllegalStateException("种子数据缺少 BUSINESS 角色");
    }

    private long createItem(String token, String name, String itemType, String sourceType) throws Exception {
        JsonNode created = call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", itemType, "sourceType", sourceType, "unit", "EA")));
        return created.path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                          String token, String payload) throws Exception {
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
