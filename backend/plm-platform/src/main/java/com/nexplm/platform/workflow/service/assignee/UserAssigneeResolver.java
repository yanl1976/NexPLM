package com.nexplm.platform.workflow.service.assignee;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 指定用户：ids 即用户 id。 */
@Component
public class UserAssigneeResolver implements AssigneeResolver {

    private final JdbcTemplate jdbcTemplate;

    public UserAssigneeResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String type() {
        return "USER";
    }

    @Override
    public String label() {
        return "指定用户";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        List<Long> ids = spec.safeIds();
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return jdbcTemplate.queryForList("""
                SELECT id FROM sys_user
                 WHERE id IN (%s) AND deleted = false AND status = 'ACTIVE'
                """.formatted(placeholders), Long.class, ids.toArray());
    }
}
