package com.nexplm.platform.supplier.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/** P8-5-3 供应商数据发放（打包导出 / 回执协同）。 */
public final class SupIssuanceDtos {

    private SupIssuanceDtos() {
    }

    public record IssuanceItemRow(Long id, String objectType, Long objectId, String itemCode,
                                  String itemName, String revision, int sortNo) {
    }

    public record IssuanceRow(Long id, String code, Long supplierId, String supplierCode,
                              String supplierName, String title, String type, String status,
                              int itemCount, OffsetDateTime sentAt, OffsetDateTime receivedAt,
                              String receiptFileName, OffsetDateTime updatedAt) {
    }

    public record IssuanceDetail(Long id, String code, Long supplierId, String supplierCode,
                                 String supplierName, String title, String type, String status,
                                 String remark, Long receiptFileId, String receiptFileName,
                                 String receiptNote, OffsetDateTime sentAt,
                                 OffsetDateTime receivedAt, List<IssuanceItemRow> items) {
    }

    public record IssuanceItemReq(@Size(max = 64) String objectType,
                                  @NotNull Long objectId) {
    }

    public record IssuanceSaveReq(@NotNull Long supplierId,
                                  @NotBlank @Size(max = 200) String title,
                                  @Size(max = 16) String type,
                                  @Size(max = 1000) String remark,
                                  @NotEmpty @Valid List<IssuanceItemReq> items) {
    }

    /** 回执：文件先经 {@code POST /files} 上传（不绑对象），这里只登记 fileId。 */
    public record ReceiptReq(@NotNull Long fileId,
                             @Size(max = 255) String fileName,
                             @Size(max = 1000) String note) {
    }
}
