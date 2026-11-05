package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.IamDtos;
import com.nexplm.platform.iam.domain.SysPermission;
import com.nexplm.platform.iam.domain.SysRole;
import com.nexplm.platform.iam.mapper.SysPermissionMapper;
import com.nexplm.platform.iam.mapper.SysRoleMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色与功能授权。
 */
@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final JdbcTemplate jdbcTemplate;

    public RoleService(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper, JdbcTemplate jdbcTemplate) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<IamDtos.RoleRow> list(Long companyId) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCompanyId, companyId)
                .orderByAsc(SysRole::getId));
        return roles.stream().map(role -> {
            Integer users = roleMapper.countUsers(role.getId());
            int permissions = roleMapper.selectPermissionCodes(role.getId()).size();
            return new IamDtos.RoleRow(role.getId(), role.getCode(), role.getName(), role.getRoleType(),
                    role.getDataScope(), role.getStatus(), users, permissions);
        }).toList();
    }

    @Transactional
    public Long create(Long companyId, IamDtos.RoleSaveReq req) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCompanyId, companyId)
                .eq(SysRole::getCode, req.code().trim()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "角色编码已存在：" + req.code());
        }
        SysRole role = new SysRole();
        role.setCompanyId(companyId);
        role.setCode(req.code().trim());
        role.setName(req.name().trim());
        role.setRoleType("BUSINESS");
        role.setDataScope(req.dataScope() == null || req.dataScope().isBlank() ? "COMPANY" : req.dataScope());
        role.setStatus(req.status() == null || req.status().isBlank() ? "ACTIVE" : req.status());
        role.setCreatedAt(OffsetDateTime.now());
        role.setUpdatedAt(OffsetDateTime.now());
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional
    public void update(Long id, IamDtos.RoleSaveReq req) {
        SysRole role = require(id);
        role.setName(req.name().trim());
        if (req.dataScope() != null && !req.dataScope().isBlank()) {
            role.setDataScope(req.dataScope());
        }
        if (req.status() != null && !req.status().isBlank()) {
            role.setStatus(req.status());
        }
        role.setUpdatedAt(OffsetDateTime.now());
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = require(id);
        if (!"BUSINESS".equals(role.getRoleType())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "内置管理角色不允许删除");
        }
        if (roleMapper.countUsers(id) > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "角色下仍有用户，无法删除");
        }
        roleMapper.deletePermissions(id);
        roleMapper.deleteById(id);
    }

    public List<String> permissionCodes(Long roleId) {
        require(roleId);
        return roleMapper.selectPermissionCodes(roleId);
    }

    @Transactional
    public void grant(Long roleId, List<String> permissionCodes) {
        require(roleId);
        List<String> codes = permissionCodes == null ? List.of() : permissionCodes.stream().distinct().toList();
        if (!codes.isEmpty()) {
            Set<String> existing = permissionMapper.selectList(null).stream()
                    .map(SysPermission::getCode).collect(Collectors.toSet());
            List<String> unknown = codes.stream().filter(code -> !existing.contains(code)).toList();
            if (!unknown.isEmpty()) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "存在未知权限点：" + String.join(",", unknown));
            }
        }
        roleMapper.deletePermissions(roleId);
        if (!codes.isEmpty()) {
            roleMapper.insertPermissions(roleId, codes);
        }
    }

    public List<IamDtos.PermissionItem> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getModule)
                        .orderByAsc(SysPermission::getCode))
                .stream()
                .map(p -> new IamDtos.PermissionItem(p.getCode(), p.getModule(), p.getAction(),
                        p.getName(), p.getNameEn()))
                .toList();
    }

    public List<SysRole> listAll(Long companyId) {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCompanyId, companyId)
                .eq(SysRole::getStatus, "ACTIVE")
                .orderByAsc(SysRole::getId));
    }

    private SysRole require(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "角色不存在");
        }
        return role;
    }

    /** 供其他模块校验权限点是否存在。 */
    public boolean permissionExists(String code) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_permission WHERE code = ?", Integer.class, code);
        return count != null && count > 0;
    }
}
