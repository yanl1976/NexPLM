package com.nexplm.platform.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计链校验：按 id 升序逐行重算哈希，找出被改过的行（`docs/07` 待整改 ⑤）。
 *
 * <p>只校验 {@code hash} 非空的行：V1 基线建了 {@code prev_hash}/{@code hash} 两列但历史
 * 写入未填，链从第一条新记录开始，历史行按"未纳入链"跳过，不误报。</p>
 */
@Service
public class AuditChainService {

    private static final int MAX_SPAN = 5000;
    private static final int MAX_BROKEN_REPORT = 20;

    private final JdbcTemplate jdbcTemplate;

    public AuditChainService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 校验结果：{@code brokenIds} 最多列 20 个，{@code checked} 为实际参与校验的行数。 */
    public record VerifyResult(long checked, int brokenCount, List<Long> brokenIds, String message) {
    }

    /**
     * 校验区间 {@code [fromId, toId]}；两者为空时取最近 {@value #MAX_SPAN} 行。
     * <p>起点是 {@code fromId} 之前最近一条非空 hash：它的 hash 作为第一条待校验行的 prev。</p>
     */
    public VerifyResult verify(Long fromId, Long toId) {
        long maxId = idOrZero("SELECT COALESCE(MAX(id), 0) FROM sys_audit_log");
        long end = toId == null || toId <= 0 ? maxId : Math.min(toId, maxId);
        long start = fromId == null || fromId <= 0 ? Math.max(1, end - MAX_SPAN + 1) : fromId;
        if (end <= 0 || start > end) {
            return new VerifyResult(0, 0, List.of(), "区间内没有审计记录");
        }
        String prev = jdbcTemplate.query("""
                        SELECT hash FROM sys_audit_log
                         WHERE id < ? AND hash IS NOT NULL
                         ORDER BY id DESC
                         LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString(1) : null, start);

        List<Row> rows = jdbcTemplate.query("""
                        SELECT id, module, action, object_type, object_id, result, user_id, detail, hash
                          FROM sys_audit_log
                         WHERE id BETWEEN ? AND ? AND hash IS NOT NULL
                         ORDER BY id
                        """,
                (rs, i) -> map(rs), start, end);

        List<Long> broken = new ArrayList<>();
        String expectedPrev = prev;
        long checked = 0;
        for (Row row : rows) {
            checked++;
            String expected = AuditChain.hash(expectedPrev,
                    AuditChain.payload(row.module(), row.action(), row.objectType(), row.objectId(),
                            row.result(), row.userId(), row.detail()));
            if (!expected.equals(row.hash())) {
                if (broken.size() < MAX_BROKEN_REPORT) {
                    broken.add(row.id());
                } else {
                    broken.set(MAX_BROKEN_REPORT - 1, row.id());
                }
            }
            // 用库里存的 hash 继续：只标出真正被改的行，不把后续行连锁误报
            expectedPrev = row.hash();
        }
        String message = broken.isEmpty()
                ? "审计链一致（校验 " + checked + " 行）"
                : "发现 " + broken.size() + " 处不一致（首个 id=" + broken.get(0) + "），可能被篡改或链断裂";
        return new VerifyResult(checked, broken.size(), broken, message);
    }

    private Row map(ResultSet rs) throws SQLException {
        long objectId = rs.getLong("object_id");
        Long object = rs.wasNull() ? null : objectId;
        long userId = rs.getLong("user_id");
        Long user = rs.wasNull() ? null : userId;
        return new Row(rs.getLong("id"), rs.getString("module"), rs.getString("action"),
                rs.getString("object_type"), object, rs.getString("result"), user,
                rs.getString("detail"), rs.getString("hash"));
    }

    private long idOrZero(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private record Row(long id, String module, String action, String objectType, Long objectId,
                       String result, Long userId, String detail, String hash) {
    }
}
