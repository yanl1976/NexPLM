package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappResourceDtos;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工艺资源库（P6 任务 3）：分类树 + 资源主数据（动态属性）+ 保存的查询条件 + 引用统计。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li><b>动态属性</b>放 {@code capp_resource.attrs}（jsonb 扁平 map），查询用 jsonb 包含匹配
 *       （{@code attrs @> '{"品牌":"XX"}'}），不为每种属性建列；</li>
 *   <li><b>分类树</b>扁平返回（带 {@code parentId}），前端组树，避免后端递归拼装与排序抖动；</li>
 *   <li><b>引用统计</b>口径 = {@code capp_routing_step.resource_id}，删除资源前必须无引用；</li>
 *   <li><b>删除分类</b>要求无子分类且无资源，避免留下孤儿主数据。</li>
 * </ul>
 */
@Service
public class CappResourceService {

    private static final Set<String> RESOURCE_STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final TypeReference<Map<String, String>> ATTRS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> CONDITION_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public CappResourceService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    // ------------------------------------------------------------------ 分类

    public List<CappResourceDtos.ClassRow> classList(Long companyId) {
        return jdbcTemplate.query("""
                        SELECT c.id, c.parent_id, c.code, c.name, c.sort_no, c.remark,
                               (SELECT COUNT(1) FROM capp_resource r
                                 WHERE r.class_id = c.id AND r.deleted = false) AS resource_count
                          FROM capp_resource_class c
                         WHERE c.company_id = ? AND c.deleted = false
                         ORDER BY c.sort_no, c.code
                        """,
                (rs, rowNum) -> new CappResourceDtos.ClassRow(rs.getLong("id"),
                        (Long) rs.getObject("parent_id"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("sort_no"), rs.getString("remark"), rs.getInt("resource_count")),
                companyId);
    }

    @Transactional
    public CappResourceDtos.ClassRow createClass(CappResourceDtos.ClassSaveReq req, Long companyId) {
        String code = req.code().trim();
        if (req.parentId() != null) {
            requireClass(req.parentId(), companyId);
        }
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO capp_resource_class (id, company_id, parent_id, code, name, sort_no, remark)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    id, companyId, req.parentId(), code, req.name().trim(),
                    req.sortNo() == null ? 0 : req.sortNo(), blankToNull(req.remark()));
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "资源分类编码已存在：" + code);
        }
        return classRow(id, companyId);
    }

    @Transactional
    public CappResourceDtos.ClassRow updateClass(Long id, CappResourceDtos.ClassSaveReq req, Long companyId) {
        CappResourceDtos.ClassRow current = classRow(id, companyId);
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "分类不能挂到自己下面");
            }
            requireClass(req.parentId(), companyId);
            requireNotDescendant(id, req.parentId(), companyId);
        }
        jdbcTemplate.update("""
                        UPDATE capp_resource_class
                           SET code = ?, name = ?, parent_id = ?, sort_no = ?, remark = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.code().trim(), req.name().trim(), req.parentId(),
                req.sortNo() == null ? current.sortNo() : req.sortNo(), blankToNull(req.remark()),
                id, companyId);
        return classRow(id, companyId);
    }

    @Transactional
    public void deleteClass(Long id, Long companyId) {
        classRow(id, companyId);
        Integer children = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_resource_class
                 WHERE parent_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该分类下还有子分类，不能删除");
        }
        Integer resources = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_resource
                 WHERE class_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        if (resources != null && resources > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该分类下还有 " + resources + " 个资源，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE capp_resource_class SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ------------------------------------------------------------------ 资源

    /** 列表入口：{@code attrsJson} 为前端传来的动态属性过滤（{@code {"品牌":"XX"}}）。 */
    public PageResult<CappResourceDtos.ResourceRow> listResources(Long companyId, Long classId, String keyword,
                                                                 String status, String attrsJson,
                                                                 int page, int size) {
        return listResources(companyId, classId, keyword, status, parseAttrs(attrsJson), page, size);
    }

    public PageResult<CappResourceDtos.ResourceRow> listResources(Long companyId, Long classId, String keyword,
                                                                 String status, Map<String, String> attrs,
                                                                 int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        StringBuilder where = new StringBuilder(" WHERE r.deleted = false AND r.company_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (classId != null) {
            where.append(" AND r.class_id = ?");
            args.add(classId);
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            if (!RESOURCE_STATUSES.contains(normalized)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "未知资源状态：" + status);
            }
            where.append(" AND r.status = ?");
            args.add(normalized);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (r.code ILIKE ? OR r.name ILIKE ? OR r.spec ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (attrs != null && !attrs.isEmpty()) {
            where.append(" AND r.attrs @> CAST(? AS jsonb)");
            args.add(toJson(attrs));
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM capp_resource r" + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<CappResourceDtos.ResourceRow> items = jdbcTemplate.query("""
                        SELECT r.id, r.class_id, c.code AS class_code, c.name AS class_name,
                               r.code, r.name, r.spec, r.unit, r.status, r.attrs::text AS attrs_text,
                               r.remark, r.updated_at,
                               (SELECT COUNT(1) FROM capp_routing_step s WHERE s.resource_id = r.id)
                                   AS reference_count
                          FROM capp_resource r
                          LEFT JOIN capp_resource_class c ON c.id = r.class_id
                        """ + where + " ORDER BY r.code LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapResource(rs), pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePage, safeSize);
    }

    public CappResourceDtos.ResourceRow resource(Long id, Long companyId) {
        List<CappResourceDtos.ResourceRow> rows = jdbcTemplate.query("""
                        SELECT r.id, r.class_id, c.code AS class_code, c.name AS class_name,
                               r.code, r.name, r.spec, r.unit, r.status, r.attrs::text AS attrs_text,
                               r.remark, r.updated_at,
                               (SELECT COUNT(1) FROM capp_routing_step s WHERE s.resource_id = r.id)
                                   AS reference_count
                          FROM capp_resource r
                          LEFT JOIN capp_resource_class c ON c.id = r.class_id
                         WHERE r.id = ? AND r.company_id = ? AND r.deleted = false
                        """,
                (rs, rowNum) -> mapResource(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "工艺资源不存在：" + id);
        }
        return rows.get(0);
    }

    @Transactional
    public CappResourceDtos.ResourceRow createResource(CappResourceDtos.ResourceSaveReq req, Long companyId,
                                                       Long userId) {
        requireClass(req.classId(), companyId);
        String status = normalizeStatus(req.status());
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO capp_resource (id, company_id, class_id, code, name, spec, unit,
                                                       status, attrs, remark, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)
                            """,
                    id, companyId, req.classId(), req.code().trim(), req.name().trim(),
                    blankToNull(req.spec()), blankToNull(req.unit()), status,
                    toJson(req.attrs() == null ? Map.of() : req.attrs()),
                    blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "资源编码已存在：" + req.code());
        }
        return resource(id, companyId);
    }

    @Transactional
    public CappResourceDtos.ResourceRow updateResource(Long id, CappResourceDtos.ResourceSaveReq req,
                                                       Long companyId, Long userId) {
        resource(id, companyId);
        requireClass(req.classId(), companyId);
        jdbcTemplate.update("""
                        UPDATE capp_resource
                           SET class_id = ?, code = ?, name = ?, spec = ?, unit = ?, status = ?,
                               attrs = CAST(? AS jsonb), remark = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.classId(), req.code().trim(), req.name().trim(), blankToNull(req.spec()),
                blankToNull(req.unit()), normalizeStatus(req.status()),
                toJson(req.attrs() == null ? Map.of() : req.attrs()),
                blankToNull(req.remark()), userId, id, companyId);
        return resource(id, companyId);
    }

    @Transactional
    public void deleteResource(Long id, Long companyId) {
        CappResourceDtos.ResourceRow current = resource(id, companyId);
        if (current.referenceCount() > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT,
                    "该资源已被 " + current.referenceCount() + " 道工序引用，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE capp_resource SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /** 引用统计：该资源被哪些工艺路线的哪道工序使用。 */
    public List<CappResourceDtos.ResourceReferenceRow> references(Long id, Long companyId) {
        resource(id, companyId);
        return jdbcTemplate.query("""
                        SELECT rt.id AS routing_id, rt.code AS routing_code, rt.name AS routing_name,
                               rt.object_id, o.code AS object_code, s.seq_no, s.name AS step_name
                          FROM capp_routing_step s
                          JOIN capp_routing rt ON rt.id = s.routing_id AND rt.deleted = false
                          LEFT JOIN plm_object o ON o.id = rt.object_id
                         WHERE s.resource_id = ? AND s.company_id = ?
                         ORDER BY rt.code, s.seq_no
                        """,
                (rs, rowNum) -> new CappResourceDtos.ResourceReferenceRow(rs.getLong("routing_id"),
                        rs.getString("routing_code"), rs.getString("routing_name"),
                        rs.getLong("object_id"), rs.getString("object_code"),
                        rs.getInt("seq_no"), rs.getString("step_name")),
                id, companyId);
    }

    // ------------------------------------------------------------------ 保存的查询条件

    public List<CappResourceDtos.QueryRow> queries(Long companyId) {
        return jdbcTemplate.query("""
                        SELECT id, name, class_id, condition_json::text AS condition_text, created_at
                          FROM capp_resource_query
                         WHERE company_id = ? AND deleted = false
                         ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> {
                    Map<String, Object> condition = parseCondition(rs.getString("condition_text"));
                    String keyword = condition.get("keyword") == null
                            ? "" : String.valueOf(condition.get("keyword"));
                    return new CappResourceDtos.QueryRow(rs.getLong("id"), rs.getString("name"),
                            (Long) rs.getObject("class_id"), keyword,
                            toStringMap(condition.get("attrs")),
                            rs.getObject("created_at", OffsetDateTime.class));
                },
                companyId);
    }

    /**
     * 保存查询条件：同名覆盖（{@code uk(company_id, name)}），避免用户反复保存出重名条目。
     */
    @Transactional
    public CappResourceDtos.QueryRow saveQuery(CappResourceDtos.QuerySaveReq req, Long companyId, Long userId) {
        if (req.classId() != null) {
            requireClass(req.classId(), companyId);
        }
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("keyword", req.keyword() == null ? "" : req.keyword().trim());
        condition.put("attrs", req.attrs() == null ? Map.of() : req.attrs());
        String conditionJson = toJson(condition);
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_resource_query (id, company_id, name, class_id, condition_json, created_by)
                        VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?)
                        ON CONFLICT (company_id, name) WHERE deleted = false
                        DO UPDATE SET class_id = EXCLUDED.class_id,
                                      condition_json = EXCLUDED.condition_json,
                                      updated_at = now()
                        """,
                id, companyId, req.name().trim(), req.classId(), conditionJson, userId);
        return queries(companyId).stream()
                .filter(row -> row.name().equals(req.name().trim()))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_INTERNAL, "查询条件保存失败"));
    }

    @Transactional
    public void deleteQuery(Long id, Long companyId) {
        int updated = jdbcTemplate.update("""
                UPDATE capp_resource_query SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (updated == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "查询条件不存在：" + id);
        }
    }

    /** 按保存的条件执行查询。 */
    public PageResult<CappResourceDtos.ResourceRow> runQuery(Long id, Long companyId, int page, int size) {
        CappResourceDtos.QueryRow query = queries(companyId).stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_NOT_FOUND, "查询条件不存在：" + id));
        return listResources(companyId, query.classId(), query.keyword(), null, query.attrs(), page, size);
    }

    // ------------------------------------------------------------------ 内部

    private CappResourceDtos.ClassRow classRow(Long id, Long companyId) {
        List<CappResourceDtos.ClassRow> rows = jdbcTemplate.query("""
                        SELECT c.id, c.parent_id, c.code, c.name, c.sort_no, c.remark,
                               (SELECT COUNT(1) FROM capp_resource r
                                 WHERE r.class_id = c.id AND r.deleted = false) AS resource_count
                          FROM capp_resource_class c
                         WHERE c.id = ? AND c.company_id = ? AND c.deleted = false
                        """,
                (rs, rowNum) -> new CappResourceDtos.ClassRow(rs.getLong("id"),
                        (Long) rs.getObject("parent_id"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("sort_no"), rs.getString("remark"), rs.getInt("resource_count")),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "资源分类不存在：" + id);
        }
        return rows.get(0);
    }

    private void requireClass(Long id, Long companyId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_resource_class
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "资源分类不存在：" + id);
        }
    }

    /** 防止把父分类挂到自己的子孙下面（会成环）。 */
    private void requireNotDescendant(Long id, Long newParentId, Long companyId) {
        Long cursor = newParentId;
        int guard = 0;
        while (cursor != null && guard++ < 50) {
            if (cursor.equals(id)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能把分类挂到自己的子分类下");
            }
            List<Long> parents = jdbcTemplate.query("""
                            SELECT parent_id FROM capp_resource_class
                             WHERE id = ? AND company_id = ?
                            """,
                    (rs, rowNum) -> (Long) rs.getObject("parent_id"), cursor, companyId);
            cursor = parents.isEmpty() ? null : parents.get(0);
        }
    }

    private CappResourceDtos.ResourceRow mapResource(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new CappResourceDtos.ResourceRow(rs.getLong("id"), rs.getLong("class_id"),
                rs.getString("class_code"), rs.getString("class_name"), rs.getString("code"),
                rs.getString("name"), rs.getString("spec"), rs.getString("unit"), rs.getString("status"),
                parseAttrs(rs.getString("attrs_text")), rs.getString("remark"),
                rs.getInt("reference_count"),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private Map<String, String> parseAttrs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = jsonMapper.readValue(json, ATTRS_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Map<String, Object> parseCondition(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = jsonMapper.readValue(json, CONDITION_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private static Map<String, String> toStringMap(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item == null ? "" : String.valueOf(item)));
        return result;
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "动态属性格式不合法");
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!RESOURCE_STATUSES.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "未知资源状态：" + status + "（可选 ACTIVE/DISABLED）");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
