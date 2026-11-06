package com.nexplm.platform.supplier.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.supplier.api.SupSupplierDtos;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 供应商主数据与零件-供应商关系（P8-4）。
 *
 * <p>刻意的取舍：</p>
 * <ul>
 *   <li><b>不建在 plm_object 里</b>：供应商不需要版本与生命周期状态机，塞进通用对象层
 *       只会让"供应商升版"这种无意义操作出现在界面上；</li>
 *   <li><b>零件关系冗余 item_code/item_name</b>：零件对象改名或被删后，关系表仍能看出
 *       当年挂的是哪个件（审计/追溯场景比"实时 join 拿不到"更重要）；</li>
 *   <li><b>同一零件的首选供应商唯一</b>：多源供货允许多条关系，但"首选"只能一个，
 *       由服务层在同一事务里互斥（跨行条件用库约束表达代价过高）；</li>
 *   <li><b>有在用零件关系的供应商不能删</b>：否则零件那边会留下悬空关系。</li>
 * </ul>
 */
@Service
public class SupplierService {

    private static final Set<String> CATEGORIES =
            Set.of("MATERIAL", "OUTSOURCE", "SERVICE", "LOGISTICS");
    private static final Set<String> STATUSES =
            Set.of("PENDING", "QUALIFIED", "SUSPENDED", "DISQUALIFIED");
    private static final Set<String> SUPPLY_TYPES = Set.of("BUY", "OUTSOURCE", "DOUBLE");
    private static final Set<String> PART_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private static final String SUPPLIER_SELECT = """
            SELECT s.*, o.name AS org_name,
                   (SELECT COUNT(1) FROM sup_part p
                     WHERE p.supplier_id = s.id AND p.deleted = false) AS part_count,
                   (SELECT COUNT(1) FROM sup_part p
                     WHERE p.supplier_id = s.id AND p.deleted = false AND p.preferred = true)
                       AS preferred_count
              FROM sup_supplier s
              LEFT JOIN sys_org o ON o.id = s.org_id
            """;

