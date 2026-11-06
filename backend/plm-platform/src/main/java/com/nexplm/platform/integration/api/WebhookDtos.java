package com.nexplm.platform.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P9-4 Webhook：订阅 / 投递。 */
public final class WebhookDtos {

    private WebhookDtos() {
    }

    public record WebhookRow(Long id, String code, String name, String url, List<String> events,
                             boolean enabled, int maxAttempts, int timeoutMs, String remark,
                             OffsetDateTime updatedAt) {
    }

    /** 密钥只写不读：详情里没有 secret，只说明"已配置"。 */
    public record WebhookDetail(Long id, String code, String name, String url, List<String> events,
                                boolean enabled, int maxAttempts, int timeoutMs, String remark,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /**
     * 新建/修改订阅。{@code secret} 留空：新建时自动生成、修改时保持不变；
     * 显式填写则以填写值为准（便于对端已有约定密钥的场景）。
     */
    public record WebhookSaveReq(@NotBlank @Size(max = 64) String code,
                                 @NotBlank @Size(max = 200) String name,
                                 @NotBlank @Size(max = 500) String url,
                                 @NotEmpty List<String> events,
                                 Boolean enabled,
                                 Integer maxAttempts,
                                 Integer timeoutMs,
                                 @Size(max = 1000) String remark,
                                 @Size(max = 200) String secret) {
    }

    /** 明文密钥只在新建与轮换响应里出现一次。 */
    public record WebhookSecret(WebhookDetail webhook, String secret) {
    }

    public record DeliveryRow(Long id, Long webhookId, String webhookCode, String eventType,
                              String eventKey, String status, int attempt, int maxAttempts,
                              Integer httpStatus, int durationMs, String errorMsg,
                              OffsetDateTime nextRetryAt, OffsetDateTime createdAt,
                              OffsetDateTime finishedAt) {
    }

    public record DeliveryDetail(Long id, Long webhookId, String webhookCode, String eventType,
                                 String eventKey, String status, int attempt, int maxAttempts,
                                 Integer httpStatus, int durationMs, String errorMsg,
                                 JsonNode payload, String responseBody, String signature,
                                 OffsetDateTime nextRetryAt, OffsetDateTime startedAt,
                                 OffsetDateTime finishedAt, OffsetDateTime createdAt) {
    }
}
