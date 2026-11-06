package com.nexplm.platform.project.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.project.api.ProjectDeliverableDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务交付物与齐套性检查（P7-4）。
 *
 * <p>三条关键口径：</p>
 * <ul>
 *   <li><b>对象多态引用</b>：{@code object_type + object_id} 不建外键，三类对象分属三张表
 *       （DOC/ITEM → {@code plm_object}，CAPP_CARD → {@code capp_card}），
 *       查询时实时回查 code/name/生命周期，不存快照，避免对象改名后两份数据不一致；</li>
 *   <li><b>齐套只认必需输出</b>：{@code output_type='OUTPUT' AND required=true}，
 *       输入项只用于追溯上游来源，不参与齐套；</li>
 *   <li><b>FAIL 极窄</b>：只有「对象已删除」{@code MISSING} 与「对象已废弃」{@code OBSOLETE}
 *       算不齐套；「存在但未发布」{@code UNRELEASED} 只是提醒——活干到一半不该拦人。</li>
 * </ul>
 */
@Service
public class ProjectDeliverableService {

    private static final Set<String> OBJECT_TYPES = Set.of("DOC", "ITEM", "CAPP_CARD");
    private static final Set<String> OUTPUT_TYPES = Set.of("INPUT", "OUTPUT");
    /** 交付物类型 → plm_object.object_type。CAPP_CARD 不在通用对象表里。 */
    private static final Map<String, String> PLM_OBJECT_TYPES =
            Map.of("DOC", "PdmDoc", "ITEM", "PdmItem");

    private final JdbcTemplate jdbcTemplate;

    public ProjectDeliverableService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- 查询

    /** 项目齐套看板：全部任务 + 交付物 + 项目级汇总（无交付物的任务也返回，便于前端整表展示）。 */
    public ProjectDeliverableDtos.DeliverableBoard board(Long projectId, Long companyId) {
        Map<String, Object> project = requireProject(projectId, companyId);
        List<Map<String, Object>> taskRows = jdbcTemplate.queryForList("""
                SELECT id, wbs_code, name, status FROM prj_task
                 WHERE project_id = ? AND deleted = false
                 ORDER BY wbs_code
                """, projectId);
        List<RawOutput> outputs = loadOutputs(projectId, companyId);
        Map<String, ObjectInfo> objects = resolveObjects(outputs);

        Map<Long, List<ProjectDeliverableDtos.DeliverableRow>> byTask = new LinkedHashMap<>();
        for (RawOutput raw : outputs) {
            ObjectInfo info = objects.get(key(raw.objectType(), raw.objectId()));
            String state = stateOf(raw.objectType(), info);
            boolean ready = !"MISSING".equals(state) && !"OBSOLETE".equals(state);
            byTask.computeIfAbsent(raw.taskId(), id -> new ArrayList<>())
                    .add(new ProjectDeliverableDtos.DeliverableRow(raw.id(), raw.taskId(),
                            raw.taskWbsCode(), raw.taskName(), raw.outputType(), raw.objectType(),
                            raw.objectId(), info == null ? null : info.code(),
                            info == null ? null : info.name(),
                            info == null ? null : info.lifecycleState(),
                            state, raw.required(), ready, raw.remark()));
        }

        int requiredTotal = 0;
        int readyTotal = 0;
        int missingTotal = 0;
        int unreleasedTotal = 0;
        List<ProjectDeliverableDtos.TaskCompletenessRow> tasks = new ArrayList<>(taskRows.size());
        for (Map<String, Object> row : taskRows) {
            Long taskId = longOf(row.get("id"));
            List<ProjectDeliverableDtos.DeliverableRow> items =
                    byTask.getOrDefault(taskId, List.of());
            int requiredCount = 0;
            int taskReadyCount = 0;
            int missingCount = 0;
            int taskUnreleased = 0;
            for (ProjectDeliverableDtos.DeliverableRow item : items) {
                if (!"OUTPUT".equals(item.outputType()) || !item.required()) {
                    continue;
                }
                requiredCount++;
                if (item.ready()) {
                    taskReadyCount++;
                } else {
                    missingCount++;
                }
                if ("UNRELEASED".equals(item.state())) {
                    taskUnreleased++;
                }
            }
            requiredTotal += requiredCount;
            readyTotal += taskReadyCount;
            missingTotal += missingCount;
            unreleasedTotal += taskUnreleased;
            tasks.add(new ProjectDeliverableDtos.TaskCompletenessRow(taskId,
                    str(row.get("wbs_code")), str(row.get("name")), str(row.get("status")),
                    requiredCount, taskReadyCount, missingCount, taskUnreleased,
                    missingCount == 0, items));
        }
        return new ProjectDeliverableDtos.DeliverableBoard(projectId, str(project.get("code")),
                str(project.get("name")), requiredTotal, readyTotal, missingTotal, unreleasedTotal,
                missingTotal == 0, tasks);
    }

