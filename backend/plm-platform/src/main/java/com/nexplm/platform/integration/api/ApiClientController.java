package com.nexplm.platform.integration.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.integration.service.ApiKeyService;
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

/** API 客户端管理（P9-3）：创建/轮换时明文 key 只返回一次。 */
@Tag(name = "集成-API 客户端")
@RestController
@RequestMapping("/api/v1/itg/clients")
public class ApiClientController {

    private final ApiKeyService apiKeyService;

    public ApiClientController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Operation(summary = "API 客户端列表")
    @GetMapping
    @PreAuthorize("hasAuthority('itg.client.read')")
    public Result<PageResult<OpenApiDtos.ApiClientRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(apiKeyService.page(CurrentUser.companyId(), keyword, page, size));
    }

    @Operation(summary = "API 客户端详情（不含密钥）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('itg.client.read')")
    public Result<OpenApiDtos.ApiClientDetail> detail(@PathVariable Long id) {
        return Result.ok(apiKeyService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建客户端（返回明文 key，仅此一次）")
    @PostMapping
    @PreAuthorize("hasAuthority('itg.client.write')")
    @Audit(module = "ITG", action = "CLIENT_CREATE", objectType = "ItgApiClient")
    public Result<OpenApiDtos.ApiClientCreated> create(
            @Valid @RequestBody OpenApiDtos.ApiClientSaveReq req) {
        return Result.ok(apiKeyService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改客户端（作用域/限流/启停/到期）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('itg.client.write')")
    @Audit(module = "ITG", action = "CLIENT_UPDATE", objectType = "ItgApiClient")
    public Result<OpenApiDtos.ApiClientDetail> update(
            @PathVariable Long id, @Valid @RequestBody OpenApiDtos.ApiClientSaveReq req) {
        return Result.ok(apiKeyService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "轮换密钥（旧 key 立即失效，返回新 key）")
    @PostMapping("/{id}/rotate")
    @PreAuthorize("hasAuthority('itg.client.write')")
    @Audit(module = "ITG", action = "CLIENT_ROTATE", objectType = "ItgApiClient")
    public Result<OpenApiDtos.ApiClientCreated> rotate(@PathVariable Long id) {
        return Result.ok(apiKeyService.rotate(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除客户端")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('itg.client.write')")
    @Audit(module = "ITG", action = "CLIENT_DELETE", objectType = "ItgApiClient")
    public Result<Void> delete(@PathVariable Long id) {
        apiKeyService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }
}
