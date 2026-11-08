package com.nexplm.platform.supplier.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.supplier.api.SupAdmissionDtos;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 供应商准入评审（P8-5-2）：PENDING → QUALIFIED 的唯一入口。
 *
 * <p>刻意的取舍：</p>
 * <ul>
 *   <li><b>准入不直接改状态</b>：「合格供应商」是要留证据的结论，直接改状态等于没有评审记录；
 *       评审单固定"谁、何时、按哪些检查项、结论如何"，供应商状态只是评审结果的投影；</li>
 *   <li><b>检查项不做模板表</b>：前端预置常用清单并允许逐条增删，后端只校验口径
 *       （至少一项、通过时不得有 FAIL、至少一项 PASS）；客户要求固定大纲时再加模板表；</li>
 *   <li><b>一张单同时只允许一到结束</b>：同一供应商存在 DRAFT/SUBMITTED 时拒绝再发起，
 *       避免两份结论打架；被驳回后可另起一张（REJECTED 是终态，留痕不覆盖）；</li>
 *   <li><b>驳回也留操作人</b>：{@code approved_by/approved_at} 记录"谁在何时做的终审"，
 *       驳回原因单独放 {@code reject_reason}。</li>
 * </ul>
 */
@Service
public class SupplierAdmissionService {

    private static final Set<String> TYPES = Set.of("INITIAL", "ANNUAL", "SPECIAL");
    private static final Set<String> STATUSES =
            Set.of("DRAFT", "SUBMITTED", "APPROVED", "REJECTED");
    private static final Set<String> RESULTS = Set.of("PASS", "FAIL", "NA");

