package com.nexplm.platform.project.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.project.service.ProjectTimesheetService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.Map;

/** 工时填报与审批（P7-5）。 */
@Tag(name = "项目-工时填报与审批")
@RestController
@RequestMapping("/api/v1")
public class ProjectTimesheetController {

    private final ProjectTimesheetService timesheetService;

    public ProjectTimesheetController(ProjectTimesheetService timesheetService) {
        this.timesheetService = timesheetService;
    }

    /**
     * 工时列表。
     *
     * <p>没有 {@code prj.project.write} 的人只能查自己的工时：传别人的 userId 会被强制改成自己，
     * 避免"普通成员能翻全员工时"这种越权。</p>
     */
    @Operation(summary = "工时列表（分页，普通成员只能看自己的）")
    @GetMapping("/prj/timesheets")
    @PreAuthorize("isAuthenticated()")
    public Result<ProjectTimesheetDtos.TimesheetPage> page(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long effectiveUserId = canManage() ? userId : CurrentUser.userId();
        return Result.ok(timesheetService.page(CurrentUser.companyId(), projectId, effectiveUserId,
                taskId, status, from, to, page, size));
    }

    @Operation(summary = "项目工时汇总（已审批/待审批/驳回 + 按人/按任务/按周）")
    @GetMapping("/prj/projects/{projectId}/timesheet-summary")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectTimesheetDtos.TimesheetSummary> summary(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(timesheetService.summary(projectId, from, to, CurrentUser.companyId()));
    }

    @Operation(summary = "资源负荷（简化：按人按周已审批工时 / 40h 周容量）")
    @GetMapping("/prj/projects/{projectId}/resource-load")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectTimesheetDtos.ResourceLoad> resourceLoad(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(timesheetService.resourceLoad(projectId, from, to, CurrentUser.companyId()));
    }

    @Operation(summary = "填报工时（本人，一天一任务一条）")
    @PostMapping("/prj/timesheets")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TIMESHEET_CREATE", objectType = "PrjTimesheet")
    public Result<ProjectTimesheetDtos.TimesheetRow> create(
            @Valid @RequestBody ProjectTimesheetDtos.TimesheetSaveReq req) {
        return Result.ok(timesheetService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改工时（仅草稿/已打回，且仅本人）")
    @PutMapping("/prj/timesheets/{id}")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TIMESHEET_UPDATE", objectType = "PrjTimesheet")
    public Result<ProjectTimesheetDtos.TimesheetRow> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectTimesheetDtos.TimesheetSaveReq req) {
        return Result.ok(timesheetService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除工时（仅草稿/已打回，且仅本人）")
    @DeleteMapping("/prj/timesheets/{id}")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TIMESHEET_DELETE", objectType = "PrjTimesheet")
    public Result<Map<String, Object>> delete(@PathVariable Long id) {
        timesheetService.delete(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(Map.of("deleted", true));
    }

    @Operation(summary = "提交审批（草稿/已打回 → 已提交）")
    @PostMapping("/prj/timesheets/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TIMESHEET_SUBMIT", objectType = "PrjTimesheet")
    public Result<ProjectTimesheetDtos.TimesheetRow> submit(@PathVariable Long id) {
        return Result.ok(timesheetService.submit(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "审批通过（不能审自己的）")
    @PostMapping("/prj/timesheets/{id}/approve")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TIMESHEET_APPROVE", objectType = "PrjTimesheet")
    public Result<ProjectTimesheetDtos.TimesheetRow> approve(@PathVariable Long id) {
        return Result.ok(timesheetService.approve(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "批量通过（整批原子，任一不合法全部回滚）")
    @PostMapping("/prj/timesheets/batch-approve")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TIMESHEET_BATCH_APPROVE", objectType = "PrjTimesheet")
    public Result<Map<String, Object>> batchApprove(
            @Valid @RequestBody ProjectTimesheetDtos.BatchApproveReq req) {
        int approved = timesheetService.batchApprove(req.ids(), CurrentUser.companyId(),
                CurrentUser.userId());
        return Result.ok(Map.of("approved", approved));
    }

    @Operation(summary = "打回（必须写原因）")
    @PostMapping("/prj/timesheets/{id}/reject")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TIMESHEET_REJECT", objectType = "PrjTimesheet")
    public Result<ProjectTimesheetDtos.TimesheetRow> reject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectTimesheetDtos.TimesheetRejectReq req) {
        return Result.ok(timesheetService.reject(id, req.reason(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    private boolean canManage() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "prj.project.write".equals(authority.getAuthority()));
    }
}
