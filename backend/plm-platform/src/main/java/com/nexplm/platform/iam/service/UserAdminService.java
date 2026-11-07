package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.IamDtos;
import com.nexplm.platform.iam.domain.SysRole;
import com.nexplm.platform.iam.domain.SysUser;
import com.nexplm.platform.iam.mapper.SysRoleMapper;
import com.nexplm.platform.iam.mapper.SysUserMapper;
import com.nexplm.platform.security.scope.DataScopeContext;
import com.nexplm.platform.security.scope.DataScopeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理（管理员视角）：分页查询、创建、修改、启停、重置密码。
 */
@Service
public class UserAdminService {

    /** 三员角色编码（V2 种子内置）：三者互斥，同一账号最多持有一个。 */
    private static final List<String> THREE_ROLE_CODES = List.of("SYS_ADMIN", "SEC_ADMIN", "AUDITOR");

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UserAdminService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                            PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<IamDtos.UserRow> page(Long companyId, String keyword, String status, int page, int size) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getCompanyId, companyId)
                .orderByDesc(SysUser::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            query.and(w -> w.like(SysUser::getUsername, kw)
                    .or().like(SysUser::getDisplayName, kw)
                    .or().like(SysUser::getEmail, kw));
        }
        if (status != null && !status.isBlank()) {
            query.eq(SysUser::getStatus, status);
        }
        applyOrgScope(query);
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, Math.min(size, 200)), query);
        List<SysUser> users = result.getRecords();
        Map<Long, List<SysRole>> rolesByUser = loadRoles(users.stream().map(SysUser::getId).toList());
        List<IamDtos.UserRow> rows = users.stream().map(user -> {
            List<SysRole> roles = rolesByUser.getOrDefault(user.getId(), List.of());
            return new IamDtos.UserRow(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getEmail(), user.getMobile(), user.getSecurityLevel(), user.getStatus(),
                    user.getMustChangePwd(), user.getLastLoginAt(),
                    roles.stream().map(SysRole::getId).toList(),
                    roles.stream().map(SysRole::getName).toList());
        }).toList();
        return PageResult.of(rows, result.getTotal(), page, size);
    }

    /** 非集团范围的用户只能看到自己所在组织（及其下级）的用户。 */
    private void applyOrgScope(LambdaQueryWrapper<SysUser> query) {
        DataScopeContext.Scope scope = DataScopeContext.get();
        if (scope == null || (scope.type() != DataScopeType.ORG && scope.type() != DataScopeType.SELF)) {
            return;
        }
        if (scope.orgIds().isEmpty()) {
            query.apply("1 = 0");
            return;
        }
        String marks = scope.orgIds().stream().map(id -> "?").collect(Collectors.joining(","));
        query.exists("SELECT 1 FROM sys_user_org uo WHERE uo.user_id = sys_user.id AND uo.org_id IN ("
                + marks + ")", scope.orgIds().toArray());
    }

    private Map<Long, List<SysRole>> loadRoles(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT ur.user_id, r.id, r.code, r.name
                  FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id
                 WHERE ur.user_id IN (%s)
                """.formatted(placeholders);
        Map<Long, List<SysRole>> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Long userId = rs.getLong(1);
            SysRole role = new SysRole();
            role.setId(rs.getLong(2));
            role.setCode(rs.getString(3));
            role.setName(rs.getString(4));
            map.computeIfAbsent(userId, key -> new ArrayList<>()).add(role);
        }, userIds.toArray());
        return map;
    }

    @Transactional
    public Long create(Long companyId, IamDtos.UserCreateReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username().trim()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "用户名已存在：" + req.username());
        }
        SysUser user = new SysUser();
        user.setCompanyId(companyId);
        user.setUsername(req.username().trim());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setDisplayName(req.displayName().trim());
        user.setEmail(req.email());
        user.setMobile(req.mobile());
        user.setSecurityLevel(req.securityLevel() == null ? 1 : req.securityLevel());
        user.setStatus("ACTIVE");
        user.setMustChangePwd(true);
        user.setPwdUpdatedAt(OffsetDateTime.now());
        user.setAuthProvider("LOCAL");
        user.setVersion(0);
        user.setDeleted(false);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.insert(user);
        replaceRoles(user.getId(), req.roleIds());
        return user.getId();
    }

    @Transactional
    public void update(Long id, IamDtos.UserUpdateReq req) {
        SysUser user = require(id);
        user.setDisplayName(req.displayName().trim());
        user.setEmail(req.email());
        user.setMobile(req.mobile());
        if (req.securityLevel() != null) {
            user.setSecurityLevel(req.securityLevel());
        }
        if (req.status() != null && !req.status().isBlank()) {
            user.setStatus(req.status());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        if (req.roleIds() != null) {
            replaceRoles(id, req.roleIds());
        }
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        SysUser user = require(id);
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPasswordHash(passwordEncoder.encode(newPassword));
        update.setMustChangePwd(true);
        update.setPwdUpdatedAt(OffsetDateTime.now());
        update.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(update);
    }

    @Transactional
    public void delete(Long id, Long operatorId) {
        SysUser user = require(id);
        if (user.getId().equals(operatorId)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能删除当前登录账号");
        }
        roleMapper.deleteUserRoles(id);
        userMapper.deleteById(id);
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        assertThreeRoleExclusive(roleIds);
        roleMapper.deleteUserRoles(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            roleMapper.insertUserRoles(userId, roleIds);
        }
    }

    /**
     * 三员互斥（批次 F-4）：系统管理员 / 安全保密管理员 / 安全审计员三者只能选其一。
     *
     * <p>依据 docs/06 EXP-20261107-16 第 6、7 条：旧系统用 {@code USERINFO.USERROLETYPE}
     * 把三员账号与普通账号区分开（组织树只查 {@code NormalUserType}），并用
     * {@code ThreeRole.setting:4} 禁止三员模块走普通功能授权。新系统的三角色在 V2 种子里
     * 已经存在（SYS_ADMIN/SEC_ADMIN/AUDITOR），但没有任何互斥约束——一个账号可以同时是
     * 系统管理员与审计员，等于自己操作自己审计，"三权分立"名存实亡。</p>
     *
     * <p>这里只做"同一账号不得同时持有多个三员角色"这一条硬约束，不复制旧系统那套
     * 独立账号表 + 登录时段限制 + 密码策略（见 docs/06 遗留）。</p>
     */
    private void assertThreeRoleExclusive(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        String placeholders = roleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT code FROM sys_role WHERE id IN (" + placeholders + ")",
                String.class, roleIds.toArray());
        List<String> hit = codes.stream().filter(THREE_ROLE_CODES::contains).toList();
        if (hit.size() > 1) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "三员角色互斥：系统管理员 / 安全保密管理员 / 安全审计员只能分配其一，"
                            + "当前同时选择了 " + hit.size() + " 个（" + String.join("、", hit) + "）");
        }
    }

    private SysUser require(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
