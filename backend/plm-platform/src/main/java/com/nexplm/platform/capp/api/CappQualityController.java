package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappQualityExcelService;
import com.nexplm.platform.capp.service.CappQualityService;
import com.nexplm.platform.security.CurrentUser;
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

/** 质量规划（P6-8 简化版）：检验项字典 + 控制计划表。 */
@Tag(name = "工艺-质量规划")
@RestController
@RequestMapping("/api/v1")
public class CappQualityController {

    private final CappQualityService qualityService;
    private final CappQualityExcelService excelService;

    public CappQualityController(CappQualityService qualityService,
                                 CappQualityExcelService excelService) {
        this.qualityService = qualityService;
        this.excelService = excelService;
    }

    // ---------------------------------------------------------------- 检验项字典

    @Operation(summary = "检验项字典列表（按类别/关键字筛选）")
    @GetMapping("/capp/quality-items")
    @PreAuthorize("hasAuthority('capp.quality.read')")
    public Result<List<CappQualityDtos.QualityItemRow>> itemList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.ok(qualityService.itemList(CurrentUser.companyId(), keyword, category));
    }

    @Operation(summary = "新建检验项（自动生成 QI-xxxxxx）")
    @PostMapping("/capp/quality-items")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_ITEM_CREATE", objectType = "CappQualityItem")
    public Result<CappQualityDtos.QualityItemRow> createItem(
            @Valid @RequestBody CappQualityDtos.QualityItemSaveReq req) {
        return Result.ok(qualityService.createItem(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改检验项")
    @PutMapping("/capp/quality-items/{id}")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_ITEM_UPDATE", objectType = "CappQualityItem")
    public Result<CappQualityDtos.QualityItemRow> updateItem(
            @PathVariable Long id, @Valid @RequestBody CappQualityDtos.QualityItemSaveReq req) {
        return Result.ok(qualityService.updateItem(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除检验项（被控制计划引用时拒绝）")
    @DeleteMapping("/capp/quality-items/{id}")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_ITEM_DELETE", objectType = "CappQualityItem")
    public Result<Void> deleteItem(@PathVariable Long id) {
        qualityService.deleteItem(id, CurrentUser.companyId());
        return Result.ok();
    }

    // ---------------------------------------------------------------- 控制计划

    @Operation(summary = "控制计划列表（按零件/路线/状态/关键字筛选）")
    @GetMapping("/capp/quality-plans")
    @PreAuthorize("hasAuthority('capp.quality.read')")
    public Result<List<CappQualityDtos.PlanRowView>> planList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long objectId,
            @RequestParam(required = false) Long routingId,
            @RequestParam(required = false) String status) {
        return Result.ok(qualityService.planList(CurrentUser.companyId(), keyword, objectId,
                routingId, status));
    }

    @Operation(summary = "控制计划详情（含行）")
    @GetMapping("/capp/quality-plans/{id}")
    @PreAuthorize("hasAuthority('capp.quality.read')")
    public Result<CappQualityDtos.PlanDetail> plan(@PathVariable Long id) {
        return Result.ok(qualityService.plan(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建控制计划（rows 为空时按路线工序自动生成骨架行）")
    @PostMapping("/capp/quality-plans")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_PLAN_CREATE", objectType = "CappQualityPlan")
    public Result<CappQualityDtos.PlanDetail> createPlan(
            @Valid @RequestBody CappQualityDtos.PlanSaveReq req) {
        return Result.ok(qualityService.createPlan(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改控制计划（仅草稿；行整表替换）")
    @PutMapping("/capp/quality-plans/{id}")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_PLAN_UPDATE", objectType = "CappQualityPlan")
    public Result<CappQualityDtos.PlanDetail> updatePlan(
            @PathVariable Long id, @Valid @RequestBody CappQualityDtos.PlanSaveReq req) {
        return Result.ok(qualityService.updatePlan(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "发布控制计划（仅草稿；存在未填特性的行时拒绝）")
    @PostMapping("/capp/quality-plans/{id}/release")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_PLAN_RELEASE", objectType = "CappQualityPlan")
    public Result<CappQualityDtos.PlanDetail> releasePlan(@PathVariable Long id) {
        return Result.ok(qualityService.releasePlan(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除控制计划（仅草稿）")
    @DeleteMapping("/capp/quality-plans/{id}")
    @PreAuthorize("hasAuthority('capp.quality.write')")
    @Audit(module = "CAPP", action = "QUALITY_PLAN_DELETE", objectType = "CappQualityPlan")
    public Result<Void> deletePlan(@PathVariable Long id) {
        qualityService.deletePlan(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "导出控制计划 Excel（13 列控制计划表）")
    @GetMapping("/capp/quality-plans/{id}/export")
    @PreAuthorize("hasAuthority('capp.quality.read')")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        CappQualityDtos.PlanDetail plan = qualityService.plan(id, CurrentUser.companyId());
        byte[] content = excelService.export(plan);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"control-plan-" + plan.code() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
