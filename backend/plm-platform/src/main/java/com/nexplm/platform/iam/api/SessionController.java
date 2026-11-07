package com.nexplm.platform.iam.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.UserSessionService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 批次 F-1 在线用户：列表 + 单条下线 + 按设备类型全部下线。
 *
 * <p>对齐旧系统 {@code OnlineUserList.page} 的操作项（刷新 / 踢出选中 / 全部踢出），
 * 差别是旧系统踢的是内存会话对象，这里踢的是 {@code sys_user_session} 行 + 令牌 sid 校验。</p>
 */
@Tag(name = "系统-在线用户")
@RestController
@RequestMapping("/api/v1/sys/sessions")
public class SessionController {

    private final UserSessionService sessionService;

    public SessionController(UserSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "在线用户列表（可按设备类型 / 用户名筛选）")
    @GetMapping
    @PreAuthorize("hasAuthority('sys.session.read')")
    public Result<PageResult<SessionDtos.SessionRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Boolean onlyActive,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(sessionService.page(CurrentUser.companyId(), keyword, deviceType,
                onlyActive, page, size));
    }

    @Operation(summary = "强制下线单条会话（该设备立即失效）")
    @PostMapping("/{id}/kick")
    @PreAuthorize("hasAuthority('sys.session.write')")
    @Audit(module = "IAM", action = "SESSION_KICK", objectType = "SysUserSession")
    public Result<Void> kick(@PathVariable Long id) {
        sessionService.kick(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "全部下线（可按 PC / MOBILE 分开，对齐旧系统 OnlineUserType）")
    @PostMapping("/kick-all")
    @PreAuthorize("hasAuthority('sys.session.write')")
    @Audit(module = "IAM", action = "SESSION_KICK_ALL", objectType = "SysUserSession")
    public Result<Map<String, Integer>> kickAll(
            @RequestParam(required = false) String deviceType) {
        int count = sessionService.kickAll(CurrentUser.companyId(), deviceType);
        return Result.ok(Map.of("kicked", count));
    }
}
