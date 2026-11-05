package com.nexplm.platform.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.metadata.api.MetaDtos;
import com.nexplm.platform.metadata.domain.MetaEntity;
import com.nexplm.platform.metadata.domain.MetaEnum;
import com.nexplm.platform.metadata.domain.MetaField;
import com.nexplm.platform.metadata.mapper.MetaEntityMapper;
import com.nexplm.platform.metadata.mapper.MetaEnumMapper;
import com.nexplm.platform.metadata.mapper.MetaFieldMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元数据服务：对象类型与字段的定义、发布、查询（带本地缓存）。
 * <p>发布前的校验规则：类型编码唯一、至少一个字段、字段编码唯一、值类型合法、引用枚举存在。</p>
 */
@Service
public class MetaService {

    /** 值类型白名单（与方案 5.1 的 31 种语义类型保持一致）。 */
    private static final Set<String> VALUE_TYPES = Set.of(
            "DefaultObjectValue", "DescriptionValue", "BoolValue", "DateTimeValue", "DateValue",
            "CodeValue", "StateValue", "StageValue",
            "EnumValue", "MultiEnumValue", "EnumRadioValue", "LovListValue",
            "VersionValue", "MinorVersionValue", "MajorMinorVersionValue",
            "SingleUserValue", "MultiUserValue", "SingleUserAutoComboxValue", "SingleGroupValue",
            "CompanyValue", "TaskPrincipalValue",
            "PositionValue", "SelectPositionValue",
            "ObjectIDValue", "SubDataModelValue", "SubDataModelIconValue", "PseItemNameCodeValue",
            "PseBomViewDataValue", "FileUploadValue", "EditButtonValue",
            "ThickRuleValue", "ThinRuleValue");

    private static final Set<String> DATA_TYPES = Set.of(
            "STRING", "VARCHAR", "TEXT", "INT", "BIGINT", "DECIMAL", "BOOLEAN", "DATETIME", "DATE", "JSON");

    private static final Set<String> VERSION_POLICIES = Set.of("LETTER", "NUMBER", "NONE");

    private final MetaEntityMapper entityMapper;
    private final MetaFieldMapper fieldMapper;
    private final MetaEnumMapper enumMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, MetaDtos.EntityDetail> detailCache = new ConcurrentHashMap<>();

    public MetaService(MetaEntityMapper entityMapper, MetaFieldMapper fieldMapper, MetaEnumMapper enumMapper,
                       JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.entityMapper = entityMapper;
        this.fieldMapper = fieldMapper;
        this.enumMapper = enumMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<MetaDtos.EntitySummary> listEntities(String keyword) {
        LambdaQueryWrapper<MetaEntity> query = new LambdaQueryWrapper<MetaEntity>()
                .orderByAsc(MetaEntity::getCode);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            query.and(w -> w.like(MetaEntity::getCode, kw).or().like(MetaEntity::getName, kw));
        }
        List<MetaEntity> entities = entityMapper.selectList(query);
        Map<String, Long> counts = fieldCounts();
        return entities.stream()
                .map(e -> new MetaDtos.EntitySummary(e.getCode(), e.getName(), e.getNameEn(), e.getParentCode(),
                        e.getStatus(), e.getVersionNo(), counts.getOrDefault(e.getCode(), 0L), e.getUpdatedAt()))
                .toList();
    }

    private Map<String, Long> fieldCounts() {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query("SELECT entity_code, COUNT(1) FROM meta_field GROUP BY entity_code",
                rs -> {
                    counts.put(rs.getString(1), rs.getLong(2));
                });
        return counts;
    }

