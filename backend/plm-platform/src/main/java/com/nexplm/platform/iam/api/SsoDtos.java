package com.nexplm.platform.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** P9-5 单点登录：身份提供方配置 + 登录握手。 */
public final class SsoDtos {

    private SsoDtos() {
    }

    public record ProviderRow(Long id, String code, String name, String type, boolean enabled,
                              boolean autoProvision, String defaultRoleCode, String remark,
                              OffsetDateTime updatedAt) {
    }

    /**
     * 配置详情。{@code config} 里密钥字段已被抹掉，
     * {@code secretFields} 说明哪些密钥当前已配置（前端显示「已配置，留空不改」）。
     */
    public record ProviderDetail(Long id, String code, String name, String type, boolean enabled,
                                 boolean autoProvision, String defaultRoleCode, String remark,
                                 Map<String, Object> config, List<String> secretFields,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record ProviderSaveReq(@NotBlank @Size(max = 64) String code,
                                  @NotBlank @Size(max = 200) String name,
                                  @NotBlank String type,
                                  Boolean enabled,
                                  Boolean autoProvision,
                                  @Size(max = 64) String defaultRoleCode,
                                  @Size(max = 1000) String remark,
                                  Map<String, Object> config) {
    }

    /** 登录页拿到的公开信息：只有编码/名称/类型，不含任何地址与密钥。 */
    public record PublicProvider(String code, String name, String type) {
    }

    public record AuthorizeResp(String url, String state) {
    }

    public record OidcCallbackReq(@NotBlank String authCode, @NotBlank String state) {
    }

    public record LdapLoginReq(@NotBlank String username, @NotBlank String password) {
    }
}
