package com.nexplm.platform.quality.service;

import com.nexplm.platform.quality.api.QualityTemplateDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 质量看板（P8-2）。
 *
 * <p>口径说明（前端只展示，不重算）：</p>
 * <ul>
 *   <li><b>关闭率用全量快照</b>（已关闭 / 全部），不按期间。按期间算的话，期间内新登记的问题
 *       会把关闭率永远压低，看起来像质量突然恶化，其实是统计口径问题；</li>
 *   <li><b>趋势只受期间影响</b>：按月统计「登记数」（reported_at）与「关闭数」（closed_at）；</li>
 *   <li><b>平均关闭天数</b>只对已关闭且有关闭时间的取平均，未关闭的不参与（不按今天估算）；</li>
 *   <li><b>超期</b> = 未关闭 且 期限早于今天。</li>
 * </ul>
 */
@Service
public class QualityDashboardService {

    private static final int TREND_MONTHS = 6;

    private final JdbcTemplate jdbcTemplate;

    public QualityDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public QualityTemplateDtos.Dashboard dashboard(Long companyId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1).minusMonths(TREND_MONTHS - 1L) : from;
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }

        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE status <> 'CLOSED') AS open_cnt,
                               COUNT(1) FILTER (WHERE status = 'CLOSED') AS closed_cnt,
                               COUNT(1) FILTER (WHERE status <> 'CLOSED' AND due_date IS NOT NULL
                                                  AND due_date < CURRENT_DATE) AS overdue_cnt,
                               COALESCE(AVG(EXTRACT(EPOCH FROM (closed_at - reported_at)) / 86400.0)
                                        FILTER (WHERE status = 'CLOSED' AND closed_at IS NOT NULL), 0)
                                 AS avg_days
                          FROM qlt_issue
                         WHERE company_id = ? AND deleted = false
                        """, companyId);
        int total = intOf(agg.get("total_cnt"));
        int closed = intOf(agg.get("closed_cnt"));
        int closedRate = total == 0 ? 0 : (int) Math.round(closed * 100.0 / total);
        double avgCloseDays = Math.round(doubleOf(agg.get("avg_days")) * 10) / 10.0;

        return new QualityTemplateDtos.Dashboard(total, intOf(agg.get("open_cnt")), closed,
                closedRate, intOf(agg.get("overdue_cnt")), avgCloseDays,
                countBy(companyId, "status"), countBy(companyId, "source"),
                countBy(companyId, "severity"), trend(companyId, start, end),
                topOwners(companyId));
    }

    /** 分组计数：{@code column} 只能取 status/source/severity（白名单，避免拼接外部入参）。 */
    private List<QualityTemplateDtos.CountRow> countBy(Long companyId, String column) {
        String sql = "SELECT " + column + " AS k, COUNT(1) AS c FROM qlt_issue"
                + " WHERE company_id = ? AND deleted = false GROUP BY " + column + " ORDER BY c DESC";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new QualityTemplateDtos.CountRow(rs.getString("k"), rs.getInt("c")),
                companyId);
    }

    /** 月度趋势：期间内每个月都补齐（没有数据补 0），前端不必再做空月对齐。 */
    private List<QualityTemplateDtos.TrendRow> trend(Long companyId, LocalDate start, LocalDate end) {
        // 用 ::date 比较而不是 <= 时点：时点比较会被服务端/驱动时区影响，业务日期口径更稳
        Map<String, Integer> created = monthCounts(companyId,
                "SELECT to_char(reported_at, 'YYYY-MM') AS m, COUNT(1) AS c FROM qlt_issue"
                        + " WHERE company_id = ? AND deleted = false"
                        + " AND reported_at::date >= ? AND reported_at::date <= ? GROUP BY 1",
                start, end);
        Map<String, Integer> closed = monthCounts(companyId,
                "SELECT to_char(closed_at, 'YYYY-MM') AS m, COUNT(1) AS c FROM qlt_issue"
                        + " WHERE company_id = ? AND deleted = false AND closed_at IS NOT NULL"
                        + " AND closed_at::date >= ? AND closed_at::date <= ? GROUP BY 1",
                start, end);
        List<QualityTemplateDtos.TrendRow> rows = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            String month = cursor.toString();
            rows.add(new QualityTemplateDtos.TrendRow(month,
                    created.getOrDefault(month, 0), closed.getOrDefault(month, 0)));
            cursor = cursor.plusMonths(1);
        }
        return rows;
    }

    private Map<String, Integer> monthCounts(Long companyId, String sql, LocalDate start,
                                             LocalDate end) {
        Map<String, Integer> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            result.put(rs.getString("m"), rs.getInt("c"));
        }, companyId, start, end);
        return result;
    }

    /** 责任人负荷 TOP5：只看未关闭，先按未关闭数、再按其中超期数排。 */
    private List<QualityTemplateDtos.OwnerLoadRow> topOwners(Long companyId) {
        return jdbcTemplate.query("""
                        SELECT i.owner_id, u.display_name AS owner_name,
                               COUNT(1) FILTER (WHERE i.status <> 'CLOSED') AS open_cnt,
                               COUNT(1) FILTER (WHERE i.due_date IS NOT NULL
                                                  AND i.due_date < CURRENT_DATE) AS overdue_cnt
                          FROM qlt_issue i
                          LEFT JOIN sys_user u ON u.id = i.owner_id
                         WHERE i.company_id = ? AND i.deleted = false
                           AND i.owner_id IS NOT NULL AND i.status <> 'CLOSED'
                         GROUP BY i.owner_id, u.display_name
                         ORDER BY open_cnt DESC, overdue_cnt DESC
                         LIMIT 5
                        """,
                (rs, rowNum) -> new QualityTemplateDtos.OwnerLoadRow(
                        rs.getLong("owner_id"), rs.getString("owner_name"),
                        rs.getInt("open_cnt"), rs.getInt("overdue_cnt")),
                companyId);
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static double doubleOf(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }
}
