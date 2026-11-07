package com.nexplm.platform.broadcast;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 批次 F-2 系统广播：发布 / 撤回 / 登录后未读弹窗 / 逐人已读。
 *
 * <p>与旧系统（{@code BROADCAST} 通用对象 + {@code BROADCASTLINK} 关联表 + 两条菜单）相比，
 * 这里用专用表并把「预约 / 查询」合成一页，判定规则集中在 {@code pending} 的 WHERE 里：
 * 已发布 + 已到生效时间 + 未过期 + 当前用户未读。</p>
 */
@Service
public class BroadcastService {

    private static final List<String> LEVELS = List.of("NORMAL", "IMPORTANT", "URGENT");
    private static final int MAX_TITLE = 200;
    private static final int MAX_CONTENT = 2000;

    private final JdbcTemplate jdbcTemplate;

    public BroadcastService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<BroadcastDtos.BroadcastRow> page(Long companyId, String keyword, String state,
                                                       Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        StringBuilder where = new StringBuilder(" WHERE b.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        String kw = blankToNull(keyword);
        if (kw != null) {
            where.append(" AND (b.title ILIKE ? OR b.content ILIKE ?)");
            args.add("%" + kw + "%");
            args.add("%" + kw + "%");
        }
        String st = blankToNull(state);
        if (st != null) {
            where.append(" AND b.publish_state = ?");
            args.add(st.toUpperCase());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_broadcast b" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<BroadcastDtos.BroadcastRow> items = jdbcTemplate.query("""
                        SELECT b.id, b.title, b.content, b.level, b.publish_state, b.publish_at,
                               b.expire_at, b.popup, COALESCE(u.display_name, u.username) AS creator,
                               b.created_at, b.updated_at,
                               (SELECT COUNT(1) FROM sys_broadcast_read r WHERE r.broadcast_id = b.id)
                                   AS read_count
                          FROM sys_broadcast b
                          LEFT JOIN sys_user u ON u.id = b.created_by
                        """ + where + " ORDER BY b.created_at DESC, b.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new BroadcastDtos.BroadcastRow(rs.getLong("id"),
                        rs.getString("title"), rs.getString("content"), rs.getString("level"),
                        rs.getString("publish_state"),
                        rs.getObject("publish_at", OffsetDateTime.class),
                        rs.getObject("expire_at", OffsetDateTime.class),
                        rs.getBoolean("popup"), rs.getString("creator"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class),
                        rs.getLong("read_count")),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0L : total, pageNo, pageSize);
    }

    @Transactional
    public Long create(Long companyId, Long userId, BroadcastDtos.SaveReq req) {
        String title = requireText(req.title(), "标题", MAX_TITLE);
        String content = requireText(req.content(), "广播内容", MAX_CONTENT);
        String level = normalizeLevel(req.level());
        Long id = jdbcTemplate.queryForObject("""
                        INSERT INTO sys_broadcast (company_id, title, content, level, publish_state,
                                                   publish_at, expire_at, popup, created_by)
                        VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?)
                        RETURNING id
                        """,
                Long.class, companyId, title, content, level, toTimestamp(req.publishAt()),
                toTimestamp(req.expireAt()), req.popup() == null || req.popup(), userId);
        return id;
    }

    @Transactional
    public void update(Long id, Long companyId, BroadcastDtos.SaveReq req) {
        BroadcastDtos.BroadcastRow row = require(id, companyId);
        if ("PUBLISHED".equals(row.publishState())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "已发布的广播不能直接改内容，请先撤回再编辑（避免已读的人看到的内容被悄悄替换）");
        }
        jdbcTemplate.update("""
                        UPDATE sys_broadcast
                           SET title = ?, content = ?, level = ?, publish_at = ?, expire_at = ?,
                               popup = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                requireText(req.title(), "标题", MAX_TITLE),
                requireText(req.content(), "广播内容", MAX_CONTENT),
                normalizeLevel(req.level()),
                toTimestamp(req.publishAt()),
                toTimestamp(req.expireAt()),
                req.popup() == null || req.popup(),
                id, companyId);
    }

