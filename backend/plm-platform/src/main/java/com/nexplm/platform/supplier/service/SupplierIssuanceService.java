package com.nexplm.platform.supplier.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.supplier.api.SupIssuanceDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 供应商数据发放（P8-5-3）：受控清单 + 打包导出 + 回执登记。
 *
 * <p>刻意的取舍：</p>
 * <ul>
 *   <li><b>先建发放单再打包</b>：只给下载按钮的话，事后无法回答"这家供应商当年拿的是哪一版"。
 *       发放单固定清单快照（编码/名称/版本），状态 DRAFT →SENT→ RECEIVED；</li>
 *   <li><b>清单冗余编码名称版本</b>：零件后续升版改名，发放单仍能还原当时发出的内容；</li>
 *   <li><b>导出 = 主文件 + manifest.csv</b>：与文档批量下载同一口径（每个零件取主文件，
 *       没有文件的零件只在清单里出现，不生成空条目）；</li>
 *   <li><b>回执只登记 file_id</b>：发放单不是工程对象，不需要版本链，
 *       文件由 {@code POST /files} 先上传（不绑对象），这里只存 fileId + 文件名；</li>
 *   <li><b>未做外部账号</b>：当前"协同上传"由内部人员代登记回执，供应商门户属后续阶段。</li>
 * </ul>
 */
@Service
public class SupplierIssuanceService {

