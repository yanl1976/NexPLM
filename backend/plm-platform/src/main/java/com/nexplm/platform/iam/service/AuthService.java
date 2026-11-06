package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.AuthDtos;
import com.nexplm.platform.iam.domain.SysUser;
import com.nexplm.platform.iam.mapper.SysUserMapper;
import com.nexplm.platform.security.JwtService;
import com.nexplm.platform.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 认证服务：登录、刷新令牌、当前用户信息。
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.TokenResp login(String username, String rawPassword) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BizException.of(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(OffsetDateTime.now())) {
            throw BizException.of(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(update);
        return issueToken(user);
    }

    public AuthDtos.TokenResp refresh(String refreshToken) {
        Claims claims = jwtService.parseAndRequireType(refreshToken, JwtService.TYPE_REFRESH);
        SysUser user = userMapper.selectById(jwtService.userId(claims));
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw BizException.of(ErrorCode.AUTH_REFRESH_INVALID);
        }
        return issueToken(user);
    }

    public AuthDtos.UserBrief currentUser(UserPrincipal principal) {
        SysUser user = userMapper.selectById(principal.userId());
        if (user == null) {
            throw BizException.of(ErrorCode.COMMON_UNAUTHORIZED);
        }
        List<String> permissions = List.copyOf(principal.permissions());
        return new AuthDtos.UserBrief(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getCompanyId(), principal.roles(), permissions, Boolean.TRUE.equals(user.getMustChangePwd()));
    }

    /** 为已确认身份的账号签发本站令牌（本地登录、刷新、SSO 共用同一出口）。 */
    public AuthDtos.TokenResp issueToken(SysUser user) {
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername(), user.getCompanyId());
        String refresh = jwtService.issueRefreshToken(user.getId(), user.getUsername(), user.getCompanyId());
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        List<String> permissions = userMapper.selectPermissionCodes(user.getId());
        AuthDtos.UserBrief brief = new AuthDtos.UserBrief(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getCompanyId(), roles, permissions, Boolean.TRUE.equals(user.getMustChangePwd()));
        return new AuthDtos.TokenResp("Bearer", access, jwtService.accessTokenTtlSeconds(), refresh, brief);
    }
}
