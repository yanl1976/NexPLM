package com.nexplm.platform.quality.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P8-2 8D 模板与质量看板 DTO。 */
public final class QualityTemplateDtos {

    private QualityTemplateDtos() {
    }

    public record TemplateRow(Long id, String code, String name, String templateType,
                              boolean enabled, String remark, int itemCount,
                              OffsetDateTime updatedAt) {
    }

    public record TemplateItemRow(Long id, String itemCode, String name, String actionType,
                                  String prompt, boolean required, int sortNo) {
    }

    public record TemplateDetail(Long id, String code, String name, String templateType,
                                 boolean enabled, String remark, List<TemplateItemRow> items) {
    }

    public record TemplateSaveReq(@NotBlank @Size(max = 200) String name,
                                  @Size(max = 32) String templateType,
                                  Boolean enabled,
                                  @Size(max = 500) String remark) {
    }

    /** 步骤：{@code itemCode}（D1/D2…）+ 复用的措施类型 + 预填提示语。 */
    public record TemplateItemSaveReq(@NotBlank @Size(max = 16) String itemCode,
                                      @NotBlank @Size(max = 200) String name,
                                      @Size(max = 16) String actionType,
                                      @Size(max = 2000) String prompt,
                                      Boolean required,
                                      Integer sortNo) {
    }

    public record TemplateItemsReq(@Valid List<TemplateItemSaveReq> items) {
    }

    /** 应用模板到问题：整批生成该轮措施。 */
    public record ApplyTemplateReq(@NotNull Long templateId) {
    }

    // ---------------------------------------------------------------- 看板

    public record CountRow(String key, int count) {
    }

    /** 月度趋势（{@code month} 形如 2026-11）。 */
    public record TrendRow(String month, int created, int closed) {
    }

    public record OwnerLoadRow(Long ownerId, String ownerName, int openCount, int overdueCount) {
    }

    /**
     * 质量看板。
     *
     * <p>{@code closedRate} 用全量快照算（已关闭 / 全部），不按期间——期间内新登记的问题
     * 会把关闭率永远压低，看起来像质量问题突然爆发，其实是统计口径问题。</p>
     */
    public record Dashboard(int total, int open, int closed, int closedRate, int overdue,
                            double avgCloseDays,
                            List<CountRow> byStatus, List<CountRow> bySource,
                            List<CountRow> bySeverity, List<TrendRow> trend,
                            List<OwnerLoadRow> topOwners) {
    }
}
