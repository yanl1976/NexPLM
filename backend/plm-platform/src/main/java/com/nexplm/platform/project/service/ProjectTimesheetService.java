package com.nexplm.platform.project.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.project.api.ProjectTimesheetDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工时填报与审批（P7-5）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li><b>按天记</b>：唯一键 {@code (task_id, user_id, work_date)}，一天一任务一条；
 *       分多段干就写备注，不做「工时块」——审批列表要能被人一眼核完；</li>
 *   <li><b>只有本人能改能填</b>：项目经理能审批但不能替人改工时，改了等于篡改考勤；
 *       且<b>不能审批自己</b>的工时（自我审批是审计缺陷）；</li>
 *   <li><b>可改范围窄</b>：DRAFT/REJECTED 才能改删，编辑后状态回落 DRAFT 再提交；
 *       APPROVED 是事实记录，不再允许改；</li>
 *   <li><b>汇总口径</b>：项目实际工时只认 APPROVED，SUBMITTED 单列「待审批」，
 *       避免「已填未审」被当成已发生；</li>
 *   <li><b>负荷简化</b>：按 APPROVED 工时 / (周数 × 40h) 估算，不接节假日日历（P7-7 再做真日历）。</li>
 * </ul>
 */
@Service
public class ProjectTimesheetService {

    private static final Set<String> STATUSES =
            Set.of("DRAFT", "SUBMITTED", "APPROVED", "REJECTED");
    private static final Set<String> WORK_TYPES = Set.of("NORMAL", "OVERTIME");
    private static final BigDecimal WEEK_CAPACITY = BigDecimal.valueOf(40);
    private static final String BASE_SELECT = """
            SELECT t.*, p.code AS project_code, p.name AS project_name,
                   k.wbs_code AS task_wbs_code, k.name AS task_name,
                   u.display_name AS user_name, a.display_name AS approver_name
              FROM prj_timesheet t
              JOIN prj_project p ON p.id = t.project_id
              JOIN prj_task k ON k.id = t.task_id
              LEFT JOIN sys_user u ON u.id = t.user_id
              LEFT JOIN sys_user a ON a.id = t.approved_by
            """;

    private final JdbcTemplate jdbcTemplate;

    public ProjectTimesheetService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- 查询

    public ProjectTimesheetDtos.TimesheetPage page(Long companyId, Long projectId, Long userId,
                                                   Long taskId, String status, LocalDate from,
                                                   LocalDate to, Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE t.company_id = ? AND t.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        appendFilters(where, args, projectId, userId, taskId, status, from, to);
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM prj_timesheet t" + where, Long.class, args.toArray());
        BigDecimal totalHours = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(t.hours), 0) FROM prj_timesheet t" + where,
                BigDecimal.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<ProjectTimesheetDtos.TimesheetRow> items = jdbcTemplate.query(
                BASE_SELECT + where + " ORDER BY t.work_date DESC, t.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs), pageArgs.toArray());
        return new ProjectTimesheetDtos.TimesheetPage(items, total == null ? 0 : total,
                pageNo, pageSize, totalHours == null ? BigDecimal.ZERO : totalHours);
    }

