package com.nexplm.platform.supplier.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P8-5-3 供应商数据发放；批次 E-1 扩展为「发放 + 多轮协同回复」。 */
public final class SupIssuanceDtos {

    private SupIssuanceDtos() {
    }

    /** 明细行：`pullState`（文件是否已被拉取）与 `replyState`（该条图纸是否被确认）是批次 E-1 新增。 */
    public record IssuanceItemRow(Long id, String objectType, Long objectId, String itemCode,
                                  String itemName, String revision, int sortNo,
                                  String pullState, String replyState) {
    }

    /** 一条协同回复（往来记录）。 */
    public record ReplyRow(Long id, String replyType, String content, LocalDate promisedDate,
                           List<Long> attachmentFileIds, List<String> attachmentNames,
                           Long repliedBy, String repliedByName, OffsetDateTime repliedAt) {
    }

    public record IssuanceRow(Long id, String code, Long supplierId, String supplierCode,
                              String supplierName, String title, String type, String status,
                              int itemCount, int replyCount, OffsetDateTime sentAt,
                              OffsetDateTime receivedAt, String receiptFileName,
                              boolean important, OffsetDateTime readAt,
                              LocalDate effectiveDate, LocalDate expireDate, String supplierGroup,
                              OffsetDateTime updatedAt) {
    }

    public record IssuanceDetail(Long id, String code, Long supplierId, String supplierCode,
                                 String supplierName, String title, String type, String status,
                                 String remark, Long receiptFileId, String receiptFileName,
                                 String receiptNote, OffsetDateTime sentAt,
                                 OffsetDateTime receivedAt, boolean important, OffsetDateTime readAt,
                                 LocalDate effectiveDate, LocalDate expireDate, String supplierGroup,
                                 List<IssuanceItemRow> items, List<ReplyRow> replies) {
    }

    public record IssuanceItemReq(@Size(max = 64) String objectType,
                                  @NotNull Long objectId) {
    }

    public record IssuanceSaveReq(@NotNull Long supplierId,
                                  @NotBlank @Size(max = 200) String title,
                                  @Size(max = 16) String type,
                                  @Size(max = 1000) String remark,
                                  @Size(max = 100) String supplierGroup,
                                  LocalDate effectiveDate,
                                  LocalDate expireDate,
                                  Boolean important,
                                  @NotEmpty @Valid List<IssuanceItemReq> items) {
    }

    /** 回执：文件先经 {@code POST /files} 上传（不绑对象），这里只登记 fileId。 */
    public record ReceiptReq(@NotNull Long fileId,
                             @Size(max = 255) String fileName,
                             @Size(max = 1000) String note) {
    }

    /**
     * 协同回复。{@code replyType}：ACK 已收阅 / CONFIRM 图纸确认 / COMMIT_DATE 承诺交期 / REJECT 无法满足；
     * {@code confirmedItemIds}/{@code rejectedItemIds} 用于把「逐条确认」落到明细行。
     */
    public record ReplyReq(@Size(max = 16) String replyType,
                           @Size(max = 2000) String content,
                           LocalDate promisedDate,
                           List<Long> attachmentFileIds,
                           List<String> attachmentNames,
                           List<Long> confirmedItemIds,
                           List<Long> rejectedItemIds) {
    }
}
