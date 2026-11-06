package com.nexplm.platform.item.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.item.service.BomAlternateService;
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

/**
 * BOM 替代件接口：备选件的增删改查。
 * <p>语义对齐旧系统 {@code ALTERNATESRELATION}：{id} 是被替换件（须已出现在 BOM 中），
 * 备选件只是候选，不会被自动写入 BOM 结构。</p>
 */
@Tag(name = "PSE-BOM")
@RestController
@RequestMapping("/api/v1/items/{id}/alternates")
public class BomAlternateController {

    private final BomAlternateService alternateService;

    public BomAlternateController(BomAlternateService alternateService) {
        this.alternateService = alternateService;
    }

    @Operation(summary = "备选件列表（viewType 非空时按 BOM 视角过滤，全局替代均可见）")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomAlternateRow>> list(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType) {
        return Result.ok(alternateService.list(id, viewType));
    }

    @Operation(summary = "添加备选件")
    @PostMapping
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_ALTERNATE_ADD", objectType = "PdmItem")
    public Result<BomDtos.BomAlternateRow> add(@PathVariable Long id,
                                               @Valid @RequestBody BomDtos.BomAlternateSaveReq req) {
        return Result.ok(alternateService.add(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "修改备选件（全局/双向/优先级/排序/备注）")
    @PutMapping("/{alternateRowId}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_ALTERNATE_UPDATE", objectType = "PdmItem")
    public Result<BomDtos.BomAlternateRow> update(@PathVariable Long id, @PathVariable Long alternateRowId,
                                                  @Valid @RequestBody BomDtos.BomAlternateUpdateReq req) {
        return Result.ok(alternateService.update(id, alternateRowId, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除备选件")
    @DeleteMapping("/{alternateRowId}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_ALTERNATE_DELETE", objectType = "PdmItem")
    public Result<Void> remove(@PathVariable Long id, @PathVariable Long alternateRowId) {
        alternateService.remove(id, alternateRowId);
        return Result.ok();
    }
}
