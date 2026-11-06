package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.item.api.BomDtos;
import com.nexplm.platform.item.domain.BomAlternate;
import com.nexplm.platform.item.mapper.BomAlternateMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BOM 替代件服务：维护「某个已在 BOM 中出现的子件可被哪些备选件替换」。
 * <p>对齐旧系统 {@code ALTERNATESRELATION} 与 {@code PseBomService.SaveAlternatesParts}：
 * 只做关系维护，不改写 BOM 行，也不参与展开与汇总。</p>
 * <p>旧系统校验中的「不允许工艺件」「不能对系列件/实例操作」在新模型中不适用
 * （零部件无 CAPP 表单类型、无系列件概念），故只保留：自身、重复、未入 BOM 三条。</p>
 */
@Service
public class BomAlternateService {

    private static final int LINE_STEP = 10;

    private final BomAlternateMapper alternateMapper;
    private final ItemService itemService;
    private final JdbcTemplate jdbcTemplate;

    public BomAlternateService(BomAlternateMapper alternateMapper, ItemService itemService,
                               JdbcTemplate jdbcTemplate) {
        this.alternateMapper = alternateMapper;
        this.itemService = itemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 备选件列表；viewType 非空时按 BOM 使用视角过滤（全局替代对所有视图生效）。 */
    public List<BomDtos.BomAlternateRow> list(Long itemObjectId, String viewType) {
        itemService.requireItem(itemObjectId);
        List<Object> args = new ArrayList<>(List.of(itemObjectId));
        StringBuilder sql = new StringBuilder("""
                        SELECT a.id, a.item_object_id, a.alternate_object_id, a.view_type, a.is_global,
                               a.is_two_way, a.priority_level, a.line_no, a.remark, a.version,
                               o.code, o.name, m.item_type, m.specification, m.unit
                          FROM bom_alternate a
                          JOIN plm_object o ON o.id = a.alternate_object_id
                           AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = a.alternate_object_id
                         WHERE a.item_object_id = ? AND a.deleted = false
                        """);
        if (viewType != null && !viewType.isBlank()) {
            sql.append(" AND (a.is_global = true OR a.view_type = ?)");
            args.add(normalizeView(viewType));
        }
        sql.append(" ORDER BY a.line_no, a.id");
        return jdbcTemplate.query(sql.toString(),
                (rs, rowNum) -> new BomDtos.BomAlternateRow(rs.getLong("id"), rs.getLong("item_object_id"),
                        rs.getLong("alternate_object_id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("item_type"), rs.getString("specification"), rs.getString("unit"),
                        rs.getString("view_type"), rs.getBoolean("is_global"), rs.getBoolean("is_two_way"),
                        rs.getString("priority_level"), rs.getInt("line_no"), rs.getString("remark"),
                        rs.getInt("version")),
                args.toArray());
    }

    @Transactional
    public BomDtos.BomAlternateRow add(Long itemObjectId, BomDtos.BomAlternateSaveReq req, Long userId) {
        itemService.requireItem(itemObjectId);
        itemService.requireItem(req.alternateId());
        if (itemObjectId.equals(req.alternateId())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能添加自身为备选件");
        }
        Integer used = jdbcTemplate.query(
                "SELECT COUNT(1) FROM bom_line WHERE child_object_id = ? AND deleted = false",
                rs -> rs.next() ? rs.getInt(1) : 0, itemObjectId);
        if (used == null || used == 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该零部件未出现在任何 BOM 中，不能维护替代件");
        }
        String view = normalizeView(req.viewType());
        Long duplicate = jdbcTemplate.query("""
                        SELECT COUNT(1) FROM bom_alternate
                         WHERE item_object_id = ? AND alternate_object_id = ? AND view_type = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getLong(1) : 0L, itemObjectId, req.alternateId(), view);
        if (duplicate != null && duplicate > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该备选件已存在，不能重复添加");
        }
        OffsetDateTime now = OffsetDateTime.now();
        BomAlternate row = new BomAlternate();
        row.setId(IdWorker.getId());
        row.setItemObjectId(itemObjectId);
        row.setAlternateObjectId(req.alternateId());
        row.setViewType(view);
        row.setIsGlobal(Boolean.TRUE.equals(req.isGlobal()));
        row.setIsTwoWay(Boolean.TRUE.equals(req.isTwoWay()));
        row.setPriorityLevel(trimToNull(req.priorityLevel()));
        row.setLineNo(nextLineNo(itemObjectId));
        row.setRemark(trimToNull(req.remark()));
        row.setVersion(0);
        row.setDeleted(false);
        row.setCreatedBy(userId);
        row.setCreatedAt(now);
        row.setUpdatedBy(userId);
        row.setUpdatedAt(now);
        alternateMapper.insert(row);
        return rowById(row.getId());
    }

    @Transactional
    public BomDtos.BomAlternateRow update(Long itemObjectId, Long alternateRowId,
                                          BomDtos.BomAlternateUpdateReq req, Long userId) {
        BomAlternate row = requireRow(itemObjectId, alternateRowId);
        BomAlternate update = new BomAlternate();
        update.setId(row.getId());
        update.setVersion(row.getVersion());
        update.setIsGlobal(req.isGlobal() == null ? row.getIsGlobal() : req.isGlobal());
        update.setIsTwoWay(req.isTwoWay() == null ? row.getIsTwoWay() : req.isTwoWay());
        update.setPriorityLevel(req.priorityLevel() == null ? null : trimToNull(req.priorityLevel()));
        update.setLineNo(req.lineNo() == null ? row.getLineNo() : req.lineNo());
        update.setRemark(req.remark() == null ? null : trimToNull(req.remark()));
        update.setUpdatedBy(userId);
        update.setUpdatedAt(OffsetDateTime.now());
        if (alternateMapper.updateById(update) == 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该替代关系已被他人修改，请刷新后重试");
        }
        return rowById(row.getId());
    }

    @Transactional
    public void remove(Long itemObjectId, Long alternateRowId) {
        BomAlternate row = requireRow(itemObjectId, alternateRowId);
        alternateMapper.deleteById(row.getId());
    }

    // ------------------------------------------------------------ 内部

    private BomDtos.BomAlternateRow rowById(Long id) {
        return jdbcTemplate.query("""
                        SELECT a.id, a.item_object_id, a.alternate_object_id, a.view_type, a.is_global,
                               a.is_two_way, a.priority_level, a.line_no, a.remark, a.version,
                               o.code, o.name, m.item_type, m.specification, m.unit
                          FROM bom_alternate a
                          JOIN plm_object o ON o.id = a.alternate_object_id
                           AND o.is_latest = true AND o.deleted = false
                          LEFT JOIN item_master m ON m.object_id = a.alternate_object_id
                         WHERE a.id = ? AND a.deleted = false
                        """,
                rs -> rs.next() ? new BomDtos.BomAlternateRow(rs.getLong("id"), rs.getLong("item_object_id"),
                        rs.getLong("alternate_object_id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("item_type"), rs.getString("specification"), rs.getString("unit"),
                        rs.getString("view_type"), rs.getBoolean("is_global"), rs.getBoolean("is_two_way"),
                        rs.getString("priority_level"), rs.getInt("line_no"), rs.getString("remark"),
                        rs.getInt("version")) : null,
                id);
    }

    private BomAlternate requireRow(Long itemObjectId, Long alternateRowId) {
        BomAlternate row = alternateMapper.selectById(alternateRowId);
        if (row == null || !itemObjectId.equals(row.getItemObjectId())) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "替代关系不存在：" + alternateRowId);
        }
        return row;
    }

    private int nextLineNo(Long itemObjectId) {
        Integer max = jdbcTemplate.query("""
                        SELECT COALESCE(MAX(line_no), 0) FROM bom_alternate
                         WHERE item_object_id = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getInt(1) : 0, itemObjectId);
        return (max == null ? 0 : max) + LINE_STEP;
    }

    private String normalizeView(String viewType) {
        return BomViews.normalize(viewType);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