    /** 添加交付物时的候选对象（按编码/名称模糊查，只列最新版本）。 */
    public List<ProjectDeliverableDtos.CandidateRow> candidates(String objectType, String keyword,
                                                                Integer limit, Long companyId) {
        String type = normalizeObjectType(objectType);
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        if ("CAPP_CARD".equals(type)) {
            StringBuilder sql = new StringBuilder("""
                    SELECT id, code, COALESCE(NULLIF(title, ''), code) AS name
                      FROM capp_card
                     WHERE company_id = ? AND deleted = false
                    """);
            List<Object> args = new ArrayList<>();
            args.add(companyId);
            if (like != null) {
                sql.append(" AND (code ILIKE ? OR title ILIKE ?)");
                args.add(like);
                args.add(like);
            }
            sql.append(" ORDER BY updated_at DESC LIMIT ?");
            args.add(size);
            return jdbcTemplate.query(sql.toString(),
                    (rs, rowNum) -> new ProjectDeliverableDtos.CandidateRow("CAPP_CARD",
                            rs.getLong("id"), rs.getString("code"), rs.getString("name"), null),
                    args.toArray());
        }
        StringBuilder sql = new StringBuilder("""
                SELECT id, code, name, lifecycle_state
                  FROM plm_object
                 WHERE company_id = ? AND object_type = ? AND deleted = false AND is_latest = true
                """);
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        args.add(PLM_OBJECT_TYPES.get(type));
        if (like != null) {
            sql.append(" AND (code ILIKE ? OR name ILIKE ?)");
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY updated_at DESC LIMIT ?");
        args.add(size);
        return jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new ProjectDeliverableDtos.CandidateRow(type, rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("lifecycle_state")),
                args.toArray());
    }

    // ---------------------------------------------------------------- 写

