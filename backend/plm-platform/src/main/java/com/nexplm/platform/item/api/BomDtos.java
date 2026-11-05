package com.nexplm.platform.item.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * BOM DTO。
 */
public final class BomDtos {

    private BomDtos() {
    }

    /** BOM 树节点；children 为空表示叶子，hasChildren 用于前端判断是否显示展开箭头。 */
    public record BomNode(Long lineId, Long childId, String code, String name, String itemType,
                          String specification, String unit, BigDecimal qty, BigDecimal lossRate,
                          Integer lineNo, String refDesignator, String remark, Integer version,
                          Boolean hasChildren, List<BomNode> children) {
    }

    public record BomLineSaveReq(
            @NotNull(message = "子件不能为空") Long childId,
            @NotNull(message = "用量不能为空") @DecimalMin(value = "0.000001", message = "用量必须大于 0") BigDecimal qty,
            String unit,
            BigDecimal lossRate,
            String refDesignator,
            String remark) {
    }

    public record BomLineUpdateReq(
            @NotNull(message = "用量不能为空") @DecimalMin(value = "0.000001", message = "用量必须大于 0") BigDecimal qty,
            String unit,
            BigDecimal lossRate,
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
}
