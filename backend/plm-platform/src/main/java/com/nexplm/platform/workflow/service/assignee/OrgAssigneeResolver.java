package com.nexplm.platform.workflow.service.assignee;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 组织：ids 即组织 id，**含子组织**（按 {@code sys_org.path} 前缀判定）。
 * <p>用户与组织是多对多（{@code sys_user_org}），取并集。</p>
 */
@Component
public class OrgAssigneeResolver implements AssigneeResolver {

    private final JdbcTemplate jdbcTemplate;

    public OrgAssigneeResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String type() {
        return "ORG";
    }

    @Override
    public String label() {
        return "组织成员（含子组织）";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        List<Long> orgIds = spec.safeIds();
        if (orgIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(orgIds.size(), "?"));
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT uo.user_id
                  FROM sys_user_org uo
                  JOIN sys_org o ON o.id = uo.org_id
                  JOIN sys_user u ON u.id = uo.user_id AND u.deleted = false AND u.status = 'ACTIVE'
                 WHERE EXISTS (SELECT 1 FROM sys_org root
                                WHERE root.id IN (%s)
                                  AND (o.id = root.id OR o.path LIKE root.path || '%%'))
                """.formatted(placeholders), Long.class, orgIds.toArray());
    }
}
