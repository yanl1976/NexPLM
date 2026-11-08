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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final UserSessionService sessionService;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordPolicy passwordPolicy;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, UserSessionService sessionService,
                       JdbcTemplate jdbcTemplate, PasswordPolicy passwordPolicy) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * 用户自助改密：校验旧密码 → 强度 → 历史不可重用 → 更新哈希并清 {@code must_change_pwd}。
     *
     * <p>此前系统只有管理员重置、没有自助改密接口，{@code must_change_pwd} 标记形同虚设
     * （前端拿得到却无处可改）。改密成功后旧密码进入历史表，不能立即改回。</p>
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword == null ? "" : oldPassword, user.getPasswordHash())) {
            throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        passwordPolicy.validate(newPassword, user.getUsername());
        passwordPolicy.assertNotReused(userId, newPassword);
        String hash = passwordEncoder.encode(newPassword);
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPasswordHash(hash);
        update.setMustChangePwd(false);
        update.setPwdUpdatedAt(OffsetDateTime.now());
        update.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(update);
        passwordPolicy.record(userId, hash);
    }

    /**
     * 登录：建会话（批次 F-1）+ 写登录日志（批次 F-3 的前置——原先 {@code sys_login_log}
     * 表存在但没有任何写入点，登录日志页会永远空）。
     *
     * <p><b>刻意不加 {@code @Transactional}</b>：失败登录也要留下 FAILED 日志，
     * 若整个方法在一个事务里，抛异常会把刚写的日志回滚掉（本用例首次即踩到）。</p>
     */
    public AuthDtos.TokenResp login(String username, String rawPassword, String deviceType,
                                    String ip, String ua) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            writeLoginLog(username, user == null ? null : user.getId(), "FAILED", "用户名或密码错误", ip, ua);
            throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            writeLoginLog(username, user.getId(), "FAILED", "账号已停用", ip, ua);
            throw BizException.of(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(OffsetDateTime.now())) {
            writeLoginLog(username, user.getId(), "FAILED", "账号已锁定", ip, ua);
            throw BizException.of(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        try {
            // 密码有效期（默认 0 天=不检查，避免锁死历史账号）
            passwordPolicy.assertNotExpired(user);
        } catch (BizException ex) {
            writeLoginLog(username, user.getId(), "FAILED", "密码已过期", ip, ua);
            throw ex;
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(update);
        writeLoginLog(username, user.getId(), "SUCCESS", null, ip, ua);
        long sessionId = sessionService.start(user.getId(), user.getUsername(), user.getCompanyId(),
                deviceType, ip, ua);
        return issueToken(user, sessionId);
    }

    /** 刷新：会话已被踢则拒绝，不再给新令牌（否则强制下线等于没做）。 */
    public AuthDtos.TokenResp refresh(String refreshToken) {
        Claims claims = jwtService.parseAndRequireType(refreshToken, JwtService.TYPE_REFRESH);
        Long sessionId = jwtService.sessionId(claims);
        if (sessionId != null && !sessionService.isActive(sessionId)) {
            throw BizException.of(ErrorCode.AUTH_SESSION_REVOKED);
        }
        SysUser user = userMapper.selectById(jwtService.userId(claims));
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw BizException.of(ErrorCode.AUTH_REFRESH_INVALID);
        }
        return issueToken(user, sessionId);
    }

    /** 主动登出：会话置失效，当前令牌立刻不可用。 */
    public void logout(Long sessionId) {
        if (sessionId != null) {
            sessionService.logout(sessionId);
        }
    }

    private void writeLoginLog(String username, Long userId, String result, String failReason,
                               String ip, String ua) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO sys_login_log (username, user_id, result, fail_reason, ip, ua)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    username, userId, result, failReason,
                    ip == null ? null : (ip.length() > 64 ? ip.substring(0, 64) : ip),
                    ua == null ? null : (ua.length() > 500 ? ua.substring(0, 500) : ua));
        } catch (RuntimeException ignored) {
            // 登录日志失败不能阻断登录（审计可补，登录不可断）
        }
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

    /** SSO 等无会话场景：令牌不带 sid（不纳入强制下线管理）。 */
    public AuthDtos.TokenResp issueToken(SysUser user) {
        return issueToken(user, null);
    }

    /** 为已确认身份的账号签发本站令牌（本地登录、刷新、SSO 共用同一出口）。 */
    public AuthDtos.TokenResp issueToken(SysUser user, Long sessionId) {
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername(),
                user.getCompanyId(), sessionId);
        String refresh = jwtService.issueRefreshToken(user.getId(), user.getUsername(),
                user.getCompanyId(), sessionId);
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        List<String> permissions = userMapper.selectPermissionCodes(user.getId());
        AuthDtos.UserBrief brief = new AuthDtos.UserBrief(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getCompanyId(), roles, permissions, Boolean.TRUE.equals(user.getMustChangePwd()));
        return new AuthDtos.TokenResp("Bearer", access, jwtService.accessTokenTtlSeconds(), refresh, brief);
    }
}
