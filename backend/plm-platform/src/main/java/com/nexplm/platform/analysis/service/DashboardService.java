package com.nexplm.platform.analysis.service;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.analysis.api.DashboardDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计看板（P10-1）：文档 / 零部件 / 变更 / 工艺 / 任务 / 质量 六域聚合。
 *
 * <p>口径决策（前端只展示，不重算）：</p>
 * <ul>
 *   <li><b>总量用当前快照</b>：文档与零部件只统计 {@code is_latest = true} 的版本，
 *       否则一个零件检入三次会让"数量"凭空翻三倍；</li>
 *   <li><b>已发布看 {@code released_at}</b>，不看 {@code lifecycle_state} 的具体字面量：
 *       生命周期状态名可由设计器自定义，用时间戳判"是否发布过"才不会因改状态名而失真；
 *       状态分布仍按 {@code lifecycle_state} 原样给出，由前端做常见值的中文映射、未知值原样显示；</li>
 *   <li><b>期间只作用于"新增"与趋势</b>：总量/完成率是全量快照，不按期间裁剪——
 *       否则期间内新登记的数据会把完成率永远压低，看起来像业务突然恶化；</li>
 *   <li><b>超期</b>只对有期限概念的域计算：变更（{@code target_date}）、任务（{@code plan_end}）、
 *       质量（{@code due_date}）；文档/零部件/工艺无期限，恒为 0，不臆造；</li>
 *   <li>各域"进行中/已完成"的取值见 {@code docs/02-数据模型设计.md} 的状态机说明，
 *       聚合 SQL 里逐域写死该域的状态集合，而不是用统一"非终态"猜测。</li>
 * </ul>
 *
 * <p>性能取舍：一次看板请求约 18 条短查询（每域 2 条 + 趋势 1 条），
 * 换来的是一域一方法、互不牵连；看板是低频页面，比拼一条巨型 UNION 更易维护。</p>
 */
@Service
public class DashboardService {

    /** 默认趋势窗口：含当月的近 6 个月。 */
    private static final int TREND_MONTHS = 6;
    private static final int DRILL_MAX_SIZE = 200;

    public static final String DOMAIN_DOC = "doc";
    public static final String DOMAIN_ITEM = "item";
    public static final String DOMAIN_CHANGE = "change";
    public static final String DOMAIN_CAPP = "capp";
    public static final String DOMAIN_TASK = "task";
    public static final String DOMAIN_QUALITY = "quality";

    private static final Map<String, String> DOMAIN_LABELS = new LinkedHashMap<>();

    static {
        DOMAIN_LABELS.put(DOMAIN_DOC, "文档");
        DOMAIN_LABELS.put(DOMAIN_ITEM, "零部件");
        DOMAIN_LABELS.put(DOMAIN_CHANGE, "变更");
        DOMAIN_LABELS.put(DOMAIN_CAPP, "工艺路线");
        DOMAIN_LABELS.put(DOMAIN_TASK, "项目任务");
        DOMAIN_LABELS.put(DOMAIN_QUALITY, "质量问题");
    }

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardDtos.Overview overview(Long companyId, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.withDayOfMonth(1).minusMonths(TREND_MONTHS - 1L) : from;
        if (start.isAfter(end)) {
            LocalDate swap = start;
            start = end;
            end = swap;
        }
        List<DashboardDtos.DomainCard> cards = List.of(
                docCard(companyId, start, end),
                itemCard(companyId, start, end),
                changeCard(companyId, start, end),
                cappCard(companyId, start, end),
                taskCard(companyId, start, end),
                qualityCard(companyId, start, end));
        return new DashboardDtos.Overview(start.toString(), end.toString(),
                List.copyOf(DOMAIN_LABELS.keySet()), cards, trend(companyId, start, end));
    }

    /* ============================================================ 六域指标卡 */

