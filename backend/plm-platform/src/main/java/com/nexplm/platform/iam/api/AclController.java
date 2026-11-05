package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.AclService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统-对象授权（ACL）")
@RestController
@RequestMapping("/api/v1/security/acl")
@PreAuthorize("hasAuthority('system.acl.manage')")
public class AclController {

    private final AclService aclService;

    public AclController(AclService aclService) {
        this.aclService = aclService;
    }

    @Operation(summary = "对象的授权列表")
    @GetMapping
    public Result<List<SecurityDtos.AclRow>> list(@RequestParam Long objectId) {
        return Result.ok(aclService.list(objectId));
    }

    @Operation(summary = "授权（支持批量）")
    @PostMapping
    @Audit(module = "SECURITY", action = "ACL_GRANT", objectType = "AclEntry")
    public Result<Void> grant(@Valid @RequestBody SecurityDtos.AclGrantReq req) {
        aclService.grant(req, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "撤销授权")
    @DeleteMapping("/{id}")
    @Audit(module = "SECURITY", action = "ACL_REVOKE", objectType = "AclEntry")
    public Result<Void> revoke(@PathVariable Long id) {
        aclService.revoke(id);
        return Result.ok();
    }

    @Operation(summary = "权限判定（调试）")
    @PostMapping("/check")
    public Result<SecurityDtos.AclCheckResp> check(@Valid @RequestBody SecurityDtos.AclCheckReq req) {
        Long userId = req.userId() == null ? CurrentUser.userId() : req.userId();
        return Result.ok(aclService.check(req.objectId(), req.action(), userId));
    }
}
