package com.nexplm.platform.broadcast;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;

/**
 * 批次 F-2 系统广播。
 *
 * <p>管理三个接口（列表/增改/发布撤回）要 {@code sys.broadcast.read|write}；
 * {@code /pending} 与 {@code /{id}/read} 是"收件人侧"能力，**只要登录即可**——
 * 普通业务用户也必须能收到公告，否则广播等于只发给管理员自己。</p>
 */
@Tag(name = "系统-广播")
@RestController
@RequestMapping("/api/v1/sys/broadcasts")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @Operation(summary = "广播管理列表（草稿/已发布/已撤回）")
    @GetMapping
    @PreAuthorize("hasAuthority('sys.broadcast.read')")
    public Result<PageResult<BroadcastDtos.BroadcastRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(broadcastService.page(CurrentUser.companyId(), keyword, state, page, size));
    }

    @Operation(summary = "新建广播（草稿，publishAt 可为预约时间）")
    @PostMapping
    @PreAuthorize("hasAuthority('sys.broadcast.write')")
    @Audit(module = "IAM", action = "BROADCAST_CREATE", objectType = "SysBroadcast")
    public Result<Long> create(@RequestBody BroadcastDtos.SaveReq req) {
        return Result.ok(broadcastService.create(CurrentUser.companyId(), CurrentUser.userId(), req));
    }

    @Operation(summary = "修改广播（已发布须先撤回）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.broadcast.write')")
    @Audit(module = "IAM", action = "BROADCAST_UPDATE", objectType = "SysBroadcast")
    public Result<Void> update(@PathVariable Long id, @RequestBody BroadcastDtos.SaveReq req) {
        broadcastService.update(id, CurrentUser.companyId(), req);
        return Result.ok(null);
    }

    @Operation(summary = "发布（publish_at 在未来即预约发布）")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('sys.broadcast.write')")
    @Audit(module = "IAM", action = "BROADCAST_PUBLISH", objectType = "SysBroadcast")
    public Result<Void> publish(@PathVariable Long id) {
        broadcastService.publish(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "撤回")
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('sys.broadcast.write')")
    @Audit(module = "IAM", action = "BROADCAST_REVOKE", objectType = "SysBroadcast")
    public Result<Void> revoke(@PathVariable Long id) {
        broadcastService.revoke(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "删除（已发布须先撤回）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.broadcast.write')")
    @Audit(module = "IAM", action = "BROADCAST_DELETE", objectType = "SysBroadcast")
    public Result<Void> delete(@PathVariable Long id) {
        broadcastService.delete(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "登录后待弹窗的未读广播（收件人侧，仅需登录）")
    @GetMapping("/pending")
    public Result<List<BroadcastDtos.PendingItem>> pending() {
        return Result.ok(broadcastService.pending(CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "标记已读（收件人侧，仅需登录）")
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        broadcastService.markRead(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }
}
