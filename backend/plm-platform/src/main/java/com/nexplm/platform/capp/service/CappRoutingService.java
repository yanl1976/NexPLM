package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappDtos;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final CodeRuleService codeRuleService;

    public CappRoutingService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
    }

    // ------------------------------------------------------------------ 查询

    public PageResult<CappDtos.RoutingRow> list(Long companyId, String keyword, Long objectId,
                                                String status, boolean typical, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        StringBuilder where = new StringBuilder(" WHERE r.deleted = false AND r.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        // 典型工艺库与零件工艺共用这张表，列表默认只看零件工艺，两者互不串台
        where.append(" AND r.typical = ?");
        args.add(typical);
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
                               r.version_no, r.status, r.typical, r.applicability, r.owner_id,
                               u.display_name AS owner_name, r.released_at,
                               r.created_at, r.updated_at,
                               (SELECT COUNT(1) FROM capp_routing_step s WHERE s.routing_id = r.id) AS step_count,
                               (SELECT COALESCE(SUM(s.setup_minutes + s.run_minutes), 0)
                                  FROM capp_routing_step s WHERE s.routing_id = r.id) AS total_minutes
                          FROM capp_routing r
                          LEFT JOIN plm_object o ON o.id = r.object_id
                          LEFT JOIN sys_user u ON u.id = r.owner_id
                        """ + where + " ORDER BY r.updated_at DESC, r.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new CappDtos.RoutingRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), (Long) rs.getObject("object_id"), rs.getString("object_code"),
                        rs.getString("object_name"), rs.getInt("version_no"), rs.getString("status"),
                        rs.getBoolean("typical"), rs.getString("applicability"), (Long) rs.getObject("owner_id"),
                        rs.getString("owner_name"), rs.getInt("step_count"),
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
                               r.version_no, r.status, r.typical, r.applicability, r.remark, r.owner_id,
                               u.display_name AS owner_name, r.released_at,
                               r.created_at, r.updated_at
                          FROM capp_routing r
                          LEFT JOIN plm_object o ON o.id = r.object_id
                          LEFT JOIN sys_user u ON u.id = r.owner_id
                         WHERE r.id = ? AND r.company_id = ? AND r.deleted = false
                        """,
                (rs, rowNum) -> {
                    List<CappDtos.StepRow> steps = stepsOf(id);
                    BigDecimal total = steps.stream()
                            .map(step -> nullToZero(step.setupMinutes()).add(nullToZero(step.runMinutes())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CappDtos.RoutingDetail(rs.getLong("id"), rs.getString("code"),
                            rs.getString("name"), (Long) rs.getObject("object_id"), rs.getString("object_code"),
                            rs.getString("object_name"), rs.getInt("version_no"), rs.getString("status"),
                            rs.getBoolean("typical"),
                            rs.getString("applicability"), rs.getString("remark"),
                            (Long) rs.getObject("owner_id"), rs.getString("owner_name"),
                            routeString(steps), total,
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
        boolean typical = Boolean.TRUE.equals(req.typical());
        // 典型工艺不绑零件；零件路线必须给零件（object_id 在库里已允许为空，靠这里守住语义）
        ObjectRef object = typical ? null : requirePart(requireObjectId(req.objectId()));
        String name = req.name().trim();
        if (typical) {
            requireUniqueTypicalName(name, null, companyId);
        }
        String code = nextCode("CappRouting", "RT");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_routing (id, company_id, code, name, object_id, object_type,
                                                  typical, version_no, status, applicability, remark, owner_id,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'DRAFT', ?, ?, ?, ?, ?)
                        """,
                id, companyId, code, name,
                object == null ? null : object.id(),
                object == null ? "Typical" : object.objectType(), typical,
                blankToNull(req.applicability()), blankToNull(req.remark()),
                req.ownerId() == null ? userId : req.ownerId(), userId, userId);
        replaceSteps(id, companyId, req.steps());
        return detail(id, companyId);
    }

    @Transactional
    public CappDtos.RoutingDetail update(Long id, CappDtos.RoutingSaveReq req, Long companyId, Long userId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        requireDraft(current.status(), "只有草稿状态的工艺路线可以修改");
        if (req.typical() != null && req.typical() != current.typical()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    current.typical() ? "典型工艺不能改成零件路线（请用「套用到零件」）"
                            : "零件路线不能改成典型工艺（请用「另存为典型工艺」）");
        }
        ObjectRef object = current.typical() ? null : requirePart(requireObjectId(req.objectId()));
        String name = req.name().trim();
        if (current.typical()) {
            requireUniqueTypicalName(name, id, companyId);
        }
        jdbcTemplate.update("""
                        UPDATE capp_routing
                           SET name = ?, object_id = ?, object_type = ?, applicability = ?, remark = ?,
                               owner_id = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                name, object == null ? null : object.id(),
                object == null ? "Typical" : object.objectType(),
                blankToNull(req.applicability()),
                blankToNull(req.remark()), req.ownerId() == null ? current.ownerId() : req.ownerId(),
                userId, id, companyId);
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
        // 「一个零件同时只有一条投产路线」只约束零件工艺；典型工艺之间互不顶替（库里可并存多条）
        if (!current.typical()) {
            jdbcTemplate.update("""
                            UPDATE capp_routing
                               SET status = 'ARCHIVED', updated_by = ?, updated_at = now()
                             WHERE company_id = ? AND object_id = ? AND object_type = ?
                               AND typical = false
                               AND status = 'RELEASED' AND deleted = false AND id <> ?
                            """, userId, companyId, current.objectId(), "PdmItem", id);
        }
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
        if (source.typical()) {
            // 典型工艺没有「版本链」概念，副本靠名称区分（同公司典型工艺名唯一）
            return cloneRouting(source, null, "Typical", true,
                    nextCopyName(source.name(), companyId), 1, companyId, userId);
        }
        return cloneRouting(source, source.objectId(), "PdmItem", false, source.name(),
                nextVersionNo(source.objectId(), companyId), companyId, userId);
    }

    /**
     * 另存为典型工艺（P6-7）：把现有零件路线的工序/工步整份拷进典型工艺库。
     *
     * <p>典型工艺不绑零件（{@code object_id} 为空），因此可被任意零件「套用」；
     * 这里只复制结构，不改动源路线，也不占用源路线的版本号。</p>
     */
    @Transactional
    public CappDtos.RoutingDetail saveAsTypical(Long id, CappDtos.TypicalSaveReq req,
                                                Long companyId, Long userId) {
        CappDtos.RoutingDetail source = detail(id, companyId);
        if (source.typical()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "该记录本身就是典型工艺，直接「复制」即可，无需另存为典型工艺");
        }
        String name = req.name().trim();
        requireUniqueTypicalName(name, null, companyId);
        long newId = IdWorker.getId();
        String code = nextCode("CappRouting", "RT");
        jdbcTemplate.update("""
                        INSERT INTO capp_routing (id, company_id, code, name, object_id, object_type,
                                                  typical, version_no, status, applicability, remark, owner_id,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, NULL, 'Typical', true, 1, 'DRAFT', ?, ?, ?, ?, ?)
                        """,
                newId, companyId, code, name,
                firstNonBlank(req.applicability(), source.applicability()),
                firstNonBlank(req.remark(), source.remark()),
                source.ownerId(), userId, userId);
        copyStepsInto(source, newId, companyId);
        return detail(newId, companyId);
    }

    /**
     * 套用典型工艺到零件（P6-7）：生成该零件的一条新草稿路线。
     *
     * <p>典型工艺本身不消耗、不修改；新路线 {@code version_no} 接着该零件现有最大值 +1，
     * 与「复制路线」的版本口径一致，套用后照常走「修改 → 发布」流程。</p>
     */
    @Transactional
    public CappDtos.RoutingDetail applyToPart(Long typicalId, CappDtos.TypicalApplyReq req,
                                              Long companyId, Long userId) {
        CappDtos.RoutingDetail typical = detail(typicalId, companyId);
        if (!typical.typical()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "只有典型工艺可以套用到零件；零件路线请用「复制」");
        }
        ObjectRef object = requirePart(requireObjectId(req.objectId()));
        String name = req.name() == null || req.name().isBlank() ? typical.name() : req.name().trim();
        return cloneRouting(typical, object.id(), object.objectType(), false, name,
                nextVersionNo(object.id(), companyId), companyId, userId);
    }

    /** 深拷贝一条路线（工序 + 工步）到目标对象/类型，返回新路线详情。 */
    private CappDtos.RoutingDetail cloneRouting(CappDtos.RoutingDetail source, Long objectId,
                                                String objectType, boolean typical, String name,
                                                int versionNo, Long companyId, Long userId) {
        long newId = IdWorker.getId();
        String code = nextCode("CappRouting", "RT");
        jdbcTemplate.update("""
                        INSERT INTO capp_routing (id, company_id, code, name, object_id, object_type,
                                                  typical, version_no, status, applicability, remark, owner_id,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?)
                        """,
                newId, companyId, code, name, objectId, objectType, typical, versionNo,
                source.applicability(), source.remark(), source.ownerId(), userId, userId);
        copyStepsInto(source, newId, companyId);
        return detail(newId, companyId);
    }

    /** 逐行插入工序与工步（批量 INSERT…SELECT 会让所有行共用一个主键，撞 uk(id)）。 */
    private void copyStepsInto(CappDtos.RoutingDetail source, long targetId, Long companyId) {
        for (CappDtos.StepRow step : source.steps()) {
            insertStep(targetId, companyId, step);
            writeSubstepsFromRows(targetId, companyId, step.seqNo(), step.substeps());
        }
    }

    private int nextVersionNo(Long objectId, Long companyId) {
        Integer maxVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version_no), 0) FROM capp_routing
                 WHERE company_id = ? AND object_id = ? AND typical = false AND deleted = false
                """, Integer.class, companyId, objectId);
        return (maxVersion == null ? 0 : maxVersion) + 1;
    }

    /** 把已读出的工步行按「目标工序号」重写到某条路线（复制、序号重建共用）。 */
    private void writeSubstepsFromRows(long routingId, Long companyId, int targetStepSeqNo,
                                       List<CappDtos.SubstepRow> substeps) {
        if (substeps == null || substeps.isEmpty()) {
            return;
        }
        for (CappDtos.SubstepRow substep : substeps) {
            jdbcTemplate.update("""
                            INSERT INTO capp_routing_substep (id, company_id, routing_id, step_seq_no,
                                                              seq_no, name, content, resource_id,
                                                              setup_minutes, run_minutes, remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, routingId, targetStepSeqNo, substep.seqNo(),
                    substep.name(), substep.content(), substep.resourceId(),
                    nullToZero(substep.setupMinutes()), nullToZero(substep.runMinutes()),
                    substep.remark());
        }
    }

    /**
     * 序号重建（P6-2）：按当前顺序把工序号重排为 10/20/30…（工艺号插队后一键整理）。
     *
     * <p>不做 UPDATE 逐行改号——{@code uk_capp_step_seq(routing_id, seq_no)} 会在「5→10、7→20」
     * 这类互换时中途撞唯一键；这里按读出的顺序整表重插，既避开唯一键冲突，也顺手清掉历史脏号。</p>
     */
    @Transactional
    public CappDtos.RoutingDetail resequence(Long id, Long companyId, Long userId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        requireDraft(current.status(), "只有草稿状态的工艺路线可以重建序号");
        List<CappDtos.StepRow> steps = current.steps();
        jdbcTemplate.update("DELETE FROM capp_routing_step WHERE routing_id = ? AND company_id = ?",
                id, companyId);
        // 工步挂的是工序号（语义键），重排后必须跟着迁移：先整表记下，插入工序后再按映射重写
        jdbcTemplate.update("DELETE FROM capp_routing_substep WHERE routing_id = ? AND company_id = ?",
                id, companyId);
        int seqNo = 10;
        for (CappDtos.StepRow step : steps) {
            insertStep(id, companyId, seqNo, step.name(), normalizeOpType(step.opType()),
                    step.workCenter(), step.resourceId(), step.setupMinutes(), step.runMinutes(), step.remark());
            writeSubstepsFromRows(id, companyId, seqNo, step.substeps());
            seqNo += 10;
        }
        jdbcTemplate.update("""
                UPDATE capp_routing SET updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        CappDtos.RoutingDetail current = detail(id, companyId);
        requireDraft(current.status(), "只有草稿状态的工艺路线可以删除");
        jdbcTemplate.update("DELETE FROM capp_routing_substep WHERE routing_id = ? AND company_id = ?",
                id, companyId);
        jdbcTemplate.update("DELETE FROM capp_routing_step WHERE routing_id = ? AND company_id = ?",
                id, companyId);
        jdbcTemplate.update("""
                UPDATE capp_routing SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /**
     * 工艺过程树：工序 → 工步。
     *
     * <p>只读视图，数据源就是 {@link #detail}（不额外建表/不缓存），保证「树」与「工序表」
     * 永远同源；{@code substepMinutes} 是工步工时之和，用于核对工步拆分是否与工序工时对得上。</p>
     */
    public List<CappDtos.ProcessTreeNode> processTree(Long id, Long companyId) {
        CappDtos.RoutingDetail routing = detail(id, companyId);
        return routing.steps().stream()
                .map(step -> {
                    List<CappDtos.SubstepRow> substeps = step.substeps() == null
                            ? List.of() : step.substeps();
                    BigDecimal substepMinutes = substeps.stream()
                            .map(item -> nullToZero(item.setupMinutes())
                                    .add(nullToZero(item.runMinutes())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CappDtos.ProcessTreeNode(step.seqNo(), step.name(), step.opType(),
                            step.workCenter(), step.resourceId(), step.resourceCode(),
                            step.resourceName(), step.setupMinutes(), step.runMinutes(),
                            substeps.size(), substepMinutes, substeps);
                })
                .toList();
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
        // 工步也随工序整表替换：先清空，再按提交内容重写（避免工序号变化后残留孤儿工步）
        jdbcTemplate.update("DELETE FROM capp_routing_substep WHERE routing_id = ? AND company_id = ?",
                routingId, companyId);
        List<CappDtos.StepSaveReq> ordered = steps.stream()
                .sorted((left, right) -> Integer.compare(left.seqNo(), right.seqNo()))
                .toList();
        for (CappDtos.StepSaveReq step : ordered) {
            if (step.resourceId() != null) {
                requireResource(step.resourceId(), companyId);
            }
            insertStep(routingId, companyId, step.seqNo(), step.name().trim(),
                    normalizeOpType(step.opType()), blankToNull(step.workCenter()), step.resourceId(),
                    step.setupMinutes(), step.runMinutes(), blankToNull(step.remark()));
            writeSubsteps(routingId, companyId, step.seqNo(), step.substeps());
        }
    }

    /** 写入一道工序的工步（同一工序内整表替换；工序号与工步号都不允许重复）。 */
    private void writeSubsteps(long routingId, Long companyId, int stepSeqNo,
                               List<CappDtos.SubstepSaveReq> substeps) {
        if (substeps == null || substeps.isEmpty()) {
            return;
        }
        Set<Integer> seqs = new HashSet<>();
        for (CappDtos.SubstepSaveReq substep : substeps) {
            if (!seqs.add(substep.seqNo())) {
                throw BizException.of(ErrorCode.CAPP_STEP_INVALID,
                        "工序 " + stepSeqNo + " 的工步号重复：" + substep.seqNo());
            }
            if (nullToZero(substep.setupMinutes()).signum() < 0
                    || nullToZero(substep.runMinutes()).signum() < 0) {
                throw BizException.of(ErrorCode.CAPP_STEP_INVALID,
                        "工步工时不能为负数：工序 " + stepSeqNo + " 工步 " + substep.seqNo());
            }
            if (substep.resourceId() != null) {
                requireResource(substep.resourceId(), companyId);
            }
        }
        List<CappDtos.SubstepSaveReq> ordered = substeps.stream()
                .sorted((left, right) -> Integer.compare(left.seqNo(), right.seqNo()))
                .toList();
        for (CappDtos.SubstepSaveReq substep : ordered) {
            jdbcTemplate.update("""
                            INSERT INTO capp_routing_substep (id, company_id, routing_id, step_seq_no,
                                                              seq_no, name, content, resource_id,
                                                              setup_minutes, run_minutes, remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, routingId, stepSeqNo, substep.seqNo(),
                    substep.name().trim(), blankToNull(substep.content()), substep.resourceId(),
                    nullToZero(substep.setupMinutes()), nullToZero(substep.runMinutes()),
                    blankToNull(substep.remark()));
        }
    }

    /** 插入一道工序：主键逐行生成（批量 INSERT…SELECT 会让所有行共用一个 id）。 */
    private void insertStep(long routingId, Long companyId, int seqNo, String name, String opType,
                            String workCenter, Long resourceId, BigDecimal setupMinutes,
                            BigDecimal runMinutes, String remark) {
        jdbcTemplate.update("""
                        INSERT INTO capp_routing_step (id, company_id, routing_id, seq_no, name, op_type,
                                                       work_center, resource_id, setup_minutes, run_minutes, remark)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), companyId, routingId, seqNo, name, opType, workCenter, resourceId,
                nullToZero(setupMinutes), nullToZero(runMinutes), remark);
    }

    private void insertStep(long routingId, Long companyId, CappDtos.StepRow step) {
        insertStep(routingId, companyId, step.seqNo(), step.name(), normalizeOpType(step.opType()),
                step.workCenter(), step.resourceId(), step.setupMinutes(), step.runMinutes(), step.remark());
    }

    private List<CappDtos.StepRow> stepsOf(Long routingId) {
        Map<Integer, List<CappDtos.SubstepRow>> substeps = substepsByStep(routingId);
        return jdbcTemplate.query("""
                        SELECT s.id, s.seq_no, s.name, s.op_type, s.work_center, s.resource_id,
                               r.code AS resource_code, r.name AS resource_name,
                               s.setup_minutes, s.run_minutes, s.remark
                          FROM capp_routing_step s
                          LEFT JOIN capp_resource r ON r.id = s.resource_id AND r.deleted = false
                         WHERE s.routing_id = ?
                         ORDER BY s.seq_no
                        """,
                (rs, rowNum) -> {
                    int seqNo = rs.getInt("seq_no");
                    return new CappDtos.StepRow(rs.getLong("id"), seqNo,
                            rs.getString("name"), rs.getString("op_type"), rs.getString("work_center"),
                            (Long) rs.getObject("resource_id"), rs.getString("resource_code"),
                            rs.getString("resource_name"),
                            rs.getBigDecimal("setup_minutes"), rs.getBigDecimal("run_minutes"),
                            rs.getString("remark"), substeps.getOrDefault(seqNo, List.of()));
                },
                routingId);
    }

    /** 一次查出整条路线的工步并按工序号分组（避免每道工序各查一次）。 */
    private Map<Integer, List<CappDtos.SubstepRow>> substepsByStep(Long routingId) {
        Map<Integer, List<CappDtos.SubstepRow>> grouped = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT s.step_seq_no, s.id, s.seq_no, s.name, s.content, s.resource_id,
                               r.code AS resource_code, r.name AS resource_name,
                               s.setup_minutes, s.run_minutes, s.remark
                          FROM capp_routing_substep s
                          LEFT JOIN capp_resource r ON r.id = s.resource_id AND r.deleted = false
                         WHERE s.routing_id = ?
                         ORDER BY s.step_seq_no, s.seq_no
                        """,
                rs -> {
                    int stepSeqNo = rs.getInt("step_seq_no");
                    grouped.computeIfAbsent(stepSeqNo, key -> new ArrayList<>())
                            .add(new CappDtos.SubstepRow(rs.getLong("id"), rs.getInt("seq_no"),
                                    rs.getString("name"), rs.getString("content"),
                                    (Long) rs.getObject("resource_id"), rs.getString("resource_code"),
                                    rs.getString("resource_name"),
                                    rs.getBigDecimal("setup_minutes"), rs.getBigDecimal("run_minutes"),
                                    rs.getString("remark")));
                },
                routingId);
        return grouped;
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

    /** 零件路线必须显式给零件：object_id 在库里允许为空（典型工艺），语义由这里守住。 */
    private static Long requireObjectId(Long objectId) {
        if (objectId == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "零件工艺路线必须指定零件（objectId）");
        }
        return objectId;
    }

    /** 典型工艺同公司内名称唯一（DB 有部分唯一索引兜底，这里先查一次给出可读文案）。 */
    private void requireUniqueTypicalName(String name, Long excludeId, Long companyId) {
        if (typicalNameExists(name, excludeId, companyId)) {
            throw BizException.of(ErrorCode.CAPP_ROUTING_DUPLICATE, "典型工艺名称已存在：" + name);
        }
    }

    private boolean typicalNameExists(String name, Long excludeId, Long companyId) {
        List<Long> ids = jdbcTemplate.query("""
                        SELECT id FROM capp_routing
                         WHERE company_id = ? AND typical = true AND deleted = false AND name = ?
                        """,
                (rs, rowNum) -> rs.getLong(1), companyId, name);
        return ids.stream().anyMatch(found -> excludeId == null || !found.equals(excludeId));
    }

    /** 典型工艺复制时的默认命名：{@code 车削典型工艺-副本}、{@code -副本2}…（避开唯一索引）。 */
    private String nextCopyName(String baseName, Long companyId) {
        String candidate = baseName + "-副本";
        int index = 2;
        while (typicalNameExists(candidate, null, companyId)) {
            candidate = baseName + "-副本" + index;
            index++;
        }
        return candidate;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? blankToNull(fallback) : primary.trim();
    }

    private void requireResource(Long resourceId, Long companyId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_resource
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, Integer.class, resourceId, companyId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "工艺资源不存在：" + resourceId);
        }
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

    /** 统一由编码规则引擎取号（批次 B2）：格式来自 code_rule.segments_json。 */
    private String nextCode(String ruleCode, String prefix) {
        return codeRuleService.next(ruleCode);
    }

    private record ObjectRef(Long id, String objectType) {
    }
}