    public MetaDtos.EntityDetail getEntity(String code) {
        MetaDtos.EntityDetail cached = detailCache.get(code);
        if (cached != null) {
            return cached;
        }
        MetaEntity entity = requireEntity(code);
        List<MetaField> fields = fieldMapper.selectList(new LambdaQueryWrapper<MetaField>()
                .eq(MetaField::getEntityCode, code)
                .orderByAsc(MetaField::getSort)
                .orderByAsc(MetaField::getCode));
        MetaDtos.EntityDetail detail = new MetaDtos.EntityDetail(entity.getCode(), entity.getName(),
                entity.getNameEn(), entity.getParentCode(), entity.getAbstractFlag(),
                readCapabilities(entity.getCapabilities()), entity.getVersionPolicy(), entity.getIterationPolicy(),
                entity.getLifecycleCode(), entity.getWorkflowAllowed(), entity.getStatus(), entity.getVersionNo(),
                fields.stream().map(this::toFieldDef).toList());
        detailCache.put(code, detail);
        return detail;
    }

    @Transactional
    public void saveEntity(MetaDtos.EntitySaveReq req, boolean create) {
        validateVersionPolicy(req.versionPolicy());
        if (req.parentCode() != null && !req.parentCode().isBlank()
                && entityMapper.selectCount(new LambdaQueryWrapper<MetaEntity>()
                .eq(MetaEntity::getCode, req.parentCode())) == 0) {
            throw BizException.of(ErrorCode.META_FIELD_INVALID, "父类型不存在：" + req.parentCode());
        }
        MetaEntity entity;
        if (create) {
            if (entityMapper.selectCount(new LambdaQueryWrapper<MetaEntity>()
                    .eq(MetaEntity::getCode, req.code())) > 0) {
                throw BizException.of(ErrorCode.META_ENTITY_DUPLICATED);
            }
            entity = new MetaEntity();
            entity.setCode(req.code());
            entity.setTableName("plm_object");
            entity.setAbstractFlag(false);
            entity.setIterationPolicy("NUMBER");
            entity.setSecurityLevel(0);
            entity.setSchemaJson("{}");
            entity.setVersionNo(1);
            entity.setStatus("DRAFT");
            entity.setCreatedAt(OffsetDateTime.now());
        } else {
            entity = requireEntity(req.code());
        }
        entity.setName(req.name().trim());
        entity.setNameEn(req.nameEn());
        entity.setParentCode(blankToNull(req.parentCode()));
        entity.setLifecycleCode(blankToNull(req.lifecycleCode()));
        entity.setVersionPolicy(req.versionPolicy() == null || req.versionPolicy().isBlank()
                ? "LETTER" : req.versionPolicy());
        entity.setWorkflowAllowed(Boolean.TRUE.equals(req.workflowAllowed()));
        entity.setCapabilities(writeCapabilities(req.capabilities()));
        entity.setUpdatedAt(OffsetDateTime.now());

        if (create) {
            entityMapper.insert(entity);
        } else {
            entityMapper.updateById(entity);
        }
        if (req.fields() != null) {
            replaceFields(entity.getCode(), req.fields());
        }
        invalidate(entity.getCode());
    }

    private void replaceFields(String entityCode, List<MetaDtos.FieldDef> fields) {
        Set<String> codes = new HashSet<>();
        for (MetaDtos.FieldDef field : fields) {
            if (!codes.add(field.code())) {
                throw BizException.of(ErrorCode.META_FIELD_INVALID, "字段编码重复：" + field.code());
            }
            validateField(field);
        }
        fieldMapper.deleteByEntity(entityCode);
        int sort = 10;
        for (MetaDtos.FieldDef field : fields) {
            MetaField entity = new MetaField();
            entity.setEntityCode(entityCode);
            entity.setCode(field.code());
            entity.setName(field.name());
            entity.setNameEn(field.nameEn());
            entity.setValueType(field.valueType());
            entity.setDataType(field.dataType() == null || field.dataType().isBlank()
                    ? defaultDataType(field.valueType()) : field.dataType());
            entity.setLength(field.length());
            entity.setRequired(Boolean.TRUE.equals(field.required()));
            entity.setDefaultValue(field.defaultValue());
            entity.setEnumCode(blankToNull(field.enumCode()));
            entity.setRefEntity(blankToNull(field.refEntity()));
            entity.setUiJson(field.uiJson() == null || field.uiJson().isBlank() ? "{}" : field.uiJson());
            entity.setSecurityLevel(0);
            entity.setIndexed(false);
            entity.setSearchable(Boolean.TRUE.equals(field.searchable()));
            entity.setSort(field.sort() == null ? sort : field.sort());
            entity.setVersionNo(1);
            entity.setStatus("DRAFT");
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            fieldMapper.insert(entity);
            sort += 10;
        }
    }

