package com.nexplm.platform.iam.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.UserAdminService;
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

@Tag(name = "系统-用户管理")
@RestController
@RequestMapping("/api/v1/system/users")
@PreAuthorize("hasAuthority('system.user.manage')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @Operation(summary = "用户分页列表")
    @GetMapping
    public Result<PageResult<IamDtos.UserRow>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(userAdminService.page(CurrentUser.companyId(), keyword, status, page, size));
    }

    @Operation(summary = "新建用户")
    @PostMapping
    @Audit(module = "IAM", action = "USER_CREATE", objectType = "SysUser", recordArgs = false)
    public Result<Long> create(@Valid @RequestBody IamDtos.UserCreateReq req) {
        return Result.ok(userAdminService.create(CurrentUser.companyId(), req));
    }

    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    @Audit(module = "IAM", action = "USER_UPDATE", objectType = "SysUser")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IamDtos.UserUpdateReq req) {
        userAdminService.update(id, req);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    @Audit(module = "IAM", action = "USER_RESET_PASSWORD", objectType = "SysUser", recordArgs = false)
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody IamDtos.ResetPwdReq req) {
        userAdminService.resetPassword(id, req.newPassword());
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @Audit(module = "IAM", action = "USER_DELETE", objectType = "SysUser")
    public Result<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id, CurrentUser.userId());
        return Result.ok();
    }
}
