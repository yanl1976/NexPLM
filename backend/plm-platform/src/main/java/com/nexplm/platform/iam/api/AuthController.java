package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.AuthService;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录、刷新令牌、当前用户")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "登录（落会话并记录登录日志）")
    @PostMapping("/login")
    @Audit(module = "IAM", action = "USER_LOGIN", recordArgs = false)
    public Result<AuthDtos.TokenResp> login(@Valid @RequestBody AuthDtos.LoginReq req,
                                            HttpServletRequest request) {
        return Result.ok(authService.login(req.username().trim(), req.password(),
                deviceType(request), clientIp(request), request.getHeader("User-Agent")));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public Result<AuthDtos.TokenResp> refresh(@Valid @RequestBody AuthDtos.RefreshReq req) {
        return Result.ok(authService.refresh(req.refreshToken()));
    }

    @Operation(summary = "登出（会话立即失效）")
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(currentSessionId(authorization));
        return Result.ok(null);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<AuthDtos.UserBrief> me() {
        return Result.ok(authService.currentUser(CurrentUser.require()));
    }

    @Operation(summary = "修改自己的密码（校验强度与历史，成功后清除强制改密标记）")
    @PostMapping("/change-password")
    @Audit(module = "IAM", action = "USER_CHANGE_PASSWORD", recordArgs = false)
    public Result<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePwdReq req) {
        authService.changePassword(CurrentUser.userId(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }

    /** 从访问令牌取 sid：登出只针对当前这条会话，不影响其它设备。 */
    private Long currentSessionId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtService.sessionId(jwtService.parseAndRequireType(
                    authorization.substring(7).trim(), JwtService.TYPE_ACCESS));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** 设备类型：显式头优先，其次按 UA 判断（旧系统按 PC/移动分开下线）。 */
    private static String deviceType(HttpServletRequest request) {
        String header = request.getHeader("X-Device-Type");
        if (header != null && !header.isBlank()) {
            return header.trim().toUpperCase();
        }
        String ua = request.getHeader("User-Agent");
        if (ua != null && (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone"))) {
            return "MOBILE";
        }
        return "PC";
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
