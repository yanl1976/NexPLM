package com.nexplm.platform.integration.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P9-3 开放平台：API 客户端管理 + 对外对象/文件查询。 */
public final class OpenApiDtos {

    private OpenApiDtos() {
    }

    public record ApiClientRow(Long id, String code, String name, String keyPrefix,
                               List<String> scopes, int rateLimitPerMin, boolean enabled,
                               String expiresAt, OffsetDateTime lastUsedAt,
                               OffsetDateTime createdAt) {
    }

    public record ApiClientDetail(Long id, String code, String name, String keyPrefix,
                                  List<String> scopes, int rateLimitPerMin, boolean enabled,
                                  String expiresAt, OffsetDateTime lastUsedAt, String remark) {
    }

    /**
     * 创建/轮换后返回明文 key —— 服务端只存 sha256，之后再也拿不到。
     */
    public record ApiClientCreated(ApiClientDetail client, String apiKey) {
    }

    public record ApiClientSaveReq(@NotBlank @Size(max = 64) String code,
                                   @NotBlank @Size(max = 200) String name,
                                   @NotEmpty List<String> scopes,
                                   Integer rateLimitPerMin,
                                   Boolean enabled,
                                   @Size(max = 20) String expiresAt,
                                   @Size(max = 1000) String remark) {
    }

    public record WhoAmI(String clientCode, String clientName, List<String> scopes,
                         int rateLimitPerMin) {
    }

    public record OpenObjectRow(Long id, String objectType, String code, String name,
                                int revision, String lifecycleState) {
    }

    public record OpenObjectDetail(Long id, String objectType, String code, String name,
                                   int revision, int iteration, String lifecycleState,
                                   String extJson) {
    }
}
