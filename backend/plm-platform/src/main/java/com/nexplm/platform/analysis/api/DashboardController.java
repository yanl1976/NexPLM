package com.nexplm.platform.analysis.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.platform.analysis.service.DashboardService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * P10-1 统计看板：六域指标卡 + 月度趋势 + 下钻明细。
 *
 * <p>只读聚合，不改任何业务数据，故只有 read 权限、无审计注解。</p>
 */
@Tag(name = "分析-统计看板")
@RestController
@RequestMapping("/api/v1/ana/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "六域总览（指标卡 + 月度趋势）")
    @GetMapping
    @PreAuthorize("hasAuthority('ana.dashboard.read')")
    public Result<DashboardDtos.Overview> overview(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(dashboardService.overview(CurrentUser.companyId(), from, to));
    }

    @Operation(summary = "下钻明细（按状态筛选，六域统一字段）")
    @GetMapping("/{domain}/items")
    @PreAuthorize("hasAuthority('ana.dashboard.read')")
    public Result<PageResult<DashboardDtos.DrillRow>> items(
            @PathVariable String domain,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(dashboardService.items(CurrentUser.companyId(), domain, status, page, size));
    }
}