    private static final Set<String> TYPES = Set.of("DESIGN", "QUALITY", "OTHER");
    private static final String ILLEGAL_NAME = "[\\\\/:*?\"<>|]";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String ISSUANCE_SELECT = """
            SELECT a.*, s.code AS supplier_code, s.name AS supplier_name,
                   (SELECT COUNT(1) FROM sup_issuance_item i
                     WHERE i.issuance_id = a.id) AS item_count
              FROM sup_issuance a
              JOIN sup_supplier s ON s.id = a.supplier_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FileService fileService;

    public SupplierIssuanceService(JdbcTemplate jdbcTemplate, FileService fileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileService = fileService;
    }

    public PageResult<SupIssuanceDtos.IssuanceRow> page(Long companyId, Long supplierId,
                                                        String status, String type,
                                                        Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE a.company_id = ? AND a.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (supplierId != null) {
            where.append(" AND a.supplier_id = ?");
            args.add(supplierId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND a.status = ?");
            args.add(normalize(status, Set.of("DRAFT", "SENT", "RECEIVED", "CANCELLED"), "发放状态"));
        }
        if (type != null && !type.isBlank()) {
            where.append(" AND a.type = ?");
            args.add(normalize(type, TYPES, "发放类型"));
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sup_issuance a" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SupIssuanceDtos.IssuanceRow> items = jdbcTemplate.query(
                ISSUANCE_SELECT + where + " ORDER BY a.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> row(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public List<SupIssuanceDtos.IssuanceRow> bySupplier(Long companyId, Long supplierId) {
        return jdbcTemplate.query(ISSUANCE_SELECT
                        + " WHERE a.company_id = ? AND a.supplier_id = ? AND a.deleted = false"
                        + " ORDER BY a.created_at DESC",
                (rs, rowNum) -> row(rs), companyId, supplierId);
    }

    public SupIssuanceDtos.IssuanceDetail detail(Long id, Long companyId) {
        List<SupIssuanceDtos.IssuanceRow> rows = jdbcTemplate.query(
                ISSUANCE_SELECT + " WHERE a.id = ? AND a.company_id = ? AND a.deleted = false",
                (rs, rowNum) -> row(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_NOT_FOUND, "数据发放单不存在：" + id);
        }
        SupIssuanceDtos.IssuanceRow r = rows.get(0);
        Map<String, Object> extra = jdbcTemplate.queryForMap("""
                SELECT remark, receipt_file_id, receipt_file_name, receipt_note
                  FROM sup_issuance WHERE id = ?
                """, id);
        Object receiptFileId = extra.get("receipt_file_id");
        Long receiptFileIdValue = receiptFileId instanceof Number number
                ? number.longValue() : null;
        return new SupIssuanceDtos.IssuanceDetail(r.id(), r.code(), r.supplierId(),
                r.supplierCode(), r.supplierName(), r.title(), r.type(), r.status(),
                str(extra.get("remark")), receiptFileIdValue,
                str(extra.get("receipt_file_name")), str(extra.get("receipt_note")),
                r.sentAt(), r.receivedAt(), items(id));
    }

    @Transactional
    public SupIssuanceDtos.IssuanceDetail create(SupIssuanceDtos.IssuanceSaveReq req,
                                                 Long companyId, Long userId) {
        requireSupplier(req.supplierId(), companyId);
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sup_issuance (id, company_id, code, supplier_id, title, type,
                                                  status, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?)
                        """,
                id, companyId, nextCode(), req.supplierId(), req.title().trim(),
                normalize(req.type() == null ? "DESIGN" : req.type(), TYPES, "发放类型"),
                blankToNull(req.remark()), userId, userId);
        writeItems(id, companyId, req.items());
        return detail(id, companyId);
    }

    @Transactional
    public SupIssuanceDtos.IssuanceDetail update(Long id, SupIssuanceDtos.IssuanceSaveReq req,
                                                 Long companyId, Long userId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        requireStatus(issuance, "DRAFT", "只有草稿状态的发放单可以修改");
        jdbcTemplate.update("""
                        UPDATE sup_issuance
                           SET title = ?, type = ?, remark = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.title().trim(),
                normalize(req.type() == null ? "DESIGN" : req.type(), TYPES, "发放类型"),
                blankToNull(req.remark()), userId, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_issuance_item WHERE issuance_id = ?", id);
        writeItems(id, companyId, req.items());
        return detail(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        requireStatus(issuance, "DRAFT", "只有草稿状态的发放单可以删除");
        jdbcTemplate.update("""
                UPDATE sup_issuance SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_issuance_item WHERE issuance_id = ?", id);
    }

    @Transactional
    public SupIssuanceDtos.IssuanceDetail send(Long id, Long companyId, Long userId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        requireStatus(issuance, "DRAFT", "只有草稿状态的发放单可以发出");
        jdbcTemplate.update("""
                UPDATE sup_issuance
                   SET status = 'SENT', sent_by = ?, sent_at = now(), updated_by = ?,
                       updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, userId, id, companyId);
        return detail(id, companyId);
    }

    /** 回执登记：只能对已发出的发放单登记，登记后置 RECEIVED（不可重复登记）。 */
    @Transactional
    public SupIssuanceDtos.IssuanceDetail receipt(Long id, SupIssuanceDtos.ReceiptReq req,
                                                  Long companyId, Long userId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        requireStatus(issuance, "SENT", "只有已发出的发放单可以登记回执");
        fileService.meta(req.fileId());
        String fileName = req.fileName() == null || req.fileName().isBlank()
                ? "receipt-" + req.fileId() : req.fileName().trim();
        jdbcTemplate.update("""
                UPDATE sup_issuance
                   SET status = 'RECEIVED', receipt_file_id = ?, receipt_file_name = ?,
                       receipt_note = ?, received_by = ?, received_at = now(),
                       updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, req.fileId(), fileName, blankToNull(req.note()), userId, userId, id, companyId);
        return detail(id, companyId);
    }

    /**
     * 打包导出：manifest.csv + 每个零件的主文件（没有文件的零件只进清单）。
     *
     * <p>与文档批量下载同口径：主文件优先，取不到主文件就取第一个文件，重名自动加序号。</p>
     */
    public byte[] exportZip(Long id, Long companyId) {
        SupIssuanceDtos.IssuanceDetail detail = detail(id, companyId);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Set<String> used = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.csv"));
            zip.write(manifest(detail).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (SupIssuanceDtos.IssuanceItemRow item : detail.items()) {
                List<FileService.FileView> files = fileService.listByOwner(item.objectId(), true);
                FileService.FileView picked = files.stream()
                        .filter(file -> FileService.ROLE_MAIN.equals(file.fileRole()))
                        .findFirst()
                        .orElse(files.isEmpty() ? null : files.get(0));
                if (picked == null) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(uniqueEntryName(used,
                        item.itemCode() + "_" + picked.fileName())));
                try (FileService.Opened opened = fileService.opened(picked.fileId())) {
                    opened.stream().transferTo(zip);
                }
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "打包导出失败：" + ex.getMessage());
        }
        return buffer.toByteArray();
    }

    // ---------------------------------------------------------------- 内部

    private String manifest(SupIssuanceDtos.IssuanceDetail detail) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("发放单号,供应商编码,供应商名称,发放日期,零件编码,零件名称,版本,是否有文件\n");
        String sentAt = detail.sentAt() == null ? "" : detail.sentAt().format(DATE);
        for (SupIssuanceDtos.IssuanceItemRow item : detail.items()) {
            boolean hasFile = !fileService.listByOwner(item.objectId(), true).isEmpty();
            csv.append(String.join(",",
                    csvCell(detail.code()), csvCell(detail.supplierCode()),
                    csvCell(detail.supplierName()), csvCell(sentAt),
                    csvCell(item.itemCode()), csvCell(item.itemName()),
                    csvCell(item.revision()), hasFile ? "是" : "否")).append('\n');
        }
        return csv.toString();
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String uniqueEntryName(Set<String> used, String raw) {
        String name = raw.replaceAll(ILLEGAL_NAME, "_");
        if (used.add(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String prefix = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int index = 2;
        while (!used.add(prefix + "(" + index + ")" + ext)) {
            index++;
        }
        return prefix + "(" + index + ")" + ext;
    }

    private void writeItems(Long issuanceId, Long companyId,
                            List<SupIssuanceDtos.IssuanceItemReq> items) {
        if (items == null || items.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID, "发放清单不能为空");
        }
        Set<Long> seen = new HashSet<>();
        int sort = 0;
        for (SupIssuanceDtos.IssuanceItemReq item : items) {
            String objectType = item.objectType() == null || item.objectType().isBlank()
                    ? "PdmItem" : item.objectType().trim();
            if (!seen.add(item.objectId())) {
                throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID,
                        "零件重复：" + item.objectId());
            }
            Map<String, Object> object = requireItem(objectType, item.objectId());
            sort++;
            jdbcTemplate.update("""
                            INSERT INTO sup_issuance_item (id, company_id, issuance_id, object_type,
                                                           object_id, item_code, item_name,
                                                           revision, sort_no)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, issuanceId, objectType, item.objectId(),
                    str(object.get("code")), str(object.get("name")),
                    object.get("revision") == null ? null : String.valueOf(object.get("revision")),
                    sort);
        }
    }

    private List<SupIssuanceDtos.IssuanceItemRow> items(Long issuanceId) {
        return jdbcTemplate.query("""
                        SELECT * FROM sup_issuance_item
                         WHERE issuance_id = ? ORDER BY sort_no, id
                        """,
                (rs, rowNum) -> new SupIssuanceDtos.IssuanceItemRow(rs.getLong("id"),
                        rs.getString("object_type"), rs.getLong("object_id"),
                        rs.getString("item_code"), rs.getString("item_name"),
                        rs.getString("revision"), rs.getInt("sort_no")),
                issuanceId);
    }

    private Map<String, Object> requireItem(String objectType, Long objectId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT code, name, revision FROM plm_object
                 WHERE id = ? AND object_type = ? AND deleted = false
                """, objectId, objectType);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID,
                    "零件对象不存在：" + objectType + "/" + objectId);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireIssuance(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM sup_issuance WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_NOT_FOUND, "数据发放单不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireSupplier(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name, status FROM sup_supplier
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_NOT_FOUND, "供应商不存在：" + id);
        }
        return rows.get(0);
    }

    private SupIssuanceDtos.IssuanceRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupIssuanceDtos.IssuanceRow(rs.getLong("id"), rs.getString("code"),
                rs.getLong("supplier_id"), rs.getString("supplier_code"),
                rs.getString("supplier_name"), rs.getString("title"), rs.getString("type"),
                rs.getString("status"), rs.getInt("item_count"),
                rs.getObject("sent_at", OffsetDateTime.class),
                rs.getObject("received_at", OffsetDateTime.class),
                rs.getString("receipt_file_name"),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private static void requireStatus(Map<String, Object> issuance, String expected,
                                      String message) {
        if (!expected.equals(str(issuance.get("status")))) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_STATE_INVALID,
                    message + "（当前 " + str(issuance.get("status")) + "）");
        }
    }

    private String nextCode() {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'SupIssuance', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("ISS-%06d", no == null ? 1L : no);
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID,
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
