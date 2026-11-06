package com.nexplm.platform.project.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.project.api.ProjectDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目（P7-1）：项目主档 + EPS 目录结构 + 项目成员。
 *
 * <p>关键规则：</p>
 * <ul>
 *   <li><b>状态只能走动作</b>：新建即 {@code DRAFT}，只能经 activate/pause/close 迁移，
 *       保存接口没有 status 字段，避免前端绕过校验直接改状态；</li>
 *   <li><b>删除只限草稿</b>：执行中/已结项项目不允许物理消失（软删除同理），保证历史可追溯；</li>
 *   <li><b>EPS 用物化路径</b>：{@code path = /父id/本id/}，查子树一条 {@code LIKE} 搞定，
 *       不必递归 CTE；节点下有子节点或项目时禁删，避免项目挂在悬空目录上；</li>
 *   <li><b>成员整表替换</b>：成员与项目是「一对多附加信息」，保存时先删后插，前端不做差异比对。</li>
 * </ul>
 */
@Service
public class ProjectService {

    /** 项目状态：草稿 / 执行中 / 暂停 / 已结项（终态）。 */
    private static final Set<String> STATUSES = Set.of("DRAFT", "ACTIVE", "PAUSED", "CLOSED");
    /** 项目类型：研发 / 订单 / 其它。 */
    private static final Set<String> TYPES = Set.of("RD", "ORDER", "OTHER");
    /** 成员角色：负责人 / 成员 / 只读。 */
    private static final Set<String> MEMBER_ROLES = Set.of("MANAGER", "MEMBER", "VIEWER");

    private final JdbcTemplate jdbcTemplate;

    public ProjectService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ================================================================ EPS