    private void appendFilters(StringBuilder where, List<Object> args, Long projectId, Long userId,
                               Long taskId, String status, LocalDate from, LocalDate to) {
        if (projectId != null) {
            where.append(" AND t.project_id = ?");
            args.add(projectId);
        }
        if (userId != null) {
            where.append(" AND t.user_id = ?");
            args.add(userId);
        }
        if (taskId != null) {
            where.append(" AND t.task_id = ?");
            args.add(taskId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND t.status = ?");
            args.add(normalizeStatus(status));
        }
        if (from != null) {
            where.append(" AND t.work_date >= ?");
            args.add(from);
        }
        if (to != null) {
            where.append(" AND t.work_date <= ?");
            args.add(to);
        }
    }

    /** 项目工时汇总：已审批/待审批/驳回 + 按人 + 按任务 + 按周。 */
    public ProjectTimesheetDtos.TimesheetSummary summary(Long projectId, LocalDate from,
                                                         LocalDate to, Long companyId) {
        Map<String, Object> project = requireProject(projectId, companyId);
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());
        if (end.isBefore(start)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID, "结束日期不能早于开始日期");
        }
        Map<String, BigDecimal> hoursByStatus = new LinkedHashMap<>();
        Map<String, Integer> countByStatus = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT status, COUNT(1) AS cnt, COALESCE(SUM(hours), 0) AS hours
                          FROM prj_timesheet
                         WHERE company_id = ? AND project_id = ? AND deleted = false
                           AND work_date BETWEEN ? AND ?
                         GROUP BY status
                        """,
                rs -> {
                    hoursByStatus.put(rs.getString("status"), rs.getBigDecimal("hours"));
                    countByStatus.put(rs.getString("status"), rs.getInt("cnt"));
                }, companyId, projectId, start, end);

        List<ProjectTimesheetDtos.UserHoursRow> byUser = jdbcTemplate.query("""
                        SELECT t.user_id, u.display_name,
                               COALESCE(SUM(CASE WHEN t.status = 'APPROVED' THEN t.hours ELSE 0 END), 0) AS approved,
                               COALESCE(SUM(CASE WHEN t.status = 'SUBMITTED' THEN t.hours ELSE 0 END), 0) AS pending
                          FROM prj_timesheet t
                          LEFT JOIN sys_user u ON u.id = t.user_id
                         WHERE t.company_id = ? AND t.project_id = ? AND t.deleted = false
                           AND t.work_date BETWEEN ? AND ?
                         GROUP BY t.user_id, u.display_name
                         ORDER BY approved DESC, t.user_id
                        """,
                (rs, rowNum) -> new ProjectTimesheetDtos.UserHoursRow(rs.getLong("user_id"),
                        rs.getString("display_name"), rs.getBigDecimal("approved"),
                        rs.getBigDecimal("pending")),
                companyId, projectId, start, end);

        List<ProjectTimesheetDtos.TaskHoursRow> byTask = jdbcTemplate.query("""
                        SELECT t.task_id, k.wbs_code, k.name,
                               COALESCE(SUM(CASE WHEN t.status = 'APPROVED' THEN t.hours ELSE 0 END), 0) AS approved,
                               COALESCE(SUM(CASE WHEN t.status = 'SUBMITTED' THEN t.hours ELSE 0 END), 0) AS pending
                          FROM prj_timesheet t
                          JOIN prj_task k ON k.id = t.task_id
                         WHERE t.company_id = ? AND t.project_id = ? AND t.deleted = false
                           AND t.work_date BETWEEN ? AND ?
                         GROUP BY t.task_id, k.wbs_code, k.name
                         ORDER BY k.wbs_code
                        """,
                (rs, rowNum) -> new ProjectTimesheetDtos.TaskHoursRow(rs.getLong("task_id"),
                        rs.getString("wbs_code"), rs.getString("name"),
                        rs.getBigDecimal("approved"), rs.getBigDecimal("pending")),
                companyId, projectId, start, end);

        List<ProjectTimesheetDtos.WeekHoursRow> byWeek = jdbcTemplate.query("""
                        SELECT date_trunc('week', work_date)::date AS week_start,
                               COALESCE(SUM(hours), 0) AS hours
                          FROM prj_timesheet
                         WHERE company_id = ? AND project_id = ? AND deleted = false
                           AND status = 'APPROVED' AND work_date BETWEEN ? AND ?
                         GROUP BY 1 ORDER BY 1
                        """,
                (rs, rowNum) -> new ProjectTimesheetDtos.WeekHoursRow(
                        rs.getObject("week_start", LocalDate.class), rs.getBigDecimal("hours")),
                companyId, projectId, start, end);

        return new ProjectTimesheetDtos.TimesheetSummary(projectId, str(project.get("code")),
                str(project.get("name")), start, end,
                hoursOf(hoursByStatus, "APPROVED"), hoursOf(hoursByStatus, "SUBMITTED"),
                hoursOf(hoursByStatus, "REJECTED"),
                countOf(countByStatus, "APPROVED"), countOf(countByStatus, "SUBMITTED"),
                byUser, byTask, byWeek);
    }

    /** 资源负荷：按人按周的已审批工时；容量按 5×8=40h/周，不接节假日日历。 */
    public ProjectTimesheetDtos.ResourceLoad resourceLoad(Long projectId, LocalDate from,
                                                          LocalDate to, Long companyId) {
        requireProject(projectId, companyId);
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());
        if (end.isBefore(start)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID, "结束日期不能早于开始日期");
        }
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<ProjectTimesheetDtos.WeekHoursRow>> weeks = new LinkedHashMap<>();
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT t.user_id, u.display_name,
                               date_trunc('week', t.work_date)::date AS week_start,
                               COALESCE(SUM(t.hours), 0) AS hours
                          FROM prj_timesheet t
                          LEFT JOIN sys_user u ON u.id = t.user_id
                         WHERE t.company_id = ? AND t.project_id = ? AND t.deleted = false
                           AND t.status = 'APPROVED' AND t.work_date BETWEEN ? AND ?
                         GROUP BY 1, 2, 3 ORDER BY 1, 3
                        """,
                rs -> {
                    long userId = rs.getLong("user_id");
                    names.putIfAbsent(userId, rs.getString("display_name"));
                    BigDecimal hours = rs.getBigDecimal("hours");
                    weeks.computeIfAbsent(userId, key -> new ArrayList<>())
                            .add(new ProjectTimesheetDtos.WeekHoursRow(
                                    rs.getObject("week_start", LocalDate.class), hours));
                    totals.merge(userId, hours, BigDecimal::add);
                }, companyId, projectId, start, end);

        int weekCount = (int) (ChronoUnit.WEEKS.between(mondayOf(start), mondayOf(end)) + 1);
        List<ProjectTimesheetDtos.ResourceLoadRow> rows = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : totals.entrySet()) {
            rows.add(new ProjectTimesheetDtos.ResourceLoadRow(entry.getKey(),
                    names.get(entry.getKey()), entry.getValue(),
                    weeks.getOrDefault(entry.getKey(), List.of())));
        }
        rows.sort((left, right) -> right.approvedHours().compareTo(left.approvedHours()));
        return new ProjectTimesheetDtos.ResourceLoad(projectId, start, end, weekCount,
                WEEK_CAPACITY.multiply(BigDecimal.valueOf(weekCount)), rows);
    }

    // ---------------------------------------------------------------- 写

    @Transactional
    public ProjectTimesheetDtos.TimesheetRow create(ProjectTimesheetDtos.TimesheetSaveReq req,
                                                    Long companyId, Long userId) {
        Map<String, Object> task = requireTask(req.taskId(), companyId);
        validate(req);
        ensureUnique(req.taskId(), userId, req.workDate(), null);
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO prj_timesheet (id, company_id, project_id, task_id, user_id,
                                                   work_date, hours, work_type, status, remark,
                                                   created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?)
                        """,
                id, companyId, longOf(task.get("project_id")), req.taskId(), userId,
                req.workDate(), req.hours(), normalizeWorkType(req.workType()),
                blankToNull(req.remark()), userId, userId);
        return row(id, companyId);
    }

    @Transactional
    public ProjectTimesheetDtos.TimesheetRow update(Long id, ProjectTimesheetDtos.TimesheetSaveReq req,
                                                    Long companyId, Long userId) {
        Map<String, Object> row = requireTimesheet(id, companyId);
        requireOwner(row, userId);
        requireEditable(str(row.get("status")));
        Map<String, Object> task = requireTask(req.taskId(), companyId);
        validate(req);
        ensureUnique(req.taskId(), longOf(row.get("user_id")), req.workDate(), id);
        jdbcTemplate.update("""
                        UPDATE prj_timesheet
                           SET project_id = ?, task_id = ?, work_date = ?, hours = ?, work_type = ?,
                               remark = ?, status = 'DRAFT', reject_reason = NULL,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                longOf(task.get("project_id")), req.taskId(), req.workDate(), req.hours(),
                normalizeWorkType(req.workType()), blankToNull(req.remark()), userId, id, companyId);
        return row(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId, Long userId) {
        Map<String, Object> row = requireTimesheet(id, companyId);
        requireOwner(row, userId);
        requireEditable(str(row.get("status")));
        jdbcTemplate.update("""
                UPDATE prj_timesheet SET deleted = true, updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
    }

    @Transactional
    public ProjectTimesheetDtos.TimesheetRow submit(Long id, Long companyId, Long userId) {
        Map<String, Object> row = requireTimesheet(id, companyId);
        requireOwner(row, userId);
        requireEditable(str(row.get("status")));
        jdbcTemplate.update("""
                        UPDATE prj_timesheet
                           SET status = 'SUBMITTED', submitted_at = now(), reject_reason = NULL,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return row(id, companyId);
    }

    /** 审批通过：不能审自己的（自我审批 = 审计缺陷）。 */
    @Transactional
    public ProjectTimesheetDtos.TimesheetRow approve(Long id, Long companyId, Long userId) {
        Map<String, Object> row = requireTimesheet(id, companyId);
        if (!"SUBMITTED".equals(str(row.get("status")))) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_STATE_INVALID,
                    "只有已提交的工时可以审批（当前：" + str(row.get("status")) + "）");
        }
        if (longOf(row.get("user_id")) != null && longOf(row.get("user_id")).equals(userId)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_STATE_INVALID, "不能审批自己填报的工时");
        }
        jdbcTemplate.update("""
                        UPDATE prj_timesheet
                           SET status = 'APPROVED', approved_by = ?, approved_at = now(),
                               reject_reason = NULL, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, userId, id, companyId);
        return row(id, companyId);
    }

    @Transactional
    public ProjectTimesheetDtos.TimesheetRow reject(Long id, String reason, Long companyId,
                                                    Long userId) {
        Map<String, Object> row = requireTimesheet(id, companyId);
        if (!"SUBMITTED".equals(str(row.get("status")))) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_STATE_INVALID,
                    "只有已提交的工时可以打回（当前：" + str(row.get("status")) + "）");
        }
        jdbcTemplate.update("""
                        UPDATE prj_timesheet
                           SET status = 'REJECTED', reject_reason = ?, approved_by = ?,
                               approved_at = now(), updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, reason.trim(), userId, userId, id, companyId);
        return row(id, companyId);
    }

    /** 批量通过：整批原子，遇到不合法直接抛（避免部分通过后对不上账）。 */
    @Transactional
    public int batchApprove(List<Long> ids, Long companyId, Long userId) {
        for (Long id : ids) {
            approve(id, companyId, userId);
        }
        return ids.size();
    }

    // ---------------------------------------------------------------- 内部

    private void validate(ProjectTimesheetDtos.TimesheetSaveReq req) {
        if (req.workDate().isAfter(LocalDate.now())) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID, "不能填报未来日期的工时");
        }
        if (req.hours().compareTo(BigDecimal.ZERO) <= 0
                || req.hours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID, "单日工时必须在 0~24 小时之间");
        }
    }

    private void ensureUnique(Long taskId, Long userId, LocalDate workDate, Long excludeId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1) FROM prj_timesheet
                 WHERE task_id = ? AND user_id = ? AND work_date = ? AND deleted = false
                """);
        List<Object> args = new ArrayList<>(List.of(taskId, userId, workDate));
        if (excludeId != null) {
            sql.append(" AND id <> ?");
            args.add(excludeId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID,
                    "这一天在这个任务上已经填过工时了，请直接修改那一条");
        }
    }

    private void requireEditable(String status) {
        if (!Set.of("DRAFT", "REJECTED").contains(status)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_STATE_INVALID,
                    "只有草稿或已打回的工时可以修改（当前：" + status + "）");
        }
    }

    private void requireOwner(Map<String, Object> row, Long userId) {
        if (longOf(row.get("user_id")) == null || !longOf(row.get("user_id")).equals(userId)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_STATE_INVALID, "只能操作自己填报的工时");
        }
    }

    private Map<String, Object> requireProject(Long projectId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name FROM prj_project
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, projectId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_NOT_FOUND, "项目不存在：" + projectId);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireTask(Long taskId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, project_id FROM prj_task
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, taskId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_TASK_NOT_FOUND, "任务不存在：" + taskId);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireTimesheet(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, user_id, project_id, task_id, work_date, hours, status, remark
                  FROM prj_timesheet WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_NOT_FOUND, "工时记录不存在：" + id);
        }
        return rows.get(0);
    }

    private ProjectTimesheetDtos.TimesheetRow row(Long id, Long companyId) {
        return jdbcTemplate.queryForObject(BASE_SELECT + " WHERE t.id = ? AND t.company_id = ?",
                (rs, rowNum) -> row(rs), id, companyId);
    }

    private ProjectTimesheetDtos.TimesheetRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ProjectTimesheetDtos.TimesheetRow(rs.getLong("id"), rs.getLong("project_id"),
                rs.getString("project_code"), rs.getString("project_name"), rs.getLong("task_id"),
                rs.getString("task_wbs_code"), rs.getString("task_name"), rs.getLong("user_id"),
                rs.getString("user_name"), rs.getObject("work_date", LocalDate.class),
                rs.getBigDecimal("hours"), rs.getString("work_type"), rs.getString("status"),
                rs.getString("remark"), rs.getObject("submitted_at", OffsetDateTime.class),
                rs.getString("approver_name"), rs.getObject("approved_at", OffsetDateTime.class),
                rs.getString("reject_reason"), rs.getObject("created_at", OffsetDateTime.class));
    }

    private static String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID,
                    "未知工时状态：" + status + "（可选 " + String.join("/", STATUSES) + "）");
        }
        return normalized;
    }

    private static String normalizeWorkType(String workType) {
        if (workType == null || workType.isBlank()) {
            return "NORMAL";
        }
        String normalized = workType.trim().toUpperCase();
        if (!WORK_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_TIMESHEET_INVALID,
                    "未知工时类型：" + workType + "（可选 " + String.join("/", WORK_TYPES) + "）");
        }
        return normalized;
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private static BigDecimal hoursOf(Map<String, BigDecimal> map, String status) {
        return map.getOrDefault(status, BigDecimal.ZERO);
    }

    private static int countOf(Map<String, Integer> map, String status) {
        return map.getOrDefault(status, 0);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
