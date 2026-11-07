package com.nexplm.platform.audit;

import com.nexplm.common.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 批次 F-3 系统运行日志查询（只读）：登录日志 + 操作审计日志。
 *
 * <p>设计依据 docs/06 EXP-20261107-16 第 3、8 条：旧系统有两个独立日志体系
 * （业务操作日志 {@code OPERATELOG} 与访问记录），新系统的对应底座是
 * {@code sys_audit_log}（{@link AuditAspect} 写入）与 {@code sys_login_log}
 * （登录流程写入）。本批只补"查"，不新增写入逻辑。</p>
 *
 * <p>刻意不做全表模糊搜索：{@code detail} 是 varchar(4000)，全表 ILIKE 会拖垮审计页，
 * 关键词只匹配模块/动作/对象类型，长文本留给前端看单行。</p>
 */
@Service
public class SystemLogService {

    /** 哈希链体检一次最多回溯的条数（够发现近期删改，又不至于拖慢页面）。 */
    private static final int CHAIN_SCAN_LIMIT = 2000;

    private final JdbcTemplate jdbcTemplate;

    public SystemLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<SystemLogDtos.LoginLogRow> pageLoginLogs(Long companyId, String username,
                                                               String result, OffsetDateTime from,
                                                               OffsetDateTime to, Integer page,
                                                               Integer size) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        // sys_login_log 没有 company_id 列（登录可能发生在账号确定之前，失败登录只有用户名），
        // 因此按 user_id 归属公司过滤：匿名失败日志（user_id 为空）不展示给任何租户。
        if (companyId != null) {
            where.append(" AND (l.user_id IS NULL OR l.user_id IN"
                    + " (SELECT id FROM sys_user WHERE company_id = ?))");
            args.add(companyId);
        }
        appendLike(where, args, "l.username", username);
        if (result != null && !result.isBlank()) {
            where.append(" AND l.result = ?");
            args.add(result.trim().toUpperCase());
        }
        appendRange(where, args, "l.created_at", from, to);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_login_log l" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SystemLogDtos.LoginLogRow> items = jdbcTemplate.query("""
                        SELECT l.id, l.username, l.user_id, u.display_name, l.result, l.fail_reason,
                               l.ip, l.ua, l.created_at
                          FROM sys_login_log l
                          LEFT JOIN sys_user u ON u.id = l.user_id
                        """ + where + " ORDER BY l.created_at DESC, l.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new SystemLogDtos.LoginLogRow(rs.getLong("id"),
                        rs.getString("username"),
                        rs.getObject("user_id", Long.class),
                        rs.getString("display_name"),
                        rs.getString("result"),
                        rs.getString("fail_reason"),
                        rs.getString("ip"),
                        rs.getString("ua"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0L : total, pageNo, pageSize);
    }

    public PageResult<SystemLogDtos.AuditLogRow> pageAuditLogs(Long companyId, String module,
                                                               String action, String result,
                                                               String keyword, Long userId,
                                                               OffsetDateTime from, OffsetDateTime to,
                                                               Integer page, Integer size) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (companyId != null) {
            where.append(" AND (a.org_id = ? OR a.org_id IS NULL)");
            args.add(companyId);
        }
        appendExact(where, args, "a.module", module);
        appendExact(where, args, "a.action", action);
        if (result != null && !result.isBlank()) {
            where.append(" AND a.result = ?");
            args.add(result.trim().toUpperCase());
        }
        if (userId != null) {
            where.append(" AND a.user_id = ?");
            args.add(userId);
        }
        String kw = blankToNull(keyword);
        if (kw != null) {
            where.append(" AND (a.module ILIKE ? OR a.action ILIKE ? OR a.object_type ILIKE ?)");
            String like = "%" + kw + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        appendRange(where, args, "a.created_at", from, to);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_audit_log a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SystemLogDtos.AuditLogRow> items = jdbcTemplate.query("""
                        SELECT a.id, a.trace_id, a.user_id, COALESCE(u.display_name, u.username) AS actor,
                               a.module, a.action, a.object_type, a.object_id, a.result, a.detail,
                               a.ip, a.ua, a.created_at
                          FROM sys_audit_log a
                          LEFT JOIN sys_user u ON u.id = a.user_id
                        """ + where + " ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new SystemLogDtos.AuditLogRow(rs.getLong("id"),
                        rs.getString("trace_id"),
                        rs.getObject("user_id", Long.class),
                        rs.getString("actor"),
                        rs.getString("module"),
                        rs.getString("action"),
                        rs.getString("object_type"),
                        rs.getObject("object_id", Long.class),
                        rs.getString("result"),
                        rs.getString("detail"),
                        rs.getString("ip"),
                        rs.getString("ua"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0L : total, pageNo, pageSize);
    }

    /** 今日概览 + 哈希链体检。 */
    public SystemLogDtos.LogSummary summary(Long companyId) {
        OffsetDateTime dayStart = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        Timestamp start = Timestamp.from(dayStart.toInstant());
        List<Object> userArgs = new ArrayList<>();
        String userScope = "";
        if (companyId != null) {
            userScope = " AND (user_id IS NULL OR user_id IN (SELECT id FROM sys_user WHERE company_id = ?))";
            userArgs.add(companyId);
        }
        Long success = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_login_log WHERE result = 'SUCCESS' AND created_at >= ?"
                        + userScope, Long.class, concat(start, userArgs));
        Long failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_login_log WHERE result <> 'SUCCESS' AND created_at >= ?"
                        + userScope, Long.class, concat(start, userArgs));
        List<Object> auditArgs = new ArrayList<>();
        String auditScope = "";
        if (companyId != null) {
            auditScope = " AND (org_id = ? OR org_id IS NULL)";
            auditArgs.add(companyId);
        }
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_audit_log WHERE created_at >= ?" + auditScope,
                Long.class, concat(start, auditArgs));
        ChainCheck chain = checkChain();
        return new SystemLogDtos.LogSummary(
                success == null ? 0L : success,
                failed == null ? 0L : failed,
                auditCount == null ? 0L : auditCount,
                chain.enabled(), chain.checked(), chain.broken(), chain.message());
    }

    /**
     * 哈希链体检：按 id 升序回看最近的日志，逐条比对 {@code prev_hash} 是否等于上一条的 {@code hash}。
     *
     * <p>当前 {@link AuditAspect} 并不写入这两个字段（V1 只建了列），所以正常结果是
     * {@code enabled=false / message="哈希链未启用"}——如实告知，不把空值报成"校验通过"。</p>
     */
    private ChainCheck checkChain() {
        List<Object[]> rows = jdbcTemplate.query("""
                        SELECT id, prev_hash, hash
                          FROM sys_audit_log
                         WHERE hash IS NOT NULL
                         ORDER BY id DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new Object[]{rs.getLong("id"), rs.getString("prev_hash"),
                        rs.getString("hash")},
                CHAIN_SCAN_LIMIT);
        if (rows.isEmpty()) {
            return new ChainCheck(false, 0, 0, "哈希链未启用（sys_audit_log.prev_hash/hash 无写入点，属预留字段）");
        }
        int broken = 0;
        // rows 是倒序，从末尾（最早）往前比对
        for (int i = rows.size() - 1; i > 0; i--) {
            String previousHash = (String) rows.get(i)[2];
            String currentPrev = (String) rows.get(i - 1)[1];
            if (previousHash == null || !previousHash.equals(currentPrev)) {
                broken++;
            }
        }
        return new ChainCheck(true, rows.size(),
                broken, broken == 0 ? "链连续" : "存在 " + broken + " 处断链（可能有日志被删改）");
    }

    private record ChainCheck(boolean enabled, int checked, int broken, String message) {
    }

    private static Object[] concat(Object head, List<Object> tail) {
        Object[] result = new Object[tail.size() + 1];
        result[0] = head;
        for (int i = 0; i < tail.size(); i++) {
            result[i + 1] = tail.get(i);
        }
        return result;
    }

    private static void appendLike(StringBuilder where, List<Object> args, String column, String value) {
        String v = blankToNull(value);
        if (v != null) {
            where.append(" AND ").append(column).append(" ILIKE ?");
            args.add("%" + v + "%");
        }
    }

    private static void appendExact(StringBuilder where, List<Object> args, String column, String value) {
        String v = blankToNull(value);
        if (v != null) {
            where.append(" AND ").append(column).append(" = ?");
            args.add(v);
        }
    }

    private static void appendRange(StringBuilder where, List<Object> args, String column,
                                    OffsetDateTime from, OffsetDateTime to) {
        if (from != null) {
            where.append(" AND ").append(column).append(" >= ?");
            args.add(Timestamp.from(from.toInstant()));
        }
        if (to != null) {
            where.append(" AND ").append(column).append(" <= ?");
            args.add(Timestamp.from(to.toInstant()));
        }
    }

    private static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int normalizeSize(Integer size) {
        return size == null || size <= 0 ? 20 : Math.min(size, 200);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
