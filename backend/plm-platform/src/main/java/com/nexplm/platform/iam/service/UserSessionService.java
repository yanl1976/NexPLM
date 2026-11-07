package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.SessionDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批次 F-1 在线用户与会话：登录落会话、请求校验会话、强制下线。
 *
 * <p>设计依据见 docs/06 EXP-20261107-16：旧系统在线用户是服务端内存会话表，踢人精确到
 * 「用户 + 设备类型 + 会话」，本类把这三层信息落到 {@code sys_user_session}，
 * 并把会话 id 写进 JWT 的 {@code sid}，从而让无状态令牌也能被提前吊销。</p>
 *
 * <p>性能取舍：{@link #isActive(long)} 每次请求都会调用，故用进程内缓存
 * {@code activeCache}（会话失效后只写 false，不会回涨，缓存天然安全）；
 * {@link #touch(long)} 5 分钟节流一次，与旧系统「最近操作时间」同量级的精度。</p>
 */
@Service
public class UserSessionService {

    /** 最近操作时间写库节流间隔（秒）。 */
    private static final long TOUCH_INTERVAL_SECONDS = 300;
    private static final int ACTIVE_CACHE_LIMIT = 5000;

    private final JdbcTemplate jdbcTemplate;
    private final Map<Long, Boolean> activeCache = new ConcurrentHashMap<>();
    private final Map<Long, Instant> lastTouch = new ConcurrentHashMap<>();

    public UserSessionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 登录成功后建会话，返回的 id 会写进 JWT 的 sid。 */
    @Transactional
    public long start(Long userId, String username, Long companyId, String deviceType,
                      String ip, String ua) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sys_user_session (id, company_id, user_id, username, device_type,
                                                      ip, ua)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, userId, username, normalizeDevice(deviceType),
                truncate(ip, 64), truncate(ua, 500));
        activeCache.put(id, true);
        return id;
    }

    /** 请求级校验：会话被踢/登出后返回 false，令牌立即作废。 */
    public boolean isActive(long sessionId) {
        Boolean cached = activeCache.get(sessionId);
        if (cached != null) {
            return cached;
        }
        List<Boolean> rows = jdbcTemplate.queryForList(
                "SELECT active FROM sys_user_session WHERE id = ?", Boolean.class, sessionId);
        boolean active = !rows.isEmpty() && Boolean.TRUE.equals(rows.get(0));
        if (activeCache.size() > ACTIVE_CACHE_LIMIT) {
            activeCache.clear();
        }
        activeCache.put(sessionId, active);
        return active;
    }

    /** 更新最近操作时间（节流，避免每个请求一次写库）。 */
    public void touch(long sessionId) {
        Instant now = Instant.now();
        Instant previous = lastTouch.get(sessionId);
        if (previous != null && Duration.between(previous, now).getSeconds() < TOUCH_INTERVAL_SECONDS) {
            return;
        }
        lastTouch.put(sessionId, now);
        jdbcTemplate.update(
                "UPDATE sys_user_session SET last_active_at = now() WHERE id = ? AND active = true",
                sessionId);
    }

    /** 主动登出。 */
    public void logout(long sessionId) {
        end(sessionId, null, "LOGOUT");
    }

    /** 强制下线单条会话（旧系统 RemoveOnlineUserByLoginID 的等价物）。 */
    @Transactional
    public void kick(long sessionId, Long companyId) {
        end(sessionId, companyId, "KICKED");
    }

    /** 全部下线（可按设备类型分开，旧系统 RemoveOnlineAllUser + OnlineUserType）。 */
    @Transactional
    public int kickAll(Long companyId, String deviceType) {
        StringBuilder sql = new StringBuilder(
                "SELECT id FROM sys_user_session WHERE company_id = ? AND active = true");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        String device = blankToNull(deviceType);
        if (device != null) {
            sql.append(" AND device_type = ?");
            args.add(device.toUpperCase());
        }
        List<Long> ids = jdbcTemplate.queryForList(sql.toString(), Long.class, args.toArray());
        for (Long id : ids) {
            end(id, companyId, "KICK_ALL");
        }
        return ids.size();
    }

    /** 会话过期清理：超出令牌有效期的会话置 EXPIRED，避免"在线用户"里堆积幽灵记录。 */
    @Transactional
    public int expireBefore(Instant threshold) {
        return jdbcTemplate.update("""
                UPDATE sys_user_session
                   SET active = false, ended_at = now(), end_reason = 'EXPIRED'
                 WHERE active = true AND last_active_at < ?
                """, java.sql.Timestamp.from(threshold));
    }

    public PageResult<SessionDtos.SessionRow> page(Long companyId, String keyword,
                                                   String deviceType, Boolean onlyActive,
                                                   Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        StringBuilder where = new StringBuilder(" WHERE s.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (onlyActive == null || onlyActive) {
            where.append(" AND s.active = true");
        }
        String device = blankToNull(deviceType);
        if (device != null) {
            where.append(" AND s.device_type = ?");
            args.add(device.toUpperCase());
        }
        String kw = blankToNull(keyword);
        if (kw != null) {
            where.append(" AND (s.username ILIKE ? OR u.display_name ILIKE ?)");
            String like = "%" + kw + "%";
            args.add(like);
            args.add(like);
        }
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_user_session s
                  LEFT JOIN sys_user u ON u.id = s.user_id
                """ + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SessionDtos.SessionRow> items = jdbcTemplate.query("""
                        SELECT s.id, s.user_id, s.username, u.display_name, s.device_type, s.ip, s.ua,
                               s.login_at, s.last_active_at, s.active,
                               EXTRACT(EPOCH FROM (COALESCE(s.ended_at, now()) - s.login_at))::bigint
                                   AS online_seconds
                          FROM sys_user_session s
                          LEFT JOIN sys_user u ON u.id = s.user_id
                        """ + where + " ORDER BY s.last_active_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new SessionDtos.SessionRow(rs.getLong("id"),
                        rs.getLong("user_id"), rs.getString("username"),
                        rs.getString("display_name"), rs.getString("device_type"),
                        rs.getString("ip"), rs.getString("ua"),
                        rs.getObject("login_at", OffsetDateTime.class),
                        rs.getObject("last_active_at", OffsetDateTime.class),
                        rs.getLong("online_seconds"), rs.getBoolean("active")),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0L : total, pageNo, pageSize);
    }

    private void end(long sessionId, Long companyId, String reason) {
        int updated;
        if (companyId == null) {
            updated = jdbcTemplate.update("""
                    UPDATE sys_user_session
                       SET active = false, ended_at = now(), end_reason = ?
                     WHERE id = ? AND active = true
                    """, reason, sessionId);
        } else {
            updated = jdbcTemplate.update("""
                    UPDATE sys_user_session
                       SET active = false, ended_at = now(), end_reason = ?
                     WHERE id = ? AND company_id = ? AND active = true
                    """, reason, sessionId, companyId);
        }
        if (updated == 0 && companyId != null) {
            throw BizException.of(ErrorCode.SESSION_NOT_FOUND, "会话不存在或已下线：" + sessionId);
        }
        activeCache.put(sessionId, false);
        lastTouch.remove(sessionId);
    }

    private static String normalizeDevice(String deviceType) {
        String value = deviceType == null || deviceType.isBlank()
                ? "PC" : deviceType.trim().toUpperCase();
        return switch (value) {
            case "MOBILE", "PC", "SSO" -> value;
            default -> "PC";
        };
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
}
