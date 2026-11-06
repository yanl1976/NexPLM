package com.nexplm.platform.capp.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;

/** P6-9 工艺下发接口预留 DTO。 */
public final class CappErpDtos {

    private CappErpDtos() {
    }

    /** 下发记录列表行。 */
    public record DispatchRow(Long id, String code, Long routingId, String routingCode, Long objectId,
                              String objectCode, String status, String payloadSha, String message,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 下发记录详情（含载荷快照）。 */
    public record DispatchDetail(Long id, String code, Long routingId, String routingCode,
                                 Long objectId, String objectCode, String status, String payloadSha,
                                 String message, Map<String, Object> payload,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 生成下发载荷的入参。 */
    public record DispatchReq(@NotNull Long routingId) {
    }

    /** 回执（P9 由 ERP 适配器回调；本阶段用于人工标记试跑结果）。 */
    public record DispatchCallbackReq(@Size(max = 16) String status,
                                      @Size(max = 500) String message) {
    }
}
