package com.nexplm.platform.project.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.project.service.ProjectTemplateService;
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

/** 项目模板与从模板立项（P7-6）。 */
@Tag(name = "项目-模板与立项")
@RestController
@RequestMapping("/api/v1")
public class ProjectTemplateController {

    private final ProjectTemplateService templateService;

    public ProjectTemplateController(ProjectTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "模板列表（带任务/目录数量）")
    @GetMapping("/prj/templates")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<PageResult<ProjectTemplateDtos.TemplateRow>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(templateService.list(CurrentUser.companyId(), keyword, enabled, page, size));
    }

    @Operation(summary = "模板详情（任务骨架 + 目录骨架）")
    @GetMapping("/prj/templates/{id}")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectTemplateDtos.TemplateDetail> detail(@PathVariable Long id) {
        return Result.ok(templateService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建模板（自动取号 PT-000001）")
    @PostMapping("/prj/templates")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TEMPLATE_CREATE", objectType = "PrjTemplate")
    public Result<ProjectTemplateDtos.TemplateDetail> create(
            @Valid @RequestBody ProjectTemplateDtos.TemplateSaveReq req) {
        return Result.ok(templateService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改模板主档")
    @PutMapping("/prj/templates/{id}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TEMPLATE_UPDATE", objectType = "PrjTemplate")
    public Result<ProjectTemplateDtos.TemplateDetail> update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectTemplateDtos.TemplateSaveReq req) {
        return Result.ok(templateService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除模板（软删，明细物理删）")
    @DeleteMapping("/prj/templates/{id}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TEMPLATE_DELETE", objectType = "PrjTemplate")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "任务骨架整表替换（编码按数组顺序生成）")
    @PutMapping("/prj/templates/{id}/tasks")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TEMPLATE_TASK_REPLACE", objectType = "PrjTemplate")
    public Result<ProjectTemplateDtos.TemplateDetail> replaceTasks(
            @PathVariable Long id,
            @Valid @RequestBody ProjectTemplateDtos.TemplateTasksReq req) {
        return Result.ok(templateService.replaceTasks(id, req.tasks(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "目录骨架整表替换（父路径先声明）")
    @PutMapping("/prj/templates/{id}/folders")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TEMPLATE_FOLDER_REPLACE", objectType = "PrjTemplate")
    public Result<ProjectTemplateDtos.TemplateDetail> replaceFolders(
            @PathVariable Long id,
            @Valid @RequestBody ProjectTemplateDtos.TemplateFoldersReq req) {
        return Result.ok(templateService.replaceFolders(id, req.folders(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "从模板立项（生成项目 + WBS 任务 + 文档目录）")
    @PostMapping("/prj/projects/from-template")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_FROM_TEMPLATE", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> fromTemplate(
            @Valid @RequestBody ProjectTemplateDtos.FromTemplateReq req) {
        return Result.ok(templateService.instantiate(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }
}