    private void validateField(MetaDtos.FieldDef field) {
        if (!VALUE_TYPES.contains(field.valueType())) {
            throw BizException.of(ErrorCode.META_FIELD_INVALID, "不支持的值类型：" + field.valueType());
        }
        if (field.dataType() != null && !field.dataType().isBlank() && !DATA_TYPES.contains(field.dataType())) {
            throw BizException.of(ErrorCode.META_FIELD_INVALID, "不支持的物理类型：" + field.dataType());
        }
        if (field.enumCode() != null && !field.enumCode().isBlank()
                && enumMapper.selectById(field.enumCode()) == null) {
            throw BizException.of(ErrorCode.META_FIELD_INVALID, "引用的枚举不存在：" + field.enumCode());
        }
    }

    private void validateVersionPolicy(String policy) {
        if (policy != null && !policy.isBlank() && !VERSION_POLICIES.contains(policy)) {
            throw BizException.of(ErrorCode.META_FIELD_INVALID, "不支持的版本策略：" + policy);
        }
    }

    @Transactional
    public void publish(String code) {
        MetaEntity entity = requireEntity(code);
        List<MetaField> fields = fieldMapper.selectList(
                new LambdaQueryWrapper<MetaField>().eq(MetaField::getEntityCode, code));
        List<String> errors = new ArrayList<>();
        if (fields.isEmpty()) {
            errors.add("至少需要定义一个字段");
        }
        Set<String> codes = new HashSet<>();
        for (MetaField field : fields) {
            if (!codes.add(field.getCode())) {
                errors.add("字段编码重复：" + field.getCode());
            }
            if (!VALUE_TYPES.contains(field.getValueType())) {
                errors.add("字段 " + field.getCode() + " 值类型非法：" + field.getValueType());
            }
            if (field.getEnumCode() != null && enumMapper.selectById(field.getEnumCode()) == null) {
                errors.add("字段 " + field.getCode() + " 引用的枚举不存在：" + field.getEnumCode());
            }
        }
        if (!errors.isEmpty()) {
            throw BizException.of(ErrorCode.META_PUBLISH_BLOCKED, String.join("；", errors));
        }
        entity.setStatus("PUBLISHED");
        entity.setVersionNo(entity.getVersionNo() == null ? 1 : entity.getVersionNo() + 1);
        entity.setUpdatedAt(OffsetDateTime.now());
        entityMapper.updateById(entity);
        fields.forEach(field -> {
            field.setStatus("PUBLISHED");
            field.setVersionNo(entity.getVersionNo());
            field.setUpdatedAt(OffsetDateTime.now());
            fieldMapper.updateById(field);
        });
        invalidate(code);
    }

