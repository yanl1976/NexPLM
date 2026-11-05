package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.IamDtos;
import com.nexplm.platform.iam.domain.SysOrg;
import com.nexplm.platform.iam.mapper.SysOrgMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 组织树维护。path 采用 /id/id/ 形式，移动节点时整棵子树重写路径。
 */
@Service
public class OrgService {

    private final SysOrgMapper orgMapper;
    private final JdbcTemplate jdbcTemplate;

    public OrgService(SysOrgMapper orgMapper, JdbcTemplate jdbcTemplate) {
        this.orgMapper = orgMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<IamDtos.OrgNode> tree(Long companyId) {
        List<SysOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getCompanyId, companyId)
                .orderByAsc(SysOrg::getSort)
                .orderByAsc(SysOrg::getId));
        Map<Long, List<SysOrg>> childrenIndex = new LinkedHashMap<>();
        for (SysOrg org : all) {
            childrenIndex.computeIfAbsent(org.getParentId() == null ? 0L : org.getParentId(),
                    key -> new ArrayList<>()).add(org);
        }
        return buildNodes(0L, childrenIndex);
    }

    private List<IamDtos.OrgNode> buildNodes(Long parentId, Map<Long, List<SysOrg>> index) {
        List<SysOrg> children = index.getOrDefault(parentId, List.of());
        return children.stream()
                .sorted(Comparator.comparing(o -> o.getSort() == null ? 0 : o.getSort()))
                .map(org -> new IamDtos.OrgNode(org.getId(), org.getParentId(), org.getCode(), org.getName(),
                        org.getOrgType(), org.getSort(), org.getStatus(), buildNodes(org.getId(), index)))
                .toList();
    }

    @Transactional
    public Long create(Long companyId, IamDtos.OrgSaveReq req) {
        Long duplicated = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getCompanyId, companyId)
                .eq(SysOrg::getCode, req.code()));
        if (duplicated != null && duplicated > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "组织编码已存在：" + req.code());
        }
        SysOrg parent = null;
        if (req.parentId() != null) {
            parent = orgMapper.selectById(req.parentId());
            if (parent == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "上级组织不存在");
            }
        }
        SysOrg org = new SysOrg();
        org.setCompanyId(companyId);
        org.setParentId(req.parentId());
        org.setCode(req.code().trim());
        org.setName(req.name().trim());
        org.setOrgType(req.orgType() == null || req.orgType().isBlank() ? "DEPT" : req.orgType());
        org.setSort(req.sort() == null ? 0 : req.sort());
        org.setLeaderId(req.leaderId());
        org.setStatus(req.status() == null || req.status().isBlank() ? "ACTIVE" : req.status());
        org.setCreatedAt(OffsetDateTime.now());
        org.setUpdatedAt(OffsetDateTime.now());
        orgMapper.insert(org);
        String parentPath = parent == null ? "/" : parent.getPath();
        org.setPath(parentPath + org.getId() + "/");
        orgMapper.updateById(org);
        return org.getId();
    }

    @Transactional
    public void update(Long id, IamDtos.OrgSaveReq req) {
        SysOrg org = require(id);
        String oldPath = org.getPath();
        boolean moved = !Objects.equals(org.getParentId(), req.parentId());

        org.setName(req.name().trim());
        org.setOrgType(req.orgType() == null || req.orgType().isBlank() ? org.getOrgType() : req.orgType());
        org.setSort(req.sort() == null ? org.getSort() : req.sort());
        org.setLeaderId(req.leaderId());
        if (req.status() != null && !req.status().isBlank()) {
            org.setStatus(req.status());
        }
        org.setUpdatedAt(OffsetDateTime.now());

        if (moved) {
            if (Objects.equals(req.parentId(), id)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能将组织挂到自身下");
            }
            SysOrg parent = req.parentId() == null ? null : require(req.parentId());
            if (parent != null && parent.getPath() != null && parent.getPath().contains("/" + id + "/")) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能移动到自己的子节点下");
            }
            org.setParentId(req.parentId());
            org.setPath((parent == null ? "/" : parent.getPath()) + id + "/");
            orgMapper.updateById(org);
            if (oldPath != null && !oldPath.isBlank()) {
                jdbcTemplate.update("""
                        UPDATE sys_org SET path = ? || substring(path from ?)
                         WHERE company_id = ? AND path LIKE ? AND id <> ?
                        """, org.getPath(), oldPath.length() + 1, org.getCompanyId(), oldPath + "%", id);
            }
            return;
        }
        orgMapper.updateById(org);
    }

    @Transactional
    public void delete(Long id) {
        SysOrg org = require(id);
        Long children = orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>().eq(SysOrg::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "存在下级组织，无法删除");
        }
        Integer users = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user_org WHERE org_id = ?", Integer.class, id);
        if (users != null && users > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "组织下仍有用户，无法删除");
        }
        orgMapper.deleteById(org.getId());
    }

    public SysOrg require(Long id) {
        SysOrg org = orgMapper.selectById(id);
        if (org == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "组织不存在");
        }
        return org;
    }
}
