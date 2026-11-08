package com.nexplm.platform.quality.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.quality.api.QltAuditDtos;
import com.nexplm.platform.quality.api.QualityIssueDtos;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IATF16949 体系审核（P8-3-2）：审核计划 + 检查表 + 不符合项 → 转质量问题闭环。
 *
 * <p>刻意的取舍：</p>
 * <ul>
 *   <li><b>不在审核模块里重建整改流程</b>：不符合项一键转成 {@code qlt_issue}（source=AUDIT），
 *       整改、多轮验证、8D 模板都复用 P8-1/P8-2；审核这边只留"问题编号"往回看；</li>
 *   <li><b>检查表为空不能开始审核</b>：没有检查表的"审核"只是走个流程，检查项是审核的实体；</li>
 *   <li><b>不符合项必须先转问题、且问题已关闭才能关闭审核</b>：否则发现项会被"结项"掩盖，
 *       这也是 IATF 外审最容易挑的毛病；</li>
 *   <li><b>检查表整表替换</b>：与工艺路线/8D 模板一致，避免脏 item_no 与半截保存。</li>
 * </ul>
 */
@Service
public class QltAuditService {

    private static final Set<String> TYPES = Set.of("INTERNAL", "EXTERNAL", "SUPPLIER", "PROCESS");
    private static final Set<String> PLAN_STATUSES = Set.of("PLANNED", "IN_PROGRESS", "CLOSED");
    private static final Set<String> RESULTS = Set.of("PENDING", "OK", "NG", "NA");
    private static final Set<String> SEVERITIES = Set.of("MINOR", "MAJOR", "CRITICAL");

