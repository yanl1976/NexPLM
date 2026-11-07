package com.nexplm.platform.integration.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.integration.service.ErpMappingService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ERP 三段字段映射（批次 G）：物料 / BOM / 请购单下发的字段对照，可编辑、可试算。
 *
 * <p>权限沿用集成配置：这是"集成怎么配"的一部分，不新开权限点。
 * 试算按只读权限（{@code itg.config.read}）开放——它不落任务、不发送，只是看报文。</p>
 */
@Tag(name = "集成-ERP 字段映射")
@RestController
@RequestMapping("/api/v1/itg/erp-mapping")
public class ErpMappingController {

    private final ErpMappingService erpMappingService;

    public ErpMappingController(ErpMappingService erpMappingService) {
        this.erpMappingService = erpMappingService;
    }

    @Operation(summary = "三段概览（列数 / 已映射 / 缺口 / 待确认）")
    @GetMapping("/segments")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<List<IntegrationDtos.ErpSegmentRow>> segments() {
        return Result.ok(erpMappingService.segments(CurrentUser.companyId()));
    }

    @Operation(summary = "某一段的字段映射明细")
    @GetMapping("/{objectCode}")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<IntegrationDtos.ErpMappingDetail> detail(@PathVariable String objectCode) {
        return Result.ok(erpMappingService.detail(objectCode, CurrentUser.companyId()));
    }

    @Operation(summary = "保存某一段的字段映射（整段替换）")
    @PutMapping("/{objectCode}")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "ERP_MAPPING_SAVE", objectType = "ItgMapping")
    public Result<IntegrationDtos.ErpMappingDetail> save(
            @PathVariable String objectCode,
            @Valid @RequestBody IntegrationDtos.ErpMappingSaveReq req) {
        return Result.ok(erpMappingService.save(objectCode, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "按下发对象试算：组装金蝶报文并列出缺口（不落任务、不发送）")
    @PostMapping("/{objectCode}/preview")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<IntegrationDtos.ErpPreviewResult> preview(
            @PathVariable String objectCode,
            @Valid @RequestBody IntegrationDtos.ErpPreviewReq req) {
        return Result.ok(erpMappingService.preview(objectCode, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }
}
