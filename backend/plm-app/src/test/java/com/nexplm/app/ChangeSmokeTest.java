package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P5 变更管理冒烟：ECR 主档 + 关联对象 + 状态机守卫。
 *
 * <p>规则：无「受影响」对象不可提交；只有草稿可改主档；提交后仍可调整关联对象；
 * 受影响对象必须带操作意图，且同一对象同一关系不可重复。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChangeSmokeTest {

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
    void ecrLifecycleWithAffectedObjects() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);

        JsonNode created = call(post("/api/v1/chg/ecrs"), admin, json(Map.of(
                "title", "验证变更申请",
                "changeType", "DESIGN",
                "priority", "HIGH",
                "reason", "装配干涉",
                "receiverId", adminId,
                "projectNo", "S2026005")));
        long ecrId = created.path("id").asLong();
        assertThat(created.path("code").asText()).startsWith("ECR-");
        assertThat(created.path("status").asText()).isEqualTo("DRAFT");

        // 没有受影响对象时不允许提交
        mockMvc.perform(post("/api/v1/chg/ecrs/{id}/submit", ecrId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        long itemId = createItem(admin, "变更零件");
        Map<String, Object> affected = new HashMap<>();
        affected.put("objectType", "PdmItem");
        affected.put("objectId", itemId);
        affected.put("relation", "AFFECTED");
        affected.put("action", "REVISE");
        JsonNode link = call(post("/api/v1/chg/ecrs/{id}/objects", ecrId), admin, json(affected));
        assertThat(link.path("relation").asText()).isEqualTo("AFFECTED");
        assertThat(link.path("revisionFrom").asInt()).isEqualTo(1);
        assertThat(link.path("objectCode").asText()).isNotBlank();

        // 同一对象同一关系重复添加 → 409
        mockMvc.perform(post("/api/v1/chg/ecrs/{id}/objects", ecrId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(affected)))
                .andExpect(status().isConflict());

        // 受影响对象缺操作意图 → 400
        Map<String, Object> noAction = new HashMap<>(affected);
        noAction.remove("action");
        mockMvc.perform(post("/api/v1/chg/ecrs/{id}/objects", ecrId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(noAction)))
                .andExpect(status().isBadRequest());

        JsonNode submitted = call(post("/api/v1/chg/ecrs/{id}/submit", ecrId), admin, null);
        assertThat(submitted.path("status").asText()).isIn("SUBMITTED", "REVIEWING");

        // 提交即发起审批流程：流程发起事件（事务提交后）把状态推进为「评审中」
        assertThat(call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null).path("status").asText())
                .isEqualTo("REVIEWING");

        // 提交后主档不可改
        mockMvc.perform(put("/api/v1/chg/ecrs/{id}", ecrId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "改标题", "changeType", "DESIGN"))))
                .andExpect(status().isConflict());

        // 提交后仍可补充「参考」对象（无需操作意图）
        Map<String, Object> reference = new HashMap<>();
        reference.put("objectType", "PdmItem");
        reference.put("objectId", itemId);
        reference.put("relation", "REFERENCE");
        call(post("/api/v1/chg/ecrs/{id}/objects", ecrId), admin, json(reference));

        JsonNode detail = call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null);
        assertThat(detail.path("objects").size()).isEqualTo(2);
        assertThat(detail.path("requesterName").asText()).isNotBlank();

        JsonNode list = call(get("/api/v1/chg/ecrs?status=REVIEWING"), admin, null);
        assertThat(list.path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(list.path("items").get(0).path("affectedCount").asInt()).isEqualTo(1);
    }

    /** 提交 → 发起 nexplmApproval → 初审与会签（接收人单人）通过 → ECR 回写 APPROVED。 */
    @Test
    void submitStartsApprovalAndArchiveWritesBackApproved() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "审批零件");
        long ecrId = createSubmittedEcr(admin, "审批流程验证", itemId, adminId);

        JsonNode detail = call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null);
        assertThat(detail.path("status").asText()).isEqualTo("REVIEWING");

        // 流程实例挂在 ChgEcr 上：ECR 不在 plm_object，编码/名称需从 chg_ecr 回填
        JsonNode instances = call(get("/api/v1/workflow/instances?objectType=ChgEcr&objectId=" + ecrId),
                admin, null);
        assertThat(instances.size()).isEqualTo(1);
        JsonNode instance = instances.get(0);
        assertThat(instance.path("status").asText()).isEqualTo("RUNNING");
        assertThat(instance.path("processKey").asText()).isEqualTo("nexplmApproval");
        assertThat(instance.path("objectCode").asText()).isEqualTo(detail.path("code").asText());
        String pid = instance.path("processInstanceId").asText();

        completeTask(admin, pid);
        completeTask(admin, pid);

        assertThat(call(get("/api/v1/workflow/instances/{id}", pid), admin, null).path("status").asText())
                .isEqualTo("COMPLETED");
        JsonNode approved = call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null);
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("approvedAt").isNull()).isFalse();
    }

    /** 提交 → 发起人撤回 → ECR 回草稿，可再改再提交。 */
    @Test
    void withdrawPutsEcrBackToDraft() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "撤回零件");
        long ecrId = createSubmittedEcr(admin, "撤回验证", itemId, adminId);

        JsonNode instances = call(get("/api/v1/workflow/instances?objectType=ChgEcr&objectId=" + ecrId),
                admin, null);
        String pid = instances.get(0).path("processInstanceId").asText();
        call(post("/api/v1/workflow/instances/{id}/withdraw", pid), admin, json(Map.of("reason", "信息不全")));

        JsonNode detail = call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null);
        assertThat(detail.path("status").asText()).isEqualTo("DRAFT");
        // 全局 non_null 序列化会省略空字段，这里直接查库断言批准时间被清空
        Integer approved = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chg_ecr WHERE id = ? AND approved_at IS NOT NULL", Integer.class, ecrId);
        assertThat(approved).isZero();
        // 回草稿后主档可再次修改
        call(put("/api/v1/chg/ecrs/{id}", ecrId), admin,
                json(Map.of("title", "撤回后改标题", "changeType", "DESIGN")));
    }

    /** ECN/Problem 闭环 + 受影响对象升版执行（P5 DoD：执行后版本正确递增）。 */
    @Test
    void ecnProblemAndExecutionFlow() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long itemId = createItem(admin, "执行零件");
        Integer revision = jdbcTemplate.queryForObject(
                "SELECT revision FROM plm_object WHERE id = ?", Integer.class, itemId);
        assertThat(revision).isEqualTo(1);

        long ecrId = createSubmittedEcr(admin, "执行验证", itemId, adminId);

        // 未批准不允许执行 / 下发通知
        mockMvc.perform(post("/api/v1/chg/ecrs/{id}/execute", ecrId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/chg/ecrs/{id}/ecns", ecrId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "越权通知"))))
                .andExpect(status().isConflict());

        // 走审批流程：初审 + 会签（均为接收人）通过后才批准
        JsonNode instances = call(get("/api/v1/workflow/instances?objectType=ChgEcr&objectId=" + ecrId),
                admin, null);
        String pid = instances.get(0).path("processInstanceId").asText();
        completeTask(admin, pid);
        completeTask(admin, pid);
        assertThat(call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null).path("status").asText())
                .isEqualTo("APPROVED");

        JsonNode ecn = call(post("/api/v1/chg/ecrs/{id}/ecns", ecrId), admin,
                json(Map.of("title", "执行通知")));
        long ecnId = ecn.path("id").asLong();
        assertThat(ecn.path("status").asText()).isEqualTo("DRAFT");
        assertThat(ecn.path("code").asText()).startsWith("ECN-");
        assertThat(call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null).path("status").asText())
                .isEqualTo("NOTICED");

        JsonNode issued = call(post("/api/v1/chg/ecns/{id}/issue", ecnId), admin, null);
        assertThat(issued.path("status").asText()).isEqualTo("ISSUED");

        JsonNode problem = call(post("/api/v1/chg/ecns/{id}/problems", ecnId), admin,
                json(Map.of("description", "装配尺寸超差")));
        long problemId = problem.path("id").asLong();
        assertThat(problem.path("status").asText()).isEqualTo("OPEN");

        // 有未关闭问题，通知不能关闭
        mockMvc.perform(post("/api/v1/chg/ecns/{id}/close", ecnId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        call(post("/api/v1/chg/problems/{id}/close", problemId), admin, null);
        JsonNode closedEcn = call(post("/api/v1/chg/ecns/{id}/close", ecnId), admin, null);
        assertThat(closedEcn.path("status").asText()).isEqualTo("CLOSED");

        // 执行：受影响对象升版
        JsonNode result = call(post("/api/v1/chg/ecrs/{id}/execute", ecrId), admin, null);
        assertThat(result.path("succeeded").asInt()).isEqualTo(1);
        assertThat(result.path("skipped").asInt()).isZero();

        JsonNode detail = call(get("/api/v1/chg/ecrs/{id}", ecrId), admin, null);
        assertThat(detail.path("status").asText()).isEqualTo("EXECUTING");
        assertThat(detail.path("objects").get(0).path("execStatus").asText()).isEqualTo("DONE");
        assertThat(detail.path("objects").get(0).path("revisionTo").asInt()).isEqualTo(2);

        // 旧版本降为历史，新版本为 v2 且同步复制子表
        Integer oldIsLatest = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM plm_object WHERE id = ? AND is_latest = false", Integer.class, itemId);
        assertThat(oldIsLatest).isEqualTo(1);
        Integer newCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM plm_object o
                  JOIN item_master im ON im.object_id = o.id
                 WHERE o.master_id = ? AND o.revision = 2 AND o.iteration = 1
                   AND o.is_latest = true AND o.deleted = false AND o.lifecycle_state = 'Draft'
                """, Integer.class, itemId);
        assertThat(newCount).isEqualTo(1);

        // 追踪时间线：申请 → 评审 → 通知 → 执行 → 关闭，全程可回溯
        call(post("/api/v1/chg/ecrs/{id}/close", ecrId), admin, null);
        JsonNode timeline = call(get("/api/v1/chg/ecrs/{id}/timeline", ecrId), admin, null);
        assertThat(timeline.size()).isGreaterThanOrEqualTo(10);
        assertThat(timeline.get(0).path("stage").asText()).isEqualTo("CREATE");
        Set<String> stages = new HashSet<>();
        timeline.forEach(node -> stages.add(node.path("stage").asText()));
        assertThat(stages).contains("CREATE", "REVIEW", "APPROVE", "NOTICE", "PROBLEM", "EXECUTE", "CLOSE");
        OffsetDateTime previous = OffsetDateTime.MIN;
        for (JsonNode node : timeline) {
            OffsetDateTime time = OffsetDateTime.parse(node.path("time").asText());
            assertThat(time.isBefore(previous)).as("时间线必须按时间升序").isFalse();
            previous = time;
        }
        assertThat(timeline.get(timeline.size() - 1).path("title").asText()).isEqualTo("关闭变更申请");
    }

    /**
     * 变更执行必须走生命周期唯一写入口：置 Released/Obsolete 留状态日志，
     * 已在目标态时幂等，不再重复写日志。
     */
    @Test
    void executeAdvancesLifecycleThroughSingleEntrance() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);
        long releaseItem = createItem(admin, "执行发布零件");
        long obsoleteItem = createItem(admin, "执行废弃零件");

        long ecrId = call(post("/api/v1/chg/ecrs"), admin, json(Map.of(
                "title", "生命周期执行验证", "changeType", "DESIGN", "receiverId", adminId)))
                .path("id").asLong();
        addAffected(admin, ecrId, releaseItem, "RELEASE");
        addAffected(admin, ecrId, obsoleteItem, "OBSOLETE");
        call(post("/api/v1/chg/ecrs/{id}/submit", ecrId), admin, null);
        approveEcr(admin, ecrId);

        JsonNode result = call(post("/api/v1/chg/ecrs/{id}/execute", ecrId), admin, null);
        assertThat(result.path("succeeded").asInt()).isEqualTo(2);
        assertThat(result.path("skipped").asInt()).isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_state FROM plm_object WHERE id = ?", String.class, releaseItem))
                .isEqualTo("Released");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lifecycle_state FROM plm_object WHERE id = ?", String.class, obsoleteItem))
                .isEqualTo("Obsolete");
        // released_at / obsolete_at 由 transition 统一补，不再由变更执行自己写
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM plm_object WHERE id = ? AND released_at IS NOT NULL",
                Integer.class, releaseItem)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM plm_object WHERE id = ? AND obsolete_at IS NOT NULL",
                Integer.class, obsoleteItem)).isEqualTo(1);
        // 状态日志：动作码来自生命周期定义，起点为草稿
        assertThat(stateLogCount(releaseItem, "release", "Draft", "Released")).isEqualTo(1);
        assertThat(stateLogCount(obsoleteItem, "obsolete", "Draft", "Obsolete")).isEqualTo(1);

        // 幂等：对已发布对象再次发布，不报错也不重复写日志
        long againEcr = createSubmittedEcr(admin, "重复发布", releaseItem, adminId, "RELEASE");
        approveEcr(admin, againEcr);
        JsonNode again = call(post("/api/v1/chg/ecrs/{id}/execute", againEcr), admin, null);
        assertThat(again.path("succeeded").asInt()).isEqualTo(1);
        assertThat(again.path("skipped").asInt()).isZero();
        assertThat(stateLogCount(releaseItem, "release", "Draft", "Released")).isEqualTo(1);
    }

    /** 影响分析：3 层 BOM 的向上/向下展开。 */
    @Test
    void impactAnalysisWalksBomBothDirections() throws Exception {
        String admin = login("admin", "Admin@12345");
        long assembly = createItem(admin, "总成A");
        long subAssembly = createItem(admin, "部件B");
        long part = createItem(admin, "零件C");
        insertBomLine(assembly, subAssembly);
        insertBomLine(subAssembly, part);

        long downEcr = createEcrWithAffected(admin, "向下影响", assembly);
        JsonNode down = call(get("/api/v1/chg/ecrs/{id}/impact?depth=3&viewType=DESIGN", downEcr), admin, null);
        Map<String, Integer> downDepths = new HashMap<>();
        for (JsonNode row : down) {
            assertThat(row.path("direction").asText()).isEqualTo("DOWN");
            downDepths.put(row.path("objectId").asText(), row.path("depth").asInt());
        }
        assertThat(downDepths.get(String.valueOf(subAssembly))).isEqualTo(1);
        assertThat(downDepths.get(String.valueOf(part))).isEqualTo(2);
        assertThat(downDepths.containsKey(String.valueOf(assembly))).isFalse();

        long upEcr = createEcrWithAffected(admin, "向上影响", part);
        JsonNode up = call(get("/api/v1/chg/ecrs/{id}/impact?depth=3&viewType=DESIGN", upEcr), admin, null);
        Map<String, Integer> upDepths = new HashMap<>();
        for (JsonNode row : up) {
            assertThat(row.path("direction").asText()).isEqualTo("UP");
            upDepths.put(row.path("objectId").asText(), row.path("depth").asInt());
        }
        assertThat(upDepths.get(String.valueOf(subAssembly))).isEqualTo(1);
        assertThat(upDepths.get(String.valueOf(assembly))).isEqualTo(2);

        // 导出影响分析：xlsx 二进制，行数/表头与页面口径一致
        MvcResult exported = mockMvc.perform(
                        get("/api/v1/chg/ecrs/{id}/impact/export?depth=3&viewType=DESIGN", downEcr)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("chg-impact-")))
                .andReturn();
        byte[] bytes = exported.getResponse().getContentAsByteArray();
        assertThat(bytes.length).isGreaterThan(0);
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("方向");
            // lastRowNum 是 0 基下标：表头占 0，数据行 N 条则末行下标 = N
            assertThat(sheet.getLastRowNum()).isEqualTo(down.size());
        }
    }

    private void insertBomLine(long parentId, long childId) {
        jdbcTemplate.update("""
                INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty)
                VALUES (?, ?, ?, 'DESIGN', 10, 1)
                """, com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), parentId, childId);
    }

    private long createEcrWithAffected(String admin, String title, long objectId) throws Exception {
        long ecrId = call(post("/api/v1/chg/ecrs"), admin,
                json(Map.of("title", title, "changeType", "DESIGN"))).path("id").asLong();
        Map<String, Object> affected = new HashMap<>();
        affected.put("objectType", "PdmItem");
        affected.put("objectId", objectId);
        affected.put("relation", "AFFECTED");
        affected.put("action", "REVISE");
        call(post("/api/v1/chg/ecrs/{id}/objects", ecrId), admin, json(affected));
        return ecrId;
    }

    /** 三套阶段状态机来自元数据 meta_lifecycle（type=CHG），改定义即时生效。 */
    @Test
    void stateMachineComesFromMetadata() throws Exception {
        String admin = login("admin", "Admin@12345");

        JsonNode definitions = call(get("/api/v1/meta/lifecycles"), admin, null);
        Set<String> codes = new HashSet<>();
        definitions.forEach(node -> codes.add(node.path("code").asText()));
        assertThat(codes).contains("CHG_ECR", "CHG_ECN", "CHG_PROBLEM");

        String original = jdbcTemplate.queryForObject(
                "SELECT definition_json::text FROM meta_lifecycle WHERE code = 'CHG_ECR'", String.class);
        try {
            // 清空 ECR 的迁移表 → 任何状态都不能提交（证明校验真的读元数据，不是代码常量）
            jdbcTemplate.update("""
                    UPDATE meta_lifecycle
                       SET definition_json = jsonb_set(definition_json, '{transitions}', '[]'::jsonb)
                     WHERE code = 'CHG_ECR'
                    """);
            long itemId = createItem(admin, "状态机零件");
            long ecrId = createEcrWithAffected(admin, "元数据状态机验证", itemId);
            mockMvc.perform(post("/api/v1/chg/ecrs/{id}/submit", ecrId)
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(status().isConflict());
        } finally {
            jdbcTemplate.update(
                    "UPDATE meta_lifecycle SET definition_json = CAST(? AS jsonb) WHERE code = 'CHG_ECR'",
                    original);
        }

        // 恢复定义后新单据可正常提交并进入评审
        long restoredEcr = createSubmittedEcr(admin, "恢复定义后提交",
                createItem(admin, "状态机零件-恢复后"), currentUserId(admin));
        assertThat(call(get("/api/v1/chg/ecrs/{id}", restoredEcr), admin, null).path("status").asText())
                .isEqualTo("REVIEWING");
    }

    /** 建一张带受影响对象、带接收人的 ECR 并提交（提交即发起审批流程）。 */
    private long createSubmittedEcr(String token, String title, long objectId, long receiverId) throws Exception {
        return createSubmittedEcr(token, title, objectId, receiverId, "REVISE");
    }

    private long createSubmittedEcr(String token, String title, long objectId, long receiverId,
                                    String action) throws Exception {
        long ecrId = call(post("/api/v1/chg/ecrs"), token, json(Map.of(
                "title", title, "changeType", "DESIGN", "receiverId", receiverId))).path("id").asLong();
        addAffected(token, ecrId, objectId, action);
        call(post("/api/v1/chg/ecrs/{id}/submit", ecrId), token, null);
        return ecrId;
    }

    private void addAffected(String token, long ecrId, long objectId, String action) throws Exception {
        Map<String, Object> affected = new HashMap<>();
        affected.put("objectType", "PdmItem");
        affected.put("objectId", objectId);
        affected.put("relation", "AFFECTED");
        affected.put("action", action);
        call(post("/api/v1/chg/ecrs/{id}/objects", ecrId), token, json(affected));
    }

    /** 走完初审 + 会签（同一接收人，两票全通过）。 */
    private void approveEcr(String token, long ecrId) throws Exception {
        JsonNode instances = call(get("/api/v1/workflow/instances?objectType=ChgEcr&objectId=" + ecrId),
                token, null);
        String pid = instances.get(0).path("processInstanceId").asText();
        completeTask(token, pid);
        completeTask(token, pid);
        assertThat(call(get("/api/v1/chg/ecrs/{id}", ecrId), token, null).path("status").asText())
                .isEqualTo("APPROVED");
    }

    private int stateLogCount(long objectId, String action, String from, String to) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM plm_state_log
                 WHERE object_id = ? AND action = ? AND from_state = ? AND to_state = ?
                """, Integer.class, objectId, action, from, to);
        return count == null ? 0 : count;
    }

    /** 办理实例当前的活动任务（本用例初审与会签都指派同一人，取首个任务即可）。 */
    private void completeTask(String token, String processInstanceId) throws Exception {
        JsonNode activeTasks = call(get("/api/v1/workflow/instances/{id}", processInstanceId), token, null)
                .path("activeTasks");
        assertThat(activeTasks.size()).as("实例 %s 应有活动任务", processInstanceId).isGreaterThan(0);
        call(post("/api/v1/workflow/tasks/{taskId}/complete", activeTasks.get(0).path("taskId").asText()),
                token, json(Map.of("comment", "同意")));
    }

    private long currentUserId(String token) throws Exception {
        return call(get("/api/v1/auth/me"), token, null).path("id").asLong();
    }

    // ------------------------------------------------------------ 工具

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
