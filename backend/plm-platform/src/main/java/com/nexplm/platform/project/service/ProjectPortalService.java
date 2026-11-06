package com.nexplm.platform.project.service;

import com.nexplm.platform.project.api.ProjectPortalDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目门户三视图（P7-8）：我的项目 / 我管理的项目 / 公司组合。
 *
 * <p>三视图不是同一份数据的三种排版，回答的是三个不同问题：</p>
 * <ul>
 *   <li><b>我的项目</b>：我要做什么——只算落在我头上的任务数（待办 = 未完成），
 *       不列全项目任务总量；</li>
 *   <li><b>我管理的项目</b>：项目快不快——用「进度 vs 时间进度」判断落后，
 *       再加逾期任务数、待审工时数这两个"卡住的活在哪"；</li>
 *   <li><b>公司组合</b>：整体盘子怎么样——状态分布、平均进度、逾期项目数，
 *       以及按落后程度排序的风险清单。</li>
 * </ul>
 *
 * <p>时间进度 = (今天 − 计划开始) / (计划结束 − 计划开始)，clamp 到 0~100；计划起止没填全就是
 * null（不猜），避免用 0 或 100 假装有结论。逾期任务 = 计划结束早于今天且未完成。</p>
 */
@Service
public class ProjectPortalService {

    private static final int RISK_LIMIT = 10;

    private final JdbcTemplate jdbcTemplate;

    public ProjectPortalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 我的项目：我是经理或成员的项目（未结项），按计划结束日升序（快的先到期）。 */
    public List<ProjectPortalDtos.PortalProjectCard> myProjects(Long companyId, Long userId) {
        return jdbcTemplate.query("""
                        SELECT p.id, p.code, p.name, p.type, p.status, p.progress,
                               p.plan_start, p.plan_end, u.display_name AS manager_name,
                               p.manager_id,
                               (SELECT COUNT(1) FROM prj_task t
                                 WHERE t.project_id = p.id AND t.deleted = false
                                   AND t.owner_id = ?) AS my_task_count,
                               (SELECT COUNT(1) FROM prj_task t
                                 WHERE t.project_id = p.id AND t.deleted = false
                                   AND t.owner_id = ?
                                   AND t.status NOT IN ('DONE')) AS my_open_task_count,
                               EXISTS (SELECT 1 FROM prj_member m
                                        WHERE m.project_id = p.id AND m.user_id = ?) AS is_member
                          FROM prj_project p
                          LEFT JOIN sys_user u ON u.id = p.manager_id
                         WHERE p.company_id = ? AND p.deleted = false AND p.status <> 'CLOSED'
                           AND (p.manager_id = ?
                                OR EXISTS (SELECT 1 FROM prj_member m
                                            WHERE m.project_id = p.id AND m.user_id = ?))
                         ORDER BY p.plan_end NULLS LAST, p.priority
                        """,
                (rs, rowNum) -> new ProjectPortalDtos.PortalProjectCard(rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("type"),
                        rs.getString("status"), rs.getInt("progress"),
                        rs.getObject("plan_start", LocalDate.class),
                        rs.getObject("plan_end", LocalDate.class),
                        rs.getString("manager_name"), rs.getInt("my_task_count"),
                        rs.getInt("my_open_task_count"),
                        rs.getLong("manager_id") == userId, rs.getBoolean("is_member")),
                userId, userId, userId, companyId, userId, userId);
    }

    /** 我管理的项目：进度偏差 + 逾期任务 + 待审工时 + 成员数（全部一次聚合，避免 N+1）。 */
    public List<ProjectPortalDtos.ManagedProjectRow> managedProjects(Long companyId, Long userId) {
        Map<Long, int[]> taskStats = aggregate("""
                SELECT t.project_id,
                       COUNT(1) AS task_count,
                       SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS done_count,
                       SUM(CASE WHEN t.plan_end < CURRENT_DATE AND t.status <> 'DONE' THEN 1 ELSE 0 END)
                           AS overdue_count
                  FROM prj_task t
                 WHERE t.deleted = false
                   AND t.project_id IN (SELECT id FROM prj_project
                                         WHERE company_id = ? AND manager_id = ? AND deleted = false)
                 GROUP BY t.project_id
                """, companyId, userId);
        Map<Long, Integer> pendingTimesheets = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT ts.project_id, COUNT(1) AS cnt
                          FROM prj_timesheet ts
                         WHERE ts.deleted = false AND ts.status = 'SUBMITTED'
                           AND ts.project_id IN (SELECT id FROM prj_project
                                                  WHERE company_id = ? AND manager_id = ?
                                                    AND deleted = false)
                         GROUP BY ts.project_id
                        """,
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        pendingTimesheets.put(rs.getLong("project_id"), rs.getInt("cnt")),
                companyId, userId);
        Map<Long, Integer> memberCounts = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT m.project_id, COUNT(1) AS cnt
                          FROM prj_member m
                         WHERE m.project_id IN (SELECT id FROM prj_project
                                                 WHERE company_id = ? AND manager_id = ? AND deleted = false)
                         GROUP BY m.project_id
                        """,
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        memberCounts.put(rs.getLong("project_id"), rs.getInt("cnt")),
                companyId, userId);

