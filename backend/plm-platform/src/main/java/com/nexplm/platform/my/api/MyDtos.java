package com.nexplm.platform.my.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** 个人工作区（我的检出 / 收藏夹 / 最近访问 / 站内消息 / 高级查询）的接口契约。 */
public final class MyDtos {

    private MyDtos() {
    }

    /**
     * 我的检出。列口径对齐旧系统 {@code CHECKOUTOBJECTVIEW.view}：
     * 对象类型 / 编码 / 检出时间，按检出时间倒序。
     */
    public record CheckoutRow(String objectType, String objectTypeName, Long objectId,
                              String code, String name, String revision,
                              OffsetDateTime checkedOutAt, OffsetDateTime expireAt, boolean expired) {
    }

    /** 收藏夹条目，对齐旧系统 {@code FAVORITELINK}（收藏对象 + 父级分组 + 顺序）。 */
    public record FavoriteRow(Long id, String objectType, String objectTypeName, Long objectId,
                              String code, String name, String lifecycleState,
                              String folder, Integer seqNo, String remark, OffsetDateTime createdAt) {
    }

    /** 最近访问条目，对齐旧系统 {@code AccessRecord}（对象 + 访问类型 + 时间）。 */
    public record RecentRow(String objectType, String objectTypeName, Long objectId,
                            String code, String name, String accessType, OffsetDateTime accessedAt) {
    }

    /** 站内消息（复用通知台账的收件箱视角）。 */
    public record MessageRow(Long id, String eventType, String title, String content,
                             String status, OffsetDateTime createdAt, OffsetDateTime readAt) {
    }

    /** 高级查询结果行（跨文档/零部件/变更单）。 */
    public record SearchRow(String objectType, String objectTypeName, Long objectId,
                            String code, String name, String lifecycleState,
                            String revision, OffsetDateTime updatedAt) {
    }

    public record FavoriteReq(@NotBlank String objectType,
                              @NotNull Long objectId,
                              @Size(max = 100) String folder,
                              @Size(max = 500) String remark) {
    }

    public record AccessReq(@NotBlank String objectType,
                            @NotNull Long objectId,
                            @Size(max = 16) String accessType) {
    }
}
