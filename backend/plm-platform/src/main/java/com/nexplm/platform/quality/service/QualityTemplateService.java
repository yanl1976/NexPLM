package com.nexplm.platform.quality.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.quality.api.QualityIssueDtos;
import com.nexplm.platform.quality.api.QualityTemplateDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 8D 模板（P8-2）：模板是一张"整改清单"，应用到问题时整批写成该轮措施。
 *
 * <p>关键取舍：<b>不建"步骤实例表"</b>。步骤只是措施的标题，两套表会让完成度要算两遍，
 * 而 P8-1 已经定死"提交验证要求当前轮措施全部 DONE"——把步骤落成措施，必填校验天然成立。</p>
 *
 * <p>同一轮重复应用同一模板会被挡住（{@code template_round}），否则一键点两次就是两份重复措施；
 * 验证被打回进入下一轮后可再次应用（轮次变了）。</p>
 */
@Service
public class QualityTemplateService {

    private static final Set<String> ACTION_TYPES =
            Set.of("CONTAINMENT", "ROOT_CAUSE", "CORRECTIVE", "PREVENTIVE", "VERIFY");
    private static final Set<String> TYPES = Set.of("8D", "CUSTOM");

    private final JdbcTemplate jdbcTemplate;
    private final QualityIssueService issueService;

    public QualityTemplateService(JdbcTemplate jdbcTemplate, QualityIssueService issueService) {
        this.jdbcTemplate = jdbcTemplate;
        this.issueService = issueService;
    }

    // ---------------------------------------------------------------- 模板

    public PageResult<QualityTemplateDtos.TemplateRow> list(Long companyId, String keyword,
                                                            Boolean enabled, Integer page,
                                                            Integer size) {
        StringBuilder where = new StringBuilder(" WHERE t.company_id = ? AND t.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (t.code ILIKE ? OR t.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (enabled != null) {
            where.append(" AND t.enabled = ?");
            args.add(enabled);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM qlt_issue_template t" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<QualityTemplateDtos.TemplateRow> items = jdbcTemplate.query("""
                        SELECT t.*,
                               (SELECT COUNT(1) FROM qlt_issue_template_item i
                                 WHERE i.template_id = t.id AND i.deleted = false) AS item_count
                          FROM qlt_issue_template t
                        """ + where + " ORDER BY t.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new QualityTemplateDtos.TemplateRow(rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("template_type"),
                        rs.getBoolean("enabled"), rs.getString("remark"), rs.getInt("item_count"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public QualityTemplateDtos.TemplateDetail detail(Long id, Long companyId) {
        var row = requireTemplate(id, companyId);
        List<QualityTemplateDtos.TemplateItemRow> items = jdbcTemplate.query("""
                        SELECT id, item_code, name, action_type, prompt, required, sort_no
                          FROM qlt_issue_template_item
                         WHERE template_id = ? AND deleted = false
                         ORDER BY sort_no, item_code
                        """,
                (rs, rowNum) -> new QualityTemplateDtos.TemplateItemRow(rs.getLong("id"),
                        rs.getString("item_code"), rs.getString("name"),
                        rs.getString("action_type"), rs.getString("prompt"),
                        rs.getBoolean("required"), rs.getInt("sort_no")),
                id);
        return new QualityTemplateDtos.TemplateDetail(id, str(row.get("code")),
                str(row.get("name")), str(row.get("template_type")),
                Boolean.TRUE.equals(row.get("enabled")), str(row.get("remark")), items);
    }

    @Transactional
    public QualityTemplateDtos.TemplateDetail create(QualityTemplateDtos.TemplateSaveReq req,
                                                     Long companyId, Long userId) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO qlt_issue_template (id, company_id, code, name, template_type,
                                                        remark, enabled, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, nextCode(), req.name().trim(), normalizeType(req.templateType()),
                blankToNull(req.remark()), req.enabled() == null || req.enabled(), userId, userId);
        return detail(id, companyId);
    }

    @Transactional
    public QualityTemplateDtos.TemplateDetail update(Long id,
                                                     QualityTemplateDtos.TemplateSaveReq req,
                                                     Long companyId, Long userId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("""
                        UPDATE qlt_issue_template
                           SET name = ?, template_type = ?, enabled = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeType(req.templateType()),
                req.enabled() == null || req.enabled(), blankToNull(req.remark()), userId, id,
                companyId);
        return detail(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("""
                UPDATE qlt_issue_template SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM qlt_issue_template_item WHERE template_id = ?", id);
    }

    /** 步骤整表替换：{@code item_code} 在模板内唯一，顺序即 {@code sort_no}。 */
    @Transactional
    public QualityTemplateDtos.TemplateDetail replaceItems(
            Long id, List<QualityTemplateDtos.TemplateItemSaveReq> items, Long companyId,
            Long userId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("DELETE FROM qlt_issue_template_item WHERE template_id = ?", id);
        List<QualityTemplateDtos.TemplateItemSaveReq> rows = items == null ? List.of() : items;
        Set<String> codes = new java.util.HashSet<>();
        int index = 0;
        for (QualityTemplateDtos.TemplateItemSaveReq row : rows) {
            String code = row.itemCode().trim().toUpperCase();
            if (!codes.add(code)) {
                throw BizException.of(ErrorCode.QLT_ISSUE_INVALID, "步骤编号重复：" + code);
            }
            jdbcTemplate.update("""
                            INSERT INTO qlt_issue_template_item (id, company_id, template_id, item_code,
                                                                 name, action_type, prompt, required, sort_no)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, id, code, row.name().trim(),
                    normalizeActionType(row.actionType()), blankToNull(row.prompt()),
                    row.required() == null || row.required(),
                    row.sortNo() == null ? (index + 1) * 10 : row.sortNo());
            index++;
        }
        jdbcTemplate.update("""
                UPDATE qlt_issue_template SET updated_by = ?, updated_at = now() WHERE id = ?
                """, userId, id);
        return detail(id, companyId);
    }

