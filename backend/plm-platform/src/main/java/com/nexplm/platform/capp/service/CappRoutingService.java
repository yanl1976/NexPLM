package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工艺路线（P6 任务 1）：零件维度的工艺路线主档 + 工序明细。
 *
 * <p>关键规则：</p>
 * <ul>
 *   <li><b>批量编辑</b>：工序以「整表替换」保存（一次事务内删旧插新），避免前端做差异比对；</li>
 *   <li><b>路线串</b>：详情按 {@code seq_no} 升序拼 {@code 10 下料 → 20 车削}，列表不重复拼；</li>
 *   <li><b>唯一已发布</b>：发布时把同一零件其它 {@code RELEASED} 路线置 {@code ARCHIVED}，
 *       保证「一个零件同一时刻只有一条可投产路线」；</li>
 *   <li><b>状态守卫</b>：只有 {@code DRAFT} 可改/可删；已发布不可改，需 {@code 复制} 成新草稿再改。</li>
 * </ul>
 */
@Service
public class CappRoutingService {

    /** 路线状态：草稿（可改可删）/ 已发布（投产口径）/ 已归档（历史版本）。 */
    private static final Set<String> STATUSES = Set.of("DRAFT", "RELEASED", "ARCHIVED");
    /** 工序类型枚举（P6-3 资源库细化前先用固定枚举）。 */
    private static final Set<String> OP_TYPES =
            Set.of("MACHINING", "ASSEMBLY", "INSPECTION", "HEAT", "SURFACE", "OTHER");

    private final JdbcTemplate jdbcTemplate;

    public CappRoutingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------------ 查询

    public PageResult<CappDtos.RoutingRow> list(Long companyId, String keyword, Long objectId,
                                                String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        StringBuilder where = new StringBuilder(" WHERE r.deleted = false AND r.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            if (!STATUSES.contains(normalized)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "未知工艺路线状态：" + status);
            }
            where.append(" AND r.status = ?");
            args.add(normalized);
        }
        if (objectId != null) {
            where.append(" AND r.object_id = ?");
            args.add(objectId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (r.code ILIKE ? OR r.name ILIKE ? OR o.code ILIKE ? OR o.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_routing r LEFT JOIN plm_object o ON o.id = r.object_id" + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<CappDtos.RoutingRow> items = jdbcTemplate.query("""
                        SELECT r.id, r.code, r.name, r.object_id, o.code AS object_code, o.name AS object_name,
                               r.version_no, r.status, r.applicability, r.released_at,
                               r.created_at, r.updated_at,
                               (SELECT COUNT(1) FROM capp_routing_step s WHERE s.routing_id = r.id) AS step_count,
                               (SELECT COALESCE(SUM(s.setup_minutes + s.run_minutes), 0)
                                  FROM capp_routing_step s WHERE s.routing_id = r.id) AS total_minutes
                          FROM capp_routing r
                          LEFT JOIN plm_object o ON o.id = r.object_id
                        """ + where + " ORDER BY r.updated_at DESC, r.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new CappDtos.RoutingRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getLong("object_id"), rs.getString("object_code"),
                        rs.getString("object_name"), rs.getInt("version_no"), rs.getString("status"),
                        rs.getString("applicability"), rs.getInt("step_count"),
                        rs.getBigDecimal("total_minutes"),
                        rs.getObject("released_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePage, safeSize);
    }

    public CappDtos.RoutingDetail detail(Long id, Long companyId) {
        List<CappDtos.RoutingDetail> rows = jdbcTemplate.query("""
                        SELECT r.id, r.code, r.name, r.object_id, o.code AS object_code, o.name AS object_name,
                               r.version_no, r.status, r.applicability, r.remark, r.released_at,
                               r.created_at, r.updated_at
                          FROM capp_routing r
                          LEFT JOIN plm_object o ON o.id = r.object_id
                         WHERE r.id = ? AND r.company_id = ? AND r.deleted = false
                        """,
                (rs, rowNum) -> {
                    List<CappDtos.StepRow> steps = stepsOf(id);
                    BigDecimal total = steps.stream()
                            .map(step -> nullToZero(step.setupMinutes()).add(nullToZero(step.runMinutes())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CappDtos.RoutingDetail(rs.getLong("id"), rs.getString("code"),
                            rs.getString("name"), rs.getLong("object_id"), rs.getString("object_code"),
                            rs.getString("object_name"), rs.getInt("version_no"), rs.getString("status"),
                            rs.getString("applicability"), rs.getString("remark"), routeString(steps), total,
                            rs.getObject("released_at", OffsetDateTime.class),
                            rs.getObject("created_at", OffsetDateTime.class),
                            rs.getObject("updated_at", OffsetDateTime.class), steps);
                },
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_ROUTING_NOT_FOUND, "工艺路线不存在：" + id);
        }
        return rows.get(0);
    }

    // ------------------------------------------------------------------ 写

    @Transactional
    public CappDtos.RoutingDetail create(CappDtos.RoutingSaveReq req, Long companyId, Long userId) {
        ObjectRef object = requirePart(req.objectId());
        String code = nextCode("CappRouting", "RT");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_routing (id, company_id, code, name, object_id, object_type,
                                                  version_no, status, applicability, remark,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, 1, 'DRAFT', ?, ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), object.id(), object.objectType(),
                blankToNull(req.applicability()), blankToNull(req.remark()), userId, userId);
        replaceSteps(id, companyId, req.steps());
        return detail(id, companyId);
    }

    @Transactional
    public CappDtos.RoutingDetail update(Long id, CappDtos.RoutingSaveReq req, Long companyId, Long userId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        requireDraft(current.status(), "只有草稿状态的工艺路线可以修改");
        ObjectRef object = requirePart(req.objectId());
        jdbcTemplate.update("""
                        UPDATE capp_routing
                           SET name = ?, object_id = ?, object_type = ?, applicability = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), object.id(), object.objectType(), blankToNull(req.applicability()),
                blankToNull(req.remark()), userId, id, companyId);
        replaceSteps(id, companyId, req.steps());
        return detail(id, companyId);
    }

