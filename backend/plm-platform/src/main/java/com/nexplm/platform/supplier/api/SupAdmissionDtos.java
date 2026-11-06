package com.nexplm.platform.supplier.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P8-5-2 供应商准入评审。 */
public final class SupAdmissionDtos {

    private SupAdmissionDtos() {
    }

    /** 列表行：带检查项统计，列表页不必逐条展开。 */
    public record AdmissionRow(Long id, String code, Long supplierId, String supplierCode,
                               String supplierName, String type, String status, String plannedDate,
                               String actualDate, String leader, int itemCount, int passCount,
                               int failCount, OffsetDateTime submittedAt,
                               OffsetDateTime approvedAt, OffsetDateTime updatedAt) {
    }

    public record AdmissionItemRow(Long id, int seq, String checkItem, String result,
                                   String evidence, String remark) {
    }

    public record AdmissionDetail(Long id, String code, Long supplierId, String supplierCode,
                                  String supplierName, String type, String status,
                                  String plannedDate, String actualDate, String leader,
                                  String conclusion, String rejectReason, String remark,
                                  OffsetDateTime submittedAt, OffsetDateTime approvedAt,
                                  List<AdmissionItemRow> items) {
    }

    public record AdmissionItemReq(@NotBlank @Size(max = 500) String checkItem,
                                   @Size(max = 8) String result,
                                   @Size(max = 500) String evidence,
                                   @Size(max = 500) String remark) {
    }

    public record AdmissionSaveReq(@NotNull Long supplierId,
                                   @Size(max = 16) String type,
                                   @Size(max = 20) String plannedDate,
                                   @Size(max = 64) String leader,
                                   @Size(max = 1000) String remark,
                                   @NotEmpty @Valid List<AdmissionItemReq> items) {
    }

    public record ApproveReq(@Size(max = 1000) String conclusion) {
    }

    public record RejectReq(@NotBlank @Size(max = 500) String reason) {
    }
}
