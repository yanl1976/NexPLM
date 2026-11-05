package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.AuthService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "登录、刷新令牌、当前用户")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    @Audit(module = "IAM", action = "USER_LOGIN", recordArgs = false)
    public Result<AuthDtos.TokenResp> login(@Valid @RequestBody AuthDtos.LoginReq req) {
        return Result.ok(authService.login(req.username().trim(), req.password()));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public Result<AuthDtos.TokenResp> refresh(@Valid @RequestBody AuthDtos.RefreshReq req) {
        return Result.ok(authService.refresh(req.refreshToken()));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<AuthDtos.UserBrief> me() {
        return Result.ok(authService.currentUser(CurrentUser.require()));
    }
}
