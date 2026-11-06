package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 单点登录握手端点（未登录可访问）。
 *
 * <p>OIDC 走"前端拿跳转地址 → IdP 认证 → 回调带 code+state 换本站令牌"，
 * 不在服务端存会话（本项目无状态）；LDAP 直接收用户名口令做 bind 校验。</p>
 */
@Tag(name = "认证-SSO", description = "OIDC 跳转与回调、LDAP/AD 登录")
@RestController
@RequestMapping("/api/v1/auth/sso")
public class SsoAuthController {

    private final SsoService ssoService;

    public SsoAuthController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @Operation(summary = "启用中的登录方式（仅编码/名称/类型）")
    @GetMapping("/providers")
    public Result<List<SsoDtos.PublicProvider>> providers() {
        return Result.ok(ssoService.publicProviders());
    }

    @Operation(summary = "获取 OIDC 跳转地址（含自签 state 与 PKCE 挑战）")
    @GetMapping("/{code}/authorize")
    public Result<SsoDtos.AuthorizeResp> authorize(@PathVariable String code) {
        return Result.ok(ssoService.authorize(code));
    }

    @Operation(summary = "OIDC 回调：用授权码换本站令牌")
    @PostMapping("/{code}/callback")
    @Audit(module = "IAM", action = "SSO_LOGIN", recordArgs = false)
    public Result<AuthDtos.TokenResp> callback(@PathVariable String code,
                                               @Valid @RequestBody SsoDtos.OidcCallbackReq req) {
        return Result.ok(ssoService.oidcCallback(code, req.authCode().trim(), req.state()));
    }

    @Operation(summary = "LDAP/AD 登录")
    @PostMapping("/{code}/ldap-login")
    @Audit(module = "IAM", action = "SSO_LDAP_LOGIN", recordArgs = false)
    public Result<AuthDtos.TokenResp> ldapLogin(@PathVariable String code,
                                                @Valid @RequestBody SsoDtos.LdapLoginReq req) {
        return Result.ok(ssoService.ldapLogin(code, req.username().trim(), req.password()));
    }
}
