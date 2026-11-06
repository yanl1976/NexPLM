package com.nexplm.platform.integration.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.integration.service.IntegrationService;
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

/** 集成与开放平台：端点配置 / 字段映射 / 集成任务（P9-1）。 */
@Tag(name = "集成-框架")
@RestController
@RequestMapping("/api/v1/itg")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    /* ---------------------------------------------------------------- 端点 */

    @Operation(summary = "集成端点列表")
    @GetMapping("/endpoints")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<PageResult<IntegrationDtos.EndpointRow>> endpointPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(integrationService.endpointPage(CurrentUser.companyId(), keyword, enabled,
                page, size));
    }

    @Operation(summary = "集成端点详情（认证信息只回传是否已配置）")
    @GetMapping("/endpoints/{id}")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<IntegrationDtos.EndpointDetail> endpointDetail(@PathVariable Long id) {
        return Result.ok(integrationService.endpointDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建集成端点")
    @PostMapping("/endpoints")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "ENDPOINT_CREATE", objectType = "ItgEndpoint")
    public Result<IntegrationDtos.EndpointDetail> createEndpoint(
            @Valid @RequestBody IntegrationDtos.EndpointSaveReq req) {
        return Result.ok(integrationService.createEndpoint(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改集成端点（authConfig 不传表示保持不变）")
    @PutMapping("/endpoints/{id}")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "ENDPOINT_UPDATE", objectType = "ItgEndpoint")
    public Result<IntegrationDtos.EndpointDetail> updateEndpoint(
            @PathVariable Long id, @Valid @RequestBody IntegrationDtos.EndpointSaveReq req) {
        return Result.ok(integrationService.updateEndpoint(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除集成端点（连带停用其映射，历史任务保留）")
    @DeleteMapping("/endpoints/{id}")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "ENDPOINT_DELETE", objectType = "ItgEndpoint")
    public Result<Void> deleteEndpoint(@PathVariable Long id) {
        integrationService.deleteEndpoint(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    /* ---------------------------------------------------------------- 映射 */

    @Operation(summary = "字段映射列表")
    @GetMapping("/mappings")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<PageResult<IntegrationDtos.MappingRow>> mappingPage(
            @RequestParam(required = false) Long endpointId,
            @RequestParam(required = false) String objectCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(integrationService.mappingPage(CurrentUser.companyId(), endpointId,
                objectCode, page, size));
    }

    @Operation(summary = "字段映射详情")
    @GetMapping("/mappings/{id}")
    @PreAuthorize("hasAuthority('itg.config.read')")
    public Result<IntegrationDtos.MappingDetail> mappingDetail(@PathVariable Long id) {
        return Result.ok(integrationService.mappingDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建字段映射（规则存 jsonb，改动无需重启）")
    @PostMapping("/mappings")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "MAPPING_CREATE", objectType = "ItgMapping")
    public Result<IntegrationDtos.MappingDetail> createMapping(
            @Valid @RequestBody IntegrationDtos.MappingSaveReq req) {
        return Result.ok(integrationService.createMapping(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改字段映射（整表替换规则）")
    @PutMapping("/mappings/{id}")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "MAPPING_UPDATE", objectType = "ItgMapping")
    public Result<IntegrationDtos.MappingDetail> updateMapping(
            @PathVariable Long id, @Valid @RequestBody IntegrationDtos.MappingSaveReq req) {
        return Result.ok(integrationService.updateMapping(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除字段映射")
    @DeleteMapping("/mappings/{id}")
    @PreAuthorize("hasAuthority('itg.config.write')")
    @Audit(module = "ITG", action = "MAPPING_DELETE", objectType = "ItgMapping")
    public Result<Void> deleteMapping(@PathVariable Long id) {
        integrationService.deleteMapping(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    /* ---------------------------------------------------------------- 任务 */

    @Operation(summary = "集成任务列表")
    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('itg.job.read')")
    public Result<PageResult<IntegrationDtos.JobRow>> jobPage(
            @RequestParam(required = false) Long endpointId,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(integrationService.jobPage(CurrentUser.companyId(), endpointId, bizType,
                status, page, size));
    }

    @Operation(summary = "集成任务详情（含每次尝试日志）")
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('itg.job.read')")
    public Result<IntegrationDtos.JobDetail> jobDetail(@PathVariable Long id) {
        return Result.ok(integrationService.jobDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "提交集成任务（幂等；dryRun 只生成报文快照）")
    @PostMapping("/jobs")
    @PreAuthorize("hasAuthority('itg.job.write')")
    @Audit(module = "ITG", action = "JOB_SUBMIT", objectType = "ItgJob")
    public Result<IntegrationDtos.JobSubmitResult> submitJob(
            @Valid @RequestBody IntegrationDtos.JobSubmitReq req) {
        return Result.ok(integrationService.submitJob(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "立即发送（待发送/失败待重试）")
    @PostMapping("/jobs/{id}/dispatch")
    @PreAuthorize("hasAuthority('itg.job.write')")
    @Audit(module = "ITG", action = "JOB_DISPATCH", objectType = "ItgJob")
    public Result<IntegrationDtos.JobDetail> dispatchJob(@PathVariable Long id) {
        return Result.ok(integrationService.dispatchJob(id, CurrentUser.companyId()));
    }

    @Operation(summary = "重放（失败/死信，使用原报文快照）")
    @PostMapping("/jobs/{id}/replay")
    @PreAuthorize("hasAuthority('itg.job.write')")
    @Audit(module = "ITG", action = "JOB_REPLAY", objectType = "ItgJob")
    public Result<IntegrationDtos.JobDetail> replayJob(@PathVariable Long id) {
        return Result.ok(integrationService.replayJob(id, CurrentUser.companyId()));
    }
}
