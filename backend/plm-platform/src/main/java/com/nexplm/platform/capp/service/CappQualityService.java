package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappQualityDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 质量规划（P6-8 简化版）：检验项字典 + 控制计划表。
 *
 * <p>关键规则：</p>
 * <ul>
 *   <li><b>控制计划行内联特性值</b>：{@code item_id} 只是「从字典带出默认值」的入口，
 *       行上的 {@code characteristic/spec/method/gauge} 才是权威值。这样字典后续改动不会
 *       篡改已发布的控制计划，符合质量记录「不可追溯变更」的要求。</li>
 *   <li><b>行绑工序号</b>（{@code step_seq_no}），与工步一致用语义键，序号重建不会让行脱离工序；
 *       同时快照 {@code step_name}，工序改名后历史计划仍显示当时的工序名。</li>
 *   <li><b>新建计划可自动生成骨架</b>：不传 rows 时按路线工序逐条生成空行（一行一工序），
 *       减少「先建空表再手填 40 行」的重复劳动。</li>
 * </ul>
 */
@Service
public class CappQualityService {

    private static final Set<String> CATEGORIES =
            Set.of("DIMENSION", "APPEARANCE", "PERFORMANCE", "MATERIAL");
    /** 控制计划表头列（导出与前端共用同一套列定义，避免两边各写一份）。 */
    public static final List<String> EXPORT_COLUMNS = List.of("行号", "工序号", "工序名称", "受控特性",
            "规格/公差", "评价/测量技术", "量具", "样本容量", "频率", "控制方法", "反应计划", "关键特性", "备注");

    private final JdbcTemplate jdbcTemplate;

    public CappQualityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- 检验项字典

    public List<CappQualityDtos.QualityItemRow> itemList(Long companyId, String keyword, String category) {
        StringBuilder where = new StringBuilder(" WHERE company_id = ? AND deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (category != null && !category.isBlank()) {
            where.append(" AND category = ?");
            args.add(normalizeCategory(category));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ? OR spec ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return jdbcTemplate.query("""
                        SELECT id, code, name, category, spec, method, gauge, unit, remark, updated_at
                          FROM capp_quality_item
                        """ + where + " ORDER BY code",
                args.toArray(),
                (rs, rowNum) -> new CappQualityDtos.QualityItemRow(rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("category"),
                        rs.getString("spec"), rs.getString("method"), rs.getString("gauge"),
                        rs.getString("unit"), rs.getString("remark"),
                        rs.getObject("updated_at", OffsetDateTime.class)));
    }

    @Transactional
    public CappQualityDtos.QualityItemRow createItem(CappQualityDtos.QualityItemSaveReq req,
                                                     Long companyId, Long userId) {
        String code = nextCode("CappQualityItem", "QI");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_quality_item (id, company_id, code, name, category, spec, method,
                                                       gauge, unit, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), normalizeCategory(req.category()),
                blankToNull(req.spec()), blankToNull(req.method()), blankToNull(req.gauge()),
                blankToNull(req.unit()), blankToNull(req.remark()), userId, userId);
        return itemList(companyId, code, null).get(0);
    }

