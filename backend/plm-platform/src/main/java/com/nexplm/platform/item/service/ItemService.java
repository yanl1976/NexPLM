package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 零部件服务：8 种类型共用一套模型（类型特有字段先以 ext_json 承载），编码自动取号 {@code PN-000001}。
 */
@Service
public class ItemService {

    public static final String OBJECT_TYPE = "PdmItem";

    private static final String DEFAULT_UNIT = "EA";
    private static final List<String> SOURCE_TYPES = List.of("MAKE", "BUY");

    private final PlmObjectMapper objectMapper;
    private final ItemMasterMapper itemMapper;
    private final MetaService metaService;
    private final LifecycleService lifecycleService;
    private final ScopeApplier scopeApplier;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public ItemService(PlmObjectMapper objectMapper, ItemMasterMapper itemMapper, MetaService metaService,
                       LifecycleService lifecycleService, ScopeApplier scopeApplier,
                       JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.objectMapper = objectMapper;
        this.itemMapper = itemMapper;
        this.metaService = metaService;
        this.lifecycleService = lifecycleService;
        this.scopeApplier = scopeApplier;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    public PageResult<ItemDtos.ItemSummary> page(int page, int size, String keyword,
                                                 String itemType, String sourceType) {
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
        scopeApplier.apply(wrapper, PlmObject::getCompanyId, PlmObject::getOrgId, PlmObject::getOwnerId);

        IPage<PlmObject> result = objectMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageNo, pageSize), wrapper);
        List<Long> ids = result.getRecords().stream().map(PlmObject::getId).toList();
        Map<Long, ItemMaster> masters = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (ItemMaster master : itemMapper.selectList(
                    new LambdaQueryWrapper<ItemMaster>().in(ItemMaster::getObjectId, ids))) {
                masters.put(master.getObjectId(), master);
            }
        }
        List<ItemDtos.ItemSummary> items = result.getRecords().stream()
                .map(object -> toSummary(object, masters.get(object.getId())))
                .toList();
        return PageResult.of(items, result.getTotal(), pageNo, pageSize);
    }

    public ItemDtos.ItemDetail detail(Long id) {
        PlmObject object = requireItem(id);
        ItemMaster master = itemMapper.selectById(id);
        Map<String, Object> ext = readExt(object.getExtJson());
        return new ItemDtos.ItemDetail(id, object.getCode(), object.getName(),
                master == null ? null : master.getItemType(),
                master == null ? text(ext.get("specification")) : master.getSpecification(),
                master == null ? text(ext.get("material")) : master.getMaterial(),
                master == null ? DEFAULT_UNIT : master.getUnit(),
                master == null ? null : master.getWeight(),
                master == null ? text(ext.get("sourceType")) : master.getSourceType(),
                master == null ? text(ext.get("stdCode")) : master.getStdCode(),
                master == null ? text(ext.get("remark")) : master.getRemark(),
                object.getLifecycleState(), object.getRevision(), object.getIteration(),
                VersionFormatter.label(object.getRevision(), object.getIteration()),
                object.getIsLatest(), object.getOwnerId(), object.getOrgId(),
                object.getSecurityLevel(), object.getCreatedAt(), object.getUpdatedAt());
    }

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
        object.setWorkflowState("NONE");
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
        master.setCreatedAt(now);
        master.setUpdatedAt(now);
        itemMapper.insert(master);
        return detail(id);
    }

    @Transactional
    public ItemDtos.ItemDetail update(Long id, ItemDtos.ItemSaveReq req, Long userId) {
        PlmObject object = requireItem(id);
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
        if (master == null) {
            master = new ItemMaster();
            master.setObjectId(id);
            master.setCreatedAt(OffsetDateTime.now());
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
        if (itemMapper.selectById(id) == null) {
            itemMapper.insert(master);
        } else {
            itemMapper.updateById(master);
        }
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
        // deleted 为 @TableLogic，必须走 deleteById 才会生成逻辑删除语句
        objectMapper.deleteById(object.getId());
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

    private ItemDtos.ItemSummary toSummary(PlmObject object, ItemMaster master) {
        Map<String, Object> ext = readExt(object.getExtJson());
        return new ItemDtos.ItemSummary(object.getId(), object.getCode(), object.getName(),
                master == null ? text(ext.get("itemType")) : master.getItemType(),
                master == null ? text(ext.get("specification")) : master.getSpecification(),
                master == null ? text(ext.get("material")) : master.getMaterial(),
                master == null ? DEFAULT_UNIT : master.getUnit(),
                master == null ? text(ext.get("sourceType")) : master.getSourceType(),
                object.getLifecycleState(), object.getRevision(), object.getIteration(),
                VersionFormatter.label(object.getRevision(), object.getIteration()),
                object.getUpdatedAt());
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
        Long companyId = jdbcTemplate.query("SELECT company_id FROM sys_user WHERE id = ?",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
        return companyId;
    }
}
