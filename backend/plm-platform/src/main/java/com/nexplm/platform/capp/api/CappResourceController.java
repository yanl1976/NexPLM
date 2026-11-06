package com.nexplm.platform.capp.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappResourceService;
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

/** 工艺资源库（P6 任务 3）：分类树 / 资源 / 引用统计 / 保存的查询条件。 */
@Tag(name = "工艺-资源库")
@RestController
@RequestMapping("/api/v1")
public class CappResourceController {

    private final CappResourceService resourceService;

    public CappResourceController(CappResourceService resourceService) {
        this.resourceService = resourceService;
    }

    // ---------------------------------------------------------------- 分类

    @Operation(summary = "资源分类列表（扁平，含资源数）")
    @GetMapping("/capp/resource-classes")
    @PreAuthorize("hasAuthority('capp.resource.read')")
    public Result<List<CappResourceDtos.ClassRow>> classes() {
        return Result.ok(resourceService.classList(CurrentUser.companyId()));
    }

    @Operation(summary = "新建资源分类")
    @PostMapping("/capp/resource-classes")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_CLASS_CREATE", objectType = "CappResourceClass")
    public Result<CappResourceDtos.ClassRow> createClass(@Valid @RequestBody CappResourceDtos.ClassSaveReq req) {
        return Result.ok(resourceService.createClass(req, CurrentUser.companyId()));
    }

    @Operation(summary = "修改资源分类")
    @PutMapping("/capp/resource-classes/{id}")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_CLASS_UPDATE", objectType = "CappResourceClass")
    public Result<CappResourceDtos.ClassRow> updateClass(@PathVariable Long id,
                                                        @Valid @RequestBody CappResourceDtos.ClassSaveReq req) {
        return Result.ok(resourceService.updateClass(id, req, CurrentUser.companyId()));
    }

    @Operation(summary = "删除资源分类（需无子分类、无资源）")
    @DeleteMapping("/capp/resource-classes/{id}")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_CLASS_DELETE", objectType = "CappResourceClass")
    public Result<Void> deleteClass(@PathVariable Long id) {
        resourceService.deleteClass(id, CurrentUser.companyId());
        return Result.ok();
    }

    // ---------------------------------------------------------------- 资源

    @Operation(summary = "资源列表（分类/关键字/状态/动态属性筛选）")
    @GetMapping("/capp/resources")
    @PreAuthorize("hasAuthority('capp.resource.read')")
    public Result<PageResult<CappResourceDtos.ResourceRow>> resources(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String attrs,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(resourceService.listResources(CurrentUser.companyId(), classId, keyword, status,
                attrs, page, size));
    }

    @Operation(summary = "资源详情（含引用数）")
    @GetMapping("/capp/resources/{id}")
    @PreAuthorize("hasAuthority('capp.resource.read')")
    public Result<CappResourceDtos.ResourceRow> resource(@PathVariable Long id) {
        return Result.ok(resourceService.resource(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建资源（动态属性键值对）")
    @PostMapping("/capp/resources")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_CREATE", objectType = "CappResource")
    public Result<CappResourceDtos.ResourceRow> createResource(
            @Valid @RequestBody CappResourceDtos.ResourceSaveReq req) {
        return Result.ok(resourceService.createResource(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改资源")
    @PutMapping("/capp/resources/{id}")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_UPDATE", objectType = "CappResource")
    public Result<CappResourceDtos.ResourceRow> updateResource(
            @PathVariable Long id, @Valid @RequestBody CappResourceDtos.ResourceSaveReq req) {
        return Result.ok(resourceService.updateResource(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除资源（有工序引用时禁止）")
    @DeleteMapping("/capp/resources/{id}")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_DELETE", objectType = "CappResource")
    public Result<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "资源引用统计（被哪些工序使用）")
    @GetMapping("/capp/resources/{id}/references")
    @PreAuthorize("hasAuthority('capp.resource.read')")
    public Result<List<CappResourceDtos.ResourceReferenceRow>> references(@PathVariable Long id) {
        return Result.ok(resourceService.references(id, CurrentUser.companyId()));
    }

    // ---------------------------------------------------------------- 保存的查询条件

    @Operation(summary = "已保存的查询条件")
    @GetMapping("/capp/resource-queries")
    @PreAuthorize("hasAuthority('capp.resource.read')")
    public Result<List<CappResourceDtos.QueryRow>> queries() {
        return Result.ok(resourceService.queries(CurrentUser.companyId()));
    }

    @Operation(summary = "保存查询条件（同名覆盖）")
    @PostMapping("/capp/resource-queries")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_QUERY_SAVE", objectType = "CappResourceQuery")
    public Result<CappResourceDtos.QueryRow> saveQuery(
            @Valid @RequestBody CappResourceDtos.QuerySaveReq req) {
        return Result.ok(resourceService.saveQuery(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除查询条件")
    @DeleteMapping("/capp/resource-queries/{id}")
    @PreAuthorize("hasAuthority('capp.resource.write')")
    @Audit(module = "CAPP", action = "RESOURCE_QUERY_DELETE", objectType = "CappResourceQuery")
    public Result<Void> deleteQuery(@PathVariable Long id) {
        resourceService.deleteQuery(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "执行保存的查询条件")
    @GetMapping("/capp/resource-queries/{id}/run")
    @PreAuthorize("hasAuthority('capp.resource.read')")
    public Result<PageResult<CappResourceDtos.ResourceRow>> runQuery(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(resourceService.runQuery(id, CurrentUser.companyId(), page, size));
    }
}
