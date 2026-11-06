package com.nexplm.platform.project.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.project.service.ProjectPortalService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 项目门户三视图（P7-8）：我的项目 / 我管理的项目 / 公司组合。 */
@Tag(name = "项目-门户")
@RestController
@RequestMapping("/api/v1")
public class ProjectPortalController {

    private final ProjectPortalService portalService;

    public ProjectPortalController(ProjectPortalService portalService) {
        this.portalService = portalService;
    }

    @Operation(summary = "我的项目（我是经理或成员，含我的任务数/待办数）")
    @GetMapping("/prj/portal/my-projects")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<List<ProjectPortalDtos.PortalProjectCard>> myProjects() {
        return Result.ok(portalService.myProjects(CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "我管理的项目（进度 vs 时间进度 + 逾期任务 + 待审工时）")
    @GetMapping("/prj/portal/managed-projects")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<List<ProjectPortalDtos.ManagedProjectRow>> managedProjects() {
        return Result.ok(portalService.managedProjects(CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "公司组合（状态分布 + 平均进度 + 逾期项目 + 风险清单）")
    @GetMapping("/prj/portal/portfolio")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectPortalDtos.PortfolioOverview> portfolio() {
        return Result.ok(portalService.portfolio(CurrentUser.companyId()));
    }
}
