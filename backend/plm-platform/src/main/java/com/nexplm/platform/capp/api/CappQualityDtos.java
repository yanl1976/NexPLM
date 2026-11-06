package com.nexplm.platform.capp.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P6-8 质量规划（检验项字典 + 控制计划表）DTO。 */
public final class CappQualityDtos {

    private CappQualityDtos() {
    }

    // ---------------------------------------------------------------- 检验项字典

    public record QualityItemRow(Long id, String code, String name, String category, String spec,
                                 String method, String gauge, String unit, String remark,
                                 OffsetDateTime updatedAt) {
    }

    public record QualityItemSaveReq(@NotBlank @Size(max = 200) String name,
                                     String category,
                                     @Size(max = 200) String spec,
                                     @Size(max = 200) String method,
                                     @Size(max = 200) String gauge,
                                     @Size(max = 32) String unit,
                                     @Size(max = 500) String remark) {
    }

    // ---------------------------------------------------------------- 控制计划

    /** 控制计划行：内联特性值（item_id 仅用于从字典带出默认值）。 */
    public record PlanRow(Long id, int seqNo, Integer stepSeqNo, Integer substepSeqNo, String stepName,
                          Long itemId, String itemCode, String characteristic, String spec,
                          String method, String gauge, String sampleSize, String frequency,
                          String controlMethod, String reactionPlan, boolean critical, String remark) {
    }

    public record PlanRowSaveReq(@NotNull Integer seqNo,
                                 Integer stepSeqNo,
                                 Integer substepSeqNo,
                                 @Size(max = 100) String stepName,
                                 Long itemId,
                                 @Size(max = 200) String characteristic,
                                 @Size(max = 200) String spec,
                                 @Size(max = 200) String method,
                                 @Size(max = 200) String gauge,
                                 @Size(max = 64) String sampleSize,
                                 @Size(max = 64) String frequency,
                                 @Size(max = 200) String controlMethod,
                                 @Size(max = 200) String reactionPlan,
                                 Boolean critical,
                                 @Size(max = 500) String remark) {
    }

    public record PlanRowView(Long id, String code, String name, Long routingId, String routingCode,
                              Long objectId, String objectCode, String objectName, String status,
                              int rowCount, String remark,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record PlanDetail(Long id, String code, String name, Long routingId, String routingCode,
                             Long objectId, String objectCode, String objectName, String status,
                             String remark, List<PlanRow> rows,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record PlanSaveReq(@NotBlank @Size(max = 200) String name,
                              @NotNull Long routingId,
                              @Valid List<PlanRowSaveReq> rows,
                              @Size(max = 500) String remark) {
    }
}