    private static final String PART_SELECT = """
            SELECT p.*, s.code AS supplier_code, s.name AS supplier_name
              FROM sup_part p
              JOIN sup_supplier s ON s.id = p.supplier_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public SupplierService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------------------------------------------------------------- 供应商

    public PageResult<SupSupplierDtos.SupplierRow> page(Long companyId, String keyword,
                                                        String category, String status,
                                                        Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE s.company_id = ? AND s.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (s.code ILIKE ? OR s.name ILIKE ? OR s.short_name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND s.category = ?");
            args.add(normalize(category, CATEGORIES, "供应商类别"));
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND s.status = ?");
            args.add(normalize(status, STATUSES, "供应商状态"));
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sup_supplier s" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SupSupplierDtos.SupplierRow> items = jdbcTemplate.query(
                SUPPLIER_SELECT + where + " ORDER BY s.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> supplierRow(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public SupSupplierDtos.SupplierDetail detail(Long id, Long companyId) {
        List<SupSupplierDtos.SupplierRow> rows = jdbcTemplate.query(
                SUPPLIER_SELECT + " WHERE s.id = ? AND s.company_id = ? AND s.deleted = false",
                (rs, rowNum) -> supplierRow(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_NOT_FOUND, "供应商不存在：" + id);
        }
        SupSupplierDtos.SupplierRow row = rows.get(0);
        java.util.Map<String, Object> text = jdbcTemplate.queryForMap("""
                SELECT email, address, tax_no, currency, payment_terms, remark
                  FROM sup_supplier WHERE id = ?
                """, id);
        SupSupplierDtos.SupplierRow full = row;
        return new SupSupplierDtos.SupplierDetail(full.id(), full.code(), full.name(),
                full.shortName(), full.category(), full.status(), full.contact(), full.phone(),
                str(text.get("email")), str(text.get("address")), str(text.get("tax_no")),
                str(text.get("currency")), str(text.get("payment_terms")),
                full.orgId(), full.orgName(), str(text.get("remark")),
                full.partCount(), full.preferredCount(), parts(id, companyId));
    }

    @Transactional
    public SupSupplierDtos.SupplierDetail create(SupSupplierDtos.SupplierSaveReq req, Long companyId,
                                                 Long userId) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sup_supplier (id, company_id, code, name, short_name, category,
                                                  status, contact, phone, email, address, tax_no,
                                                  currency, payment_terms, org_id, remark,
                                                  created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, nextCode(), req.name().trim(), blankToNull(req.shortName()),
                normalize(req.category() == null ? "MATERIAL" : req.category(), CATEGORIES,
                        "供应商类别"),
                normalize(req.status() == null ? "PENDING" : req.status(), STATUSES, "供应商状态"),
                blankToNull(req.contact()), blankToNull(req.phone()), blankToNull(req.email()),
                blankToNull(req.address()), blankToNull(req.taxNo()),
                req.currency() == null || req.currency().isBlank() ? "CNY" : req.currency().trim(),
                blankToNull(req.paymentTerms()), req.orgId(), blankToNull(req.remark()),
                userId, userId);
        return detail(id, companyId);
    }

    @Transactional
    public SupSupplierDtos.SupplierDetail update(Long id, SupSupplierDtos.SupplierSaveReq req,
                                                 Long companyId, Long userId) {
        requireSupplier(id, companyId);
        jdbcTemplate.update("""
                        UPDATE sup_supplier
                           SET name = ?, short_name = ?, category = ?, status = ?, contact = ?,
                               phone = ?, email = ?, address = ?, tax_no = ?, currency = ?,
                               payment_terms = ?, org_id = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), blankToNull(req.shortName()),
                normalize(req.category() == null ? "MATERIAL" : req.category(), CATEGORIES,
                        "供应商类别"),
                normalize(req.status() == null ? "PENDING" : req.status(), STATUSES, "供应商状态"),
                blankToNull(req.contact()), blankToNull(req.phone()), blankToNull(req.email()),
                blankToNull(req.address()), blankToNull(req.taxNo()),
                req.currency() == null || req.currency().isBlank() ? "CNY" : req.currency().trim(),
                blankToNull(req.paymentTerms()), req.orgId(), blankToNull(req.remark()),
                userId, id, companyId);
        return detail(id, companyId);
    }

    /** 有在用零件关系时禁止删除，避免零件侧出现悬空关系。 */
    @Transactional
    public void delete(Long id, Long companyId) {
        requireSupplier(id, companyId);
        Integer active = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sup_part
                 WHERE supplier_id = ? AND company_id = ? AND deleted = false AND status = 'ACTIVE'
                """, Integer.class, id, companyId);
        if (active != null && active > 0) {
            throw BizException.of(ErrorCode.SUP_IN_USE,
                    "该供应商还有 " + active + " 条在用零件关系，请先停用或删除关系");
        }
        jdbcTemplate.update("""
                UPDATE sup_supplier SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_part WHERE supplier_id = ?", id);
    }

    public List<SupSupplierDtos.PartRow> parts(Long supplierId, Long companyId) {
        return jdbcTemplate.query(PART_SELECT
                        + " WHERE p.supplier_id = ? AND p.company_id = ? AND p.deleted = false"
                        + " ORDER BY p.preferred DESC, p.updated_at DESC",
                (rs, rowNum) -> partRow(rs), supplierId, companyId);
    }

    /** 按零件反查供应商（多源询价场景）。 */
    public List<SupSupplierDtos.PartRow> partsByItem(Long companyId, String objectType,
                                                     Long objectId) {
        String type = objectType == null || objectType.isBlank() ? "PdmItem" : objectType.trim();
        return jdbcTemplate.query(PART_SELECT
                        + " WHERE p.company_id = ? AND p.object_type = ? AND p.object_id = ?"
                        + " AND p.deleted = false AND p.status = 'ACTIVE'"
                        + " AND s.deleted = false ORDER BY p.preferred DESC, s.code",
                (rs, rowNum) -> partRow(rs), companyId, type, objectId);
    }

    // ---------------------------------------------------------------- 零件关系

    @Transactional
    public SupSupplierDtos.SupplierDetail addPart(Long supplierId,
                                                  SupSupplierDtos.PartSaveReq req, Long companyId,
                                                  Long userId) {
        requireSupplier(supplierId, companyId);
        String objectType = normalizeObjectType(req.objectType());
        Object[] item = resolveItem(objectType, req.objectId());
        boolean preferred = Boolean.TRUE.equals(req.preferred());
        try {
            jdbcTemplate.update("""
                            INSERT INTO sup_part (id, company_id, supplier_id, object_type, object_id,
                                                  item_code, item_name, supply_type, preferred,
                                                  lead_time_days, moq, price, currency, status,
                                                  remark, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, supplierId, objectType, req.objectId(),
                    item[0], item[1],
                    normalize(req.supplyType() == null ? "BUY" : req.supplyType(), SUPPLY_TYPES,
                            "供货类型"),
                    preferred, req.leadTimeDays(), req.moq(), req.price(),
                    req.currency() == null || req.currency().isBlank() ? "CNY"
                            : req.currency().trim(),
                    normalize(req.status() == null ? "ACTIVE" : req.status(), PART_STATUSES,
                            "关系状态"),
                    blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SUP_INVALID, "该供应商已经关联了这个零件");
        }
        if (preferred) {
            clearOtherPreferred(companyId, objectType, req.objectId(), supplierId);
        }
        return detail(supplierId, companyId);
    }

    @Transactional
    public SupSupplierDtos.SupplierDetail updatePart(Long partId,
                                                     SupSupplierDtos.PartSaveReq req, Long companyId,
                                                     Long userId) {
        var part = requirePart(partId, companyId);
        String objectType = req.objectType() == null || req.objectType().isBlank()
                ? str(part.get("object_type")) : normalizeObjectType(req.objectType());
        Object[] item = resolveItem(objectType, req.objectId());
        boolean preferred = Boolean.TRUE.equals(req.preferred());
        try {
            jdbcTemplate.update("""
                            UPDATE sup_part
                               SET object_type = ?, object_id = ?, item_code = ?, item_name = ?,
                                   supply_type = ?, preferred = ?, lead_time_days = ?, moq = ?,
                                   price = ?, currency = ?, status = ?, remark = ?,
                                   updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ?
                            """,
                    objectType, req.objectId(), item[0], item[1],
                    normalize(req.supplyType() == null ? "BUY" : req.supplyType(), SUPPLY_TYPES,
                            "供货类型"),
                    preferred, req.leadTimeDays(), req.moq(), req.price(),
                    req.currency() == null || req.currency().isBlank() ? "CNY"
                            : req.currency().trim(),
                    normalize(req.status() == null ? "ACTIVE" : req.status(), PART_STATUSES,
                            "关系状态"),
                    blankToNull(req.remark()), userId, partId, companyId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SUP_INVALID, "该供应商已经关联了这个零件");
        }
        if (preferred) {
            clearOtherPreferred(companyId, objectType, req.objectId(),
                    longOf(part.get("supplier_id")));
        }
        return detail(longOf(part.get("supplier_id")), companyId);
    }

    @Transactional
    public void removePart(Long partId, Long companyId) {
        var part = requirePart(partId, companyId);
        jdbcTemplate.update("""
                UPDATE sup_part SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, partId, companyId);
        jdbcTemplate.update("""
                UPDATE sup_supplier SET updated_at = now() WHERE id = ?
                """, longOf(part.get("supplier_id")));
    }

    // ---------------------------------------------------------------- 内部

    /** 首选供应商互斥：同一零件的其它关系取消首选。 */
    private void clearOtherPreferred(Long companyId, String objectType, Long objectId,
                                     Long keepSupplierId) {
        jdbcTemplate.update("""
                        UPDATE sup_part SET preferred = false, updated_at = now()
                         WHERE company_id = ? AND object_type = ? AND object_id = ?
                           AND supplier_id <> ? AND preferred = true AND deleted = false
                        """, companyId, objectType, objectId, keepSupplierId);
    }

    /** 零件对象必须真实存在；同时把编码/名称冗余进关系表。 */
    private Object[] resolveItem(String objectType, Long objectId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT code, name FROM plm_object
                 WHERE id = ? AND object_type = ? AND deleted = false
                """, objectId, objectType);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_INVALID,
                    "零件对象不存在：" + objectType + "/" + objectId);
        }
        return new Object[]{str(rows.get(0).get("code")), str(rows.get(0).get("name"))};
    }

    private SupSupplierDtos.SupplierRow supplierRow(java.sql.ResultSet rs)
            throws java.sql.SQLException {
        return new SupSupplierDtos.SupplierRow(rs.getLong("id"), rs.getString("code"),
                rs.getString("name"), rs.getString("short_name"), rs.getString("category"),
                rs.getString("status"), rs.getString("contact"), rs.getString("phone"),
                (Long) rs.getObject("org_id"), rs.getString("org_name"),
                rs.getInt("part_count"), rs.getInt("preferred_count"),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private SupSupplierDtos.PartRow partRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupSupplierDtos.PartRow(rs.getLong("id"), rs.getLong("supplier_id"),
                rs.getString("supplier_code"), rs.getString("supplier_name"),
                rs.getString("object_type"), rs.getLong("object_id"), rs.getString("item_code"),
                rs.getString("item_name"), rs.getString("supply_type"), rs.getBoolean("preferred"),
                (Integer) rs.getObject("lead_time_days"), rs.getBigDecimal("moq"),
                rs.getBigDecimal("price"), rs.getString("currency"), rs.getString("status"),
                rs.getString("remark"), rs.getObject("updated_at", OffsetDateTime.class));
    }

    private java.util.Map<String, Object> requireSupplier(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name FROM sup_supplier
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_NOT_FOUND, "供应商不存在：" + id);
        }
        return rows.get(0);
    }

    private java.util.Map<String, Object> requirePart(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, supplier_id, object_type, object_id FROM sup_part
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_NOT_FOUND, "零件关系不存在：" + id);
        }
        return rows.get(0);
    }

    private String nextCode() {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'SupSupplier', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("SUP-%06d", no == null ? 1L : no);
    }

    private static String normalizeObjectType(String objectType) {
        return objectType == null || objectType.isBlank() ? "PdmItem" : objectType.trim();
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.SUP_INVALID,
                    "未知" + label + "：" + value + "（可选 " + String.join("/", allowed) + "）");
        }
        return normalized;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
