package com.nexplm.platform.project.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.project.api.ProjectDtos;
import com.nexplm.platform.project.api.ProjectTemplateDtos;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目模板与「从模板立项」（P7-6）。
 *
 * <p>三个关键口径：</p>
 * <ul>
 *   <li><b>模板任务用相对偏移</b>：存「相对开工日第几天 + 工期」，立项时才换算成真实日期。
 *       模板里存绝对日期，换个月立项就全错；</li>
 *   <li><b>编码按数组顺序生成</b>：前端只传 {@code parentWbsCode}，服务端算出 1 / 1.2 / 1.2.3
 *       并落库（与 WBS 的 resequence 同思路），避免手填编码撞唯一键；</li>
 *   <li><b>立项是快照</b>：模板改了不影响已立项项目——生成时任务/目录都已落成实体，
 *       模板与项目之间不保留引用关系。</li>
 * </ul>
 */
@Service
public class ProjectTemplateService {

    private static final Set<String> TYPES = Set.of("RD", "ORDER", "OTHER");
    private static final Set<String> TASK_TYPES = Set.of("TASK", "MILESTONE");

    private final JdbcTemplate jdbcTemplate;
    private final ProjectService projectService;
    private final CodeRuleService codeRuleService;

    public ProjectTemplateService(JdbcTemplate jdbcTemplate, ProjectService projectService,
                                  CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectService = projectService;
        this.codeRuleService = codeRuleService;
    }

    // ---------------------------------------------------------------- 查询