    @Transactional
    public void deleteEntity(String code) {
        MetaEntity entity = requireEntity(code);
        if ("PUBLISHED".equals(entity.getStatus())) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "已发布的类型不允许删除，请先停用");
        }
        Long children = entityMapper.selectCount(new LambdaQueryWrapper<MetaEntity>()
                .eq(MetaEntity::getParentCode, code));
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "存在子类型，无法删除");
        }
        fieldMapper.deleteByEntity(code);
        entityMapper.deleteById(entity.getId());
        invalidate(code);
    }

    // ---------- 枚举 ----------

    public List<MetaDtos.EnumDetail> listEnums() {
        return enumMapper.selectList(new LambdaQueryWrapper<MetaEnum>().orderByAsc(MetaEnum::getCode))
                .stream().map(e -> new MetaDtos.EnumDetail(e.getCode(), e.getName(), enumItems(e.getCode())))
                .toList();
    }

    public MetaDtos.EnumDetail getEnum(String code) {
        MetaEnum metaEnum = enumMapper.selectById(code);
        if (metaEnum == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "枚举不存在：" + code);
        }
        return new MetaDtos.EnumDetail(metaEnum.getCode(), metaEnum.getName(), enumItems(code));
    }

    private List<MetaDtos.EnumItemDef> enumItems(String code) {
        return jdbcTemplate.query("""
                        SELECT item_code, item_name, sort FROM meta_enum_item
                         WHERE enum_code = ? ORDER BY sort, item_code
                        """,
                (rs, rowNum) -> new MetaDtos.EnumItemDef(rs.getString(1), rs.getString(2), rs.getInt(3)),
                code);
    }

    @Transactional
    public void saveEnumItems(String code, List<MetaDtos.EnumItemDef> items) {
        MetaEnum metaEnum = enumMapper.selectById(code);
        if (metaEnum == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "枚举不存在：" + code);
        }
        Set<String> codes = new HashSet<>();
        for (MetaDtos.EnumItemDef item : items == null ? List.<MetaDtos.EnumItemDef>of() : items) {
            if (item.code() == null || item.code().isBlank()) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "枚举项编码不能为空");
            }
            if (!codes.add(item.code())) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "枚举项编码重复：" + item.code());
            }
        }
        jdbcTemplate.update("DELETE FROM meta_enum_item WHERE enum_code = ?", code);
        int index = 0;
        for (MetaDtos.EnumItemDef item : items == null ? List.<MetaDtos.EnumItemDef>of() : items) {
            jdbcTemplate.update("""
                            INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort)
                            VALUES (?, ?, ?, ?)
                            """,
                    code, item.code(), item.name() == null ? item.code() : item.name(),
                    item.sort() == null ? index * 10 : item.sort());
            index++;
        }
        invalidate(null);
    }

    // ---------- 内部 ----------

    private MetaEntity requireEntity(String code) {
        MetaEntity entity = entityMapper.selectOne(new LambdaQueryWrapper<MetaEntity>()
                .eq(MetaEntity::getCode, code)
                .last("limit 1"));
        if (entity == null) {
            throw BizException.of(ErrorCode.META_ENTITY_NOT_FOUND, "对象类型不存在：" + code);
        }
        return entity;
    }

    private MetaDtos.FieldDef toFieldDef(MetaField field) {
        return new MetaDtos.FieldDef(field.getCode(), field.getName(), field.getNameEn(), field.getValueType(),
                field.getDataType(), field.getLength(), field.getRequired(), field.getDefaultValue(),
                field.getEnumCode(), field.getRefEntity(), field.getSearchable(), field.getSort(), field.getUiJson());
    }

    private List<String> readCapabilities(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String writeCapabilities(List<String> capabilities) {
        try {
            return objectMapper.writeValueAsString(capabilities == null ? List.of() : capabilities);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String defaultDataType(String valueType) {
        return switch (valueType) {
            case "BoolValue" -> "BOOLEAN";
            case "DateTimeValue", "DateValue" -> "DATETIME";
            case "VersionValue", "MinorVersionValue", "MajorMinorVersionValue" -> "INT";
            case "DescriptionValue", "MultiUserValue" -> "TEXT";
            case "FileUploadValue", "SubDataModelValue" -> "JSON";
            default -> "VARCHAR";
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void invalidate(String code) {
        if (code == null) {
            detailCache.clear();
        } else {
            detailCache.remove(code);
        }
    }
}
