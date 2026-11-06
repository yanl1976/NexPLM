package com.nexplm.platform.quality.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.quality.api.QualityIssueDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 质量问题闭环（P8-1）：登记 → 分派 → 多轮整改 → 验证关闭。
 *
 * <p>状态机只走动作，保存接口没有 status：</p>
 * <pre>
 *   OPEN ─assign─▶ ASSIGNED ─submit─▶ VERIFYING ─pass─▶ CLOSED
 *                                        └─fail─▶ IN_PROGRESS（轮次 +1）─submit─▶ VERIFYING …
 * </pre>
 *
 * <p>几次刻意取舍：</p>
 * <ul>
 *   <li><b>验证不通过 = 开新一轮</b>而非打回同一条措施：第 1 轮分析与第 2 轮补充分析
 *       要能分开看、分开追责，所以措施带 {@code round_no}；</li>
 *   <li><b>提交验证要求当前轮措施全部完成</b>：还有未做完的措施就送审，验证人只能干等；</li>
 *   <li><b>验证阶段冻结措施</b>：验证中还能改措施等于边审边改，结论不可信；要改先打回；</li>
 *   <li><b>登记即可分派</b>：现场登记时往往当场就知道责任人，允许 create 直接带 owner
 *       并落到 ASSIGNED，避免"先建再点一次分派"。</li>
 * </ul>
 */
@Service
public class QualityIssueService {

    private static final Set<String> STATUSES =
            Set.of("OPEN", "ASSIGNED", "IN_PROGRESS", "VERIFYING", "CLOSED");
    private static final Set<String> SOURCES = Set.of("INTERNAL", "CUSTOMER", "SUPPLIER", "AUDIT");
    private static final Set<String> SEVERITIES = Set.of("MINOR", "MAJOR", "CRITICAL");
    private static final Set<String> ACTION_TYPES =
            Set.of("CONTAINMENT", "ROOT_CAUSE", "CORRECTIVE", "PREVENTIVE", "VERIFY");
    private static final String BASE_SELECT = """
            SELECT i.*, u.display_name AS owner_name, r.display_name AS reporter_name,
                   (SELECT COUNT(1) FROM qlt_issue_action a
                     WHERE a.issue_id = i.id AND a.deleted = false) AS action_count,
                   (SELECT COUNT(1) FROM qlt_issue_action a
                     WHERE a.issue_id = i.id AND a.deleted = false AND a.status = 'PENDING')
                       AS open_action_count
              FROM qlt_issue i
              LEFT JOIN sys_user u ON u.id = i.owner_id
              LEFT JOIN sys_user r ON r.id = i.reported_by
            """;

    private final JdbcTemplate jdbcTemplate;

    public QualityIssueService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- 查询

