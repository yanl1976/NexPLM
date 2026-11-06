package com.nexplm.platform.change.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.change.api.ChgDtos;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 变更管理（P5）：ECR 主档与关联对象。
 *
 * <p>本阶段实现申请侧闭环（草稿 → 提交 → 评审），关联对象区分
 * 受影响（AFFECTED，需执行）/ 影响（IMPACT，待评估）/ 参考（REFERENCE，仅上下文）。
 * 变更执行（批量升版、置废弃）与 ECN/Problem 由后续迭代接入，
 * 表结构已在 V18 中预留（{@code chg_ecn} / {@code chg_problem}）。</p>
 */
@Service
public class ChgService {

    private static final Set<String> CHANGE_TYPES = Set.of("DESIGN", "PROCESS");
    private static final Set<String> PRIORITIES = Set.of("HIGH", "NORMAL", "LOW");
    private static final Set<String> RELATIONS = Set.of("AFFECTED", "IMPACT", "REFERENCE");
    private static final Set<String> ACTIONS = Set.of("REVISE", "OBSOLETE", "RELEASE");

    /** 仅草稿可改主档字段。 */
    private static final Set<String> MAIN_EDITABLE = Set.of("DRAFT");
    /** 提交前的状态都允许调整受影响对象清单。 */
    private static final Set<String> OBJECT_EDITABLE = Set.of("DRAFT", "SUBMITTED", "REVIEWING");
    /** 可关闭的状态。 */
    private static final Set<String> CLOSABLE = Set.of("APPROVED", "NOTICED", "EXECUTING");

    private final JdbcTemplate jdbcTemplate;

    public ChgService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------------ 查询

