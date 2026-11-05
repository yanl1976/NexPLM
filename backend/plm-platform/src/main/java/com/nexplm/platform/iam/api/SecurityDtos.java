package com.nexplm.platform.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 规则权限与对象 ACL 的 DTO。
 */
public final class SecurityDtos {

    private SecurityDtos() {
    }

    // ---------- 规则权限 ----------
    public record RuleRow(Long id, String code, String name, String objectType, String exprJson,
                          String actions, Integer priority, String effect, Boolean enabled,
                          String remark, OffsetDateTime updatedAt) {
    }

    public record RuleSaveReq(
            @NotBlank(message = "规则编码不能为空")
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_.-]{1,63}$", message = "规则编码格式不合法")
            String code,
            @NotBlank(message = "规则名称不能为空") @Size(max = 200) String name,
            @NotBlank(message = "对象类型不能为空") String objectType,
            @NotBlank(message = "条件表达式不能为空") String exprJson,
            @NotBlank(message = "授权操作不能为空") String actions,
            Integer priority,
            String effect,
            Boolean enabled,
            String remark) {
    }

    public record RuleTestReq(Long ruleId, String exprJson, String currentUserId, String currentCompanyId,
                              Map<String, Object> attributes) {
    }

    public record RuleTestResp(boolean matched, List<String> matchedRules) {
    }

    // ---------- 对象 ACL ----------
    public record AclRow(Long id, Long objectId, String actorType, Long actorId, String actorName,
                         String actions, String effect, OffsetDateTime expireAt, String source) {
    }

    public record AclGrantReq(
            @NotNull(message = "对象 ID 不能为空") Long objectId,
            @NotBlank(message = "被授权者类型不能为空") String actorType,
            Long actorId,
            @NotBlank(message = "操作集合不能为空") String actions,
            String effect,
            OffsetDateTime expireAt,
            List<AclGrantItem> items) {
    }

    public record AclGrantItem(String actorType, Long actorId, String actions, String effect, OffsetDateTime expireAt) {
    }

    public record AclCheckReq(@NotNull Long objectId, @NotBlank String action, Long userId) {
    }

    public record AclCheckResp(boolean allowed, String reason) {
    }
}
