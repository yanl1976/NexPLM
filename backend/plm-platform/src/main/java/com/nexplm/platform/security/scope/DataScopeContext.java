package com.nexplm.platform.security.scope;

import java.util.List;

/**
 * 当前请求的数据范围上下文（随请求线程存活，由 JWT 过滤器设置与清理）。
 */
public final class DataScopeContext {

    public record Scope(DataScopeType type, Long companyId, Long userId, List<Long> orgIds) {

        public Scope {
            orgIds = orgIds == null ? List.of() : List.copyOf(orgIds);
        }
    }

    private static final ThreadLocal<Scope> HOLDER = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(Scope scope) {
        HOLDER.set(scope);
    }

    public static Scope get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
