package com.nexplm.platform.quality.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.quality.api.SysDocDtos;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * IATF16949 体系文件（P8-3-1）：文件夹 + 模板 + 文件 + 版本 + 审批发布。
 *
 * <p>刻意的取舍：</p>
 * <ul>
 *   <li><b>不复用通用文档层</b>：受控版本（version_no）+ 生效日 + 评审周期 + 作废留档这套语义
 *       plm_object 里没有，硬塞要改所有对象共用的主表；而通用文档升版只能走变更单，
 *       体系文件改一句条款不该强制开变更；</li>
 *   <li><b>正文先章节化</b>（sections jsonb）：模板给章节骨架，提交审批前必填章节必须有内容，
 *       这样"按模板生成"才有实际约束力；正文文件后续用 file_id 挂文件中心，不重做存储；</li>
 *   <li><b>发布 = 写版本快照</b>：历史版本只增不改，修订时版本号 +1 回到草稿，
 *       旧版本在 sys_doc_version 里可回溯；</li>
 *   <li><b>文件夹 code 创建后不可改</b>：path 由 code 拼成，改 code 要重算整棵子树，
 *       代价远大于收益。</li>
 * </ul>
 */
@Service
public class SysDocService {

    private static final Set<String> LEVELS = Set.of("MANUAL", "PROCEDURE", "WI", "RECORD");
    private static final Set<String> STATUSES = Set.of("DRAFT", "SUBMITTED", "RELEASED", "OBSOLETE");

