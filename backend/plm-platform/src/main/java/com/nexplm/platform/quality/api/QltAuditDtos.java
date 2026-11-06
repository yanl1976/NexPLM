package com.nexplm.platform.quality.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P8-3-2 IATF16949 体系审核：计划 + 检查表 + 不符合项（转质量问题闭环）。 */
public final class QltAuditDtos {

    private QltAuditDtos() {
    }

    /** 列表行：带检查项/不符合项计数，列表页不必逐条查。 */
    public record PlanRow(Long id, String code, String title, String auditType, String standard,
                          LocalDate plannedDate, LocalDate actualDate, Long auditorId,
                          String auditorName, String auditee, String status,
                          int itemCount, int findingCount, int openFindingCount,
                          OffsetDateTime updatedAt) {
    }

    public record ItemRow(Long id, String itemNo, String clause, String question, String result,
                          String evidence, int sortNo) {
    }

    /** 不符合项：{@code issueId/issueCode} 非空即已转质量问题，整改在 P8-1 里做。 */
    public record FindingRow(Long id, String code, Long planId, Long itemId, String itemNo,
                             String title, String description, String severity, LocalDate dueDate,
                             String status, Long issueId, String issueCode,
                             OffsetDateTime closedAt, OffsetDateTime createdAt) {
    }

    /** 检查表结果统计 + 不符合项统计（前端只展示）。 */
    public record AuditStats(int itemTotal, int okCount, int ngCount, int naCount,
                             int pendingCount, int findingTotal, int openFindingCount) {
    }

    public record PlanDetail(Long id, String code, String title, String auditType, String standard,
                             String scope, LocalDate plannedDate, LocalDate actualDate,
                             Long auditorId, String auditorName, String auditee, String status,
                             String summary, String remark, AuditStats stats,
                             List<ItemRow> items, List<FindingRow> findings) {
    }

    public record PlanSaveReq(@NotBlank @Size(max = 200) String title,
                              @Size(max = 16) String auditType,
                              @Size(max = 200) String standard,
                              @Size(max = 500) String scope,
                              LocalDate plannedDate,
                              Long auditorId,
                              @Size(max = 200) String auditee,
                              @Size(max = 2000) String summary,
                              @Size(max = 1000) String remark) {
    }

    /** 检查项：{@code result} 取 PENDING/OK/NG/NA，整表替换保存。 */
    public record ItemSaveReq(@NotBlank @Size(max = 32) String itemNo,
                              @Size(max = 64) String clause,
                              @NotBlank @Size(max = 500) String question,
                              @Size(max = 16) String result,
                              @Size(max = 1000) String evidence,
                              Integer sortNo) {
    }

    public record ItemsReq(@Valid List<ItemSaveReq> items) {
    }

    public record FindingSaveReq(@NotBlank @Size(max = 200) String title,
                                 @Size(max = 2000) String description,
                                 @Size(max = 16) String severity,
                                 LocalDate dueDate,
                                 Long itemId) {
    }
}
