package com.nexplm.platform.notify.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.notify.service.NotifyService;
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

import java.util.List;
import java.util.Map;

/**
 * 通知通道与发送记录（仅系统管理员）。
 *
 * <p>配置里含 SMTP 口令与机器人 webhook/密钥，故权限只授 SYS_ADMIN，
 * 详情接口一律抹掉密钥字段、只回 {@code secretFields}。</p>
 */
@Tag(name = "系统-通知通道")
@RestController
@RequestMapping("/api/v1/sys/notify-channels")
public class NotifyController {

    private final NotifyService notifyService;

    public NotifyController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Operation(summary = "支持的通道类型")
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('sys.notify.read')")
    public Result<List<String>> types() {
        return Result.ok(notifyService.channelTypes());
    }

    @Operation(summary = "可订阅的事件字典")
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('sys.notify.read')")
    public Result<Map<String, String>> events() {
        return Result.ok(NotifyService.EVENT_LABELS);
    }

    @Operation(summary = "通知通道列表")
    @GetMapping
    @PreAuthorize("hasAuthority('sys.notify.read')")
    public Result<PageResult<NotifyDtos.ChannelRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(notifyService.channelPage(CurrentUser.companyId(), keyword, page, size));
    }

    @Operation(summary = "通道详情（密钥不回传）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.notify.read')")
    public Result<NotifyDtos.ChannelDetail> detail(@PathVariable Long id) {
        return Result.ok(notifyService.channelDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建通道")
    @PostMapping
    @PreAuthorize("hasAuthority('sys.notify.write')")
    @Audit(module = "IAM", action = "NOTIFY_CHANNEL_CREATE", objectType = "SysNotifyChannel")
    public Result<NotifyDtos.ChannelDetail> create(@Valid @RequestBody NotifyDtos.ChannelSaveReq req) {
        return Result.ok(notifyService.createChannel(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改通道（密钥字段留空表示保持不变）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.notify.write')")
    @Audit(module = "IAM", action = "NOTIFY_CHANNEL_UPDATE", objectType = "SysNotifyChannel")
    public Result<NotifyDtos.ChannelDetail> update(@PathVariable Long id,
                                                   @Valid @RequestBody NotifyDtos.ChannelSaveReq req) {
        return Result.ok(notifyService.updateChannel(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除通道（软删，历史消息保留）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sys.notify.write')")
    @Audit(module = "IAM", action = "NOTIFY_CHANNEL_DELETE", objectType = "SysNotifyChannel")
    public Result<Void> delete(@PathVariable Long id) {
        notifyService.deleteChannel(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }

    @Operation(summary = "测试发送（走真实通道，失败原因原样返回）")
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('sys.notify.write')")
    @Audit(module = "IAM", action = "NOTIFY_CHANNEL_TEST", objectType = "SysNotifyChannel")
    public Result<NotifyDtos.MessageDetail> test(@PathVariable Long id) {
        return Result.ok(notifyService.testChannel(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "发送记录")
    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAuthority('sys.notify.read')")
    public Result<PageResult<NotifyDtos.MessageRow>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(notifyService.messagePage(CurrentUser.companyId(), id, status, page, size));
    }

    @Operation(summary = "消息详情（含渲染后的文案快照）")
    @GetMapping("/messages/{id}")
    @PreAuthorize("hasAuthority('sys.notify.read')")
    public Result<NotifyDtos.MessageDetail> message(@PathVariable Long id) {
        return Result.ok(notifyService.messageDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "重发（失败/死信，使用原文案快照）")
    @PostMapping("/messages/{id}/resend")
    @PreAuthorize("hasAuthority('sys.notify.write')")
    @Audit(module = "IAM", action = "NOTIFY_MESSAGE_RESEND", objectType = "SysNotifyMessage")
    public Result<NotifyDtos.MessageDetail> resend(@PathVariable Long id) {
        return Result.ok(notifyService.resend(id, CurrentUser.companyId()));
    }
}
