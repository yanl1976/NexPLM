package com.nexplm.platform.workspace.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.doc.service.CheckoutService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 工作空间控制台（按蓝本「工作空间」页面）：三空间树 + 对象列表。
 *
 * <p>与既有 `/api/v1/workspace`（我的空间/收藏/最近）并存：那套是个人视图，
 * 这套是"三空间 + 项目文件夹 + 对象列表"的浏览器视图。</p>
 */
@Tag(name = "工作空间控制台")
@RestController
@RequestMapping("/api/v1/ws-console")
public class WorkspaceConsoleController {

    private final JdbcTemplate jdbcTemplate;
    private final CheckoutService checkoutService;

    public WorkspaceConsoleController(JdbcTemplate jdbcTemplate, CheckoutService checkoutService) {
        this.jdbcTemplate = jdbcTemplate;
        this.checkoutService = checkoutService;
    }

    /** 检出：锁定对象，其他人不能再编辑（蓝本 CHECKOUT）。 */
    @Operation(summary = "检出对象")
    @PostMapping("/objects/{id}/checkout")
    @PreAuthorize("hasAuthority('pse.item.write')")
    public Result<CheckoutService.Lock> checkout(@PathVariable Long id) {
        return Result.ok(checkoutService.acquire(id, CurrentUser.userId(), null, null, null));
    }

    /** 取消检出：只能由检出人自己释放。 */
    @Operation(summary = "取消检出")
    @DeleteMapping("/objects/{id}/checkout")
    @PreAuthorize("hasAuthority('pse.item.write')")
    public Result<Void> checkin(@PathVariable Long id) {
        checkoutService.release(id, CurrentUser.userId());
        return Result.ok();
    }

    /** 文件夹扁平列表；space = MY / PUBLIC / PROJECT。 */
    @Operation(summary = "工作空间文件夹（扁平，前端按 parentId 拼树）")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<Map<String, Object>>> tree(@RequestParam(defaultValue = "MY") String space) {
        if ("PROJECT".equalsIgnoreCase(space)) {
            return Result.ok(jdbcTemplate.queryForList("""
                    SELECT f.id, f.parent_id, f.name, f.path, 'PROJECT' AS space,
                           f.project_id, p.name AS project_name
                      FROM prj_project_folder f
                      LEFT JOIN prj_project p ON p.id = f.project_id
                     WHERE f.deleted = false
                     ORDER BY f.path
                    """));
        }
        String type = space == null || space.isBlank() ? "MY" : space.trim().toUpperCase();
        return Result.ok(jdbcTemplate.queryForList("""
                SELECT id, parent_id, name, path, space_type AS space,
                       NULL::bigint AS project_id, NULL::varchar AS project_name
                  FROM ws_folder
                 WHERE deleted = false AND space_type = ?
                 ORDER BY path
                """, type));
    }

    /** 文件夹内容（对象列表，带项目代号 / 规格型号 / 检出信息）。 */
    @Operation(summary = "文件夹内容（对象列表）")
    @GetMapping("/objects")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<Map<String, Object>>> objects(@RequestParam String space,
                                                     @RequestParam Long folderId) {
        String table = "PROJECT".equalsIgnoreCase(space) ? "prj_project_content" : "ws_folder_content";
        String column = "PROJECT".equalsIgnoreCase(space) ? "project_folder_id" : "folder_id";
        return Result.ok(jdbcTemplate.queryForList("""
                SELECT o.id, o.object_type, o.code, o.name,
                       o.revision, o.iteration, o.lifecycle_state, o.workflow_state,
                       m.item_type, m.using_state, m.specification,
                       p.code AS project_code,
                       u.username AS checkout_user,
                       c.checked_out_at
                  FROM %s f
                  JOIN plm_object o ON o.id = f.object_id AND o.deleted = false
                  LEFT JOIN item_master m ON m.object_id = o.id
                  LEFT JOIN prj_project p ON p.id = m.project_id
                  LEFT JOIN plm_checkout c ON c.object_id = o.id
                  LEFT JOIN sys_user u ON u.id = c.user_id
                 WHERE f.%s = ?
                 ORDER BY o.code
                """.formatted(table, column), folderId));
    }
}
