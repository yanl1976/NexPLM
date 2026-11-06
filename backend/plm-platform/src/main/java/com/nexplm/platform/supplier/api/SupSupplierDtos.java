package com.nexplm.platform.supplier.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
/** P8-4 供应商主数据与零件-供应商关系。 */
public final class SupSupplierDtos {

    private SupSupplierDtos() {
    }

    /** 列表行：带零件关系计数与最近评级结果，列表页不必逐条查。 */
    public record SupplierRow(Long id, String code, String name, String shortName, String category,
                              String status, String contact, String phone, Long orgId,
                              String orgName, int partCount, int preferredCount, String grade,
                              BigDecimal ratingScore, OffsetDateTime ratedAt,
                              OffsetDateTime updatedAt) {
    }

    public record SupplierDetail(Long id, String code, String name, String shortName,
                                 String category, String status, String contact, String phone,
                                 String email, String address, String taxNo, String currency,
                                 String paymentTerms, Long orgId, String orgName, String remark,
                                 int partCount, int preferredCount, String grade,
                                 BigDecimal ratingScore, OffsetDateTime ratedAt,
                                 List<PartRow> parts) {
    }

    /** {@code code} 由系统取号（SUP-000001），保存接口不接受编码。 */
    public record SupplierSaveReq(@NotBlank @Size(max = 200) String name,
                                  @Size(max = 100) String shortName,
                                  @Size(max = 16) String category,
                                  @Size(max = 16) String status,
                                  @Size(max = 64) String contact,
                                  @Size(max = 64) String phone,
                                  @Size(max = 128) String email,
                                  @Size(max = 500) String address,
                                  @Size(max = 64) String taxNo,
                                  @Size(max = 8) String currency,
                                  @Size(max = 100) String paymentTerms,
                                  Long orgId,
                                  @Size(max = 1000) String remark) {
    }

    /** 零件关系行：{@code supplierXxx} 在"按零件反查"时才有值。 */
    public record PartRow(Long id, Long supplierId, String supplierCode, String supplierName,
                          String objectType, Long objectId, String itemCode, String itemName,
                          String supplyType, boolean preferred, Integer leadTimeDays,
                          BigDecimal moq, BigDecimal price, String currency, String status,
                          String remark, OffsetDateTime updatedAt) {
    }

    public record PartSaveReq(@Size(max = 64) String objectType,
                              @NotNull Long objectId,
                              @Size(max = 16) String supplyType,
                              Boolean preferred,
                              Integer leadTimeDays,
                              BigDecimal moq,
                              BigDecimal price,
                              @Size(max = 8) String currency,
                              @Size(max = 16) String status,
                              @Size(max = 1000) String remark) {
    }
}
