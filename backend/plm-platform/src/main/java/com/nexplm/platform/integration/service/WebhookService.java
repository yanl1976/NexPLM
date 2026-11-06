package com.nexplm.platform.integration.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.integration.api.WebhookDtos;
import com.nexplm.platform.integration.support.ItgHttpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P9-4 Webhook：事件订阅 + 签名投递 + 退避重试 + 重放。
 *
 * <p>投递语义与 P9-1 集成任务保持一致：同步发一次，失败按退避排重试，次数用尽进死信（DEAD），
 * 死信可用原报文快照重放。区别在于这里发的是<b>原始事件报文</b>（不经过字段映射），
 * 并带 HMAC-SHA256 签名，对端可独立校验来源与完整性。</p>
 *
 * <p>为什么不复用 {@code itg_job}：那个表的幂等键是 {@code (endpoint_id, biz_type, biz_key)}、
 * 报文要过映射引擎，而 webhook 的语义是"一条订阅 × 一个事件 = 一条投递"，且报文必须原样保留
 * 才能保证重放内容一致。混用会让两种幂等语义互相牵制。</p>
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    /** 文档发布（{@code PdmDoc} 生命周期迁到 {@code Released}）。 */
    public static final String EVENT_DOC_RELEASED = "doc.released";
    /** 变更生效（ECR 执行有对象成功）。 */
    public static final String EVENT_CHANGE_EFFECTIVE = "change.effective";
    /** 任务完成（{@code prj_task.status} → {@code DONE}）。 */
    public static final String EVENT_TASK_COMPLETED = "task.completed";

    /** 事件字典：顺序稳定，前端直接按此顺序展示。 */
    public static final Map<String, String> EVENT_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(EVENT_DOC_RELEASED, "文档发布");
        labels.put(EVENT_CHANGE_EFFECTIVE, "变更生效");
        labels.put(EVENT_TASK_COMPLETED, "任务完成");
        EVENT_LABELS = Collections.unmodifiableMap(labels);
    }

    private static final Set<String> EVENT_TYPES = EVENT_LABELS.keySet();
    private static final int MAX_RESPONSE = 2000;
    private static final int MAX_ERROR = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;
    private final ItgHttpSender sender;
    private final int retryBaseSeconds;

    public WebhookService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper, ItgHttpSender sender,
                          @Value("${nexplm.webhook.retry.base-seconds:30}") int retryBaseSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.sender = sender;
        this.retryBaseSeconds = retryBaseSeconds;
    }

    /* ================================================================ 订阅 */

    public PageResult<WebhookDtos.WebhookRow> webhookPage(Long companyId, String keyword,
                                                          Boolean enabled, Integer page,
                                                          Integer size) {
        StringBuilder where = new StringBuilder(" WHERE w.company_id = ? AND w.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (w.code ILIKE ? OR w.name ILIKE ? OR w.url ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (enabled != null) {
            where.append(" AND w.enabled = ?");
            args.add(enabled);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM itg_webhook w" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<WebhookDtos.WebhookRow> items = jdbcTemplate.query("""
                        SELECT w.id, w.code, w.name, w.url, w.events::text AS events, w.enabled,
                               w.max_attempts, w.timeout_ms, w.remark, w.updated_at
                          FROM itg_webhook w
                        """ + where + " ORDER BY w.code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new WebhookDtos.WebhookRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("url"), readEvents(rs.getString("events")),
                        rs.getBoolean("enabled"), rs.getInt("max_attempts"), rs.getInt("timeout_ms"),
                        rs.getString("remark"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public WebhookDtos.WebhookDetail webhookDetail(Long id, Long companyId) {
        return toDetail(requireWebhook(id, companyId));
    }

    @Transactional
    public WebhookDtos.WebhookSecret createWebhook(WebhookDtos.WebhookSaveReq req, Long companyId,
                                                   Long userId) {
        String url = validateUrl(req.url());
        List<String> events = validateEvents(req.events());
        String secret = blankToNull(req.secret()) == null ? generateSecret() : req.secret().trim();
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO itg_webhook (id, company_id, code, name, url, secret, events,
                                                     enabled, max_attempts, timeout_ms, remark,
                                                     created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)
                            """,
                    id, companyId, req.code().trim(), req.name().trim(), url, secret,
                    toJson(events), req.enabled() == null || req.enabled(),
                    clampAttempts(req.maxAttempts()), clampTimeout(req.timeoutMs()),
                    blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_DUPLICATED,
                    "Webhook 编码已存在：" + req.code());
        }
        return new WebhookDtos.WebhookSecret(webhookDetail(id, companyId), secret);
    }

    /** 修改订阅：{@code secret} 留空表示保持不变（与端点认证"不传即不改"同口径）。 */
    @Transactional
    public WebhookDtos.WebhookDetail updateWebhook(Long id, WebhookDtos.WebhookSaveReq req,
                                                   Long companyId, Long userId) {
        Map<String, Object> current = requireWebhook(id, companyId);
        String url = validateUrl(req.url());
        List<String> events = validateEvents(req.events());
        String secret = blankToNull(req.secret()) == null
                ? str(current.get("secret")) : req.secret().trim();
        try {
            jdbcTemplate.update("""
                            UPDATE itg_webhook
                               SET code = ?, name = ?, url = ?, secret = ?,
                                   events = CAST(? AS jsonb), enabled = ?, max_attempts = ?,
                                   timeout_ms = ?, remark = ?, updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ? AND deleted = false
                            """,
                    req.code().trim(), req.name().trim(), url, secret, toJson(events),
                    req.enabled() == null || req.enabled(), clampAttempts(req.maxAttempts()),
                    clampTimeout(req.timeoutMs()), blankToNull(req.remark()), userId, id, companyId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_DUPLICATED,
                    "Webhook 编码已存在：" + req.code());
        }
        return webhookDetail(id, companyId);
    }

    @Transactional
    public void deleteWebhook(Long id, Long companyId, Long userId) {
        requireWebhook(id, companyId);
        jdbcTemplate.update("""
                UPDATE itg_webhook SET deleted = true, enabled = false, updated_by = ?,
                                       updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
    }

    /** 轮换密钥：明文只返回一次，旧密钥立即失效（对端需同步替换）。 */
    @Transactional
    public WebhookDtos.WebhookSecret rotateSecret(Long id, Long companyId, Long userId) {
        requireWebhook(id, companyId);
        String secret = generateSecret();
        jdbcTemplate.update("""
                UPDATE itg_webhook SET secret = ?, updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, secret, userId, id, companyId);
        return new WebhookDtos.WebhookSecret(webhookDetail(id, companyId), secret);
    }

    /* ================================================================ 投递 */

    public PageResult<WebhookDtos.DeliveryRow> deliveryPage(Long companyId, Long webhookId,
                                                            String status, Integer page,
                                                            Integer size) {
        StringBuilder where = new StringBuilder(" WHERE d.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (webhookId != null) {
            where.append(" AND d.webhook_id = ?");
            args.add(webhookId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND d.status = ?");
            args.add(status.trim().toUpperCase());
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM itg_webhook_delivery d" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<WebhookDtos.DeliveryRow> items = jdbcTemplate.query("""
                        SELECT d.id, d.webhook_id, w.code AS webhook_code, d.event_type, d.event_key,
                               d.status, d.attempt, d.max_attempts, d.http_status, d.duration_ms,
                               d.error_msg, d.next_retry_at, d.created_at, d.finished_at
                          FROM itg_webhook_delivery d
                          LEFT JOIN itg_webhook w ON w.id = d.webhook_id
                        """ + where + " ORDER BY d.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new WebhookDtos.DeliveryRow(rs.getLong("id"),
                        rs.getLong("webhook_id"), rs.getString("webhook_code"),
                        rs.getString("event_type"), rs.getString("event_key"),
                        rs.getString("status"), rs.getInt("attempt"), rs.getInt("max_attempts"),
                        (Integer) rs.getObject("http_status"), rs.getInt("duration_ms"),
                        rs.getString("error_msg"),
                        rs.getObject("next_retry_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("finished_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public WebhookDtos.DeliveryDetail deliveryDetail(Long id, Long companyId) {
        return toDeliveryDetail(requireDelivery(id, companyId));
    }

    /** 重放：FAILED / DEAD → 归零重试次数并用原报文快照重发。 */
    @Transactional
    public WebhookDtos.DeliveryDetail replayDelivery(Long id, Long companyId, Long userId) {
        Map<String, Object> delivery = requireDelivery(id, companyId);
        String status = str(delivery.get("status"));
        if (!"FAILED".equals(status) && !"DEAD".equals(status)) {
            throw BizException.of(ErrorCode.ITG_DELIVERY_STATE_INVALID,
                    "只有失败或死信投递可以重放（当前 " + status + "）");
        }
        jdbcTemplate.update("""
                UPDATE itg_webhook_delivery
                   SET status = 'PENDING', attempt = 0, next_retry_at = null, error_msg = null,
                       updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        execute(id);
        return deliveryDetail(id, companyId);
    }

    /** 测试投递：给指定订阅发一条 {@code webhook.test}，用来验证地址/密钥是否接通。 */
    public WebhookDtos.DeliveryDetail testDelivery(Long id, Long companyId, Long userId) {
        Map<String, Object> webhook = requireWebhook(id, companyId);
        String eventKey = "webhook.test:" + System.currentTimeMillis();
        ObjectNode payload = jsonMapper.createObjectNode();
        payload.put("event", "webhook.test");
        payload.put("eventId", eventKey);
        payload.put("occurredAt", OffsetDateTime.now().toString());
        payload.put("companyId", companyId);
        payload.putObject("data")
                .put("message", "这是一条测试投递，收到即表示地址与签名可用")
                .put("operatorId", userId);
        Long deliveryId = insertDelivery(longOf(webhook.get("id")), companyId, "webhook.test",
                eventKey, payload, clampAttempts(intOf(webhook.get("max_attempts"))));
        if (deliveryId == null) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_INVALID, "测试投递创建失败");
        }
        execute(deliveryId);
        return deliveryDetail(deliveryId, companyId);
    }

    /**
     * 事件分发：按事件类型找启用中的订阅，逐条落投递并立即发送一次。
     *
     * <p>同一订阅 + 同一事件只落一行（唯一索引兜底），所以业务侧重试触发不会给对端重复通知。</p>
     *
     * @return 新建的投递条数
     */
    public int dispatch(String eventType, String eventKey, JsonNode payload, Long companyId) {
        List<Map<String, Object>> hooks = jdbcTemplate.queryForList("""
                SELECT id, max_attempts FROM itg_webhook
                 WHERE company_id = ? AND enabled = true AND deleted = false
                   AND events @> CAST(? AS jsonb)
                """, companyId, toJson(List.of(eventType)));
        int created = 0;
        for (Map<String, Object> hook : hooks) {
            Long deliveryId = insertDelivery(longOf(hook.get("id")), companyId, eventType, eventKey,
                    payload, clampAttempts(intOf(hook.get("max_attempts"))));
            if (deliveryId != null) {
                created++;
                execute(deliveryId);
            }
        }
        return created;
    }

    /** 定时重试：到期的 PENDING / FAILED 逐条重发，返回处理条数。 */
    public int retryDue(int limit) {
        List<Long> due = jdbcTemplate.queryForList("""
                SELECT id FROM itg_webhook_delivery
                 WHERE status = 'PENDING'
                    OR (status = 'FAILED' AND next_retry_at IS NOT NULL AND next_retry_at <= now())
                 ORDER BY created_at
                 LIMIT ?
                """, Long.class, limit);
        int done = 0;
        for (Long id : due) {
            try {
                execute(id);
                done++;
            } catch (RuntimeException ex) {
                log.warn("Webhook 投递重试失败：id={} 原因={}", id, ex.getMessage());
            }
        }
        return done;
    }

    /** 回收卡在 SENDING 的投递（进程重启/线程中断），按剩余次数决定回 FAILED 还是 DEAD。 */
    public int reclaimStaleSending(int minutes) {
        return jdbcTemplate.update("""
                UPDATE itg_webhook_delivery
                   SET status = CASE WHEN attempt >= max_attempts THEN 'DEAD' ELSE 'FAILED' END,
                       error_msg = '投递中断（进程重启或超时）',
                       next_retry_at = now(), updated_at = now()
                 WHERE status = 'SENDING' AND started_at < now() - make_interval(mins => ?)
                """, Math.max(1, minutes));
    }

    /* ================================================================ 内部 */

    /** 落一条投递（幂等）；已存在同事件则返回 null。 */
    private Long insertDelivery(Long webhookId, Long companyId, String eventType, String eventKey,
                                JsonNode payload, int maxAttempts) {
        long id = IdWorker.getId();
        int inserted;
        try {
            inserted = jdbcTemplate.update("""
                            INSERT INTO itg_webhook_delivery (id, company_id, webhook_id, event_type,
                                                              event_key, payload, status, attempt,
                                                              max_attempts, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING', 0, ?, now(), now())
                            ON CONFLICT (webhook_id, event_key) DO NOTHING
                            """,
                    id, companyId, webhookId, eventType, eventKey,
                    jsonMapper.writeValueAsString(payload), maxAttempts);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_INVALID, "事件报文序列化失败");
        }
        return inserted > 0 ? id : null;
    }

    /** 一次投递：抢占 → 签名 → 发送 → 落状态（失败按退避排重试或进死信）。 */
    private void execute(Long deliveryId) {
        int claimed = jdbcTemplate.update("""
                UPDATE itg_webhook_delivery
                   SET status = 'SENDING', attempt = attempt + 1, started_at = now(),
                       next_retry_at = null, updated_at = now()
                 WHERE id = ? AND status IN ('PENDING', 'FAILED')
                """, deliveryId);
        if (claimed == 0) {
            return;
        }
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT d.*, d.payload::text AS payload_text, w.code AS webhook_code, w.url,
                       w.secret, w.timeout_ms, w.enabled AS hook_enabled, w.deleted AS hook_deleted
                  FROM itg_webhook_delivery d
                  LEFT JOIN itg_webhook w ON w.id = d.webhook_id
                 WHERE d.id = ?
                """, deliveryId);
        String payload = str(row.get("payload_text"));
        int attempt = intOf(row.get("attempt"));
        int maxAttempt = Math.max(1, intOf(row.get("max_attempts")));

        ItgHttpSender.SendResult result;
        String signature = null;
        if (row.get("url") == null || Boolean.TRUE.equals(row.get("hook_deleted"))
                || !Boolean.TRUE.equals(row.get("hook_enabled"))) {
            result = new ItgHttpSender.SendResult(0, null, "订阅不存在、已删除或已停用", 0);
        } else {
            signature = hmacHex(str(row.get("secret")), payload);
            Map<String, Object> headers = new LinkedHashMap<>();
            headers.put("X-NexPLM-Event", str(row.get("event_type")));
            headers.put("X-NexPLM-Delivery", String.valueOf(deliveryId));
            headers.put("X-NexPLM-Webhook", str(row.get("webhook_code")));
            headers.put("X-NexPLM-Timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            headers.put("X-NexPLM-Signature", "sha256=" + signature);
            result = sender.send(new ItgHttpSender.EndpointRef(str(row.get("webhook_code")),
                    str(row.get("url")), "NONE", Map.of(), headers, intOf(row.get("timeout_ms"))),
                    payload);
        }

        if (result.success()) {
            jdbcTemplate.update("""
                    UPDATE itg_webhook_delivery
                       SET status = 'SENT', http_status = ?, response_body = ?, error_msg = null,
                           duration_ms = ?, signature = ?, finished_at = now(), updated_at = now()
                     WHERE id = ?
                    """, result.httpStatus(), truncate(result.body(), MAX_RESPONSE),
                    (int) result.durationMs(), signature, deliveryId);
            return;
        }
        boolean dead = attempt >= maxAttempt;
        String error = result.error() == null
                ? "对端返回 HTTP " + result.httpStatus() : result.error();
        jdbcTemplate.update("""
                        UPDATE itg_webhook_delivery
                           SET status = ?, http_status = ?, response_body = ?, error_msg = ?,
                               duration_ms = ?, signature = ?, finished_at = now(),
                               next_retry_at = CASE WHEN ? THEN NULL ELSE
                                   now() + make_interval(secs => ?) END,
                               updated_at = now()
                         WHERE id = ?
                        """,
                dead ? "DEAD" : "FAILED", result.httpStatus(),
                truncate(result.body(), MAX_RESPONSE), truncate(error, MAX_ERROR),
                (int) result.durationMs(), signature, dead, backoffSeconds(attempt), deliveryId);
        log.info("Webhook 投递失败：id={} attempt={}/{} status={} 原因={}", deliveryId, attempt,
                maxAttempt, dead ? "DEAD" : "FAILED", error);
    }

    private Map<String, Object> requireWebhook(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_webhook WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_NOT_FOUND, "Webhook 订阅不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireDelivery(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.*, d.payload::text AS payload_text, d.created_at
                  FROM itg_webhook_delivery d
                 WHERE d.id = ? AND d.company_id = ?
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_DELIVERY_NOT_FOUND, "投递记录不存在：" + id);
        }
        return rows.get(0);
    }

    private WebhookDtos.WebhookDetail toDetail(Map<String, Object> row) {
        return new WebhookDtos.WebhookDetail(longOf(row.get("id")), str(row.get("code")),
                str(row.get("name")), str(row.get("url")),
                readEvents(str(row.get("events"))), Boolean.TRUE.equals(row.get("enabled")),
                intOf(row.get("max_attempts")), intOf(row.get("timeout_ms")), str(row.get("remark")),
                offset(row.get("created_at")), offset(row.get("updated_at")));
    }

    private WebhookDtos.DeliveryDetail toDeliveryDetail(Map<String, Object> row) {
        JsonNode payload;
        try {
            payload = jsonMapper.readTree(str(row.get("payload_text")));
        } catch (JsonProcessingException ex) {
            payload = jsonMapper.createObjectNode();
        }
        return new WebhookDtos.DeliveryDetail(longOf(row.get("id")), longOf(row.get("webhook_id")),
                str(row.get("webhook_code")), str(row.get("event_type")), str(row.get("event_key")),
                str(row.get("status")), intOf(row.get("attempt")), intOf(row.get("max_attempts")),
                row.get("http_status") == null ? null : intOf(row.get("http_status")),
                intOf(row.get("duration_ms")), str(row.get("error_msg")), payload,
                str(row.get("response_body")), str(row.get("signature")),
                offset(row.get("next_retry_at")), offset(row.get("started_at")),
                offset(row.get("finished_at")), offset(row.get("created_at")));
    }

    private List<String> readEvents(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readerForListOf(String.class).readValue(json);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private List<String> validateEvents(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_INVALID, "至少要订阅一个事件");
        }
        List<String> cleaned = new ArrayList<>();
        for (String event : events) {
            String value = event == null ? "" : event.trim();
            if (!EVENT_TYPES.contains(value)) {
                throw BizException.of(ErrorCode.ITG_WEBHOOK_INVALID,
                        "未知事件类型：" + event + "（可选 " + String.join("/", EVENT_TYPES) + "）");
            }
            if (!cleaned.contains(value)) {
                cleaned.add(value);
            }
        }
        return cleaned;
    }

    private String validateUrl(String url) {
        // 复用集成发送器的校验：只允许 http/https 绝对地址，防止 file:// 读本地文件
        return ItgHttpSender.parseUri(url).toString();
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.ITG_WEBHOOK_INVALID, "JSON 序列化失败");
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String hmacHex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Webhook 签名计算失败", ex);
        }
    }

    private int clampAttempts(Integer attempts) {
        if (attempts == null) {
            return 5;
        }
        return Math.min(Math.max(attempts, 1), 10);
    }

    private int clampTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return 5000;
        }
        return Math.min(Math.max(timeoutMs, 1000), 30000);
    }

    private int backoffSeconds(int attempt) {
        long seconds = (long) retryBaseSeconds * (1L << Math.min(6, Math.max(0, attempt - 1)));
        return (int) Math.min(3600L, seconds);
    }

    private static OffsetDateTime offset(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(java.time.ZoneOffset.ofHours(8));
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static Long longOf(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
