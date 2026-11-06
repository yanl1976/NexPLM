package com.nexplm.platform.quality.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.quality.service.QualityDashboardService;
import com.nexplm.platform.quality.service.QualityTemplateService;
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

/** 8D 模板可配 + 质量看板（P8-2）。 */
@Tag(name = "质量-8D 模板与看板")
@RestController
@RequestMapping("/api/v1")
public class QualityTemplateController {

    private final QualityTemplateService templateService;
    private final QualityDashboardService dashboardService;

    public QualityTemplateController(QualityTemplateService templateService,
                                     QualityDashboardService dashboardService) {
        this.templateService = templateService;
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "8D 模板列表")
    @GetMapping("/qlt/templates")
    @PreAuthorize("hasAuthority('qlt.template.read')")
    public Result<PageResult<QualityTemplateDtos.TemplateRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(templateService.list(CurrentUser.companyId(), keyword, enabled, page, size));
    }

    @Operation(summary = "8D 模板详情（含步骤）")
    @GetMapping("/qlt/templates/{id}")
    @PreAuthorize("hasAuthority('qlt.template.read')")
    public Result<QualityTemplateDtos.TemplateDetail> detail(@PathVariable Long id) {
        return Result.ok(templateService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建 8D 模板（自动取号 DT-xxxxxx）")
    @PostMapping("/qlt/templates")
    @PreAuthorize("hasAuthority('qlt.template.write')")
    @Audit(module = "QLT", action = "TEMPLATE_CREATE", objectType = "QltTemplate")
    public Result<QualityTemplateDtos.TemplateDetail> create(
            @Valid @RequestBody QualityTemplateDtos.TemplateSaveReq req) {
        return Result.ok(templateService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改 8D 模板基础信息")
    @PutMapping("/qlt/templates/{id}")
    @PreAuthorize("hasAuthority('qlt.template.write')")
    @Audit(module = "QLT", action = "TEMPLATE_UPDATE", objectType = "QltTemplate")
    public Result<QualityTemplateDtos.TemplateDetail> update(
            @PathVariable Long id, @Valid @RequestBody QualityTemplateDtos.TemplateSaveReq req) {
        return Result.ok(templateService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除 8D 模板（软删，已生成的措施不受影响）")
    @DeleteMapping("/qlt/templates/{id}")
    @PreAuthorize("hasAuthority('qlt.template.write')")
    @Audit(module = "QLT", action = "TEMPLATE_DELETE", objectType = "QltTemplate")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "整表替换模板步骤（顺序即执行顺序）")
    @PutMapping("/qlt/templates/{id}/items")
    @PreAuthorize("hasAuthority('qlt.template.write')")
    @Audit(module = "QLT", action = "TEMPLATE_ITEM_REPLACE", objectType = "QltTemplate")
    public Result<QualityTemplateDtos.TemplateDetail> replaceItems(
            @PathVariable Long id,
            @Valid @RequestBody QualityTemplateDtos.TemplateItemsReq req) {
        return Result.ok(templateService.replaceItems(id, req.items(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "应用模板到质量问题（批量生成该轮措施）")
    @PostMapping("/qlt/issues/{id}/apply-template")
    @PreAuthorize("hasAuthority('qlt.issue.write')")
    @Audit(module = "QLT", action = "ISSUE_APPLY_TEMPLATE", objectType = "QltIssue")
    public Result<QualityIssueDtos.IssueDetail> applyTemplate(
            @PathVariable Long id, @Valid @RequestBody QualityTemplateDtos.ApplyTemplateReq req) {
        return Result.ok(templateService.applyToIssue(id, req.templateId(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "质量看板（关闭率为全量快照，趋势按月）")
    @GetMapping("/qlt/dashboard")
    @PreAuthorize("hasAuthority('qlt.issue.read')")
    public Result<QualityTemplateDtos.Dashboard> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return Result.ok(dashboardService.dashboard(CurrentUser.companyId(), from, to));
    }
}
