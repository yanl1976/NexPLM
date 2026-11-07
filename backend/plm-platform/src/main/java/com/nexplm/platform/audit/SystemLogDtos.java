package com.nexplm.platform.audit;

import java.time.OffsetDateTime;

/** 批次 F-3 系统运行日志 DTO（登录日志 + 操作审计日志 + 链路体检结果）。 */
public final class SystemLogDtos {

    private SystemLogDtos() {
    }

    /** 登录日志行（对应 sys_login_log，字段与旧系统登录审计检索列一致）。 */
    public record LoginLogRow(Long id, String username, Long userId, String displayName,
                              String result, String failReason, String ip, String ua,
                              OffsetDateTime createdAt) {
    }

    /** 操作审计行（对应 sys_audit_log，模块/动作/对象/结果/明细）。 */
    public record AuditLogRow(Long id, String traceId, Long userId, String displayName,
                              String module, String action, String objectType, Long objectId,
                              String result, String detail, String ip, String ua,
                              OffsetDateTime createdAt) {
    }

    /**
     * 日志概览：当日登录成功/失败数、当日审计条数，以及哈希链体检。
     *
     * <p>{@code chainEnabled=false} 表示 {@code sys_audit_log.prev_hash/hash} 尚未有写入点
     * （V1 建表预留了字段，写日志的 AuditAspect 目前不填），此时不伪造"校验通过"。</p>
     */
    public record LogSummary(long loginSuccess, long loginFailed, long auditCount,
                             boolean chainEnabled, int chainChecked, int chainBroken,
                             String chainMessage) {
    }
}