    private static final String PLAN_SELECT = """
            SELECT p.*, u.display_name AS auditor_name,
                   (SELECT COUNT(1) FROM qlt_audit_item i
                     WHERE i.plan_id = p.id AND i.deleted = false) AS item_count,
                   (SELECT COUNT(1) FROM qlt_audit_finding f
                     WHERE f.plan_id = p.id AND f.deleted = false) AS finding_count,
                   (SELECT COUNT(1) FROM qlt_audit_finding f
                     WHERE f.plan_id = p.id AND f.deleted = false AND f.status = 'OPEN')
                       AS open_finding_count
              FROM qlt_audit_plan p
              LEFT JOIN sys_user u ON u.id = p.auditor_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final QualityIssueService issueService;
    private final CodeRuleService codeRuleService;

    public QltAuditService(JdbcTemplate jdbcTemplate, QualityIssueService issueService,
                           CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.issueService = issueService;
        this.codeRuleService = codeRuleService;
    }

    // ---------------------------------------------------------------- 查询

    public PageResult<QltAuditDtos.PlanRow> page(Long companyId, String keyword, String status,
                                                 String auditType, Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE p.company_id = ? AND p.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.code ILIKE ? OR p.title ILIKE ? OR p.scope ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND p.status = ?");
            args.add(normalize(status, PLAN_STATUSES, "状态"));
        }
        if (auditType != null && !auditType.isBlank()) {
            where.append(" AND p.audit_type = ?");
            args.add(normalize(auditType, TYPES, "审核类型"));
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM qlt_audit_plan p" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<QltAuditDtos.PlanRow> items = jdbcTemplate.query(
                PLAN_SELECT + where
                        + " ORDER BY CASE p.status WHEN 'IN_PROGRESS' THEN 0 WHEN 'PLANNED' THEN 1"
                        + " ELSE 2 END, p.planned_date DESC NULLS LAST, p.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> planRow(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public QltAuditDtos.PlanDetail detail(Long id, Long companyId) {
        List<QltAuditDtos.PlanRow> rows = jdbcTemplate.query(
                PLAN_SELECT + " WHERE p.id = ? AND p.company_id = ? AND p.deleted = false",
                (rs, rowNum) -> planRow(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.AUDIT_NOT_FOUND, "审核计划不存在：" + id);
        }
        QltAuditDtos.PlanRow row = rows.get(0);
        List<QltAuditDtos.ItemRow> items = jdbcTemplate.query("""
                        SELECT id, item_no, clause, question, result, evidence, sort_no
                          FROM qlt_audit_item
                         WHERE plan_id = ? AND deleted = false
                         ORDER BY sort_no, item_no
                        """,
                (rs, rowNum) -> new QltAuditDtos.ItemRow(rs.getLong("id"), rs.getString("item_no"),
                        rs.getString("clause"), rs.getString("question"), rs.getString("result"),
                        rs.getString("evidence"), rs.getInt("sort_no")),
                id);
        List<QltAuditDtos.FindingRow> findings = jdbcTemplate.query("""
                        SELECT f.*, i.item_no
                          FROM qlt_audit_finding f
                          LEFT JOIN qlt_audit_item i ON i.id = f.item_id
                         WHERE f.plan_id = ? AND f.deleted = false
                         ORDER BY f.status DESC, f.created_at, f.id
                        """,
                (rs, rowNum) -> new QltAuditDtos.FindingRow(rs.getLong("id"),
                        rs.getString("code"), rs.getLong("plan_id"),
                        (Long) rs.getObject("item_id"), rs.getString("item_no"),
                        rs.getString("title"), rs.getString("description"),
                        rs.getString("severity"), rs.getObject("due_date", LocalDate.class),
                        rs.getString("status"), (Long) rs.getObject("issue_id"),
                        rs.getString("issue_code"),
                        rs.getObject("closed_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class)),
                id);
        int ok = 0;
        int ng = 0;
        int na = 0;
        int pending = 0;
        for (QltAuditDtos.ItemRow item : items) {
            switch (item.result() == null ? "PENDING" : item.result()) {
                case "OK" -> ok++;
                case "NG" -> ng++;
                case "NA" -> na++;
                default -> pending++;
            }
        }
        int openFindings = 0;
        for (QltAuditDtos.FindingRow finding : findings) {
            if ("OPEN".equals(finding.status())) {
                openFindings++;
            }
        }
        QltAuditDtos.AuditStats stats = new QltAuditDtos.AuditStats(items.size(), ok, ng, na,
                pending, findings.size(), openFindings);
        // 长文本字段单独一次查询：列表行不需要它们，塞进 PlanRow 会让列表白查大字段
        java.util.Map<String, Object> text = jdbcTemplate.queryForMap(
                "SELECT scope, summary, remark FROM qlt_audit_plan WHERE id = ?", id);
        return new QltAuditDtos.PlanDetail(row.id(), row.code(), row.title(), row.auditType(),
                row.standard(), str(text.get("scope")), row.plannedDate(), row.actualDate(),
                row.auditorId(), row.auditorName(), row.auditee(), row.status(),
                str(text.get("summary")), str(text.get("remark")), stats, items, findings);
    }

    // ---------------------------------------------------------------- 计划

    @Transactional
    public QltAuditDtos.PlanDetail createPlan(QltAuditDtos.PlanSaveReq req, Long companyId,
                                              Long userId) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO qlt_audit_plan (id, company_id, code, title, audit_type, standard,
                                                    scope, planned_date, auditor_id, auditee, status,
                                                    summary, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PLANNED', ?, ?, ?, ?)
                        """,
                id, companyId, nextCode("QltAuditPlan", "AU-"), req.title().trim(),
                normalize(req.auditType() == null ? "INTERNAL" : req.auditType(), TYPES, "审核类型"),
                blankToNull(req.standard()), blankToNull(req.scope()), req.plannedDate(),
                req.auditorId(), blankToNull(req.auditee()), blankToNull(req.summary()),
                blankToNull(req.remark()), userId, userId);
        return detail(id, companyId);
    }

    @Transactional
    public QltAuditDtos.PlanDetail updatePlan(Long id, QltAuditDtos.PlanSaveReq req,
                                              Long companyId, Long userId) {
        QltAuditDtos.PlanRow row = requirePlan(id, companyId);
        if ("CLOSED".equals(row.status())) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "已关闭的审核不能修改");
        }
        jdbcTemplate.update("""
                        UPDATE qlt_audit_plan
                           SET title = ?, audit_type = ?, standard = ?, scope = ?, planned_date = ?,
                               auditor_id = ?, auditee = ?, summary = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.title().trim(), normalize(req.auditType() == null ? "INTERNAL" : req.auditType(),
                        TYPES, "审核类型"), blankToNull(req.standard()), blankToNull(req.scope()),
                req.plannedDate(), req.auditorId(), blankToNull(req.auditee()),
                blankToNull(req.summary()), blankToNull(req.remark()), userId, id, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public void deletePlan(Long id, Long companyId) {
        QltAuditDtos.PlanRow row = requirePlan(id, companyId);
        if (!"PLANNED".equals(row.status())) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "只有未开始的审核可以删除");
        }
        if (row.findingCount() > 0) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "该审核已登记不符合项，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE qlt_audit_plan SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM qlt_audit_item WHERE plan_id = ?", id);
    }

    /** 检查表整表替换：{@code item_no} 计划内唯一，{@code result} 缺省 PENDING。 */
    @Transactional
    public QltAuditDtos.PlanDetail replaceItems(Long id, List<QltAuditDtos.ItemSaveReq> items,
                                                Long companyId, Long userId) {
        QltAuditDtos.PlanRow row = requirePlan(id, companyId);
        if ("CLOSED".equals(row.status())) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "已关闭的审核不能改检查表");
        }
        jdbcTemplate.update("DELETE FROM qlt_audit_item WHERE plan_id = ?", id);
        Set<String> numbers = new HashSet<>();
        int index = 0;
        for (QltAuditDtos.ItemSaveReq item : items == null ? List.<QltAuditDtos.ItemSaveReq>of()
                : items) {
            String itemNo = item.itemNo().trim().toUpperCase();
            if (!numbers.add(itemNo)) {
                throw BizException.of(ErrorCode.AUDIT_INVALID, "检查项编号重复：" + itemNo);
            }
            jdbcTemplate.update("""
                            INSERT INTO qlt_audit_item (id, company_id, plan_id, item_no, clause,
                                                        question, result, evidence, sort_no)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, id, itemNo, blankToNull(item.clause()),
                    item.question().trim(),
                    normalize(item.result() == null ? "PENDING" : item.result(), RESULTS, "检查结果"),
                    blankToNull(item.evidence()),
                    item.sortNo() == null ? (index + 1) * 10 : item.sortNo());
            index++;
        }
        jdbcTemplate.update("""
                UPDATE qlt_audit_plan SET updated_by = ?, updated_at = now() WHERE id = ?
                """, userId, id);
        return detail(id, companyId);
    }

    /** 开始审核：要求检查表非空，记录实际开始日。 */
    @Transactional
    public QltAuditDtos.PlanDetail start(Long id, Long companyId, Long userId) {
        QltAuditDtos.PlanRow row = requirePlan(id, companyId);
        if (!"PLANNED".equals(row.status())) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "只有计划中的审核可以开始（当前：" + row.status() + "）");
        }
        if (row.itemCount() == 0) {
            throw BizException.of(ErrorCode.AUDIT_INVALID, "检查表为空，请先配置检查项再开始审核");
        }
        jdbcTemplate.update("""
                        UPDATE qlt_audit_plan
                           SET status = 'IN_PROGRESS',
                               actual_date = COALESCE(actual_date, CURRENT_DATE),
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 关闭审核：存在未关闭不符合项时拒绝（发现项不能被结项掩盖）。 */
    @Transactional
    public QltAuditDtos.PlanDetail close(Long id, Long companyId, Long userId) {
        QltAuditDtos.PlanRow row = requirePlan(id, companyId);
        if (!"IN_PROGRESS".equals(row.status())) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "只有进行中的审核可以关闭（当前：" + row.status() + "）");
        }
        if (row.openFindingCount() > 0) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "还有 " + row.openFindingCount() + " 个不符合项未关闭，不能关闭审核");
        }
        jdbcTemplate.update("""
                        UPDATE qlt_audit_plan SET status = 'CLOSED',
                                                 actual_date = COALESCE(actual_date, CURRENT_DATE),
                                                 updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    // ---------------------------------------------------------------- 不符合项

    @Transactional
    public QltAuditDtos.PlanDetail addFinding(Long planId, QltAuditDtos.FindingSaveReq req,
                                              Long companyId, Long userId) {
        QltAuditDtos.PlanRow row = requirePlan(planId, companyId);
        if ("CLOSED".equals(row.status())) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "已关闭的审核不能再登记不符合项");
        }
        if (req.itemId() != null) {
            requireItem(req.itemId(), planId, companyId);
        }
        jdbcTemplate.update("""
                        INSERT INTO qlt_audit_finding (id, company_id, plan_id, item_id, code, title,
                                                        description, severity, due_date, status,
                                                        created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, ?)
                        """,
                IdWorker.getId(), companyId, planId, req.itemId(),
                nextCode("QltAuditFinding", "FD-"), req.title().trim(),
                blankToNull(req.description()),
                normalize(req.severity() == null ? "MINOR" : req.severity(), SEVERITIES, "严重度"),
                req.dueDate(), userId, userId);
        jdbcTemplate.update("""
                UPDATE qlt_audit_plan SET updated_by = ?, updated_at = now() WHERE id = ?
                """, userId, planId);
        return detail(planId, companyId);
    }

    /** 改不符合项：转成质量问题后只读（整改内容归 P8-1，避免两处各改一份）。 */
    @Transactional
    public QltAuditDtos.PlanDetail updateFinding(Long findingId,
                                                 QltAuditDtos.FindingSaveReq req, Long companyId,
                                                 Long userId) {
        var finding = requireFinding(findingId, companyId);
        if ("CLOSED".equals(str(finding.get("status")))) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "已关闭的不符合项不能修改");
        }
        if (finding.get("issue_id") != null) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "该不符合项已转质量问题，整改内容请在质量问题里修改");
        }
        jdbcTemplate.update("""
                        UPDATE qlt_audit_finding
                           SET title = ?, description = ?, severity = ?, due_date = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.title().trim(), blankToNull(req.description()),
                normalize(req.severity() == null ? "MINOR" : req.severity(), SEVERITIES, "严重度"),
                req.dueDate(), userId, findingId, companyId);
        return detail(longOf(finding.get("plan_id")), companyId);
    }

    @Transactional
    public void deleteFinding(Long findingId, Long companyId) {
        var finding = requireFinding(findingId, companyId);
        if (finding.get("issue_id") != null) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "该不符合项已转质量问题，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE qlt_audit_finding SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, findingId, companyId);
    }

    /**
     * 转质量问题：生成 {@code qlt_issue}（source=AUDIT）并回填 finding.issue_id/issue_code。
     *
     * <p>{@code objectCode} 记审核计划编码，回答"这条问题从哪次审核来的"。</p>
     */
    @Transactional
    public QltAuditDtos.PlanDetail findingToIssue(Long findingId, Long companyId, Long userId) {
        var finding = requireFinding(findingId, companyId);
        if (finding.get("issue_id") != null) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "该不符合项已转质量问题：" + str(finding.get("issue_code")));
        }
        Long planId = longOf(finding.get("plan_id"));
        QltAuditDtos.PlanRow plan = requirePlan(planId, companyId);
        String description = str(finding.get("description"));
        String prefix = "来源：体系审核 " + plan.code() + "（" + plan.title() + "）";
        QualityIssueDtos.IssueDetail issue = issueService.create(
                new QualityIssueDtos.IssueSaveReq(
                        str(finding.get("title")),
                        description == null || description.isBlank() ? prefix : prefix + "\n" + description,
                        "AUDIT", str(finding.get("severity")), null,
                        dateOf(finding.get("due_date")),
                        "QltAuditPlan", planId, plan.code(), null, null, null),
                companyId, userId);
        jdbcTemplate.update("""
                        UPDATE qlt_audit_finding
                           SET issue_id = ?, issue_code = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                issue.issue().id(), issue.issue().code(), userId, findingId, companyId);
        return detail(planId, companyId);
    }

    /** 关闭不符合项：必须已转问题且该问题已关闭（整改闭环才收口）。 */
    @Transactional
    public QltAuditDtos.PlanDetail closeFinding(Long findingId, Long companyId, Long userId) {
        var finding = requireFinding(findingId, companyId);
        if ("CLOSED".equals(str(finding.get("status")))) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID, "该不符合项已关闭");
        }
        Object issueId = finding.get("issue_id");
        if (issueId == null) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "请先把不符合项转成质量问题，整改完成后再关闭");
        }
        String issueStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM qlt_issue WHERE id = ? AND company_id = ? AND deleted = false",
                String.class, longOf(issueId), companyId);
        if (!"CLOSED".equals(issueStatus)) {
            throw BizException.of(ErrorCode.AUDIT_STATE_INVALID,
                    "关联质量问题尚未关闭（当前：" + issueStatus + "），整改验证通过后才能关闭不符合项");
        }
        jdbcTemplate.update("""
                        UPDATE qlt_audit_finding
                           SET status = 'CLOSED', closed_at = now(), updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, findingId, companyId);
        return detail(longOf(finding.get("plan_id")), companyId);
    }

    // ---------------------------------------------------------------- 内部

    private QltAuditDtos.PlanRow planRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new QltAuditDtos.PlanRow(rs.getLong("id"), rs.getString("code"),
                rs.getString("title"), rs.getString("audit_type"), rs.getString("standard"),
                rs.getObject("planned_date", LocalDate.class),
                rs.getObject("actual_date", LocalDate.class),
                (Long) rs.getObject("auditor_id"), rs.getString("auditor_name"),
                rs.getString("auditee"), rs.getString("status"), rs.getInt("item_count"),
                rs.getInt("finding_count"), rs.getInt("open_finding_count"),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private QltAuditDtos.PlanRow requirePlan(Long id, Long companyId) {
        List<QltAuditDtos.PlanRow> rows = jdbcTemplate.query(
                PLAN_SELECT + " WHERE p.id = ? AND p.company_id = ? AND p.deleted = false",
                (rs, rowNum) -> planRow(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.AUDIT_NOT_FOUND, "审核计划不存在：" + id);
        }
        return rows.get(0);
    }

    private java.util.Map<String, Object> requireFinding(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, plan_id, code, title, description, severity, due_date, status, issue_id,
                       issue_code
                  FROM qlt_audit_finding WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.AUDIT_NOT_FOUND, "不符合项不存在：" + id);
        }
        return rows.get(0);
    }

    private void requireItem(Long itemId, Long planId, Long companyId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM qlt_audit_item
                 WHERE id = ? AND plan_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, itemId, planId, companyId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.AUDIT_INVALID, "检查项不存在：" + itemId);
        }
    }

    /** 取号统一走 code_rule（台账 B19）；{@code prefix} 仅为兼容调用签名。 */
    private String nextCode(String ruleCode, String prefix) {
        return codeRuleService.next(ruleCode);
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.AUDIT_INVALID,
                    "未知" + label + "：" + value + "（可选 " + String.join("/", allowed) + "）");
        }
        return normalized;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /** {@code queryForList} 取 date 列一般是 {@link java.sql.Date}，这里统一收口。 */
    private static LocalDate dateOf(Object value) {
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return value instanceof LocalDate localDate ? localDate : null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
