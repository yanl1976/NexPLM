package com.nexplm.platform.doc.api;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.Result;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 图文档文件夹（DRW-05）。
 *
 * <p>蓝本用 `WSFOLDERCONTENT`（`CETYPE='DOCMASTER'`）把文档挂到文件夹，与分类
 * `DOCDATASET.DATASETCLASS` 是两套维度：**文件夹用于整理/查找，分类用于属性**。
 * 新系统复用 `ws_folder`（`space_type='DOC'`）+ `ws_folder_content`。</p>
 */
@Tag(name = "图文档文件夹")
@RestController
@RequestMapping("/api/v1/doc-folders")
public class DocFolderController {

    private static final String SPACE = "DOC";

    private final JdbcTemplate jdbcTemplate;

    public DocFolderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 当前用户的文档文件夹（扁平，前端按 parent_id 组树）。 */
    @Operation(summary = "文档文件夹树")
    @GetMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<Map<String, Object>>> tree() {
        Long userId = CurrentUser.userId();
        return Result.ok(jdbcTemplate.queryForList("""
                SELECT id, parent_id, name, path, sort
                  FROM ws_folder
                 WHERE space_type = ? AND owner_id = ? AND deleted = false
                 ORDER BY path, sort, id
                """, SPACE, userId));
    }

    /** 新建文件夹。 */
    @Operation(summary = "新建文档文件夹")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Map<String, Object>> create(@Valid @RequestBody FolderReq req) {
        Long userId = CurrentUser.userId();
        Long companyId = CurrentUser.companyId() == null ? 0L : CurrentUser.companyId();
        String parentPath = "/";
        if (req.parentId() != null) {
            List<String> paths = jdbcTemplate.queryForList("""
                    SELECT path FROM ws_folder
                     WHERE id = ? AND space_type = ? AND owner_id = ? AND deleted = false
                    """, String.class, req.parentId(), SPACE, userId);
            if (paths.isEmpty()) {
                throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "父文件夹不存在：" + req.parentId());
            }
            parentPath = paths.get(0);
        }
        String name = req.name().trim();
        Long exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM ws_folder
                 WHERE space_type = ? AND owner_id = ? AND deleted = false
                   AND COALESCE(parent_id, -1) = COALESCE(?, -1) AND name = ?
                """, Long.class, SPACE, userId, req.parentId(), name);
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "同级已存在同名文件夹：" + name);
        }
        Long id = IdWorker.getId();
        String path = ("/".equals(parentPath) ? "" : parentPath) + "/" + name;
        jdbcTemplate.update("""
                INSERT INTO ws_folder (id, company_id, org_id, space_type, parent_id, path, name, owner_id,
                                       group_id, acl_inherit, sort, deleted, created_by, created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, ?, ?, ?, NULL, true, 0, false, ?, now(), now())
                """, id, companyId, SPACE, req.parentId(), path, name, userId, userId);
        return Result.ok(Map.of("id", id, "name", name, "path", path));
    }

    /** 重命名文件夹（同步子孙 path 前缀）。 */
    @Operation(summary = "重命名文档文件夹")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Void> rename(@PathVariable Long id, @Valid @RequestBody FolderReq req) {
        Long userId = CurrentUser.userId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, path FROM ws_folder
                 WHERE id = ? AND space_type = ? AND owner_id = ? AND deleted = false
                """, id, SPACE, userId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "文件夹不存在：" + id);
        }
        String oldPath = String.valueOf(rows.get(0).get("path"));
        String name = req.name().trim();
        String newPath = oldPath.substring(0, oldPath.lastIndexOf('/') + 1) + name;
        jdbcTemplate.update("""
                UPDATE ws_folder SET name = ?, path = ?, updated_at = now() WHERE id = ?
                """, name, newPath, id);
        for (Map<String, Object> child : jdbcTemplate.queryForList("""
                SELECT id, path FROM ws_folder WHERE path LIKE ? AND deleted = false
                """, oldPath + "/%")) {
            String childPath = String.valueOf(child.get("path"));
            jdbcTemplate.update("UPDATE ws_folder SET path = ?, updated_at = now() WHERE id = ?",
                    newPath + childPath.substring(oldPath.length()), child.get("id"));
        }
        return Result.ok();
    }

    /** 删除文件夹（有子文件夹时拒绝，避免误删整棵树）。 */
    @Operation(summary = "删除文档文件夹")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = CurrentUser.userId();
        Long children = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM ws_folder
                 WHERE parent_id = ? AND space_type = ? AND owner_id = ? AND deleted = false
                """, Long.class, id, SPACE, userId);
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "请先删除子文件夹");
        }
        jdbcTemplate.update("""
                UPDATE ws_folder SET deleted = true, updated_at = now()
                 WHERE id = ? AND space_type = ? AND owner_id = ?
                """, id, SPACE, userId);
        jdbcTemplate.update("DELETE FROM ws_folder_content WHERE folder_id = ?", id);
        return Result.ok();
    }

    /** 把文档加入文件夹（不改变文档分类）。 */
    @Operation(summary = "文档加入文件夹")
    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Void> addDocument(@PathVariable Long id, @RequestParam Long docId) {
        requireFolder(id);
        Long exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM ws_folder_content WHERE folder_id = ? AND object_id = ?
                """, Long.class, id, docId);
        if (exists != null && exists > 0) {
            return Result.ok();
        }
        jdbcTemplate.update("""
                INSERT INTO ws_folder_content (id, folder_id, object_id, sort, created_by, created_at)
                VALUES (?, ?, ?, 0, ?, now())
                """, IdWorker.getId(), id, docId, CurrentUser.userId());
        return Result.ok();
    }

    /** 从文件夹移出（只删映射）。 */
    @Operation(summary = "文档移出文件夹")
    @DeleteMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public Result<Void> removeDocument(@PathVariable Long id, @PathVariable Long docId) {
        jdbcTemplate.update("DELETE FROM ws_folder_content WHERE folder_id = ? AND object_id = ?", id, docId);
        return Result.ok();
    }

    private void requireFolder(Long id) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM ws_folder
                 WHERE id = ? AND space_type = ? AND owner_id = ? AND deleted = false
                """, Long.class, id, SPACE, CurrentUser.userId());
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "文件夹不存在：" + id);
        }
    }

    /** 文件夹请求。 */
    public record FolderReq(Long parentId,
                            @NotBlank(message = "文件夹名称不能为空") @Size(max = 100) String name) {
    }
}
