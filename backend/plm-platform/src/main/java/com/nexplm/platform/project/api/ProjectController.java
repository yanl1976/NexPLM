package com.nexplm.platform.project.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.project.service.ProjectService;
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

import java.util.List;

/** 项目与 EPS（P7-1）。 */
@Tag(name = "项目-项目主档")
@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // ---------------------------------------------------------------- EPS

    @Operation(summary = "EPS 目录树（含各节点直挂项目数）")
    @GetMapping("/prj/eps")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<List<ProjectDtos.EpsNode>> epsTree() {
        return Result.ok(projectService.epsTree(CurrentUser.companyId()));
    }

    @Operation(summary = "新建 EPS 节点（按代码建目录）")
    @PostMapping("/prj/eps")
    @PreAuthorize("hasAuthority('prj.eps.write')")
    @Audit(module = "PRJ", action = "EPS_CREATE", objectType = "PrjEps")
    public Result<ProjectDtos.EpsNode> createEps(@Valid @RequestBody ProjectDtos.EpsSaveReq req) {
        return Result.ok(projectService.createEps(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改 EPS 节点（P7-1 不支持移动节点）")
    @PutMapping("/prj/eps/{id}")
    @PreAuthorize("hasAuthority('prj.eps.write')")
    @Audit(module = "PRJ", action = "EPS_UPDATE", objectType = "PrjEps")
    public Result<ProjectDtos.EpsNode> updateEps(@PathVariable Long id,
                                                 @Valid @RequestBody ProjectDtos.EpsSaveReq req) {
        return Result.ok(projectService.updateEps(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除 EPS 节点（有子节点或项目时 409）")
    @DeleteMapping("/prj/eps/{id}")
    @PreAuthorize("hasAuthority('prj.eps.write')")
    @Audit(module = "PRJ", action = "EPS_DELETE", objectType = "PrjEps")
    public Result<Void> deleteEps(@PathVariable Long id) {
        projectService.deleteEps(id, CurrentUser.companyId());
        return Result.ok();
    }

    // ---------------------------------------------------------------- 项目

    @Operation(summary = "项目列表（按状态/EPS 子树/关键字筛选）")
    @GetMapping("/prj/projects")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<PageResult<ProjectDtos.ProjectRow>> list(@RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Long epsId,
                                                           @RequestParam(defaultValue = "false") boolean includeSubtree,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return Result.ok(projectService.list(CurrentUser.companyId(), keyword, status, epsId,
                includeSubtree, page, size));
    }

    @Operation(summary = "项目详情（含成员清单）")
    @GetMapping("/prj/projects/{id}")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectDtos.ProjectDetail> detail(@PathVariable Long id) {
        return Result.ok(projectService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "立项（新建项目，自动生成 PJ-xxxxxx，状态草稿）")
    @PostMapping("/prj/projects")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_CREATE", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> create(@Valid @RequestBody ProjectDtos.ProjectSaveReq req) {
        return Result.ok(projectService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改项目（已结项不可改）")
    @PutMapping("/prj/projects/{id}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_UPDATE", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> update(@PathVariable Long id,
                                                    @Valid @RequestBody ProjectDtos.ProjectSaveReq req) {
        return Result.ok(projectService.update(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "替换项目成员（整表）")
    @PutMapping("/prj/projects/{id}/members")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_MEMBERS", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> replaceMembers(
            @PathVariable Long id,
            @Valid @RequestBody List<ProjectDtos.MemberSaveReq> members) {
        return Result.ok(projectService.replaceMembers(id, members, CurrentUser.companyId()));
    }

    @Operation(summary = "启动项目（草稿/暂停 → 执行中）")
    @PostMapping("/prj/projects/{id}/activate")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_ACTIVATE", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> activate(@PathVariable Long id) {
        return Result.ok(projectService.activate(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "暂停项目（执行中 → 暂停）")
    @PostMapping("/prj/projects/{id}/pause")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_PAUSE", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> pause(@PathVariable Long id) {
        return Result.ok(projectService.pause(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "项目结项（执行中/暂停 → 已结项）")
    @PostMapping("/prj/projects/{id}/close")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_CLOSE", objectType = "PrjProject")
    public Result<ProjectDtos.ProjectDetail> close(@PathVariable Long id) {
        return Result.ok(projectService.close(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除项目（仅草稿，软删除）")
    @DeleteMapping("/prj/projects/{id}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "PROJECT_DELETE", objectType = "PrjProject")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id, CurrentUser.companyId());
        return Result.ok();
    }
}