    /** EPS 树：一次查出全部节点后按 parent_id 组装（节点量级百级，不必递归 SQL）。 */
    public List<ProjectDtos.EpsNode> epsTree(Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                        SELECT e.id, e.parent_id, e.code, e.name, e.path, e.sort_no, e.remark,
                               (SELECT COUNT(1) FROM prj_project p
                                 WHERE p.company_id = e.company_id AND p.eps_id = e.id
                                   AND p.deleted = false) AS project_count
                          FROM prj_eps e
                         WHERE e.company_id = ? AND e.deleted = false
                         ORDER BY e.sort_no, e.code
                        """, companyId);
        Map<Long, List<Map<String, Object>>> byParent = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long parentId = longOrNull(row.get("parent_id"));
            byParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(row);
        }
        return buildEpsNodes(byParent, null);
    }

    private List<ProjectDtos.EpsNode> buildEpsNodes(Map<Long, List<Map<String, Object>>> byParent,
                                                    Long parentId) {
        List<Map<String, Object>> children = byParent.getOrDefault(parentId, List.of());
        List<ProjectDtos.EpsNode> nodes = new ArrayList<>(children.size());
        for (Map<String, Object> row : children) {
            Long id = longOrNull(row.get("id"));
            nodes.add(new ProjectDtos.EpsNode(id, longOrNull(row.get("parent_id")),
                    str(row.get("code")), str(row.get("name")), str(row.get("path")),
                    intOf(row.get("sort_no")), str(row.get("remark")), intOf(row.get("project_count")),
                    buildEpsNodes(byParent, id)));
        }
        return nodes;
    }

    /** 新建 EPS 节点：{@code path} 由父节点路径 + 自身 id 拼出（根节点从 {@code /} 起）。 */
    @Transactional
    public ProjectDtos.EpsNode createEps(ProjectDtos.EpsSaveReq req, Long companyId, Long userId) {
        String parentPath = "/";
        if (req.parentId() != null) {
            Map<String, Object> parent = requireEps(req.parentId(), companyId);
            parentPath = str(parent.get("path"));
        }
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO prj_eps (id, company_id, parent_id, code, name, path, sort_no, remark,
                                             created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, req.parentId(), req.code().trim(), req.name().trim(),
                parentPath + id + "/", req.sortNo() == null ? 10 : req.sortNo(),
                blankToNull(req.remark()), userId, userId);
        return epsNode(id, companyId);
    }

    /**
     * 修改 EPS 节点。
     *
     * <p>P7-1 不支持「移动节点」：移动要连带重写整棵子树的物化路径，属于结构高危操作，
     * 留到需要时再单开接口；这里 parent_id 保持不变。</p>
     */
    @Transactional
    public ProjectDtos.EpsNode updateEps(Long id, ProjectDtos.EpsSaveReq req, Long companyId, Long userId) {
        requireEps(id, companyId);
        jdbcTemplate.update("""
                        UPDATE prj_eps
                           SET code = ?, name = ?, sort_no = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.code().trim(), req.name().trim(), req.sortNo() == null ? 10 : req.sortNo(),
                blankToNull(req.remark()), userId, id, companyId);
        return epsNode(id, companyId);
    }

    /** 删除 EPS：有子节点或有项目挂着就拒删（409），避免项目挂到不存在的目录。 */
    @Transactional
    public void deleteEps(Long id, Long companyId) {
        requireEps(id, companyId);
        Integer children = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM prj_eps WHERE company_id = ? AND parent_id = ? AND deleted = false
                """, Integer.class, companyId, id);
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.PRJ_EPS_IN_USE, "该 EPS 下还有 " + children + " 个子节点，不能删除");
        }
        Integer projects = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM prj_project WHERE company_id = ? AND eps_id = ? AND deleted = false
                """, Integer.class, companyId, id);
        if (projects != null && projects > 0) {
            throw BizException.of(ErrorCode.PRJ_EPS_IN_USE, "该 EPS 下还有 " + projects + " 个项目，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE prj_eps SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    private ProjectDtos.EpsNode epsNode(Long id, Long companyId) {
        return epsTree(companyId).stream().flatMap(node -> flatten(node).stream())
                .filter(node -> node.id().equals(id))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.PRJ_EPS_NOT_FOUND, "EPS 节点不存在：" + id));
    }

    private List<ProjectDtos.EpsNode> flatten(ProjectDtos.EpsNode node) {
        List<ProjectDtos.EpsNode> all = new ArrayList<>();
        all.add(new ProjectDtos.EpsNode(node.id(), node.parentId(), node.code(), node.name(),
                node.path(), node.sortNo(), node.remark(), node.projectCount(), List.of()));
        node.children().forEach(child -> all.addAll(flatten(child)));
        return all;
    }

    private Map<String, Object> requireEps(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, parent_id, code, name, path FROM prj_eps
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_EPS_NOT_FOUND, "EPS 节点不存在：" + id);
        }
        return rows.get(0);
    }

    // ================================================================ 项目

    public PageResult<ProjectDtos.ProjectRow> list(Long companyId, String keyword, String status,
                                                   Long epsId, boolean includeSubtree,
                                                   int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        StringBuilder where = new StringBuilder(" WHERE p.deleted = false AND p.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            if (!STATUSES.contains(normalized)) {
                throw BizException.of(ErrorCode.PRJ_INVALID, "未知项目状态：" + status);
            }
            where.append(" AND p.status = ?");
            args.add(normalized);
        }
        if (epsId != null) {
            if (includeSubtree) {
                Map<String, Object> eps = requireEps(epsId, companyId);
                where.append("""
                         AND p.eps_id IN (SELECT id FROM prj_eps
                                           WHERE company_id = ? AND deleted = false AND path LIKE ?)""");
                args.add(companyId);
                args.add(str(eps.get("path")) + "%");
            } else {
                where.append(" AND p.eps_id = ?");
                args.add(epsId);
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.code ILIKE ? OR p.name ILIKE ? OR u.display_name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM prj_project p
                  LEFT JOIN sys_user u ON u.id = p.manager_id
                """ + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<ProjectDtos.ProjectRow> items = jdbcTemplate.query("""
                        SELECT p.*, e.name AS eps_name, u.display_name AS manager_name,
                               (SELECT COUNT(1) FROM prj_member m WHERE m.project_id = p.id) AS member_count
                          FROM prj_project p
                          LEFT JOIN prj_eps e ON e.id = p.eps_id
                          LEFT JOIN sys_user u ON u.id = p.manager_id
                        """ + where + " ORDER BY p.priority, p.updated_at DESC, p.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ProjectDtos.ProjectRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), (Long) rs.getObject("eps_id"), rs.getString("eps_name"),
                        (Long) rs.getObject("manager_id"), rs.getString("manager_name"),
                        rs.getString("type"), rs.getString("status"), rs.getInt("priority"),
                        rs.getObject("plan_start", LocalDate.class),
                        rs.getObject("plan_end", LocalDate.class),
                        rs.getObject("actual_start", LocalDate.class),
                        rs.getObject("actual_end", LocalDate.class),
                        rs.getInt("progress"), rs.getInt("member_count"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePage, safeSize);
    }

    public ProjectDtos.ProjectDetail detail(Long id, Long companyId) {
        List<ProjectDtos.ProjectDetail> rows = jdbcTemplate.query("""
                        SELECT p.*, e.name AS eps_name, u.display_name AS manager_name
                          FROM prj_project p
                          LEFT JOIN prj_eps e ON e.id = p.eps_id
                          LEFT JOIN sys_user u ON u.id = p.manager_id
                         WHERE p.id = ? AND p.company_id = ? AND p.deleted = false
                        """,
                (rs, rowNum) -> new ProjectDtos.ProjectDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), (Long) rs.getObject("eps_id"), rs.getString("eps_name"),
                        (Long) rs.getObject("manager_id"), rs.getString("manager_name"),
                        rs.getString("type"), rs.getString("status"), rs.getInt("priority"),
                        rs.getObject("plan_start", LocalDate.class),
                        rs.getObject("plan_end", LocalDate.class),
                        rs.getObject("actual_start", LocalDate.class),
                        rs.getObject("actual_end", LocalDate.class),
                        rs.getInt("progress"), rs.getString("remark"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class),
                        membersOf(id)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.PRJ_NOT_FOUND, "项目不存在：" + id);
        }
        return rows.get(0);
    }

    @Transactional
    public ProjectDtos.ProjectDetail create(ProjectDtos.ProjectSaveReq req, Long companyId, Long userId) {
        validateSave(req);
        if (req.epsId() != null) {
            requireEps(req.epsId(), companyId);
        }
        String code = nextCode("PrjProject", "PJ");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO prj_project (id, company_id, code, name, eps_id, manager_id, type,
                                                 status, priority, plan_start, plan_end, progress, remark,
                                                 created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), req.epsId(),
                req.managerId() == null ? userId : req.managerId(),
                normalizeType(req.type()), req.priority() == null ? 3 : req.priority(),
                req.planStart(), req.planEnd(), req.progress() == null ? 0 : req.progress(),
                blankToNull(req.remark()), userId, userId);
        replaceMembers(id, companyId, req.members());
        return detail(id, companyId);
    }

    @Transactional
    public ProjectDtos.ProjectDetail update(Long id, ProjectDtos.ProjectSaveReq req,
                                            Long companyId, Long userId) {
        ProjectDtos.ProjectDetail current = detail(id, companyId);
        requireNotClosed(current.status(), "已结项的项目不能再修改");
        validateSave(req);
        if (req.epsId() != null) {
            requireEps(req.epsId(), companyId);
        }
        jdbcTemplate.update("""
                        UPDATE prj_project
                           SET name = ?, eps_id = ?, manager_id = ?, type = ?, priority = ?,
                               plan_start = ?, plan_end = ?, progress = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), req.epsId(),
                req.managerId() == null ? current.managerId() : req.managerId(),
                normalizeType(req.type()), req.priority() == null ? current.priority() : req.priority(),
                req.planStart(), req.planEnd(),
                req.progress() == null ? current.progress() : req.progress(),
                blankToNull(req.remark()), userId, id, companyId);
        // members 为 null 表示「本次不动成员」（前端只改主档时不必回传全量成员）
        if (req.members() != null) {
            replaceMembers(id, companyId, req.members());
        }
        return detail(id, companyId);
    }

    /**
     * 删除：只允许草稿（执行中/已结项项目要留痕，走 close 结项而不是删除）。
     *
     * <p>连带清理：成员、任务依赖、WBS 任务（软删）——草稿项目本就是「还没成立」的临时数据，
     * 留一堆孤儿任务只会污染后续任务统计。</p>
     */
    @Transactional
    public void delete(Long id, Long companyId) {
        ProjectDtos.ProjectDetail current = detail(id, companyId);
        if (!"DRAFT".equals(current.status())) {
            throw BizException.of(ErrorCode.PRJ_STATE_INVALID,
                    "只有草稿状态的项目可以删除（当前状态：" + current.status() + "）");
        }
        jdbcTemplate.update("DELETE FROM prj_member WHERE project_id = ? AND company_id = ?", id, companyId);
        jdbcTemplate.update("DELETE FROM prj_task_dep WHERE project_id = ? AND company_id = ?", id, companyId);
        jdbcTemplate.update("""
                UPDATE prj_task SET deleted = true, updated_at = now()
                 WHERE project_id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        jdbcTemplate.update("""
                UPDATE prj_project SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /** 启动：DRAFT/PAUSED → ACTIVE，首次启动补实际开始日。 */
    @Transactional
    public ProjectDtos.ProjectDetail activate(Long id, Long companyId, Long userId) {
        ProjectDtos.ProjectDetail current = detail(id, companyId);
        if (!Set.of("DRAFT", "PAUSED").contains(current.status())) {
            throw BizException.of(ErrorCode.PRJ_STATE_INVALID,
                    "只有草稿或暂停的项目可以启动（当前状态：" + current.status() + "）");
        }
        jdbcTemplate.update("""
                        UPDATE prj_project
                           SET status = 'ACTIVE', actual_start = COALESCE(actual_start, CURRENT_DATE),
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 暂停：ACTIVE → PAUSED。 */
    @Transactional
    public ProjectDtos.ProjectDetail pause(Long id, Long companyId, Long userId) {
        ProjectDtos.ProjectDetail current = detail(id, companyId);
        if (!"ACTIVE".equals(current.status())) {
            throw BizException.of(ErrorCode.PRJ_STATE_INVALID,
                    "只有执行中的项目可以暂停（当前状态：" + current.status() + "）");
        }
        jdbcTemplate.update("""
                        UPDATE prj_project SET status = 'PAUSED', updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 结项：ACTIVE/PAUSED → CLOSED（终态），补实际结束日。 */
    @Transactional
    public ProjectDtos.ProjectDetail close(Long id, Long companyId, Long userId) {
        ProjectDtos.ProjectDetail current = detail(id, companyId);
        if (!Set.of("ACTIVE", "PAUSED").contains(current.status())) {
            throw BizException.of(ErrorCode.PRJ_STATE_INVALID,
                    "只有执行中或暂停的项目可以结项（当前状态：" + current.status() + "）");
        }
        jdbcTemplate.update("""
                        UPDATE prj_project
                           SET status = 'CLOSED', actual_end = COALESCE(actual_end, CURRENT_DATE),
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 成员整表替换（前端传全量）。 */
    @Transactional
    public ProjectDtos.ProjectDetail replaceMembers(Long id, List<ProjectDtos.MemberSaveReq> members,
                                                    Long companyId) {
        detail(id, companyId);
        replaceMembers(id, companyId, members);
        return detail(id, companyId);
    }

    // ================================================================ 内部

    private void replaceMembers(Long projectId, Long companyId, List<ProjectDtos.MemberSaveReq> members) {
        if (members == null) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (ProjectDtos.MemberSaveReq member : members) {
            if (!seen.add(member.userId())) {
                throw BizException.of(ErrorCode.PRJ_INVALID, "成员重复：" + member.userId());
            }
            if (member.role() != null && !member.role().isBlank()
                    && !MEMBER_ROLES.contains(member.role().trim().toUpperCase())) {
                throw BizException.of(ErrorCode.PRJ_INVALID,
                        "未知成员角色：" + member.role() + "（可选 " + String.join("/", MEMBER_ROLES) + "）");
            }
            Integer exists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM sys_user WHERE id = ? AND deleted = false
                    """, Integer.class, member.userId());
            if (exists == null || exists == 0) {
                throw BizException.of(ErrorCode.PRJ_INVALID, "用户不存在：" + member.userId());
            }
        }
        jdbcTemplate.update("DELETE FROM prj_member WHERE project_id = ? AND company_id = ?",
                projectId, companyId);
        for (ProjectDtos.MemberSaveReq member : members) {
            jdbcTemplate.update("""
                            INSERT INTO prj_member (id, company_id, project_id, user_id, role)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, projectId, member.userId(),
                    member.role() == null || member.role().isBlank()
                            ? "MEMBER" : member.role().trim().toUpperCase());
        }
    }

    private List<ProjectDtos.MemberRow> membersOf(Long projectId) {
        return jdbcTemplate.query("""
                        SELECT m.user_id, m.role, u.username, u.display_name
                          FROM prj_member m
                          LEFT JOIN sys_user u ON u.id = m.user_id
                         WHERE m.project_id = ?
                         ORDER BY m.role, m.id
                        """,
                (rs, rowNum) -> new ProjectDtos.MemberRow(rs.getLong("user_id"),
                        rs.getString("username"), rs.getString("display_name"), rs.getString("role")),
                projectId);
    }

    private void validateSave(ProjectDtos.ProjectSaveReq req) {
        if (req.planStart() != null && req.planEnd() != null && req.planEnd().isBefore(req.planStart())) {
            throw BizException.of(ErrorCode.PRJ_INVALID, "计划结束日不能早于计划开始日");
        }
        if (req.progress() != null && (req.progress() < 0 || req.progress() > 100)) {
            throw BizException.of(ErrorCode.PRJ_INVALID, "进度必须在 0~100 之间");
        }
    }

    private static void requireNotClosed(String status, String message) {
        if ("CLOSED".equals(status)) {
            throw BizException.of(ErrorCode.PRJ_STATE_INVALID, message + "（当前状态：" + status + "）");
        }
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "RD";
        }
        String normalized = type.trim().toUpperCase();
        if (!TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.PRJ_INVALID,
                    "未知项目类型：" + type + "（可选 " + String.join("/", TYPES) + "）");
        }
        return normalized;
    }

    private String nextCode(String ruleCode, String prefix) {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, ?, 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId(), ruleCode);
        return String.format("%s-%06d", prefix, no == null ? 1L : no);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Long longOrNull(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