    public PageResult<ProjectTemplateDtos.TemplateRow> list(Long companyId, String keyword,
                                                            Boolean enabled, Integer page,
                                                            Integer size) {
        StringBuilder where = new StringBuilder(" WHERE t.company_id = ? AND t.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (t.code ILIKE ? OR t.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (enabled != null) {
            where.append(" AND t.enabled = ?");
            args.add(enabled);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM prj_template t" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<ProjectTemplateDtos.TemplateRow> items = jdbcTemplate.query("""
                        SELECT t.*,
                               (SELECT COUNT(1) FROM prj_template_task x
                                 WHERE x.template_id = t.id AND x.deleted = false) AS task_count,
                               (SELECT COUNT(1) FROM prj_template_folder f
                                 WHERE f.template_id = t.id AND f.deleted = false) AS folder_count
                          FROM prj_template t
                        """ + where + " ORDER BY t.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ProjectTemplateDtos.TemplateRow(rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("type"),
                        rs.getBoolean("enabled"), rs.getString("remark"),
                        rs.getInt("task_count"), rs.getInt("folder_count"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public ProjectTemplateDtos.TemplateDetail detail(Long id, Long companyId) {
        Map<String, Object> row = requireTemplate(id, companyId);
        List<ProjectTemplateDtos.TemplateTaskRow> tasks = jdbcTemplate.query("""
                        SELECT id, parent_id, wbs_code, name, type, start_offset, duration_days,
                               sort_no, remark
                          FROM prj_template_task
                         WHERE template_id = ? AND deleted = false
                         ORDER BY sort_no, wbs_code
                        """,
                (rs, rowNum) -> new ProjectTemplateDtos.TemplateTaskRow(rs.getLong("id"),
                        (Long) rs.getObject("parent_id"), rs.getString("wbs_code"),
                        rs.getString("name"), rs.getString("type"), rs.getInt("start_offset"),
                        rs.getInt("duration_days"), rs.getInt("sort_no"), rs.getString("remark")),
                id);
        List<ProjectTemplateDtos.TemplateFolderRow> folders = jdbcTemplate.query("""
                        SELECT id, parent_path, name, sort_no
                          FROM prj_template_folder
                         WHERE template_id = ? AND deleted = false
                         ORDER BY parent_path, sort_no, id
                        """,
                (rs, rowNum) -> new ProjectTemplateDtos.TemplateFolderRow(rs.getLong("id"),
                        rs.getString("parent_path"), rs.getString("name"), rs.getInt("sort_no")),
                id);
        return new ProjectTemplateDtos.TemplateDetail(id, str(row.get("code")),
                str(row.get("name")), str(row.get("type")), Boolean.TRUE.equals(row.get("enabled")),
                str(row.get("remark")), tasks, folders);
    }

    // ---------------------------------------------------------------- 模板写

    @Transactional
    public ProjectTemplateDtos.TemplateDetail create(ProjectTemplateDtos.TemplateSaveReq req,
                                                     Long companyId, Long userId) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO prj_template (id, company_id, code, name, type, remark, enabled,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, nextCode(companyId), req.name().trim(), normalizeType(req.type()),
                blankToNull(req.remark()), req.enabled() == null || req.enabled(), userId, userId);
        return detail(id, companyId);
    }

    @Transactional
    public ProjectTemplateDtos.TemplateDetail update(Long id,
                                                     ProjectTemplateDtos.TemplateSaveReq req,
                                                     Long companyId, Long userId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("""
                        UPDATE prj_template
                           SET name = ?, type = ?, enabled = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeType(req.type()),
                req.enabled() == null || req.enabled(), blankToNull(req.remark()),
                userId, id, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("""
                UPDATE prj_template SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        // 模板明细物理删：软删会撞 (template_id, wbs_code) 的唯一索引，且模板一旦删掉就没人再读
        jdbcTemplate.update("DELETE FROM prj_template_task WHERE template_id = ?", id);
        jdbcTemplate.update("DELETE FROM prj_template_folder WHERE template_id = ?", id);
    }

    /** 任务整表替换：编码按数组顺序生成，{@code parentWbsCode} 必须指向本次靠前出现的任务。 */
    @Transactional
    public ProjectTemplateDtos.TemplateDetail replaceTasks(Long id,
                                                           List<ProjectTemplateDtos.TemplateTaskSaveReq> tasks,
                                                           Long companyId, Long userId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("DELETE FROM prj_template_task WHERE template_id = ?", id);
        Map<String, Integer> counters = new HashMap<>();
        Map<String, Long> idByCode = new HashMap<>();
        List<ProjectTemplateDtos.TemplateTaskSaveReq> rows = tasks == null ? List.of() : tasks;
        int index = 0;
        for (ProjectTemplateDtos.TemplateTaskSaveReq row : rows) {
            String parentCode = row.parentWbsCode() == null || row.parentWbsCode().isBlank()
                    ? null : row.parentWbsCode().trim();
            if (parentCode != null && !idByCode.containsKey(parentCode)) {
                throw BizException.of(ErrorCode.PRJ_TEMPLATE_INVALID,
                        "父任务编码不存在或顺序不对（父任务必须在数组里更靠前）：" + parentCode);
            }
            int seq = counters.merge(parentCode == null ? "" : parentCode, 1, Integer::sum);
            String code = parentCode == null ? String.valueOf(seq) : parentCode + "." + seq;
            Long parentId = parentCode == null ? null : idByCode.get(parentCode);
            long taskId = IdWorker.getId();
            jdbcTemplate.update("""
                            INSERT INTO prj_template_task (id, company_id, template_id, parent_id,
                                                           wbs_code, name, type, start_offset,
                                                           duration_days, sort_no, remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    taskId, companyId, id, parentId, code, row.name().trim(),
                    normalizeTaskType(row.type()),
                    row.startOffset() == null ? 0 : Math.max(row.startOffset(), 0),
                    row.durationDays() == null ? 1 : Math.max(row.durationDays(), 1),
                    (index + 1) * 10, blankToNull(row.remark()));
            idByCode.put(code, taskId);
            index++;
        }
        jdbcTemplate.update("UPDATE prj_template SET updated_by = ?, updated_at = now() WHERE id = ?",
                userId, id);
        return detail(id, companyId);
    }

    /** 目录整表替换：父路径必须是 {@code /} 或本次已声明的某个目录路径。 */
    @Transactional
    public ProjectTemplateDtos.TemplateDetail replaceFolders(
            Long id, List<ProjectTemplateDtos.TemplateFolderSaveReq> folders, Long companyId,
            Long userId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("DELETE FROM prj_template_folder WHERE template_id = ?", id);
        List<ProjectTemplateDtos.TemplateFolderSaveReq> rows = folders == null ? List.of() : folders;
        List<String> paths = new ArrayList<>();
        paths.add("/");
        int index = 0;
        for (ProjectTemplateDtos.TemplateFolderSaveReq row : rows) {
            String parentPath = row.parentPath() == null || row.parentPath().isBlank()
                    ? "/" : normalizePath(row.parentPath());
            if (!paths.contains(parentPath)) {
                throw BizException.of(ErrorCode.PRJ_TEMPLATE_INVALID,
                        "父目录不存在或顺序不对（父目录必须在数组里更靠前）：" + parentPath);
            }
            jdbcTemplate.update("""
                            INSERT INTO prj_template_folder (id, company_id, template_id, parent_path,
                                                             name, sort_no)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, id, parentPath, row.name().trim(),
                    row.sortNo() == null ? (index + 1) * 10 : row.sortNo());
            paths.add(childPath(parentPath, row.name().trim()));
            index++;
        }
        jdbcTemplate.update("UPDATE prj_template SET updated_by = ?, updated_at = now() WHERE id = ?",
                userId, id);
        return detail(id, companyId);
    }

    // ---------------------------------------------------------------- 立项

    /** 从模板立项：建项目 → 复制任务（偏移换真实日期）→ 复制目录（逻辑路径换物化路径）。 */
    @Transactional
    public ProjectDtos.ProjectDetail instantiate(ProjectTemplateDtos.FromTemplateReq req,
                                                 Long companyId, Long userId) {
        ProjectTemplateDtos.TemplateDetail template = detail(req.templateId(), companyId);
        if (!template.enabled()) {
            throw BizException.of(ErrorCode.PRJ_TEMPLATE_INVALID,
                    "模板已停用，不能用于立项：" + template.code());
        }
        LocalDate planEnd = null;
        for (ProjectTemplateDtos.TemplateTaskRow task : template.tasks()) {
            LocalDate end = req.planStart()
                    .plusDays(task.startOffset() + Math.max(task.durationDays(), 1) - 1L);
            if (planEnd == null || end.isAfter(planEnd)) {
                planEnd = end;
            }
        }
        ProjectDtos.ProjectDetail project = projectService.create(new ProjectDtos.ProjectSaveReq(
                req.name().trim(), req.epsId(), req.managerId(), template.type(), req.priority(),
                req.planStart(), planEnd, 0, blankToNull(req.remark()), null), companyId, userId);

        Map<Long, Long> taskIdMap = new HashMap<>();
        for (ProjectTemplateDtos.TemplateTaskRow task : template.tasks()) {
            Long parentId = task.parentId() == null ? null : taskIdMap.get(task.parentId());
            LocalDate start = req.planStart().plusDays(task.startOffset());
            LocalDate end = start.plusDays(Math.max(task.durationDays(), 1) - 1L);
            long newId = IdWorker.getId();
            jdbcTemplate.update("""
                            INSERT INTO prj_task (id, company_id, project_id, parent_id, wbs_code,
                                                  name, type, status, sort_no, plan_start, plan_end,
                                                  duration_days, progress, owner_id, remark,
                                                  created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'NOT_STARTED', ?, ?, ?, ?, 0, ?, ?, ?, ?)
                            """,
                    newId, companyId, project.id(), parentId, task.wbsCode(), task.name(),
                    task.type(), task.sortNo(), start, end,
                    Math.max(task.durationDays(), 1),
                    req.managerId() == null ? userId : req.managerId(),
                    task.remark(), userId, userId);
            taskIdMap.put(task.id(), newId);
        }

        // 目录：按父路径深度排序（浅的先建），逻辑路径 → 新 id + 物化路径
        List<ProjectTemplateDtos.TemplateFolderRow> folders = new ArrayList<>(template.folders());
        folders.sort(Comparator.comparingInt((ProjectTemplateDtos.TemplateFolderRow row) ->
                depthOf(row.parentPath())).thenComparingInt(ProjectTemplateDtos.TemplateFolderRow::sortNo));
        Map<String, Long> folderIdByPath = new HashMap<>();
        Map<String, String> materializedByPath = new HashMap<>();
        folderIdByPath.put("/", null);
        materializedByPath.put("/", "/");
        for (ProjectTemplateDtos.TemplateFolderRow folder : folders) {
            String parentPath = normalizePath(folder.parentPath());
            Long parentId = folderIdByPath.get(parentPath);
            String parentMaterialized = materializedByPath.getOrDefault(parentPath, "/");
            long newId = IdWorker.getId();
            String path = parentMaterialized + newId + "/";
            jdbcTemplate.update("""
                            INSERT INTO prj_project_folder (id, company_id, project_id, parent_id,
                                                            path, name, sort_no, created_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    newId, companyId, project.id(), parentId, path, folder.name(),
                    folder.sortNo(), userId);
            String logical = childPath(parentPath, folder.name());
            folderIdByPath.put(logical, newId);
            materializedByPath.put(logical, path);
        }
        return project;
    }

    // ---------------------------------------------------------------- 内部

    private Map<String, Object> requireTemplate(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name, type, enabled, remark FROM prj_template
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_TEMPLATE_NOT_FOUND, "项目模板不存在：" + id);
        }
        return rows.get(0);
    }

    /** 取号统一走 code_rule（台账 B19）；{@code companyId} 仅为兼容调用签名。 */
    private String nextCode(Long companyId) {
        return codeRuleService.next("PrjTemplate");
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "RD";
        }
        String normalized = type.trim().toUpperCase();
        if (!TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_TEMPLATE_INVALID,
                    "未知项目类型：" + type + "（可选 " + String.join("/", TYPES) + "）");
        }
        return normalized;
    }

    private static String normalizeTaskType(String type) {
        if (type == null || type.isBlank()) {
            return "TASK";
        }
        String normalized = type.trim().toUpperCase();
        if (!TASK_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_TEMPLATE_INVALID,
                    "未知任务类型：" + type + "（可选 " + String.join("/", TASK_TYPES) + "）");
        }
        return normalized;
    }

    /** 逻辑路径规范：以 / 开头、以 / 结尾（根为 /）。 */
    static String normalizePath(String path) {
        String value = path == null || path.isBlank() ? "/" : path.trim().replace('\\', '/');
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (!value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    static String childPath(String parentPath, String name) {
        return normalizePath(parentPath) + name.trim() + "/";
    }

    private static int depthOf(String path) {
        return normalizePath(path).split("/").length;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
