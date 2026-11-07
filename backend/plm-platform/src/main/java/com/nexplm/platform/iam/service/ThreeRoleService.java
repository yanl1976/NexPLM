package com.nexplm.platform.iam.service;

import com.nexplm.platform.iam.api.IamDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 批次 F-4 三员配置视图：三角色成员 + 互斥违规清单。
 *
 * <p>只读视图：三员的实际分配仍在「用户管理」里做（保存时由
 * {@link UserAdminService} 校验互斥），本页的定位是"看分布 + 揪历史脏数据"——
 * 旧系统靠 {@code USERROLETYPE} 保证互斥，新系统的三角色是纯 RBAC 数据，
 * 启用互斥校验之前可能已经存在一人多角色，必须能看见才谈得上清理。</p>
 */
@Service
public class ThreeRoleService {

    /** 三员角色编码与中文名（与 V2 种子一致）。 */
    private static final List<String[]> THREE_ROLES = List.of(
            new String[]{"SYS_ADMIN", "系统管理员"},
            new String[]{"SEC_ADMIN", "安全保密管理员"},
            new String[]{"AUDITOR", "安全审计员"});

    private final JdbcTemplate jdbcTemplate;

    public ThreeRoleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IamDtos.ThreeRoleOverview overview(Long companyId) {
        List<IamDtos.ThreeRoleGroup> groups = new ArrayList<>();
        for (String[] role : THREE_ROLES) {
            List<IamDtos.ThreeRoleMember> members = jdbcTemplate.query("""
                            SELECT u.id, u.username, u.display_name, u.status, u.last_login_at
                              FROM sys_user_role ur
                              JOIN sys_role r ON r.id = ur.role_id
                              JOIN sys_user u ON u.id = ur.user_id
                             WHERE r.code = ? AND r.company_id = ?
                             ORDER BY u.username
                            """,
                    (rs, rowNum) -> new IamDtos.ThreeRoleMember(rs.getLong("id"),
                            rs.getString("username"), rs.getString("display_name"),
                            rs.getString("status"),
                            rs.getObject("last_login_at", OffsetDateTime.class)),
                    role[0], companyId);
            groups.add(new IamDtos.ThreeRoleGroup(role[0], role[1], members));
        }
        List<IamDtos.ThreeRoleViolation> violations = jdbcTemplate.query("""
                        SELECT u.id, u.username, u.display_name,
                               string_agg(r.code, ',' ORDER BY r.code) AS codes,
                               COUNT(DISTINCT r.code) AS role_count
                          FROM sys_user u
                          JOIN sys_user_role ur ON ur.user_id = u.id
                          JOIN sys_role r ON r.id = ur.role_id
                         WHERE r.code IN ('SYS_ADMIN', 'SEC_ADMIN', 'AUDITOR')
                           AND r.company_id = ?
                         GROUP BY u.id, u.username, u.display_name
                        HAVING COUNT(DISTINCT r.code) > 1
                         ORDER BY u.username
                        """,
                (rs, rowNum) -> new IamDtos.ThreeRoleViolation(rs.getLong("id"),
                        rs.getString("username"), rs.getString("display_name"),
                        rs.getString("codes"), rs.getInt("role_count")),
                companyId);
        String note = violations.isEmpty()
                ? "三员互斥校验已生效：同一账号最多持有一个三员角色。"
                : "发现 " + violations.size() + " 个账号同时持有多个三员角色（历史数据），"
                + "请在「用户管理」中改为单一角色——校验只在保存时生效，不会自动改历史数据。";
        return new IamDtos.ThreeRoleOverview(groups, violations, note);
    }
}
