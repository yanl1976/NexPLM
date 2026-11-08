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
import java.time.LocalDate;
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

    public List<BomDtos.BomNode> tree(Long rootId, String viewType, Integer depth, LocalDate effectAt) {
        itemService.requireItem(rootId);
        String view = normalizeView(viewType);
        int maxDepth = depth == null ? 5 : Math.min(Math.max(1, depth), MAX_DEPTH);
        return childrenOf(rootId, view, 1, maxDepth, effectAt);
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
                               o.id AS child_exists, m.unit AS child_unit
                          FROM sub
                          JOIN bom_line b ON b.parent_object_id = sub.parent
                           AND b.child_object_id = sub.child AND b.view_type = ? AND b.deleted = false
                          LEFT JOIN plm_object o ON o.id = b.child_object_id
                           AND o.deleted = false AND o.is_latest = true
                          LEFT JOIN item_master m ON m.object_id = b.child_object_id
                        """,
                rs -> {
                    String unit = rs.getString("unit");
                    if (unit == null || unit.isBlank()) {
                        unit = rs.getString("child_unit");
                    }
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

    /**
     * BOM 展开（多层级扁平列表，用量按层级累乘，叠加损耗率得到有效用量）。
     * <p>对应旧系统 {@code FOWARDBOMSTATISTIC} 的逐行输出口径：不聚合，输出层级与路径
     * （{@code LPATH} = 由各级「位置」串成的点分路径）。{@code effectAt} 非空时按有效性区间过滤。</p>
     */
    public List<BomDtos.BomSummaryRow> summary(Long rootId, String viewType, Integer maxLevel, LocalDate effectAt) {
        itemService.requireItem(rootId);
        String view = normalizeView(viewType);
        int depth = maxLevel == null ? 10 : Math.min(Math.max(1, maxLevel), MAX_DEPTH);
        List<Object> args = new ArrayList<>(List.of(rootId, view));
        StringBuilder sql = new StringBuilder("""
                        WITH RECURSIVE bom_tree AS (
                            SELECT b.child_object_id AS child, b.parent_object_id AS parent, 1 AS level,
                                   CAST(b.qty AS numeric) AS qty, COALESCE(b.loss_rate, 0) AS loss_rate,
                                   b.line_no, b.unit, b.effect_from, b.effect_to,
                                   CAST(b.line_no AS text) AS path
                              FROM bom_line b
                             WHERE b.parent_object_id = ? AND b.view_type = ? AND b.deleted = false
                        """);
        appendEffectFilter(sql, "b", effectAt, args);
        sql.append("""
                            UNION ALL
                            SELECT b.child_object_id, b.parent_object_id, t.level + 1,
                                   CAST(t.qty * b.qty AS numeric), COALESCE(b.loss_rate, 0), b.line_no, b.unit,
                                   b.effect_from, b.effect_to,
                                   CAST(t.path || '.' || CAST(b.line_no AS text) AS text)
                              FROM bom_line b
                              JOIN bom_tree t ON b.parent_object_id = t.child
                             WHERE b.view_type = ? AND b.deleted = false AND t.level < ?
                        """);
        args.add(view);
        args.add(depth);
        appendEffectFilter(sql, "b", effectAt, args);
        sql.append("""
                        )
                        SELECT t.level, t.path, t.parent, t.child, t.qty, t.loss_rate, t.line_no,
                               o.code, o.name, m.item_type, m.specification,
                               COALESCE(NULLIF(t.unit, ''), m.unit) AS unit,
                               t.effect_from, t.effect_to,
                               t.qty * (1 + t.loss_rate / 100.0) AS effective_qty
                          FROM bom_tree t
                          JOIN plm_object o ON o.id = t.child AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = t.child
                         ORDER BY t.level, t.line_no, o.code
                        """);
        return jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new BomDtos.BomSummaryRow(rs.getInt("level"), rs.getString("path"),
                        rs.getLong("parent"), rs.getLong("child"), rs.getString("code"), rs.getString("name"),
                        rs.getString("item_type"), rs.getString("specification"), rs.getString("unit"),
                        rs.getBigDecimal("qty"), rs.getBigDecimal("loss_rate"),
                        rs.getBigDecimal("effective_qty"), rs.getInt("line_no"),
                        rs.getObject("effect_from", LocalDate.class), rs.getObject("effect_to", LocalDate.class)),
                args.toArray());
    }

    /**
     * BOM 汇总：按子件合并全树用量（同一子件多处出现累计）。
     * <p>单位口径遵循 PLM 设定：用量以子件基本单位计量（旧系统 {@code PSEBOMLINE.LQUALITY} 迁移时
     * 本身不带单位维度），{@code bom_line.unit} 只是行级冗余快照。故汇总是「按子件」而非「按子件+行单位」，
     * 单位统一取子件主数据 {@code item_master.unit}，行单位仅在主数据缺省时兜底。</p>
     */
    public List<BomDtos.BomRollupRow> rollup(Long rootId, String viewType, Integer maxLevel,
                                             LocalDate effectAt, boolean leafOnly) {
        itemService.requireItem(rootId);
        String view = normalizeView(viewType);
        int depth = maxLevel == null ? 10 : Math.min(Math.max(1, maxLevel), MAX_DEPTH);
        List<Object> args = new ArrayList<>(List.of(rootId, view));
        StringBuilder sql = new StringBuilder("""
                        WITH RECURSIVE bom_tree AS (
                            SELECT b.child_object_id AS child, b.parent_object_id AS parent, 1 AS level,
                                   CAST(b.qty AS numeric) AS qty, COALESCE(b.loss_rate, 0) AS loss_rate, b.unit
                              FROM bom_line b
                             WHERE b.parent_object_id = ? AND b.view_type = ? AND b.deleted = false
                        """);
        appendEffectFilter(sql, "b", effectAt, args);
        sql.append("""
                            UNION ALL
                            SELECT b.child_object_id, b.parent_object_id, t.level + 1,
                                   CAST(t.qty * b.qty AS numeric), COALESCE(b.loss_rate, 0), b.unit
                              FROM bom_line b
                              JOIN bom_tree t ON b.parent_object_id = t.child
                             WHERE b.view_type = ? AND b.deleted = false AND t.level < ?
                        """);
        args.add(view);
        args.add(depth);
        appendEffectFilter(sql, "b", effectAt, args);
        sql.append("""
                        )
                        SELECT t.child, o.code, o.name, m.item_type, m.specification,
                               COALESCE(NULLIF(m.unit, ''), MAX(t.unit)) AS unit,
                               SUM(t.qty) AS total_qty,
                               SUM(t.qty * (1 + t.loss_rate / 100.0)) AS total_effective_qty,
                               COUNT(1) AS occurrences
                          FROM bom_tree t
                          JOIN plm_object o ON o.id = t.child AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = t.child
                        """);
        if (leafOnly) {
            sql.append(" WHERE NOT EXISTS (SELECT 1 FROM bom_tree t2 WHERE t2.parent = t.child)");
        }
        sql.append("""
                         GROUP BY t.child, o.code, o.name, m.item_type, m.specification, m.unit
                         ORDER BY o.code
                        """);
        return jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new BomDtos.BomRollupRow(rs.getLong("child"), rs.getString("code"),
                        rs.getString("name"), rs.getString("item_type"), rs.getString("specification"),
                        rs.getString("unit"), rs.getBigDecimal("total_qty"),
                        rs.getBigDecimal("total_effective_qty"), rs.getInt("occurrences")),
                args.toArray());
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
        line.setTotalQty(req.totalQty());
        line.setSurplusQty(req.surplusQty());
        line.setSubUseQty(req.subUseQty());
        line.setBomType(normalizeBomType(req.bomType(), "DESIGN"));
        line.setItemRoute(trimToNull(req.itemRoute()));
        line.setDataSource(trimToNull(req.dataSource()));
        line.setUnit(normalizeUnit(req.unit(), req.childId()));
        line.setLossRate(req.lossRate() == null ? BigDecimal.ZERO : req.lossRate());
        validateEffectRange(req.effectFrom(), req.effectTo());
        line.setEffectFrom(req.effectFrom());
        line.setEffectTo(req.effectTo());
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
        update.setTotalQty(req.totalQty());
        update.setSurplusQty(req.surplusQty());
        update.setSubUseQty(req.subUseQty());
        // 空表示不动（保留原类型），避免前端不传字段时被冲成 DESIGN
        update.setBomType(req.bomType() == null || req.bomType().isBlank()
                ? line.getBomType() : normalizeBomType(req.bomType(), "DESIGN"));
        update.setItemRoute(trimToNull(req.itemRoute()));
        update.setDataSource(trimToNull(req.dataSource()));
        update.setUnit(normalizeUnit(req.unit(), line.getChildObjectId()));
        update.setLossRate(req.lossRate() == null ? BigDecimal.ZERO : req.lossRate());
        validateEffectRange(req.effectFrom(), req.effectTo());
        update.setEffectFrom(req.effectFrom());
        update.setEffectTo(req.effectTo());
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

    /**
     * 批量改用量/损耗率/单位（对齐旧系统 {@code BatchAlterEditNumItem}）。
     * <p>只更新请求中非空的字段；每行都要校验是否属于该根件的 BOM 树，避免越权改别棵树。</p>
     */
    @Transactional
    public BomDtos.BomBatchResult batchUpdateQty(Long rootId, BomDtos.BomBatchQtyReq req, Long userId) {
        itemService.requireItem(rootId);
        boolean hasUnit = req.unit() != null && !req.unit().isBlank();
        if (req.qty() == null && req.lossRate() == null && !hasUnit) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请至少提供用量/损耗率/单位中的一项");
        }
        if (req.qty() != null && req.qty().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "用量必须大于 0");
        }
        List<String> messages = new ArrayList<>();
        int updated = 0;
        int skipped = 0;
        for (Long lineId : req.lineIds()) {
            BomLine line = bomMapper.selectById(lineId);
            if (line == null) {
                skipped++;
                messages.add("BOM 行不存在：" + lineId);
                continue;
            }
            if (!inSubtree(rootId, line.getParentObjectId(), line.getViewType())) {
                skipped++;
                messages.add("BOM 行不属于该零部件的结构：" + lineId);
                continue;
            }
            jdbcTemplate.update("""
                            UPDATE bom_line
                               SET qty = COALESCE(?, qty),
                                   loss_rate = COALESCE(?, loss_rate),
                                   unit = COALESCE(?, unit),
                                   version = version + 1,
                                   updated_by = ?,
                                   updated_at = now()
                             WHERE id = ? AND deleted = false
                            """,
                    req.qty(), req.lossRate(), hasUnit ? req.unit().trim() : null, userId, lineId);
            updated++;
        }
        return new BomDtos.BomBatchResult(req.lineIds().size(), updated, skipped, messages);
    }

    /**
     * 批量替换子件（对齐旧系统 {@code BatchUpdateBomOnReplace}）：把该根件树中的 A 全部换成 B。
     * <p>逐行校验：不属于该树、会挂到自身、目标父件下已有 B、会形成环 —— 命中则跳过并给出原因。</p>
     */
    @Transactional
    public BomDtos.BomBatchResult batchReplace(Long rootId, BomDtos.BomBatchReplaceReq req, Long userId) {
        itemService.requireItem(rootId);
        itemService.requireItem(req.fromChildId());
        itemService.requireItem(req.toChildId());
        if (req.fromChildId().equals(req.toChildId())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "替换目标不能与被替换件相同");
        }
        String view = BomViews.normalize(req.viewType());
        List<Object> args = new ArrayList<>(List.of(req.fromChildId(), view));
        StringBuilder sql = new StringBuilder("""
                        SELECT b.id, b.parent_object_id
                          FROM bom_line b
                         WHERE b.child_object_id = ? AND b.view_type = ? AND b.deleted = false
                        """);
        if (req.parentIds() != null && !req.parentIds().isEmpty()) {
            sql.append(" AND b.parent_object_id IN (")
                    .append(String.join(",", java.util.Collections.nCopies(req.parentIds().size(), "?")))
                    .append(")");
            args.addAll(req.parentIds());
        }
        List<Object[]> lines = new ArrayList<>();
        jdbcTemplate.query(sql.toString(), rs -> {
            lines.add(new Object[]{rs.getLong("id"), rs.getLong("parent_object_id")});
        }, args.toArray());

        List<String> messages = new ArrayList<>();
        int updated = 0;
        int skipped = 0;
        for (Object[] row : lines) {
            Long lineId = (Long) row[0];
            Long parentId = (Long) row[1];
            if (!inSubtree(rootId, parentId, view)) {
                skipped++;
                messages.add("第 " + lineId + " 行不在该零部件的结构中");
                continue;
            }
            if (parentId.equals(req.toChildId())) {
                skipped++;
                messages.add("第 " + lineId + " 行替换后会挂到自身");
                continue;
            }
            Long duplicate = jdbcTemplate.query("""
                            SELECT COUNT(1) FROM bom_line
                             WHERE parent_object_id = ? AND child_object_id = ? AND view_type = ?
                               AND deleted = false AND id <> ?
                            """, rs -> rs.next() ? rs.getLong(1) : 0L,
                    parentId, req.toChildId(), view, lineId);
            if (duplicate != null && duplicate > 0) {
                skipped++;
                messages.add("第 " + lineId + " 行：目标父件下已存在该子件");
                continue;
            }
            try {
                ensureNoCycle(parentId, req.toChildId(), view);
            } catch (BizException ex) {
                skipped++;
                messages.add("第 " + lineId + " 行：" + ex.getMessage());
                continue;
            }
            jdbcTemplate.update("""
                            UPDATE bom_line
                               SET child_object_id = ?, version = version + 1,
                                   updated_by = ?, updated_at = now()
                             WHERE id = ? AND deleted = false
                            """, req.toChildId(), userId, lineId);
            updated++;
        }
        return new BomDtos.BomBatchResult(lines.size(), updated, skipped, messages);
    }

    private boolean inSubtree(Long rootId, Long objectId, String view) {
        if (objectId == null) {
            return false;
        }
        return objectId.equals(rootId) || ancestorsOf(objectId, view).contains(rootId);
    }

    // ------------------------------------------------------------ 内部

    private List<BomDtos.BomNode> childrenOf(Long parentId, String view, int level, int maxDepth, LocalDate effectAt) {
        List<Object> args = new ArrayList<>(List.of(parentId, view));
        StringBuilder sql = new StringBuilder("""
                        SELECT b.id, b.child_object_id, b.qty,
                               b.total_qty, b.surplus_qty, b.sub_use_qty,
                               b.bom_type, b.item_route, b.data_source,
                               b.unit, b.loss_rate, b.line_no,
                               b.effect_from, b.effect_to, b.ref_designator, b.remark, b.version,
                               o.code, o.name, m.item_type, m.specification,
                               EXISTS (SELECT 1 FROM bom_line c
                                        WHERE c.parent_object_id = b.child_object_id
                                          AND c.view_type = b.view_type AND c.deleted = false) AS has_children
                          FROM bom_line b
                          JOIN plm_object o ON o.id = b.child_object_id
                           AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = b.child_object_id
                         WHERE b.parent_object_id = ? AND b.view_type = ? AND b.deleted = false
                        """);
        appendEffectFilter(sql, "b", effectAt, args);
        sql.append(" ORDER BY b.line_no, b.id");
        List<BomDtos.BomNode> nodes = jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new BomDtos.BomNode(rs.getLong("id"), rs.getLong("child_object_id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("item_type"),
                        rs.getString("specification"), rs.getString("unit"), rs.getBigDecimal("qty"),
                        rs.getBigDecimal("total_qty"), rs.getBigDecimal("surplus_qty"),
                        rs.getBigDecimal("sub_use_qty"), rs.getString("bom_type"),
                        rs.getString("item_route"), rs.getString("data_source"),
                        rs.getBigDecimal("loss_rate"), rs.getInt("line_no"),
                        rs.getObject("effect_from", LocalDate.class), rs.getObject("effect_to", LocalDate.class),
                        rs.getString("ref_designator"), rs.getString("remark"), rs.getInt("version"),
                        rs.getBoolean("has_children"), new ArrayList<>()),
                args.toArray());
        if (level >= maxDepth) {
            return nodes;
        }
        for (BomDtos.BomNode node : nodes) {
            if (Boolean.TRUE.equals(node.hasChildren())) {
                node.children().addAll(childrenOf(node.childId(), view, level + 1, maxDepth, effectAt));
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
                itemMeta[0], itemMeta[1], line.getUnit(), line.getQty(),
                line.getTotalQty(), line.getSurplusQty(), line.getSubUseQty(),
                line.getBomType(), line.getItemRoute(), line.getDataSource(),
                line.getLossRate(), line.getLineNo(),
                line.getEffectFrom(), line.getEffectTo(),
                line.getRefDesignator(), line.getRemark(), line.getVersion(), hasChildren, new ArrayList<>());
    }

    /** BOM 类型归一：空用默认值，非空转大写下划线（`Quote` → `QUOTE`）。 */
    private static String normalizeBomType(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * BOM-01：对齐蓝本 `PSEBOMLINEEDITCHECKNEW`。
     *
     * <p>蓝本做「正向展开（待加子件向下）+ 反向展开（当前父件向上）」，两个集合
     * union 后按节点计数，出现两次（即两边都有）就判成环；本方法用交集等价表达。
     * 两个方向都受 {@link #MAX_DEPTH}（蓝本 {@code @MAXLEVEL}）限制，避免脏数据
     * 导致递归失控。</p>
     *
     * <p>蓝本 {@code @ISCONTAIMSELF=1} 可放行自包含；我们默认禁止自引用
     *（`add()` 里拦 {@code parentId == childId}），开关登记为待业务。</p>
     */
    private void ensureNoCycle(Long parentId, Long childId, String view) {
        Set<Long> forward = descendantsOfBounded(childId, view);
        Set<Long> backward = ancestorsOfBounded(parentId, view);
        forward.retainAll(backward);
        if (!forward.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "会形成循环引用：目标子件是当前件的上级");
        }
    }

    /** 反向展开：从当前父件向上找祖先（含起点），受 MAX_DEPTH 限制。 */
    private Set<Long> ancestorsOfBounded(Long itemId, String view) {
        Set<Long> nodes = new HashSet<>();
        jdbcTemplate.query("""
                        WITH RECURSIVE anc(node, lvl) AS (
                            SELECT ?, 0
                            UNION
                            SELECT b.parent_object_id, a.lvl + 1
                              FROM bom_line b JOIN anc a ON b.child_object_id = a.node
                             WHERE b.view_type = ? AND b.deleted = false AND a.lvl < ?
                        )
                        SELECT node FROM anc
                        """,
                rs -> { nodes.add(rs.getLong(1)); }, itemId, view, MAX_DEPTH);
        return nodes;
    }

    /** 正向展开：从待加子件向下找后代（排除自身，对应蓝本 CITEMUID != @ADDITEMMASTERID_TEMP）。 */
    private Set<Long> descendantsOfBounded(Long itemId, String view) {
        Set<Long> nodes = new HashSet<>();
        jdbcTemplate.query("""
                        WITH RECURSIVE des(node, lvl) AS (
                            SELECT ?, 0
                            UNION
                            SELECT b.child_object_id, d.lvl + 1
                              FROM bom_line b JOIN des d ON b.parent_object_id = d.node
                             WHERE b.view_type = ? AND b.deleted = false AND d.lvl < ?
                        )
                        SELECT node FROM des WHERE node <> ?
                        """,
                rs -> { nodes.add(rs.getLong(1)); }, itemId, view, MAX_DEPTH, itemId);
        return nodes;
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
        return BomViews.normalize(viewType);
    }

    /**
     * 有效性区间过滤：{@code effectAt} 为空表示不按有效性筛选，对齐旧系统「不传 VALIDITYQUERYDATA 即不选有效性」。
     * <p>旧规则为 {@code STARTDATA < v AND (ENDDATA IS NULL OR ENDDATA > v)} 的开区间，
     * 新模型采用业务直觉更强的闭区间：{@code effect_from <= v <= effect_to}，空值表示该侧不限。</p>
     */
    private void appendEffectFilter(StringBuilder sql, String alias, LocalDate effectAt, List<Object> args) {
        if (effectAt == null) {
            return;
        }
        sql.append(" AND (").append(alias).append(".effect_from IS NULL OR ")
                .append(alias).append(".effect_from <= ?)")
                .append(" AND (").append(alias).append(".effect_to IS NULL OR ")
                .append(alias).append(".effect_to >= ?)");
        args.add(effectAt);
        args.add(effectAt);
    }

    private void validateEffectRange(LocalDate effectFrom, LocalDate effectTo) {
        if (effectFrom != null && effectTo != null && effectFrom.isAfter(effectTo)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "有效性起始日期不能晚于截止日期");
        }
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
