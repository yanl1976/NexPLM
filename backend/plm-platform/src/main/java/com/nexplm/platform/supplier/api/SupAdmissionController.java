package com.nexplm.platform.supplier.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.supplier.service.SupplierAdmissionService;
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

/** 供应商准入评审：PENDING → QUALIFIED 的唯一入口（P8-5-2）。 */
@Tag(name = "供应商-准入")
@RestController
@RequestMapping("/api/v1")
public class SupAdmissionController {

    private final SupplierAdmissionService admissionService;

    public SupAdmissionController(SupplierAdmissionService admissionService) {
        this.admissionService = admissionService;
    }

    @Operation(summary = "准入评审列表（可按供应商/状态/类型过滤）")
    @GetMapping("/sup/admissions")
    @PreAuthorize("hasAuthority('sup.admission.read')")
    public Result<PageResult<SupAdmissionDtos.AdmissionRow>> page(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(admissionService.page(CurrentUser.companyId(), supplierId, status, type,
                page, size));
    }

    @Operation(summary = "准入评审详情（含检查项）")
    @GetMapping("/sup/admissions/{id}")
    @PreAuthorize("hasAuthority('sup.admission.read')")
    public Result<SupAdmissionDtos.AdmissionDetail> detail(@PathVariable Long id) {
        return Result.ok(admissionService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "发起准入评审（自动取号 ADM-xxxxxx）")
    @PostMapping("/sup/admissions")
    @PreAuthorize("hasAuthority('sup.admission.write')")
    @Audit(module = "SUP", action = "ADMISSION_CREATE", objectType = "SupAdmission")
    public Result<SupAdmissionDtos.AdmissionDetail> create(
            @Valid @RequestBody SupAdmissionDtos.AdmissionSaveReq req) {
        return Result.ok(admissionService.create(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改准入评审草稿（检查项整表替换）")
    @PutMapping("/sup/admissions/{id}")
    @PreAuthorize("hasAuthority('sup.admission.write')")
    @Audit(module = "SUP", action = "ADMISSION_UPDATE", objectType = "SupAdmission")
    public Result<SupAdmissionDtos.AdmissionDetail> update(
            @PathVariable Long id, @Valid @RequestBody SupAdmissionDtos.AdmissionSaveReq req) {
        return Result.ok(admissionService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除准入评审草稿")
    @DeleteMapping("/sup/admissions/{id}")
    @PreAuthorize("hasAuthority('sup.admission.write')")
    @Audit(module = "SUP", action = "ADMISSION_DELETE", objectType = "SupAdmission")
    public Result<Void> delete(@PathVariable Long id) {
        admissionService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "提交准入评审（此后检查项冻结）")
    @PostMapping("/sup/admissions/{id}/submit")
    @PreAuthorize("hasAuthority('sup.admission.write')")
    @Audit(module = "SUP", action = "ADMISSION_SUBMIT", objectType = "SupAdmission")
    public Result<SupAdmissionDtos.AdmissionDetail> submit(@PathVariable Long id) {
        return Result.ok(admissionService.submit(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "通过准入（无 FAIL 且有 PASS，供应商转合格）")
    @PostMapping("/sup/admissions/{id}/approve")
    @PreAuthorize("hasAuthority('sup.admission.write')")
    @Audit(module = "SUP", action = "ADMISSION_APPROVE", objectType = "SupAdmission")
    public Result<SupAdmissionDtos.AdmissionDetail> approve(
            @PathVariable Long id,
            @RequestBody(required = false) SupAdmissionDtos.ApproveReq req) {
        return Result.ok(admissionService.approve(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "驳回准入（记录原因）")
    @PostMapping("/sup/admissions/{id}/reject")
    @PreAuthorize("hasAuthority('sup.admission.write')")
    @Audit(module = "SUP", action = "ADMISSION_REJECT", objectType = "SupAdmission")
    public Result<SupAdmissionDtos.AdmissionDetail> reject(
            @PathVariable Long id, @Valid @RequestBody SupAdmissionDtos.RejectReq req) {
        return Result.ok(admissionService.reject(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "某供应商的全部准入评审")
    @GetMapping("/sup/suppliers/{id}/admissions")
    @PreAuthorize("hasAuthority('sup.admission.read')")
    public Result<List<SupAdmissionDtos.AdmissionRow>> supplierAdmissions(@PathVariable Long id) {
        return Result.ok(admissionService.bySupplier(CurrentUser.companyId(), id));
    }
}
