package com.nexplm.platform.ws.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 工作空间 DTO。
 */
public final class WsDtos {

    private WsDtos() {
    }

    public record FolderNode(Long id, Long parentId, String name, String spaceType, String path,
                             Long ownerId, Long groupId, Boolean aclInherit, Integer sort,
                             OffsetDateTime createdAt) {
    }

    public record FolderCreateReq(
            @NotBlank(message = "空间类型不能为空") String spaceType,
            Long parentId,
            @NotBlank(message = "文件夹名称不能为空") @Size(max = 200) String name,
            Long groupId,
            Boolean aclInherit,
            Integer sort) {
    }

    public record FolderRenameReq(@NotBlank(message = "文件夹名称不能为空") @Size(max = 200) String name) {
    }

    /** 移动到目标文件夹；{@code parentId} 为空表示移到空间根。 */
    public record FolderMoveReq(Long parentId) {
    }

    public record ContentAddReq(@NotNull(message = "对象不能为空") Long objectId, Integer sort) {
    }

    public record ContentMoveReq(@NotNull(message = "对象不能为空") Long objectId,
                                 Long fromFolderId,
                                 @NotNull(message = "目标文件夹不能为空") Long toFolderId) {
    }

    public record ContentItem(Long id, Long folderId, Long objectId, String objectType, String code,
                              String name, String lifecycleState, Integer sort, OffsetDateTime addedAt) {
    }

    public record FavoriteItem(Long objectId, String objectType, String code, String name,
                               String lifecycleState, OffsetDateTime createdAt) {
    }

    public record RecentItem(Long objectId, String objectType, String code, String name,
                             String lifecycleState, OffsetDateTime accessedAt) {
    }
}
