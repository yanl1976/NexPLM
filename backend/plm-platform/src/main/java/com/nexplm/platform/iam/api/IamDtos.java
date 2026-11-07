package com.nexplm.platform.iam.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 组织 / 用户 / 角色 / 权限点 DTO。
 */
public final class IamDtos {

    private IamDtos() {
    }

    // ---------- 组织 ----------
    public record OrgNode(Long id, Long parentId, String code, String name, String orgType,
                          Integer sort, String status, List<OrgNode> children) {
    }

    public record OrgSaveReq(
            Long parentId,
            @NotBlank(message = "组织编码不能为空") @Size(max = 64) String code,
            @NotBlank(message = "组织名称不能为空") @Size(max = 200) String name,
            @Size(max = 32) String orgType,
            Integer sort,
            Long leaderId,
            String status) {
    }

    // ---------- 用户 ----------
    public record UserRow(Long id, String username, String displayName, String email, String mobile,
                          Integer securityLevel, String status, Boolean mustChangePwd,
                          OffsetDateTime lastLoginAt, List<Long> roleIds, List<String> roleNames) {
    }

    public record UserCreateReq(
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "^[A-Za-z0-9_.-]{3,64}$", message = "用户名需为 3-64 位字母、数字或 _.-")
            String username,
            @NotBlank(message = "密码不能为空") @Size(min = 8, max = 64, message = "密码长度需 8-64 位") String password,
            @NotBlank(message = "姓名不能为空") @Size(max = 100) String displayName,
            @Email(message = "邮箱格式不正确") String email,
            String mobile,
            Integer securityLevel,
            List<Long> roleIds) {
    }

    public record UserUpdateReq(
            @NotBlank(message = "姓名不能为空") @Size(max = 100) String displayName,
            @Email(message = "邮箱格式不正确") String email,
            String mobile,
            Integer securityLevel,
            String status,
            List<Long> roleIds) {
    }

    public record ResetPwdReq(
            @NotBlank(message = "新密码不能为空") @Size(min = 8, max = 64, message = "密码长度需 8-64 位") String newPassword) {
    }

    // ---------- 角色 ----------
    public record RoleRow(Long id, String code, String name, String roleType, String dataScope,
                          String status, Integer userCount, Integer permissionCount) {
    }

    public record RoleSaveReq(
            @NotBlank(message = "角色编码不能为空") @Size(max = 64) String code,
            @NotBlank(message = "角色名称不能为空") @Size(max = 200) String name,
            String dataScope,
            String status) {
    }

    public record PermissionItem(String code, String module, String action, String name, String nameEn) {
    }

    public record RolePermissionReq(List<String> permissionCodes) {
    }

    // ---------- 三员（批次 F-4） ----------
    public record ThreeRoleMember(Long userId, String username, String displayName, String status,
                                  OffsetDateTime lastLoginAt) {
    }

    public record ThreeRoleGroup(String roleCode, String roleName, List<ThreeRoleMember> members) {
    }

    /** 互斥违规：同一账号同时持有多个三员角色（启用校验前的历史数据）。 */
    public record ThreeRoleViolation(Long userId, String username, String displayName,
                                     String roleCodes, int roleCount) {
    }

    public record ThreeRoleOverview(List<ThreeRoleGroup> roles, List<ThreeRoleViolation> violations,
                                    String note) {
    }
}
