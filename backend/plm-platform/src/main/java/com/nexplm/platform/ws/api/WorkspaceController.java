package com.nexplm.platform.ws.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.ws.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/**
 * 工作空间接口：文件夹树、文件夹内容、收藏、最近访问。
 */
@Tag(name = "PDM-工作空间")
@RestController
@RequestMapping("/api/v1/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "文件夹树（平铺返回，前端按 parentId 组树）")
    @GetMapping("/folders")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<WsDtos.FolderNode>> folders(
            @RequestParam(value = "space", defaultValue = "MY") String space) {
        return Result.ok(workspaceService.listFolders(space, CurrentUser.userId(), CurrentUser.companyId()));
    }

    @Operation(summary = "新建文件夹")
    @PostMapping("/folders")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_FOLDER_CREATE", objectType = "WsFolder")
    public Result<WsDtos.FolderNode> createFolder(@Valid @RequestBody WsDtos.FolderCreateReq req) {
        return Result.ok(workspaceService.createFolder(req, CurrentUser.userId(), CurrentUser.companyId()));
    }

    @Operation(summary = "重命名文件夹")
    @PutMapping("/folders/{id}/name")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_FOLDER_RENAME", objectType = "WsFolder")
    public Result<WsDtos.FolderNode> renameFolder(@PathVariable Long id,
                                                  @Valid @RequestBody WsDtos.FolderRenameReq req) {
        return Result.ok(workspaceService.renameFolder(id, req.name(),
                CurrentUser.userId(), CurrentUser.companyId()));
    }

    @Operation(summary = "移动文件夹（parentId 为空表示移到空间根）")
    @PutMapping("/folders/{id}/parent")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_FOLDER_MOVE", objectType = "WsFolder")
    public Result<Void> moveFolder(@PathVariable Long id,
                                   @RequestBody WsDtos.FolderMoveReq req) {
        workspaceService.moveFolder(id, req.parentId(), CurrentUser.userId(), CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "删除文件夹（需先清空）")
    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_FOLDER_DELETE", objectType = "WsFolder")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        workspaceService.deleteFolder(id, CurrentUser.userId(), CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "文件夹内容")
    @GetMapping("/folders/{id}/content")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<WsDtos.ContentItem>> content(@PathVariable Long id) {
        return Result.ok(workspaceService.listContent(id, CurrentUser.userId(), CurrentUser.companyId()));
    }

    @Operation(summary = "把对象加入文件夹")
    @PostMapping("/folders/{id}/content")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_CONTENT_ADD", objectType = "WsFolder")
    public Result<Void> addContent(@PathVariable Long id, @Valid @RequestBody WsDtos.ContentAddReq req) {
        workspaceService.addContent(id, req.objectId(), req.sort(),
                CurrentUser.userId(), CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "从文件夹移除对象")
    @DeleteMapping("/folders/{id}/content/{objectId}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_CONTENT_REMOVE", objectType = "WsFolder")
    public Result<Void> removeContent(@PathVariable Long id, @PathVariable Long objectId) {
        workspaceService.removeContent(id, objectId, CurrentUser.userId(), CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "在文件夹之间移动对象")
    @PostMapping("/content/move")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "WS_CONTENT_MOVE", objectType = "WsFolder")
    public Result<Void> moveContent(@Valid @RequestBody WsDtos.ContentMoveReq req) {
        workspaceService.moveContent(req.objectId(), req.fromFolderId(), req.toFolderId(),
                CurrentUser.userId(), CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "收藏列表")
    @GetMapping("/favorites")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<WsDtos.FavoriteItem>> favorites() {
        return Result.ok(workspaceService.listFavorites(CurrentUser.userId()));
    }

    @Operation(summary = "添加收藏")
    @PostMapping("/favorites/{objectId}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<Void> addFavorite(@PathVariable Long objectId) {
        workspaceService.addFavorite(objectId, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorites/{objectId}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<Void> removeFavorite(@PathVariable Long objectId) {
        workspaceService.removeFavorite(objectId, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "最近访问")
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<WsDtos.RecentItem>> recent(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Result.ok(workspaceService.listRecent(CurrentUser.userId(), limit));
    }

    @Operation(summary = "记录一次对象访问")
    @PostMapping("/recent/{objectId}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<Void> touchRecent(@PathVariable Long objectId) {
        workspaceService.touchRecent(objectId, CurrentUser.userId());
        return Result.ok();
    }
}
