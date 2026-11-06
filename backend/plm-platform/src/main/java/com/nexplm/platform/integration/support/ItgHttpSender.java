package com.nexplm.platform.integration.support;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * 集成发送器：一个任务一次 POST，失败不抛异常而是返回结果（失败要落任务与日志，不能中断批处理）。
 *
 * <p>只允许 http/https 绝对地址：配置文件里填错成 {@code file:///} 之类会让服务端读本地文件。</p>
 */
@Component
public class ItgHttpSender {

    /** 发送结果：httpStatus=0 表示没拿到响应（连接失败/超时）。 */
    public record SendResult(int httpStatus, String body, String error, long durationMs) {
        public boolean success() {
            return httpStatus >= 200 && httpStatus < 300;
        }
    }

    public record EndpointRef(String code, String baseUrl, String authType,
                              Map<String, Object> authConfig, Map<String, Object> headers,
                              int timeoutMs) {
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public SendResult send(EndpointRef endpoint, String payloadJson) {
        long started = System.nanoTime();
        HttpRequest request;
        try {
            request = buildRequest(endpoint, payloadJson);
        } catch (BizException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            return new SendResult(0, null, "请求构造失败：" + ex.getMessage(), elapsed(started));
        }
        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new SendResult(response.statusCode(), response.body(), null, elapsed(started));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new SendResult(0, null, "发送被中断", elapsed(started));
        } catch (Exception ex) {
            return new SendResult(0, null, ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    elapsed(started));
        }
    }

    private HttpRequest buildRequest(EndpointRef endpoint, String payloadJson) {
        URI uri = parseUri(endpoint.baseUrl());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(1000, endpoint.timeoutMs())))
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8));
        if (endpoint.headers() != null) {
            endpoint.headers().forEach((name, value) -> {
                if (name != null && !name.isBlank() && value != null) {
                    builder.header(name, String.valueOf(value));
                }
            });
        }
        applyAuth(builder, endpoint.authType(), endpoint.authConfig());
        return builder.build();
    }

    private void applyAuth(HttpRequest.Builder builder, String authType,
                           Map<String, Object> authConfig) {
        String type = authType == null ? "NONE" : authType.trim().toUpperCase();
        if ("NONE".equals(type)) {
            return;
        }
        Map<String, Object> config = authConfig == null ? Map.of() : authConfig;
        switch (type) {
            case "BASIC" -> {
                String raw = value(config, "username") + ":" + value(config, "password");
                builder.header("Authorization", "Basic "
                        + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
            }
            case "BEARER" -> builder.header("Authorization", "Bearer " + value(config, "token"));
            case "HEADER" -> {
                String name = value(config, "headerName");
                if (name == null || name.isBlank()) {
                    throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID,
                            "认证方式 HEADER 需要配置 headerName");
                }
                builder.header(name, value(config, "headerValue"));
            }
            default -> throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID,
                    "未知认证方式：" + authType + "（可选 NONE/BASIC/BEARER/HEADER）");
        }
    }

    /** 端点地址必须是 http/https 绝对地址。 */
    public static URI parseUri(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID, "端点地址不能为空");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl.trim());
        } catch (RuntimeException ex) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID, "端点地址不合法：" + baseUrl);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null) {
            throw BizException.of(ErrorCode.ITG_ENDPOINT_INVALID,
                    "端点地址必须是 http/https 绝对地址：" + baseUrl);
        }
        return uri;
    }

    private static String value(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long elapsed(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}
