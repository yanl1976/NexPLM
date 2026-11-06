package com.nexplm.platform.item.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.item.service.BomExcelService;
import com.nexplm.platform.item.service.BomService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * BOM 接口：三视图树查询、多层级展开、按子件汇总、行维护、移动、被引用与结构校验。
 */
@Tag(name = "PSE-BOM")
@RestController
@RequestMapping("/api/v1/items/{id}/bom")
public class BomController {

    private final BomService bomService;
    private final BomExcelService bomExcelService;

    public BomController(BomService bomService, BomExcelService bomExcelService) {
        this.bomService = bomService;
        this.bomExcelService = bomExcelService;
    }

    @Operation(summary = "BOM 树（viewType=DESIGN/PROCESS/MANUFACTURE，depth 默认 5，effectAt 按有效性过滤）")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomNode>> tree(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestParam(value = "depth", required = false) Integer depth,
            @RequestParam(value = "effectAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectAt) {
        return Result.ok(bomService.tree(id, viewType, depth, effectAt));
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

    @Operation(summary = "批量改用量/损耗率/单位（只更新传入字段）")
    @PostMapping("/batch/update-qty")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_BATCH_QTY", objectType = "PdmItem")
    public Result<BomDtos.BomBatchResult> batchUpdateQty(@PathVariable Long id,
                                                         @Valid @RequestBody BomDtos.BomBatchQtyReq req) {
        return Result.ok(bomService.batchUpdateQty(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "批量替换子件（该零部件的树内 A → B）")
    @PostMapping("/batch/replace")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_BATCH_REPLACE", objectType = "PdmItem")
    public Result<BomDtos.BomBatchResult> batchReplace(@PathVariable Long id,
                                                       @Valid @RequestBody BomDtos.BomBatchReplaceReq req) {
        return Result.ok(bomService.batchReplace(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "结构校验（用量/单位/孤儿件/循环引用）")
    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<BomDtos.BomValidateResult> validate(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType) {
        return Result.ok(bomService.validate(id, viewType));
    }

    @Operation(summary = "BOM 展开（多层级扁平列表，用量累乘 + 损耗率 + 层级路径）")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomSummaryRow>> summary(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestParam(value = "depth", required = false) Integer depth,
            @RequestParam(value = "effectAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectAt) {
        return Result.ok(bomService.summary(id, viewType, depth, effectAt));
    }

    @Operation(summary = "BOM 汇总（按子件累计全树用量，leafOnly=true 只统计叶子件）")
    @GetMapping("/rollup")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.BomRollupRow>> rollup(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestParam(value = "depth", required = false) Integer depth,
            @RequestParam(value = "effectAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectAt,
            @RequestParam(value = "leafOnly", required = false, defaultValue = "false") boolean leafOnly) {
        return Result.ok(bomService.rollup(id, viewType, depth, effectAt, leafOnly));
    }

    @Operation(summary = "导出 BOM 展开结果（xlsx，层级号可直接回导）")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public ResponseEntity<byte[]> export(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestParam(value = "depth", required = false) Integer depth,
            @RequestParam(value = "effectAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectAt) {
        byte[] content = bomExcelService.export(id, viewType, depth, effectAt);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"bom-" + id + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "BOM 导入预演（解析 + 校验 + 新增/更新分类，不落库）")
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('pse.item.write')")
    public Result<BomDtos.BomExcelPreview> importPreview(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestPart("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return Result.ok(bomExcelService.preview(id, viewType, input));
        }
    }

    @Operation(summary = "BOM 导入执行（增量合并：新增/更新，不删除现有行）")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "BOM_IMPORT", objectType = "PdmItem")
    public Result<BomDtos.BomExcelImportResult> importBom(
            @PathVariable Long id,
            @RequestParam(value = "viewType", required = false) String viewType,
            @RequestPart("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return Result.ok(bomExcelService.apply(id, viewType, input, CurrentUser.userId()));
        }
    }

    @Operation(summary = "被引用（哪些父件使用该零部件）")
    @GetMapping("/where-used")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<BomDtos.WhereUsedRow>> whereUsed(@PathVariable Long id) {
        // 该端点语义是「查询本零部件的上级引用」，故直接用 id 作为子件查询
        return Result.ok(bomService.whereUsed(id));
    }
}
