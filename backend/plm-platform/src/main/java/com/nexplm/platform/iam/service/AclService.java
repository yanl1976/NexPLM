package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.iam.domain.AclEntry;
import com.nexplm.platform.iam.mapper.AclEntryMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对象实例权限（ACL）。
 * <p>判定顺序：显式 DENY 优先 > 显式 ALLOW > 属主隐式全权 > 默认拒绝。
 * 规则权限（{@link PermissionRuleService}）得到的操作由调用方合并进 {@link #check} 结果。</p>
 */
@Service
public class AclService {

    private static final Set<String> ACTOR_TYPES = Set.of(
            "USER", "ROLE", "GROUP", "GROUP_ROLE", "CREATOR", "OWNER", "ALL");
    private static final Set<String> EFFECTS = Set.of("ALLOW", "DENY");

    /** 属主隐式权限：审批（APPROVE）不在其中，避免自审自批。 */
    private static final Set<String> OWNER_IMPLICIT = Set.of(
            "READ", "WRITE", "DELETE", "GRANT", "CHECKOUT", "RELEASE");

    private final AclEntryMapper aclMapper;
    private final JdbcTemplate jdbcTemplate;

    public AclService(AclEntryMapper aclMapper, JdbcTemplate jdbcTemplate) {
        this.aclMapper = aclMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SecurityDtos.AclRow> list(Long objectId) {
        List<AclEntry> entries = aclMapper.selectList(new LambdaQueryWrapper<AclEntry>()
                .eq(AclEntry::getObjectId, objectId)
                .orderByAsc(AclEntry::getActorType)
                .orderByAsc(AclEntry::getId));
        Map<String, String> names = loadActorNames(entries);
        return entries.stream()
                .map(entry -> new SecurityDtos.AclRow(entry.getId(), entry.getObjectId(), entry.getActorType(),
                        entry.getActorId(), names.get(entry.getActorType() + ":" + entry.getActorId()),
                        entry.getActions(), entry.getEffect(), entry.getExpireAt(), entry.getSource()))
                .toList();
    }

    @Transactional
    public void grant(SecurityDtos.AclGrantReq req, Long operatorId) {
        List<SecurityDtos.AclGrantItem> items = new ArrayList<>();
        if (req.items() != null && !req.items().isEmpty()) {
            items.addAll(req.items());
        } else {
            items.add(new SecurityDtos.AclGrantItem(req.actorType(), req.actorId(), req.actions(),
                    req.effect(), req.expireAt()));
        }
        for (SecurityDtos.AclGrantItem item : items) {
            validate(item.actorType(), item.actions(), item.effect());
            AclEntry entry = new AclEntry();
            entry.setObjectId(req.objectId());
            entry.setActorType(item.actorType().toUpperCase());
            entry.setActorId(item.actorId());
            entry.setActions(normalizeActions(item.actions()));
            entry.setEffect(item.effect() == null || item.effect().isBlank() ? "ALLOW" : item.effect().toUpperCase());
            entry.setExpireAt(item.expireAt());
            entry.setSource("MANUAL");
            entry.setCreatedBy(operatorId);
            entry.setCreatedAt(OffsetDateTime.now());
            aclMapper.insert(entry);
        }
    }

    @Transactional
    public void revoke(Long id) {
        if (aclMapper.selectById(id) == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "授权记录不存在");
        }
        aclMapper.deleteById(id);
    }

    /**
     * 判定用户对对象是否有某操作权限（不含规则权限）。
     */
    public SecurityDtos.AclCheckResp check(Long objectId, String action, Long userId) {
        Set<Long> roleIds = queryIds("SELECT role_id FROM sys_user_role WHERE user_id = ?", userId);
        Set<Long> groupIds = queryIds("SELECT group_id FROM sys_user_group WHERE user_id = ?", userId);
        Set<Long> groupRoleIds = groupIds.isEmpty() ? Set.of()
                : queryIds("SELECT role_id FROM sys_group_role WHERE group_id IN ("
                + placeholders(groupIds.size()) + ")", groupIds.toArray());
        Long ownerId = queryOwner(objectId);
        String normalized = action.trim().toUpperCase();

        boolean allowed = false;
        for (AclEntry entry : aclMapper.selectList(new LambdaQueryWrapper<AclEntry>()
                .eq(AclEntry::getObjectId, objectId))) {
            if (entry.getExpireAt() != null && entry.getExpireAt().isBefore(OffsetDateTime.now())) {
                continue;
            }
            if (!matches(entry, userId, roleIds, groupIds, groupRoleIds, ownerId)) {
                continue;
            }
            if (!containsAction(entry.getActions(), normalized)) {
                continue;
            }
            if ("DENY".equals(entry.getEffect())) {
                return new SecurityDtos.AclCheckResp(false, "命中拒绝规则：" + entry.getActorType());
            }
            allowed = true;
        }
        if (allowed) {
            return new SecurityDtos.AclCheckResp(true, "命中显式授权");
        }
        if (ownerId != null && ownerId.equals(userId) && OWNER_IMPLICIT.contains(normalized)) {
            return new SecurityDtos.AclCheckResp(true, "属主隐式权限");
        }
        return new SecurityDtos.AclCheckResp(false, "无匹配授权（默认拒绝）");
    }

    private boolean matches(AclEntry entry, Long userId, Set<Long> roleIds, Set<Long> groupIds,
                            Set<Long> groupRoleIds, Long ownerId) {
        return switch (entry.getActorType()) {
            case "ALL" -> true;
            case "USER" -> userId.equals(entry.getActorId());
            case "ROLE" -> entry.getActorId() != null && roleIds.contains(entry.getActorId());
            case "GROUP" -> entry.getActorId() != null && groupIds.contains(entry.getActorId());
            case "GROUP_ROLE" -> entry.getActorId() != null && groupRoleIds.contains(entry.getActorId());
            case "OWNER" -> ownerId != null && ownerId.equals(userId);
            case "CREATOR" -> ownerId != null && ownerId.equals(userId);
            default -> false;
        };
    }

    private boolean containsAction(String actions, String action) {
        if (actions == null) {
            return false;
        }
        for (String item : actions.split(",")) {
            if (item.trim().equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    private Long queryOwner(Long objectId) {
        try {
            return jdbcTemplate.queryForObject("SELECT owner_id FROM plm_object WHERE id = ?", Long.class, objectId);
        } catch (Exception ex) {
            return null;
        }
    }

    private Set<Long> queryIds(String sql, Object... args) {
        try {
            return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, args));
        } catch (Exception ex) {
            return Set.of();
        }
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private void validate(String actorType, String actions, String effect) {
        String type = actorType == null ? "" : actorType.toUpperCase();
        if (!ACTOR_TYPES.contains(type)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不支持的被授权者类型：" + actorType);
        }
        if (actions == null || actions.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "授权操作不能为空");
        }
        if (effect != null && !effect.isBlank() && !EFFECTS.contains(effect.toUpperCase())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "effect 仅支持 ALLOW / DENY");
        }
    }

    private String normalizeActions(String actions) {
        List<String> normalized = new ArrayList<>();
        for (String item : actions.split(",")) {
            String trimmed = item.trim().toUpperCase();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!PermissionRuleService.ACTIONS.contains(trimmed)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不支持的操作：" + trimmed);
            }
            normalized.add(trimmed);
        }
        if (normalized.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "至少需要授予一个操作");
        }
        return String.join(",", normalized);
    }

    private Map<String, String> loadActorNames(List<AclEntry> entries) {
        Map<String, String> names = new HashMap<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> roleIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        for (AclEntry entry : entries) {
            if (entry.getActorId() == null) {
                continue;
            }
            switch (entry.getActorType()) {
                case "USER" -> userIds.add(entry.getActorId());
                case "ROLE" -> roleIds.add(entry.getActorId());
                case "GROUP" -> groupIds.add(entry.getActorId());
                default -> {
                }
            }
        }
        fill(names, "USER", "SELECT id, display_name AS name FROM sys_user WHERE id IN (%s)", userIds);
        fill(names, "ROLE", "SELECT id, name FROM sys_role WHERE id IN (%s)", roleIds);
        fill(names, "GROUP", "SELECT id, name FROM sys_group WHERE id IN (%s)", groupIds);
        return names;
    }

    private void fill(Map<String, String> target, String actorType, String sql, Set<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        try {
            jdbcTemplate.query(sql.formatted(placeholders(ids.size())), rs -> {
                target.put(actorType + ":" + rs.getLong(1), rs.getString(2));
            }, ids.toArray());
        } catch (Exception ignored) {
            // 名称回填失败不影响授权数据
        }
    }
}
