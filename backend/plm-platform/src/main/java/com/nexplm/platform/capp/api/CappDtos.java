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

    /**
     * 工步行（工序内部的操作细分）。
     *
     * <p>隶属于「工序号」而不是工序行 id：工序行 id 在序号重建时会变，绑定 id 会产生孤儿工步。</p>
     */
    public record SubstepRow(Long id, int seqNo, String name, String content, Long resourceId,
                             String resourceCode, String resourceName,
                             BigDecimal setupMinutes, BigDecimal runMinutes, String remark) {
    }

    /** 工步保存入参（同一工序内整表替换）。 */
    public record SubstepSaveReq(@NotNull @Positive Integer seqNo,
                                 @NotBlank @Size(max = 100) String name,
                                 @Size(max = 500) String content,
                                 Long resourceId,
                                 BigDecimal setupMinutes,
                                 BigDecimal runMinutes,
                                 @Size(max = 500) String remark) {
    }

    /** 工序行（含工步）。 */
    public record StepRow(Long id, int seqNo, String name, String opType, String workCenter,
                          Long resourceId, String resourceCode, String resourceName,
                          BigDecimal setupMinutes, BigDecimal runMinutes, String remark,
                          List<SubstepRow> substeps) {
    }

    /** 工序保存入参（保存时整表替换，id 不参与；工步随工序一并整表替换）。 */
    public record StepSaveReq(@NotNull @Positive Integer seqNo,
                              @NotBlank @Size(max = 100) String name,
                              @Size(max = 32) String opType,
                              @Size(max = 100) String workCenter,
                              Long resourceId,
                              BigDecimal setupMinutes,
                              BigDecimal runMinutes,
                              @Size(max = 500) String remark,
                              @Valid List<SubstepSaveReq> substeps) {
    }

    /** 工艺过程树节点：一道工序 + 其下的工步。 */
    public record ProcessTreeNode(int stepSeqNo, String stepName, String opType, String workCenter,
                                  Long resourceId, String resourceCode, String resourceName,
                                  BigDecimal setupMinutes, BigDecimal runMinutes,
                                  int substepCount, BigDecimal substepMinutes,
                                  List<SubstepRow> substeps) {
    }

    /**
     * 工艺路线保存入参：主档 + 工序（批量编辑，整表替换）。
     *
     * <p>{@code typical=true} 时无需 {@code objectId}（典型工艺不绑定零件）；零件路线必须给零件。</p>
     */
    public record RoutingSaveReq(@NotBlank @Size(max = 200) String name,
                                 Long objectId,
                                 @Size(max = 200) String applicability,
                                 @Size(max = 500) String remark,
                                 Long ownerId,
                                 @NotEmpty @Valid List<StepSaveReq> steps,
                                 Boolean typical) {
    }

    /** 另存为典型工艺入参：只需起个名字，其余（工序/工步/适用性）沿用源路线。 */
    public record TypicalSaveReq(@NotBlank @Size(max = 200) String name,
                                 @Size(max = 200) String applicability,
                                 @Size(max = 500) String remark) {
    }

    /** 典型工艺套用到零件入参：目标零件 + 可选的新路线名（不填沿用典型工艺名）。 */
    public record TypicalApplyReq(@NotNull Long objectId,
                                  @Size(max = 200) String name) {
    }

    /** 工艺路线列表行（{@code typical=true} 表示典型工艺，此时零件字段为空）。 */
    public record RoutingRow(Long id, String code, String name, Long objectId, String objectCode,
                             String objectName, int versionNo, String status, boolean typical,
                             String applicability,
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
                                String objectName, int versionNo, String status, boolean typical,
                                String applicability,
                                String remark, Long ownerId, String ownerName,
                                String routeString, BigDecimal totalMinutes,
                                OffsetDateTime releasedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                List<StepRow> steps) {
    }
}
