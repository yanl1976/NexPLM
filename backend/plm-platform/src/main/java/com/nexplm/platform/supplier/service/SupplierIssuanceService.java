package com.nexplm.platform.supplier.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.notify.service.NotifyService;
import com.nexplm.platform.supplier.api.SupIssuanceDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

    private static final Logger log = LoggerFactory.getLogger(SupplierIssuanceService.class);

    private static final Set<String> TYPES = Set.of("DESIGN", "QUALITY", "OTHER");
    private static final Set<String> REPLY_TYPES = Set.of("ACK", "CONFIRM", "COMMIT_DATE", "REJECT");
    private static final Map<String, String> REPLY_TYPE_LABELS = Map.of(
            "ACK", "已收阅", "CONFIRM", "图纸确认", "COMMIT_DATE", "承诺交期", "REJECT", "无法满足");
    private static final String ILLEGAL_NAME = "[\\\\/:*?\"<>|]";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String ISSUANCE_SELECT = """
            SELECT a.*, s.code AS supplier_code, s.name AS supplier_name,
                   (SELECT COUNT(1) FROM sup_issuance_item i
                     WHERE i.issuance_id = a.id) AS item_count,
                   (SELECT COUNT(1) FROM sup_issuance_reply r
                     WHERE r.issuance_id = a.id AND r.deleted = false) AS reply_count
              FROM sup_issuance a
              JOIN sup_supplier s ON s.id = a.supplier_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final NotifyService notifyService;

    public SupplierIssuanceService(JdbcTemplate jdbcTemplate, FileService fileService,
                                   ObjectMapper objectMapper, NotifyService notifyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.notifyService = notifyService;
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
                SELECT remark, receipt_file_id, receipt_file_name, receipt_note,
                       supplier_group, effective_at, expire_at, is_important, read_at
                  FROM sup_issuance WHERE id = ?
                """, id);
        Object receiptFileId = extra.get("receipt_file_id");
        Long receiptFileIdValue = receiptFileId instanceof Number number
                ? number.longValue() : null;
        return new SupIssuanceDtos.IssuanceDetail(r.id(), r.code(), r.supplierId(),
                r.supplierCode(), r.supplierName(), r.title(), r.type(), r.status(),
                str(extra.get("remark")), receiptFileIdValue,
                str(extra.get("receipt_file_name")), str(extra.get("receipt_note")),
                r.sentAt(), r.receivedAt(),
                Boolean.TRUE.equals(extra.get("is_important")),
                r.readAt(), dateOf(extra.get("effective_at")), dateOf(extra.get("expire_at")),
                str(extra.get("supplier_group")),
                items(id), replies(id));
    }

    @Transactional
    public SupIssuanceDtos.IssuanceDetail create(SupIssuanceDtos.IssuanceSaveReq req,
                                                 Long companyId, Long userId) {
        requireSupplier(req.supplierId(), companyId);
        requireValidWindow(req.effectiveDate(), req.expireDate());
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sup_issuance (id, company_id, code, supplier_id, title, type,
                                                  status, remark, supplier_group, effective_at,
                                                  expire_at, is_important, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, CAST(? AS date), CAST(? AS date),
                                ?, ?, ?)
                        """,
                id, companyId, nextCode(), req.supplierId(), req.title().trim(),
                normalize(req.type() == null ? "DESIGN" : req.type(), TYPES, "发放类型"),
                blankToNull(req.remark()), blankToNull(req.supplierGroup()),
                req.effectiveDate(), req.expireDate(),
                Boolean.TRUE.equals(req.important()), userId, userId);
        writeItems(id, companyId, req.items());
        return detail(id, companyId);
    }

    @Transactional
    public SupIssuanceDtos.IssuanceDetail update(Long id, SupIssuanceDtos.IssuanceSaveReq req,
                                                 Long companyId, Long userId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        requireStatus(issuance, "DRAFT", "只有草稿状态的发放单可以修改");
        requireValidWindow(req.effectiveDate(), req.expireDate());
        jdbcTemplate.update("""
                        UPDATE sup_issuance
                           SET title = ?, type = ?, remark = ?, supplier_group = ?,
                               effective_at = CAST(? AS date), expire_at = CAST(? AS date),
                               is_important = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.title().trim(),
                normalize(req.type() == null ? "DESIGN" : req.type(), TYPES, "发放类型"),
                blankToNull(req.remark()), blankToNull(req.supplierGroup()),
                req.effectiveDate(), req.expireDate(), Boolean.TRUE.equals(req.important()),
                userId, id, companyId);
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
        requireNotExpired(issuance);
        jdbcTemplate.update("""
                UPDATE sup_issuance
                   SET status = 'SENT', sent_by = ?, sent_at = now(), updated_by = ?,
                       updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, userId, id, companyId);
        SupIssuanceDtos.IssuanceDetail sent = detail(id, companyId);
        notifyQuietly(NotifyService.EVENT_SUPPLIER_ISSUANCE_SENT,
                "supplier.issuance.sent:" + sent.code(),
                "供应商数据已发放：" + sent.code() + " " + sent.title(),
                "供应商 " + sent.supplierName() + "；共 " + sent.items().size() + " 个零件"
                        + (sent.expireDate() == null ? "" : "；有效期至 " + sent.expireDate()),
                companyId, issuance, userId);
        return sent;
    }

    /** 回执登记：只能对已发出的发放单登记，登记后置 RECEIVED（不可重复登记）。 */
    @Transactional
    public SupIssuanceDtos.IssuanceDetail receipt(Long id, SupIssuanceDtos.ReceiptReq req,
                                                  Long companyId, Long userId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        requireStatus(issuance, "SENT", "只有已发出的发放单可以登记回执");
        requireNotExpired(issuance);
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
        SupIssuanceDtos.IssuanceDetail received = detail(id, companyId);
        notifyQuietly(NotifyService.EVENT_SUPPLIER_RECEIPT_RECEIVED,
                "supplier.receipt.received:" + received.code(),
                "供应商已回执：" + received.code() + " " + received.title(),
                "回执文件：" + fileName + (blankToNull(req.note()) == null ? "" : "；说明：" + req.note()),
                companyId, issuance, userId);
        return received;
    }

    /** 标记已读（旧系统进入查看器即置 Read，CoordinationManager.js:214-228）。 */
    @Transactional
    public void markRead(Long id, Long companyId) {
        requireIssuance(id, companyId);
        jdbcTemplate.update("""
                UPDATE sup_issuance SET read_at = COALESCE(read_at, now())
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    @Transactional
    public void setImportant(Long id, Long companyId, boolean important) {
        requireIssuance(id, companyId);
        jdbcTemplate.update("""
                UPDATE sup_issuance SET is_important = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, important, id, companyId);
    }

    /**
     * 协同回复（多轮）：已发出（SENT）或已回执（RECEIVED）都可以继续回复。
     *
     * <p>与旧系统的差别：旧系统"回复"是再建一条 DISTRIBUTE 并把原说明拼进文本，父子无关联；
     * 这里落在 {@code sup_issuance_reply}，可按单查往来、可统计回复次数，并可把
     * 「逐条确认/驳回」落到明细行（旧系统只有附件级 PULLSTATE）。</p>
     */
    @Transactional
    public SupIssuanceDtos.IssuanceDetail reply(Long id, SupIssuanceDtos.ReplyReq req,
                                                Long companyId, Long userId) {
        Map<String, Object> issuance = requireIssuance(id, companyId);
        String status = str(issuance.get("status"));
        if (!"SENT".equals(status) && !"RECEIVED".equals(status)) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_STATE_INVALID,
                    "只有已发出或已回执的发放单可以登记协同回复（当前 " + status + "）");
        }
        requireNotExpired(issuance);
        String replyType = normalize(req.replyType() == null ? "ACK" : req.replyType(),
                REPLY_TYPES, "回复类型");
        if ("COMMIT_DATE".equals(replyType) && req.promisedDate() == null) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID, "承诺交期回复必须填写承诺日期");
        }
        List<Long> fileIds = req.attachmentFileIds() == null ? List.of() : req.attachmentFileIds();
        List<String> names = req.attachmentNames() == null ? List.of() : req.attachmentNames();
        List<Long> validFileIds = new ArrayList<>();
        List<String> validNames = new ArrayList<>();
        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);
            if (fileId == null) {
                continue;
            }
            fileService.meta(fileId);
            validFileIds.add(fileId);
            String name = i < names.size() && names.get(i) != null && !names.get(i).isBlank()
                    ? names.get(i).trim() : "attachment-" + fileId;
            validNames.add(name);
        }
        String content = blankToNull(req.content());
        if (content == null && "ACK".equals(replyType)) {
            content = "已收阅";
        }
        long replyId = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sup_issuance_reply (id, company_id, issuance_id, reply_type,
                               content, promised_date, attachment_file_ids, attachment_names, replied_by)
                        VALUES (?, ?, ?, ?, ?, CAST(? AS date), CAST(? AS jsonb), CAST(? AS jsonb), ?)
                        """,
                replyId, companyId, id, replyType, content, req.promisedDate(),
                toJson(validFileIds), toJson(validNames), userId);
        updateItemReplyState(id, companyId, req.confirmedItemIds(), "CONFIRMED");
        updateItemReplyState(id, companyId, req.rejectedItemIds(), "REJECTED");
        jdbcTemplate.update(
                "UPDATE sup_issuance SET updated_by = ?, updated_at = now() WHERE id = ?",
                userId, id);
        SupIssuanceDtos.IssuanceDetail replied = detail(id, companyId);
        String typeLabel = REPLY_TYPE_LABELS.getOrDefault(replyType, replyType);
        StringBuilder text = new StringBuilder(typeLabel);
        if (content != null) {
            text.append("：").append(content);
        }
        if (req.promisedDate() != null) {
            text.append("（承诺交期 ").append(req.promisedDate()).append("）");
        }
        if (!validNames.isEmpty()) {
            text.append("；附件：").append(String.join("、", validNames));
        }
        notifyQuietly(NotifyService.EVENT_SUPPLIER_REPLY_CREATED,
                "supplier.reply.created:" + replyId,
                "供应商协同回复：" + replied.code() + " " + typeLabel,
                text.toString(), companyId, issuance, userId);
        return replied;
    }

    /**
     * 站内通知：投递失败只记日志，不能把「发放/回执/回复」这些业务动作带崩。
     *
     * <p>投递本身跑在 {@code REQUIRES_NEW} 事务里（NotifyService），因此失败不会污染业务事务。</p>
     */
    private void notifyQuietly(String eventType, String eventKey, String title, String content,
                               Long companyId, Map<String, Object> issuance, Long actorId) {
        try {
            Long owner = issuance.get("created_by") instanceof Number number
                    ? number.longValue() : null;
            Long recipient = owner != null ? owner : actorId;
            if (recipient == null) {
                return;
            }
            notifyService.dispatchToUsers(eventType, eventKey, title, content, companyId,
                    List.of(recipient));
        } catch (RuntimeException ex) {
            log.warn("供应商协同站内通知投递失败：event={} key={} 原因={}",
                    eventType, eventKey, ex.getMessage());
        }
    }

    /** 附件拉取标记（旧系统 PULLSTATE Pull/UnPull 的等价物，粒度到明细行）。 */
    @Transactional
    public void markItemPulled(Long id, Long itemId, Long companyId) {
        requireIssuance(id, companyId);
        int updated = jdbcTemplate.update("""
                UPDATE sup_issuance_item SET pull_state = 'PULL'
                 WHERE id = ? AND issuance_id = ? AND company_id = ?
                """, itemId, id, companyId);
        if (updated == 0) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID, "发放清单明细不存在：" + itemId);
        }
    }

    private void updateItemReplyState(Long issuanceId, Long companyId, List<Long> itemIds,
                                      String state) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        for (Long itemId : itemIds) {
            if (itemId == null) {
                continue;
            }
            jdbcTemplate.update("""
                    UPDATE sup_issuance_item SET reply_state = ?
                     WHERE id = ? AND issuance_id = ? AND company_id = ?
                    """, state, itemId, issuanceId, companyId);
        }
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
                        rs.getString("revision"), rs.getInt("sort_no"),
                        rs.getString("pull_state"), rs.getString("reply_state")),
                issuanceId);
    }

    /** 协同往来（时间正序）：一条发放单下可能有多轮回复（旧系统 AnswerAgain 语义）。 */
    private List<SupIssuanceDtos.ReplyRow> replies(Long issuanceId) {
        return jdbcTemplate.query("""
                        SELECT r.*, u.display_name AS replied_by_name
                          FROM sup_issuance_reply r
                          LEFT JOIN sys_user u ON u.id = r.replied_by
                         WHERE r.issuance_id = ? AND r.deleted = false
                         ORDER BY r.replied_at, r.id
                        """,
                (rs, rowNum) -> new SupIssuanceDtos.ReplyRow(rs.getLong("id"),
                        rs.getString("reply_type"), rs.getString("content"),
                        dateOf(rs.getDate("promised_date")),
                        fromJson(rs.getString("attachment_file_ids"), new TypeReference<List<Long>>() {
                        }),
                        fromJson(rs.getString("attachment_names"), new TypeReference<List<String>>() {
                        }),
                        rs.getObject("replied_by", Long.class), rs.getString("replied_by_name"),
                        rs.getObject("replied_at", OffsetDateTime.class)),
                issuanceId);
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID, "回复附件序列化失败");
        }
    }

    private static LocalDate dateOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return null;
    }

    /** 生效/失效窗口合法性：失效日不能早于生效日。 */
    private static void requireValidWindow(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && expireDate.isBefore(effectiveDate)) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID, "失效日期不能早于生效日期");
        }
    }

    /**
     * 失效拦截放在服务端：旧系统只在列表行点击的 JS 里比字符串日期
     * （CoordinationManager.js:256-259），绕过前端直接调接口就失效。
     */
    private static void requireNotExpired(Map<String, Object> issuance) {
        LocalDate expireDate = dateOf(issuance.get("expire_at"));
        if (expireDate != null && expireDate.isBefore(LocalDate.now())) {
            throw BizException.of(ErrorCode.SUP_ISSUANCE_INVALID,
                    "该数据已失效（失效日期 " + expireDate + "），不能再发送/回执/回复");
        }
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
                rs.getString("status"), rs.getInt("item_count"), rs.getInt("reply_count"),
                rs.getObject("sent_at", OffsetDateTime.class),
                rs.getObject("received_at", OffsetDateTime.class),
                rs.getString("receipt_file_name"),
                rs.getBoolean("is_important"),
                rs.getObject("read_at", OffsetDateTime.class),
                dateOf(rs.getDate("effective_at")), dateOf(rs.getDate("expire_at")),
                rs.getString("supplier_group"),
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
