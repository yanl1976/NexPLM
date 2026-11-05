package com.nexplm.platform.item.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.item.service.BomService;
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
 * BOM 接口：三视图树查询、行维护、移动、被引用与结构校验。
 */
@Tag(name = "PSE-BOM")
@RestController
@RequestMapping("/api/v1/items/{id}/bom")
public class BomController {

    private final BomService bomService;

    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @Operation(summary = "BOM 树（viewType=DESIGN/PROCESS/MANUFACTURE，depth 默认 5）")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomNode>> tree(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestParam(value = "depth", required = false) Integer depth) {
        return Result.ok(bomService.tree(id, viewType, depth));
    }

    @Operation(summary = "添加子件")
    @PostMapping
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_ADD", objectType = "PdmItem")
    public Result<BomDtos.BomNode> add(@PathVariable Long id,
                                       @RequestParam(value = "viewType", required = false) String viewType,
                                       @Valid @RequestBody BomDtos.BomLineSaveReq req) {
        return Result.ok(bomService.add(id, viewType, req, CurrentUser.userId()));
    }

    @Operation(summary = "修改 BOM 行（用量/单位/损耗率/位号/备注）")
    @PutMapping("/lines/{lineId}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_UPDATE", objectType = "PdmItem")
    public Result<BomDtos.BomNode> update(@PathVariable Long id, @PathVariable Long lineId,
                                          @Valid @RequestBody BomDtos.BomLineUpdateReq req) {
        return Result.ok(bomService.update(lineId, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除 BOM 行")
    @DeleteMapping("/lines/{lineId}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_DELETE", objectType = "PdmItem")
    public Result<Void> remove(@PathVariable Long id, @PathVariable Long lineId) {
        bomService.remove(lineId);
        return Result.ok();
    }

    @Operation(summary = "移动 BOM 行（换父件或调整序号）")
    @PostMapping("/lines/{lineId}/move")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_MOVE", objectType = "PdmItem")
    public Result<BomDtos.BomNode> move(@PathVariable Long id, @PathVariable Long lineId,
                                        @RequestBody BomDtos.BomMoveReq req) {
        return Result.ok(bomService.move(lineId, req, CurrentUser.userId()));
    }

    @Operation(summary = "结构校验（用量/单位/孤儿件/循环引用）")
    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<BomDtos.BomValidateResult> validate(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType) {
        return Result.ok(bomService.validate(id, viewType));
    }

    @Operation(summary = "被引用（哪些父件使用该零部件）")
    @GetMapping("/where-used")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.WhereUsedRow>> whereUsed(@PathVariable Long id) {
        // 该端点语义是「查询本零部件的上级引用」，故直接用 id 作为子件查询
        return Result.ok(bomService.whereUsed(id));
    }
}
