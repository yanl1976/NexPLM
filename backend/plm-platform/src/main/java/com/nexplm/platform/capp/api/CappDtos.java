package com.nexplm.platform.capp.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** P6 工艺（CAPP）DTO。 */
public final class CappDtos {

    private CappDtos() {
    }

    /** 工序行。 */
    public record StepRow(Long id, int seqNo, String name, String opType, String workCenter,
                          Long resourceId, String resourceCode, String resourceName,
                          BigDecimal setupMinutes, BigDecimal runMinutes, String remark) {
    }

    /** 工序保存入参（保存时整表替换，id 不参与）。 */
    public record StepSaveReq(@NotNull @Positive Integer seqNo,
                              @NotBlank @Size(max = 100) String name,
                              @Size(max = 32) String opType,
                              @Size(max = 100) String workCenter,
                              Long resourceId,
                              BigDecimal setupMinutes,
                              BigDecimal runMinutes,
                              @Size(max = 500) String remark) {
    }

    /** 工艺路线保存入参：主档 + 工序（批量编辑，整表替换）。 */
    public record RoutingSaveReq(@NotBlank @Size(max = 200) String name,
                                 @NotNull Long objectId,
                                 @Size(max = 200) String applicability,
                                 @Size(max = 500) String remark,
                                 Long ownerId,
                                 @NotEmpty @Valid List<StepSaveReq> steps) {
    }

    /** 工艺路线列表行。 */
    public record RoutingRow(Long id, String code, String name, Long objectId, String objectCode,
                             String objectName, int versionNo, String status, String applicability,
                             Long ownerId, String ownerName,
                             int stepCount, BigDecimal totalMinutes, OffsetDateTime releasedAt,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /**
     * 工艺路线详情。
     *
     * <p>{@code routeString} 是「路线串」：{@code 10 下料 → 20 车削 → 30 检验}，
     * 由工序按 seq_no 升序拼装，供列表/导出直接展示。</p>
     */
    public record RoutingDetail(Long id, String code, String name, Long objectId, String objectCode,
                                String objectName, int versionNo, String status, String applicability,
                                String remark, Long ownerId, String ownerName,
                                String routeString, BigDecimal totalMinutes,
                                OffsetDateTime releasedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                List<StepRow> steps) {
    }
}
