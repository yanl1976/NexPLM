package com.nexplm.platform.item.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.item.service.BomBaselineService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * BOM 基线接口：冻结当前结构快照，并与当前结构做差异对比。
 */
@Tag(name = "PSE-BOM")
@RestController
@RequestMapping("/api/v1/items/{id}/baselines")
public class BomBaselineController {

    private final BomBaselineService baselineService;

    public BomBaselineController(BomBaselineService baselineService) {
        this.baselineService = baselineService;
    }

    @Operation(summary = "基线列表（可按视图过滤，按冻结时间倒序）")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomBaselineRow>> list(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType) {
        return Result.ok(baselineService.list(id, viewType));
    }

    @Operation(summary = "创建基线（冻结当前视图下的 BOM 结构）")
    @PostMapping
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_BASELINE_CREATE", objectType = "PdmItem")
    public Result<BomDtos.BomBaselineRow> create(@PathVariable Long id,
                                                 @Valid @RequestBody BomDtos.BomBaselineSaveReq req) {
        return Result.ok(baselineService.create(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "基线快照行")
    @GetMapping("/{baselineId}/lines")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomBaselineLineRow>> lines(@PathVariable Long id, @PathVariable Long baselineId) {
        return Result.ok(baselineService.lines(id, baselineId));
    }

    @Operation(summary = "基线与当前结构差异（新增/删除/用量变更/子件替换）")
    @GetMapping("/{baselineId}/diff")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<BomDtos.BomBaselineDiff> diff(
            @PathVariable Long id,
            @PathVariable Long baselineId,
            @RequestParam(value = "effectAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectAt,
            @RequestParam(value = "depth", required = false) Integer depth) {
        return Result.ok(baselineService.diffWithCurrent(id, baselineId, effectAt, depth));
    }

    @Operation(summary = "删除基线（快照行保留留档）")
    @DeleteMapping("/{baselineId}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_BASELINE_DELETE", objectType = "PdmItem")
    public Result<Void> delete(@PathVariable Long id, @PathVariable Long baselineId) {
        baselineService.delete(id, baselineId);
        return Result.ok();
    }
}
