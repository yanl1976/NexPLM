package com.nexplm.platform.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 认证相关 DTO 集合。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginReq(
            @NotBlank(message = "用户名不能为空") @Size(max = 64) String username,
            @NotBlank(message = "密码不能为空") @Size(max = 128) String password) {
    }

    public record RefreshReq(@NotBlank(message = "刷新令牌不能为空") String refreshToken) {
    }

    public record UserBrief(Long id, String username, String displayName, Long companyId,
                            List<String> roles, List<String> permissions, boolean mustChangePwd) {
    }

    public record TokenResp(String tokenType, String accessToken, long expiresIn,
                            String refreshToken, UserBrief user) {
    }

    /** 自助改密：旧密码 + 新密码（强度与历史不可重用由 {@code PasswordPolicy} 校验）。 */
    public record ChangePwdReq(
            @NotBlank(message = "原密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 8, max = 64, message = "密码长度需 8-64 位") String newPassword) {
    }
}