    /** 发布：DRAFT → RELEASED，同零件的其它已发布路线自动归档。 */
    @Transactional
    public CappDtos.RoutingDetail release(Long id, Long companyId, Long userId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        requireDraft(current.status(), "只有草稿状态的工艺路线可以发布");
        if (current.steps().isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_STEP_INVALID, "工艺路线至少需要一道工序才能发布");
        }
        jdbcTemplate.update("""
                        UPDATE capp_routing
                           SET status = 'ARCHIVED', updated_by = ?, updated_at = now()
                         WHERE company_id = ? AND object_id = ? AND object_type = ?
                           AND status = 'RELEASED' AND deleted = false AND id <> ?
                        """, userId, companyId, current.objectId(), "PdmItem", id);
        jdbcTemplate.update("""
                        UPDATE capp_routing
                           SET status = 'RELEASED', released_at = now(), updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 归档：RELEASED → ARCHIVED（停产/换线时手工收口）。 */
    @Transactional
    public CappDtos.RoutingDetail archive(Long id, Long companyId, Long userId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        if (!"RELEASED".equals(current.status())) {
            throw BizException.of(ErrorCode.CAPP_STATE_INVALID,
                    "只有已发布的工艺路线可以归档（当前状态：" + current.status() + "）");
        }
        jdbcTemplate.update("""
                        UPDATE capp_routing SET status = 'ARCHIVED', updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 复制为新草稿（改工艺留痕：旧路线不动，新路线 version_no + 1 继续改）。 */
    @Transactional
    public CappDtos.RoutingDetail copy(Long id, Long companyId, Long userId) {
        CappDtos.RoutingDetail source = detail(id, companyId);
        Integer maxVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version_no), 0) FROM capp_routing
                 WHERE company_id = ? AND object_id = ? AND deleted = false
                """, Integer.class, companyId, source.objectId());
        long newId = IdWorker.getId();
        String code = nextCode("CappRouting", "RT");
        jdbcTemplate.update("""
                        INSERT INTO capp_routing (id, company_id, code, name, object_id, object_type,
                                                  version_no, status, applicability, remark,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, 'PdmItem', ?, 'DRAFT', ?, ?, ?, ?)
                        """,
                newId, companyId, code, source.name(), source.objectId(),
                (maxVersion == null ? 0 : maxVersion) + 1, source.applicability(), source.remark(),
                userId, userId);
        // 逐行插入（INSERTSELECT 会让所有行共用一个主键，撞 uk(id)）
        for (CappDtos.StepRow step : stepsOf(id)) {
            insertStep(newId, companyId, step);
        }
        return detail(newId, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        requireDraft(current.status(), "只有草稿状态的工艺路线可以删除");
        jdbcTemplate.update("DELETE FROM capp_routing_step WHERE routing_id = ? AND company_id = ?",
                id, companyId);
        jdbcTemplate.update("""
                UPDATE capp_routing SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ------------------------------------------------------------------ 内部

