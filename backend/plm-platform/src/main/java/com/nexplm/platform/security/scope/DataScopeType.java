package com.nexplm.platform.security.scope;

/**
 * 数据范围（由角色 data_scope 决定，多角色取最宽）。
 * <p>ALL 集团全部 / COMPANY 本公司 / ORG 本组织及下级 / SELF 仅本人。</p>
 */
public enum DataScopeType {

    ALL(4),
    COMPANY(3),
    ORG(2),
    SELF(1);

    private final int level;

    DataScopeType(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public static DataScopeType parse(String value) {
        if (value == null || value.isBlank()) {
            return COMPANY;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return COMPANY;
        }
    }

    /** 多角色取最宽范围（level 最大）。 */
    public static DataScopeType widest(DataScopeType left, DataScopeType right) {
        return left.level() >= right.level() ? left : right;
    }
}
