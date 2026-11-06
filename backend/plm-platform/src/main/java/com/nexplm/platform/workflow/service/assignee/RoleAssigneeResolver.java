package com.nexplm.platform.workflow.service.assignee;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 角色：ids 即角色 id，取角色下全部在用用户。 */
@Component
public class RoleAssigneeResolver implements AssigneeResolver {

    private final JdbcTemplate jdbcTemplate;

    public RoleAssigneeResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String type() {
        return "ROLE";
    }

    @Override
    public String label() {
        return "角色成员";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        List<Long> roleIds = spec.safeIds();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(roleIds.size(), "?"));
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id
                  FROM sys_user u
                  JOIN sys_user_role ur ON ur.user_id = u.id
                 WHERE ur.role_id IN (%s) AND u.deleted = false AND u.status = 'ACTIVE'
                """.formatted(placeholders), Long.class, roleIds.toArray());
    }
}
