package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.service.CheckoutService;
import com.nexplm.platform.item.api.ItemDtos;
import com.nexplm.platform.item.domain.ItemMaster;
import com.nexplm.platform.item.mapper.ItemMasterMapper;
import com.nexplm.platform.lifecycle.service.LifecycleService;
import com.nexplm.platform.metadata.service.MetaService;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.object.version.VersionFormatter;
import com.nexplm.platform.security.scope.ScopeApplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 零部件服务：8 种类型共用一套模型（类型特有字段先以 ext_json 承载），编码自动取号 {@code PN-000001}。
 * <p>页面复刻参照旧系统 PSEITEM：列表并列三状态（生命周期/使用状态/流程状态）、
 * 设计属性独立表 {@code item_design_attr}、零件视图层 {@code item_view}。</p>
 */
@Service
public class ItemService {

    public static final String OBJECT_TYPE = "PdmItem";

    private static final String DEFAULT_UNIT = "EA";
    private static final List<String> SOURCE_TYPES = List.of("MAKE", "BUY");
    /** 旧系统：已发布/已废弃只能走修订，不能直接改。 */
    private static final Set<String> LOCKED_STATES = Set.of("Released", "Obsolete");
    private static final List<String> USING_STATES = List.of("Using", "StopUsing", "GiveUp");

