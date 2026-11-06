package com.nexplm.platform.notify.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.integration.support.ItgHttpSender;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 企业微信群机器人通道。
 *
 * <p>webhook URL 里带 key，本身就是凭据，故整体按密钥处理（详情不回传）。</p>
 *
 * <p>企业微信 text 消息内容上限 2048 字节，这里按 1800 字符截断并提示"已截断"，
 * 避免超长导致整条消息被拒（用户至少还能看到摘要）。</p>
 */
@Component
public class WecomNotifySender implements NotifySender {

    private static final int MAX_CONTENT = 1800;
    private static final int TIMEOUT_MS = 8000;

    private final ItgHttpSender http;
    private final ObjectMapper jsonMapper;

    public WecomNotifySender(ItgHttpSender http, ObjectMapper jsonMapper) {
        this.http = http;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String type() {
        return "WECOM";
    }

    @Override
    public List<String> secretFields() {
        return List.of("webhook");
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
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("content", render(title, content));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", text);
        ItgHttpSender.SendResult result = http.send(new ItgHttpSender.EndpointRef("notify-wecom",
                webhook, "NONE", Map.of(), Map.of(), TIMEOUT_MS), toJson(body));
        return parse(result, "企业微信");
    }

    private SendResult parse(ItgHttpSender.SendResult result, String peer) {
        if (!result.success()) {
            return SendResult.fail(result.httpStatus() == 0
                    ? peer + " 不可达：" + result.error()
                    : peer + " 返回 HTTP " + result.httpStatus() + "：" + trim(result.body()));
        }
        try {
            JsonNode node = jsonMapper.readTree(result.body());
            if (node.path("errcode").asInt(-1) == 0) {
                return SendResult.ok(trim(result.body()));
            }
            return SendResult.fail(peer + " 受理失败：errcode=" + node.path("errcode").asInt()
                    + " errmsg=" + node.path("errmsg").asText(""));
        } catch (Exception ex) {
            return SendResult.fail(peer + " 响应无法解析：" + trim(result.body()));
        }
    }

    private String requireWebhook(Map<String, Object> config) {
        Object raw = config == null ? null : config.get("webhook");
        String webhook = raw == null ? "" : String.valueOf(raw).trim();
        if (webhook.isBlank()) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "企业微信通道缺少 webhook 地址");
        }
        try {
            URI uri = ItgHttpSender.parseUri(webhook);
            if (!Set.of("http", "https").contains(uri.getScheme())) {
                throw new IllegalArgumentException("scheme");
            }
        } catch (RuntimeException ex) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID,
                    "企业微信 webhook 必须是 http/https 地址：" + webhook);
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
