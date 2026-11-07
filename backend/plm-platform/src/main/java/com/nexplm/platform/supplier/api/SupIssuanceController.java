package com.nexplm.platform.supplier.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.supplier.service.SupplierIssuanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 供应商数据发放：受控清单 + 打包导出 + 回执登记（P8-5-3）。 */
@Tag(name = "供应商-数据发放")
@RestController
@RequestMapping("/api/v1")
public class SupIssuanceController {

    private final SupplierIssuanceService issuanceService;

    public SupIssuanceController(SupplierIssuanceService issuanceService) {
        this.issuanceService = issuanceService;
    }

    @Operation(summary = "发放单列表（可按供应商/状态/类型过滤）")
    @GetMapping("/sup/issuances")
    @PreAuthorize("hasAuthority('sup.issuance.read')")
    public Result<PageResult<SupIssuanceDtos.IssuanceRow>> page(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(issuanceService.page(CurrentUser.companyId(), supplierId, status, type,
                page, size));
    }

    @Operation(summary = "发放单详情（含清单快照）")
    @GetMapping("/sup/issuances/{id}")
    @PreAuthorize("hasAuthority('sup.issuance.read')")
    public Result<SupIssuanceDtos.IssuanceDetail> detail(@PathVariable Long id) {
        return Result.ok(issuanceService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建发放单（自动取号 ISS-xxxxxx）")
    @PostMapping("/sup/issuances")
    @PreAuthorize("hasAuthority('sup.issuance.write')")
    @Audit(module = "SUP", action = "ISSUANCE_CREATE", objectType = "SupIssuance")
    public Result<SupIssuanceDtos.IssuanceDetail> create(
            @Valid @RequestBody SupIssuanceDtos.IssuanceSaveReq req) {
        return Result.ok(issuanceService.create(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改发放单草稿（清单整表替换）")
    @PutMapping("/sup/issuances/{id}")
    @PreAuthorize("hasAuthority('sup.issuance.write')")
    @Audit(module = "SUP", action = "ISSUANCE_UPDATE", objectType = "SupIssuance")
    public Result<SupIssuanceDtos.IssuanceDetail> update(
            @PathVariable Long id, @Valid @RequestBody SupIssuanceDtos.IssuanceSaveReq req) {
        return Result.ok(issuanceService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除发放单草稿")
    @DeleteMapping("/sup/issuances/{id}")
    @PreAuthorize("hasAuthority('sup.issuance.write')")
    @Audit(module = "SUP", action = "ISSUANCE_DELETE", objectType = "SupIssuance")
    public Result<Void> delete(@PathVariable Long id) {
        issuanceService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "发出（草稿 → 已发放，此后清单冻结）")
    @PostMapping("/sup/issuances/{id}/send")
    @PreAuthorize("hasAuthority('sup.issuance.write')")
    @Audit(module = "SUP", action = "ISSUANCE_SEND", objectType = "SupIssuance")
    public Result<SupIssuanceDtos.IssuanceDetail> send(@PathVariable Long id) {
        return Result.ok(issuanceService.send(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "登记回执（文件先经 /files 上传，再登记 fileId）")
    @PostMapping("/sup/issuances/{id}/receipt")
    @PreAuthorize("hasAuthority('sup.issuance.write')")
    @Audit(module = "SUP", action = "ISSUANCE_RECEIPT", objectType = "SupIssuance")
    public Result<SupIssuanceDtos.IssuanceDetail> receipt(
            @PathVariable Long id, @Valid @RequestBody SupIssuanceDtos.ReceiptReq req) {
        return Result.ok(issuanceService.receipt(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "标记已读（进入详情即已读，旧系统 ReadState 等价物）")
    @PostMapping("/sup/issuances/{id}/read")
    @PreAuthorize("hasAuthority('sup.issuance.read')")
    public Result<Void> markRead(@PathVariable Long id) {
        issuanceService.markRead(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "置/取消重要标记")
    @PostMapping("/sup/issuances/{id}/important")
    @PreAuthorize("hasAuthority('sup.issuance.read')")
    public Result<Void> setImportant(@PathVariable Long id, @RequestParam boolean important) {
        issuanceService.setImportant(id, CurrentUser.companyId(), important);
        return Result.ok(null);
    }

    @Operation(summary = "协同回复（多轮，可带附件与逐条确认）")
    @PostMapping("/sup/issuances/{id}/replies")
    @PreAuthorize("hasAuthority('sup.coordination.write')")
    @Audit(module = "SUP", action = "ISSUANCE_REPLY", objectType = "SupIssuance")
    public Result<SupIssuanceDtos.IssuanceDetail> reply(
            @PathVariable Long id, @Valid @RequestBody SupIssuanceDtos.ReplyReq req) {
        return Result.ok(issuanceService.reply(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "附件已拉取标记（旧系统 PULLSTATE）")
    @PostMapping("/sup/issuances/{id}/items/{itemId}/pull")
    @PreAuthorize("hasAuthority('sup.coordination.write')")
    public Result<Void> markItemPulled(@PathVariable Long id, @PathVariable Long itemId) {
        issuanceService.markItemPulled(id, itemId, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "打包导出（manifest.csv + 零件主文件）")
    @PostMapping(value = "/sup/issuances/{id}/export", produces = "application/zip")
    @PreAuthorize("hasAuthority('sup.issuance.read')")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] zip = issuanceService.exportZip(id, CurrentUser.companyId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"issuance.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zip.length)
                .body(zip);
    }

    @Operation(summary = "某供应商的全部发放单")
    @GetMapping("/sup/suppliers/{id}/issuances")
    @PreAuthorize("hasAuthority('sup.issuance.read')")
    public Result<List<SupIssuanceDtos.IssuanceRow>> supplierIssuances(@PathVariable Long id) {
        return Result.ok(issuanceService.bySupplier(CurrentUser.companyId(), id));
    }
}
