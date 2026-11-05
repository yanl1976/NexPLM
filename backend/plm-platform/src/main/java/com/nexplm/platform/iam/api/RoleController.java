package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.RoleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统-角色与授权")
@RestController
@RequestMapping("/api/v1/system/roles")
@PreAuthorize("hasAuthority('system.role.manage')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<IamDtos.RoleRow>> list() {
        return Result.ok(roleService.list(CurrentUser.companyId()));
    }

    @Operation(summary = "全部权限点")
    @GetMapping("/permissions")
    public Result<List<IamDtos.PermissionItem>> permissions() {
        return Result.ok(roleService.listPermissions());
    }

    @Operation(summary = "角色的权限点")
    @GetMapping("/{id}/permissions")
    public Result<List<String>> rolePermissions(@PathVariable Long id) {
        return Result.ok(roleService.permissionCodes(id));
    }

    @Operation(summary = "重设角色权限")
    @PutMapping("/{id}/permissions")
    @Audit(module = "IAM", action = "ROLE_GRANT", objectType = "SysRole")
    public Result<Void> grant(@PathVariable Long id, @RequestBody IamDtos.RolePermissionReq req) {
        roleService.grant(id, req.permissionCodes());
        return Result.ok();
    }

    @Operation(summary = "新建角色")
    @PostMapping
    @Audit(module = "IAM", action = "ROLE_CREATE", objectType = "SysRole")
    public Result<Long> create(@Valid @RequestBody IamDtos.RoleSaveReq req) {
        return Result.ok(roleService.create(CurrentUser.companyId(), req));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @Audit(module = "IAM", action = "ROLE_UPDATE", objectType = "SysRole")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IamDtos.RoleSaveReq req) {
        roleService.update(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @Audit(module = "IAM", action = "ROLE_DELETE", objectType = "SysRole")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
