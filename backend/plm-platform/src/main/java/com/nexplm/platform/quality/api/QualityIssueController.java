package com.nexplm.platform.quality.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.quality.service.QualityIssueService;
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

/** 质量问题闭环（P8-1）。 */
@Tag(name = "质量-问题闭环")
@RestController
@RequestMapping("/api/v1")
public class QualityIssueController {

    private final QualityIssueService issueService;

    public QualityIssueController(QualityIssueService issueService) {
        this.issueService = issueService;
    }

    @Operation(summary = "质量问题列表（未关闭优先，按期限升序）")
    @GetMapping("/qlt/issues")
    @PreAuthorize("hasAuthority('qlt.issue.read')")
    public Result<PageResult<QualityIssueDtos.IssueRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(issueService.page(CurrentUser.companyId(), keyword, status, severity,
                source, ownerId, overdue, page, size));
    }

    @Operation(summary = "质量问题详情（含各轮整改措施）")
    @GetMapping("/qlt/issues/{id}")
    @PreAuthorize("hasAuthority('qlt.issue.read')")
    public Result<QualityIssueDtos.IssueDetail> detail(@PathVariable Long id) {
        return Result.ok(issueService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "登记质量问题（带责任人则直接为已分派）")
    @PostMapping("/qlt/issues")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_CREATE", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> create(
            @Valid @RequestBody QualityIssueDtos.IssueSaveReq req) {
        return Result.ok(issueService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改质量问题基础信息（已关闭不可改）")
    @PutMapping("/qlt/issues/{id}")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_UPDATE", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> update(
            @PathVariable Long id, @Valid @RequestBody QualityIssueDtos.IssueSaveReq req) {
        return Result.ok(issueService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "分派责任人")
    @PostMapping("/qlt/issues/{id}/assign")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_ASSIGN", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> assign(
            @PathVariable Long id, @Valid @RequestBody QualityIssueDtos.IssueAssignReq req) {
        return Result.ok(issueService.assign(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "新增整改措施（记在当前轮次）")
    @PostMapping("/qlt/issues/{id}/actions")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_ACTION_CREATE", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> addAction(
            @PathVariable Long id, @Valid @RequestBody QualityIssueDtos.ActionSaveReq req) {
        return Result.ok(issueService.addAction(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改整改措施")
    @PutMapping("/qlt/issues/{id}/actions/{actionId}")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_ACTION_UPDATE", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> updateAction(
            @PathVariable Long id, @PathVariable Long actionId,
            @Valid @RequestBody QualityIssueDtos.ActionSaveReq req) {
        return Result.ok(issueService.updateAction(id, actionId, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "标记措施完成/取消完成")
    @PostMapping("/qlt/issues/{id}/actions/{actionId}/done")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_ACTION_DONE", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> markAction(
            @PathVariable Long id, @PathVariable Long actionId,
            @RequestParam(defaultValue = "true") boolean done) {
        return Result.ok(issueService.markAction(id, actionId, done, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除整改措施（软删）")
    @DeleteMapping("/qlt/issues/{id}/actions/{actionId}")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_ACTION_DELETE", objectType = "QltIssue")
    public Result<Void> deleteAction(@PathVariable Long id, @PathVariable Long actionId) {
        issueService.deleteAction(id, actionId, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }

    @Operation(summary = "提交验证（当前轮措施必须全部完成）")
    @PostMapping("/qlt/issues/{id}/submit")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_SUBMIT", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> submit(@PathVariable Long id) {
        return Result.ok(issueService.submitForVerify(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "验证（通过关闭；不通过退回整改并开启下一轮）")
    @PostMapping("/qlt/issues/{id}/verify")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_VERIFY", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> verify(
            @PathVariable Long id, @Valid @RequestBody QualityIssueDtos.IssueVerifyReq req) {
        return Result.ok(issueService.verify(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }
}