    private static final String DOC_SELECT = """
            SELECT d.*, f.name AS folder_name, t.code AS template_code,
                   u.display_name AS owner_name
              FROM sys_doc d
              LEFT JOIN sys_doc_folder f ON f.id = d.folder_id
              LEFT JOIN sys_doc_template t ON t.id = d.template_id
              LEFT JOIN sys_user u ON u.id = d.owner_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SysDocService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- 文件夹

    public List<SysDocDtos.FolderRow> folders(Long companyId) {
        return jdbcTemplate.query("""
                        SELECT f.*, (SELECT COUNT(1) FROM sys_doc d
                                      WHERE d.folder_id = f.id AND d.deleted = false) AS doc_count
                          FROM sys_doc_folder f
                         WHERE f.company_id = ? AND f.deleted = false
                         ORDER BY f.path, f.sort_no
                        """,
                (rs, rowNum) -> new SysDocDtos.FolderRow(rs.getLong("id"),
                        (Long) rs.getObject("parent_id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("path"), rs.getInt("sort_no"),
                        rs.getInt("doc_count")),
                companyId);
    }

    @Transactional
    public SysDocDtos.FolderRow createFolder(SysDocDtos.FolderSaveReq req, Long companyId,
                                             Long userId) {
        String code = req.code().trim().toUpperCase();
        String path = "/" + code;
        if (req.parentId() != null) {
            var parent = requireFolder(req.parentId(), companyId);
            path = str(parent.get("path")) + "/" + code;
        }
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO sys_doc_folder (id, company_id, parent_id, code, name, path,
                                                        sort_no, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    id, companyId, req.parentId(), code, req.name().trim(), path,
                    req.sortNo() == null ? 10 : req.sortNo(), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "文件夹编码重复：" + code);
        }
        return folder(id, companyId);
    }

    @Transactional
    public SysDocDtos.FolderRow updateFolder(Long id, SysDocDtos.FolderSaveReq req, Long companyId,
                                             Long userId) {
        var row = requireFolder(id, companyId);
        String code = req.code() == null ? str(row.get("code")) : req.code().trim().toUpperCase();
        if (!code.equals(str(row.get("code")))) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID,
                    "文件夹编码不可修改（子文件夹路径由它拼成）：" + code);
        }
        jdbcTemplate.update("""
                        UPDATE sys_doc_folder SET name = ?, sort_no = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), req.sortNo() == null ? 10 : req.sortNo(), userId, id, companyId);
        return folder(id, companyId);
    }

    @Transactional
    public void deleteFolder(Long id, Long companyId) {
        requireFolder(id, companyId);
        Integer children = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_doc_folder
                 WHERE parent_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        Integer docs = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_doc
                 WHERE folder_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        if ((children != null && children > 0) || (docs != null && docs > 0)) {
            throw BizException.of(ErrorCode.SYSDOC_FOLDER_IN_USE,
                    "文件夹下还有 " + (children == null ? 0 : children) + " 个子文件夹、"
                            + (docs == null ? 0 : docs) + " 个文件，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE sys_doc_folder SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ---------------------------------------------------------------- 模板

    public List<SysDocDtos.TemplateRow> templates(Long companyId, String keyword, Boolean enabled) {
        StringBuilder where = new StringBuilder(" WHERE t.company_id = ? AND t.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (t.code ILIKE ? OR t.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (enabled != null) {
            where.append(" AND t.enabled = ?");
            args.add(enabled);
        }
        return jdbcTemplate.query("""
                        SELECT t.*, f.name AS folder_name,
                               jsonb_array_length(COALESCE(t.sections, '[]'::jsonb)) AS section_count
                          FROM sys_doc_template t
                          LEFT JOIN sys_doc_folder f ON f.id = t.folder_id
                        """ + where + " ORDER BY t.updated_at DESC",
                (rs, rowNum) -> new SysDocDtos.TemplateRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("doc_level"),
                        (Long) rs.getObject("folder_id"), rs.getString("folder_name"),
                        rs.getInt("default_review_months"), rs.getBoolean("enabled"),
                        rs.getInt("section_count"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                args.toArray());
    }

    public SysDocDtos.TemplateDetail template(Long id, Long companyId) {
        var row = requireTemplate(id, companyId);
        return new SysDocDtos.TemplateDetail(id, str(row.get("code")), str(row.get("name")),
                str(row.get("doc_level")), (Long) row.get("folder_id"), str(row.get("folder_name")),
                intOf(row.get("default_review_months")), Boolean.TRUE.equals(row.get("enabled")),
                str(row.get("remark")), readSectionTemplates(row.get("sections")));
    }

    @Transactional
    public SysDocDtos.TemplateDetail createTemplate(SysDocDtos.TemplateSaveReq req, Long companyId,
                                                    Long userId) {
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sys_doc_template (id, company_id, code, name, doc_level, folder_id,
                                                      default_review_months, sections, remark, enabled,
                                                      created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
                        """,
                id, companyId, nextTemplateCode(), req.name().trim(),
                normalizeLevel(req.docLevel()), req.folderId(),
                req.defaultReviewMonths() == null ? 12 : req.defaultReviewMonths(),
                writeJson(normalizeSectionTemplates(req.sections())), blankToNull(req.remark()),
                req.enabled() == null || req.enabled(), userId, userId);
        return template(id, companyId);
    }

    @Transactional
    public SysDocDtos.TemplateDetail updateTemplate(Long id, SysDocDtos.TemplateSaveReq req,
                                                    Long companyId, Long userId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("""
                        UPDATE sys_doc_template
                           SET name = ?, doc_level = ?, folder_id = ?, default_review_months = ?,
                               sections = ?::jsonb, remark = ?, enabled = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeLevel(req.docLevel()), req.folderId(),
                req.defaultReviewMonths() == null ? 12 : req.defaultReviewMonths(),
                writeJson(normalizeSectionTemplates(req.sections())), blankToNull(req.remark()),
                req.enabled() == null || req.enabled(), userId, id, companyId);
        return template(id, companyId);
    }

    /** 模板软删：已按它生成的文件不受影响（文件里有自己的章节快照）。 */
    @Transactional
    public void deleteTemplate(Long id, Long companyId) {
        requireTemplate(id, companyId);
        jdbcTemplate.update("""
                UPDATE sys_doc_template SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ---------------------------------------------------------------- 文件

    public PageResult<SysDocDtos.DocRow> docs(Long companyId, String keyword, Long folderId,
                                              String status, String docLevel, Boolean dueReviewOnly,
                                              Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE d.company_id = ? AND d.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (d.code ILIKE ? OR d.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (folderId != null) {
            // 含子文件夹：path 前缀匹配，避免前端逐层点选
            String path = str(requireFolder(folderId, companyId).get("path"));
            where.append(" AND d.folder_id IN (SELECT cf.id FROM sys_doc_folder cf"
                    + " WHERE cf.company_id = ? AND cf.deleted = false"
                    + " AND (cf.id = ? OR cf.path LIKE ?))");
            args.add(companyId);
            args.add(folderId);
            args.add(path + "/%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND d.status = ?");
            args.add(normalizeStatus(status));
        }
        if (docLevel != null && !docLevel.isBlank()) {
            where.append(" AND d.doc_level = ?");
            args.add(normalizeLevel(docLevel));
        }
        if (Boolean.TRUE.equals(dueReviewOnly)) {
            where.append(" AND d.status = 'RELEASED' AND d.next_review_date IS NOT NULL"
                    + " AND d.next_review_date <= CURRENT_DATE");
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_doc d" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SysDocDtos.DocRow> items = jdbcTemplate.query(
                DOC_SELECT + where + " ORDER BY d.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> docRow(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public SysDocDtos.DocDetail doc(Long id, Long companyId) {
        // 版本表单独查：在主结果集遍历中再发查询会依赖驱动的缓冲行为，不冒这个险
        List<SysDocDtos.DocDetail> rows = jdbcTemplate.query(
                DOC_SELECT + " WHERE d.id = ? AND d.company_id = ? AND d.deleted = false",
                (rs, rowNum) -> new SysDocDtos.DocDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("doc_level"),
                        (Long) rs.getObject("folder_id"), rs.getString("folder_name"),
                        (Long) rs.getObject("template_id"), rs.getString("template_code"),
                        rs.getInt("version_no"), rs.getString("status"),
                        (Long) rs.getObject("owner_id"), rs.getString("owner_name"),
                        rs.getInt("review_months"),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getObject("next_review_date", LocalDate.class),
                        rs.getString("summary"), (Long) rs.getObject("file_id"),
                        rs.getString("approve_note"),
                        rs.getObject("approved_at", OffsetDateTime.class),
                        rs.getObject("obsolete_at", OffsetDateTime.class),
                        readSections(rs.getString("sections")), List.of()),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SYSDOC_NOT_FOUND, "体系文件不存在：" + id);
        }
        SysDocDtos.DocDetail base = rows.get(0);
        return new SysDocDtos.DocDetail(base.id(), base.code(), base.name(), base.docLevel(),
                base.folderId(), base.folderName(), base.templateId(), base.templateCode(),
                base.versionNo(), base.status(), base.ownerId(), base.ownerName(),
                base.reviewMonths(), base.effectiveDate(), base.nextReviewDate(), base.summary(),
                base.fileId(), base.approveNote(), base.approvedAt(), base.obsoleteAt(),
                base.sections(), versionRows(id));
    }

    /** 按模板生成：层级/文件夹/评审周期/章节骨架都取模板，编号与名称由业务填。 */
    @Transactional
    public SysDocDtos.DocDetail createFromTemplate(SysDocDtos.DocFromTemplateReq req,
                                                   Long companyId, Long userId) {
        SysDocDtos.TemplateDetail template = template(req.templateId(), companyId);
        if (!template.enabled()) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "模板已停用：" + template.code());
        }
        List<SysDocDtos.DocSection> sections = new ArrayList<>();
        for (SysDocDtos.SectionTemplate item : template.sections()) {
            sections.add(new SysDocDtos.DocSection(item.key(), item.title(), "", item.required()));
        }
        SysDocDtos.DocSaveReq save = new SysDocDtos.DocSaveReq(req.code(), req.name(),
                template.docLevel(), template.folderId(), req.ownerId(),
                template.defaultReviewMonths(), null, sections);
        return insertDoc(save, template.id(), companyId, userId);
    }

    @Transactional
    public SysDocDtos.DocDetail createDoc(SysDocDtos.DocSaveReq req, Long companyId, Long userId) {
        return insertDoc(req, null, companyId, userId);
    }

    @Transactional
    public SysDocDtos.DocDetail updateDoc(Long id, SysDocDtos.DocSaveReq req, Long companyId,
                                          Long userId) {
        var row = requireDoc(id, companyId);
        requireStatus(row, "DRAFT", "只有草稿状态的体系文件可以修改");
        jdbcTemplate.update("""
                        UPDATE sys_doc
                           SET code = ?, name = ?, doc_level = ?, folder_id = ?, owner_id = ?,
                               review_months = ?, summary = ?, sections = ?::jsonb,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.code().trim(), req.name().trim(), normalizeLevel(req.docLevel()),
                req.folderId(), req.ownerId(),
                req.reviewMonths() == null ? 12 : req.reviewMonths(),
                blankToNull(req.summary()), writeJson(normalizeDocSections(req.sections())),
                userId, id, companyId);
        return doc(id, companyId);
    }

    /** 删除：只允许草稿（已发布过的是受控记录，只能作废不能删）。 */
    @Transactional
    public void deleteDoc(Long id, Long companyId) {
        var row = requireDoc(id, companyId);
        requireStatus(row, "DRAFT", "只有草稿状态的体系文件可以删除（已发布的请走作废）");
        Integer released = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_doc_version WHERE doc_id = ?", Integer.class, id);
        if (released != null && released > 0) {
            throw BizException.of(ErrorCode.SYSDOC_STATE_INVALID,
                    "该文件已有发布版本记录，不能删除，请改为作废");
        }
        jdbcTemplate.update("""
                UPDATE sys_doc SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /** 提交审批：必填章节必须有内容，否则审批人看不到实质内容只能驳回。 */
    @Transactional
    public SysDocDtos.DocDetail submit(Long id, Long companyId, Long userId) {
        var row = requireDoc(id, companyId);
        requireStatus(row, "DRAFT", "只有草稿可以提交审批");
        List<SysDocDtos.DocSection> sections = readSections(str(row.get("sections")));
        List<String> missing = new ArrayList<>();
        for (SysDocDtos.DocSection section : sections) {
            if (Boolean.TRUE.equals(section.required())
                    && (section.content() == null || section.content().isBlank())) {
                missing.add(section.title() == null ? section.key() : section.title());
            }
        }
        if (!missing.isEmpty()) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID,
                    "以下必填章节还没有内容：" + String.join("、", missing));
        }
        jdbcTemplate.update("""
                        UPDATE sys_doc SET status = 'SUBMITTED', submitted_at = now(), submitted_by = ?,
                                           updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                userId, userId, id, companyId);
        return doc(id, companyId);
    }

    /**
     * 审批：通过 → 发布（写版本快照、算生效日与下次评审日）；驳回 → 退回草稿（原因必填）。
     */
    @Transactional
    public SysDocDtos.DocDetail approve(Long id, SysDocDtos.DocApproveReq req, Long companyId,
                                        Long userId) {
        var row = requireDoc(id, companyId);
        requireStatus(row, "SUBMITTED", "只有已提交审批的体系文件可以审批");
        if (!req.passed() && (req.note() == null || req.note().isBlank())) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "驳回必须写清原因");
        }
        if (!req.passed()) {
            jdbcTemplate.update("""
                            UPDATE sys_doc SET status = 'DRAFT', approve_note = ?,
                                               submitted_at = null, submitted_by = null,
                                               updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ?
                            """, req.note().trim(), userId, id, companyId);
            return doc(id, companyId);
        }
        int versionNo = intOf(row.get("version_no"));
        int reviewMonths = intOf(row.get("review_months")) <= 0 ? 12 : intOf(row.get("review_months"));
        LocalDate today = LocalDate.now();
        jdbcTemplate.update("""
                        INSERT INTO sys_doc_version (id, company_id, doc_id, version_no, sections,
                                                     change_note, status, released_at, released_by,
                                                     created_by)
                        VALUES (?, ?, ?, ?, ?::jsonb, ?, 'RELEASED', now(), ?, ?)
                        """,
                IdWorker.getId(), companyId, id, versionNo, str(row.get("sections")),
                coalesceChangeNote(str(row.get("change_note")), versionNo), userId, userId);
        jdbcTemplate.update("""
                        UPDATE sys_doc
                           SET status = 'RELEASED', approved_at = now(), approved_by = ?,
                               approve_note = ?, effective_date = ?, next_review_date = ?,
                               change_note = null, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                userId, blankToNull(req.note()), today, today.plusMonths(reviewMonths),
                userId, id, companyId);
        return doc(id, companyId);
    }

    /** 修订：已发布 → 草稿（版本号 +1），旧版本留在 sys_doc_version。 */
    @Transactional
    public SysDocDtos.DocDetail revise(Long id, String changeNote, Long companyId, Long userId) {
        var row = requireDoc(id, companyId);
        requireStatus(row, "RELEASED", "只有已发布的体系文件可以发起修订");
        jdbcTemplate.update("""
                        UPDATE sys_doc
                           SET status = 'DRAFT', version_no = version_no + 1,
                               effective_date = null, approved_at = null, approved_by = null,
                               approve_note = null, change_note = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                blankToNull(changeNote), userId, id, companyId);
        return doc(id, companyId);
    }

    /** 作废：已发布 → 作废（保留版本历史，不物理删除）。 */
    @Transactional
    public SysDocDtos.DocDetail obsolete(Long id, Long companyId, Long userId) {
        var row = requireDoc(id, companyId);
        requireStatus(row, "RELEASED", "只有已发布的体系文件可以作废");
        jdbcTemplate.update("""
                        UPDATE sys_doc SET status = 'OBSOLETE', obsolete_at = now(),
                                           updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                userId, id, companyId);
        return doc(id, companyId);
    }

    /** 到期评审清单：已发布且下次评审日落在未来 {@code days} 天内（含已逾期）。 */
    public List<SysDocDtos.DocRow> dueReview(Long companyId, Integer days) {
        int horizon = days == null || days < 0 ? 30 : days;
        return jdbcTemplate.query(DOC_SELECT + """
                         WHERE d.company_id = ? AND d.deleted = false AND d.status = 'RELEASED'
                           AND d.next_review_date IS NOT NULL
                           AND d.next_review_date <= CURRENT_DATE + (? * INTERVAL '1 day')
                         ORDER BY d.next_review_date
                        """,
                (rs, rowNum) -> docRow(rs), companyId, horizon);
    }

    // ---------------------------------------------------------------- 内部

    private SysDocDtos.DocDetail insertDoc(SysDocDtos.DocSaveReq req, Long templateId,
                                           Long companyId, Long userId) {
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO sys_doc (id, company_id, folder_id, template_id, code, name,
                                                 doc_level, version_no, status, owner_id, review_months,
                                                 summary, sections, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 1, 'DRAFT', ?, ?, ?, ?::jsonb, ?, ?)
                            """,
                    id, companyId, req.folderId(), templateId, req.code().trim(),
                    req.name().trim(), normalizeLevel(req.docLevel()), req.ownerId(),
                    req.reviewMonths() == null ? 12 : req.reviewMonths(),
                    blankToNull(req.summary()),
                    writeJson(normalizeDocSections(req.sections())), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "文件编号已存在：" + req.code().trim());
        }
        return doc(id, companyId);
    }

    private SysDocDtos.DocRow docRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        LocalDate nextReview = rs.getObject("next_review_date", LocalDate.class);
        String status = rs.getString("status");
        boolean dueReview = nextReview != null && "RELEASED".equals(status)
                && !nextReview.isAfter(LocalDate.now());
        return new SysDocDtos.DocRow(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("doc_level"), (Long) rs.getObject("folder_id"),
                rs.getString("folder_name"), (Long) rs.getObject("template_id"),
                rs.getString("template_code"), rs.getInt("version_no"), status,
                (Long) rs.getObject("owner_id"), rs.getString("owner_name"),
                rs.getObject("effective_date", LocalDate.class), nextReview,
                rs.getInt("review_months"), dueReview,
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private List<SysDocDtos.VersionRow> versionRows(Long docId) {
        return jdbcTemplate.query("""
                        SELECT v.*, u.display_name AS released_by_name,
                               jsonb_array_length(COALESCE(v.sections, '[]'::jsonb)) AS section_count
                          FROM sys_doc_version v
                          LEFT JOIN sys_user u ON u.id = v.released_by
                         WHERE v.doc_id = ?
                         ORDER BY v.version_no DESC
                        """,
                (rs, rowNum) -> new SysDocDtos.VersionRow(rs.getLong("id"),
                        rs.getInt("version_no"), rs.getString("change_note"), rs.getString("status"),
                        rs.getObject("released_at", OffsetDateTime.class),
                        rs.getString("released_by_name"), rs.getInt("section_count")),
                docId);
    }

    private SysDocDtos.FolderRow folder(Long id, Long companyId) {
        var row = requireFolder(id, companyId);
        Integer docCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_doc WHERE folder_id = ? AND deleted = false
                """, Integer.class, id);
        return new SysDocDtos.FolderRow(id, (Long) row.get("parent_id"), str(row.get("code")),
                str(row.get("name")), str(row.get("path")), intOf(row.get("sort_no")),
                docCount == null ? 0 : docCount);
    }

    private java.util.Map<String, Object> requireFolder(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, parent_id, code, name, path, sort_no FROM sys_doc_folder
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SYSDOC_NOT_FOUND, "文件夹不存在：" + id);
        }
        return rows.get(0);
    }

    private java.util.Map<String, Object> requireTemplate(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT t.*, f.name AS folder_name FROM sys_doc_template t
                  LEFT JOIN sys_doc_folder f ON f.id = t.folder_id
                 WHERE t.id = ? AND t.company_id = ? AND t.deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SYSDOC_NOT_FOUND, "体系文件模板不存在：" + id);
        }
        return rows.get(0);
    }

    private java.util.Map<String, Object> requireDoc(Long id, Long companyId) {
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, status, version_no, review_months, sections, change_note FROM sys_doc
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SYSDOC_NOT_FOUND, "体系文件不存在：" + id);
        }
        return rows.get(0);
    }

    private static void requireStatus(java.util.Map<String, Object> row, String expected,
                                      String message) {
        if (!expected.equals(str(row.get("status")))) {
            throw BizException.of(ErrorCode.SYSDOC_STATE_INVALID,
                    message + "（当前：" + str(row.get("status")) + "）");
        }
    }

    private List<SysDocDtos.SectionTemplate> normalizeSectionTemplates(
            List<SysDocDtos.SectionTemplate> sections) {
        List<SysDocDtos.SectionTemplate> result = new ArrayList<>();
        if (sections == null) {
            return result;
        }
        Set<String> keys = new java.util.HashSet<>();
        for (SysDocDtos.SectionTemplate item : sections) {
            String title = item.title() == null ? "" : item.title().trim();
            if (title.isEmpty()) {
                continue;
            }
            String key = item.key() == null || item.key().isBlank()
                    ? "S" + (result.size() + 1) : item.key().trim().toUpperCase();
            if (!keys.add(key)) {
                throw BizException.of(ErrorCode.SYSDOC_INVALID, "章节编号重复：" + key);
            }
            result.add(new SysDocDtos.SectionTemplate(key, title, blankToNull(item.hint()),
                    item.required() == null || item.required()));
        }
        return result;
    }

    private List<SysDocDtos.DocSection> normalizeDocSections(List<SysDocDtos.DocSection> sections) {
        List<SysDocDtos.DocSection> result = new ArrayList<>();
        if (sections == null) {
            return result;
        }
        Set<String> keys = new java.util.HashSet<>();
        for (SysDocDtos.DocSection item : sections) {
            String title = item.title() == null ? "" : item.title().trim();
            if (title.isEmpty()) {
                continue;
            }
            String key = item.key() == null || item.key().isBlank()
                    ? "S" + (result.size() + 1) : item.key().trim().toUpperCase();
            if (!keys.add(key)) {
                throw BizException.of(ErrorCode.SYSDOC_INVALID, "章节编号重复：" + key);
            }
            result.add(new SysDocDtos.DocSection(key, title, item.content(),
                    Boolean.TRUE.equals(item.required())));
        }
        return result;
    }

    private List<SysDocDtos.SectionTemplate> readSectionTemplates(Object json) {
        String text = json == null ? null : String.valueOf(json);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(text,
                    new TypeReference<List<SysDocDtos.SectionTemplate>>() {
                    });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "章节模板数据损坏，请联系管理员");
        }
    }

    private List<SysDocDtos.DocSection> readSections(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SysDocDtos.DocSection>>() {
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "章节内容数据损坏，请联系管理员");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "章节数据序列化失败");
        }
    }

    private String nextTemplateCode() {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'SysDocTemplate', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("TPL-%06d", no == null ? 1L : no);
    }

    private static String coalesceChangeNote(String changeNote, int versionNo) {
        if (changeNote != null && !changeNote.isBlank()) {
            return changeNote;
        }
        return versionNo <= 1 ? "首次发布" : "第 " + versionNo + " 版";
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "PROCEDURE";
        }
        String normalized = level.trim().toUpperCase();
        if (!LEVELS.contains(normalized)) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID,
                    "未知文件层级：" + level + "（可选 " + String.join("/", LEVELS) + "）");
        }
        return normalized;
    }

    private static String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw BizException.of(ErrorCode.SYSDOC_INVALID, "未知状态：" + status);
        }
        return normalized;
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
