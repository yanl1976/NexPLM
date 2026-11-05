package com.nexplm.platform.security.scope;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 解析用户的数据范围：取角色中最宽 scope，并计算"本组织及下级"的组织 ID 集合。
 * <p>组织隔离（sys_org_isolate）与共享（sys_org_share）在 P2 与对象查询一起接入。</p>
 */
@Service
public class DataScopeResolver {

    private final JdbcTemplate jdbcTemplate;

    public DataScopeResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DataScopeContext.Scope resolve(Long userId, Long companyId) {
        DataScopeType type = DataScopeType.COMPANY;
        try {
            List<String> scopes = jdbcTemplate.queryForList("""
                            SELECT r.data_scope FROM sys_role r
                              JOIN sys_user_role ur ON ur.role_id = r.id
                             WHERE ur.user_id = ? AND r.status = 'ACTIVE'
                            """, String.class, userId);
            for (String value : scopes) {
                type = DataScopeType.widest(type, DataScopeType.parse(value));
            }
        } catch (Exception ignored) {
            // 角色数据异常时退回本公司范围（最小可用）
        }

        List<Long> orgIds = List.of();
        if (type == DataScopeType.ORG) {
            orgIds = subtreeOrgIds(userId, companyId);
        } else if (type == DataScopeType.SELF) {
            orgIds = myOrgIds(userId);
        }
        return new DataScopeContext.Scope(type, companyId, userId, orgIds);
    }

    private List<Long> myOrgIds(Long userId) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT org_id FROM sys_user_org WHERE user_id = ?", Long.class, userId);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<Long> subtreeOrgIds(Long userId, Long companyId) {
        try {
            return jdbcTemplate.queryForList("""
                            SELECT o.id FROM sys_org o
                             WHERE o.company_id = ?
                               AND EXISTS (
                                   SELECT 1 FROM sys_user_org uo
                                     JOIN sys_org mine ON mine.id = uo.org_id
                                    WHERE uo.user_id = ?
                                      AND o.path LIKE mine.path || '%'
                               )
                            """, Long.class, companyId, userId);
        } catch (Exception ex) {
            return List.of();
        }
    }
}
