package com.nexplm.platform.audit;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 批次 F-3 系统运行日志页：登录日志 + 操作审计日志（只读）。
 *
 * <p>与既有"对象日志"的区别：对象日志在零部件/文档详情里按 objectId 查单个对象的历史，
 * 这里是全局检索（跨模块/跨用户/按时间范围），对应旧系统 {@code OperateLogSearch} 的定位；
 * 旧系统把该模块列为"不能在功能授权中授权"（{@code ThreeRole.setting:4}），
 * 本系统用独立权限码 {@code sys.log.read} 控制（仅系统管理员与安全审计员持有）。</p>
 */
@Tag(name = "系统-运行日志")
@RestController
@RequestMapping("/api/v1/sys/logs")
public class SystemLogController {

    private final SystemLogService logService;
    private final AuditChainService chainService;

    public SystemLogController(SystemLogService logService, AuditChainService chainService) {
        this.logService = logService;
        this.chainService = chainService;
    }

    @Operation(summary = "今日概览 + 审计哈希链体检")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('sys.log.read')")
    public Result<SystemLogDtos.LogSummary> summary() {
        return Result.ok(logService.summary(CurrentUser.companyId()));
    }

    @Operation(summary = "登录日志（成功/失败都记）")
    @GetMapping("/login")
    @PreAuthorize("hasAuthority('sys.log.read')")
    public Result<PageResult<SystemLogDtos.LoginLogRow>> login(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(logService.pageLoginLogs(CurrentUser.companyId(), username, result,
                parseFrom(from), parseTo(to), page, size));
    }

    @Operation(summary = "操作审计日志（模块/动作/结果/关键词/时间范围）")
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('sys.log.read')")
    public Result<PageResult<SystemLogDtos.AuditLogRow>> audit(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(logService.pageAuditLogs(CurrentUser.companyId(), module, action, result,
                keyword, userId, parseFrom(from), parseTo(to), page, size));
    }

    @Operation(summary = "审计链完整性校验（可指定 id 区间，默认最近 5000 行）")
    @GetMapping("/audit/verify")
    @PreAuthorize("hasAuthority('sys.log.read')")
    public Result<AuditChainService.VerifyResult> verifyAuditChain(
            @RequestParam(required = false) Long fromId,
            @RequestParam(required = false) Long toId) {
        return Result.ok(chainService.verify(fromId, toId));
    }

    /** 支持 {@code 2026-11-07} 与 {@code 2026-11-07T09:30:00} 两种写法（前端日期选择器给的是前者）。 */
    private static OffsetDateTime parseFrom(String value) {
        LocalDateTime time = parse(value);
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** 日期型 to 取当天 23:59:59，避免用户选"到 11-07"却漏掉当天数据。 */
    private static OffsetDateTime parseTo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() == 10) {
            return LocalDate.parse(text).atTime(23, 59, 59)
                    .atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        LocalDateTime time = parse(text);
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() == 10) {
            return LocalDate.parse(text).atStartOfDay();
        }
        return text.contains("T") || text.contains(" ")
                ? LocalDateTime.parse(text.replace(' ', 'T'))
                : LocalDate.parse(text).atStartOfDay();
    }
}
