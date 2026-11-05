package com.nexplm.platform.item.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 零部件 DTO。
 */
public final class ItemDtos {

    private ItemDtos() {
    }

    public record ItemSummary(Long id, String code, String name, String itemType, String specification,
                              String material, String unit, String sourceType, String lifecycleState,
                              Integer revision, Integer iteration, String revisionLabel,
                              OffsetDateTime updatedAt) {
    }

    public record ItemDetail(Long id, String code, String name, String itemType, String specification,
                             String material, String unit, BigDecimal weight, String sourceType,
                             String stdCode, String remark, String lifecycleState, Integer revision,
                             Integer iteration, String revisionLabel, Boolean isLatest, Long ownerId,
                             Long orgId, Integer securityLevel, OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
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
            Integer securityLevel) {
    }
}