    private static final String ADMISSION_SELECT = """
            SELECT a.*, s.code AS supplier_code, s.name AS supplier_name,
                   (SELECT COUNT(1) FROM sup_admission_item i
                     WHERE i.admission_id = a.id) AS item_count,
                   (SELECT COUNT(1) FROM sup_admission_item i
                     WHERE i.admission_id = a.id AND i.result = 'PASS') AS pass_count,
                   (SELECT COUNT(1) FROM sup_admission_item i
                     WHERE i.admission_id = a.id AND i.result = 'FAIL') AS fail_count
              FROM sup_admission a
              JOIN sup_supplier s ON s.id = a.supplier_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final CodeRuleService codeRuleService;

    public SupplierAdmissionService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
    }

    public PageResult<SupAdmissionDtos.AdmissionRow> page(Long companyId, Long supplierId,
                                                          String status, String type,
                                                          Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE a.company_id = ? AND a.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (supplierId != null) {
            where.append(" AND a.supplier_id = ?");
            args.add(supplierId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND a.status = ?");
            args.add(normalize(status, STATUSES, "评审状态"));
        }
        if (type != null && !type.isBlank()) {
            where.append(" AND a.type = ?");
            args.add(normalize(type, TYPES, "评审类型"));
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sup_admission a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SupAdmissionDtos.AdmissionRow> items = jdbcTemplate.query(
                ADMISSION_SELECT + where + " ORDER BY a.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public List<SupAdmissionDtos.AdmissionRow> bySupplier(Long companyId, Long supplierId) {
        return jdbcTemplate.query(ADMISSION_SELECT
                        + " WHERE a.company_id = ? AND a.supplier_id = ? AND a.deleted = false"
                        + " ORDER BY a.created_at DESC",
                (rs, rowNum) -> row(rs), companyId, supplierId);
    }

    public SupAdmissionDtos.AdmissionDetail detail(Long id, Long companyId) {
        List<SupAdmissionDtos.AdmissionRow> rows = jdbcTemplate.query(
                ADMISSION_SELECT + " WHERE a.id = ? AND a.company_id = ? AND a.deleted = false",
                (rs, rowNum) -> row(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_NOT_FOUND, "准入评审不存在：" + id);
        }
        SupAdmissionDtos.AdmissionRow r = rows.get(0);
        Map<String, Object> extra = jdbcTemplate.queryForMap("""
                SELECT conclusion, reject_reason, remark FROM sup_admission WHERE id = ?
                """, id);
        return new SupAdmissionDtos.AdmissionDetail(r.id(), r.code(), r.supplierId(),
                r.supplierCode(), r.supplierName(), r.type(), r.status(), r.plannedDate(),
                r.actualDate(), r.leader(), str(extra.get("conclusion")),
                str(extra.get("reject_reason")), str(extra.get("remark")), r.submittedAt(),
                r.approvedAt(), items(id));
    }

    @Transactional
    public SupAdmissionDtos.AdmissionDetail create(SupAdmissionDtos.AdmissionSaveReq req,
                                                   Long companyId, Long userId) {
        requireSupplier(req.supplierId(), companyId);
        Integer open = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sup_admission
                 WHERE company_id = ? AND supplier_id = ? AND deleted = false
                   AND status IN ('DRAFT', 'SUBMITTED')
                """, Integer.class, companyId, req.supplierId());
        if (open != null && open > 0) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_STATE_INVALID,
                    "该供应商已有未结束的准入评审，请先完成或删除它");
        }
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sup_admission (id, company_id, code, supplier_id, type, status,
                                                   planned_date, leader, remark, created_by,
                                                   updated_by)
                        VALUES (?, ?, ?, ?, ?, 'DRAFT', CAST(? AS date), ?, ?, ?, ?)
                        """,
                id, companyId, nextCode(), req.supplierId(),
                normalize(req.type() == null ? "INITIAL" : req.type(), TYPES, "评审类型"),
                normalizeDate(req.plannedDate()), blankToNull(req.leader()),
                blankToNull(req.remark()), userId, userId);
        writeItems(id, companyId, req.items());
        return detail(id, companyId);
    }

    @Transactional
    public SupAdmissionDtos.AdmissionDetail update(Long id, SupAdmissionDtos.AdmissionSaveReq req,
                                                   Long companyId, Long userId) {
        Map<String, Object> admission = requireAdmission(id, companyId);
        requireStatus(admission, "DRAFT", "只有草稿状态的准入评审可以修改");
        jdbcTemplate.update("""
                        UPDATE sup_admission
                           SET type = ?, planned_date = CAST(? AS date), leader = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                normalize(req.type() == null ? "INITIAL" : req.type(), TYPES, "评审类型"),
                normalizeDate(req.plannedDate()), blankToNull(req.leader()),
                blankToNull(req.remark()), userId, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_admission_item WHERE admission_id = ?", id);
        writeItems(id, companyId, req.items());
        return detail(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        Map<String, Object> admission = requireAdmission(id, companyId);
        requireStatus(admission, "DRAFT", "只有草稿状态的准入评审可以删除");
        jdbcTemplate.update("""
                UPDATE sup_admission SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_admission_item WHERE admission_id = ?", id);
    }

    @Transactional
    public SupAdmissionDtos.AdmissionDetail submit(Long id, Long companyId, Long userId) {
        Map<String, Object> admission = requireAdmission(id, companyId);
        requireStatus(admission, "DRAFT", "只有草稿状态的准入评审可以提交");
        jdbcTemplate.update("""
                UPDATE sup_admission
                   SET status = 'SUBMITTED', submitted_by = ?, submitted_at = now(),
                       updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, userId, id, companyId);
        return detail(id, companyId);
    }

    /**
     * 通过评审：要求无 FAIL 且至少一项 PASS，然后把供应商置为 QUALIFIED。
     *
     * <p>这是系统里唯一把供应商变成"合格"的动作——评级再高也不会自动准入。</p>
     */
    @Transactional
    public SupAdmissionDtos.AdmissionDetail approve(Long id, SupAdmissionDtos.ApproveReq req,
                                                    Long companyId, Long userId) {
        Map<String, Object> admission = requireAdmission(id, companyId);
        requireStatus(admission, "SUBMITTED", "只有已提交的准入评审可以通过");
        List<SupAdmissionDtos.AdmissionItemRow> items = items(id);
        long fail = items.stream().filter(item -> "FAIL".equals(item.result())).count();
        long pass = items.stream().filter(item -> "PASS".equals(item.result())).count();
        if (fail > 0) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_STATE_INVALID,
                    "还有 " + fail + " 项检查未通过（FAIL），不能通过准入");
        }
        if (pass == 0) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_STATE_INVALID,
                    "至少需要一项检查结果为通过（PASS）");
        }
        String conclusion = req == null || req.conclusion() == null || req.conclusion().isBlank()
                ? "准入评审通过" : req.conclusion().trim();
        jdbcTemplate.update("""
                UPDATE sup_admission
                   SET status = 'APPROVED', conclusion = ?, actual_date = current_date,
                       approved_by = ?, approved_at = now(), updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, conclusion, userId, userId, id, companyId);
        Long supplierId = longOf(admission.get("supplier_id"));
        jdbcTemplate.update("""
                UPDATE sup_supplier
                   SET status = 'QUALIFIED', qualified_at = now(), updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, supplierId, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public SupAdmissionDtos.AdmissionDetail reject(Long id, SupAdmissionDtos.RejectReq req,
                                                   Long companyId, Long userId) {
        Map<String, Object> admission = requireAdmission(id, companyId);
        requireStatus(admission, "SUBMITTED", "只有已提交的准入评审可以驳回");
        jdbcTemplate.update("""
                UPDATE sup_admission
                   SET status = 'REJECTED', reject_reason = ?, actual_date = current_date,
                       approved_by = ?, approved_at = now(), updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, req.reason().trim(), userId, userId, id, companyId);
        return detail(id, companyId);
    }

    // ---------------------------------------------------------------- 内部

    private void writeItems(Long admissionId, Long companyId,
                            List<SupAdmissionDtos.AdmissionItemReq> items) {
        if (items == null || items.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_INVALID, "检查项不能为空");
        }
        int seq = 0;
        for (SupAdmissionDtos.AdmissionItemReq item : items) {
            seq++;
            String result = normalize(item.result() == null ? "NA" : item.result(), RESULTS,
                    "检查结果");
            jdbcTemplate.update("""
                            INSERT INTO sup_admission_item (id, company_id, admission_id, seq,
                                                            check_item, result, evidence, remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, admissionId, seq, item.checkItem().trim(),
                    result, blankToNull(item.evidence()), blankToNull(item.remark()));
        }
    }

    private List<SupAdmissionDtos.AdmissionItemRow> items(Long admissionId) {
        return jdbcTemplate.query("""
                        SELECT * FROM sup_admission_item
                         WHERE admission_id = ? ORDER BY seq, id
                        """,
                (rs, rowNum) -> new SupAdmissionDtos.AdmissionItemRow(rs.getLong("id"),
                        rs.getInt("seq"), rs.getString("check_item"), rs.getString("result"),
                        rs.getString("evidence"), rs.getString("remark")),
                admissionId);
    }

    private SupAdmissionDtos.AdmissionRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupAdmissionDtos.AdmissionRow(rs.getLong("id"), rs.getString("code"),
                rs.getLong("supplier_id"), rs.getString("supplier_code"),
                rs.getString("supplier_name"), rs.getString("type"), rs.getString("status"),
                rs.getString("planned_date"), rs.getString("actual_date"),
                rs.getString("leader"), rs.getInt("item_count"), rs.getInt("pass_count"),
                rs.getInt("fail_count"), rs.getObject("submitted_at", OffsetDateTime.class),
                rs.getObject("approved_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private Map<String, Object> requireAdmission(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM sup_admission WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_NOT_FOUND, "准入评审不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireSupplier(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name, status FROM sup_supplier
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_NOT_FOUND, "供应商不存在：" + id);
        }
        return rows.get(0);
    }

    private static void requireStatus(Map<String, Object> admission, String expected,
                                      String message) {
        if (!expected.equals(str(admission.get("status")))) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_STATE_INVALID,
                    message + "（当前 " + str(admission.get("status")) + "）");
        }
    }

    /** 取号统一走 code_rule（台账 B19）。 */
    private String nextCode() {
        return codeRuleService.next("SupAdmission");
    }

    /** 只做格式校验：不接受 2026-13-01 这类脏值，空串视为未填。 */
    private static String normalizeDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String value = date.trim();
        try {
            LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_INVALID,
                    "日期格式应为 YYYY-MM-DD：" + date);
        }
        return value;
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.SUP_ADMISSION_INVALID,
                    "未知" + label + "：" + value + "（可选 " + String.join("/", allowed) + "）");
        }
        return normalized;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
