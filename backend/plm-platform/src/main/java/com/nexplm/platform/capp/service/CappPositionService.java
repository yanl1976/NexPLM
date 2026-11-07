package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappPositionDtos;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工厂结构（工位）与消耗定义，对齐旧系统 {@code PositionManager} + {@code ConsumptionStation}。
 *
 * <p>旧系统工位是树（{@code CEID/PEID} 自关联），层级靠 {@code POSITIONLEVEL} 区分；
 * 这里保留同样的父子关系，但把层级做成白名单（工厂/车间/产线/工位/工作地），
 * 避免旧系统那种"层级字段随手填字符串"的脏数据。</p>
 *
 * <p>消耗定义挂在工位上（旧 {@code PARENTCODE}），并保留"适用全部路线"、
 * "适配产品"等旧字段——它们决定工序消耗怎么参与 BOM 反算。</p>
 */
@Service
public class CappPositionService {

    private static final Set<String> LEVELS =
            Set.of("PLANT", "WORKSHOP", "LINE", "STATION", "WORKCENTER");

    private final JdbcTemplate jdbcTemplate;
    private final CodeRuleService codeRuleService;

    public CappPositionService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
    }

    // ------------------------------------------------------------ 工位树

    public List<CappPositionDtos.PositionNode> tree(Long companyId) {
        List<CappPositionDtos.PositionNode> flat = jdbcTemplate.query("""
                SELECT id, code, name, parent_id, position_level, logistics_unit, capacity, shifts,
                       enabled, remark
                  FROM capp_position WHERE company_id = ? AND deleted = false
                 ORDER BY position_level, code
                """, (rs, i) -> new CappPositionDtos.PositionNode(rs.getLong("id"),
                rs.getString("code"), rs.getString("name"),
                (Long) rs.getObject("parent_id"), rs.getString("position_level"),
                rs.getString("logistics_unit"), rs.getBigDecimal("capacity"),
                (Integer) rs.getObject("shifts"), rs.getBoolean("enabled"),
                rs.getString("remark"), new ArrayList<>()), companyId);

        Map<Long, CappPositionDtos.PositionNode> byId = new LinkedHashMap<>();
        flat.forEach(node -> byId.put(node.id(), node));
        List<CappPositionDtos.PositionNode> roots = new ArrayList<>();
        for (CappPositionDtos.PositionNode node : flat) {
            CappPositionDtos.PositionNode parent =
                    node.parentId() == null ? null : byId.get(node.parentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        roots.sort(Comparator.comparing(CappPositionDtos.PositionNode::code));
        return roots;
    }

    @Transactional
    public CappPositionDtos.PositionNode create(CappPositionDtos.PositionReq req, Long companyId, Long userId) {
        String level = normalizeLevel(req.positionLevel());
        String code = req.code() == null || req.code().isBlank()
                ? codeRuleService.next("CappPosition") : req.code().trim();
        requireUniqueCode(companyId, code, null);
        if (req.parentId() != null) {
            requirePosition(req.parentId(), companyId);
        }
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO capp_position (id, company_id, code, name, parent_id, position_level,
                       logistics_unit, capacity, shifts, remark, enabled, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, companyId, code, req.name(), req.parentId(), level, req.logisticsUnit(),
                req.capacity(), req.shifts(), req.remark(),
                req.enabled() == null || req.enabled(), userId);
        return get(id, companyId);
    }

    @Transactional
    public CappPositionDtos.PositionNode update(Long id, CappPositionDtos.PositionReq req, Long companyId) {
        CappPositionDtos.PositionNode old = get(id, companyId);
        if (req.parentId() != null && req.parentId().equals(id)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "上级工位不能是自己");
        }
        if (req.parentId() != null) {
            requirePosition(req.parentId(), companyId);
            requireNotDescendant(id, req.parentId(), companyId);
        }
        String code = req.code() == null || req.code().isBlank() ? old.code() : req.code().trim();
        if (!code.equals(old.code())) {
            requireUniqueCode(companyId, code, id);
        }
        jdbcTemplate.update("""
                UPDATE capp_position SET code = ?, name = ?, parent_id = ?, position_level = ?,
                       logistics_unit = ?, capacity = ?, shifts = ?, remark = ?, enabled = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, code, req.name(), req.parentId(), normalizeLevel(req.positionLevel()),
                req.logisticsUnit(), req.capacity(), req.shifts(), req.remark(),
                req.enabled() == null ? old.enabled() : req.enabled(), id, companyId);
        return get(id, companyId);
    }

    /** 删除：有下级或有消耗定义时拒绝，避免把工艺数据挂空。 */
    @Transactional
    public void delete(Long id, Long companyId) {
        get(id, companyId);
        Long children = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_position WHERE parent_id = ? AND deleted = false", Long.class, id);
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该工位下还有下级节点，不能删除");
        }
        Long consumptions = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_consumption WHERE position_id = ? AND deleted = false", Long.class, id);
        if (consumptions != null && consumptions > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该工位下还有消耗定义，不能删除");
        }
        jdbcTemplate.update("UPDATE capp_position SET deleted = true, updated_at = now() WHERE id = ?", id);
    }

    private CappPositionDtos.PositionNode get(Long id, Long companyId) {
        List<CappPositionDtos.PositionNode> rows = jdbcTemplate.query("""
                SELECT id, code, name, parent_id, position_level, logistics_unit, capacity, shifts,
                       enabled, remark
                  FROM capp_position WHERE id = ? AND company_id = ? AND deleted = false
                """, (rs, i) -> new CappPositionDtos.PositionNode(rs.getLong("id"),
                rs.getString("code"), rs.getString("name"), (Long) rs.getObject("parent_id"),
                rs.getString("position_level"), rs.getString("logistics_unit"),
                rs.getBigDecimal("capacity"), (Integer) rs.getObject("shifts"),
                rs.getBoolean("enabled"), rs.getString("remark"), new ArrayList<>()), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "工位不存在");
        }
        return rows.get(0);
    }

    private void requirePosition(Long id, Long companyId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_position WHERE id = ? AND company_id = ? AND deleted = false",
                Long.class, id, companyId);
        if (count == null || count == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "上级工位不存在");
        }
    }

    /** 防环：新上级不能是自己的后代。 */
    private void requireNotDescendant(Long id, Long candidateParentId, Long companyId) {
        Long cursor = candidateParentId;
        int guard = 0;
        while (cursor != null && guard++ < 64) {
            if (cursor.equals(id)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能把下级工位设为上级（会形成环）");
            }
            cursor = jdbcTemplate.query("SELECT parent_id FROM capp_position WHERE id = ?",
                    rs -> rs.next() ? (Long) rs.getObject(1) : null, cursor);
        }
    }

    private void requireUniqueCode(Long companyId, String code, Long excludeId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM capp_position
                 WHERE company_id = ? AND code = ? AND deleted = false AND (?::bigint IS NULL OR id <> ?)
                """, Long.class, companyId, code, excludeId, excludeId);
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "工位编码已存在：" + code);
        }
    }

    private static String normalizeLevel(String level) {
        String value = level == null || level.isBlank() ? "STATION" : level.trim().toUpperCase();
        if (!LEVELS.contains(value)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "工位层级只能是 PLANT/WORKSHOP/LINE/STATION/WORKCENTER");
        }
        return value;
    }

    // ------------------------------------------------------------ 消耗定义

    public List<CappPositionDtos.ConsumptionRow> consumptions(Long companyId, Long positionId, String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.code, c.name, c.position_id, p.code AS position_code, p.name AS position_name,
                       c.object_id, c.item_code, c.item_name, c.qty, c.unit, c.ref_item,
                       c.suitability, c.fit_product, c.all_route, c.is_primary, c.remark, c.created_at
                  FROM capp_consumption c
                  LEFT JOIN capp_position p ON p.id = c.position_id
                 WHERE c.company_id = ? AND c.deleted = false
                """);
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (positionId != null) {
            sql.append(" AND c.position_id = ?");
            args.add(positionId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (lower(c.code) LIKE ? OR lower(c.name) LIKE ? OR lower(c.item_code) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY c.code");
        return jdbcTemplate.query(sql.toString(), (rs, i) -> new CappPositionDtos.ConsumptionRow(
                rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                (Long) rs.getObject("position_id"), rs.getString("position_code"),
                rs.getString("position_name"), (Long) rs.getObject("object_id"),
                rs.getString("item_code"), rs.getString("item_name"), rs.getBigDecimal("qty"),
                rs.getString("unit"), rs.getBoolean("ref_item"), rs.getString("suitability"),
                rs.getString("fit_product"), rs.getBoolean("all_route"), rs.getBoolean("is_primary"),
                rs.getString("remark"), time(rs.getTimestamp("created_at"))), args.toArray());
    }

    @Transactional
    public Long createConsumption(CappPositionDtos.ConsumptionReq req, Long companyId, Long userId) {
        if (req.positionId() != null) {
            requirePosition(req.positionId(), companyId);
        }
        String code = req.code() == null || req.code().isBlank()
                ? codeRuleService.next("CappConsumption") : req.code().trim();
        Long dup = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_consumption WHERE company_id = ? AND code = ? AND deleted = false",
                Long.class, companyId, code);
        if (dup != null && dup > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "消耗定义编码已存在：" + code);
        }
        // 物料按 object_id 反查编码/名称，避免前端传错造成"编码与 id 不一致"
        String itemCode = req.itemCode();
        String itemName = req.itemName();
        if (req.objectId() != null) {
            List<Map<String, Object>> items = jdbcTemplate.queryForList(
                    "SELECT code, name FROM plm_object WHERE id = ? AND deleted = false", req.objectId());
            if (items.isEmpty()) {
                throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "消耗物料不存在");
            }
            itemCode = String.valueOf(items.get(0).get("code"));
            itemName = String.valueOf(items.get(0).get("name"));
        }
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO capp_consumption (id, company_id, code, name, position_id, object_id,
                       item_code, item_name, qty, unit, ref_item, suitability, fit_product,
                       all_route, is_primary, remark, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, companyId, code, req.name(), req.positionId(), req.objectId(), itemCode,
                itemName, req.qty() == null ? BigDecimal.ONE : req.qty(), req.unit(),
                req.refItem() == null || req.refItem(), req.suitability(), req.fitProduct(),
                req.allRoute() != null && req.allRoute(), req.isPrimary() != null && req.isPrimary(),
                req.remark(), userId);
        return id;
    }

    @Transactional
    public void updateConsumption(Long id, CappPositionDtos.ConsumptionReq req, Long companyId) {
        Long exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_consumption WHERE id = ? AND company_id = ? AND deleted = false",
                Long.class, id, companyId);
        if (exists == null || exists == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "消耗定义不存在");
        }
        if (req.positionId() != null) {
            requirePosition(req.positionId(), companyId);
        }
        jdbcTemplate.update("""
                UPDATE capp_consumption SET name = ?, position_id = ?, object_id = ?, item_code = ?,
                       item_name = ?, qty = ?, unit = ?, ref_item = ?, suitability = ?, fit_product = ?,
                       all_route = ?, is_primary = ?, remark = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, req.name(), req.positionId(), req.objectId(), req.itemCode(), req.itemName(),
                req.qty() == null ? BigDecimal.ONE : req.qty(), req.unit(),
                req.refItem() == null || req.refItem(), req.suitability(), req.fitProduct(),
                req.allRoute() != null && req.allRoute(), req.isPrimary() != null && req.isPrimary(),
                req.remark(), id, companyId);
    }

    @Transactional
    public void deleteConsumption(Long id, Long companyId) {
        int deleted = jdbcTemplate.update(
                "UPDATE capp_consumption SET deleted = true, updated_at = now() WHERE id = ? AND company_id = ?",
                id, companyId);
        if (deleted == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "消耗定义不存在");
        }
    }

    private static OffsetDateTime time(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }
}