    private DashboardDtos.DomainCard docCard(Long companyId, LocalDate start, LocalDate end) {
        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE o.released_at IS NOT NULL) AS done_cnt,
                               COUNT(1) FILTER (WHERE o.created_at::date >= ?
                                                  AND o.created_at::date <= ?) AS period_cnt
                          FROM plm_object o
                         WHERE o.company_id = ? AND o.object_type = 'PdmDoc'
                           AND o.is_latest = true AND o.deleted = false
                        """, start, end, companyId);
        long total = longOf(agg.get("total_cnt"));
        long done = longOf(agg.get("done_cnt"));
        return new DashboardDtos.DomainCard(DOMAIN_DOC, DOMAIN_LABELS.get(DOMAIN_DOC),
                total, total - done, done, 0L, rate(done, total),
                longOf(agg.get("period_cnt")),
                objectStateCounts(companyId, "PdmDoc"));
    }

    private DashboardDtos.DomainCard itemCard(Long companyId, LocalDate start, LocalDate end) {
        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE o.released_at IS NOT NULL) AS done_cnt,
                               COUNT(1) FILTER (WHERE o.created_at::date >= ?
                                                  AND o.created_at::date <= ?) AS period_cnt
                          FROM plm_object o
                         WHERE o.company_id = ? AND o.object_type = 'PdmItem'
                           AND o.is_latest = true AND o.deleted = false
                        """, start, end, companyId);
        long total = longOf(agg.get("total_cnt"));
        long done = longOf(agg.get("done_cnt"));
        return new DashboardDtos.DomainCard(DOMAIN_ITEM, DOMAIN_LABELS.get(DOMAIN_ITEM),
                total, total - done, done, 0L, rate(done, total),
                longOf(agg.get("period_cnt")),
                objectStateCounts(companyId, "PdmItem"));
    }

    private DashboardDtos.DomainCard changeCard(Long companyId, LocalDate start, LocalDate end) {
        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE status NOT IN ('CLOSED', 'REJECTED', 'CANCELLED'))
                                   AS active_cnt,
                               COUNT(1) FILTER (WHERE status = 'CLOSED') AS done_cnt,
                               COUNT(1) FILTER (WHERE status NOT IN ('CLOSED', 'REJECTED', 'CANCELLED')
                                                  AND target_date IS NOT NULL
                                                  AND target_date < CURRENT_DATE) AS overdue_cnt,
                               COUNT(1) FILTER (WHERE created_at::date >= ?
                                                  AND created_at::date <= ?) AS period_cnt
                          FROM chg_ecr
                         WHERE company_id = ? AND deleted = false
                        """, start, end, companyId);
        long total = longOf(agg.get("total_cnt"));
        long done = longOf(agg.get("done_cnt"));
        return new DashboardDtos.DomainCard(DOMAIN_CHANGE, DOMAIN_LABELS.get(DOMAIN_CHANGE),
                total, longOf(agg.get("active_cnt")), done, longOf(agg.get("overdue_cnt")),
                rate(done, total), longOf(agg.get("period_cnt")),
                countBy("SELECT status AS k, COUNT(1) AS c FROM chg_ecr"
                        + " WHERE company_id = ? AND deleted = false"
                        + " GROUP BY status ORDER BY c DESC", companyId));
    }

    private DashboardDtos.DomainCard cappCard(Long companyId, LocalDate start, LocalDate end) {
        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE status = 'DRAFT') AS active_cnt,
                               COUNT(1) FILTER (WHERE status = 'RELEASED') AS done_cnt,
                               COUNT(1) FILTER (WHERE created_at::date >= ?
                                                  AND created_at::date <= ?) AS period_cnt
                          FROM capp_routing
                         WHERE company_id = ? AND deleted = false
                        """, start, end, companyId);
        long total = longOf(agg.get("total_cnt"));
        long done = longOf(agg.get("done_cnt"));
        return new DashboardDtos.DomainCard(DOMAIN_CAPP, DOMAIN_LABELS.get(DOMAIN_CAPP),
                total, longOf(agg.get("active_cnt")), done, 0L, rate(done, total),
                longOf(agg.get("period_cnt")),
                countBy("SELECT status AS k, COUNT(1) AS c FROM capp_routing"
                        + " WHERE company_id = ? AND deleted = false"
                        + " GROUP BY status ORDER BY c DESC", companyId));
    }

    private DashboardDtos.DomainCard taskCard(Long companyId, LocalDate start, LocalDate end) {
        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE status IN ('NOT_STARTED', 'IN_PROGRESS', 'BLOCKED'))
                                   AS active_cnt,
                               COUNT(1) FILTER (WHERE status = 'DONE') AS done_cnt,
                               COUNT(1) FILTER (WHERE status <> 'DONE' AND plan_end IS NOT NULL
                                                  AND plan_end < CURRENT_DATE) AS overdue_cnt,
                               COUNT(1) FILTER (WHERE created_at::date >= ?
                                                  AND created_at::date <= ?) AS period_cnt
                          FROM prj_task
                         WHERE company_id = ? AND deleted = false
                        """, start, end, companyId);
        long total = longOf(agg.get("total_cnt"));
        long done = longOf(agg.get("done_cnt"));
        return new DashboardDtos.DomainCard(DOMAIN_TASK, DOMAIN_LABELS.get(DOMAIN_TASK),
                total, longOf(agg.get("active_cnt")), done, longOf(agg.get("overdue_cnt")),
                rate(done, total), longOf(agg.get("period_cnt")),
                countBy("SELECT status AS k, COUNT(1) AS c FROM prj_task"
                        + " WHERE company_id = ? AND deleted = false"
                        + " GROUP BY status ORDER BY c DESC", companyId));
    }

    private DashboardDtos.DomainCard qualityCard(Long companyId, LocalDate start, LocalDate end) {
        Map<String, Object> agg = jdbcTemplate.queryForMap("""
                        SELECT COUNT(1) AS total_cnt,
                               COUNT(1) FILTER (WHERE status <> 'CLOSED') AS active_cnt,
                               COUNT(1) FILTER (WHERE status = 'CLOSED') AS done_cnt,
                               COUNT(1) FILTER (WHERE status <> 'CLOSED' AND due_date IS NOT NULL
                                                  AND due_date < CURRENT_DATE) AS overdue_cnt,
                               COUNT(1) FILTER (WHERE reported_at::date >= ?
                                                  AND reported_at::date <= ?) AS period_cnt
                          FROM qlt_issue
                         WHERE company_id = ? AND deleted = false
                        """, start, end, companyId);
        long total = longOf(agg.get("total_cnt"));
        long done = longOf(agg.get("done_cnt"));
        return new DashboardDtos.DomainCard(DOMAIN_QUALITY, DOMAIN_LABELS.get(DOMAIN_QUALITY),
                total, longOf(agg.get("active_cnt")), done, longOf(agg.get("overdue_cnt")),
                rate(done, total), longOf(agg.get("period_cnt")),
                countBy("SELECT status AS k, COUNT(1) AS c FROM qlt_issue"
                        + " WHERE company_id = ? AND deleted = false"
                        + " GROUP BY status ORDER BY c DESC", companyId));
    }

    /** 文档/零部件的状态分布：按 {@code plm_object.lifecycle_state} 原样计数。 */
    private List<DashboardDtos.CountRow> objectStateCounts(Long companyId, String objectType) {
        return jdbcTemplate.query("""
                        SELECT o.lifecycle_state AS k, COUNT(1) AS c
                          FROM plm_object o
                         WHERE o.company_id = ? AND o.object_type = ?
                           AND o.is_latest = true AND o.deleted = false
                         GROUP BY o.lifecycle_state ORDER BY c DESC
                        """,
                (rs, rowNum) -> new DashboardDtos.CountRow(rs.getString("k"), rs.getInt("c")),
                companyId, objectType);
    }

    private List<DashboardDtos.CountRow> countBy(String sql, Long companyId) {
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new DashboardDtos.CountRow(rs.getString("k"), rs.getInt("c")),
                companyId);
    }

    /* ============================================================ 月度趋势 */

    private List<DashboardDtos.TrendRow> trend(Long companyId, LocalDate start, LocalDate end) {
        // 六个域各自"新增"的时间口径：文档/零部件看对象创建，质量看登记，其余看创建；
        // 一条 UNION ALL 完成，避免 6 次往返。
        String sql = """
                SELECT 'doc' AS domain, to_char(created_at, 'YYYY-MM') AS m, COUNT(1) AS c
                  FROM plm_object
                 WHERE company_id = ? AND object_type = 'PdmDoc' AND is_latest = true AND deleted = false
                   AND created_at::date BETWEEN ? AND ? GROUP BY 2
                UNION ALL
                SELECT 'item', to_char(created_at, 'YYYY-MM'), COUNT(1)
                  FROM plm_object
                 WHERE company_id = ? AND object_type = 'PdmItem' AND is_latest = true AND deleted = false
                   AND created_at::date BETWEEN ? AND ? GROUP BY 2
                UNION ALL
                SELECT 'change', to_char(created_at, 'YYYY-MM'), COUNT(1)
                  FROM chg_ecr
                 WHERE company_id = ? AND deleted = false
                   AND created_at::date BETWEEN ? AND ? GROUP BY 2
                UNION ALL
                SELECT 'capp', to_char(created_at, 'YYYY-MM'), COUNT(1)
                  FROM capp_routing
                 WHERE company_id = ? AND deleted = false
                   AND created_at::date BETWEEN ? AND ? GROUP BY 2
                UNION ALL
                SELECT 'task', to_char(created_at, 'YYYY-MM'), COUNT(1)
                  FROM prj_task
                 WHERE company_id = ? AND deleted = false
                   AND created_at::date BETWEEN ? AND ? GROUP BY 2
                UNION ALL
                SELECT 'quality', to_char(reported_at, 'YYYY-MM'), COUNT(1)
                  FROM qlt_issue
                 WHERE company_id = ? AND deleted = false
                   AND reported_at::date BETWEEN ? AND ? GROUP BY 2
                """;
        Map<String, Map<String, Long>> byMonth = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            byMonth.computeIfAbsent(rs.getString("m"), key -> new LinkedHashMap<>())
                    .merge(rs.getString("domain"), rs.getLong("c"), Long::sum);
        }, companyId, start, end, companyId, start, end, companyId, start, end,
                companyId, start, end, companyId, start, end, companyId, start, end);

        List<DashboardDtos.TrendRow> rows = new ArrayList<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            String month = cursor.toString();
            Map<String, Long> counts = new LinkedHashMap<>();
            long total = 0;
            for (String domain : DOMAIN_LABELS.keySet()) {
                long value = byMonth.getOrDefault(month, Map.of()).getOrDefault(domain, 0L);
                counts.put(domain, value);
                total += value;
            }
            rows.add(new DashboardDtos.TrendRow(month, counts, total));
            cursor = cursor.plusMonths(1);
        }
        return rows;
    }

    /* ============================================================ 下钻明细 */

    public PageResult<DashboardDtos.DrillRow> items(Long companyId, String domain, String status,
                                                    Integer page, Integer size) {
        String normalized = domain == null ? "" : domain.trim().toLowerCase();
        if (!DOMAIN_LABELS.containsKey(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "未知的看板域：" + domain);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, DRILL_MAX_SIZE);
        String state = status == null || status.isBlank() ? null : status.trim();

        DrillQuery query = switch (normalized) {
            case DOMAIN_DOC -> new DrillQuery("""
                    SELECT o.id, o.code, o.name, o.lifecycle_state AS status,
                           u.display_name AS owner, o.updated_at AS ts
                      FROM plm_object o LEFT JOIN sys_user u ON u.id = o.owner_id
                     WHERE o.company_id = ? AND o.object_type = 'PdmDoc'
                       AND o.is_latest = true AND o.deleted = false
                    """, "o.lifecycle_state", "o.updated_at DESC");
            case DOMAIN_ITEM -> new DrillQuery("""
                    SELECT o.id, o.code, o.name, o.lifecycle_state AS status,
                           u.display_name AS owner, o.updated_at AS ts
                      FROM plm_object o LEFT JOIN sys_user u ON u.id = o.owner_id
                     WHERE o.company_id = ? AND o.object_type = 'PdmItem'
                       AND o.is_latest = true AND o.deleted = false
                    """, "o.lifecycle_state", "o.updated_at DESC");
            case DOMAIN_CHANGE -> new DrillQuery("""
                    SELECT e.id, e.code, e.title AS name, e.status,
                           u.display_name AS owner, e.created_at AS ts
                      FROM chg_ecr e LEFT JOIN sys_user u ON u.id = e.requester_id
                     WHERE e.company_id = ? AND e.deleted = false
                    """, "e.status", "e.created_at DESC");
            case DOMAIN_CAPP -> new DrillQuery("""
                    SELECT r.id, r.code, r.name, r.status,
                           CAST(NULL AS varchar) AS owner, r.updated_at AS ts
                      FROM capp_routing r
                     WHERE r.company_id = ? AND r.deleted = false
                    """, "r.status", "r.updated_at DESC");
            case DOMAIN_TASK -> new DrillQuery("""
                    SELECT t.id, t.wbs_code AS code, t.name, t.status,
                           u.display_name AS owner, t.created_at AS ts
                      FROM prj_task t LEFT JOIN sys_user u ON u.id = t.owner_id
                     WHERE t.company_id = ? AND t.deleted = false
                    """, "t.status", "t.created_at DESC");
            default -> new DrillQuery("""
                    SELECT i.id, i.code, i.title AS name, i.status,
                           u.display_name AS owner, i.reported_at AS ts
                      FROM qlt_issue i LEFT JOIN sys_user u ON u.id = i.owner_id
                     WHERE i.company_id = ? AND i.deleted = false
                    """, "i.status", "i.reported_at DESC");
        };

        String where = state == null ? "" : " AND " + query.statusColumn() + " = ?";
        Object[] countArgs = state == null ? new Object[]{companyId}
                : new Object[]{companyId, state};
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM (" + query.baseSql() + where + ") t", Long.class, countArgs);

        List<Object> args = new ArrayList<>(List.of(companyId));
        if (state != null) {
            args.add(state);
        }
        args.add(pageSize);
        args.add((long) (pageNo - 1) * pageSize);
        List<DashboardDtos.DrillRow> items = jdbcTemplate.query(
                query.baseSql() + where + " ORDER BY " + query.orderBy() + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new DashboardDtos.DrillRow(
                        rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("status"), rs.getString("owner"),
                        rs.getObject("ts", OffsetDateTime.class)),
                args.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    /** 下钻的基础 SQL 片段：各域列名不同，统一在 SELECT 里起别名。 */
    private record DrillQuery(String baseSql, String statusColumn, String orderBy) {
    }

    /* ============================================================ 工具 */

    private static int rate(long done, long total) {
        return total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
