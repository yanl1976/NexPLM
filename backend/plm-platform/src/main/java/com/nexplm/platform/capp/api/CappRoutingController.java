package com.nexplm.platform.capp.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappRoutingService;
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

/** 工艺路线（P6 任务 1）。 */
@Tag(name = "工艺-工艺路线")
@RestController
@RequestMapping("/api/v1")
public class CappRoutingController {

    private final CappRoutingService routingService;

    public CappRoutingController(CappRoutingService routingService) {
        this.routingService = routingService;
    }

    @Operation(summary = "工艺路线列表（按零件/状态/关键字筛选）")
    @GetMapping("/capp/routings")
    @PreAuthorize("hasAuthority('capp.routing.read')")
    public Result<PageResult<CappDtos.RoutingRow>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long objectId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return Result.ok(routingService.list(CurrentUser.companyId(), keyword, objectId, status, page, size));
    }

    @Operation(summary = "工艺路线详情（含工序与路线串）")
    @GetMapping("/capp/routings/{id}")
    @PreAuthorize("hasAuthority('capp.routing.read')")
    public Result<CappDtos.RoutingDetail> detail(@PathVariable Long id) {
        return Result.ok(routingService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "工艺过程树（工序 → 工步，与工序表同源）")
    @GetMapping("/capp/routings/{id}/process-tree")
    @PreAuthorize("hasAuthority('capp.routing.read')")
    public Result<java.util.List<CappDtos.ProcessTreeNode>> processTree(@PathVariable Long id) {
        return Result.ok(routingService.processTree(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建工艺路线（自动生成 RT-xxxxxx）")
    @PostMapping("/capp/routings")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_CREATE", objectType = "CappRouting")
    public Result<CappDtos.RoutingDetail> create(@Valid @RequestBody CappDtos.RoutingSaveReq req) {
        return Result.ok(routingService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改工艺路线（仅草稿；工序整表替换）")
    @PutMapping("/capp/routings/{id}")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_UPDATE", objectType = "CappRouting")
    public Result<CappDtos.RoutingDetail> update(@PathVariable Long id,
                                                 @Valid @RequestBody CappDtos.RoutingSaveReq req) {
        return Result.ok(routingService.update(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "发布工艺路线（同零件其它已发布自动归档）")
    @PostMapping("/capp/routings/{id}/release")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_RELEASE", objectType = "CappRouting")
    public Result<CappDtos.RoutingDetail> release(@PathVariable Long id) {
        return Result.ok(routingService.release(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "归档工艺路线（已发布 → 已归档）")
    @PostMapping("/capp/routings/{id}/archive")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_ARCHIVE", objectType = "CappRouting")
    public Result<CappDtos.RoutingDetail> archive(@PathVariable Long id) {
        return Result.ok(routingService.archive(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "工序序号重建（按当前顺序重排 10/20/30…，仅草稿）")
    @PostMapping("/capp/routings/{id}/resequence")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_RESEQUENCE", objectType = "CappRouting")
    public Result<CappDtos.RoutingDetail> resequence(@PathVariable Long id) {
        return Result.ok(routingService.resequence(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "复制为新草稿（保留原路线，版本号 +1）")
    @PostMapping("/capp/routings/{id}/copy")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_COPY", objectType = "CappRouting")
    public Result<CappDtos.RoutingDetail> copy(@PathVariable Long id) {
        return Result.ok(routingService.copy(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除工艺路线（仅草稿，软删除）")
    @DeleteMapping("/capp/routings/{id}")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ROUTING_DELETE", objectType = "CappRouting")
    public Result<Void> delete(@PathVariable Long id) {
        routingService.delete(id, CurrentUser.companyId());
        return Result.ok();
    }
}