    public PageResult<QualityIssueDtos.IssueRow> page(Long companyId, String keyword, String status,
                                                      String severity, String source, Long ownerId,
                                                      Boolean overdue, Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE i.company_id = ? AND i.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (i.code ILIKE ? OR i.title ILIKE ? OR i.object_code ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND i.status = ?");
            args.add(normalize(status, STATUSES, "状态"));
        }
        if (severity != null && !severity.isBlank()) {
            where.append(" AND i.severity = ?");
            args.add(normalize(severity, SEVERITIES, "严重度"));
        }
        if (source != null && !source.isBlank()) {
            where.append(" AND i.source = ?");
            args.add(normalize(source, SOURCES, "来源"));
        }
        if (ownerId != null) {
            where.append(" AND i.owner_id = ?");
            args.add(ownerId);
        }
        if (Boolean.TRUE.equals(overdue)) {
            where.append(" AND i.status <> 'CLOSED' AND i.due_date IS NOT NULL"
                    + " AND i.due_date < CURRENT_DATE");
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM qlt_issue i" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<QualityIssueDtos.IssueRow> items = jdbcTemplate.query(
                BASE_SELECT + where + " ORDER BY i.status = 'CLOSED', i.due_date NULLS LAST, i.id DESC"
                        + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public QualityIssueDtos.IssueDetail detail(Long id, Long companyId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        List<QualityIssueDtos.ActionRow> actions = jdbcTemplate.query("""
                        SELECT a.*, u.display_name AS owner_name
                          FROM qlt_issue_action a
                          LEFT JOIN sys_user u ON u.id = a.owner_id
                         WHERE a.issue_id = ? AND a.deleted = false
                         ORDER BY a.round_no, a.created_at, a.id
                        """,
                (rs, rowNum) -> new QualityIssueDtos.ActionRow(rs.getLong("id"),
                        rs.getInt("round_no"), rs.getString("action_type"), rs.getString("content"),
                        (Long) rs.getObject("owner_id"), rs.getString("owner_name"),
                        rs.getObject("due_date", LocalDate.class), rs.getString("status"),
                        rs.getObject("done_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class)),
                id);
        return new QualityIssueDtos.IssueDetail(issue, actions);
    }

    // ---------------------------------------------------------------- 写

    @Transactional
    public QualityIssueDtos.IssueDetail create(QualityIssueDtos.IssueSaveReq req, Long companyId,
                                               Long userId) {
        if (req.ownerId() != null) {
            requireUser(req.ownerId());
        }
        long id = IdWorker.getId();
        String code = nextCode();
        jdbcTemplate.update("""
                        INSERT INTO qlt_issue (id, company_id, code, title, description, source,
                                               severity, status, round_no, object_type, object_id,
                                               object_code, project_id, supplier_id, owner_id,
                                               reported_by, due_date, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, code, req.title().trim(), blankToNull(req.description()),
                normalize(req.source() == null ? "INTERNAL" : req.source(), SOURCES, "来源"),
                normalize(req.severity() == null ? "MAJOR" : req.severity(), SEVERITIES, "严重度"),
                req.ownerId() == null ? "OPEN" : "ASSIGNED",
                blankToNull(req.objectType()), req.objectId(), blankToNull(req.objectCode()),
                req.projectId(), req.supplierId(), req.ownerId(), userId, req.dueDate(),
                blankToNull(req.remark()), userId, userId);
        return detail(id, companyId);
    }

    /** 修改基础信息：已关闭不可改；责任人改动不算"分派动作"，状态不回退。 */
    @Transactional
    public QualityIssueDtos.IssueDetail update(Long id, QualityIssueDtos.IssueSaveReq req,
                                               Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        requireOpen(issue, "已关闭的质量问题不能再修改");
        if (req.ownerId() != null) {
            requireUser(req.ownerId());
        }
        jdbcTemplate.update("""
                        UPDATE qlt_issue
                           SET title = ?, description = ?, source = ?, severity = ?, owner_id = ?,
                               due_date = ?, object_type = ?, object_id = ?, object_code = ?,
                               project_id = ?, supplier_id = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.title().trim(), blankToNull(req.description()),
                normalize(req.source() == null ? issue.source() : req.source(), SOURCES, "来源"),
                normalize(req.severity() == null ? issue.severity() : req.severity(), SEVERITIES,
                        "严重度"),
                req.ownerId() == null ? issue.ownerId() : req.ownerId(), req.dueDate(),
                blankToNull(req.objectType()), req.objectId(), blankToNull(req.objectCode()),
                req.projectId(), req.supplierId(), blankToNull(req.remark()), userId, id, companyId);
        return detail(id, companyId);
    }

    /** 分派责任人：登记后到提交验证前都能重新分派（换人不换状态机）。 */
    @Transactional
    public QualityIssueDtos.IssueDetail assign(Long id, QualityIssueDtos.IssueAssignReq req,
                                               Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        requireOpen(issue, "已关闭的质量问题不能再分派");
        if ("VERIFYING".equals(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, "验证中的质量问题不能再改责任人");
        }
        requireUser(req.ownerId());
        jdbcTemplate.update("""
                        UPDATE qlt_issue
                           SET owner_id = ?, due_date = COALESCE(?, due_date),
                               status = CASE WHEN status = 'OPEN' THEN 'ASSIGNED' ELSE status END,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.ownerId(), req.dueDate(), userId, id, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public QualityIssueDtos.IssueDetail addAction(Long id, QualityIssueDtos.ActionSaveReq req,
                                                  Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        requireOpen(issue, "已关闭的质量问题不能再添加措施");
        if ("VERIFYING".equals(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID,
                    "验证中的质量问题不能再改措施，如确需补充请先由验证人打回");
        }
        jdbcTemplate.update("""
                        INSERT INTO qlt_issue_action (id, company_id, issue_id, round_no, action_type,
                                                      content, owner_id, due_date, status,
                                                      created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                        """,
                IdWorker.getId(), companyId, id, issue.roundNo(),
                normalize(req.actionType() == null ? "CORRECTIVE" : req.actionType(), ACTION_TYPES,
                        "措施类型"),
                req.content().trim(), req.ownerId() == null ? issue.ownerId() : req.ownerId(),
                req.dueDate(), userId, userId);
        // 有措施就进入整改中（ASSIGNED 只是"派了人"，IN_PROGRESS 才是"在改"）
        if (!"IN_PROGRESS".equals(issue.status())) {
            jdbcTemplate.update("""
                    UPDATE qlt_issue SET status = 'IN_PROGRESS', updated_by = ?, updated_at = now()
                     WHERE id = ? AND company_id = ?
                    """, userId, id, companyId);
        }
        return detail(id, companyId);
    }

    @Transactional
    public QualityIssueDtos.IssueDetail updateAction(Long id, Long actionId,
                                                     QualityIssueDtos.ActionSaveReq req,
                                                     Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        requireOpen(issue, "已关闭的质量问题不能再改措施");
        if ("VERIFYING".equals(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, "验证中的质量问题不能再改措施");
        }
        requireAction(actionId, id, companyId);
        jdbcTemplate.update("""
                        UPDATE qlt_issue_action
                           SET content = ?, action_type = ?, owner_id = ?, due_date = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND issue_id = ? AND company_id = ?
                        """,
                req.content().trim(),
                normalize(req.actionType() == null ? "CORRECTIVE" : req.actionType(), ACTION_TYPES,
                        "措施类型"),
                req.ownerId(), req.dueDate(), userId, actionId, id, companyId);
        return detail(id, companyId);
    }

    /** 措施完成/取消完成：整改过程中允许反复勾（做错了就取消勾）。 */
    @Transactional
    public QualityIssueDtos.IssueDetail markAction(Long id, Long actionId, boolean done,
                                                   Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        requireOpen(issue, "已关闭的质量问题不能再改措施状态");
        requireAction(actionId, id, companyId);
        jdbcTemplate.update("""
                        UPDATE qlt_issue_action
                           SET status = ?, done_at = CASE WHEN ? THEN now() ELSE NULL END,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND issue_id = ? AND company_id = ?
                        """,
                done ? "DONE" : "PENDING", done, userId, actionId, id, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public void deleteAction(Long id, Long actionId, Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        requireOpen(issue, "已关闭的质量问题不能再删措施");
        if ("VERIFYING".equals(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, "验证中的质量问题不能再删措施");
        }
        requireAction(actionId, id, companyId);
        jdbcTemplate.update("""
                UPDATE qlt_issue_action SET deleted = true, updated_by = ?, updated_at = now()
                 WHERE id = ? AND issue_id = ? AND company_id = ?
                """, userId, actionId, id, companyId);
    }

    /** 提交验证：当前轮次必须有措施且全部完成，否则验证人只能干等。 */
    @Transactional
    public QualityIssueDtos.IssueDetail submitForVerify(Long id, Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        if (!Set.of("OPEN", "ASSIGNED", "IN_PROGRESS").contains(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID,
                    "只有未提交验证的质量问题可以提交（当前：" + issue.status() + "）");
        }
        if (issue.ownerId() == null) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, "请先分派责任人再提交验证");
        }
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM qlt_issue_action
                 WHERE issue_id = ? AND round_no = ? AND deleted = false
                """, Integer.class, id, issue.roundNo());
        Integer pending = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM qlt_issue_action
                 WHERE issue_id = ? AND round_no = ? AND deleted = false AND status = 'PENDING'
                """, Integer.class, id, issue.roundNo());
        if (total == null || total == 0) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID,
                    "第 " + issue.roundNo() + " 轮还没有任何整改措施，不能提交验证");
        }
        if (pending != null && pending > 0) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID,
                    "第 " + issue.roundNo() + " 轮还有 " + pending + " 条措施未完成");
        }
        jdbcTemplate.update("""
                UPDATE qlt_issue SET status = 'VERIFYING', updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 验证：通过则关闭；不通过则退回整改并开启下一轮（旧轮次措施原样保留）。 */
    @Transactional
    public QualityIssueDtos.IssueDetail verify(Long id, QualityIssueDtos.IssueVerifyReq req,
                                               Long companyId, Long userId) {
        QualityIssueDtos.IssueRow issue = requireIssue(id, companyId);
        if (!"VERIFYING".equals(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID,
                    "只有已提交验证的质量问题可以验证（当前：" + issue.status() + "）");
        }
        if (!req.passed() && (req.note() == null || req.note().isBlank())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID, "验证不通过必须写清原因");
        }
        if (req.passed()) {
            jdbcTemplate.update("""
                            UPDATE qlt_issue
                               SET status = 'CLOSED', closed_at = now(), verify_by = ?, verify_at = now(),
                                   verify_note = ?, updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ?
                            """, userId, blankToNull(req.note()), userId, id, companyId);
        } else {
            jdbcTemplate.update("""
                            UPDATE qlt_issue
                               SET status = 'IN_PROGRESS', round_no = round_no + 1,
                                   verify_by = ?, verify_at = now(), verify_note = ?,
                                   updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ?
                            """, userId, req.note().trim(), userId, id, companyId);
        }
        return detail(id, companyId);
    }

    // ---------------------------------------------------------------- 内部

    private QualityIssueDtos.IssueRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
        LocalDate dueDate = rs.getObject("due_date", LocalDate.class);
        String status = rs.getString("status");
        boolean overdue = dueDate != null && dueDate.isBefore(LocalDate.now())
                && !"CLOSED".equals(status);
        return new QualityIssueDtos.IssueRow(rs.getLong("id"), rs.getString("code"),
                rs.getString("title"), rs.getString("source"), rs.getString("severity"), status,
                rs.getInt("round_no"), rs.getString("object_type"),
                (Long) rs.getObject("object_id"), rs.getString("object_code"),
                (Long) rs.getObject("project_id"), (Long) rs.getObject("supplier_id"),
                (Long) rs.getObject("template_id"), rs.getString("template_code"),
                (Long) rs.getObject("owner_id"), rs.getString("owner_name"),
                rs.getString("reporter_name"),
                rs.getObject("reported_at", OffsetDateTime.class), dueDate,
                rs.getObject("closed_at", OffsetDateTime.class), rs.getString("verify_note"),
                rs.getInt("action_count"), rs.getInt("open_action_count"), overdue,
                rs.getString("remark"), rs.getObject("updated_at", OffsetDateTime.class));
    }

    private QualityIssueDtos.IssueRow requireIssue(Long id, Long companyId) {
        List<QualityIssueDtos.IssueRow> rows = jdbcTemplate.query(
                BASE_SELECT + " WHERE i.id = ? AND i.company_id = ? AND i.deleted = false",
                (rs, rowNum) -> row(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.QLT_ISSUE_NOT_FOUND, "质量问题不存在：" + id);
        }
        return rows.get(0);
    }

    private void requireAction(Long actionId, Long issueId, Long companyId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM qlt_issue_action
                 WHERE id = ? AND issue_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, actionId, issueId, companyId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID, "整改措施不存在：" + actionId);
        }
    }

    private void requireOpen(QualityIssueDtos.IssueRow issue, String message) {
        if ("CLOSED".equals(issue.status())) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, message);
        }
    }

    private void requireUser(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_user WHERE id = ?
                """, Integer.class, userId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID, "用户不存在：" + userId);
        }
    }

    private String nextCode() {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'QltIssue', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("QI-%06d", no == null ? 1L : no);
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID,
                    "未知" + label + "：" + value + "（可选 " + String.join("/", allowed) + "）");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
