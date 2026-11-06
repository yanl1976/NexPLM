package com.nexplm.platform.supplier.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.supplier.service.SupplierService;
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

/** 供应商主数据与零件-供应商关系（P8-4）。 */
@Tag(name = "供应商-主数据")
@RestController
@RequestMapping("/api/v1")
public class SupSupplierController {

    private final SupplierService supplierService;

    public SupSupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @Operation(summary = "供应商列表（可按类别/状态/关键字过滤）")
    @GetMapping("/sup/suppliers")
    @PreAuthorize("hasAuthority('sup.supplier.read')")
    public Result<PageResult<SupSupplierDtos.SupplierRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(supplierService.page(CurrentUser.companyId(), keyword, category, status,
                page, size));
    }

    @Operation(summary = "供应商详情（含零件关系）")
    @GetMapping("/sup/suppliers/{id}")
    @PreAuthorize("hasAuthority('sup.supplier.read')")
    public Result<SupSupplierDtos.SupplierDetail> detail(@PathVariable Long id) {
        return Result.ok(supplierService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建供应商（自动取号 SUP-xxxxxx）")
    @PostMapping("/sup/suppliers")
    @PreAuthorize("hasAuthority('sup.supplier.write')")
    @Audit(module = "SUP", action = "SUPPLIER_CREATE", objectType = "SupSupplier")
    public Result<SupSupplierDtos.SupplierDetail> create(
            @Valid @RequestBody SupSupplierDtos.SupplierSaveReq req) {
        return Result.ok(supplierService.create(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改供应商")
    @PutMapping("/sup/suppliers/{id}")
    @PreAuthorize("hasAuthority('sup.supplier.write')")
    @Audit(module = "SUP", action = "SUPPLIER_UPDATE", objectType = "SupSupplier")
    public Result<SupSupplierDtos.SupplierDetail> update(
            @PathVariable Long id, @Valid @RequestBody SupSupplierDtos.SupplierSaveReq req) {
        return Result.ok(supplierService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除供应商（有在用零件关系时拒绝）")
    @DeleteMapping("/sup/suppliers/{id}")
    @PreAuthorize("hasAuthority('sup.supplier.write')")
    @Audit(module = "SUP", action = "SUPPLIER_DELETE", objectType = "SupSupplier")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "新增零件关系（同一零件多供应商；首选唯一）")
    @PostMapping("/sup/suppliers/{id}/parts")
    @PreAuthorize("hasAuthority('sup.supplier.write')")
    @Audit(module = "SUP", action = "SUPPLIER_PART_ADD", objectType = "SupSupplier")
    public Result<SupSupplierDtos.SupplierDetail> addPart(
            @PathVariable Long id, @Valid @RequestBody SupSupplierDtos.PartSaveReq req) {
        return Result.ok(supplierService.addPart(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改零件关系")
    @PutMapping("/sup/parts/{partId}")
    @PreAuthorize("hasAuthority('sup.supplier.write')")
    @Audit(module = "SUP", action = "SUPPLIER_PART_UPDATE", objectType = "SupSupplier")
    public Result<SupSupplierDtos.SupplierDetail> updatePart(
            @PathVariable Long partId, @Valid @RequestBody SupSupplierDtos.PartSaveReq req) {
        return Result.ok(supplierService.updatePart(partId, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除零件关系（软删）")
    @DeleteMapping("/sup/parts/{partId}")
    @PreAuthorize("hasAuthority('sup.supplier.write')")
    @Audit(module = "SUP", action = "SUPPLIER_PART_REMOVE", objectType = "SupSupplier")
    public Result<Void> removePart(@PathVariable Long partId) {
        supplierService.removePart(partId, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "按零件反查供应商（多源询价）")
    @GetMapping("/sup/parts/by-item")
    @PreAuthorize("hasAuthority('sup.supplier.read')")
    public Result<List<SupSupplierDtos.PartRow>> partsByItem(
            @RequestParam(required = false) String objectType,
            @RequestParam Long objectId) {
        return Result.ok(supplierService.partsByItem(CurrentUser.companyId(), objectType, objectId));
    }
}
