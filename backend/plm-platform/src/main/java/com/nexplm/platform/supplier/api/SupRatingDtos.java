package com.nexplm.platform.supplier.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** P8-5-1 供应商评级：维度可配方案 + 评级快照。 */
public final class SupRatingDtos {

    private SupRatingDtos() {
    }

    /** 等级档位：{@code min} 为该档最低总分，数组顺序即从高到低。 */
    public record GradeBand(String grade, BigDecimal min) {
    }

    public record DimRow(Long id, String code, String name, BigDecimal weight,
                         BigDecimal maxScore, int sortNo, boolean enabled) {
    }

    public record SchemeRow(Long id, String code, String name, String category,
                            String suspendGrade, String status, int dimCount,
                            BigDecimal weightSum, OffsetDateTime updatedAt) {
    }

    public record SchemeDetail(Long id, String code, String name, String category,
                               List<GradeBand> gradeRule, String suspendGrade, String status,
                               String remark, int ratingCount, List<DimRow> dims) {
    }

    /** 维度整表替换保存：{@code weight} 合计必须等于 100。 */
    public record SchemeSaveReq(@NotBlank @Size(max = 200) String name,
                                @Size(max = 16) String category,
                                @NotEmpty @Valid List<GradeBand> gradeRule,
                                @Size(max = 8) String suspendGrade,
                                @Size(max = 16) String status,
                                @Size(max = 1000) String remark,
                                @NotEmpty @Valid List<DimSaveReq> dims) {
    }

    public record DimSaveReq(@Size(max = 32) String code,
                             @NotBlank @Size(max = 64) String name,
                             @NotNull BigDecimal weight,
                             BigDecimal maxScore,
                             Boolean enabled) {
    }

    public record ItemRow(Long id, String dimCode, String dimName, BigDecimal weight,
                          BigDecimal maxScore, BigDecimal score, String comment) {
    }

    public record RatingRow(Long id, Long supplierId, String supplierCode, String supplierName,
                            Long schemeId, String schemeCode, String schemeName, String period,
                            String status, BigDecimal totalScore, String grade,
                            OffsetDateTime ratedAt, OffsetDateTime confirmedAt) {
    }

    public record RatingDetail(Long id, Long supplierId, String supplierCode, String supplierName,
                               Long schemeId, String schemeCode, String schemeName, String period,
                               String status, BigDecimal totalScore, String grade,
                               String remark, OffsetDateTime confirmedAt, List<ItemRow> items) {
    }

    /** 打分：{@code items} 必须覆盖方案全部启用维度，不能漏项。 */
    public record RatingSaveReq(@NotNull Long supplierId,
                                @NotNull Long schemeId,
                                @NotBlank @Size(max = 20) String period,
                                @Size(max = 1000) String remark,
                                @NotEmpty @Valid List<RatingItemReq> items) {
    }

    public record RatingItemReq(@NotNull Long dimId,
                                @NotNull BigDecimal score,
                                @Size(max = 500) String comment) {
    }
}
