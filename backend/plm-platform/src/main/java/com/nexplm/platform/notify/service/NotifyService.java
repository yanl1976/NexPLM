package com.nexplm.platform.notify.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.notify.api.NotifyDtos;
import com.nexplm.platform.notify.channel.NotifySender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * P9-6 通知服务：通道配置 + 事件分发 + 发件箱（同步发一次 / 退避重试 / 死信重发）。
 *
 * <p>通道实现走 {@link NotifySender} SPI，本类只负责"发给谁、发什么、失败怎么办"，
 * 不关心是 SMTP 还是机器人 webhook。</p>
 *
 * <p>与 P9-4 Webhook 的差别：webhook 面向外部系统的原样报文，通知面向"人读的标题+正文"，
 * 因此这里存的是<b>渲染后的文案快照</b>，重发不会再取一次业务数据。</p>
 */
@Service
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    /** 文档发布（PdmDoc → Released）。 */
    public static final String EVENT_DOC_RELEASED = "doc.released";
    /** 变更生效（ECR 执行有对象成功）。 */
    public static final String EVENT_CHANGE_EFFECTIVE = "change.effective";
    /** 任务完成（prj_task → DONE）。 */
    public static final String EVENT_TASK_COMPLETED = "task.completed";
    /** 流程节点办理完成（工作流单个任务，会签每票一条）。 */
    public static final String EVENT_WF_TASK_COMPLETED = "wf.task.completed";
    /** 流程审批通过并归档（工作流实例走完全部分支）。 */
    public static final String EVENT_WF_PROCESS_COMPLETED = "wf.process.completed";
    /** 供应商数据发放（批次 E）。 */
    public static final String EVENT_SUPPLIER_ISSUANCE_SENT = "supplier.issuance.sent";
    /** 供应商登记回执（批次 E）。 */
    public static final String EVENT_SUPPLIER_RECEIPT_RECEIVED = "supplier.receipt.received";
    /** 供应商协同回复（批次 E）。 */
    public static final String EVENT_SUPPLIER_REPLY_CREATED = "supplier.reply.created";

    public static final Map<String, String> EVENT_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(EVENT_DOC_RELEASED, "文档发布");
        labels.put(EVENT_CHANGE_EFFECTIVE, "变更生效");
        labels.put(EVENT_TASK_COMPLETED, "任务完成");
        labels.put(EVENT_WF_TASK_COMPLETED, "流程节点办理");
        labels.put(EVENT_WF_PROCESS_COMPLETED, "流程审批通过");
        labels.put(EVENT_SUPPLIER_ISSUANCE_SENT, "供应商数据发放");
        labels.put(EVENT_SUPPLIER_RECEIPT_RECEIVED, "供应商回执");
        labels.put(EVENT_SUPPLIER_REPLY_CREATED, "供应商协同回复");
        EVENT_LABELS = Collections.unmodifiableMap(labels);
    }

    /** 站内通道编码（每公司一条，懒创建）。 */
    public static final String INTERNAL_CODE = "INTERNAL";

    private static final List<String> EVENT_TYPES = List.copyOf(EVENT_LABELS.keySet());
    private static final int MAX_RESPONSE = 2000;
    private static final int MAX_ERROR = 1000;
    private static final int MAX_CONTENT = 4000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;
    private final Map<String, NotifySender> senders;
    private final int retryBaseSeconds;

    public NotifyService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper,
                         List<NotifySender> senderBeans,
                         @Value("${nexplm.notify.retry.base-seconds:30}") int retryBaseSeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.senders = senderBeans.stream()
                .collect(Collectors.toMap(NotifySender::type, Function.identity()));
        this.retryBaseSeconds = retryBaseSeconds;
    }

    /** 已注册的通道类型（前端下拉/校验用）。 */
    public List<String> channelTypes() {
        return List.copyOf(senders.keySet());
    }

    /* ============================================================ 通道维护 */

    public PageResult<NotifyDtos.ChannelRow> channelPage(Long companyId, String keyword,
                                                         Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE company_id = ? AND deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_notify_channel" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<NotifyDtos.ChannelRow> items = jdbcTemplate.query("""
                        SELECT id, code, name, type, enabled, max_attempts, events::text AS events,
                               remark, updated_at
                          FROM sys_notify_channel
                        """ + where + " ORDER BY code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new NotifyDtos.ChannelRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("type"), rs.getBoolean("enabled"),
                        rs.getInt("max_attempts"), readStringList(rs.getString("events")),
                        rs.getString("remark"), rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public NotifyDtos.ChannelDetail channelDetail(Long id, Long companyId) {
        return toDetail(requireChannel(id, companyId));
    }

    @Transactional
    public NotifyDtos.ChannelDetail createChannel(NotifyDtos.ChannelSaveReq req, Long companyId,
                                                  Long userId) {
        String type = requireType(req.type());
        List<String> events = validateEvents(req.events());
        Map<String, Object> config = validateConfig(type, req.config());
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO sys_notify_channel (id, company_id, code, name, type, enabled,
                                                            events, config, max_attempts, remark,
                                                            created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?)
                            """,
                    id, companyId, req.code().trim(), req.name().trim(), type,
                    req.enabled() == null || req.enabled(), toJson(events), toJson(config),
                    clampAttempts(req.maxAttempts()), blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_DUPLICATED,
                    "通知通道编码已存在：" + req.code());
        }
        return channelDetail(id, companyId);
    }

    @Transactional
    public NotifyDtos.ChannelDetail updateChannel(Long id, NotifyDtos.ChannelSaveReq req,
                                                  Long companyId, Long userId) {
        Map<String, Object> current = requireChannel(id, companyId);
        String type = requireType(req.type());
        List<String> events = validateEvents(req.events());
        Map<String, Object> config = validateConfig(type, req.config());
        // 密钥字段留空 = 保持不变（前端拿不到原值，只能这样表达"不改"）
        Map<String, Object> oldConfig = readConfig(current.get("config_text"));
        for (String secretKey : secretFieldsOf(type)) {
            Object incoming = config.get(secretKey);
            if (incoming == null || String.valueOf(incoming).isBlank()) {
                Object saved = oldConfig.get(secretKey);
                if (saved != null && !String.valueOf(saved).isBlank()) {
                    config.put(secretKey, saved);
                } else {
                    config.remove(secretKey);
                }
            }
        }
        try {
            jdbcTemplate.update("""
                            UPDATE sys_notify_channel
                               SET code = ?, name = ?, type = ?, enabled = ?, events = CAST(? AS jsonb),
                                   config = CAST(? AS jsonb), max_attempts = ?, remark = ?,
                                   updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ? AND deleted = false
                            """,
                    req.code().trim(), req.name().trim(), type, req.enabled() == null || req.enabled(),
                    toJson(events), toJson(config), clampAttempts(req.maxAttempts()),
                    blankToNull(req.remark()), userId, id, companyId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_DUPLICATED,
                    "通知通道编码已存在：" + req.code());
        }
        return channelDetail(id, companyId);
    }

    @Transactional
    public void deleteChannel(Long id, Long companyId, Long userId) {
        requireChannel(id, companyId);
        jdbcTemplate.update("""
                UPDATE sys_notify_channel SET deleted = true, enabled = false, updated_by = ?,
                                              updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
    }

    /* ============================================================ 发送 */

    /**
     * 事件分发：给订阅了该事件的启用通道各发一条。
     *
     * @return 新建的消息条数（同一通道 + 同一事件只落一条，重复触发不会重复轰炸）
     */
    public int dispatch(String eventType, String eventKey, String title, String content,
                        Long companyId) {
        List<Map<String, Object>> channels = jdbcTemplate.queryForList("""
                SELECT id, max_attempts FROM sys_notify_channel
                 WHERE company_id = ? AND enabled = true AND deleted = false
                   AND events @> CAST(? AS jsonb)
                """, companyId, toJson(List.of(eventType)));
        int created = 0;
        for (Map<String, Object> channel : channels) {
            Long messageId = insertMessage(longOf(channel.get("id")), companyId, eventType, eventKey,
                    title, content, intOf(channel.get("max_attempts")));
            if (messageId != null) {
                created++;
                deliver(messageId);
            }
        }
        return created;
    }

    /**
     * 站内投递（批次 E）：与外部通道解耦——即使一个外部通道都没配，也要进站内收件箱
     * （对齐旧系统 {@code Communication.setting} 的 {@code DefaultAlertType=InternalMail}）。
     *
     * <p>幂等口径：同一 {@code eventKey} 只落一条消息（复用 INTERNAL 通道的
     * {@code uk(channel_id, event_key)}），同一收件人只落一条收件箱行。</p>
     *
     * @return 本次实际新增的收件箱条数（重复触发返回 0）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int dispatchToUsers(String eventType, String eventKey, String title, String content,
                               Long companyId, Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        long channelId = ensureInternalChannel(companyId);
        Long messageId = insertMessage(channelId, companyId, eventType, eventKey, title, content, 1);
        if (messageId != null) {
            // 站内没有"外部发送"动作：直接置 SENT，否则会留在 PENDING 被重试线程反复捞
            jdbcTemplate.update("""
                    UPDATE sys_notify_message
                       SET status = 'SENT', attempt = 1, started_at = now(),
                           finished_at = now(), updated_at = now()
                     WHERE id = ?
                    """, messageId);
        } else {
            List<Long> exists = jdbcTemplate.queryForList(
                    "SELECT id FROM sys_notify_message WHERE channel_id = ? AND event_key = ?",
                    Long.class, channelId, eventKey);
            if (exists.isEmpty()) {
                return 0;
            }
            messageId = exists.get(0);
        }
        int created = 0;
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            created += jdbcTemplate.update("""
                    INSERT INTO sys_notify_inbox (id, company_id, message_id, user_id)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (message_id, user_id) DO NOTHING
                    """, IdWorker.getId(), companyId, messageId, userId);
        }
        return created;
    }

    /** 站内通道懒创建：每公司一条（INTERNAL 的 events 为空，不会被外部通道分发误选）。 */
    private long ensureInternalChannel(Long companyId) {
        List<Long> ids = findInternalChannel(companyId);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO sys_notify_channel (id, company_id, code, name, type, enabled,
                                                            events, config, max_attempts)
                            VALUES (?, ?, ?, '站内消息', 'INTERNAL', true,
                                    CAST('[]' AS jsonb), CAST('{}' AS jsonb), 1)
                            """, id, companyId, INTERNAL_CODE);
            return id;
        } catch (DuplicateKeyException ex) {
            List<Long> again = findInternalChannel(companyId);
            return again.isEmpty() ? id : again.get(0);
        }
    }

    private List<Long> findInternalChannel(Long companyId) {
        return jdbcTemplate.queryForList("""
                SELECT id FROM sys_notify_channel
                 WHERE company_id = ? AND code = ? AND deleted = false
                """, Long.class, companyId, INTERNAL_CODE);
    }

    /** 手动测试发送：走真实通道，失败原因直接回给调用方。 */
    public NotifyDtos.MessageDetail testChannel(Long id, Long companyId, Long userId) {
        Map<String, Object> channel = requireChannel(id, companyId);
        long channelId = longOf(channel.get("id"));
        String eventKey = "notify.test:" + System.currentTimeMillis();
        Long messageId = insertMessage(channelId, companyId, "notify.test", eventKey,
                "【NexPLM】通知通道测试",
                "这是一条测试通知，收到即表示通道配置可用。\n操作人ID：" + userId,
                intOf(channel.get("max_attempts")));
        if (messageId == null) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "测试消息创建失败");
        }
        deliver(messageId);
        return messageDetail(messageId, companyId);
    }

    public PageResult<NotifyDtos.MessageRow> messagePage(Long companyId, Long channelId,
                                                         String status, Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE m.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (channelId != null) {
            where.append(" AND m.channel_id = ?");
            args.add(channelId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND m.status = ?");
            args.add(status.trim().toUpperCase());
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_notify_message m" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<NotifyDtos.MessageRow> items = jdbcTemplate.query("""
                        SELECT m.id, m.channel_id, c.code AS channel_code, m.event_type, m.event_key,
                               m.title, m.status, m.attempt, m.max_attempts, m.error_msg,
                               m.next_retry_at, m.created_at, m.finished_at
                          FROM sys_notify_message m
                          LEFT JOIN sys_notify_channel c ON c.id = m.channel_id
                        """ + where + " ORDER BY m.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new NotifyDtos.MessageRow(rs.getLong("id"),
                        rs.getLong("channel_id"), rs.getString("channel_code"),
                        rs.getString("event_type"), rs.getString("event_key"), rs.getString("title"),
                        rs.getString("status"), rs.getInt("attempt"), rs.getInt("max_attempts"),
                        rs.getString("error_msg"),
                        rs.getObject("next_retry_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("finished_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public NotifyDtos.MessageDetail messageDetail(Long id, Long companyId) {
        return toMessageDetail(requireMessage(id, companyId));
    }

    /** 重发：FAILED / DEAD → 归零重试次数，用原文案快照再发一次。 */
    @Transactional
    public NotifyDtos.MessageDetail resend(Long id, Long companyId) {
        Map<String, Object> message = requireMessage(id, companyId);
        String status = (String) message.get("status");
        if (!"FAILED".equals(status) && !"DEAD".equals(status)) {
            throw BizException.of(ErrorCode.NOTIFY_MESSAGE_STATE_INVALID,
                    "只有失败或死信消息可以重发（当前 " + status + "）");
        }
        jdbcTemplate.update("""
                UPDATE sys_notify_message
                   SET status = 'PENDING', attempt = 0, next_retry_at = null, error_msg = null,
                       updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        deliver(id);
        return messageDetail(id, companyId);
    }

    /** 定时重试到期消息，返回处理条数。 */
    public int retryDue(int limit) {
        List<Long> due = jdbcTemplate.queryForList("""
                SELECT id FROM sys_notify_message
                 WHERE status = 'PENDING'
                    OR (status = 'FAILED' AND next_retry_at IS NOT NULL AND next_retry_at <= now())
                 ORDER BY created_at
                 LIMIT ?
                """, Long.class, limit);
        int done = 0;
        for (Long id : due) {
            try {
                deliver(id);
                done++;
            } catch (RuntimeException ex) {
                log.warn("通知重试失败：id={} 原因={}", id, ex.getMessage());
            }
        }
        return done;
    }

    /** 回收卡在 SENDING 的消息（进程重启/线程中断）。 */
    public int reclaimStaleSending(int minutes) {
        return jdbcTemplate.update("""
                UPDATE sys_notify_message
                   SET status = CASE WHEN attempt >= max_attempts THEN 'DEAD' ELSE 'FAILED' END,
                       error_msg = '发送中断（进程重启或超时）',
                       next_retry_at = now(), updated_at = now()
                 WHERE status = 'SENDING' AND started_at < now() - make_interval(mins => ?)
                """, Math.max(1, minutes));
    }

    /* ============================================================ 内部 */

    private Long insertMessage(long channelId, Long companyId, String eventType, String eventKey,
                               String title, String content, int maxAttempts) {
        long id = IdWorker.getId();
        int inserted = jdbcTemplate.update("""
                        INSERT INTO sys_notify_message (id, company_id, channel_id, event_type,
                                                        event_key, title, content, status, attempt,
                                                        max_attempts, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?, now(), now())
                        ON CONFLICT (channel_id, event_key) DO NOTHING
                        """,
                id, companyId, channelId, eventType, eventKey,
                truncate(title, 300), truncate(content, MAX_CONTENT), Math.max(1, maxAttempts));
        return inserted > 0 ? id : null;
    }

    /** 一次投递：抢占 → 选通道实现 → 发送 → 落状态（失败退避或死信）。 */
    private void deliver(Long messageId) {
        int claimed = jdbcTemplate.update("""
                UPDATE sys_notify_message
                   SET status = 'SENDING', attempt = attempt + 1, started_at = now(),
                       next_retry_at = null, updated_at = now()
                 WHERE id = ? AND status IN ('PENDING', 'FAILED')
                """, messageId);
        if (claimed == 0) {
            return;
        }
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT m.id, m.title, m.content, m.attempt, m.max_attempts, m.event_type,
                       c.type AS channel_type, c.config::text AS config_text,
                       c.enabled AS channel_enabled, c.deleted AS channel_deleted
                  FROM sys_notify_message m
                  LEFT JOIN sys_notify_channel c ON c.id = m.channel_id
                 WHERE m.id = ?
                """, messageId);
        int attempt = intOf(row.get("attempt"));
        int maxAttempt = Math.max(1, intOf(row.get("max_attempts")));

        NotifySender.SendResult result;
        String type = (String) row.get("channel_type");
        if (type == null || Boolean.TRUE.equals(row.get("channel_deleted"))
                || !Boolean.TRUE.equals(row.get("channel_enabled"))) {
            result = NotifySender.SendResult.fail("通道不存在、已删除或已停用");
        } else {
            NotifySender sender = senders.get(type);
            result = sender == null
                    ? NotifySender.SendResult.fail("未注册的通道类型：" + type)
                    : sender.send(readConfig(row.get("config_text")),
                    (String) row.get("title"), (String) row.get("content"));
        }

        if (result.success()) {
            jdbcTemplate.update("""
                    UPDATE sys_notify_message
                       SET status = 'SENT', response = ?, error_msg = null, finished_at = now(),
                           updated_at = now()
                     WHERE id = ?
                    """, truncate(result.response(), MAX_RESPONSE), messageId);
            return;
        }
        boolean dead = attempt >= maxAttempt;
        jdbcTemplate.update("""
                        UPDATE sys_notify_message
                           SET status = ?, response = ?, error_msg = ?, finished_at = now(),
                               next_retry_at = CASE WHEN ? THEN NULL ELSE
                                   now() + make_interval(secs => ?) END,
                               updated_at = now()
                         WHERE id = ?
                        """,
                dead ? "DEAD" : "FAILED", truncate(result.response(), MAX_RESPONSE),
                truncate(result.error(), MAX_ERROR), dead, backoffSeconds(attempt), messageId);
        log.info("通知发送失败：id={} type={} attempt={}/{} status={} 原因={}", messageId, type,
                attempt, maxAttempt, dead ? "DEAD" : "FAILED", result.error());
    }

    private Map<String, Object> requireChannel(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *, events::text AS events_text, config::text AS config_text
                  FROM sys_notify_channel
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_NOT_FOUND, "通知通道不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireMessage(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.*, c.code AS channel_code FROM sys_notify_message m
                  LEFT JOIN sys_notify_channel c ON c.id = m.channel_id
                 WHERE m.id = ? AND m.company_id = ?
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.NOTIFY_MESSAGE_NOT_FOUND, "通知消息不存在：" + id);
        }
        return rows.get(0);
    }

    private NotifyDtos.ChannelDetail toDetail(Map<String, Object> row) {
        String type = (String) row.get("type");
        Map<String, Object> config = readConfig(row.get("config_text"));
        List<String> secretFields = new ArrayList<>();
        for (String key : secretFieldsOf(type)) {
            Object value = config.remove(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                secretFields.add(key);
            }
        }
        return new NotifyDtos.ChannelDetail(longOf(row.get("id")), (String) row.get("code"),
                (String) row.get("name"), type, Boolean.TRUE.equals(row.get("enabled")),
                intOf(row.get("max_attempts")), readStringList((String) row.get("events_text")),
                config, secretFields, (String) row.get("remark"), offset(row.get("created_at")),
                offset(row.get("updated_at")));
    }

    private NotifyDtos.MessageDetail toMessageDetail(Map<String, Object> row) {
        return new NotifyDtos.MessageDetail(longOf(row.get("id")), longOf(row.get("channel_id")),
                (String) row.get("channel_code"), (String) row.get("event_type"),
                (String) row.get("event_key"), (String) row.get("title"),
                (String) row.get("content"), (String) row.get("status"),
                intOf(row.get("attempt")), intOf(row.get("max_attempts")),
                (String) row.get("error_msg"), (String) row.get("response"),
                offset(row.get("next_retry_at")), offset(row.get("started_at")),
                offset(row.get("finished_at")), offset(row.get("created_at")));
    }

    private String requireType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        if (!senders.containsKey(value)) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID,
                    "不支持的通道类型：" + type + "（可选 " + String.join("/", senders.keySet()) + "）");
        }
        return value;
    }

    private List<String> validateEvents(List<String> events) {
        if (events == null || events.isEmpty()) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "至少订阅一个事件");
        }
        List<String> cleaned = new ArrayList<>();
        for (String event : events) {
            String value = event == null ? "" : event.trim();
            if (!EVENT_TYPES.contains(value)) {
                throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID,
                        "未知事件类型：" + event + "（可选 " + String.join("/", EVENT_TYPES) + "）");
            }
            if (!cleaned.contains(value)) {
                cleaned.add(value);
            }
        }
        return cleaned;
    }

    private Map<String, Object> validateConfig(String type, Map<String, Object> config) {
        Map<String, Object> value = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
        NotifySender sender = senders.get(type);
        if (sender != null) {
            sender.validate(value);
        }
        return value;
    }

    private List<String> secretFieldsOf(String type) {
        NotifySender sender = senders.get(type);
        return sender == null ? List.of() : sender.secretFields();
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Map<String, Object> readConfig(Object json) {
        if (json == null) {
            return new LinkedHashMap<>();
        }
        try {
            return jsonMapper.readValue(String.valueOf(json),
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.NOTIFY_CHANNEL_INVALID, "配置序列化失败");
        }
    }

    private int clampAttempts(Integer attempts) {
        if (attempts == null) {
            return 3;
        }
        return Math.min(Math.max(attempts, 1), 10);
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

    private static int intOf(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private static long longOf(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
