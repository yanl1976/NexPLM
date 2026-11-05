package com.nexplm.common.api;

import org.slf4j.MDC;

/**
 * 统一响应体。成功时 code=OK；失败时 code 为业务错误码（见 {@link com.nexplm.common.exception.ErrorCode}）。
 */
public record Result<T>(String code, String message, T data, String traceId) {

    private static final String OK = "OK";
    private static final String TRACE_ID_KEY = "traceId";

    public static <T> Result<T> ok(T data) {
        return new Result<>(OK, "成功", data, currentTraceId());
    }

    public static Result<Void> ok() {
        return new Result<>(OK, "成功", null, currentTraceId());
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null, currentTraceId());
    }

    private static String currentTraceId() {
        String trace = MDC.get(TRACE_ID_KEY);
        return trace == null ? "" : trace;
    }
}
