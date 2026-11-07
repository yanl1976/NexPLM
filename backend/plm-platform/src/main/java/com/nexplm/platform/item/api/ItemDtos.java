package com.nexplm.platform.item.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 零部件 DTO。
 * <p>复刻旧系统 PSEITEM 页面：列表并列三状态（生命周期 / 使用状态 / 流程状态），
 * 详情页带设计属性（旧 PSEBASEFORM）与检出信息。</p>
 */
public final class ItemDtos {

    private ItemDtos() {
    }

    /** 列表行：含旧系统主列表要求的发布时间、创建人、修改人。 */
    public record ItemSummary(Long id, String code, String name, String itemType, String specification,
                              String material, String unit, String sourceType,
                              String lifecycleState, String phaseState, String usingState, String workflowState,
                              Integer revision, Integer iteration, String revisionLabel,
                              Long createdBy, String createdByName, Long updatedBy, String updatedByName,
                              OffsetDateTime releasedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record ItemDetail(Long id, String code, String name, String itemType, String specification,
                             String material, String unit, BigDecimal weight, String sourceType,
                             String stdCode, String remark, String lifecycleState, String phaseState,
                             String usingState, String workflowState, Integer revision, Integer iteration,
                             String revisionLabel, Boolean isLatest, Long ownerId, Long orgId,
                             Integer securityLevel, Long createdBy, String createdByName,
                             Long updatedBy, String updatedByName, OffsetDateTime releasedAt,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt,
                             ItemDesignAttr designAttr, CheckoutInfo checkout) {
    }

    /** 设计属性（旧系统 PSEBASEFORM）：独立表，随零件 1:1。 */
    public record ItemDesignAttr(String provider, String dwgNo, String productCategory, String productModel,
                                 String productSpec, String materialType, String materialType2,
                                 String surfaceTreatment, Boolean isTooling, String weightUnit,
                                 String montage, Boolean colorItem) {
    }

    /** 检出信息：谁在什么时间检出、是否当前用户。 */
    public record CheckoutInfo(Long userId, String userName, OffsetDateTime checkedOutAt,
                               OffsetDateTime expireAt, Boolean mine) {
    }

    public record ItemSaveReq(
            @Size(max = 128, message = "编码不能超过 128 个字符") String code,
            @NotBlank(message = "零部件名称不能为空") @Size(max = 500) String name,
            @NotBlank(message = "零部件类型不能为空") String itemType,
            @Size(max = 500) String specification,
            @Size(max = 64) String material,
            @Size(max = 32) String unit,
            BigDecimal weight,
            String sourceType,
            @Size(max = 128) String stdCode,
            @Size(max = 500) String remark,
            Long orgId,
            Integer securityLevel,
            ItemDesignAttr designAttr) {
    }

    /** 使用状态迁移（旧系统 STATEID：Using / StopUsing / GiveUp）。 */
    public record UsingStateReq(@NotBlank(message = "使用状态不能为空") String usingState) {
    }

    /** 零件视图行（旧系统 PSEITEMVIEW）。 */
    public record ItemViewRow(Long id, String viewType, String viewTypeLabel, String description,
                              Boolean isDefault, String lifecycleState, String workflowState,
                              Integer bomLineCount, OffsetDateTime createdAt) {
    }

    public record ItemViewReq(String viewType, @Size(max = 500) String description,
                              Long copyFromViewId) {
    }

    /** 版本行（旧系统「历史」页签）。 */
    public record VersionRow(Long id, Integer revision, Integer iteration, String revisionLabel,
                             String lifecycleState, Boolean isLatest, Long createdBy,
                             String createdByName, OffsetDateTime createdAt, OffsetDateTime releasedAt) {
    }

    /** 设计文档行（零件 ↔ 图文档关系）。 */
    public record ItemDocRow(Long id, String code, String name, String docType, String relationType,
                             String lifecycleState, String revisionLabel, OffsetDateTime updatedAt) {
    }

    public record LogRow(String logType, String action, String fromState, String toState, String detail,
                         String result, Long operatorId, String operatorLabel, String ip,
                         OffsetDateTime createdAt) {
    }

    /** 批量操作结果（部分成功语义，与 BOM 批量一致）。 */
    public record BatchResult(int total, int done, int skipped, java.util.List<String> messages) {
    }

    // ------------------------------------------------------------ 版本比较

    /**
     * 版本逐字段差异行。
     * <p>值均为<b>原始码值</b>（如 {@code MAKE} / {@code Released}），中文展示由前端字典映射，
     * 避免后端为了一处 diff 再复制一套枚举翻译。</p>
     */
    public record VersionDiff(String group, String field, String label, String baseValue,
                              String targetValue, boolean changed) {
    }

    public record VersionCompare(Long baseId, String baseLabel, Long targetId, String targetLabel,
                                 java.util.List<VersionDiff> rows, int changedCount, int totalCount) {
    }

    // ------------------------------------------------------------ 批量新建 / 批量改属性 / 转交所有权

    /** 批量新建：逐条独立处理，名称或类型非法的行记入 messages 并跳过。 */
    public record BatchCreateReq(java.util.List<ItemSaveReq> items) {
    }

    /**
     * 批量改属性补丁：<b>null 表示不修改</b>该字段。
     * <p>与 {@link ItemSaveReq} 的区别是「缺省即保留」，因此不能直接复用全量保存接口。</p>
     */
    public record ItemBatchPatch(String code, String name, String itemType, String specification,
                                 String material, String unit, BigDecimal weight, String sourceType,
                                 String stdCode, String remark, Integer securityLevel,
                                 ItemDesignAttr designAttr) {
    }

    public record BatchUpdateReq(@NotEmpty(message = "请至少选择一个零部件")
                                 java.util.List<Long> ids,
                                 @NotNull(message = "请提供要修改的属性")
                                 ItemBatchPatch patch) {
    }

    /** 转交所有权：把选中零部件的属主改为目标用户。 */
    public record TransferOwnerReq(@NotEmpty(message = "请至少选择一个零部件")
                                   java.util.List<Long> ids,
                                   @NotNull(message = "请选择接手人")
                                   Long targetUserId) {
    }
}
