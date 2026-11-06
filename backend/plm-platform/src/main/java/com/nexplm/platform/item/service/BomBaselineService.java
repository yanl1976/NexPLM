package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.item.api.BomDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * BOM 基线服务：把某根件 + 视图的当前展开结果冻结为不可变快照，并支持「基线 vs 当前结构」差异对比。
 * <p>匹配键为层级路径 {@code path}（旧系统 {@code LPATH} 语义）：同一子件可在多处出现，
 * 只有路径能稳定标识一行。差异类型：新增 / 删除 / 用量或损耗率变更 / 子件被替换。</p>
 */
@Service
public class BomBaselineService {

    private static final int MAX_DEPTH = 20;

    private static final RowMapper<BomDtos.BomBaselineRow> HEADER_MAPPER = (rs, rowNum) ->
            new BomDtos.BomBaselineRow(rs.getLong("id"), rs.getLong("root_object_id"), rs.getString("code"),
                    rs.getString("name"), rs.getString("view_type"), rs.getInt("line_count"),
                    rs.getObject("frozen_at", OffsetDateTime.class), rs.getString("remark"),
                    rs.getObject("created_by", Long.class));

    private final JdbcTemplate jdbcTemplate;
    private final BomService bomService;
    private final ItemService itemService;

    public BomBaselineService(JdbcTemplate jdbcTemplate, BomService bomService, ItemService itemService) {
        this.jdbcTemplate = jdbcTemplate;
        this.bomService = bomService;
        this.itemService = itemService;
    }

    /** 冻结当前结构为基线。 */
    @Transactional
    public BomDtos.BomBaselineRow create(Long rootId, BomDtos.BomBaselineSaveReq req, Long userId) {
        itemService.requireItem(rootId);
        String view = BomViews.normalize(req.viewType());
        int depth = req.depth() == null ? MAX_DEPTH : Math.min(Math.max(1, req.depth()), MAX_DEPTH);
        String code = req.code().trim();
        Long duplicate = jdbcTemplate.query("""
                        SELECT COUNT(1) FROM bom_baseline
                         WHERE root_object_id = ? AND code = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getLong(1) : 0L, rootId, code);
        if (duplicate != null && duplicate > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "基线编码已存在：" + code);
        }
        List<BomDtos.BomSummaryRow> rows = bomService.summary(rootId, view, depth, null);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该视图下没有 BOM 结构，无法创建基线");
        }
        long id = IdWorker.getId();
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update("""
                        INSERT INTO bom_baseline (id, root_object_id, code, name, view_type, line_count, frozen_at,
                                                  remark, deleted, created_by, created_at, updated_by, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, false, ?, ?, ?, ?)
                        """,
                id, rootId, code, trimToNull(req.name()), view, rows.size(), now, trimToNull(req.remark()),
                userId, now, userId, now);
        List<Object[]> batch = new ArrayList<>(rows.size());
        for (BomDtos.BomSummaryRow row : rows) {
            batch.add(new Object[]{IdWorker.getId(), id, row.level(), row.path(), row.parentId(), row.childId(),
                    row.lineNo(), row.qty(), row.lossRate() == null ? BigDecimal.ZERO : row.lossRate(),
                    row.unit(), row.effectFrom(), row.effectTo()});
        }
        jdbcTemplate.batchUpdate("""
                        INSERT INTO bom_baseline_line (id, baseline_id, level, path, parent_object_id, child_object_id,
                                                       line_no, qty, loss_rate, unit, effect_from, effect_to)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, batch);
        return headerById(id);
    }

