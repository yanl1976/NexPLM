package com.nexplm.platform.capp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 工厂结构（工位）与消耗定义，对齐旧系统 {@code PositionManager}（POSITIONITEM 树）
 * 与 {@code ConsumptionStation}（CSOBJ 消耗站）。
 */
public final class CappPositionDtos {

    private CappPositionDtos() {
    }

    /** 树节点：旧系统 DEEPPOSITIONLINKVIEW 的 ECODE/ENAME/POSITIONLEVEL/LOGISTICSUNIT + 父子。 */
    public record PositionNode(Long id, String code, String name, Long parentId, String positionLevel,
                               String logisticsUnit, BigDecimal capacity, Integer shifts,
                               Boolean enabled, String remark, List<PositionNode> children) {
    }

    public record PositionReq(
            @Size(max = 64) String code,
            @NotBlank @Size(max = 200) String name,
            Long parentId,
            @Size(max = 16) String positionLevel,
            @Size(max = 64) String logisticsUnit,
            BigDecimal capacity,
            Integer shifts,
            Boolean enabled,
            @Size(max = 500) String remark) {
    }

    /** 消耗定义：旧系统 CSOBJ（物料 + 用量 + 适用性 + 适配产品 + 适用全部路线）。 */
    public record ConsumptionRow(Long id, String code, String name, Long positionId,
                                 String positionCode, String positionName,
                                 Long objectId, String itemCode, String itemName,
                                 BigDecimal qty, String unit, Boolean refItem,
                                 String suitability, String fitProduct,
                                 Boolean allRoute, Boolean isPrimary, String remark,
                                 OffsetDateTime createdAt) {
    }

    public record ConsumptionReq(
            @Size(max = 64) String code,
            @Size(max = 200) String name,
            Long positionId,
            Long objectId,
            @Size(max = 64) String itemCode,
            @Size(max = 200) String itemName,
            BigDecimal qty,
            @Size(max = 32) String unit,
            Boolean refItem,
            @Size(max = 200) String suitability,
            @Size(max = 200) String fitProduct,
            Boolean allRoute,
            Boolean isPrimary,
            @Size(max = 500) String remark) {
    }
}
