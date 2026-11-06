package com.nexplm.platform.notify.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.integration.support.ItgHttpSender;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 钉钉自定义机器人通道（支持加签）。
 *
 * <p>钉钉的加签规则：{@code sign = base64(HMAC-SHA256(timestamp + "\n" + secret, key=secret))}，
 * 且 timestamp/sign 放在 URL 查询串里。secret 一旦配置就必须每次带签，
 * 否则钉钉会以 "sign not match" 拒收。</p>
 */
@Component
public class DingtalkNotifySender implements NotifySender {

    private static final int MAX_CONTENT = 1800;
    private static final int TIMEOUT_MS = 8000;

    private final ItgHttpSender http;
    private final ObjectMapper jsonMapper;

    public DingtalkNotifySender(ItgHttpSender http, ObjectMapper jsonMapper) {
        this.http = http;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String type() {
        return "DINGTALK";
    }

    @Override
    public List<String> secretFields() {
        return List.of("webhook", "secret");
    }

    @Override
    public void validate(Map<String, Object> config) {
        requireWebhook(config);
    }

    @Override
    public SendResult send(Map<String, Object> config, String title, String content) {
        String webhook;
        try {
            webhook = requireWebhook(config);
        } catch (BizException ex) {
            return SendResult.fail(ex.getMessage());
        }
        String secret = config.get("secret") == null ? "" : String.valueOf(config.get("secret")).trim();
        if (!secret.isBlank()) {
            long timestamp = System.currentTimeMillis();
            String sign = sign(timestamp, secret);
            webhook = webhook + (webhook.contains("?") ? "&" : "?")
                    + "timestamp=" + timestamp + "&sign=" + sign;
        }
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("content", render(title, content));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", text);
        ItgHttpSender.SendResult result = http.send(new ItgHttpSender.EndpointRef("notify-dingtalk",
                webhook, "NONE", Map.of(), Map.of(), TIMEOUT_MS), toJson(body));
        if (!result.success()) {
            return SendResult.fail(result.httpStatus() == 0
                    ? "钉钉不可达：" + result.error()
                    : "钉钉返回 HTTP " + result.httpStatus() + "：" + trim(result.body()));
        }
        try {
            JsonNode node = jsonMapper.readTree(result.body());
            if (node.path("errcode").asInt(-1) == 0) {
                return SendResult.ok(trim(result.body()));
            }
            return SendResult.fail("钉钉受理失败：errcode=" + node.path("errcode").asInt()
                    + " errmsg=" + node.path("errmsg").asText(""));
        } catch (Exception ex) {
            return SendResult.fail("钉钉响应无法解析：" + trim(result.body()));
        }
    }

    /** 加签：base64(HMAC-SHA256(timestamp+"\n"+secret, secret))，再做 URL 编码。 */
    static String sign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(digest),
                    StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("钉钉加签失败", ex);
        }
    }

    private String requireWebhook(Map<String, Object> config) {
        Object raw = config == null ? null : config.get("webhook");
        String webhook = raw == null ? "" : String.valueOf(raw).trim();
        if (webhook.isBlank()) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "钉钉通道缺少 webhook 地址");
        }
        try {
            var uri = ItgHttpSender.parseUri(webhook);
            if (!Set.of("http", "https").contains(uri.getScheme())) {
                throw new IllegalArgumentException("scheme");
            }
        } catch (RuntimeException ex) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID,
                    "钉钉 webhook 必须是 http/https 地址：" + webhook);
        }
        return webhook;
    }

    private String render(String title, String content) {
        String merged = (title == null ? "" : title) + "\n" + (content == null ? "" : content);
        if (merged.length() <= MAX_CONTENT) {
            return merged;
        }
        return merged.substring(0, MAX_CONTENT) + "\n…（内容过长已截断）";
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
