package com.nexplm.platform.security;

import java.util.List;
import java.util.Set;

/**
 * 登录用户上下文。由 JWT 过滤器构建，随请求存活，可通过 CurrentUser 获取。
 */
public record UserPrincipal(
        Long userId,
        String username,
        String displayName,
        Long companyId,
        List<String> roles,
        Set<String> permissions) {

    public boolean hasPermission(String code) {
        return permissions.contains(code);
    }

    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }
}
