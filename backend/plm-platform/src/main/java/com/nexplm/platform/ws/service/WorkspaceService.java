package com.nexplm.platform.ws.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.ws.api.WsDtos;
import com.nexplm.platform.ws.domain.WsFolder;
import com.nexplm.platform.ws.domain.WsFolderContent;
import com.nexplm.platform.ws.mapper.WsFolderContentMapper;
import com.nexplm.platform.ws.mapper.WsFolderMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 工作空间服务：三种空间（我的/公共/组）的文件夹树、文件夹内容、收藏与最近访问。
 * <p>权限模型（P2 阶段）：</p>
 * <ul>
 *   <li>MY 空间：仅属主可见可写；</li>
 *   <li>PUBLIC 空间：同公司可见，暂按同公司可写（P4 接入 ACL 后收紧）；</li>
 *   <li>GROUP 空间：用户所属组可见可写。</li>
 * </ul>
 * <p>文件夹移动用物化路径前缀重写子树，禁止移动到自身或自己的后代（防环）。</p>
 */
@Service
public class WorkspaceService {

    private static final Set<String> SPACES =
            Set.of(WsFolder.SPACE_MY, WsFolder.SPACE_PUBLIC, WsFolder.SPACE_GROUP);
    private static final int RECENT_LIMIT_MAX = 100;

    private final WsFolderMapper folderMapper;
    private final WsFolderContentMapper contentMapper;
    private final PlmObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public WorkspaceService(WsFolderMapper folderMapper, WsFolderContentMapper contentMapper,
                            PlmObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.folderMapper = folderMapper;
        this.contentMapper = contentMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------ 文件夹

    public List<WsDtos.FolderNode> listFolders(String spaceType, Long userId, Long companyId) {
        String space = normalizeSpace(spaceType);
        LambdaQueryWrapper<WsFolder> wrapper = new LambdaQueryWrapper<WsFolder>()
                .eq(WsFolder::getSpaceType, space)
                .orderByAsc(WsFolder::getPath)
                .orderByAsc(WsFolder::getSort);
        switch (space) {
            case WsFolder.SPACE_MY -> wrapper.eq(WsFolder::getOwnerId, userId);
            case WsFolder.SPACE_PUBLIC -> wrapper.eq(WsFolder::getCompanyId, companyId);
            default -> {
                List<Long> groupIds = myGroupIds(userId);
                if (groupIds.isEmpty()) {
                    wrapper.apply("1 = 0");
                } else {
                    wrapper.in(WsFolder::getGroupId, groupIds);
                }
            }
        }
        return folderMapper.selectList(wrapper).stream().map(this::toNode).toList();
    }

    @Transactional
    public WsDtos.FolderNode createFolder(WsDtos.FolderCreateReq req, Long userId, Long companyId) {
        String space = normalizeSpace(req.spaceType());
        WsFolder parent = null;
        if (req.parentId() != null) {
            parent = requireFolder(req.parentId());
            if (!space.equals(parent.getSpaceType())) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "父文件夹不在同一空间");
            }
            assertVisible(parent, userId, companyId);
        }
        Long groupId = null;
        if (WsFolder.SPACE_GROUP.equals(space)) {
            groupId = req.groupId() != null ? req.groupId()
                    : (parent != null ? parent.getGroupId() : null);
            if (groupId == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "组空间必须指定所属组");
            }
            if (!myGroupIds(userId).contains(groupId)) {
                throw BizException.of(ErrorCode.COMMON_FORBIDDEN, "当前用户不属于该组");
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        long id = IdWorker.getId();
        WsFolder folder = new WsFolder();
        folder.setId(id);
        folder.setCompanyId(companyId);
        folder.setSpaceType(space);
        folder.setParentId(req.parentId());
        folder.setPath((parent == null ? "/" : parent.getPath()) + id + "/");
        folder.setName(req.name().trim());
        folder.setOwnerId(WsFolder.SPACE_MY.equals(space) || parent == null
                ? userId : parent.getOwnerId());
        folder.setGroupId(groupId);
        folder.setAclInherit(req.aclInherit() == null ? Boolean.TRUE : req.aclInherit());
        folder.setSort(req.sort() == null ? 0 : req.sort());
        folder.setDeleted(false);
        folder.setCreatedBy(userId);
        folder.setCreatedAt(now);
        folder.setUpdatedBy(userId);
        folder.setUpdatedAt(now);
        folderMapper.insert(folder);
        return toNode(folder);
    }

    @Transactional
    public WsDtos.FolderNode renameFolder(Long id, String name, Long userId, Long companyId) {
        WsFolder folder = requireFolder(id);
        assertWritable(folder, userId, companyId);
        folder.setName(name.trim());
        folder.setUpdatedBy(userId);
        folder.setUpdatedAt(OffsetDateTime.now());
        folderMapper.updateById(folder);
        return toNode(folder);
    }

