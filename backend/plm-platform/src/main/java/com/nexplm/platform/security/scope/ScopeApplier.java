package com.nexplm.platform.security.scope;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.springframework.stereotype.Component;

/**
 * 把数据范围注入查询条件（显式调用，而非 SQL 拦截 Magic）。
 * <p>显式调用便于代码审查与单元测试：任何未调用 {@code apply} 的业务查询都等于"未做数据隔离"，
 * 由代码评审与 ArchUnit 规则保证敏感查询必须调用。</p>
 */
@Component
public class ScopeApplier {

    /**
     * @param companyColumn 组织/公司列（按公司过滤）
     * @param orgColumn     组织列（ORG 范围使用，可为 null 表示该表无组织列）
     * @param ownerColumn   属主列（SELF 范围使用，可为 null）
     */
    public <T> LambdaQueryWrapper<T> apply(LambdaQueryWrapper<T> wrapper,
                                           SFunction<T, ?> companyColumn,
                                           SFunction<T, ?> orgColumn,
                                           SFunction<T, ?> ownerColumn) {
        DataScopeContext.Scope scope = DataScopeContext.get();
        if (scope == null || scope.type() == DataScopeType.ALL) {
            return wrapper;
        }
        if (companyColumn != null && scope.companyId() != null) {
            wrapper.eq(companyColumn, scope.companyId());
        }
        switch (scope.type()) {
            case COMPANY -> {
                // 已按公司过滤
            }
            case ORG -> {
                if (orgColumn == null) {
                    wrapper.apply("1 = 0");
                } else if (scope.orgIds().isEmpty()) {
                    wrapper.apply("1 = 0");
                } else {
                    wrapper.in(orgColumn, scope.orgIds());
                }
            }
            case SELF -> {
                if (ownerColumn == null) {
                    wrapper.apply("1 = 0");
                } else {
                    wrapper.eq(ownerColumn, scope.userId());
                }
            }
            default -> wrapper.apply("1 = 0");
        }
        return wrapper;
    }

    /**
     * 单对象可见性校验（P10-5 安全加固）。
     *
     * <p>只做列表过滤是不够的：{@code GET /docs/{id}} 这类按主键直取的接口不经过 wrapper，
     * 列表里看不到的数据照样能被拼 id 拿到。这里提供与 {@link #apply} <b>完全同一套语义</b>的
     * 判定，供各域的 {@code requireObject} 在返回详情前调用；不通过时调用方统一按"不存在"处理，
     * 避免泄露"存在但无权看"。</p>
     */
    public boolean isVisible(Long companyId, Long orgId, Long ownerId) {
        DataScopeContext.Scope scope = DataScopeContext.get();
        if (scope == null || scope.type() == DataScopeType.ALL) {
            return true;
        }
        if (scope.companyId() != null && !scope.companyId().equals(companyId)) {
            return false;
        }
        return switch (scope.type()) {
            case COMPANY -> true;
            case ORG -> orgId != null && scope.orgIds().contains(orgId);
            case SELF -> ownerId != null && ownerId.equals(scope.userId());
            default -> false;
        };
    }
}