    private void replaceSteps(long routingId, Long companyId, List<CappDtos.StepSaveReq> steps) {
        Set<Integer> seqs = new HashSet<>();
        for (CappDtos.StepSaveReq step : steps) {
            if (!OP_TYPES.contains(normalizeOpType(step.opType()))) {
                throw BizException.of(ErrorCode.CAPP_STEP_INVALID,
                        "未知工序类型：" + step.opType() + "（可选 " + String.join("/", OP_TYPES) + "）");
            }
            if (!seqs.add(step.seqNo())) {
                throw BizException.of(ErrorCode.CAPP_STEP_INVALID, "工序号重复：" + step.seqNo());
            }
            if (nullToZero(step.setupMinutes()).signum() < 0 || nullToZero(step.runMinutes()).signum() < 0) {
                throw BizException.of(ErrorCode.CAPP_STEP_INVALID, "工时不能为负数：" + step.seqNo());
            }
        }
        jdbcTemplate.update("DELETE FROM capp_routing_step WHERE routing_id = ? AND company_id = ?",
                routingId, companyId);
        List<CappDtos.StepSaveReq> ordered = steps.stream()
                .sorted((left, right) -> Integer.compare(left.seqNo(), right.seqNo()))
                .toList();
        for (CappDtos.StepSaveReq step : ordered) {
            insertStep(routingId, companyId, step.seqNo(), step.name().trim(),
                    normalizeOpType(step.opType()), blankToNull(step.workCenter()),
                    step.setupMinutes(), step.runMinutes(), blankToNull(step.remark()));
        }
    }

    /** 插入一道工序：主键逐行生成（批量 INSERT…SELECT 会让所有行共用一个 id）。 */
    private void insertStep(long routingId, Long companyId, int seqNo, String name, String opType,
                            String workCenter, BigDecimal setupMinutes, BigDecimal runMinutes, String remark) {
        jdbcTemplate.update("""
                        INSERT INTO capp_routing_step (id, company_id, routing_id, seq_no, name, op_type,
                                                       work_center, setup_minutes, run_minutes, remark)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), companyId, routingId, seqNo, name, opType, workCenter,
                nullToZero(setupMinutes), nullToZero(runMinutes), remark);
    }

    private void insertStep(long routingId, Long companyId, CappDtos.StepRow step) {
        insertStep(routingId, companyId, step.seqNo(), step.name(), normalizeOpType(step.opType()),
                step.workCenter(), step.setupMinutes(), step.runMinutes(), step.remark());
    }

    private List<CappDtos.StepRow> stepsOf(Long routingId) {
        return jdbcTemplate.query("""
                        SELECT id, seq_no, name, op_type, work_center, setup_minutes, run_minutes, remark
                          FROM capp_routing_step
                         WHERE routing_id = ?
                         ORDER BY seq_no
                        """,
                (rs, rowNum) -> new CappDtos.StepRow(rs.getLong("id"), rs.getInt("seq_no"),
                        rs.getString("name"), rs.getString("op_type"), rs.getString("work_center"),
                        rs.getBigDecimal("setup_minutes"), rs.getBigDecimal("run_minutes"),
                        rs.getString("remark")),
                routingId);
    }

    /** 路线串：{@code 10 下料 → 20 车削 → 30 检验}。 */
    private static String routeString(List<CappDtos.StepRow> steps) {
        return steps.stream()
                .map(step -> step.seqNo() + " " + step.name())
                .collect(Collectors.joining(" → "));
    }

    /** 校验并返回零件（只允许 PdmItem，避免把工艺路线挂到文档上）。 */
    private ObjectRef requirePart(Long objectId) {
        List<ObjectRef> rows = jdbcTemplate.query("""
                        SELECT id, object_type FROM plm_object
                         WHERE id = ? AND deleted = false
                        """,
                (rs, rowNum) -> new ObjectRef(rs.getLong("id"), rs.getString("object_type")), objectId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "零件不存在：" + objectId);
        }
        if (!"PdmItem".equals(rows.get(0).objectType())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "工艺路线只能挂在零件上（当前对象类型：" + rows.get(0).objectType() + "）");
        }
        return rows.get(0);
    }

    private static void requireDraft(String status, String message) {
        if (!"DRAFT".equals(status)) {
            throw BizException.of(ErrorCode.CAPP_STATE_INVALID, message + "（当前状态：" + status + "）");
        }
    }

    private static String normalizeOpType(String opType) {
        return opType == null || opType.isBlank() ? "MACHINING" : opType.trim().toUpperCase();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private record ObjectRef(Long id, String objectType) {
    }
}
