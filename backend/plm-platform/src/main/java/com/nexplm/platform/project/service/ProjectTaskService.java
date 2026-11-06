package com.nexplm.platform.project.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.project.api.ProjectTaskDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WBS 任务与依赖（P7-2）。
 *
 * <p>关键规则：</p>
 * <ul>
 *   <li><b>WBS 编码由服务端生成</b>：「父编码 + 父节点下最大序号 + 1」，前端不手填，
 *       避免撞唯一索引；顺序调整走 {@code tasks/resequence} 统一重排（两阶段写，避开互换撞键）；</li>
 *   <li><b>项目进度自动汇总</b>：只统计<b>叶子任务</b>（有子任务的父节点不计数，否则重复加权），
 *       权重取工期天数（无工期取 1），每次任务增删改后写回 {@code prj_project.progress}；</li>
 *   <li><b>依赖防环</b>：建依赖前对「后续任务」做一次 DFS，若已能走回前置任务说明成环，直接 400；</li>
 *   <li><b>删除守卫</b>：有子任务的任务不能删（要先删子任务），删任务时连带清掉它的依赖边。</li>
 * </ul>
 */
@Service
public class ProjectTaskService {

    private static final Set<String> TYPES = Set.of("TASK", "MILESTONE");
    private static final Set<String> STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "DONE", "BLOCKED");
    private static final Set<String> DEP_TYPES = Set.of("FS", "SS", "FF", "SF");

    private final JdbcTemplate jdbcTemplate;

    public ProjectTaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- 查询

    /** WBS 看板：任务树 + 依赖 + 时间范围 + 项目汇总进度。 */
    public ProjectTaskDtos.TaskBoard board(Long projectId, Long companyId) {
        Map<String, Object> project = requireProject(projectId, companyId);
        List<ProjectTaskDtos.TaskRow> flat = tasksFlat(projectId);
        LocalDate rangeStart = null;
        LocalDate rangeEnd = null;
        for (ProjectTaskDtos.TaskRow task : flat) {
            if (task.planStart() != null && (rangeStart == null || task.planStart().isBefore(rangeStart))) {
                rangeStart = task.planStart();
            }
            if (task.planEnd() != null && (rangeEnd == null || task.planEnd().isAfter(rangeEnd))) {
                rangeEnd = task.planEnd();
            }
        }
        return new ProjectTaskDtos.TaskBoard(projectId, str(project.get("code")), str(project.get("name")),
                intOf(project.get("progress")), rangeStart, rangeEnd,
                buildTree(flat), dependencies(projectId));
    }

    private List<ProjectTaskDtos.TaskRow> tasksFlat(Long projectId) {
        return jdbcTemplate.query("""
                        SELECT t.*, u.display_name AS owner_name,
                               (SELECT COUNT(1) FROM prj_task c
                                 WHERE c.project_id = t.project_id AND c.parent_id = t.id
                                   AND c.deleted = false) AS child_count
                          FROM prj_task t
                          LEFT JOIN sys_user u ON u.id = t.owner_id
                         WHERE t.project_id = ? AND t.deleted = false
                         ORDER BY t.wbs_code
                        """,
                (rs, rowNum) -> new ProjectTaskDtos.TaskRow(rs.getLong("id"), rs.getLong("project_id"),
                        (Long) rs.getObject("parent_id"), rs.getString("wbs_code"), rs.getString("name"),
                        rs.getString("type"), rs.getString("status"), rs.getInt("sort_no"),
                        rs.getObject("plan_start", LocalDate.class),
                        rs.getObject("plan_end", LocalDate.class),
                        rs.getObject("actual_start", LocalDate.class),
                        rs.getObject("actual_end", LocalDate.class),
                        (Integer) rs.getObject("duration_days"), rs.getInt("progress"),
                        (Long) rs.getObject("owner_id"), rs.getString("owner_name"), rs.getString("remark"),
                        rs.getInt("child_count"), List.of()),
                projectId);
    }

    /** 扁平列表 → WBS 树（按 wbs_code 已排序，兄弟顺序即编码顺序）。 */
    private List<ProjectTaskDtos.TaskRow> buildTree(List<ProjectTaskDtos.TaskRow> flat) {
        Map<Long, List<ProjectTaskDtos.TaskRow>> byParent = new LinkedHashMap<>();
        for (ProjectTaskDtos.TaskRow task : flat) {
            byParent.computeIfAbsent(task.parentId(), key -> new ArrayList<>()).add(task);
        }
        return attachChildren(byParent, null);
    }

    private List<ProjectTaskDtos.TaskRow> attachChildren(
            Map<Long, List<ProjectTaskDtos.TaskRow>> byParent, Long parentId) {
        List<ProjectTaskDtos.TaskRow> siblings = byParent.getOrDefault(parentId, List.of());
        List<ProjectTaskDtos.TaskRow> nodes = new ArrayList<>(siblings.size());
        for (ProjectTaskDtos.TaskRow task : siblings) {
            nodes.add(new ProjectTaskDtos.TaskRow(task.id(), task.projectId(), task.parentId(),
                    task.wbsCode(), task.name(), task.type(), task.status(), task.sortNo(),
                    task.planStart(), task.planEnd(), task.actualStart(), task.actualEnd(),
                    task.durationDays(), task.progress(), task.ownerId(), task.ownerName(),
                    task.remark(), task.childCount(), attachChildren(byParent, task.id())));
        }
        return nodes;
    }

    private List<ProjectTaskDtos.DependencyRow> dependencies(Long projectId) {
        return jdbcTemplate.query("""
                        SELECT d.id, d.pred_task_id, d.succ_task_id, d.dep_type, d.lag_days,
                               p.wbs_code || ' ' || p.name AS pred_name,
                               s.wbs_code || ' ' || s.name AS succ_name
                          FROM prj_task_dep d
                          LEFT JOIN prj_task p ON p.id = d.pred_task_id
                          LEFT JOIN prj_task s ON s.id = d.succ_task_id
                         WHERE d.project_id = ?
                         ORDER BY d.id
                        """,
                (rs, rowNum) -> new ProjectTaskDtos.DependencyRow(rs.getLong("id"),
                        rs.getLong("pred_task_id"), rs.getLong("succ_task_id"),
                        rs.getString("dep_type"), rs.getInt("lag_days"),
                        rs.getString("pred_name"), rs.getString("succ_name")),
                projectId);
    }

    // ---------------------------------------------------------------- 任务写

    @Transactional
    public ProjectTaskDtos.TaskBoard createTask(Long projectId, ProjectTaskDtos.TaskSaveReq req,
                                                Long companyId, Long userId) {
        requireProject(projectId, companyId);
        validateTask(req);
        Long parentId = req.parentId();
        if (parentId != null) {
            requireTaskInProject(parentId, projectId);
        }
        int progress = req.progress() == null ? 0 : req.progress();
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO prj_task (id, company_id, project_id, parent_id, wbs_code, name, type,
                                              status, sort_no, plan_start, plan_end, actual_start, actual_end,
                                              duration_days, progress, owner_id, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, projectId, parentId, nextWbsCode(projectId, parentId),
                req.name().trim(), normalizeType(req.type()),
                normalizeStatus(req.status(), progress),
                req.sortNo() == null ? nextSortNo(projectId, parentId) : req.sortNo(),
                req.planStart(), req.planEnd(), req.actualStart(), req.actualEnd(),
                durationOf(req), progress, req.ownerId() == null ? userId : req.ownerId(),
                blankToNull(req.remark()), userId, userId);
        recalcProgress(projectId);
        return board(projectId, companyId);
    }

    @Transactional
    public ProjectTaskDtos.TaskBoard updateTask(Long taskId, ProjectTaskDtos.TaskSaveReq req,
                                                Long companyId, Long userId) {
        Map<String, Object> task = requireTask(taskId, companyId);
        Long projectId = longOf(task.get("project_id"));
        validateTask(req);
        int progress = req.progress() == null ? intOf(task.get("progress")) : req.progress();
        jdbcTemplate.update("""
                        UPDATE prj_task
                           SET name = ?, type = ?, status = ?, sort_no = ?,
                               plan_start = ?, plan_end = ?, actual_start = ?, actual_end = ?,
                               duration_days = ?, progress = ?, owner_id = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeType(req.type()),
                normalizeStatus(req.status(), progress),
                req.sortNo() == null ? intOf(task.get("sort_no")) : req.sortNo(),
                req.planStart(), req.planEnd(), req.actualStart(), req.actualEnd(),
                durationOf(req), progress,
                req.ownerId() == null ? longOf(task.get("owner_id")) : req.ownerId(),
                blankToNull(req.remark()), userId, taskId, companyId);
        recalcProgress(projectId);
        return board(projectId, companyId);
    }

    @Transactional
    public ProjectTaskDtos.TaskBoard deleteTask(Long taskId, Long companyId) {
        Map<String, Object> task = requireTask(taskId, companyId);
        Long projectId = longOf(task.get("project_id"));
        Integer children = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM prj_task WHERE project_id = ? AND parent_id = ? AND deleted = false
                """, Integer.class, projectId, taskId);
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.PRJ_TASK_IN_USE, "该任务下还有 " + children + " 个子任务，不能删除");
        }
        jdbcTemplate.update("DELETE FROM prj_task_dep WHERE pred_task_id = ? OR succ_task_id = ?",
                taskId, taskId);
        jdbcTemplate.update("""
                UPDATE prj_task SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, taskId, companyId);
        recalcProgress(projectId);
        return board(projectId, companyId);
    }

    /**
     * WBS 编码重排：按 {@code (sort_no, id)} 重排兄弟顺序并重写全部编码。
     *
     * <p>两阶段写：先把全项目编码改成 {@code tmp-<id>}，再写最终编码——否则「1.2 与 1.3 互换」
     * 会在中途撞 {@code uk_prj_task_wbs}。</p>
     */
    @Transactional
    public ProjectTaskDtos.TaskBoard resequence(Long projectId, Long companyId, Long userId) {
        requireProject(projectId, companyId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, parent_id, sort_no FROM prj_task
                 WHERE project_id = ? AND deleted = false
                 ORDER BY sort_no, id
                """, projectId);
        Map<Long, List<Map<String, Object>>> byParent = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byParent.computeIfAbsent(longOf(row.get("parent_id")), key -> new ArrayList<>()).add(row);
        }
        Map<Long, String> codes = new LinkedHashMap<>();
        assignCodes(byParent, null, null, codes);
        for (Long id : codes.keySet()) {
            jdbcTemplate.update("UPDATE prj_task SET wbs_code = ? WHERE id = ?", "tmp-" + id, id);
        }
        for (Map.Entry<Long, String> entry : codes.entrySet()) {
            jdbcTemplate.update("UPDATE prj_task SET wbs_code = ?, updated_by = ?, updated_at = now() WHERE id = ?",
                    entry.getValue(), userId, entry.getKey());
        }
        return board(projectId, companyId);
    }

    private void assignCodes(Map<Long, List<Map<String, Object>>> byParent, Long parentId,
                             String parentCode, Map<Long, String> codes) {
        List<Map<String, Object>> siblings = byParent.getOrDefault(parentId, List.of());
        int index = 1;
        for (Map<String, Object> row : siblings) {
            Long id = longOf(row.get("id"));
            String code = parentCode == null ? String.valueOf(index) : parentCode + "." + index;
            codes.put(id, code);
            assignCodes(byParent, id, code, codes);
            index++;
        }
    }

    // ---------------------------------------------------------------- 依赖写

    @Transactional
    public ProjectTaskDtos.TaskBoard createDependency(Long projectId,
                                                      ProjectTaskDtos.DependencySaveReq req,
                                                      Long companyId) {
        requireProject(projectId, companyId);
        if (req.predTaskId().equals(req.succTaskId())) {
            throw BizException.of(ErrorCode.PRJ_TASK_DEP_INVALID, "任务不能依赖自己");
        }
        requireTaskInProject(req.predTaskId(), projectId);
        requireTaskInProject(req.succTaskId(), projectId);
        String depType = req.depType() == null || req.depType().isBlank()
                ? "FS" : req.depType().trim().toUpperCase();
        if (!DEP_TYPES.contains(depType)) {
            throw BizException.of(ErrorCode.PRJ_TASK_DEP_INVALID,
                    "未知依赖类型：" + req.depType() + "（可选 " + String.join("/", DEP_TYPES) + "）");
        }
        Integer duplicated = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM prj_task_dep
                 WHERE pred_task_id = ? AND succ_task_id = ?
                """, Integer.class, req.predTaskId(), req.succTaskId());
        if (duplicated != null && duplicated > 0) {
            throw BizException.of(ErrorCode.PRJ_TASK_DEP_INVALID, "这两条任务之间已经有依赖了");
        }
        Map<Long, List<Long>> graph = dependencyGraph(projectId);
        if (reachable(graph, req.succTaskId(), req.predTaskId(), new HashSet<>())) {
            throw BizException.of(ErrorCode.PRJ_TASK_DEP_INVALID,
                    "该依赖会形成环（后续任务已经能经其它依赖回到前置任务）");
        }
        jdbcTemplate.update("""
                        INSERT INTO prj_task_dep (id, company_id, project_id, pred_task_id, succ_task_id,
                                                  dep_type, lag_days)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), companyId, projectId, req.predTaskId(), req.succTaskId(),
                depType, req.lagDays() == null ? 0 : req.lagDays());
        return board(projectId, companyId);
    }

    @Transactional
    public ProjectTaskDtos.TaskBoard deleteDependency(Long dependencyId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, project_id FROM prj_task_dep WHERE id = ? AND company_id = ?
                """, dependencyId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_TASK_DEP_INVALID, "任务依赖不存在：" + dependencyId);
        }
        Long projectId = longOf(rows.get(0).get("project_id"));
        jdbcTemplate.update("DELETE FROM prj_task_dep WHERE id = ?", dependencyId);
        return board(projectId, companyId);
    }

    /** 项目内依赖图：前置 → 后续列表。 */
    private Map<Long, List<Long>> dependencyGraph(Long projectId) {
        Map<Long, List<Long>> graph = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT pred_task_id, succ_task_id FROM prj_task_dep WHERE project_id = ?
                        """,
                rs -> {
                    graph.computeIfAbsent(rs.getLong("pred_task_id"), key -> new ArrayList<>())
                            .add(rs.getLong("succ_task_id"));
                },
                projectId);
        return graph;
    }

    private boolean reachable(Map<Long, List<Long>> graph, Long from, Long target, Set<Long> visited) {
        if (from.equals(target)) {
            return true;
        }
        if (!visited.add(from)) {
            return false;
        }
        for (Long next : graph.getOrDefault(from, List.of())) {
            if (reachable(graph, next, target, visited)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 进度汇总

    /**
     * 项目进度 = 叶子任务加权平均（权重=工期天数，缺省 1），写回 {@code prj_project.progress}。
     *
     * <p>只算叶子：父任务的进度若也参与，会把同一份工作量算两遍。</p>
     */
    public int recalcProgress(Long projectId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, parent_id, progress, duration_days, plan_start, plan_end
                  FROM prj_task WHERE project_id = ? AND deleted = false
                """, projectId);
        Set<Long> parentIds = new HashSet<>();
        for (Map<String, Object> row : rows) {
            Long parentId = longOf(row.get("parent_id"));
            if (parentId != null) {
                parentIds.add(parentId);
            }
        }
        double weighted = 0;
        double weights = 0;
        for (Map<String, Object> row : rows) {
            Long id = longOf(row.get("id"));
            if (parentIds.contains(id)) {
                continue;
            }
            double weight = weightOf(row);
            weighted += intOf(row.get("progress")) * weight;
            weights += weight;
        }
        int progress = weights == 0 ? 0 : (int) Math.round(weighted / weights);
        jdbcTemplate.update("""
                UPDATE prj_project SET progress = ?, updated_at = now()
                 WHERE id = ? AND deleted = false
                """, progress, projectId);
        return progress;
    }

    private double weightOf(Map<String, Object> row) {
        Object duration = row.get("duration_days");
        if (duration instanceof Number number && number.intValue() > 0) {
            return number.intValue();
        }
        Object start = row.get("plan_start");
        Object end = row.get("plan_end");
        if (start instanceof java.sql.Date startDate && end instanceof java.sql.Date endDate) {
            long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
            return Math.max(days, 1);
        }
        if (start instanceof LocalDate startDate && end instanceof LocalDate endDate) {
            return Math.max(ChronoUnit.DAYS.between(startDate, endDate) + 1, 1);
        }
        return 1;
    }

    // ---------------------------------------------------------------- 内部

    /** 父节点下最大序号 + 1（用编号而不是子节点个数，避免删中间节点后编码撞车）。 */
    private String nextWbsCode(Long projectId, Long parentId) {
        String parentCode = null;
        List<String> siblingCodes;
        if (parentId == null) {
            siblingCodes = jdbcTemplate.query("""
                            SELECT wbs_code FROM prj_task
                             WHERE project_id = ? AND parent_id IS NULL AND deleted = false
                            """, (rs, rowNum) -> rs.getString(1), projectId);
        } else {
            Map<String, Object> parent = requireTaskInProject(parentId, projectId);
            parentCode = str(parent.get("wbs_code"));
            siblingCodes = jdbcTemplate.query("""
                            SELECT wbs_code FROM prj_task
                             WHERE project_id = ? AND parent_id = ? AND deleted = false
                            """, (rs, rowNum) -> rs.getString(1), projectId, parentId);
        }
        int max = 0;
        for (String code : siblingCodes) {
            int index = code.lastIndexOf('.');
            String tail = index < 0 ? code : code.substring(index + 1);
            try {
                max = Math.max(max, Integer.parseInt(tail));
            } catch (NumberFormatException ignored) {
                // 脏编码（如 resequence 中途的 tmp-）直接跳过，不影响新编码生成
            }
        }
        int next = max + 1;
        return parentCode == null ? String.valueOf(next) : parentCode + "." + next;
    }

    private int nextSortNo(Long projectId, Long parentId) {
        Integer max = parentId == null
                ? jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(sort_no), 0) FROM prj_task
                         WHERE project_id = ? AND parent_id IS NULL AND deleted = false
                        """, Integer.class, projectId)
                : jdbcTemplate.queryForObject("""
                        SELECT COALESCE(MAX(sort_no), 0) FROM prj_task
                         WHERE project_id = ? AND parent_id = ? AND deleted = false
                        """, Integer.class, projectId, parentId);
        return (max == null ? 0 : max) + 10;
    }

    private void validateTask(ProjectTaskDtos.TaskSaveReq req) {
        if (req.planStart() != null && req.planEnd() != null && req.planEnd().isBefore(req.planStart())) {
            throw BizException.of(ErrorCode.PRJ_TASK_INVALID, "计划结束日不能早于计划开始日");
        }
        if (req.progress() != null && (req.progress() < 0 || req.progress() > 100)) {
            throw BizException.of(ErrorCode.PRJ_TASK_INVALID, "进度必须在 0~100 之间");
        }
    }

    private Integer durationOf(ProjectTaskDtos.TaskSaveReq req) {
        if (req.durationDays() != null) {
            return req.durationDays() <= 0 ? null : req.durationDays();
        }
        if (req.planStart() != null && req.planEnd() != null) {
            return (int) (ChronoUnit.DAYS.between(req.planStart(), req.planEnd()) + 1);
        }
        return null;
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "TASK";
        }
        String normalized = type.trim().toUpperCase();
        if (!TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_TASK_INVALID,
                    "未知任务类型：" + type + "（可选 " + String.join("/", TYPES) + "）");
        }
        return normalized;
    }

    /** 没显式给状态时由进度推断（100=完成 / 0=未开始 / 其余进行中），BLOCKED 需显式指定。 */
    private static String normalizeStatus(String status, int progress) {
        if (status == null || status.isBlank()) {
            if (progress >= 100) {
                return "DONE";
            }
            return progress > 0 ? "IN_PROGRESS" : "NOT_STARTED";
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_TASK_INVALID,
                    "未知任务状态：" + status + "（可选 " + String.join("/", STATUSES) + "）");
        }
        return normalized;
    }

    private Map<String, Object> requireProject(Long projectId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name, progress FROM prj_project
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, projectId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_NOT_FOUND, "项目不存在：" + projectId);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireTask(Long taskId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, project_id, parent_id, wbs_code, sort_no, progress, owner_id FROM prj_task
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, taskId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_TASK_NOT_FOUND, "任务不存在：" + taskId);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireTaskInProject(Long taskId, Long projectId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, wbs_code FROM prj_task
                 WHERE id = ? AND project_id = ? AND deleted = false
                """, taskId, projectId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_TASK_INVALID, "任务不属于该项目：" + taskId);
        }
        return rows.get(0);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
