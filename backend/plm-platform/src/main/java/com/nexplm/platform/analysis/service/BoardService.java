package com.nexplm.platform.analysis.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.analysis.api.BoardDtos;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * P10-2 自定义看板：看板定义（布局 jsonb）+ 数据集白名单 + 预览执行。
 *
 * <p><b>安全边界</b>：数据集是编译期注册的（{@link #DATASETS}），每个数据集一条固定 SQL、
 * 只接受"公司 ID + 取前 N 条"两个绑定参数，用户输入永远不进 SQL 文本。
 * 这是本功能最关键的设计取舍——若允许用户填 SQL，"能编辑看板"就等价于"能跨公司读全库"
 * 外加"能跑慢查询拖垮生产库"。</p>
 *
 * <p>布局存 jsonb 而非组件子表：看板是整体编辑对象（拖拽排序后整块保存），
 * 组件没有独立查询需求；拆表只会引入排序冲突。</p>
 */
@Service
public class BoardService {

    private static final int MAX_COMPONENTS = 20;
    private static final int MAX_TOP = 100;
    private static final Set<String> COMPONENT_TYPES = Set.of("metric", "bar", "table");
    /** 组件 id 允许的最长长度（前端生成 uuid 或后端补）。 */
    private static final int MAX_COMPONENT_ID = 64;

    /** 数据集定义：元信息 + 固定 SQL（只绑 companyId，可选 LIMIT）。 */
    private record DatasetDef(BoardDtos.DatasetMeta meta, String sql) {
    }

    private static final Map<String, DatasetDef> DATASETS = new LinkedHashMap<>();

    static {
        register("doc.byState", "文档按状态", "doc", """
                SELECT o.lifecycle_state AS k, COUNT(1) AS c
                  FROM plm_object o
                 WHERE o.company_id = ? AND o.object_type = 'PdmDoc'
                   AND o.is_latest = true AND o.deleted = false
                 GROUP BY 1 ORDER BY c DESC
                """);
        register("doc.byType", "文档按类型", "doc", """
                SELECT d.doc_type AS k, COUNT(1) AS c
                  FROM plm_object o JOIN doc_master d ON d.object_id = o.id
                 WHERE o.company_id = ? AND o.object_type = 'PdmDoc'
                   AND o.is_latest = true AND o.deleted = false
                 GROUP BY 1 ORDER BY c DESC
                """);
        register("item.byType", "零部件按类型", "item", """
                SELECT m.item_type AS k, COUNT(1) AS c
                  FROM plm_object o JOIN item_master m ON m.object_id = o.id
                 WHERE o.company_id = ? AND o.object_type = 'PdmItem'
                   AND o.is_latest = true AND o.deleted = false
                 GROUP BY 1 ORDER BY c DESC
                """);
        register("item.bySupply", "零部件按供应方式", "item", """
                SELECT m.source_type AS k, COUNT(1) AS c
                  FROM plm_object o JOIN item_master m ON m.object_id = o.id
                 WHERE o.company_id = ? AND o.object_type = 'PdmItem'
                   AND o.is_latest = true AND o.deleted = false
                 GROUP BY 1 ORDER BY c DESC
                """);
        register("change.byStatus", "变更按状态", "change",
                groupBy("chg_ecr", "status"));
        register("change.byType", "变更按类型", "change",
                groupBy("chg_ecr", "change_type"));
        register("change.byPriority", "变更按优先级", "change",
                groupBy("chg_ecr", "priority"));
        register("capp.byStatus", "工艺路线按状态", "capp",
                groupBy("capp_routing", "status"));
        register("task.byStatus", "任务按状态", "task",
                groupBy("prj_task", "status"));
        register("quality.byStatus", "质量问题按状态", "quality",
                groupBy("qlt_issue", "status"));
        register("quality.bySeverity", "质量问题按严重度", "quality",
                groupBy("qlt_issue", "severity"));
        register("quality.bySource", "质量问题按来源", "quality",
                groupBy("qlt_issue", "source"));
    }

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public BoardService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    /* ============================================================ 数据集字典 */

    public List<BoardDtos.DatasetMeta> datasets() {
        return DATASETS.values().stream().map(DatasetDef::meta).toList();
    }

    /* ============================================================ 看板维护 */

    public PageResult<BoardDtos.BoardRow> page(Long companyId, String keyword, Integer page,
                                               Integer size) {
        StringBuilder where = new StringBuilder(" WHERE company_id = ? AND deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ana_board" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((long) (pageNo - 1) * pageSize);
        List<BoardDtos.BoardRow> items = jdbcTemplate.query("""
                        SELECT id, code, name, description, shared, layout::text AS layout_text,
                               updated_at
                          FROM ana_board
                        """ + where + " ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new BoardDtos.BoardRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("description"), rs.getBoolean("shared"),
                        readComponents(rs.getString("layout_text")).size(),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public BoardDtos.BoardDetail detail(Long id, Long companyId) {
        return toDetail(requireBoard(id, companyId));
    }

    public BoardDtos.BoardDetail create(BoardDtos.BoardSaveReq req, Long companyId, Long userId) {
        List<BoardDtos.Component> components = validateComponents(req.components());
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO ana_board (id, company_id, code, name, description, layout,
                                                   shared, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?)
                            """,
                    id, companyId, req.code().trim(), req.name().trim(),
                    blankToNull(req.description()), toJson(components),
                    req.shared() != null && req.shared(), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ANA_BOARD_DUPLICATED,
                    "看板编码已存在：" + req.code());
        }
        return detail(id, companyId);
    }

    public BoardDtos.BoardDetail update(Long id, BoardDtos.BoardSaveReq req, Long companyId,
                                        Long userId) {
        requireBoard(id, companyId);
        List<BoardDtos.Component> components = validateComponents(req.components());
        try {
            jdbcTemplate.update("""
                            UPDATE ana_board
                               SET code = ?, name = ?, description = ?, layout = CAST(? AS jsonb),
                                   shared = ?, updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ? AND deleted = false
                            """,
                    req.code().trim(), req.name().trim(), blankToNull(req.description()),
                    toJson(components), req.shared() != null && req.shared(), userId, id, companyId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ANA_BOARD_DUPLICATED,
                    "看板编码已存在：" + req.code());
        }
        return detail(id, companyId);
    }

    public void delete(Long id, Long companyId, Long userId) {
        requireBoard(id, companyId);
        jdbcTemplate.update("""
                UPDATE ana_board SET deleted = true, updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
    }

    /* ============================================================ 预览数据 */

    public BoardDtos.BoardData data(Long id, Long companyId) {
        Map<String, Object> board = requireBoard(id, companyId);
        List<BoardDtos.Component> components = readComponents(String.valueOf(board.get("layout_text")));
        List<BoardDtos.ComponentData> data = new ArrayList<>();
        for (BoardDtos.Component component : components) {
            DatasetDef def = DATASETS.get(component.dataset());
            if (def == null) {
                // 数据集被下线（代码里删掉）时不让整页崩，如实标空
                data.add(new BoardDtos.ComponentData(component.id(), component.type(),
                        component.title(), component.dataset(), null, 0, List.of()));
                continue;
            }
            int top = component.top() == null ? 10 : Math.min(Math.max(component.top(), 1), MAX_TOP);
            List<BoardDtos.CountRow> rows = jdbcTemplate.query(
                    def.sql() + " LIMIT ?",
                    (rs, rowNum) -> new BoardDtos.CountRow(rs.getString("k"), rs.getInt("c")),
                    companyId, top);
            long total = rows.stream().mapToLong(BoardDtos.CountRow::count).sum();
            data.add(new BoardDtos.ComponentData(component.id(), component.type(),
                    component.title(), component.dataset(), def.meta().domain(), total, rows));
        }
        return new BoardDtos.BoardData(id, String.valueOf(board.get("name")), data);
    }

    /* ============================================================ 内部 */

    private Map<String, Object> requireBoard(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                        SELECT id, code, name, description, shared, layout::text AS layout_text,
                               created_at, updated_at
                          FROM ana_board
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ANA_BOARD_NOT_FOUND, "看板不存在：" + id);
        }
        return rows.get(0);
    }

    private BoardDtos.BoardDetail toDetail(Map<String, Object> row) {
        return new BoardDtos.BoardDetail(longOf(row.get("id")), str(row.get("code")),
                str(row.get("name")), str(row.get("description")),
                Boolean.TRUE.equals(row.get("shared")),
                readComponents(str(row.get("layout_text"))),
                toOffset(row.get("created_at")), toOffset(row.get("updated_at")));
    }

    /** 组件校验：类型/数据集白名单、条数上限、span 与 top 范围，并补齐 id 与排序。 */
    private List<BoardDtos.Component> validateComponents(List<BoardDtos.Component> components) {
        if (components == null || components.isEmpty()) {
            throw BizException.of(ErrorCode.ANA_BOARD_INVALID, "看板至少需要一个组件");
        }
        if (components.size() > MAX_COMPONENTS) {
            throw BizException.of(ErrorCode.ANA_BOARD_INVALID,
                    "组件数超过上限 " + MAX_COMPONENTS);
        }
        List<BoardDtos.Component> result = new ArrayList<>();
        int index = 0;
        for (BoardDtos.Component component : components) {
            String type = component.type() == null ? "" : component.type().trim();
            if (!COMPONENT_TYPES.contains(type)) {
                throw BizException.of(ErrorCode.ANA_BOARD_INVALID, "不支持的组件类型：" + type);
            }
            if (!DATASETS.containsKey(component.dataset())) {
                throw BizException.of(ErrorCode.ANA_BOARD_INVALID,
                        "未知的数据集：" + component.dataset());
            }
            int span = component.span() == null ? 1 : component.span();
            if (span < 1 || span > 3) {
                throw BizException.of(ErrorCode.ANA_BOARD_INVALID, "组件宽度只能是 1~3 列");
            }
            int top = component.top() == null ? 10 : component.top();
            if (top < 1 || top > MAX_TOP) {
                throw BizException.of(ErrorCode.ANA_BOARD_INVALID, "取前 N 条只能是 1~" + MAX_TOP);
            }
            String id = component.id() == null || component.id().isBlank()
                    ? UUID.randomUUID().toString()
                    : component.id().trim();
            if (id.length() > MAX_COMPONENT_ID) {
                throw BizException.of(ErrorCode.ANA_BOARD_INVALID, "组件 id 过长");
            }
            result.add(new BoardDtos.Component(id, type,
                    component.title() == null || component.title().isBlank()
                            ? DATASETS.get(component.dataset()).meta().label()
                            : component.title().trim(),
                    component.dataset(), top, span, index++));
        }
        return result;
    }

    private List<BoardDtos.Component> readComponents(String json) {
        if (json == null || json.isBlank() || "null".equals(json)) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<List<BoardDtos.Component>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.ANA_BOARD_INVALID, "看板布局序列化失败");
        }
    }

    private static void register(String code, String label, String domain, String sql) {
        DATASETS.put(code, new DatasetDef(new BoardDtos.DatasetMeta(code, label, domain), sql));
    }

    /** 只需按某列分组计数的域（表名与列名是代码里的常量，不来自请求）。 */
    private static String groupBy(String table, String column) {
        return "SELECT " + column + " AS k, COUNT(1) AS c FROM " + table
                + " WHERE company_id = ? AND deleted = false GROUP BY 1 ORDER BY c DESC";
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /** queryForList 取 timestamptz 可能给 Timestamp，也可能给 OffsetDateTime，两种都归一。 */
    private static OffsetDateTime toOffset(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            java.time.Instant instant = timestamp.toInstant();
            return instant.atOffset(java.time.ZoneId.systemDefault().getRules().getOffset(instant));
        }
        return null;
    }
}
