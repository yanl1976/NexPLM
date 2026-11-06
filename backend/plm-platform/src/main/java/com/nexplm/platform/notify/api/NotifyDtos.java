package com.nexplm.platform.notify.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** P9-6 通知通道：通道配置 + 发送记录。 */
public final class NotifyDtos {

    private NotifyDtos() {
    }

    public record ChannelRow(Long id, String code, String name, String type, boolean enabled,
                             int maxAttempts, List<String> events, String remark,
                             OffsetDateTime updatedAt) {
    }

    /** 详情：密钥字段被抹掉，{@code secretFields} 表示哪些已配置。 */
    public record ChannelDetail(Long id, String code, String name, String type, boolean enabled,
                                int maxAttempts, List<String> events, Map<String, Object> config,
                                List<String> secretFields, String remark,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record ChannelSaveReq(@NotBlank @Size(max = 64) String code,
                                 @NotBlank @Size(max = 200) String name,
                                 @NotBlank String type,
                                 Boolean enabled,
                                 Integer maxAttempts,
                                 @NotEmpty List<String> events,
                                 @Size(max = 1000) String remark,
                                 Map<String, Object> config) {
    }

    public record MessageRow(Long id, Long channelId, String channelCode, String eventType,
                             String eventKey, String title, String status, int attempt,
                             int maxAttempts, String errorMsg, OffsetDateTime nextRetryAt,
                             OffsetDateTime createdAt, OffsetDateTime finishedAt) {
    }

    public record MessageDetail(Long id, Long channelId, String channelCode, String eventType,
                                String eventKey, String title, String content, String status,
                                int attempt, int maxAttempts, String errorMsg, String response,
                                OffsetDateTime nextRetryAt, OffsetDateTime startedAt,
                                OffsetDateTime finishedAt, OffsetDateTime createdAt) {
    }
}
