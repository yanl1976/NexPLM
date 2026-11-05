package com.nexplm.platform.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocDtos;
import com.nexplm.platform.doc.domain.DocMaster;
import com.nexplm.platform.doc.mapper.DocMasterMapper;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.lifecycle.service.LifecycleService;
import com.nexplm.platform.metadata.service.MetaService;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.object.version.VersionFormatter;
import com.nexplm.platform.security.scope.ScopeApplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 图文档服务：对象与子表的创建/更新/查询、与文件仓的绑定、检入检出与版本迭代。
 * <p>约定：</p>
 * <ul>
 *   <li>业务字段存 {@code plm_object.ext_json}（文档类型/来源/关键词/描述），子表只存结构性统计；</li>
 *   <li>版本 = master_id（族）+ revision（大版本，字母）+ iteration（小版本，检入递增）；</li>
 *   <li>检入生成新 iteration：新对象复制属性与文件链接，物理文件共享不复制，旧版本 {@code is_latest} 置否。</li>
 * </ul>
 */
@Service
public class DocService {

    public static final String OBJECT_TYPE = "PdmDoc";
    private static final String ENUM_DOC_TYPE = "DocType";
    private static final String ENUM_DOC_SOURCE = "DocSource";
    private static final String SOURCE_NORMAL = "NORMAL";
    private static final Set<String> LOCKED_STATES = Set.of("Released", "Obsolete");

