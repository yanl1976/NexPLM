package com.nexplm.platform.integration.kingdee;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.integration.api.IntegrationDtos;
import com.nexplm.platform.integration.service.IntegrationService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 金蝶云星空下发入口（P9-2）：本地对象 → 源报文 → 字段映射 → 集成任务。
 *
 * <p>与 {@code /itg/jobs} 的区别：那边是"报文我已经拼好了"，这边是"给我对象 id，
 * 我自己按业务类型去取数组装"——界面上的下发按钮走这条。</p>
 */
@Tag(name = "集成-金蝶下发")
@RestController
@RequestMapping("/api/v1/itg/kingdee")
public class KingdeeController {

    private final KingdeePayloadService payloadService;
    private final IntegrationService integrationService;

    public KingdeeController(KingdeePayloadService payloadService,
                             IntegrationService integrationService) {
        this.payloadService = payloadService;
        this.integrationService = integrationService;
    }

    @Operation(summary = "报文预览（只组装源报文，不落任务、不发送）")
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('itg.job.read')")
    public Result<IntegrationDtos.KingdeePreviewResult> preview(
            @Valid @RequestBody IntegrationDtos.KingdeePreviewReq req) {
        KingdeePayloadService.SourcePayload source = payloadService.build(
                CurrentUser.companyId(), req.bizType(), req.bizId(), req.viewType());
        return Result.ok(new IntegrationDtos.KingdeePreviewResult(source.bizType(),
                source.bizKey(), source.payload()));
    }

    @Operation(summary = "下发到金蝶（幂等：同一版本重复下发返回原任务）")
    @PostMapping("/push")
    @PreAuthorize("hasAuthority('itg.job.write')")
    @Audit(module = "ITG", action = "KINGDEE_PUSH", objectType = "ItgJob")
    public Result<IntegrationDtos.JobSubmitResult> push(
            @Valid @RequestBody IntegrationDtos.KingdeePushReq req) {
        KingdeePayloadService.SourcePayload source = payloadService.build(
                CurrentUser.companyId(), req.bizType(), req.bizId(), req.viewType());
        return Result.ok(integrationService.submitJob(new IntegrationDtos.JobSubmitReq(
                        req.endpointId(), source.bizType(), source.bizKey(), source.payload(),
                        req.dryRun(), req.maxAttempt()),
                CurrentUser.companyId(), CurrentUser.userId()));
    }
}