    @Transactional
    public ProjectDeliverableDtos.DeliverableBoard add(Long taskId,
                                                       ProjectDeliverableDtos.DeliverableSaveReq req,
                                                       Long companyId, Long userId) {
        Map<String, Object> task = requireTask(taskId, companyId);
        String objectType = normalizeObjectType(req.objectType());
        String outputType = normalizeOutputType(req.outputType());
        requireObject(objectType, req.objectId(), companyId);
        Integer duplicated = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM prj_task_output
                 WHERE task_id = ? AND output_type = ? AND object_type = ? AND object_id = ?
                   AND deleted = false
                """, Integer.class, taskId, outputType, objectType, req.objectId());
        if (duplicated != null && duplicated > 0) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INVALID, "该对象已经挂在这个任务上了");
        }
        jdbcTemplate.update("""
                        INSERT INTO prj_task_output (id, company_id, task_id, output_type, object_type,
                                                     object_id, required, remark, created_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), companyId, taskId, outputType, objectType, req.objectId(),
                req.required() == null || req.required(), blankToNull(req.remark()), userId);
        return board(longOf(task.get("project_id")), companyId);
    }

    @Transactional
    public ProjectDeliverableDtos.DeliverableBoard update(Long outputId,
                                                          ProjectDeliverableDtos.DeliverableUpdateReq req,
                                                          Long companyId) {
        Map<String, Object> row = requireOutput(outputId, companyId);
        Long taskId = longOf(row.get("task_id"));
        String objectType = str(row.get("object_type"));
        Long objectId = longOf(row.get("object_id"));
        String outputType = req.outputType() == null || req.outputType().isBlank()
                ? str(row.get("output_type")) : normalizeOutputType(req.outputType());
        boolean required = req.required() == null
                ? Boolean.TRUE.equals(row.get("required")) : req.required();
        String remark = req.remark() == null ? str(row.get("remark")) : blankToNull(req.remark());
        if (!outputType.equals(str(row.get("output_type")))) {
            Integer duplicated = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM prj_task_output
                     WHERE task_id = ? AND output_type = ? AND object_type = ? AND object_id = ?
                       AND deleted = false AND id <> ?
                    """, Integer.class, taskId, outputType, objectType, objectId, outputId);
            if (duplicated != null && duplicated > 0) {
                throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INVALID,
                        "该任务在这个方向下已经有同一个对象了");
            }
        }
        jdbcTemplate.update("""
                        UPDATE prj_task_output
                           SET output_type = ?, required = ?, remark = ?
                         WHERE id = ? AND company_id = ?
                        """, outputType, required, remark, outputId, companyId);
        return board(longOf(row.get("project_id")), companyId);
    }

    @Transactional
    public ProjectDeliverableDtos.DeliverableBoard remove(Long outputId, Long companyId) {
        Map<String, Object> row = requireOutput(outputId, companyId);
        jdbcTemplate.update("""
                UPDATE prj_task_output SET deleted = true WHERE id = ? AND company_id = ?
                """, outputId, companyId);
        return board(longOf(row.get("project_id")), companyId);
    }

    /**
     * 提交审核前的齐套守卫：必需输出项只要有一项不可用（缺失/废弃）就不许提交。
     *
     * <p>没声明任何必需交付物的任务直接放行——「没要求」不等于「不齐套」，
     * 否则 P7-3 已上线的任务全部会被拦住。</p>
     */
    public void assertOutputsReady(Long taskId, Long companyId) {
        List<RawOutput> outputs = jdbcTemplate.query("""
                        SELECT o.id, o.task_id, o.output_type, o.object_type, o.object_id, o.required,
                               o.remark, t.wbs_code AS task_wbs_code, t.name AS task_name
                          FROM prj_task_output o
                          JOIN prj_task t ON t.id = o.task_id
                         WHERE o.task_id = ? AND o.company_id = ? AND o.deleted = false
                         ORDER BY o.id
                        """,
                (rs, rowNum) -> rawOutput(rs), taskId, companyId);
        List<RawOutput> required = outputs.stream()
                .filter(item -> "OUTPUT".equals(item.outputType()) && item.required())
                .toList();
        if (required.isEmpty()) {
            return;
        }
        Map<String, ObjectInfo> objects = resolveObjects(required);
        List<String> blockers = new ArrayList<>();
        for (RawOutput item : required) {
            ObjectInfo info = objects.get(key(item.objectType(), item.objectId()));
            String state = stateOf(item.objectType(), info);
            if ("MISSING".equals(state) || "OBSOLETE".equals(state)) {
                String label = info == null || info.code() == null
                        ? item.objectType() + "#" + item.objectId() : info.code();
                if (info != null && info.name() != null) {
                    label = label + " " + info.name();
                }
                blockers.add(label);
            }
        }
        if (!blockers.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INCOMPLETE,
                    "以下必需交付物未齐套（已删除或废弃）：" + String.join("、", blockers));
        }
    }

    // ---------------------------------------------------------------- 对象解析

    private List<RawOutput> loadOutputs(Long projectId, Long companyId) {
        return jdbcTemplate.query("""
                        SELECT o.id, o.task_id, o.output_type, o.object_type, o.object_id, o.required,
                               o.remark, t.wbs_code AS task_wbs_code, t.name AS task_name
                          FROM prj_task_output o
                          JOIN prj_task t ON t.id = o.task_id
                         WHERE o.company_id = ? AND t.project_id = ? AND t.deleted = false
                           AND o.deleted = false
                         ORDER BY t.wbs_code,
                                  CASE o.output_type WHEN 'OUTPUT' THEN 0 ELSE 1 END, o.id
                        """,
                (rs, rowNum) -> rawOutput(rs), companyId, projectId);
    }

    private RawOutput rawOutput(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RawOutput(rs.getLong("id"), rs.getLong("task_id"),
                rs.getString("task_wbs_code"), rs.getString("task_name"),
                rs.getString("output_type"), rs.getString("object_type"),
                rs.getLong("object_id"), rs.getBoolean("required"), rs.getString("remark"));
    }

    /**
     * 批量回查三类对象。
     *
     * <p>刻意<b>不过滤 deleted</b>：交付物缺失时用户要看到「缺的是哪一份」，
     * 若连编码都查不到，前端只能显示一个雪花 ID，等于没提示。
     * 软删的行带着 {@code deleted=true} 返回，判定为 {@code MISSING}。</p>
     */
    private Map<String, ObjectInfo> resolveObjects(List<RawOutput> outputs) {
        Set<Long> plmIds = new HashSet<>();
        Set<Long> cardIds = new HashSet<>();
        for (RawOutput item : outputs) {
            if ("CAPP_CARD".equals(item.objectType())) {
                cardIds.add(item.objectId());
            } else if (PLM_OBJECT_TYPES.containsKey(item.objectType())) {
                plmIds.add(item.objectId());
            }
        }
        Map<String, ObjectInfo> objects = new HashMap<>();
        if (!plmIds.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(plmIds.size(), "?"));
            jdbcTemplate.query("""
                            SELECT id, code, name, lifecycle_state, deleted FROM plm_object
                             WHERE id IN (%s)
                            """.formatted(placeholders),
                    rs -> {
                        long id = rs.getLong("id");
                        ObjectInfo info = new ObjectInfo(rs.getString("code"), rs.getString("name"),
                                rs.getString("lifecycle_state"), rs.getBoolean("deleted"));
                        objects.put(key("DOC", id), info);
                        objects.put(key("ITEM", id), info);
                    },
                    plmIds.toArray());
        }
        if (!cardIds.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(cardIds.size(), "?"));
            jdbcTemplate.query("""
                            SELECT id, code, COALESCE(NULLIF(title, ''), code) AS name, deleted
                              FROM capp_card
                             WHERE id IN (%s)
                            """.formatted(placeholders),
                    rs -> {
                        long id = rs.getLong("id");
                        objects.put(key("CAPP_CARD", id), new ObjectInfo(rs.getString("code"),
                                rs.getString("name"), null, rs.getBoolean("deleted")));
                    },
                    cardIds.toArray());
        }
        return objects;
    }

    private static String stateOf(String objectType, ObjectInfo info) {
        if (info == null || info.deleted()) {
            return "MISSING";
        }
        if ("CAPP_CARD".equals(objectType)) {
            return "OK";   // 卡片没有生命周期，存在即可用
        }
        String state = info.lifecycleState();
        if ("Obsolete".equals(state)) {
            return "OBSOLETE";
        }
        if (!"Released".equals(state)) {
            return "UNRELEASED";
        }
        return "OK";
    }

    private void requireObject(String objectType, Long objectId, Long companyId) {
        Integer count;
        if ("CAPP_CARD".equals(objectType)) {
            count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM capp_card WHERE id = ? AND company_id = ? AND deleted = false
                    """, Integer.class, objectId, companyId);
        } else {
            count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM plm_object
                     WHERE id = ? AND company_id = ? AND object_type = ? AND deleted = false
                    """, Integer.class, objectId, companyId, PLM_OBJECT_TYPES.get(objectType));
        }
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INVALID,
                    "对象不存在或不属于当前公司：" + objectType + "#" + objectId);
        }
    }

    // ---------------------------------------------------------------- 内部

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

    private Map<String, Object> requireOutput(Long outputId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT o.id, o.task_id, o.output_type, o.object_type, o.object_id, o.required,
                       o.remark, t.project_id
                  FROM prj_task_output o
                  JOIN prj_task t ON t.id = o.task_id
                 WHERE o.id = ? AND o.company_id = ? AND o.deleted = false
                """, outputId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_NOT_FOUND, "交付物不存在：" + outputId);
        }
        return rows.get(0);
    }

    private static String normalizeObjectType(String objectType) {
        if (objectType == null || objectType.isBlank()) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INVALID,
                    "必须指定对象类型（" + String.join("/", OBJECT_TYPES) + "）");
        }
        String normalized = objectType.trim().toUpperCase();
        if (!OBJECT_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INVALID,
                    "未知对象类型：" + objectType + "（可选 " + String.join("/", OBJECT_TYPES) + "）");
        }
        return normalized;
    }

    private static String normalizeOutputType(String outputType) {
        if (outputType == null || outputType.isBlank()) {
            return "OUTPUT";
        }
        String normalized = outputType.trim().toUpperCase();
        if (!OUTPUT_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_DELIVERABLE_INVALID,
                    "未知交付方向：" + outputType + "（可选 " + String.join("/", OUTPUT_TYPES) + "）");
        }
        return normalized;
    }

    private static String key(String objectType, Long objectId) {
        return objectType + "#" + objectId;
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

    /** 交付物原始行（未解析对象信息）。 */
    private record RawOutput(Long id, Long taskId, String taskWbsCode, String taskName,
                             String outputType, String objectType, Long objectId,
                             boolean required, String remark) {
    }

    /** 对象信息（回查结果）。{@code deleted=true} 表示对象已被软删，判定为 MISSING。 */
    private record ObjectInfo(String code, String name, String lifecycleState, boolean deleted) {
    }
}
