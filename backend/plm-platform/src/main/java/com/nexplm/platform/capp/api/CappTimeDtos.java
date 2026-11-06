package com.nexplm.platform.capp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** P6-6 工时定额 DTO。 */
public final class CappTimeDtos {

    private CappTimeDtos() {
    }

    /** 定额规则行。 */
    public record TimeRuleRow(Long id, String code, String name, String opType, String workCenter,
                              BigDecimal setupBaseMinutes, BigDecimal setupFactor,
                              BigDecimal runFactor, int batchQty, int priority, boolean enabled,
                              String remark, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /**
     * 规则保存入参。
     *
     * <p>{@code opType} / {@code workCenter} 留空表示通配：两者都空即「全局默认规则」。</p>
     */
    public record TimeRuleSaveReq(@NotBlank @Size(max = 200) String name,
                                  @Size(max = 32) String opType,
                                  @Size(max = 100) String workCenter,
                                  BigDecimal setupBaseMinutes,
                                  BigDecimal setupFactor,
                                  BigDecimal runFactor,
                                  Integer batchQty,
                                  Integer priority,
                                  Boolean enabled,
                                  @Size(max = 500) String remark) {
    }

    /** 单道工序的定额明细。 */
    public record TimeStepRow(int seqNo, String name, String opType, String workCenter,
                              BigDecimal rawSetup, BigDecimal rawRun,
                              BigDecimal setupEffective, BigDecimal runEffective,
                              String ruleCode, String ruleName) {
    }

    /**
     * 试算结果。
     *
     * <p>{@code singlePieceMinutes} 只含单件工时（不含准备），{@code batchMinutes} =
     * 准备工时合计 + 单件工时合计 × 批量——两个口径分开给，避免前端各自再算一遍导致不一致。</p>
     */
    public record TimeCalcResult(Long routingId, String routingCode, String objectCode,
                                 String objectName, int batchQty,
                                 List<TimeStepRow> steps,
                                 BigDecimal totalSetupEffective, BigDecimal singlePieceMinutes,
                                 BigDecimal batchMinutes,
                                 int matchedSteps, int unmatchedSteps) {
    }
}
