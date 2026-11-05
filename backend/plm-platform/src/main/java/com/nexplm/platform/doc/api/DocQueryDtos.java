package com.nexplm.platform.doc.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 保存的查询条件 DTO。
 */
public final class DocQueryDtos {

    private DocQueryDtos() {
    }

    public record SavedQuery(Long id, String name, String entityCode, Long ownerId, String ownerName,
                             String conditionJson, Boolean shared, Boolean mine,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record SavedQueryReq(
            @NotBlank(message = "查询名称不能为空") @Size(max = 100) String name,
            @NotBlank(message = "查询条件不能为空") @Size(max = 4000) String conditionJson,
            Boolean shared) {
    }
}
