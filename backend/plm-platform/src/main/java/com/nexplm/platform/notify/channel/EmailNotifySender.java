package com.nexplm.platform.notify.channel;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 邮件通道（SMTP）。
 *
 * <p>配置取值顺序：<b>通道配置</b> → <b>application.yml 的 {@code nexplm.notify.email.*} 占位符</b>。
 * 两处都没填时不做"假装成功"：直接返回「SMTP 未配置」，消息落 FAILED 并可在页面上看到原因。
 * 这样部署漏配会立刻暴露，而不是通知静默消失。</p>
 *
 * <p>通道配置字段：host/port/username/password/from/fromName/ssl/starttls/to。</p>
 */
@Component
public class EmailNotifySender implements NotifySender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotifySender.class);

    private final String defaultHost;
    private final String defaultPort;
    private final String defaultUsername;
    private final String defaultPassword;
    private final String defaultFrom;
    private final String defaultFromName;
    private final boolean defaultSsl;
    private final boolean defaultStarttls;
    private final int timeoutMs;

    public EmailNotifySender(@Value("${nexplm.notify.email.host:}") String host,
                             @Value("${nexplm.notify.email.port:465}") String port,
                             @Value("${nexplm.notify.email.username:}") String username,
                             @Value("${nexplm.notify.email.password:}") String password,
                             @Value("${nexplm.notify.email.from:}") String from,
                             @Value("${nexplm.notify.email.from-name:NexPLM}") String fromName,
                             @Value("${nexplm.notify.email.ssl:true}") boolean ssl,
                             @Value("${nexplm.notify.email.starttls:false}") boolean starttls,
                             @Value("${nexplm.notify.email.timeout-ms:8000}") int timeoutMs) {
        this.defaultHost = host;
        this.defaultPort = port;
        this.defaultUsername = username;
        this.defaultPassword = password;
        this.defaultFrom = from;
        this.defaultFromName = fromName;
        this.defaultSsl = ssl;
        this.defaultStarttls = starttls;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String type() {
        return "EMAIL";
    }

    @Override
    public List<String> secretFields() {
        return List.of("password");
    }

    @Override
    public void validate(Map<String, Object> config) {
        String port = value(config, "port", defaultPort);
        if (port != null && !port.isBlank()) {
            try {
                int parsed = Integer.parseInt(port.trim());
                if (parsed < 1 || parsed > 65535) {
                    throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "SMTP 端口超出范围：" + port);
                }
            } catch (NumberFormatException ex) {
                throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "SMTP 端口必须是数字：" + port);
            }
        }
        if (recipients(config).isEmpty()) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID,
                    "邮件通道至少需要一个收件人（config.to，多个用逗号分隔）");
        }
        for (String address : recipients(config)) {
            if (!address.contains("@")) {
                throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "收件人格式不正确：" + address);
            }
        }
    }

    @Override
    public SendResult send(Map<String, Object> config, String title, String content) {
        String host = value(config, "host", defaultHost);
        if (host == null || host.isBlank()) {
            return SendResult.fail("SMTP 未配置：可在通道配置里填写 host，或设置环境变量 "
                    + "NEXPLM_SMTP_HOST / NEXPLM_SMTP_PORT / NEXPLM_SMTP_FROM（占位符已就绪）");
        }
        String from = value(config, "from", defaultFrom);
        if (from == null || from.isBlank()) {
            return SendResult.fail("SMTP 发件人未配置：请填写通道配置 from 或环境变量 NEXPLM_SMTP_FROM");
        }
        String port = value(config, "port", defaultPort);
        String username = value(config, "username", defaultUsername);
        String password = value(config, "password", defaultPassword);
        String fromName = value(config, "fromName", defaultFromName);
        boolean ssl = boolValue(config.get("ssl"), defaultSsl);
        boolean starttls = boolValue(config.get("starttls"), defaultStarttls);

        Properties props = new Properties();
        props.put("mail.smtp.host", host.trim());
        props.put("mail.smtp.port", (port == null || port.isBlank()) ? "465" : port.trim());
        props.put("mail.smtp.connectiontimeout", String.valueOf(timeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(timeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(timeoutMs));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.starttls.required", String.valueOf(starttls));
        boolean auth = username != null && !username.isBlank();
        props.put("mail.smtp.auth", String.valueOf(auth));

        Session session = auth
                ? Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username.trim(), password == null ? "" : password);
            }
        })
                : Session.getInstance(props);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(fromName == null || fromName.isBlank()
                    ? new InternetAddress(from.trim())
                    : new InternetAddress(from.trim(), fromName.trim(), "UTF-8"));
            InternetAddress[] to = recipients(config).stream()
                    .map(address -> {
                        try {
                            return new InternetAddress(address);
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("收件人格式不正确：" + address);
                        }
                    }).toArray(InternetAddress[]::new);
            message.setRecipients(Message.RecipientType.TO, to);
            message.setSubject(title == null ? "NexPLM 通知" : title, "UTF-8");
            message.setText(content, "UTF-8");
            message.setHeader("X-NexPLM-Notify", "1");
            Transport.send(message);
            return SendResult.ok("SMTP " + host + ":" + props.get("mail.smtp.port") + " 已投递给 "
                    + to.length + " 个收件人");
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.info("邮件通知发送失败：{} {}", host, ex.getMessage());
            return SendResult.fail("SMTP 发送失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            return SendResult.fail("SMTP 发送异常：" + ex.getMessage());
        }
    }

    /** 收件人：支持逗号/分号分隔字符串，或数组。 */
    private List<String> recipients(Map<String, Object> config) {
        Object raw = config.get("to");
        List<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                addAddress(result, item == null ? null : String.valueOf(item));
            }
            return result;
        }
        for (String part : String.valueOf(raw).split("[,;\\s]+")) {
            addAddress(result, part);
        }
        return result;
    }

    private void addAddress(List<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private String value(Map<String, Object> config, String key, String fallback) {
        Object raw = config == null ? null : config.get(key);
        if (raw != null && !String.valueOf(raw).isBlank()) {
            return String.valueOf(raw);
        }
        return fallback;
    }

    private boolean boolValue(Object raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        return Boolean.parseBoolean(String.valueOf(raw));
    }
}
