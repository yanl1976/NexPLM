package com.nexplm.platform.iam.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.SsoService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单点登录配置维护（仅系统管理员）。
 *
 * <p>配置里含 IdP 客户端密钥 / 域服务账号口令，故权限只授 SYS_ADMIN，
 * 且详情接口一律抹掉密钥字段、只回 {@code secretFields} 说明"已配置"。</p>
 */
@Tag(name = "系统-单点登录")
@RestController
@RequestMapping("/api/v1/sys/sso-providers")
public class SsoProviderController {

    private final SsoService ssoService;

    public SsoProviderController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @Operation(summary = "登录方式列表")
    @GetMapping
    @PreAuthorize("hasAuthority('sys.sso.read')")
    public Result<PageResult<SsoDtos.ProviderRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(ssoService.providerPage(CurrentUser.companyId(), keyword, page, size));
    }

    @Operation(summary = "登录方式详情（密钥不回传）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.sso.read')")
    public Result<SsoDtos.ProviderDetail> detail(@PathVariable Long id) {
        return Result.ok(ssoService.providerDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建登录方式")
    @PostMapping
    @PreAuthorize("hasAuthority('sys.sso.write')")
    @Audit(module = "IAM", action = "SSO_PROVIDER_CREATE", objectType = "SysSsoProvider")
    public Result<SsoDtos.ProviderDetail> create(@Valid @RequestBody SsoDtos.ProviderSaveReq req) {
        return Result.ok(ssoService.createProvider(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改登录方式（密钥字段留空表示保持不变）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.sso.write')")
    @Audit(module = "IAM", action = "SSO_PROVIDER_UPDATE", objectType = "SysSsoProvider")
    public Result<SsoDtos.ProviderDetail> update(@PathVariable Long id,
                                                 @Valid @RequestBody SsoDtos.ProviderSaveReq req) {
        return Result.ok(ssoService.updateProvider(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除登录方式（软删，历史账号绑定保留）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.sso.write')")
    @Audit(module = "IAM", action = "SSO_PROVIDER_DELETE", objectType = "SysSsoProvider")
    public Result<Void> delete(@PathVariable Long id) {
        ssoService.deleteProvider(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }
}