    public List<BomDtos.BomBaselineRow> list(Long rootId, String viewType) {
        itemService.requireItem(rootId);
        List<Object> args = new ArrayList<>(List.of(rootId));
        StringBuilder sql = new StringBuilder("""
                        SELECT id, root_object_id, code, name, view_type, line_count, frozen_at, remark, created_by
                          FROM bom_baseline
                         WHERE root_object_id = ? AND deleted = false
                        """);
        if (viewType != null && !viewType.isBlank()) {
            sql.append(" AND view_type = ?");
            args.add(BomViews.normalize(viewType));
        }
        sql.append(" ORDER BY frozen_at DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), HEADER_MAPPER, args.toArray());
    }

    /** 基线快照行（子件已删除时 code/name 为空，不影响路径匹配）。 */
    public List<BomDtos.BomBaselineLineRow> lines(Long rootId, Long baselineId) {
        requireBaseline(rootId, baselineId);
        return jdbcTemplate.query("""
                        SELECT l.id, l.level, l.path, l.parent_object_id, l.child_object_id, l.line_no,
                               l.qty, l.loss_rate, l.unit, l.effect_from, l.effect_to,
                               o.code, o.name, m.item_type, m.specification
                          FROM bom_baseline_line l
                          LEFT JOIN plm_object o ON o.id = l.child_object_id
                           AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = l.child_object_id
                         WHERE l.baseline_id = ?
                         ORDER BY l.level, l.path
                        """,
                (rs, rowNum) -> new BomDtos.BomBaselineLineRow(rs.getLong("id"), rs.getInt("level"),
                        rs.getString("path"), rs.getObject("parent_object_id", Long.class),
                        rs.getLong("child_object_id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("item_type"), rs.getString("specification"), rs.getString("unit"),
                        rs.getBigDecimal("qty"), rs.getBigDecimal("loss_rate"), rs.getInt("line_no"),
                        rs.getObject("effect_from", LocalDate.class), rs.getObject("effect_to", LocalDate.class)),
                baselineId);
    }

    /**
     * 基线与当前结构的差异。
     * <p>{@code effectAt} 透传给当前结构展开，用于「按某日期的有效结构」对比；为空表示比全部 BOM 行。</p>
     */
    public BomDtos.BomBaselineDiff diffWithCurrent(Long rootId, Long baselineId, LocalDate effectAt, Integer depth) {
        BomDtos.BomBaselineRow header = requireBaseline(rootId, baselineId);
        int maxDepth = depth == null ? MAX_DEPTH : Math.min(Math.max(1, depth), MAX_DEPTH);
        List<BomDtos.BomBaselineLineRow> baselineLines = lines(rootId, baselineId);
        List<BomDtos.BomSummaryRow> currentLines = bomService.summary(rootId, header.viewType(), maxDepth, effectAt);

        Map<String, BomDtos.BomBaselineLineRow> baselineByPath = new LinkedHashMap<>();
        for (BomDtos.BomBaselineLineRow line : baselineLines) {
            baselineByPath.put(line.path(), line);
        }
        Map<String, BomDtos.BomSummaryRow> currentByPath = new LinkedHashMap<>();
        for (BomDtos.BomSummaryRow line : currentLines) {
            currentByPath.put(line.path(), line);
        }
        Set<String> paths = new TreeSet<>();
        paths.addAll(baselineByPath.keySet());
        paths.addAll(currentByPath.keySet());

        List<BomDtos.BomBaselineDiffRow> rows = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int changed = 0;
        for (String path : paths) {
            BomDtos.BomBaselineLineRow base = baselineByPath.get(path);
            BomDtos.BomSummaryRow cur = currentByPath.get(path);
            if (base == null) {
                added++;
                rows.add(new BomDtos.BomBaselineDiffRow("ADDED", path, cur.level(), cur.childId(), cur.code(),
                        cur.name(), cur.unit(), null, cur.qty(), null, cur.lossRate()));
            } else if (cur == null) {
                removed++;
                rows.add(new BomDtos.BomBaselineDiffRow("REMOVED", path, base.level(), base.childId(), base.code(),
                        base.name(), base.unit(), base.qty(), null, base.lossRate(), null));
            } else if (!Objects.equals(base.childId(), cur.childId())) {
                changed++;
                rows.add(new BomDtos.BomBaselineDiffRow("CHILD_CHANGED", path, cur.level(), cur.childId(), cur.code(),
                        cur.name(), cur.unit(), base.qty(), cur.qty(), base.lossRate(), cur.lossRate()));
            } else if (!sameValue(base.qty(), cur.qty()) || !sameValue(base.lossRate(), cur.lossRate())) {
                changed++;
                rows.add(new BomDtos.BomBaselineDiffRow("QTY_CHANGED", path, cur.level(), cur.childId(), cur.code(),
                        cur.name(), cur.unit(), base.qty(), cur.qty(), base.lossRate(), cur.lossRate()));
            }
        }
        return new BomDtos.BomBaselineDiff(baselineId, header.code(), header.viewType(),
                added, removed, changed, rows);
    }

    @Transactional
    public void delete(Long rootId, Long baselineId) {
        BomDtos.BomBaselineRow header = requireBaseline(rootId, baselineId);
        jdbcTemplate.update("UPDATE bom_baseline SET deleted = true, updated_at = now() WHERE id = ?", header.id());
    }

    // ------------------------------------------------------------ 内部

    private BomDtos.BomBaselineRow headerById(Long id) {
        return jdbcTemplate.query("""
                        SELECT id, root_object_id, code, name, view_type, line_count, frozen_at, remark, created_by
                          FROM bom_baseline WHERE id = ? AND deleted = false
                        """, rs -> rs.next() ? HEADER_MAPPER.mapRow(rs, 0) : null, id);
    }

    private BomDtos.BomBaselineRow requireBaseline(Long rootId, Long baselineId) {
        BomDtos.BomBaselineRow header = headerById(baselineId);
        if (header == null || !rootId.equals(header.rootObjectId())) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "基线不存在：" + baselineId);
        }
        return header;
    }

    private boolean sameValue(BigDecimal left, BigDecimal right) {
        BigDecimal a = left == null ? BigDecimal.ZERO : left;
        BigDecimal b = right == null ? BigDecimal.ZERO : right;
        return a.compareTo(b) == 0;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