    private final PlmObjectMapper objectMapper;
    private final DocMasterMapper docMapper;
    private final FileService fileService;
    private final CheckoutService checkoutService;
    private final MetaService metaService;
    private final LifecycleService lifecycleService;
    private final DocClassService docClassService;
    private final ScopeApplier scopeApplier;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public DocService(PlmObjectMapper objectMapper, DocMasterMapper docMapper, FileService fileService,
                      CheckoutService checkoutService, MetaService metaService,
                      LifecycleService lifecycleService, DocClassService docClassService,
                      ScopeApplier scopeApplier, JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.objectMapper = objectMapper;
        this.docMapper = docMapper;
        this.fileService = fileService;
        this.checkoutService = checkoutService;
        this.metaService = metaService;
        this.lifecycleService = lifecycleService;
        this.docClassService = docClassService;
        this.scopeApplier = scopeApplier;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    // ------------------------------------------------------------ 创建与修改

    @Transactional
    public DocDtos.DocDetail create(DocDtos.DocSaveReq req, Long userId) {
        String docType = requireEnum(ENUM_DOC_TYPE, req.docType(), "文档类型");
        String source = requireEnum(ENUM_DOC_SOURCE,
                req.source() == null || req.source().isBlank() ? SOURCE_NORMAL : req.source(), "文档来源");
        OffsetDateTime now = OffsetDateTime.now();
        long id = IdWorker.getId();
        Map<String, String> classAttrs = resolveClassAttrs(req.classId(), req.classAttrs());

        PlmObject object = new PlmObject();
        object.setId(id);
        object.setObjectType(OBJECT_TYPE);
        object.setMasterId(id);
        object.setCode(resolveCode(req.code()));
        object.setName(req.name().trim());
        object.setCompanyId(companyId(userId));
        object.setOrgId(req.orgId());
        object.setOwnerId(userId);
        object.setSecurityLevel(req.securityLevel() == null ? 0 : req.securityLevel());
        object.setLifecycleState(lifecycleService.definitionForType(OBJECT_TYPE).initial());
        object.setWorkflowState("NONE");
        object.setRevision(1);
        object.setIteration(1);
        object.setIsLatest(true);
        object.setExtJson(buildExt(docType, source, req.keyword(), req.description(),
                req.classId(), classAttrs));
        object.setVersion(0);
        object.setDeleted(false);
        object.setCreatedBy(userId);
        object.setCreatedAt(now);
        object.setUpdatedBy(userId);
        object.setUpdatedAt(now);
        objectMapper.insert(object);

        DocMaster master = new DocMaster();
        master.setObjectId(id);
        master.setDocType(docType);
        master.setClassId(req.classId());
        master.setSource(source);
        master.setFileCount(0);
        master.setIsDerived(false);
        master.setUpdatedAt(now);
        docMapper.insert(master);
        return detail(id);
    }

    @Transactional
    public DocDtos.DocDetail update(Long id, DocDtos.DocSaveReq req, Long userId) {
        PlmObject object = requireObject(id);
        ensureWritable(object, userId);
        String docType = requireEnum(ENUM_DOC_TYPE, req.docType(), "文档类型");
        String source = requireEnum(ENUM_DOC_SOURCE,
                req.source() == null || req.source().isBlank() ? SOURCE_NORMAL : req.source(), "文档来源");

        Map<String, String> classAttrs = resolveClassAttrs(req.classId(), req.classAttrs());

        PlmObject update = new PlmObject();
        update.setId(id);
        update.setVersion(object.getVersion());
        update.setName(req.name().trim());
        update.setOrgId(req.orgId());
        update.setSecurityLevel(req.securityLevel() == null ? 0 : req.securityLevel());
        update.setExtJson(buildExt(docType, source, req.keyword(), req.description(),
                req.classId(), classAttrs));
        update.setUpdatedBy(userId);
        update.setUpdatedAt(OffsetDateTime.now());
        if (objectMapper.updateById(update) == 0) {
            throw BizException.of(ErrorCode.OBJ_VERSION_CONFLICT);
        }

        DocMaster master = docMapper.selectById(id);
        if (master == null) {
            master = new DocMaster();
            master.setObjectId(id);
            master.setFileCount(0);
            master.setUpdatedAt(OffsetDateTime.now());
        }
        master.setDocType(docType);
        master.setClassId(req.classId());
        master.setSource(source);
        if (docMapper.selectById(id) == null) {
            docMapper.insert(master);
        } else {
            docMapper.updateById(master);
        }
        return detail(id);
    }

    // ------------------------------------------------------------ 查询

    public DocDtos.DocDetail detail(Long id) {
        PlmObject object = requireObject(id);
        DocMaster master = docMapper.selectById(id);
        Map<String, Object> ext = readExt(object.getExtJson());
        List<FileService.FileView> files = fileService.listByOwner(id, true);
        DocDtos.CheckoutInfo checkout = checkoutService.active(id)
                .map(this::toCheckoutInfo).orElse(null);
        Long classId = master == null ? null : master.getClassId();
        return new DocDtos.DocDetail(id, object.getCode(), object.getName(),
                asText(ext.get("docType"), master == null ? null : master.getDocType()),
                asText(ext.get("source"), master == null ? null : master.getSource()),
                asText(ext.get("keyword"), null), asText(ext.get("description"), null),
                object.getLifecycleState(), object.getPhaseState(), object.getWorkflowState(),
                object.getRevision(), object.getIteration(),
                VersionFormatter.label(object.getRevision(), object.getIteration()),
                object.getIsLatest(), object.getMasterId(), object.getOwnerId(), object.getOrgId(),
                object.getSecurityLevel(),
                master == null ? null : master.getMainFileId(),
                master == null ? null : master.getFileExt(),
                master == null ? null : master.getFileCount(),
                master == null ? null : master.getPageCount(),
                checkout, files, object.getCreatedAt(), object.getUpdatedAt(),
                classId, className(classId), readClassAttrs(ext.get("classAttrs")));
    }

    public PageResult<DocDtos.DocSummary> page(int page, int size, String keyword,
                                               String docType, String lifecycleState, Long classId) {
        int pageNo = Math.max(1, page);
        int pageSize = Math.min(Math.max(1, size), 200);
        LambdaQueryWrapper<PlmObject> wrapper = new LambdaQueryWrapper<PlmObject>()
                .eq(PlmObject::getObjectType, OBJECT_TYPE)
                .eq(PlmObject::getIsLatest, Boolean.TRUE)
                .orderByDesc(PlmObject::getUpdatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlmObject::getCode, kw).or().like(PlmObject::getName, kw));
        }
        if (docType != null && !docType.isBlank()) {
            wrapper.apply("ext_json ->> 'docType' = {0}", docType);
        }
        if (lifecycleState != null && !lifecycleState.isBlank()) {
            wrapper.eq(PlmObject::getLifecycleState, lifecycleState);
        }
        if (classId != null) {
            List<Long> classIds = docClassService.subtreeIds(classId);
            if (classIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.apply("id IN (SELECT object_id FROM doc_master WHERE class_id IN ("
                        + joinIds(classIds) + "))");
            }
        }
        scopeApplier.apply(wrapper, PlmObject::getCompanyId, PlmObject::getOrgId, PlmObject::getOwnerId);