        return jdbcTemplate.query("""
                        SELECT p.id, p.code, p.name, p.status, p.progress, p.plan_start, p.plan_end,
                               u.display_name AS manager_name
                          FROM prj_project p
                          LEFT JOIN sys_user u ON u.id = p.manager_id
                         WHERE p.company_id = ? AND p.manager_id = ? AND p.deleted = false
                         ORDER BY p.plan_end NULLS LAST, p.priority
                        """,
                (rs, rowNum) -> {
                    long projectId = rs.getLong("id");
                    LocalDate start = rs.getObject("plan_start", LocalDate.class);
                    LocalDate end = rs.getObject("plan_end", LocalDate.class);
                    Integer timeProgress = timeProgress(start, end);
                    int progress = rs.getInt("progress");
                    int[] stats = taskStats.getOrDefault(projectId, new int[]{0, 0, 0});
                    return new ProjectPortalDtos.ManagedProjectRow(projectId, rs.getString("code"),
                            rs.getString("name"), rs.getString("status"), progress, start, end,
                            timeProgress,
                            timeProgress == null ? null : progress - timeProgress,
                            stats[0], stats[1], stats[2],
                            pendingTimesheets.getOrDefault(projectId, 0),
                            memberCounts.getOrDefault(projectId, 0),
                            rs.getString("manager_name"));
                },
                companyId, userId);
    }

    /** 公司组合：状态分布 + 平均进度 + 逾期项目数 + 风险清单（落后最多的前 10 个）。 */
    public ProjectPortalDtos.PortfolioOverview portfolio(Long companyId) {
        Map<Long, int[]> overdueStats = aggregate("""
                SELECT t.project_id,
                       SUM(CASE WHEN t.plan_end < CURRENT_DATE AND t.status <> 'DONE' THEN 1 ELSE 0 END)
                           AS overdue_count,
                       0 AS unused_1,
                       0 AS unused_2
                  FROM prj_task t
                 WHERE t.deleted = false
                   AND t.project_id IN (SELECT id FROM prj_project
                                         WHERE company_id = ? AND deleted = false)
                 GROUP BY t.project_id
                """, companyId);

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<Long, ProjectPortalDtos.PortfolioRiskRow> risks = new LinkedHashMap<>();
        int[] totals = new int[4]; // total, active, closed, overdue
        long[] progressSum = new long[1];
        jdbcTemplate.query("""
                        SELECT p.id, p.code, p.name, p.status, p.progress, p.plan_start, p.plan_end
                          FROM prj_project p
                         WHERE p.company_id = ? AND p.deleted = false
                        """,
                rs -> {
                    long projectId = rs.getLong("id");
                    String status = rs.getString("status");
                    int progress = rs.getInt("progress");
                    LocalDate start = rs.getObject("plan_start", LocalDate.class);
                    LocalDate end = rs.getObject("plan_end", LocalDate.class);
                    Integer timeProgress = timeProgress(start, end);
                    Integer deviation = timeProgress == null ? null : progress - timeProgress;
                    int[] stats = overdueStats.getOrDefault(projectId, new int[]{0, 0});
                    totals[0] += 1;
                    if ("CLOSED".equals(status)) {
                        totals[2] += 1;
                    } else {
                        totals[1] += 1;
                    }
                    boolean overdue = end != null && end.isBefore(LocalDate.now())
                            && !"CLOSED".equals(status);
                    if (overdue) {
                        totals[3] += 1;
                    }
                    progressSum[0] += progress;
                    statusCounts.merge(status, 1, Integer::sum);
                    if (!"CLOSED".equals(status) && (overdue
                            || (deviation != null && deviation <= 0))) {
                        risks.put(projectId, new ProjectPortalDtos.PortfolioRiskRow(projectId,
                                rs.getString("code"), rs.getString("name"), status, progress,
                                timeProgress, deviation, end, stats[0]));
                    }
                }, companyId);

        List<ProjectPortalDtos.PortfolioRiskRow> riskRows = new ArrayList<>(risks.values());
        riskRows.sort(Comparator
                .comparingInt((ProjectPortalDtos.PortfolioRiskRow row) ->
                        row.deviation() == null ? 0 : row.deviation())
                .thenComparing(Comparator.comparingInt(
                        ProjectPortalDtos.PortfolioRiskRow::overdueTaskCount).reversed()));
        if (riskRows.size() > RISK_LIMIT) {
            riskRows = riskRows.subList(0, RISK_LIMIT);
        }
        List<ProjectPortalDtos.PortfolioStatusCount> byStatus = new ArrayList<>();
        statusCounts.forEach((status, count) ->
                byStatus.add(new ProjectPortalDtos.PortfolioStatusCount(status, count)));
        int avgProgress = totals[0] == 0 ? 0 : (int) Math.round((double) progressSum[0] / totals[0]);
        return new ProjectPortalDtos.PortfolioOverview(totals[0], totals[1], totals[2], totals[3],
                avgProgress, byStatus, riskRows);
    }

    // ---------------------------------------------------------------- 内部

    /** 聚合查询统一按位置读：第 1 列恒为 project_id，后 3 列依次为三个指标。 */
    private Map<Long, int[]> aggregate(String sql, Object... args) {
        Map<Long, int[]> result = new HashMap<>();
        jdbcTemplate.query(sql,
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getLong(1),
                        new int[]{rs.getInt(2), rs.getInt(3), rs.getInt(4)}), args);
        return result;
    }

    static Integer timeProgress(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return null;
        }
        long total = ChronoUnit.DAYS.between(start, end);
        if (total <= 0) {
            return LocalDate.now().isBefore(start) ? 0 : 100;
        }
        long elapsed = ChronoUnit.DAYS.between(start, LocalDate.now());
        long percent = Math.round(elapsed * 100.0 / total);
        return (int) Math.max(0, Math.min(100, percent));
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
