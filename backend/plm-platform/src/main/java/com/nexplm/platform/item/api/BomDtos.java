package com.nexplm.platform.item.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * BOM DTO。
 */
public final class BomDtos {

    private BomDtos() {
    }

    /**
     * BOM 树节点；children 为空表示叶子，hasChildren 用于前端判断是否显示展开箭头。
     * <p>{@code qty} 是单台用量（蓝本 {@code USEQTY}）；{@code totalQty} 为空表示按层级累乘计算。</p>
     */
    public record BomNode(Long lineId, Long childId, String code, String name, String itemType,
                          String specification, String unit, BigDecimal qty,
                          BigDecimal totalQty, BigDecimal surplusQty, BigDecimal subUseQty,
                          String bomType, String itemRoute, String dataSource,
                          BigDecimal lossRate,
                          Integer lineNo, LocalDate effectFrom, LocalDate effectTo,
                          String refDesignator, String remark, Integer version,
                          Boolean hasChildren, List<BomNode> children) {
    }

    public record BomLineSaveReq(
            @NotNull(message = "子件不能为空") Long childId,
            @NotNull(message = "用量不能为空") @DecimalMin(value = "0.000001", message = "用量必须大于 0") BigDecimal qty,
            @DecimalMin(value = "0", message = "总用量不能为负") BigDecimal totalQty,
            @DecimalMin(value = "0", message = "剩余用量不能为负") BigDecimal surplusQty,
            @DecimalMin(value = "0", message = "子用量不能为负") BigDecimal subUseQty,
            String bomType,
            String itemRoute,
            String dataSource,
            String unit,
            BigDecimal lossRate,
            LocalDate effectFrom,
            LocalDate effectTo,
            String refDesignator,
            String remark) {
    }

    public record BomLineUpdateReq(
            @NotNull(message = "用量不能为空") @DecimalMin(value = "0.000001", message = "用量必须大于 0") BigDecimal qty,
            @DecimalMin(value = "0", message = "总用量不能为负") BigDecimal totalQty,
            @DecimalMin(value = "0", message = "剩余用量不能为负") BigDecimal surplusQty,
            @DecimalMin(value = "0", message = "子用量不能为负") BigDecimal subUseQty,
            String bomType,
            String itemRoute,
            String dataSource,
            String unit,
            BigDecimal lossRate,
            LocalDate effectFrom,
            LocalDate effectTo,
            String refDesignator,
            String remark) {
    }

    /** 移动：newParentId 为空表示不动父件（仅调整序号）。 */
    public record BomMoveReq(Long newParentId, Integer newLineNo, String viewType) {
    }

    public record WhereUsedRow(Long lineId, Long parentId, String parentCode, String parentName,
                               String viewType, BigDecimal qty, Integer lineNo) {
    }

    public record BomIssue(String type, Long lineId, Long parentId, Long childId, String message) {
    }

    public record BomValidateResult(int total, List<BomIssue> issues) {
    }

    /** 展开行：qty 为按层级累乘后的用量，effectiveQty 再叠加本层损耗率；path 为层级路径（对应旧系统 LPATH）。 */
    public record BomSummaryRow(Integer level, String path, Long parentId, Long childId, String code, String name,
                                String itemType, String specification, String unit, BigDecimal qty,
                                BigDecimal lossRate, BigDecimal effectiveQty, Integer lineNo,
                                LocalDate effectFrom, LocalDate effectTo) {
    }

    /** 汇总行：同一子件在整棵树中出现的次数与累计用量。 */
    public record BomRollupRow(Long childId, String code, String name, String itemType,
                               String specification, String unit, BigDecimal totalQty,
                               BigDecimal totalEffectiveQty, Integer occurrences) {
    }

    /** 替代件行：itemObjectId 为被替换件，alternate* 为备选件信息。 */
    public record BomAlternateRow(Long id, Long itemObjectId, Long alternateId, String code, String name,
                                  String itemType, String specification, String unit, String viewType,
                                  Boolean isGlobal, Boolean isTwoWay, String priorityLevel, Integer lineNo,
                                  String remark, Integer version) {
    }

    /** 新增替代件：viewType 为空时取全局口径（isGlobal 默认 false，即按 DESIGN 视图的局部替代）。 */
    public record BomAlternateSaveReq(
            @NotNull(message = "备选件不能为空") Long alternateId,
            String viewType,
            Boolean isGlobal,
            Boolean isTwoWay,
            String priorityLevel,
            String remark) {
    }

    public record BomAlternateUpdateReq(
            Boolean isGlobal,
            Boolean isTwoWay,
            String priorityLevel,
            Integer lineNo,
            String remark) {
    }

    /** 基线头（冻结快照的元信息）。 */
    public record BomBaselineRow(Long id, Long rootObjectId, String code, String name, String viewType,
                                 Integer lineCount, OffsetDateTime frozenAt, String remark, Long createdBy) {
    }

    /** 基线快照行：path 为层级路径（与展开结果一一对应）。 */
    public record BomBaselineLineRow(Long id, Integer level, String path, Long parentId, Long childId,
                                     String code, String name, String itemType, String specification,
                                     String unit, BigDecimal qty, BigDecimal lossRate, Integer lineNo,
                                     LocalDate effectFrom, LocalDate effectTo) {
    }

    /** 创建基线：depth 为空时按最大层数冻结。 */
    public record BomBaselineSaveReq(
            @NotBlank(message = "基线编码不能为空") String code,
            String name,
            String viewType,
            Integer depth,
            String remark) {
    }

    /** 差异行：changeType = ADDED / REMOVED / QTY_CHANGED / CHILD_CHANGED。 */
    public record BomBaselineDiffRow(String changeType, String path, Integer level, Long childId, String code,
                                     String name, String unit, BigDecimal baselineQty, BigDecimal currentQty,
                                     BigDecimal baselineLossRate, BigDecimal currentLossRate) {
    }

    public record BomBaselineDiff(Long baselineId, String baselineCode, String viewType,
                                  int added, int removed, int changed, List<BomBaselineDiffRow> rows) {
    }

    /** Excel 导入预览行：action = NEW（新增）/ UPDATE（更新）/ SAME（无变化）。 */
    public record BomExcelPreviewRow(int rowNum, String levelCode, String parentLevelCode, Long childId,
                                     String code, String name, BigDecimal qty, BigDecimal lossRate, String unit,
                                     LocalDate effectFrom, LocalDate effectTo, String action) {
    }

    public record BomExcelPreview(int total, int valid, int invalid, int createCount, int updateCount,
                                  int unchangedCount, List<BomExcelPreviewRow> rows, List<String> errors) {
    }

    public record BomExcelImportResult(int created, int updated, int skipped) {
    }

    /** 批量改用量：只更新非空字段（qty/lossRate/unit），lineIds 必须属于该根件的 BOM 树。 */
    public record BomBatchQtyReq(
            @NotNull(message = "请选择要修改的 BOM 行") List<Long> lineIds,
            BigDecimal qty,
            BigDecimal lossRate,
            String unit) {
    }

    /** 批量替换子件：把该根件树中的 fromChildId 全部替换为 toChildId。 */
    public record BomBatchReplaceReq(
            @NotNull(message = "被替换子件不能为空") Long fromChildId,
            @NotNull(message = "替换目标子件不能为空") Long toChildId,
            String viewType,
            List<Long> parentIds) {
    }

    /** 批量结果：skipped 附带逐条原因，便于前端提示。 */
    public record BomBatchResult(int total, int updated, int skipped, List<String> messages) {
    }
}
