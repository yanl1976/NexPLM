package com.nexplm.platform.quality.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.quality.service.QltAuditService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** IATF16949 体系审核：计划 + 检查表 + 不符合项转质量问题（P8-3-2）。 */
@Tag(name = "质量-体系审核")
@RestController
@RequestMapping("/api/v1")
public class QltAuditController {

    private final QltAuditService auditService;

    public QltAuditController(QltAuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(summary = "审核计划列表（进行中优先）")
    @GetMapping("/qlt/audit-plans")
    @PreAuthorize("hasAuthority('qlt.audit.read')")
    public Result<PageResult<QltAuditDtos.PlanRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(auditService.page(CurrentUser.companyId(), keyword, status, auditType,
                page, size));
    }

    @Operation(summary = "审核详情（含检查表、不符合项与统计）")
    @GetMapping("/qlt/audit-plans/{id}")
    @PreAuthorize("hasAuthority('qlt.audit.read')")
    public Result<QltAuditDtos.PlanDetail> detail(@PathVariable Long id) {
        return Result.ok(auditService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建审核计划（自动取号 AU-xxxxxx）")
    @PostMapping("/qlt/audit-plans")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_PLAN_CREATE", objectType = "QltAuditPlan")
    public Result<QltAuditDtos.PlanDetail> create(
            @Valid @RequestBody QltAuditDtos.PlanSaveReq req) {
        return Result.ok(auditService.createPlan(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改审核计划（已关闭不可改）")
    @PutMapping("/qlt/audit-plans/{id}")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_PLAN_UPDATE", objectType = "QltAuditPlan")
    public Result<QltAuditDtos.PlanDetail> update(
            @PathVariable Long id, @Valid @RequestBody QltAuditDtos.PlanSaveReq req) {
        return Result.ok(auditService.updatePlan(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除审核计划（仅未开始且无不符合项）")
    @DeleteMapping("/qlt/audit-plans/{id}")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_PLAN_DELETE", objectType = "QltAuditPlan")
    public Result<Void> delete(@PathVariable Long id) {
        auditService.deletePlan(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "整表替换检查表")
    @PutMapping("/qlt/audit-plans/{id}/items")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_ITEM_REPLACE", objectType = "QltAuditPlan")
    public Result<QltAuditDtos.PlanDetail> replaceItems(
            @PathVariable Long id, @Valid @RequestBody QltAuditDtos.ItemsReq req) {
        return Result.ok(auditService.replaceItems(id, req.items(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "开始审核（检查表不能为空）")
    @PostMapping("/qlt/audit-plans/{id}/start")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_PLAN_START", objectType = "QltAuditPlan")
    public Result<QltAuditDtos.PlanDetail> start(@PathVariable Long id) {
        return Result.ok(auditService.start(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "关闭审核（不允许存在未关闭的不符合项）")
    @PostMapping("/qlt/audit-plans/{id}/close")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_PLAN_CLOSE", objectType = "QltAuditPlan")
    public Result<QltAuditDtos.PlanDetail> close(@PathVariable Long id) {
        return Result.ok(auditService.close(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "登记不符合项（自动取号 FD-xxxxxx）")
    @PostMapping("/qlt/audit-plans/{id}/findings")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_FINDING_CREATE", objectType = "QltAuditFinding")
    public Result<QltAuditDtos.PlanDetail> addFinding(
            @PathVariable Long id, @Valid @RequestBody QltAuditDtos.FindingSaveReq req) {
        return Result.ok(auditService.addFinding(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改不符合项（转问题后只读）")
    @PutMapping("/qlt/audit-findings/{id}")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_FINDING_UPDATE", objectType = "QltAuditFinding")
    public Result<QltAuditDtos.PlanDetail> updateFinding(
            @PathVariable Long id, @Valid @RequestBody QltAuditDtos.FindingSaveReq req) {
        return Result.ok(auditService.updateFinding(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除不符合项（已转问题不可删）")
    @DeleteMapping("/qlt/audit-findings/{id}")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_FINDING_DELETE", objectType = "QltAuditFinding")
    public Result<Void> deleteFinding(@PathVariable Long id) {
        auditService.deleteFinding(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "不符合项转质量问题（走 P8-1 整改闭环）")
    @PostMapping("/qlt/audit-findings/{id}/to-issue")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_FINDING_TO_ISSUE", objectType = "QltAuditFinding")
    public Result<QltAuditDtos.PlanDetail> toIssue(@PathVariable Long id) {
        return Result.ok(auditService.findingToIssue(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "关闭不符合项（关联质量问题必须已关闭）")
    @PostMapping("/qlt/audit-findings/{id}/close")
    @PreAuthorize("hasAuthority('qlt.audit.write')")
    @Audit(module = "QLT", action = "AUDIT_FINDING_CLOSE", objectType = "QltAuditFinding")
    public Result<QltAuditDtos.PlanDetail> closeFinding(@PathVariable Long id) {
        return Result.ok(auditService.closeFinding(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }
}