    /**
     * 应用模板：按步骤顺序批量生成该轮措施（content 预填提示语）。
     *
     * <p>生成的是<b>快照</b>：之后改模板、删模板都不影响已生成措施。</p>
     */
    @Transactional
    public QualityIssueDtos.IssueDetail applyToIssue(Long issueId, Long templateId, Long companyId,
                                                     Long userId) {
        QualityTemplateDtos.TemplateDetail template = detail(templateId, companyId);
        if (!template.enabled()) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID, "模板已停用：" + template.code());
        }
        if (template.items().isEmpty()) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID, "模板还没有配置步骤：" + template.code());
        }
        List<java.util.Map<String, Object>> issues = jdbcTemplate.queryForList("""
                SELECT id, status, round_no, owner_id, template_id, template_round
                  FROM qlt_issue WHERE id = ? AND company_id = ? AND deleted = false
                """, issueId, companyId);
        if (issues.isEmpty()) {
            throw BizException.of(ErrorCode.QLT_ISSUE_NOT_FOUND, "质量问题不存在：" + issueId);
        }
        java.util.Map<String, Object> issue = issues.get(0);
        String status = str(issue.get("status"));
        int roundNo = ((Number) issue.get("round_no")).intValue();
        if ("CLOSED".equals(status)) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, "已关闭的质量问题不能再应用模板");
        }
        if ("VERIFYING".equals(status)) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID, "验证中的质量问题不能再应用模板");
        }
        Object appliedTemplate = issue.get("template_id");
        Object appliedRound = issue.get("template_round");
        if (appliedTemplate != null && appliedRound != null
                && ((Number) appliedTemplate).longValue() == templateId
                && ((Number) appliedRound).intValue() == roundNo) {
            throw BizException.of(ErrorCode.QLT_ISSUE_STATE_INVALID,
                    "第 " + roundNo + " 轮已经应用过该模板了，请直接编辑措施内容");
        }
        Long ownerId = issue.get("owner_id") == null ? null
                : ((Number) issue.get("owner_id")).longValue();
        for (QualityTemplateDtos.TemplateItemRow item : template.items()) {
            String content = item.prompt() == null || item.prompt().isBlank()
                    ? "[" + item.itemCode() + "] " + item.name()
                    : "[" + item.itemCode() + "] " + item.prompt();
            jdbcTemplate.update("""
                            INSERT INTO qlt_issue_action (id, company_id, issue_id, round_no,
                                                          action_type, content, owner_id, status,
                                                          created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                            """,
                    IdWorker.getId(), companyId, issueId, roundNo, item.actionType(), content,
                    ownerId, userId, userId);
        }
        jdbcTemplate.update("""
                        UPDATE qlt_issue
                           SET template_id = ?, template_code = ?, template_round = ?,
                               status = CASE WHEN status IN ('OPEN', 'ASSIGNED') THEN 'IN_PROGRESS'
                                             ELSE status END,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                templateId, template.code(), roundNo, userId, issueId, companyId);
        return issueService.detail(issueId, companyId);
    }

    // ---------------------------------------------------------------- 内部

    private java.util.Map<String, Object> requireTemplate(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name, template_type, enabled, remark FROM qlt_issue_template
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.QLT_TEMPLATE_NOT_FOUND, "质量模板不存在：" + id);
        }
        return rows.get(0);
    }

    private String nextCode() {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'QltTemplate', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("DT-%06d", no == null ? 1L : no);
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "8D";
        }
        String normalized = type.trim().toUpperCase();
        if (!TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID,
                    "未知模板类型：" + type + "（可选 " + String.join("/", TYPES) + "）");
        }
        return normalized;
    }

    private static String normalizeActionType(String actionType) {
        if (actionType == null || actionType.isBlank()) {
            return "CORRECTIVE";
        }
        String normalized = actionType.trim().toUpperCase();
        if (!ACTION_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.QLT_ISSUE_INVALID,
                    "未知措施类型：" + actionType + "（可选 " + String.join("/", ACTION_TYPES) + "）");
        }
        return normalized;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