    @Transactional
    public void moveFolder(Long id, Long newParentId, Long userId, Long companyId) {
        WsFolder folder = requireFolder(id);
        assertWritable(folder, userId, companyId);
        WsFolder parent = null;
        if (newParentId != null) {
            parent = requireFolder(newParentId);
            if (parent.getId().equals(folder.getId())) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能移动到自身");
            }
            if (parent.getPath().startsWith(folder.getPath())) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能移动到自己的子文件夹");
            }
            if (!parent.getSpaceType().equals(folder.getSpaceType())) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "目标文件夹不在同一空间");
            }
            assertWritable(parent, userId, companyId);
        }
        String oldPath = folder.getPath();
        String newPath = (parent == null ? "/" : parent.getPath()) + folder.getId() + "/";
        if (oldPath.equals(newPath)) {
            return;
        }
        // 移到空间根时 parentId 需显式置空，updateById 会跳过 null 字段
        folderMapper.update(null, new LambdaUpdateWrapper<WsFolder>()
                .eq(WsFolder::getId, folder.getId())
                .set(WsFolder::getParentId, newParentId)
                .set(WsFolder::getPath, newPath)
                .set(WsFolder::getUpdatedBy, userId)
                .set(WsFolder::getUpdatedAt, OffsetDateTime.now()));
        folderMapper.rebaseSubtree(oldPath, newPath, oldPath.length() + 1, folder.getId());
    }

    /** 删除文件夹：要求为空（无子目录、无内容），逻辑删除。 */
    @Transactional
    public void deleteFolder(Long id, Long userId, Long companyId) {
        WsFolder folder = requireFolder(id);
        assertWritable(folder, userId, companyId);
        Long children = folderMapper.selectCount(new LambdaQueryWrapper<WsFolder>()
                .eq(WsFolder::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "文件夹下还有子文件夹，请先清空");
        }
        if (contentMapper.countByFolder(id) > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "文件夹下还有内容，请先移除");
        }
        folderMapper.deleteById(id);
    }

    // ------------------------------------------------------------ 文件夹内容

    public List<WsDtos.ContentItem> listContent(Long folderId, Long userId, Long companyId) {
        WsFolder folder = requireFolder(folderId);
        assertVisible(folder, userId, companyId);
        return jdbcTemplate.query("""
                        SELECT c.id, c.folder_id, c.object_id, o.object_type, o.code, o.name,
                               o.lifecycle_state, c.sort, c.created_at
                          FROM ws_folder_content c
                          JOIN plm_object o ON o.id = c.object_id AND o.deleted = false
                         WHERE c.folder_id = ?
                         ORDER BY c.sort, c.created_at
                        """,
                (rs, rowNum) -> new WsDtos.ContentItem(rs.getLong("id"), rs.getLong("folder_id"),
                        rs.getLong("object_id"), rs.getString("object_type"), rs.getString("code"),
                        rs.getString("name"), rs.getString("lifecycle_state"), rs.getInt("sort"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                folderId);
    }

    @Transactional
    public void addContent(Long folderId, Long objectId, Integer sort, Long userId, Long companyId) {
        WsFolder folder = requireFolder(folderId);
        assertWritable(folder, userId, companyId);
        requireObject(objectId);
        if (contentMapper.countByFolderAndObject(folderId, objectId) > 0) {
            return;
        }
        WsFolderContent content = new WsFolderContent();
        content.setFolderId(folderId);
        content.setObjectId(objectId);
        content.setSort(sort == null ? 0 : sort);
        content.setCreatedBy(userId);
        content.setCreatedAt(OffsetDateTime.now());
        contentMapper.insert(content);
    }

    @Transactional
    public void removeContent(Long folderId, Long objectId, Long userId, Long companyId) {
        WsFolder folder = requireFolder(folderId);
        assertWritable(folder, userId, companyId);
        contentMapper.delete(new LambdaQueryWrapper<WsFolderContent>()
                .eq(WsFolderContent::getFolderId, folderId)
                .eq(WsFolderContent::getObjectId, objectId));
    }

    /** 把对象在文件夹之间移动（同一对象可被多个文件夹引用，这里只处理指定来源）。 */
    @Transactional
    public void moveContent(Long objectId, Long fromFolderId, Long toFolderId, Long userId, Long companyId) {
        WsFolder target = requireFolder(toFolderId);
        assertWritable(target, userId, companyId);
        requireObject(objectId);
        if (fromFolderId != null && !fromFolderId.equals(toFolderId)) {
            WsFolder source = requireFolder(fromFolderId);
            assertWritable(source, userId, companyId);
            contentMapper.delete(new LambdaQueryWrapper<WsFolderContent>()
                    .eq(WsFolderContent::getFolderId, fromFolderId)
                    .eq(WsFolderContent::getObjectId, objectId));
        }
        if (contentMapper.countByFolderAndObject(toFolderId, objectId) == 0) {
            WsFolderContent content = new WsFolderContent();
            content.setFolderId(toFolderId);
            content.setObjectId(objectId);
            content.setSort(0);
            content.setCreatedBy(userId);
            content.setCreatedAt(OffsetDateTime.now());
            contentMapper.insert(content);
        }
    }

    // ------------------------------------------------------------ 收藏与最近访问

    @Transactional
    public void addFavorite(Long objectId, Long userId) {
        requireObject(objectId);
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ws_favorite WHERE user_id = ? AND object_id = ?",
                Integer.class, userId, objectId);
        if (exists != null && exists > 0) {
            return;
        }
        jdbcTemplate.update("INSERT INTO ws_favorite (id, user_id, object_id) VALUES (?, ?, ?)",
                IdWorker.getId(), userId, objectId);
    }

    public void removeFavorite(Long objectId, Long userId) {
        jdbcTemplate.update("DELETE FROM ws_favorite WHERE user_id = ? AND object_id = ?", userId, objectId);
    }

    public List<WsDtos.FavoriteItem> listFavorites(Long userId) {
        return jdbcTemplate.query("""
                        SELECT f.object_id, o.object_type, o.code, o.name, o.lifecycle_state, f.created_at
                          FROM ws_favorite f
                          JOIN plm_object o ON o.id = f.object_id AND o.deleted = false
                         WHERE f.user_id = ?
                         ORDER BY f.created_at DESC
                        """,
                (rs, rowNum) -> new WsDtos.FavoriteItem(rs.getLong("object_id"), rs.getString("object_type"),
                        rs.getString("code"), rs.getString("name"), rs.getString("lifecycle_state"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                userId);
    }

    public void touchRecent(Long objectId, Long userId) {
        requireObject(objectId);
        jdbcTemplate.update("""
                        INSERT INTO ws_recent (user_id, object_id, accessed_at) VALUES (?, ?, now())
                        ON CONFLICT (user_id, object_id) DO UPDATE SET accessed_at = now()
                        """, userId, objectId);
    }

    public List<WsDtos.RecentItem> listRecent(Long userId, int limit) {
        int size = Math.min(Math.max(1, limit), RECENT_LIMIT_MAX);
        return jdbcTemplate.query("""
                        SELECT r.object_id, o.object_type, o.code, o.name, o.lifecycle_state, r.accessed_at
                          FROM ws_recent r
                          JOIN plm_object o ON o.id = r.object_id AND o.deleted = false
                         WHERE r.user_id = ?
                         ORDER BY r.accessed_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new WsDtos.RecentItem(rs.getLong("object_id"), rs.getString("object_type"),
                        rs.getString("code"), rs.getString("name"), rs.getString("lifecycle_state"),
                        rs.getObject("accessed_at", OffsetDateTime.class)),
                userId, size);
    }

    // ------------------------------------------------------------ 内部

    private String normalizeSpace(String spaceType) {
        String space = spaceType == null ? WsFolder.SPACE_MY : spaceType.trim().toUpperCase(Locale.ROOT);
        if (!SPACES.contains(space)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "空间类型不合法，可选：MY/PUBLIC/GROUP");
        }
        return space;
    }

    private WsFolder requireFolder(Long id) {
        if (id == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "文件夹标识不能为空");
        }
        WsFolder folder = folderMapper.selectById(id);
        if (folder == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "文件夹不存在：" + id);
        }
        return folder;
    }

    private void requireObject(Long objectId) {
        PlmObject object = objectMapper.selectById(objectId);
        if (object == null) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND);
        }
    }

    private void assertVisible(WsFolder folder, Long userId, Long companyId) {
        switch (folder.getSpaceType()) {
            case WsFolder.SPACE_MY -> {
                if (!userId.equals(folder.getOwnerId())) {
                    throw BizException.of(ErrorCode.COMMON_FORBIDDEN, "无权访问他人的个人空间");
                }
            }
            case WsFolder.SPACE_PUBLIC -> {
                if (!companyId.equals(folder.getCompanyId())) {
                    throw BizException.of(ErrorCode.COMMON_FORBIDDEN, "无权访问其他公司的公共空间");
                }
            }
            case WsFolder.SPACE_GROUP -> {
                if (folder.getGroupId() == null || !myGroupIds(userId).contains(folder.getGroupId())) {
                    throw BizException.of(ErrorCode.COMMON_FORBIDDEN, "无权访问该组空间");
                }
            }
            default -> throw BizException.of(ErrorCode.COMMON_FORBIDDEN);
        }
    }

    private void assertWritable(WsFolder folder, Long userId, Long companyId) {
        assertVisible(folder, userId, companyId);
    }

    private List<Long> myGroupIds(Long userId) {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT group_id FROM sys_user_group WHERE user_id = ?", Long.class, userId);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private WsDtos.FolderNode toNode(WsFolder folder) {
        return new WsDtos.FolderNode(folder.getId(), folder.getParentId(), folder.getName(),
                folder.getSpaceType(), folder.getPath(), folder.getOwnerId(), folder.getGroupId(),
                folder.getAclInherit(), folder.getSort(), folder.getCreatedAt());
    }
}