    /** 发布：{@code publish_at} 为空或早于当前时间则立即生效；未来的就是"预约广播"。 */
    @Transactional
    public void publish(Long id, Long companyId) {
        BroadcastDtos.BroadcastRow row = require(id, companyId);
        if ("PUBLISHED".equals(row.publishState())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该广播已发布");
        }
        if (row.expireAt() != null && row.expireAt().isBefore(OffsetDateTime.now())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "失效时间已过，请先修改失效时间再发布");
        }
        jdbcTemplate.update("""
                UPDATE sys_broadcast
                   SET publish_state = 'PUBLISHED',
                       publish_at = COALESCE(publish_at, now()), updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /** 撤回：旧系统只能靠删除对象，这里保留记录并撤回，已读记录仍在（审计可查）。 */
    @Transactional
    public void revoke(Long id, Long companyId) {
        BroadcastDtos.BroadcastRow row = require(id, companyId);
        if (!"PUBLISHED".equals(row.publishState())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "只有已发布的广播可以撤回");
        }
        jdbcTemplate.update("""
                UPDATE sys_broadcast
                   SET publish_state = 'REVOKED', updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /** 删除：已发布的必须先撤回（否则收件人的已读记录会指向不存在的广播）。 */
    @Transactional
    public void delete(Long id, Long companyId) {
        BroadcastDtos.BroadcastRow row = require(id, companyId);
        if ("PUBLISHED".equals(row.publishState())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "已发布的广播请先撤回再删除");
        }
        jdbcTemplate.update("DELETE FROM sys_broadcast_read WHERE broadcast_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_broadcast WHERE id = ? AND company_id = ?", id, companyId);
    }

    /** 登录后弹窗：已发布 + 已到点 + 未过期 + 未读。 */
    public List<BroadcastDtos.PendingItem> pending(Long companyId, Long userId) {
        return jdbcTemplate.query("""
                        SELECT b.id, b.title, b.content, b.level, b.publish_at
                          FROM sys_broadcast b
                         WHERE b.company_id = ?
                           AND b.publish_state = 'PUBLISHED'
                           AND b.popup = true
                           AND (b.publish_at IS NULL OR b.publish_at <= now())
                           AND (b.expire_at IS NULL OR b.expire_at > now())
                           AND NOT EXISTS (SELECT 1 FROM sys_broadcast_read r
                                            WHERE r.broadcast_id = b.id AND r.user_id = ?)
                         ORDER BY b.publish_at DESC, b.id DESC
                         LIMIT 20
                        """,
                (rs, rowNum) -> new BroadcastDtos.PendingItem(rs.getLong("id"),
                        rs.getString("title"), rs.getString("content"), rs.getString("level"),
                        rs.getObject("publish_at", OffsetDateTime.class)),
                companyId, userId);
    }

    @Transactional
    public void markRead(Long broadcastId, Long companyId, Long userId) {
        require(broadcastId, companyId);
        jdbcTemplate.update("""
                INSERT INTO sys_broadcast_read (broadcast_id, user_id)
                VALUES (?, ?)
                ON CONFLICT (broadcast_id, user_id) DO NOTHING
                """, broadcastId, userId);
    }

    private BroadcastDtos.BroadcastRow require(Long id, Long companyId) {
        List<BroadcastDtos.BroadcastRow> rows = jdbcTemplate.query("""
                        SELECT b.id, b.title, b.content, b.level, b.publish_state, b.publish_at,
                               b.expire_at, b.popup, NULL::text AS creator, b.created_at, b.updated_at,
                               0 AS read_count
                          FROM sys_broadcast b
                         WHERE b.id = ? AND b.company_id = ?
                        """,
                (rs, rowNum) -> new BroadcastDtos.BroadcastRow(rs.getLong("id"),
                        rs.getString("title"), rs.getString("content"), rs.getString("level"),
                        rs.getString("publish_state"),
                        rs.getObject("publish_at", OffsetDateTime.class),
                        rs.getObject("expire_at", OffsetDateTime.class),
                        rs.getBoolean("popup"), rs.getString("creator"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class),
                        rs.getLong("read_count")),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "广播不存在：" + id);
        }
        return rows.get(0);
    }

    private static String normalizeLevel(String level) {
        String value = level == null || level.isBlank() ? "NORMAL" : level.trim().toUpperCase();
        return LEVELS.contains(value) ? value : "NORMAL";
    }

    private static String requireText(String value, String label, int max) {
        if (value == null || value.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, label + "不能为空");
        }
        String text = value.trim();
        if (text.length() > max) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    label + "长度不能超过 " + max + " 个字符");
        }
        return text;
    }

    /**
     * 解析前端日期选择器给的时间：支持 {@code 2026-11-07}、{@code 2026-11-07 09:00:00}、
     * {@code 2026-11-07T09:00:00}（按服务器时区），以及带偏移的 ISO 串。
     */
    private static Timestamp toTimestamp(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        String normalized = text.replace(' ', 'T');
        try {
            return Timestamp.from(OffsetDateTime.parse(normalized).toInstant());
        } catch (DateTimeParseException ignored) {
            // 无偏移的本地时间，继续按服务器时区解释
        }
        try {
            LocalDateTime local = normalized.length() == 10
                    ? LocalDate.parse(normalized).atStartOfDay()
                    : LocalDateTime.parse(normalized);
            return Timestamp.from(local.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "时间格式不正确：" + value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