        Page<PlmObject> result = objectMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<Long> ids = result.getRecords().stream().map(PlmObject::getId).toList();
        Map<Long, DocMaster> masters = new HashMap<>();
        if (!ids.isEmpty()) {
            for (DocMaster master : docMapper.selectList(
                    new LambdaQueryWrapper<DocMaster>().in(DocMaster::getObjectId, ids))) {
                masters.put(master.getObjectId(), master);
            }
        }
        Map<Long, String> classNames = new HashMap<>();
        List<Long> classIds = masters.values().stream()
                .map(DocMaster::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!classIds.isEmpty()) {
            jdbcTemplate.query("SELECT id, name FROM doc_class WHERE id IN (" + joinIds(classIds) + ")",
                    rs -> {
                        classNames.put(rs.getLong(1), rs.getString(2));
                    });
        }
        List<DocDtos.DocSummary> items = result.getRecords().stream()
                .map(object -> {
                    DocMaster master = masters.get(object.getId());
                    Long itemClassId = master == null ? null : master.getClassId();
                    return toSummary(object, master,
                            itemClassId == null ? null : classNames.get(itemClassId));
                })
                .toList();
        return PageResult.of(items, result.getTotal(), pageNo, pageSize);
    }

    /** 版本历史：同 master 族内按大版本、小版本倒序。 */
    public List<DocDtos.DocVersion> versions(Long id) {
        PlmObject object = requireObject(id);
        return objectMapper.selectList(new LambdaQueryWrapper<PlmObject>()
                        .eq(PlmObject::getMasterId, object.getMasterId())
                        .orderByDesc(PlmObject::getRevision)
                        .orderByDesc(PlmObject::getIteration))
                .stream()
                .map(item -> new DocDtos.DocVersion(item.getId(), item.getRevision(), item.getIteration(),
                        VersionFormatter.label(item.getRevision(), item.getIteration()),
                        item.getLifecycleState(), item.getIsLatest(),
                        item.getCreatedAt(), item.getUpdatedAt()))
                .toList();
    }

    // ------------------------------------------------------------ 文件

    @Transactional
    public FileService.UploadResult uploadFile(Long docId, String fileName, String mime, String fileRole,
                                               Integer sort, InputStream inputStream, Long userId) {
        PlmObject object = requireObject(docId);
        ensureWritable(object, userId);
        FileService.UploadResult result = fileService.upload(docId, fileRole, sort, fileName, mime,
                inputStream, userId);
        refreshFileStats(docId);
        touch(docId, userId);
        return result;
    }

    @Transactional
    public void unbindFile(Long docId, Long linkId, Long userId) {
        PlmObject object = requireObject(docId);
        ensureWritable(object, userId);
        Long owner = jdbcTemplate.query("SELECT owner_object_id FROM file_link WHERE id = ?",
                rs -> rs.next() ? rs.getLong(1) : null, linkId);
        if (owner == null) {
            throw BizException.of(ErrorCode.FILE_LINK_NOT_FOUND);
        }
        if (!owner.equals(docId)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "该文件不属于当前文档");
        }
        fileService.detach(linkId);
        refreshFileStats(docId);
        touch(docId, userId);
    }

    // ------------------------------------------------------------ 检入检出

    @Transactional
    public DocDtos.DocDetail checkout(Long id, String remark, Long userId, String clientIp, String machine) {
        PlmObject object = requireObject(id);
        if (LOCKED_STATES.contains(object.getLifecycleState())) {
            throw BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                    "文档处于「" + object.getLifecycleState() + "」状态，需走变更流程后才能修改");
        }
        checkoutService.acquire(id, userId, clientIp, machine, remark);
        return detail(id);
    }

    /**
     * 检入：生成新 iteration。新版本继承属性与最新文件链接，旧版本保留可下载。
     */
    @Transactional
    public DocDtos.DocDetail checkin(Long id, DocDtos.CheckinReq req, Long userId) {
        PlmObject current = requireObject(id);
        CheckoutService.Lock lock = checkoutService.active(id)
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_CONFLICT, "文档未被检出，无法检入"));
        if (!lock.userId().equals(userId)) {
            throw BizException.of(ErrorCode.COMMON_FORBIDDEN,
                    "该文档由他人检出（用户 " + lock.userId() + "），不能代为检入");
        }

        OffsetDateTime now = OffsetDateTime.now();
        long newId = IdWorker.getId();
        jdbcTemplate.update("""
                        UPDATE plm_object SET is_latest = false, updated_by = ?, updated_at = ?
                         WHERE master_id = ? AND is_latest = true AND deleted = false
                        """, userId, now, current.getMasterId());

        PlmObject next = new PlmObject();
        next.setId(newId);
        next.setObjectType(OBJECT_TYPE);
        next.setMasterId(current.getMasterId());
        next.setCode(current.getCode());
        next.setName(current.getName());
        next.setCompanyId(current.getCompanyId());
        next.setOrgId(current.getOrgId());
        next.setOwnerId(current.getOwnerId());
        next.setOwnerGroupId(current.getOwnerGroupId());
        next.setSecurityLevel(current.getSecurityLevel());
        next.setLifecycleState(current.getLifecycleState());
        next.setPhaseState(current.getPhaseState());
        next.setWorkflowState(current.getWorkflowState());
        next.setRevision(current.getRevision());
        next.setIteration((current.getIteration() == null ? 1 : current.getIteration()) + 1);
        next.setIsLatest(true);
        next.setExtJson(current.getExtJson());
        next.setVersion(0);
        next.setDeleted(false);
        next.setCreatedBy(userId);
        next.setCreatedAt(now);
        next.setUpdatedBy(userId);
        next.setUpdatedAt(now);
        objectMapper.insert(next);

        DocMaster source = docMapper.selectById(id);
        DocMaster master = new DocMaster();
        master.setObjectId(newId);
        master.setDocType(source == null ? null : source.getDocType());
        master.setClassId(source == null ? null : source.getClassId());
        master.setVaultId(source == null ? null : source.getVaultId());
        master.setFileExt(source == null ? null : source.getFileExt());
        master.setFileCount(source == null ? 0 : source.getFileCount());
        master.setPageCount(req != null && req.pageCount() != null
                ? req.pageCount() : (source == null ? null : source.getPageCount()));
        master.setSource(source == null ? SOURCE_NORMAL : source.getSource());
        master.setIsDerived(source == null ? Boolean.FALSE : source.getIsDerived());
        master.setMainFileId(source == null ? null : source.getMainFileId());
        master.setUpdatedAt(now);
        docMapper.insert(master);

        // 文件链接复制到新版本：物理文件共享，引用计数递增
        fileService.copyLatestLinks(id, newId, userId);
        refreshFileStats(newId);

        checkoutService.release(id, userId);
        return detail(newId);
    }

    /** 取消检出：放弃本次编辑，不产生新版本。 */
    @Transactional
    public void cancelCheckout(Long id, Long userId) {
        requireObject(id);
        checkoutService.release(id, userId);
    }

    /** 强制解锁：返回被解锁的原持有者，供审计记录。 */
    @Transactional
    public Long forceUnlock(Long id, Long operatorId) {
        requireObject(id);
        Long holder = checkoutService.forceRelease(id);
        jdbcTemplate.update("""
                        INSERT INTO plm_state_log
                          (id, object_id, state_type, from_state, to_state, action, reason, operator_id, created_at)
                        VALUES (?, ?, 'MAIN', NULL, NULL, 'FORCE_UNLOCK', ?, ?, ?)
                        """,
                IdWorker.getId(), id, "强制解锁（原持有者：" + holder + "）", operatorId, OffsetDateTime.now());
        return holder;
    }

    // ------------------------------------------------------------ 内部

    private void refreshFileStats(Long docId) {
        List<FileService.FileView> files = fileService.listByOwner(docId, true);
        FileService.FileView main = files.stream()
                .filter(file -> FileService.ROLE_MAIN.equals(file.fileRole()))
                .findFirst()
                .orElse(files.isEmpty() ? null : files.get(0));
        docMapper.updateFileStats(docId, files.size(),
                main == null ? null : main.fileId(),
                main == null ? null : main.ext());
    }

    private void touch(Long docId, Long userId) {
        jdbcTemplate.update("UPDATE plm_object SET updated_by = ?, updated_at = ? WHERE id = ?",
                userId, OffsetDateTime.now(), docId);
    }

    private void ensureWritable(PlmObject object, Long userId) {
        if (LOCKED_STATES.contains(object.getLifecycleState())) {
            throw BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                    "文档处于「" + object.getLifecycleState() + "」状态，不可修改");
        }
        checkoutService.active(object.getId()).ifPresent(lock -> {
            if (!lock.userId().equals(userId)) {
                throw BizException.of(ErrorCode.OBJ_CHECKOUT_CONFLICT,
                        "文档已被他人检出（用户 " + lock.userId() + "），当前为只读");
            }
        });
    }

    private PlmObject requireObject(Long id) {
        if (id == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "文档标识不能为空");
        }
        PlmObject object = objectMapper.selectById(id);
        if (object == null || !OBJECT_TYPE.equals(object.getObjectType())) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "文档不存在：" + id);
        }
        return object;
    }

    private String requireEnum(String enumCode, String value, String label) {
        if (value == null || value.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, label + "不能为空");
        }
        List<String> allowed = metaService.getEnum(enumCode).items().stream()
                .map(item -> item.code()).toList();
        if (!allowed.contains(value)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    label + "取值不合法：" + value + "，可选：" + String.join("/", allowed));
        }
        return value;
    }

    private String resolveCode(String code) {
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'PdmDoc', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("DOC-%06d", no == null ? 1L : no);
    }

    private Long companyId(Long userId) {
        return jdbcTemplate.query("SELECT company_id FROM sys_user WHERE id = ?",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
    }

    private Map<String, String> resolveClassAttrs(Long classId, Map<String, String> attrs) {
        if (classId == null) {
            return Map.of();
        }
        return docClassService.normalizeDocAttrs(classId, attrs);
    }

    /** 从 ext_json 读取分类属性值（只保留字符串值）。 */
    private Map<String, String> readClassAttrs(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, String> attrs = new LinkedHashMap<>();
        raw.forEach((key, item) -> {
            if (key != null && item != null) {
                attrs.put(String.valueOf(key), String.valueOf(item));
            }
        });
        return attrs;
    }

    private String className(Long classId) {
        if (classId == null) {
            return null;
        }
        return jdbcTemplate.query("SELECT name FROM doc_class WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null, classId);
    }

    private String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    private String buildExt(String docType, String source, String keyword, String description,
                            Long classId, Map<String, String> classAttrs) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("docType", docType);
        ext.put("source", source);
        if (keyword != null && !keyword.isBlank()) {
            ext.put("keyword", keyword.trim());
        }
        if (description != null && !description.isBlank()) {
            ext.put("description", description);
        }
        if (classId != null) {
            ext.put("classId", classId);
        }
        if (classAttrs != null && !classAttrs.isEmpty()) {
            ext.put("classAttrs", classAttrs);
        }
        try {
            return jsonMapper.writeValueAsString(ext);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.COMMON_INTERNAL, "文档扩展属性序列化失败：" + ex.getMessage());
        }
    }

    private Map<String, Object> readExt(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String asText(Object value, String fallback) {
        return value instanceof String text && !text.isBlank() ? text : fallback;
    }

    private DocDtos.DocSummary toSummary(PlmObject object, DocMaster master, String className) {
        Map<String, Object> ext = readExt(object.getExtJson());
        return new DocDtos.DocSummary(object.getId(), object.getCode(), object.getName(),
                asText(ext.get("docType"), master == null ? null : master.getDocType()),
                asText(ext.get("source"), master == null ? null : master.getSource()),
                object.getLifecycleState(), object.getRevision(), object.getIteration(),
                VersionFormatter.label(object.getRevision(), object.getIteration()),
                master == null ? 0 : master.getFileCount(),
                master == null ? null : master.getFileExt(),
                object.getOwnerId(), object.getUpdatedAt(),
                master == null ? null : master.getClassId(), className);
    }

    private DocDtos.CheckoutInfo toCheckoutInfo(CheckoutService.Lock lock) {
        String label = null;
        try {
            label = jdbcTemplate.queryForObject("SELECT display_name FROM sys_user WHERE id = ?",
                    String.class, lock.userId());
        } catch (Exception ignored) {
            // 用户被删除时降级为裸 ID
        }
        return new DocDtos.CheckoutInfo(lock.userId(), label, lock.checkedOutAt(), lock.expireAt(), false);
    }
}