    private final PlmObjectMapper objectMapper;
    private final ItemMasterMapper itemMapper;
    private final MetaService metaService;
    private final LifecycleService lifecycleService;
    private final ScopeApplier scopeApplier;
    private final CheckoutService checkoutService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public ItemService(PlmObjectMapper objectMapper, ItemMasterMapper itemMapper, MetaService metaService,
                       LifecycleService lifecycleService, ScopeApplier scopeApplier,
                       CheckoutService checkoutService, JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.objectMapper = objectMapper;
        this.itemMapper = itemMapper;
        this.metaService = metaService;
        this.lifecycleService = lifecycleService;
        this.scopeApplier = scopeApplier;
        // 检出锁按 object_id 通用（plm_checkout），零部件与图文档共用一套实现
        this.checkoutService = checkoutService;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    // ------------------------------------------------------------ 列表

    /**
     * 列表查询：对齐旧系统 8 项条件（名称/编号/类型/创建人/创建时间区间/发布时间区间）+ 使用状态、材料、规格。
     */
    public PageResult<ItemDtos.ItemSummary> page(int page, int size, String keyword,
                                                 String itemType, String sourceType, String usingState,
                                                 String phaseState, String workflowState, Long createdBy,
                                                 LocalDate createdFrom, LocalDate createdTo,
                                                 LocalDate releasedFrom, LocalDate releasedTo,
                                                 String material, String specification) {
        int pageNo = Math.max(1, page);
        int pageSize = Math.min(Math.max(1, size), 200);
        LambdaQueryWrapper<PlmObject> wrapper = new LambdaQueryWrapper<PlmObject>()
                .eq(PlmObject::getObjectType, OBJECT_TYPE)
                .eq(PlmObject::getIsLatest, Boolean.TRUE)
                .orderByDesc(PlmObject::getUpdatedAt);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PlmObject::getCode, kw)
                    .or().like(PlmObject::getName, kw)
                    .or().apply("ext_json ->> 'specification' ILIKE {0}", "%" + kw + "%"));
        }
        if (itemType != null && !itemType.isBlank()) {
            wrapper.apply("id IN (SELECT object_id FROM item_master WHERE item_type = {0})",
                    itemType.trim().toUpperCase(Locale.ROOT));
        }
        if (sourceType != null && !sourceType.isBlank()) {
            wrapper.apply("id IN (SELECT object_id FROM item_master WHERE source_type = {0})",
                    sourceType.trim().toUpperCase(Locale.ROOT));
        }
        if (usingState != null && !usingState.isBlank()) {
            wrapper.apply("id IN (SELECT object_id FROM item_master WHERE using_state = {0})",
                    usingState.trim());
        }
        if (material != null && !material.isBlank()) {
            wrapper.apply("id IN (SELECT object_id FROM item_master WHERE material ILIKE {0})",
                    "%" + material.trim() + "%");
        }
        if (specification != null && !specification.isBlank()) {
            wrapper.apply("id IN (SELECT object_id FROM item_master WHERE specification ILIKE {0})",
                    "%" + specification.trim() + "%");
        }
        if (phaseState != null && !phaseState.isBlank()) {
            wrapper.eq(PlmObject::getPhaseState, phaseState.trim());
        }
        if (workflowState != null && !workflowState.isBlank()) {
            wrapper.eq(PlmObject::getWorkflowState, workflowState.trim());
        }
        if (createdBy != null) {
            wrapper.eq(PlmObject::getCreatedBy, createdBy);
        }
        if (createdFrom != null) {
            wrapper.ge(PlmObject::getCreatedAt, createdFrom.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
        }
        if (createdTo != null) {
            wrapper.le(PlmObject::getCreatedAt, createdTo.atTime(LocalTime.MAX).atOffset(OffsetDateTime.now().getOffset()));
        }
        if (releasedFrom != null) {
            wrapper.ge(PlmObject::getReleasedAt, releasedFrom.atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
        }
        if (releasedTo != null) {
            wrapper.le(PlmObject::getReleasedAt, releasedTo.atTime(LocalTime.MAX).atOffset(OffsetDateTime.now().getOffset()));
        }
        scopeApplier.apply(wrapper, PlmObject::getCompanyId, PlmObject::getOrgId, PlmObject::getOwnerId);

        IPage<PlmObject> result = objectMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, pageSize), wrapper);
        List<PlmObject> records = result.getRecords();
        List<Long> ids = records.stream().map(PlmObject::getId).toList();
        Map<Long, ItemMaster> masters = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ItemMaster master : itemMapper.selectList(
                    new LambdaQueryWrapper<ItemMaster>().in(ItemMaster::getObjectId, ids))) {
                masters.put(master.getObjectId(), master);
            }
        }
        Map<Long, String> userNames = userNames(records);
        List<ItemDtos.ItemSummary> items = records.stream()
                .map(object -> toSummary(object, masters.get(object.getId()), userNames))
                .toList();
        return PageResult.of(items, result.getTotal(), pageNo, pageSize);
    }

    // ------------------------------------------------------------ 详情

    public ItemDtos.ItemDetail detail(Long id) {
        PlmObject object = requireItem(id);
        ItemMaster master = itemMapper.selectById(id);
        Map<String, Object> ext = readExt(object.getExtJson());
        Map<Long, String> names = userNames(List.of(object));
        return new ItemDtos.ItemDetail(id, object.getCode(), object.getName(),
                master == null ? null : master.getItemType(),
                master == null ? text(ext.get("specification")) : master.getSpecification(),
                master == null ? text(ext.get("material")) : master.getMaterial(),
                master == null ? DEFAULT_UNIT : master.getUnit(),
                master == null ? null : master.getWeight(),
                master == null ? text(ext.get("sourceType")) : master.getSourceType(),
                master == null ? text(ext.get("stdCode")) : master.getStdCode(),
                master == null ? text(ext.get("remark")) : master.getRemark(),
                object.getLifecycleState(), object.getPhaseState(),
                master == null || master.getUsingState() == null ? "Using" : master.getUsingState(),
                object.getWorkflowState(), object.getRevision(), object.getIteration(),
                VersionFormatter.label(object.getRevision(), object.getIteration()),
                object.getIsLatest(), object.getOwnerId(), object.getOrgId(),
                object.getSecurityLevel(), object.getCreatedBy(), names.get(object.getCreatedBy()),
                object.getUpdatedBy(), names.get(object.getUpdatedBy()), object.getReleasedAt(),
                object.getCreatedAt(), object.getUpdatedAt(),
                designAttr(id), checkoutInfo(id));
    }

    /** 设计属性（旧系统 PSEBASEFORM）；未填过时返回空对象而不是 null，前端表单绑定更稳。 */
    public ItemDtos.ItemDesignAttr designAttr(Long id) {
        return jdbcTemplate.query("""
                        SELECT provider, dwg_no, product_category, product_model, product_spec,
                               material_type, material_type2, surface_treatment, is_tooling,
                               weight_unit, montage, color_item
                          FROM item_design_attr WHERE object_id = ?
                        """,
                rs -> rs.next()
                        ? new ItemDtos.ItemDesignAttr(rs.getString("provider"), rs.getString("dwg_no"),
                        rs.getString("product_category"), rs.getString("product_model"),
                        rs.getString("product_spec"), rs.getString("material_type"),
                        rs.getString("material_type2"), rs.getString("surface_treatment"),
                        rs.getBoolean("is_tooling"), rs.getString("weight_unit"),
                        rs.getString("montage"), rs.getBoolean("color_item"))
                        : new ItemDtos.ItemDesignAttr(null, null, null, null, null, null, null, null,
                        false, null, null, false),
                id);
    }

    public ItemDtos.CheckoutInfo checkoutInfo(Long id) {
        Optional<CheckoutService.Lock> active = checkoutService.active(id);
        if (active.isEmpty()) {
            return null;
        }
        CheckoutService.Lock lock = active.get();
        Map<Long, String> names = userNameById(lock.userId());
        Long current = com.nexplm.platform.security.CurrentUser.userId();
        return new ItemDtos.CheckoutInfo(lock.userId(), names.get(lock.userId()),
                lock.checkedOutAt(), lock.expireAt(), current != null && current.equals(lock.userId()));
    }

    // ------------------------------------------------------------ 增删改

    @Transactional
    public ItemDtos.ItemDetail create(ItemDtos.ItemSaveReq req, Long userId) {
        String itemType = normalizeItemType(req.itemType());
        String sourceType = normalizeSourceType(req.sourceType());
        String unit = normalizeUnit(req.unit());
        long id = IdWorker.getId();
        OffsetDateTime now = OffsetDateTime.now();

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
        // 旧系统流程状态默认 New；新系统历史上写 NONE，前端字典两者都映射为「新建」
        object.setWorkflowState("New");
        object.setRevision(1);
        object.setIteration(1);
        object.setIsLatest(true);
        object.setExtJson(buildExt(itemType, req, unit, sourceType));
        object.setVersion(0);
        object.setDeleted(false);
        object.setCreatedBy(userId);
        object.setCreatedAt(now);
        object.setUpdatedBy(userId);
        object.setUpdatedAt(now);
        objectMapper.insert(object);

        ItemMaster master = new ItemMaster();
        master.setObjectId(id);
        master.setItemType(itemType);
        master.setSpecification(trimToNull(req.specification()));
        master.setMaterial(trimToNull(req.material()));
        master.setUnit(unit);
        master.setWeight(req.weight());
        master.setSourceType(sourceType);
        master.setStdCode(trimToNull(req.stdCode()));
        master.setRemark(trimToNull(req.remark()));
        master.setUsingState("Using");
        master.setCreatedAt(now);
        master.setUpdatedAt(now);
        itemMapper.insert(master);

        saveDesignAttr(id, req.designAttr(), now);
        // 每个零件至少有一个默认设计视图（旧系统一样：新建零件自动带 Design 视图）
        createView(id, new ItemDtos.ItemViewReq("DESIGN", "默认设计视图", null), userId);
        return detail(id);
    }

    @Transactional
    public ItemDtos.ItemDetail update(Long id, ItemDtos.ItemSaveReq req, Long userId) {
        PlmObject object = requireItem(id);
        ensureEditable(object, userId);
        String itemType = normalizeItemType(req.itemType());
        String sourceType = normalizeSourceType(req.sourceType());
        String unit = normalizeUnit(req.unit());

        PlmObject update = new PlmObject();
        update.setId(id);
        update.setVersion(object.getVersion());
        update.setName(req.name().trim());
        update.setOrgId(req.orgId());
        update.setSecurityLevel(req.securityLevel() == null ? 0 : req.securityLevel());
        update.setExtJson(buildExt(itemType, req, unit, sourceType));
        update.setUpdatedBy(userId);
        update.setUpdatedAt(OffsetDateTime.now());
        if (objectMapper.updateById(update) == 0) {
            throw BizException.of(ErrorCode.OBJ_VERSION_CONFLICT);
        }

        ItemMaster master = itemMapper.selectById(id);
        boolean insert = master == null;
        if (insert) {
            master = new ItemMaster();
            master.setObjectId(id);
            master.setCreatedAt(OffsetDateTime.now());
            master.setUsingState("Using");
        }
        master.setItemType(itemType);
        master.setSpecification(trimToNull(req.specification()));
        master.setMaterial(trimToNull(req.material()));
        master.setUnit(unit);
        master.setWeight(req.weight());
        master.setSourceType(sourceType);
        master.setStdCode(trimToNull(req.stdCode()));
        master.setRemark(trimToNull(req.remark()));
        master.setUpdatedAt(OffsetDateTime.now());
        if (insert) {
            itemMapper.insert(master);
        } else {
            itemMapper.updateById(master);
        }
        saveDesignAttr(id, req.designAttr(), OffsetDateTime.now());
        return detail(id);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        PlmObject object = requireItem(id);
        Long children = jdbcTemplate.query("SELECT COUNT(1) FROM bom_line WHERE parent_object_id = ? AND deleted = false",
                rs -> rs.next() ? rs.getLong(1) : 0L, id);
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该零部件仍有 " + children + " 条子件，不能删除");
        }
        Long parents = jdbcTemplate.query("SELECT COUNT(1) FROM bom_line WHERE child_object_id = ? AND deleted = false",
                rs -> rs.next() ? rs.getLong(1) : 0L, id);
        if (parents != null && parents > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该零部件已被 " + parents + " 处 BOM 引用，不能删除");
        }
        jdbcTemplate.update("UPDATE item_view SET deleted = true, updated_at = now() WHERE item_object_id = ?", id);
        checkoutService.forceRelease(id);
        // deleted 为 @TableLogic，必须走 deleteById 才会生成逻辑删除语句
        objectMapper.deleteById(object.getId());
    }

    /** 批量删除：逐条独立事务语义，返回部分成功结果（与 BOM 批量口径一致）。 */
    public ItemDtos.BatchResult batchDelete(List<Long> ids, Long userId) {
        List<String> messages = new ArrayList<>();
        int done = 0;
        int skipped = 0;
        for (Long id : ids) {
            try {
                delete(id, userId);
                done++;
            } catch (BizException ex) {
                skipped++;
                messages.add(id + "：" + ex.getMessage());
            }
        }
        return new ItemDtos.BatchResult(ids.size(), done, skipped, messages);
    }

    /** 使用状态迁移（旧系统 使用状态机：Using → StopUsing → GiveUp）。 */
    @Transactional
    public ItemDtos.ItemDetail changeUsingState(Long id, String usingState, Long userId) {
        requireItem(id);
        String target = usingState == null ? "" : usingState.trim();
        if (!USING_STATES.contains(target)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不支持的使用状态：" + usingState);
        }
        jdbcTemplate.update("UPDATE item_master SET using_state = ?, updated_at = now() WHERE object_id = ?",
                target, id);
        jdbcTemplate.update("UPDATE plm_object SET updated_by = ?, updated_at = now() WHERE id = ?", userId, id);
        return detail(id);
    }

    // ------------------------------------------------------------ 视图（旧系统 PSEITEMVIEW）

    public List<ItemDtos.ItemViewRow> listViews(Long itemId) {
        PlmObject object = requireItem(itemId);
        return jdbcTemplate.query("""
                        SELECT v.id, v.view_type, v.description, v.is_default, v.created_at,
                               (SELECT COUNT(1) FROM bom_line b
                                 WHERE b.parent_object_id = v.item_object_id
                                   AND b.view_type = v.view_type AND b.deleted = false) AS bom_line_count
                          FROM item_view v
                         WHERE v.item_object_id = ? AND v.deleted = false
                         ORDER BY v.is_default DESC, v.view_type
                        """,
                (rs, rowNum) -> new ItemDtos.ItemViewRow(
                        rs.getLong("id"), rs.getString("view_type"), viewTypeLabel(rs.getString("view_type")),
                        rs.getString("description"), rs.getBoolean("is_default"),
                        object.getLifecycleState(), object.getWorkflowState(),
                        rs.getInt("bom_line_count"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                itemId);
    }

    @Transactional
    public ItemDtos.ItemViewRow createView(Long itemId, ItemDtos.ItemViewReq req, Long userId) {
        requireItem(itemId);
        String viewType = req.viewType() == null || req.viewType().isBlank()
                ? "DESIGN" : req.viewType().trim().toUpperCase(Locale.ROOT);
        Long exists = jdbcTemplate.query("""
                        SELECT COUNT(1) FROM item_view
                         WHERE item_object_id = ? AND view_type = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getLong(1) : 0L, itemId, viewType);
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该零件已存在「" + viewTypeLabel(viewType) + "」");
        }
        long id = IdWorker.getId();
        OffsetDateTime now = OffsetDateTime.now();
        boolean isDefault = jdbcTemplate.query("SELECT COUNT(1) FROM item_view WHERE item_object_id = ? AND deleted = false",
                rs -> rs.next() ? rs.getLong(1) : 0L, itemId) == 0;
        jdbcTemplate.update("""
                        INSERT INTO item_view (id, item_object_id, view_type, description, is_default,
                                               source_view_id, created_by, created_at, updated_by, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, itemId, viewType, trimToNull(req.description()), isDefault,
                req.copyFromViewId(), userId, now, userId, now);
        if (req.copyFromViewId() != null) {
            copyBomFromView(itemId, req.copyFromViewId(), viewType, userId, now);
        }
        return listViews(itemId).stream().filter(v -> v.id().equals(id)).findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_NOT_FOUND, "视图创建失败"));
    }

    @Transactional
    public void deleteView(Long itemId, Long viewId, Long userId) {
        requireItem(itemId);
        Map<String, Object> row = jdbcTemplate.query("""
                        SELECT view_type, is_default FROM item_view
                         WHERE id = ? AND item_object_id = ? AND deleted = false
                        """,
                rs -> rs.next() ? Map.<String, Object>of("viewType", rs.getString(1), "isDefault", rs.getBoolean(2))
                        : null, viewId, itemId);
        if (row == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "视图不存在：" + viewId);
        }
        if (Boolean.TRUE.equals(row.get("isDefault"))) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "默认视图不允许删除");
        }
        Long lines = jdbcTemplate.query("""
                        SELECT COUNT(1) FROM bom_line
                         WHERE parent_object_id = ? AND view_type = ? AND deleted = false
                        """, rs -> rs.next() ? rs.getLong(1) : 0L, itemId, row.get("viewType"));
        if (lines != null && lines > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT,
                    "该视图下还有 " + lines + " 条 BOM 行，请先清空再删除");
        }
        jdbcTemplate.update("UPDATE item_view SET deleted = true, updated_by = ?, updated_at = now() WHERE id = ?",
                userId, viewId);
    }

    private void copyBomFromView(Long itemId, Long sourceViewId, String targetViewType, Long userId, OffsetDateTime now) {
        String sourceType = jdbcTemplate.query("SELECT view_type FROM item_view WHERE id = ? AND item_object_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, sourceViewId, itemId);
        if (sourceType == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "复制来源视图不存在：" + sourceViewId);
        }
        jdbcTemplate.update("""
                        INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty,
                                              unit, loss_rate, ref_designator, remark, version,
                                              created_by, created_at, updated_by, updated_at)
                        SELECT ? + row_number() OVER (ORDER BY line_no, id), parent_object_id, child_object_id,
                               ?, line_no, qty, unit, loss_rate, ref_designator, remark, 0,
                               ?, ?, ?, ?
                          FROM bom_line
                         WHERE parent_object_id = ? AND view_type = ? AND deleted = false
                        """,
                IdWorker.getId(), targetViewType, userId, now, userId, now, itemId, sourceType);
    }

    // ------------------------------------------------------------ 内部

    public PlmObject requireItem(Long id) {
        if (id == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "零部件标识不能为空");
        }
        PlmObject object = objectMapper.selectById(id);
        if (object == null || !OBJECT_TYPE.equals(object.getObjectType())) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "零部件不存在：" + id);
        }
        // 按 id 直取也要过数据作用域：列表过滤不到的对象不能靠拼 id 拿到，越权按"不存在"处理
        if (!scopeApplier.isVisible(object.getCompanyId(), object.getOrgId(), object.getOwnerId())) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "零部件不存在：" + id);
        }
        return object;
    }

    /**
     * 可编辑性：已发布/已废弃只能走修订（旧系统同一规则）；他人检出时只读。
     */
    public void ensureEditable(PlmObject object, Long userId) {
        if (LOCKED_STATES.contains(object.getLifecycleState())) {
            throw BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                    "零部件处于「" + object.getLifecycleState() + "」状态，请先修订再修改");
        }
        checkoutService.active(object.getId()).ifPresent(lock -> {
            if (userId != null && !lock.userId().equals(userId)) {
                throw BizException.of(ErrorCode.OBJ_CHECKOUT_CONFLICT,
                        "该零部件已被他人检出（用户 " + lock.userId() + "），当前为只读");
            }
        });
    }

    private void saveDesignAttr(Long id, ItemDtos.ItemDesignAttr attr, OffsetDateTime now) {
        if (attr == null) {
            return;
        }
        jdbcTemplate.update("""
                        INSERT INTO item_design_attr (object_id, provider, dwg_no, product_category, product_model,
                                                      product_spec, material_type, material_type2, surface_treatment,
                                                      is_tooling, weight_unit, montage, color_item,
                                                      created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (object_id) DO UPDATE SET
                            provider = EXCLUDED.provider, dwg_no = EXCLUDED.dwg_no,
                            product_category = EXCLUDED.product_category,
                            product_model = EXCLUDED.product_model,
                            product_spec = EXCLUDED.product_spec,
                            material_type = EXCLUDED.material_type,
                            material_type2 = EXCLUDED.material_type2,
                            surface_treatment = EXCLUDED.surface_treatment,
                            is_tooling = EXCLUDED.is_tooling, weight_unit = EXCLUDED.weight_unit,
                            montage = EXCLUDED.montage, color_item = EXCLUDED.color_item,
                            updated_at = EXCLUDED.updated_at
                        """,
                id, trimToNull(attr.provider()), trimToNull(attr.dwgNo()),
                trimToNull(attr.productCategory()), trimToNull(attr.productModel()),
                trimToNull(attr.productSpec()), trimToNull(attr.materialType()),
                trimToNull(attr.materialType2()), trimToNull(attr.surfaceTreatment()),
                Boolean.TRUE.equals(attr.isTooling()), trimToNull(attr.weightUnit()),
                trimToNull(attr.montage()), Boolean.TRUE.equals(attr.colorItem()), now, now);
    }

    private ItemDtos.ItemSummary toSummary(PlmObject object, ItemMaster master, Map<Long, String> userNames) {
        Map<String, Object> ext = readExt(object.getExtJson());
        return new ItemDtos.ItemSummary(object.getId(), object.getCode(), object.getName(),
                master == null ? text(ext.get("itemType")) : master.getItemType(),
                master == null ? text(ext.get("specification")) : master.getSpecification(),
                master == null ? text(ext.get("material")) : master.getMaterial(),
                master == null ? DEFAULT_UNIT : master.getUnit(),
                master == null ? text(ext.get("sourceType")) : master.getSourceType(),
                object.getLifecycleState(), object.getPhaseState(),
                master == null || master.getUsingState() == null ? "Using" : master.getUsingState(),
                object.getWorkflowState(), object.getRevision(), object.getIteration(),
                VersionFormatter.label(object.getRevision(), object.getIteration()),
                object.getCreatedBy(), userNames.get(object.getCreatedBy()),
                object.getUpdatedBy(), userNames.get(object.getUpdatedBy()),
                object.getReleasedAt(), object.getCreatedAt(), object.getUpdatedAt());
    }

    private Map<Long, String> userNames(List<PlmObject> objects) {
        Set<Long> ids = new java.util.LinkedHashSet<>();
        for (PlmObject object : objects) {
            if (object.getCreatedBy() != null) {
                ids.add(object.getCreatedBy());
            }
            if (object.getUpdatedBy() != null) {
                ids.add(object.getUpdatedBy());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        jdbcTemplate.query("SELECT id, display_name FROM sys_user WHERE id IN (" + placeholders + ")",
                rs -> {
                    names.put(rs.getLong(1), rs.getString(2));
                }, ids.toArray());
        return names;
    }

    private Map<Long, String> userNameById(Long userId) {
        Map<Long, String> names = new HashMap<>();
        if (userId == null) {
            return names;
        }
        jdbcTemplate.query("SELECT id, display_name FROM sys_user WHERE id = ?",
                rs -> {
                    names.put(rs.getLong(1), rs.getString(2));
                }, userId);
        return names;
    }

    private String viewTypeLabel(String viewType) {
        if (viewType == null) {
            return "";
        }
        return switch (viewType.toUpperCase(Locale.ROOT)) {
            case "DESIGN" -> "设计视图";
            case "PROCESS" -> "工艺视图";
            case "MANUFACTURE" -> "制造视图";
            default -> viewType;
        };
    }

    private String buildExt(String itemType, ItemDtos.ItemSaveReq req, String unit, String sourceType) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("itemType", itemType);
        ext.put("sourceType", sourceType);
        ext.put("unit", unit);
        if (req.specification() != null && !req.specification().isBlank()) {
            ext.put("specification", req.specification().trim());
        }
        if (req.material() != null && !req.material().isBlank()) {
            ext.put("material", req.material().trim());
        }
        if (req.weight() != null) {
            ext.put("weight", req.weight());
        }
        if (req.stdCode() != null && !req.stdCode().isBlank()) {
            ext.put("stdCode", req.stdCode().trim());
        }
        if (req.remark() != null && !req.remark().isBlank()) {
            ext.put("remark", req.remark().trim());
        }
        try {
            return jsonMapper.writeValueAsString(ext);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.COMMON_INTERNAL, "序列化零部件扩展字段失败");
        }
    }

    private Map<String, Object> readExt(String extJson) {
        if (extJson == null || extJson.isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(extJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveCode(String code) {
        if (code != null && !code.isBlank()) {
            return code.trim();
        }
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, 'PdmItem', 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId());
        return String.format("PN-%06d", no == null ? 1L : no);
    }

    private String normalizeItemType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "零部件类型不能为空");
        }
        String normalized = itemType.trim().toUpperCase(Locale.ROOT);
        boolean matched = metaService.getEnum("ItemType").items().stream()
                .anyMatch(item -> item.code().equals(normalized));
        if (!matched) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不支持的零部件类型：" + itemType);
        }
        return normalized;
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return "MAKE";
        }
        String normalized = sourceType.trim().toUpperCase(Locale.ROOT);
        if (!SOURCE_TYPES.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "供应方式仅支持 MAKE（自制）/ BUY（外购）");
        }
        return normalized;
    }

    private String normalizeUnit(String unit) {
        return unit == null || unit.isBlank() ? DEFAULT_UNIT : unit.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private Long companyId(Long userId) {
        return jdbcTemplate.query("SELECT company_id FROM sys_user WHERE id = ?",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
    }
}
