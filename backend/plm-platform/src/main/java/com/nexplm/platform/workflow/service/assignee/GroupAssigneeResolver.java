package com.nexplm.platform.workflow.service.assignee;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 用户组：ids 即 {@code sys_group.id}，取组内全部在用用户。 */
@Component
public class GroupAssigneeResolver implements AssigneeResolver {

    private final JdbcTemplate jdbcTemplate;

    public GroupAssigneeResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String type() {
        return "GROUP";
    }

    @Override
    public String label() {
        return "用户组成员";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        List<Long> groupIds = spec.safeIds();
        if (groupIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(groupIds.size(), "?"));
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT ug.user_id
                  FROM sys_user_group ug
                  JOIN sys_user u ON u.id = ug.user_id AND u.deleted = false AND u.status = 'ACTIVE'
                 WHERE ug.group_id IN (%s)
                """.formatted(placeholders), Long.class, groupIds.toArray());
    }
}
