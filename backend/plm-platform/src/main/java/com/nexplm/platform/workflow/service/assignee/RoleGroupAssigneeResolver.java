package com.nexplm.platform.workflow.service.assignee;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 组 + 角色：ids 必须为 {@code [角色 id, 组 id]}，取「该组内且拥有该角色」的用户。
 */
@Component
public class RoleGroupAssigneeResolver implements AssigneeResolver {

    private final JdbcTemplate jdbcTemplate;

    public RoleGroupAssigneeResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String type() {
        return "ROLE_GROUP";
    }

    @Override
    public String label() {
        return "组+角色";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        List<Long> ids = spec.safeIds();
        if (ids.size() < 2) {
            throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "组+角色需同时提供角色 id 与组 id");
        }
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT u.id
                  FROM sys_user u
                  JOIN sys_user_role ur ON ur.user_id = u.id
                  JOIN sys_user_group ug ON ug.user_id = u.id
                 WHERE ur.role_id = ? AND ug.group_id = ?
                   AND u.deleted = false AND u.status = 'ACTIVE'
                """, Long.class, ids.get(0), ids.get(1));
    }
}
