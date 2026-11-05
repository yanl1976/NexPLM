package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.item.api.BomDtos;
import com.nexplm.platform.item.domain.BomLine;
import com.nexplm.platform.item.mapper.BomLineMapper;
import com.nexplm.platform.object.domain.PlmObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * BOM 服务：三视图（设计/工艺/制造）树查询与行维护。
 * <p>约束：同一父件同一视图下子件唯一（DB 唯一索引 + 服务层预检查）；禁止自引用与循环引用；
 * 用量必须大于 0；并发修改以乐观锁 version 兜底，冲突返回 409。</p>
 */
@Service
public class BomService {

    private static final List<String> VIEW_TYPES = List.of("DESIGN", "PROCESS", "MANUFACTURE");
    private static final int MAX_DEPTH = 20;
    private static final int LINE_STEP = 10;

    private final BomLineMapper bomMapper;
    private final ItemService itemService;
    private final JdbcTemplate jdbcTemplate;

    public BomService(BomLineMapper bomMapper, ItemService itemService, JdbcTemplate jdbcTemplate) {
        this.bomMapper = bomMapper;
        this.itemService = itemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------ 查询

    public List<BomDtos.BomNode> tree(Long rootId, String viewType, Integer depth) {
        itemService.requireItem(rootId);
        String view = normalizeView(viewType);
        int maxDepth = depth == null ? 5 : Math.min(Math.max(1, depth), MAX_DEPTH);
        return childrenOf(rootId, view, 1, maxDepth);
    }

    public List<BomDtos.WhereUsedRow> whereUsed(Long childId) {
        itemService.requireItem(childId);
        return jdbcTemplate.query("""
                        SELECT b.id, b.parent_object_id, b.view_type, b.qty, b.line_no, o.code, o.name
                          FROM bom_line b
                          JOIN plm_object o ON o.id = b.parent_object_id
                           AND o.is_latest = true AND o.deleted = false
                         WHERE b.child_object_id = ? AND b.deleted = false
                         ORDER BY o.code, b.view_type
                        """,
                (rs, rowNum) -> new BomDtos.WhereUsedRow(rs.getLong("id"), rs.getLong("parent_object_id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("view_type"),
                        rs.getBigDecimal("qty"), rs.getInt("line_no")),
                childId);
    }

    /** 结构校验：用量异常、单位缺失、子件不存在、循环引用。 */
    public BomDtos.BomValidateResult validate(Long rootId, String viewType) {
        itemService.requireItem(rootId);
        String view = normalizeView(viewType);
        List<BomDtos.BomIssue> issues = new ArrayList<>();

        jdbcTemplate.query("""
                        WITH RECURSIVE sub(parent, child) AS (
                            SELECT parent_object_id, child_object_id FROM bom_line
                             WHERE parent_object_id = ? AND view_type = ? AND deleted = false
                            UNION
                            SELECT b.parent_object_id, b.child_object_id FROM bom_line b
                              JOIN sub s ON b.parent_object_id = s.child
                             WHERE b.view_type = ? AND b.deleted = false
                        )
                        SELECT b.id, b.parent_object_id, b.child_object_id, b.qty, b.unit,
                               o.id AS child_exists
                          FROM sub
                          JOIN bom_line b ON b.parent_object_id = sub.parent
                           AND b.child_object_id = sub.child AND b.view_type = ? AND b.deleted = false
                          LEFT JOIN plm_object o ON o.id = b.child_object_id
                           AND o.deleted = false AND o.is_latest = true
                        """,
                rs -> {
                    String unit = rs.getString("unit");
                    BigDecimal qty = rs.getBigDecimal("qty");
                    Long lineId = rs.getLong("id");
                    Long parentId = rs.getLong("parent_object_id");
                    Long childId = rs.getLong("child_object_id");
                    if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                        issues.add(new BomDtos.BomIssue("QTY_INVALID", lineId, parentId, childId, "用量必须大于 0"));
                    }
                    if (unit == null || unit.isBlank()) {
                        issues.add(new BomDtos.BomIssue("UNIT_MISSING", lineId, parentId, childId, "缺少计量单位"));
                    }
                    rs.getLong("child_exists");
                    if (rs.wasNull()) {
                        issues.add(new BomDtos.BomIssue("ORPHAN", lineId, parentId, childId, "子件已删除或不存在"));
                    }
                }, rootId, view, view, view);

        Long cycle = jdbcTemplate.queryForObject("""
                        WITH RECURSIVE walk(root, node) AS (
                            SELECT parent_object_id, child_object_id FROM bom_line
                             WHERE parent_object_id = ? AND view_type = ? AND deleted = false
                            UNION
                            SELECT w.root, b.child_object_id FROM bom_line b
                              JOIN walk w ON b.parent_object_id = w.node
                             WHERE b.view_type = ? AND b.deleted = false
                        )
                        SELECT COUNT(1) FROM walk WHERE node = root
                        """, Long.class, rootId, view, view);
        if (cycle != null && cycle > 0) {
            issues.add(new BomDtos.BomIssue("CYCLE", null, rootId, null, "BOM 存在循环引用"));
        }
        return new BomDtos.BomValidateResult(issues.size(), issues);
    }

    // ------------------------------------------------------------ 维护

    @Transactional
    public BomDtos.BomNode add(Long parentId, String viewType, BomDtos.BomLineSaveReq req, Long userId) {
        itemService.requireItem(parentId);
        String view = normalizeView(viewType);
        if (parentId.equals(req.childId())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能把零部件自身作为子件");
        }
        itemService.requireItem(req.childId());
        ensureNoCycle(parentId, req.childId(), view);
        Long duplicate = jdbcTemplate.query("""
                        SELECT COUNT(1) FROM bom_line
                         WHERE parent_object_id = ? AND child_object_id = ? AND view_type = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getLong(1) : 0L, parentId, req.childId(), view);
        if (duplicate != null && duplicate > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该子件已在 BOM 中，请直接修改用量");
        }
        OffsetDateTime now = OffsetDateTime.now();
        BomLine line = new BomLine();
        line.setId(IdWorker.getId());
        line.setParentObjectId(parentId);
        line.setChildObjectId(req.childId());
        line.setViewType(view);
        line.setLineNo(nextLineNo(parentId, view));
        line.setQty(req.qty());
        line.setUnit(normalizeUnit(req.unit(), req.childId()));
        line.setLossRate(req.lossRate() == null ? BigDecimal.ZERO : req.lossRate());
        line.setRefDesignator(trimToNull(req.refDesignator()));
        line.setRemark(trimToNull(req.remark()));
        line.setVersion(0);
        line.setDeleted(false);
        line.setCreatedBy(userId);
        line.setCreatedAt(now);
        line.setUpdatedBy(userId);
        line.setUpdatedAt(now);
        bomMapper.insert(line);
        return nodeById(line.getId());
    }

    @Transactional
    public BomDtos.BomNode update(Long lineId, BomDtos.BomLineUpdateReq req, Long userId) {
        BomLine line = requireLine(lineId);
        BomLine update = new BomLine();
        update.setId(lineId);
        update.setVersion(line.getVersion());
        update.setQty(req.qty());
        update.setUnit(normalizeUnit(req.unit(), line.getChildObjectId()));
        update.setLossRate(req.lossRate() == null ? BigDecimal.ZERO : req.lossRate());
        update.setRefDesignator(trimToNull(req.refDesignator()));
        update.setRemark(trimToNull(req.remark()));
        update.setUpdatedBy(userId);
        update.setUpdatedAt(OffsetDateTime.now());
        if (bomMapper.updateById(update) == 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该 BOM 行已被他人修改，请刷新后重试");
        }
        return nodeById(lineId);
    }

    @Transactional
    public void remove(Long lineId) {
        requireLine(lineId);
        bomMapper.deleteById(lineId);
    }

    /** 移动：可换父件（需防环与重复校验）或仅调整序号。 */
    @Transactional
    public BomDtos.BomNode move(Long lineId, BomDtos.BomMoveReq req, Long userId) {
        BomLine line = requireLine(lineId);
        String view = req.viewType() == null ? line.getViewType() : normalizeView(req.viewType());
        Long newParent = req.newParentId() == null ? line.getParentObjectId() : req.newParentId();
        if (newParent.equals(line.getChildObjectId())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能把子件挂到自身下");
        }
        if (!newParent.equals(line.getParentObjectId()) || !view.equals(line.getViewType())) {
            itemService.requireItem(newParent);
            ensureNoCycle(newParent, line.getChildObjectId(), view);
            Long duplicate = jdbcTemplate.query("""
                            SELECT COUNT(1) FROM bom_line
                             WHERE parent_object_id = ? AND child_object_id = ? AND view_type = ?
                               AND deleted = false AND id <> ?
                            """, rs -> rs.next() ? rs.getLong(1) : 0L,
                    newParent, line.getChildObjectId(), view, lineId);
            if (duplicate != null && duplicate > 0) {
                throw BizException.of(ErrorCode.COMMON_CONFLICT, "目标父件下已存在同一子件");
            }
        }
        BomLine update = new BomLine();
        update.setId(lineId);
        update.setVersion(line.getVersion());
        update.setParentObjectId(newParent);
        update.setViewType(view);
        update.setLineNo(req.newLineNo() == null ? nextLineNo(newParent, view) : req.newLineNo());
        update.setUpdatedBy(userId);
        update.setUpdatedAt(OffsetDateTime.now());
        if (bomMapper.updateById(update) == 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该 BOM 行已被他人修改，请刷新后重试");
        }
        return nodeById(lineId);
    }

    // ------------------------------------------------------------ 内部

    private List<BomDtos.BomNode> childrenOf(Long parentId, String view, int level, int maxDepth) {
        List<BomDtos.BomNode> nodes = jdbcTemplate.query("""
                        SELECT b.id, b.child_object_id, b.qty, b.unit, b.loss_rate, b.line_no,
                               b.ref_designator, b.remark, b.version,
                               o.code, o.name, m.item_type, m.specification,
                               EXISTS (SELECT 1 FROM bom_line c
                                        WHERE c.parent_object_id = b.child_object_id
                                          AND c.view_type = b.view_type AND c.deleted = false) AS has_children
                          FROM bom_line b
                          JOIN plm_object o ON o.id = b.child_object_id
                           AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = b.child_object_id
                         WHERE b.parent_object_id = ? AND b.view_type = ? AND b.deleted = false
                         ORDER BY b.line_no, b.id
                        """,
                (rs, rowNum) -> new BomDtos.BomNode(rs.getLong("id"), rs.getLong("child_object_id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("item_type"),
                        rs.getString("specification"), rs.getString("unit"), rs.getBigDecimal("qty"),
                        rs.getBigDecimal("loss_rate"), rs.getInt("line_no"), rs.getString("ref_designator"),
                        rs.getString("remark"), rs.getInt("version"), rs.getBoolean("has_children"),
                        new ArrayList<>()),
                parentId, view);
        if (level >= maxDepth) {
            return nodes;
        }
        for (BomDtos.BomNode node : nodes) {
            if (Boolean.TRUE.equals(node.hasChildren())) {
                node.children().addAll(childrenOf(node.childId(), view, level + 1, maxDepth));
            }
        }
        return nodes;
    }

    private BomDtos.BomNode nodeById(Long lineId) {
        BomLine line = requireLine(lineId);
        PlmObject child = itemService.requireItem(line.getChildObjectId());
        String[] itemMeta = jdbcTemplate.query(
                "SELECT item_type, specification FROM item_master WHERE object_id = ?",
                rs -> rs.next() ? new String[]{rs.getString(1), rs.getString(2)} : new String[]{null, null},
                line.getChildObjectId());
        boolean hasChildren = Boolean.TRUE.equals(jdbcTemplate.query(
                "SELECT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = ? AND view_type = ? AND deleted = false)",
                rs -> rs.next() && rs.getBoolean(1), line.getChildObjectId(), line.getViewType()));
        return new BomDtos.BomNode(line.getId(), line.getChildObjectId(), child.getCode(), child.getName(),
                itemMeta[0], itemMeta[1], line.getUnit(), line.getQty(), line.getLossRate(), line.getLineNo(),
                line.getRefDesignator(), line.getRemark(), line.getVersion(), hasChildren, new ArrayList<>());
    }

    private void ensureNoCycle(Long parentId, Long childId, String view) {
        Set<Long> ancestors = ancestorsOf(parentId, view);
        if (ancestors.contains(childId)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "会形成循环引用：目标子件是当前件的上级");
        }
    }

    private Set<Long> ancestorsOf(Long itemId, String view) {
        Set<Long> ancestors = new HashSet<>();
        jdbcTemplate.query("""
                        WITH RECURSIVE anc AS (
                            SELECT parent_object_id FROM bom_line
                             WHERE child_object_id = ? AND view_type = ? AND deleted = false
                            UNION
                            SELECT b.parent_object_id FROM bom_line b
                              JOIN anc a ON b.child_object_id = a.parent_object_id
                             WHERE b.view_type = ? AND b.deleted = false
                        )
                        SELECT parent_object_id FROM anc
                        """,
                rs -> {
                    ancestors.add(rs.getLong(1));
                }, itemId, view, view);
        return ancestors;
    }

    private BomLine requireLine(Long lineId) {
        BomLine line = bomMapper.selectById(lineId);
        if (line == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "BOM 行不存在：" + lineId);
        }
        return line;
    }

    private int nextLineNo(Long parentId, String view) {
        Integer max = jdbcTemplate.query("""
                        SELECT COALESCE(MAX(line_no), 0) FROM bom_line
                         WHERE parent_object_id = ? AND view_type = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getInt(1) : 0, parentId, view);
        return (max == null ? 0 : max) + LINE_STEP;
    }

    private String normalizeView(String viewType) {
        if (viewType == null || viewType.isBlank()) {
            return "DESIGN";
        }
        String normalized = viewType.trim().toUpperCase(Locale.ROOT);
        if (!VIEW_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "不支持的 BOM 视图：" + viewType + "，可选：" + String.join("/", VIEW_TYPES));
        }
        return normalized;
    }

    private String normalizeUnit(String unit, Long childId) {
        if (unit != null && !unit.isBlank()) {
            return unit.trim();
        }
        String childUnit = jdbcTemplate.query("SELECT unit FROM item_master WHERE object_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, childId);
        return childUnit == null || childUnit.isBlank() ? "EA" : childUnit;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
