package com.nexplm.platform.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** P9-1 集成框架：端点 / 映射 / 任务。 */
public final class IntegrationDtos {

    private IntegrationDtos() {
    }

    /* ---------------------------------------------------------------- 端点 */

    public record EndpointRow(Long id, String code, String name, String systemCode,
                              String baseUrl, String authType, boolean authConfigured,
                              int timeoutMs, boolean enabled, String remark,
                              OffsetDateTime updatedAt) {
    }

    public record EndpointDetail(Long id, String code, String name, String systemCode,
                                 String baseUrl, String authType, boolean authConfigured,
                                 Map<String, Object> headers, int timeoutMs, boolean enabled,
                                 String remark) {
    }

    /**
     * 认证信息只写不读：{@code authConfig} 传 null 表示"不修改"，
     * 避免前端拿到脱敏数据后再保存把密钥抹掉。
     */
    public record EndpointSaveReq(@NotBlank @Size(max = 64) String code,
                                  @NotBlank @Size(max = 200) String name,
                                  @Size(max = 32) String systemCode,
                                  @NotBlank @Size(max = 500) String baseUrl,
                                  @Size(max = 16) String authType,
                                  Map<String, Object> authConfig,
                                  Map<String, Object> headers,
                                  Integer timeoutMs,
                                  Boolean enabled,
                                  @Size(max = 1000) String remark) {
    }

    /* ---------------------------------------------------------------- 映射 */

    public record MappingRuleReq(@NotBlank @Size(max = 200) String source,
                                 @NotBlank @Size(max = 200) String target,
                                 @Size(max = 16) String transform,
                                 Boolean required) {
    }

    public record MappingRow(Long id, Long endpointId, String endpointCode, String objectCode,
                             String direction, String payloadVersion, int ruleCount,
                             boolean enabled, String remark, OffsetDateTime updatedAt) {
    }

    public record MappingDetail(Long id, Long endpointId, String endpointCode, String objectCode,
                                String direction, String payloadVersion,
                                List<MappingRuleReq> rules, boolean enabled, String remark) {
    }

    public record MappingSaveReq(@NotNull Long endpointId,
                                 @NotBlank @Size(max = 64) String objectCode,
                                 @Size(max = 8) String direction,
                                 @Size(max = 16) String payloadVersion,
                                 @NotEmpty @Valid List<MappingRuleReq> rules,
                                 Boolean enabled,
                                 @Size(max = 1000) String remark) {
    }

    /* ---------------------------------------------------------------- 任务 */

    public record JobRow(Long id, String jobNo, Long endpointId, String endpointCode,
                         String bizType, String bizKey, String direction, String status,
                         int attempt, int maxAttempt, Integer httpStatus, int durationMs,
                         OffsetDateTime nextRetryAt, OffsetDateTime createdAt,
                         OffsetDateTime finishedAt) {
    }

    public record JobLogRow(Long id, int attempt, String status, Integer httpStatus,
                            Integer durationMs, String errorMsg, OffsetDateTime createdAt) {
    }

    public record JobDetail(Long id, String jobNo, Long endpointId, String endpointCode,
                            Long mappingId, String bizType, String bizKey, String direction,
                            String status, int attempt, int maxAttempt, Integer httpStatus,
                            int durationMs, String errorMsg, JsonNode requestPayload,
                            String responseBody, OffsetDateTime nextRetryAt,
                            OffsetDateTime startedAt, OffsetDateTime finishedAt,
                            List<JobLogRow> logs) {
    }

    /**
     * 提交任务：{@code dryRun=true} 只生成报文快照（PREVIEW），永不发送。
     * 幂等键是 (endpointId, bizType, bizKey)，重复提交返回已存在的那条任务。
     */
    public record JobSubmitReq(@NotNull Long endpointId,
                               @NotBlank @Size(max = 64) String bizType,
                               @NotBlank @Size(max = 200) String bizKey,
                               @NotNull JsonNode payload,
                               Boolean dryRun,
                               Integer maxAttempt) {
    }

    public record JobSubmitResult(JobDetail job, boolean duplicated) {
    }

    /* ---------------------------------------------------------------- 金蝶适配 */

    public record KingdeePushReq(@NotNull Long endpointId,
                                 @NotBlank @Size(max = 64) String bizType,
                                 @NotNull Long bizId,
                                 @Size(max = 16) String viewType,
                                 Boolean dryRun,
                                 Integer maxAttempt) {
    }

    public record KingdeePreviewReq(@NotBlank @Size(max = 64) String bizType,
                                    @NotNull Long bizId,
                                    @Size(max = 16) String viewType) {
    }

    public record KingdeePreviewResult(String bizType, String bizKey, JsonNode payload) {
    }
}
