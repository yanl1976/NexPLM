package com.nexplm.platform.security;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前用户访问入口。避免业务代码到处读 SecurityContext。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UserPrincipal orNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof UserPrincipal userPrincipal ? userPrincipal : null;
    }

    public static UserPrincipal require() {
        UserPrincipal principal = orNull();
        if (principal == null) {
            throw BizException.of(ErrorCode.COMMON_UNAUTHORIZED);
        }
        return principal;
    }

    public static Long userId() {
        return require().userId();
    }

    public static Long companyId() {
        return require().companyId();
    }
}
