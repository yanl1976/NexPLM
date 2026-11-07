package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工装管理，对齐旧系统 {@code CAPP/Tooling}：「台账 + 申请单 + 借用/占用」三段。
 *
 * <p>旧系统申请单走工作流（{@code CreateToolingAfterToolingApplyPublishedPage} 在发布后建台账）。
 * 这里保留状态机（草稿→提交→批准/驳回），批准时自动生成台账；**不强行接入 Flowable**——
 * 工装申请是轻审批，接入流程引擎会把「改个规格」变成跨模块操作，后续如需统一走 BPMN，
 * 只需把 approve 改为工作流回调（接口已按此形状设计）。</p>
 */
@Service
public class CappToolingService {

    private static final Set<String> TYPES = Set.of("FIXTURE", "GAUGE", "MOLD", "CUTTER", "AID");
    private static final Set<String> STATUS = Set.of("IDLE", "IN_USE", "MAINTAIN", "SCRAP");

    private final JdbcTemplate jdbcTemplate;
    private final CodeRuleService codeRuleService;

    public CappToolingService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
    }

    // ------------------------------------------------------------ 台账

    public List<Map<String, Object>> list(Long companyId, String keyword, String toolingType, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id, t.code, t.name, t.tooling_type, t.spec, t.status, t.location, t.owner_id,
                       u.display_name AS owner_name, t.purchase_date, t.remark,
                       (SELECT COUNT(1) FROM capp_tooling_borrow b
                         WHERE b.tooling_id = t.id AND b.returned_at IS NULL) AS borrowed
                  FROM capp_tooling t
                  LEFT JOIN sys_user u ON u.id = t.owner_id
                 WHERE t.company_id = ? AND t.deleted = false
                """);
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (lower(t.code) LIKE ? OR lower(t.name) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
        }
        if (toolingType != null && !toolingType.isBlank()) {
            sql.append(" AND t.tooling_type = ?");
            args.add(normalizeType(toolingType));
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.status = ?");
            args.add(normalizeStatus(status));
        }
        sql.append(" ORDER BY t.code");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public Long create(Long companyId, Long userId, ToolingReq req) {
        String type = normalizeType(req.toolingType());
        String status = normalizeStatus(req.status() == null ? "IDLE" : req.status());
        String code = req.code() == null || req.code().isBlank()
                ? codeRuleService.next("CappTooling") : req.code().trim();
        requireUnique(companyId, code, null);
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO capp_tooling (id, company_id, code, name, tooling_type, spec, status,
                       location, owner_id, purchase_date, remark, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, companyId, code, req.name(), type, req.spec(), status, req.location(),
                req.ownerId(), req.purchaseDate() == null ? null : Date.valueOf(req.purchaseDate()),
                req.remark(), userId);
        return id;
    }

    @Transactional
    public void update(Long id, Long companyId, ToolingReq req) {
        Map<String, Object> old = requireTooling(id, companyId);
        String code = req.code() == null || req.code().isBlank()
                ? String.valueOf(old.get("code")) : req.code().trim();
        if (!code.equals(String.valueOf(old.get("code")))) {
            requireUnique(companyId, code, id);
        }
        jdbcTemplate.update("""
                UPDATE capp_tooling SET code = ?, name = ?, tooling_type = ?, spec = ?, status = ?,
                       location = ?, owner_id = ?, purchase_date = ?, remark = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, code, req.name(), normalizeType(req.toolingType()), req.spec(),
                normalizeStatus(req.status() == null ? String.valueOf(old.get("status")) : req.status()),
                req.location(), req.ownerId(),
                req.purchaseDate() == null ? null : Date.valueOf(req.purchaseDate()),
                req.remark(), id, companyId);
    }

    /** 删除：有未归还借用时拒绝（否则借用记录会指向一个不存在的东西）。 */
    @Transactional
    public void delete(Long id, Long companyId) {
        requireTooling(id, companyId);
        Long active = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_tooling_borrow
                 WHERE tooling_id = ? AND returned_at IS NULL
                """, Long.class, id);
        if (active != null && active > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该工装仍在借用中，先归还再删除");
        }
        jdbcTemplate.update("UPDATE capp_tooling SET deleted = true, updated_at = now() WHERE id = ?", id);
    }

    // ------------------------------------------------------------ 申请单

    public List<Map<String, Object>> applies(Long companyId, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.code, a.tooling_type, a.tooling_name, a.spec, a.reason, a.status,
                       a.applicant_id, ua.display_name AS applicant_name,
                       a.approved_by, ub.display_name AS approver_name, a.approved_at,
                       a.tooling_id, t.code AS tooling_code, a.created_at
                  FROM capp_tooling_apply a
                  LEFT JOIN sys_user ua ON ua.id = a.applicant_id
                  LEFT JOIN sys_user ub ON ub.id = a.approved_by
                  LEFT JOIN capp_tooling t ON t.id = a.tooling_id
                 WHERE a.company_id = ? AND a.deleted = false
                """);
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            args.add(status.trim().toUpperCase());
        }
        sql.append(" ORDER BY a.created_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public Long createApply(Long companyId, Long userId, ApplyReq req) {
        String code = req.code() == null || req.code().isBlank()
                ? codeRuleService.next("CappToolingApply") : req.code().trim();
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO capp_tooling_apply (id, company_id, code, tooling_type, tooling_name, spec,
                       reason, status, applicant_id, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, companyId, code, normalizeType(req.toolingType()), req.toolingName(),
                req.spec(), req.reason(), "DRAFT", userId, req.remark());
        return id;
    }

    /** 提交：草稿 → 已提交。 */
    @Transactional
    public void submitApply(Long id, Long companyId) {
        Map<String, Object> apply = requireApply(id, companyId);
        if (!"DRAFT".equals(String.valueOf(apply.get("status")))) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "只有草稿可以提交");
        }
        jdbcTemplate.update(
                "UPDATE capp_tooling_apply SET status = 'SUBMITTED', updated_at = now() WHERE id = ?", id);
    }

    /**
     * 审批：已提交 → 批准/驳回。
     *
     * <p>批准时**自动生成工装台账**（对齐旧系统申请单发布后建台账），并把台账 id 回写到申请单，
     * 使「申请 → 实物」可追溯。</p>
     */
    @Transactional
    public Map<String, Object> approveApply(Long id, Long companyId, Long userId, boolean approved,
                                            String comment, String toolingCode) {
        Map<String, Object> apply = requireApply(id, companyId);
        if (!"SUBMITTED".equals(String.valueOf(apply.get("status")))) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "只有已提交的申请单可以审批");
        }
        Long toolingId = null;
        if (approved) {
            String code = toolingCode == null || toolingCode.isBlank()
                    ? codeRuleService.next("CappTooling") : toolingCode.trim();
            requireUnique(companyId, code, null);
            toolingId = IdWorker.getId();
            jdbcTemplate.update("""
                    INSERT INTO capp_tooling (id, company_id, code, name, tooling_type, spec, status,
                           remark, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, 'IDLE', ?, ?)
                    """, toolingId, companyId, code, apply.get("tooling_name"),
                    apply.get("tooling_type"), apply.get("spec"),
                    "由工装申请单 " + apply.get("code") + " 批准后生成", userId);
        }
        jdbcTemplate.update("""
                UPDATE capp_tooling_apply SET status = ?, approved_by = ?, approved_at = now(),
                       tooling_id = ?, remark = COALESCE(?, remark), updated_at = now()
                 WHERE id = ?
                """, approved ? "APPROVED" : "REJECTED", userId, toolingId, comment, id);
        return Map.of("status", approved ? "APPROVED" : "REJECTED", "toolingId", toolingId == null ? "" : toolingId);
    }

    // ------------------------------------------------------------ 借用/占用

    public List<Map<String, Object>> borrows(Long companyId, Long toolingId) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.id, b.tooling_id, t.code AS tooling_code, t.name AS tooling_name,
                       b.borrower_id, u.display_name AS borrower_name, b.routing_id,
                       r.code AS routing_code, b.borrowed_at, b.returned_at, b.remark
                  FROM capp_tooling_borrow b
                  JOIN capp_tooling t ON t.id = b.tooling_id
                  LEFT JOIN sys_user u ON u.id = b.borrower_id
                  LEFT JOIN capp_routing r ON r.id = b.routing_id
                 WHERE b.company_id = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (toolingId != null) {
            sql.append(" AND b.tooling_id = ?");
            args.add(toolingId);
        }
        sql.append(" ORDER BY b.borrowed_at DESC");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    /** 借出：工装必须处于在库状态，借出后台账置 IN_USE。 */
    @Transactional
    public Long borrow(Long companyId, Long userId, Long toolingId, Long routingId, String remark) {
        Map<String, Object> tooling = requireTooling(toolingId, companyId);
        if (!"IDLE".equals(String.valueOf(tooling.get("status")))) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "工装当前不在库，不能借出");
        }
        Long active = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_tooling_borrow WHERE tooling_id = ? AND returned_at IS NULL",
                Long.class, toolingId);
        if (active != null && active > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该工装已有未归还的借用记录");
        }
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO capp_tooling_borrow (id, company_id, tooling_id, borrower_id, routing_id, remark)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, companyId, toolingId, userId, routingId, remark);
        jdbcTemplate.update(
                "UPDATE capp_tooling SET status = 'IN_USE', updated_at = now() WHERE id = ?", toolingId);
        return id;
    }

    /** 归还：置 returned_at，台账回 IDLE。 */
    @Transactional
    public void giveBack(Long borrowId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tooling_id, returned_at FROM capp_tooling_borrow WHERE id = ? AND company_id = ?",
                borrowId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "借用记录不存在");
        }
        if (rows.get(0).get("returned_at") != null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该借用已归还");
        }
        jdbcTemplate.update(
                "UPDATE capp_tooling_borrow SET returned_at = now() WHERE id = ?", borrowId);
        Long toolingId = ((Number) rows.get(0).get("tooling_id")).longValue();
        jdbcTemplate.update(
                "UPDATE capp_tooling SET status = 'IDLE', updated_at = now() WHERE id = ?", toolingId);
    }

    // ------------------------------------------------------------ 内部工具

    private Map<String, Object> requireTooling(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, code, status FROM capp_tooling WHERE id = ? AND company_id = ? AND deleted = false",
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "工装不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> requireApply(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, status, tooling_name, tooling_type, spec
                  FROM capp_tooling_apply WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "工装申请单不存在");
        }
        return rows.get(0);
    }

    private void requireUnique(Long companyId, String code, Long excludeId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_tooling
                 WHERE company_id = ? AND code = ? AND deleted = false AND (?::bigint IS NULL OR id <> ?)
                """, Long.class, companyId, code, excludeId, excludeId);
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "工装编码已存在：" + code);
        }
    }

    private static String normalizeType(String type) {
        String value = type == null || type.isBlank() ? "FIXTURE" : type.trim().toUpperCase();
        if (!TYPES.contains(value)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "工装类型只能是 FIXTURE/GAUGE/MOLD/CUTTER/AID");
        }
        return value;
    }

    private static String normalizeStatus(String status) {
        String value = status == null || status.isBlank() ? "IDLE" : status.trim().toUpperCase();
        if (!STATUS.contains(value)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "工装状态只能是 IDLE/IN_USE/MAINTAIN/SCRAP");
        }
        return value;
    }

    private static OffsetDateTime time(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    public record ToolingReq(String code, String name, String toolingType, String spec, String status,
                             String location, Long ownerId, java.time.LocalDate purchaseDate,
                             String remark) {
    }

    public record ApplyReq(String code, String toolingType, String toolingName, String spec,
                           String reason, String remark) {
    }
}