    public PageResult<ChgDtos.EcrRow> list(Long companyId, String keyword, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        StringBuilder where = new StringBuilder(" WHERE e.deleted = false AND e.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (status != null && !status.isBlank()) {
            where.append(" AND e.status = ?");
            args.add(status.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (e.code ILIKE ? OR e.title ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM chg_ecr e" + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<ChgDtos.EcrRow> items = jdbcTemplate.query("""
                        SELECT e.id, e.code, e.title, e.change_type, e.category, e.priority, e.status,
                               e.requester_id, u.display_name AS requester_name, e.receiver_id,
                               e.project_no, e.target_date, e.created_at,
                               (SELECT COUNT(1) FROM chg_object o
                                 WHERE o.ecr_id = e.id AND o.relation = 'AFFECTED') AS affected_count
                          FROM chg_ecr e
                          LEFT JOIN sys_user u ON u.id = e.requester_id
                        """ + where + " ORDER BY e.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ChgDtos.EcrRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("title"), rs.getString("change_type"), rs.getString("category"),
                        rs.getString("priority"), rs.getString("status"), rs.getLong("requester_id"),
                        rs.getString("requester_name"), (Long) rs.getObject("receiver_id"),
                        rs.getString("project_no"), rs.getObject("target_date", LocalDate.class),
                        rs.getObject("created_at", OffsetDateTime.class), rs.getInt("affected_count")),
                pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePage, safeSize);
    }

    public ChgDtos.EcrDetail detail(Long id, Long companyId) {
        List<ChgDtos.EcrDetail> rows = jdbcTemplate.query("""
                        SELECT e.*, ru.display_name AS requester_name, cu.display_name AS receiver_name
                          FROM chg_ecr e
                          LEFT JOIN sys_user ru ON ru.id = e.requester_id
                          LEFT JOIN sys_user cu ON cu.id = e.receiver_id
                         WHERE e.id = ? AND e.deleted = false AND e.company_id = ?
                        """,
                (rs, rowNum) -> new ChgDtos.EcrDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getString("title"), rs.getString("change_type"), rs.getString("category"),
                        rs.getString("reason"), rs.getString("feasibility"), rs.getString("priority"),
                        rs.getString("status"), rs.getLong("requester_id"), rs.getString("requester_name"),
                        (Long) rs.getObject("receiver_id"), rs.getString("receiver_name"),
                        rs.getString("project_no"), rs.getObject("target_date", LocalDate.class),
                        rs.getString("remark"),
                        rs.getObject("approved_at", OffsetDateTime.class),
                        rs.getObject("closed_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class),
                        List.of(), List.of()),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CHG_ECR_NOT_FOUND, "变更申请不存在：" + id);
        }
        ChgDtos.EcrDetail base = rows.get(0);
        return new ChgDtos.EcrDetail(base.id(), base.code(), base.title(), base.changeType(), base.category(),
                base.reason(), base.feasibility(), base.priority(), base.status(), base.requesterId(),
                base.requesterName(), base.receiverId(), base.receiverName(), base.projectNo(),
                base.targetDate(), base.remark(), base.approvedAt(), base.closedAt(),
                base.createdAt(), base.updatedAt(), objects(id, companyId), ecns(id, companyId));
    }

    private List<ChgDtos.ObjectRow> objects(Long ecrId, Long companyId) {
        return jdbcTemplate.query("""
                        SELECT o.id, o.object_type, o.object_id, o.relation, o.action,
                               o.revision_from, o.revision_to, o.exec_status, o.remark,
                               p.code AS object_code, p.name AS object_name
                          FROM chg_object o
                          LEFT JOIN plm_object p ON p.id = o.object_id
                         WHERE o.ecr_id = ? AND o.company_id = ?
                         ORDER BY o.relation, o.id
                        """,
                (rs, rowNum) -> new ChgDtos.ObjectRow(rs.getLong("id"), rs.getString("object_type"),
                        rs.getLong("object_id"), rs.getString("object_code"), rs.getString("object_name"),
                        rs.getString("relation"), rs.getString("action"),
                        (Integer) rs.getObject("revision_from"), (Integer) rs.getObject("revision_to"),
                        rs.getString("exec_status"), rs.getString("remark")),
                ecrId, companyId);
    }

    private List<ChgDtos.EcnRow> ecns(Long ecrId, Long companyId) {
        return jdbcTemplate.query("""
                        SELECT n.id, n.code, n.ecr_id, n.title, n.status, n.executor_id,
                               u.display_name AS executor_name, n.plan_date, n.issued_at, n.closed_at
                          FROM chg_ecn n
                          LEFT JOIN sys_user u ON u.id = n.executor_id
                         WHERE n.ecr_id = ? AND n.deleted = false AND n.company_id = ?
                         ORDER BY n.id
                        """,
                (rs, rowNum) -> new ChgDtos.EcnRow(rs.getLong("id"), rs.getString("code"),
                        rs.getLong("ecr_id"), rs.getString("title"), rs.getString("status"),
                        (Long) rs.getObject("executor_id"), rs.getString("executor_name"),
                        rs.getObject("plan_date", LocalDate.class),
                        rs.getObject("issued_at", OffsetDateTime.class),
                        rs.getObject("closed_at", OffsetDateTime.class)),
                ecrId, companyId);
    }

    // ------------------------------------------------------------------ 主档

    @Transactional
    public ChgDtos.EcrDetail create(ChgDtos.EcrSaveReq req, Long companyId, Long userId) {
        String changeType = requireIn(req.changeType(), CHANGE_TYPES, "变更类型");
        String priority = req.priority() == null || req.priority().isBlank() ? "NORMAL" : req.priority().trim();
        requireIn(priority, PRIORITIES, "优先级");
        long id = IdWorker.getId();
        String code = nextCode("ChgEcr", "ECR");
        jdbcTemplate.update("""
                        INSERT INTO chg_ecr (id, company_id, code, title, change_type, category, reason,
                                             feasibility, priority, status, requester_id, receiver_id,
                                             project_no, target_date, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, code, req.title().trim(), changeType, blankToNull(req.category()),
                blankToNull(req.reason()), blankToNull(req.feasibility()), priority, userId,
                req.receiverId(), blankToNull(req.projectNo()), req.targetDate(),
                blankToNull(req.remark()), userId, userId);
        return detail(id, companyId);
    }

    @Transactional
    public ChgDtos.EcrDetail update(Long id, ChgDtos.EcrSaveReq req, Long companyId, Long userId) {
        ChgDtos.EcrDetail current = detail(id, companyId);
        requireEditable(current.status(), MAIN_EDITABLE, "只有草稿状态可以修改变更申请");
        String changeType = requireIn(req.changeType(), CHANGE_TYPES, "变更类型");
        String priority = req.priority() == null || req.priority().isBlank()
                ? current.priority() : requireIn(req.priority().trim(), PRIORITIES, "优先级");
        jdbcTemplate.update("""
                        UPDATE chg_ecr
                           SET title = ?, change_type = ?, category = ?, reason = ?, feasibility = ?,
                               priority = ?, receiver_id = ?, project_no = ?, target_date = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.title().trim(), changeType, blankToNull(req.category()), blankToNull(req.reason()),
                blankToNull(req.feasibility()), priority, req.receiverId(), blankToNull(req.projectNo()),
                req.targetDate(), blankToNull(req.remark()), userId, id, companyId);
        return detail(id, companyId);
    }

    /** 提交评审：必须至少有一个「受影响」对象，否则变更没有执行内容。 */
    @Transactional
    public ChgDtos.EcrDetail submit(Long id, Long companyId, Long userId) {
        ChgDtos.EcrDetail current = detail(id, companyId);
        requireEditable(current.status(), MAIN_EDITABLE, "只有草稿状态可以提交");
        int affected = countObjects(id, "AFFECTED");
        if (affected == 0) {
            throw BizException.of(ErrorCode.CHG_OBJECT_INVALID, "请先添加至少一个「受影响」对象再提交");
        }
        jdbcTemplate.update("""
                        UPDATE chg_ecr SET status = 'SUBMITTED', updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 关闭变更（执行完成后收口）。 */
    @Transactional
    public ChgDtos.EcrDetail close(Long id, Long companyId, Long userId) {
        ChgDtos.EcrDetail current = detail(id, companyId);
        requireEditable(current.status(), CLOSABLE, "当前状态不允许关闭");
        jdbcTemplate.update("""
                        UPDATE chg_ecr SET status = 'CLOSED', closed_at = now(), updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    // ------------------------------------------------------------------ 关联对象

    @Transactional
    public ChgDtos.ObjectRow addObject(Long ecrId, ChgDtos.ObjectAddReq req, Long companyId, Long userId) {
        ChgDtos.EcrDetail current = detail(ecrId, companyId);
        requireEditable(current.status(), OBJECT_EDITABLE, "当前状态不允许调整关联对象");

        String relation = req.relation() == null || req.relation().isBlank()
                ? "AFFECTED" : requireIn(req.relation().trim(), RELATIONS, "关联关系");
        String action = blankToNull(req.action());
        if ("AFFECTED".equals(relation)) {
            if (action == null) {
                throw BizException.of(ErrorCode.CHG_OBJECT_INVALID,
                        "受影响对象必须指定操作意图（REVISE / OBSOLETE / RELEASE）");
            }
            requireIn(action, ACTIONS, "操作意图");
        } else if (action != null) {
            throw BizException.of(ErrorCode.CHG_OBJECT_INVALID, "只有受影响对象可以指定操作意图");
        }

        Integer revision = jdbcTemplate.query(
                "SELECT revision FROM plm_object WHERE id = ? AND deleted = false AND company_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null, req.objectId(), companyId);
        if (revision == null) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "对象不存在：" + req.objectId());
        }

        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO chg_object (id, company_id, ecr_id, object_type, object_id, relation,
                                                    action, revision_from, exec_status, remark, created_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                            """,
                    id, companyId, ecrId, req.objectType().trim(), req.objectId(), relation, action,
                    revision, blankToNull(req.remark()), userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该对象已在本变更的同一关系中");
        }
        return objects(ecrId, companyId).stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.CHG_OBJECT_INVALID, "关联对象写入失败"));
    }

    @Transactional
    public void removeObject(Long ecrId, Long linkId, Long companyId) {
        ChgDtos.EcrDetail current = detail(ecrId, companyId);
        requireEditable(current.status(), OBJECT_EDITABLE, "当前状态不允许调整关联对象");
        int deleted = jdbcTemplate.update(
                "DELETE FROM chg_object WHERE id = ? AND ecr_id = ? AND company_id = ?",
                linkId, ecrId, companyId);
        if (deleted == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "关联对象不存在：" + linkId);
        }
    }

    // ------------------------------------------------------------------ 工具

    private int countObjects(Long ecrId, String relation) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chg_object WHERE ecr_id = ? AND relation = ?",
                Integer.class, ecrId, relation);
        return count == null ? 0 : count;
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

    private static void requireEditable(String status, Set<String> allowed, String message) {
        if (!allowed.contains(status)) {
            throw BizException.of(ErrorCode.CHG_STATE_INVALID, message + "（当前状态：" + status + "）");
        }
    }

    private static String requireIn(String value, Set<String> allowed, String label) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || !allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    label + "取值不合法：" + value + "，允许值 " + allowed);
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
