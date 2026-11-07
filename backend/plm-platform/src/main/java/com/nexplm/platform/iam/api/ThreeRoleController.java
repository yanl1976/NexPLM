package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.iam.service.ThreeRoleService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批次 F-4 三员配置视图（只读）。
 *
 * <p>三员分配仍在「用户管理」里做，保存时由 {@code UserAdminService} 校验互斥；
 * 本接口只提供"三角色各自有谁 + 谁违反了互斥"的分布视图，避免再造一套
 * 类似旧系统 {@code ThreeRolePlatformManagement} 的独立账号体系。</p>
 */
@Tag(name = "系统-三员管理")
@RestController
@RequestMapping("/api/v1/sys/three-roles")
public class ThreeRoleController {

    private final ThreeRoleService threeRoleService;

    public ThreeRoleController(ThreeRoleService threeRoleService) {
        this.threeRoleService = threeRoleService;
    }

    @Operation(summary = "三员角色分布与互斥违规清单")
    @GetMapping
    @PreAuthorize("hasAuthority('sys.three-role.read')")
    public Result<IamDtos.ThreeRoleOverview> overview() {
        return Result.ok(threeRoleService.overview(CurrentUser.companyId()));
    }
}