    @Transactional
    public CappQualityDtos.QualityItemRow updateItem(Long id, CappQualityDtos.QualityItemSaveReq req,
                                                     Long companyId, Long userId) {
        requireItem(id, companyId);
        jdbcTemplate.update("""
                        UPDATE capp_quality_item
                           SET name = ?, category = ?, spec = ?, method = ?, gauge = ?, unit = ?,
                               remark = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeCategory(req.category()), blankToNull(req.spec()),
                blankToNull(req.method()), blankToNull(req.gauge()), blankToNull(req.unit()),
                blankToNull(req.remark()), userId, id, companyId);
        return itemList(companyId, null, null).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_NOT_FOUND, "检验项不存在：" + id));
    }

    @Transactional
    public void deleteItem(Long id, Long companyId) {
        requireItem(id, companyId);
        Integer refs = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_quality_plan_row WHERE item_id = ? AND company_id = ?
                """, Integer.class, id, companyId);
        if (refs != null && refs > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT,
                    "该检验项已被 " + refs + " 行控制计划引用，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE capp_quality_item SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ---------------------------------------------------------------- 控制计划

    public List<CappQualityDtos.PlanRowView> planList(Long companyId, String keyword, Long objectId,
                                                      Long routingId, String status) {
        StringBuilder where = new StringBuilder(" WHERE q.company_id = ? AND q.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (objectId != null) {
            where.append(" AND q.object_id = ?");
            args.add(objectId);
        }
        if (routingId != null) {
            where.append(" AND q.routing_id = ?");
            args.add(routingId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND q.status = ?");
            args.add(status.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (q.code ILIKE ? OR q.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        return jdbcTemplate.query("""
                        SELECT q.id, q.code, q.name, q.routing_id, r.code AS routing_code, q.object_id,
                               o.code AS object_code, o.name AS object_name, q.status, q.remark,
                               (SELECT COUNT(1) FROM capp_quality_plan_row w WHERE w.plan_id = q.id)
                                   AS row_count,
                               q.created_at, q.updated_at
                          FROM capp_quality_plan q
                          LEFT JOIN capp_routing r ON r.id = q.routing_id
                          LEFT JOIN plm_object o ON o.id = q.object_id
                        """ + where + " ORDER BY q.updated_at DESC, q.id DESC",
                args.toArray(),
                (rs, rowNum) -> new CappQualityDtos.PlanRowView(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getLong("routing_id"), rs.getString("routing_code"),
                        rs.getLong("object_id"), rs.getString("object_code"),
                        rs.getString("object_name"), rs.getString("status"), rs.getInt("row_count"),
                        rs.getString("remark"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)));
    }

    public CappQualityDtos.PlanDetail plan(Long id, Long companyId) {
        List<CappQualityDtos.PlanDetail> rows = jdbcTemplate.query("""
                        SELECT q.id, q.code, q.name, q.routing_id, r.code AS routing_code, q.object_id,
                               o.code AS object_code, o.name AS object_name, q.status, q.remark,
                               q.created_at, q.updated_at
                          FROM capp_quality_plan q
                          LEFT JOIN capp_routing r ON r.id = q.routing_id
                          LEFT JOIN plm_object o ON o.id = q.object_id
                         WHERE q.id = ? AND q.company_id = ? AND q.deleted = false
                        """,
                (rs, rowNum) -> new CappQualityDtos.PlanDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getLong("routing_id"), rs.getString("routing_code"),
                        rs.getLong("object_id"), rs.getString("object_code"),
                        rs.getString("object_name"), rs.getString("status"), rs.getString("remark"),
                        rowsOf(id),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "控制计划不存在：" + id);
        }
        return rows.get(0);
    }

    private List<CappQualityDtos.PlanRow> rowsOf(Long planId) {
        return jdbcTemplate.query("""
                        SELECT w.id, w.seq_no, w.step_seq_no, w.substep_seq_no, w.step_name, w.item_id,
                               i.code AS item_code, w.characteristic, w.spec, w.method, w.gauge,
                               w.sample_size, w.frequency, w.control_method, w.reaction_plan,
                               w.critical, w.remark
                          FROM capp_quality_plan_row w
                          LEFT JOIN capp_quality_item i ON i.id = w.item_id AND i.deleted = false
                         WHERE w.plan_id = ?
                         ORDER BY w.seq_no
                        """,
                (rs, rowNum) -> new CappQualityDtos.PlanRow(rs.getLong("id"), rs.getInt("seq_no"),
                        (Integer) rs.getObject("step_seq_no"), (Integer) rs.getObject("substep_seq_no"),
                        rs.getString("step_name"), (Long) rs.getObject("item_id"),
                        rs.getString("item_code"), rs.getString("characteristic"), rs.getString("spec"),
                        rs.getString("method"), rs.getString("gauge"), rs.getString("sample_size"),
                        rs.getString("frequency"), rs.getString("control_method"),
                        rs.getString("reaction_plan"), rs.getBoolean("critical"), rs.getString("remark")),
                planId);
    }

    @Transactional
    public CappQualityDtos.PlanDetail createPlan(CappQualityDtos.PlanSaveReq req, Long companyId,
                                                 Long userId) {
        Map<String, Object> routing = requireRouting(req.routingId(), companyId);
        long routingId = req.routingId();
        long objectId = ((Number) routing.get("object_id")).longValue();
        String code = nextCode("CappQualityPlan", "QP");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_quality_plan (id, company_id, code, name, routing_id, object_id,
                                                       status, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), routingId, objectId,
                blankToNull(req.remark()), userId, userId);
        List<CappQualityDtos.PlanRowSaveReq> rows = req.rows();
        if (rows == null || rows.isEmpty()) {
            rows = skeletonRows(routingId, companyId);
        }
        writeRows(id, companyId, rows);
        return plan(id, companyId);
    }

    @Transactional
    public CappQualityDtos.PlanDetail updatePlan(Long id, CappQualityDtos.PlanSaveReq req, Long companyId,
                                                 Long userId) {
        CappQualityDtos.PlanDetail current = plan(id, companyId);
        requireDraft(current.status(), "只有草稿状态的控制计划可以修改");
        Map<String, Object> routing = requireRouting(req.routingId(), companyId);
        long objectId = ((Number) routing.get("object_id")).longValue();
        jdbcTemplate.update("""
                        UPDATE capp_quality_plan
                           SET name = ?, routing_id = ?, object_id = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), req.routingId(), objectId, blankToNull(req.remark()),
                userId, id, companyId);
        jdbcTemplate.update("DELETE FROM capp_quality_plan_row WHERE plan_id = ?", id);
        writeRows(id, companyId, req.rows() == null ? List.of() : req.rows());
        return plan(id, companyId);
    }

    @Transactional
    public CappQualityDtos.PlanDetail releasePlan(Long id, Long companyId, Long userId) {
        CappQualityDtos.PlanDetail current = plan(id, companyId);
        requireDraft(current.status(), "只有草稿状态的控制计划可以发布");
        if (current.rows().isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "控制计划没有行，不能发布");
        }
        long blank = current.rows().stream()
                .filter(row -> row.characteristic() == null || row.characteristic().isBlank())
                .count();
        if (blank > 0) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                    "还有 " + blank + " 行没有填写受控特性，不能发布");
        }
        jdbcTemplate.update("""
                UPDATE capp_quality_plan SET status = 'RELEASED', updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
        return plan(id, companyId);
    }

    @Transactional
    public void deletePlan(Long id, Long companyId) {
        CappQualityDtos.PlanDetail current = plan(id, companyId);
        requireDraft(current.status(), "只有草稿状态的控制计划可以删除");
        jdbcTemplate.update("DELETE FROM capp_quality_plan_row WHERE plan_id = ?", id);
        jdbcTemplate.update("""
                UPDATE capp_quality_plan SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ---------------------------------------------------------------- 内部

    private void writeRows(Long planId, Long companyId, List<CappQualityDtos.PlanRowSaveReq> rows) {
        Set<Integer> seqs = new HashSet<>();
        List<CappQualityDtos.PlanRowSaveReq> ordered = new ArrayList<>(rows);
        ordered.sort((left, right) -> Integer.compare(
                left.seqNo() == null ? 0 : left.seqNo(), right.seqNo() == null ? 0 : right.seqNo()));
        for (CappQualityDtos.PlanRowSaveReq row : ordered) {
            int seqNo = row.seqNo() == null ? 0 : row.seqNo();
            if (seqNo <= 0) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "控制计划行号必须大于 0");
            }
            if (!seqs.add(seqNo)) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "控制计划行号重复：" + seqNo);
            }
            if (row.itemId() != null) {
                requireItem(row.itemId(), companyId);
            }
            jdbcTemplate.update("""
                            INSERT INTO capp_quality_plan_row (id, company_id, plan_id, seq_no,
                                                               step_seq_no, substep_seq_no, step_name,
                                                               item_id, characteristic, spec, method,
                                                               gauge, sample_size, frequency,
                                                               control_method, reaction_plan, critical,
                                                               remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, planId, seqNo, row.stepSeqNo(), row.substepSeqNo(),
                    blankToNull(row.stepName()), row.itemId(), blankToNull(row.characteristic()),
                    blankToNull(row.spec()), blankToNull(row.method()), blankToNull(row.gauge()),
                    blankToNull(row.sampleSize()), blankToNull(row.frequency()),
                    blankToNull(row.controlMethod()), blankToNull(row.reactionPlan()),
                    Boolean.TRUE.equals(row.critical()), blankToNull(row.remark()));
        }
    }

    /** 按路线工序生成骨架行（一行一工序），减少手工建表。 */
    private List<CappQualityDtos.PlanRowSaveReq> skeletonRows(Long routingId, Long companyId) {
        List<CappQualityDtos.PlanRowSaveReq> rows = new ArrayList<>();
        List<Map<String, Object>> steps = jdbcTemplate.queryForList("""
                SELECT seq_no, name FROM capp_routing_step
                 WHERE routing_id = ? AND company_id = ?
                 ORDER BY seq_no
                """, routingId, companyId);
        int seqNo = 10;
        for (Map<String, Object> step : steps) {
            rows.add(new CappQualityDtos.PlanRowSaveReq(seqNo, ((Number) step.get("seq_no")).intValue(),
                    null, (String) step.get("name"), null, null, null, null, null, null, null, null,
                    null, false, null));
            seqNo += 10;
        }
        return rows;
    }

    private Map<String, Object> requireRouting(Long routingId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, object_id FROM capp_routing
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, routingId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_ROUTING_NOT_FOUND, "工艺路线不存在：" + routingId);
        }
        return rows.get(0);
    }

    private void requireItem(Long itemId, Long companyId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_quality_item
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, Integer.class, itemId, companyId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "检验项不存在：" + itemId);
        }
    }

    private void requireDraft(String status, String message) {
        if (!"DRAFT".equals(status)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, message + "（当前状态：" + status + "）");
        }
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "DIMENSION";
        }
        String value = category.trim().toUpperCase();
        if (!CATEGORIES.contains(value)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知检验项类别：" + category
                    + "（可选 " + String.join("/", CATEGORIES) + "）");
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nextCode(String ruleCode, String prefix) {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, ?, 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId(), ruleCode);
        return String.format("%s-%06d", prefix, no == null ? 1L : no);
    }
}
