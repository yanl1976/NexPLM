package com.nexplm.platform.support.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL jsonb 与 Java String 的互转。
 * <p>MyBatis 默认把 String 以 varchar 绑定，PG 会拒绝写入 jsonb 列；这里用 {@link Types#OTHER}
 * 让驱动按 unknown 类型发送，由 PG 隐式转换为 jsonb。</p>
 * <p>使用方式：字段上标注 {@code @TableField(typeHandler = JsonbStringTypeHandler.class)}，
 * 实体类标注 {@code @TableName(autoResultMap = true)}。</p>
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
