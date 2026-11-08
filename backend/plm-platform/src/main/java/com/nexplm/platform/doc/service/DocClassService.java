package com.nexplm.platform.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocClassDtos;
import com.nexplm.platform.doc.domain.DocClass;
import com.nexplm.platform.doc.domain.DocClassAttr;
import com.nexplm.platform.doc.mapper.DocClassAttrMapper;
import com.nexplm.platform.doc.mapper.DocClassMapper;
import com.nexplm.platform.metadata.service.MetaService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 分类服务：分类树维护、分类属性（含继承）定义、对象分类属性校验。
 * <p><b>本类同时服务图文档（{@code PdmDoc}）与零部件（{@code PdmItem}）</b>：
 * {@code doc_class}/{@code doc_class_attr} 两张表由本类独占写入（单一口径），
 * 不同对象类型靠 {@code entity_code} 隔离，编码可以在不同类型下重复。</p>
 * <p>规则：</p>
 * <ul>
 *   <li>分类编码在同一对象类型内唯一，形如 {@code MECH-ASSY}；</li>
 *   <li>{@code path} 为「/父编码/子编码」，调整编码或父级时整棵子树的 path 前缀同步替换；</li>
 *   <li>属性继承：子分类读取自身全部属性 + 祖先 {@code inheritable=true} 的属性，同编码以最近者为准；</li>
 *   <li>对象保存时按「生效属性」校验必填与值类型，属性值只保留已定义属性。</li>
 * </ul>
 * <p>类名沿用历史的 {@code DocClassService}（表名也叫 doc_class），实体含义已泛化为「对象分类」，
 * 迁移见 {@code docs/adr/0001}。</p>
 */
@Service
public class DocClassService {

    public static final String ENTITY_CODE = "PdmDoc";

    /** 零部件分类的对象类型（与 PdmItem 元数据实体编码一致）。 */
    public static final String ENTITY_ITEM = "PdmItem";

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");
    private static final Set<String> VALUE_TYPES = Set.of("STRING", "NUMBER", "DATE", "ENUM", "BOOL");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISABLED");
    private static final int MAX_DEPTH = 20;

    private final DocClassMapper classMapper;
    private final DocClassAttrMapper attrMapper;
    private final MetaService metaService;
    private final JdbcTemplate jdbcTemplate;

    public DocClassService(DocClassMapper classMapper, DocClassAttrMapper attrMapper,
                           MetaService metaService, JdbcTemplate jdbcTemplate) {
        this.classMapper = classMapper;
        this.attrMapper = attrMapper;
        this.metaService = metaService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------ 分类树

    public List<DocClassDtos.ClassNode> tree(String entityCode) {
        String entity = normalizeEntity(entityCode);
        List<DocClass> all = classMapper.selectList(new LambdaQueryWrapper<DocClass>()
                .eq(DocClass::getEntityCode, entity)
                .orderByAsc(DocClass::getSort)
                .orderByAsc(DocClass::getCode));
        Map<Long, Long> counts = objectCounts(entity);
        Map<Long, DocClassDtos.ClassNode> nodes = new LinkedHashMap<>();
        for (DocClass item : all) {
            nodes.put(item.getId(), new DocClassDtos.ClassNode(item.getId(), item.getParentId(),
                    item.getCode(), item.getName(), item.getPath(), item.getSort(), item.getStatus(),
                    item.getRemark(), item.getCodeRuleCode(), item.getCodePath(),
                    counts.getOrDefault(item.getId(), 0L), new ArrayList<>()));
        }
        List<DocClassDtos.ClassNode> roots = new ArrayList<>();
        for (DocClass item : all) {
            DocClassDtos.ClassNode node = nodes.get(item.getId());
            DocClassDtos.ClassNode parent = item.getParentId() == null ? null : nodes.get(item.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public DocClass requireClass(Long id) {
        if (id == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "分类标识不能为空");
        }
        DocClass clazz = classMapper.selectById(id);
        if (clazz == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "分类不存在：" + id);
        }
        return clazz;
    }

    /** 按编码查分类（批量导入等按编码引用的场景），默认文档类型。 */
    public java.util.Optional<DocClass> findByCode(String code) {
        return findByCode(ENTITY_CODE, code);
    }

    /** 按「对象类型 + 编码」查分类。 */
    public java.util.Optional<DocClass> findByCode(String entityCode, String code) {
        if (code == null || code.isBlank()) {
            return java.util.Optional.empty();
        }
        return classMapper.selectList(new LambdaQueryWrapper<DocClass>()
                        .eq(DocClass::getEntityCode, normalizeEntity(entityCode))
                        .eq(DocClass::getCode, code.trim())
                        .last("limit 1"))
                .stream().findFirst();
    }

    /** 取分类并校验其属于指定对象类型（防止把文档分类挂到零件上）。 */
    public DocClass requireClass(Long id, String entityCode) {
        DocClass clazz = requireClass(id);
        String expected = normalizeEntity(entityCode);
        if (!expected.equals(clazz.getEntityCode())) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "分类「" + clazz.getName() + "」不属于对象类型 " + expected + "，不能使用");
        }
        return clazz;
    }

    /**
     * 按「对象类型 + 关联编码规则」找分类（自动归类建议用）。
     * <p>一条规则在同一对象类型下至多关联一个分类，由 {@link #normalizeCodeRuleCode} 保证。</p>
     */
    public java.util.Optional<DocClass> findByCodeRule(String entityCode, String ruleCode) {
        if (ruleCode == null || ruleCode.isBlank()) {
            return java.util.Optional.empty();
        }
        return classMapper.selectList(new LambdaQueryWrapper<DocClass>()
                        .eq(DocClass::getEntityCode, normalizeEntity(entityCode))
                        .eq(DocClass::getCodeRuleCode, ruleCode.trim())
                        .last("limit 1"))
                .stream().findFirst();
    }

    /** 子树分类 ID（含自身），用于「按分类检索文档」时包含子分类。 */
    public List<Long> subtreeIds(Long classId) {
        DocClass root = requireClass(classId);
        return jdbcTemplate.queryForList("""
                        SELECT id FROM doc_class
                         WHERE entity_code = ? AND deleted = false
                           AND (id = ? OR path LIKE ? ESCAPE '\\')
                        """, Long.class, root.getEntityCode(), classId, escapeLike(root.getPath()) + "/%");
    }

    @Transactional
    public DocClassDtos.ClassNode create(DocClassDtos.ClassSaveReq req, Long userId) {
        return create(ENTITY_CODE, req, userId);
    }

    /** 在指定对象类型下新建分类（父级必须同属该类型）。 */
    @Transactional
    public DocClassDtos.ClassNode create(String entityCode, DocClassDtos.ClassSaveReq req, Long userId) {
        String entity = normalizeEntity(entityCode);
        String code = requireCode(req.code());
        DocClass parent = req.parentId() == null ? null : requireClass(req.parentId(), entity);
        ensureCodeUnique(entity, code, null);
        OffsetDateTime now = OffsetDateTime.now();
        DocClass clazz = new DocClass();
        clazz.setId(IdWorker.getId());
        clazz.setParentId(parent == null ? null : parent.getId());
        clazz.setEntityCode(entity);
        clazz.setCode(code);
        clazz.setName(req.name().trim());
        clazz.setPath(parent == null ? "/" + code : parent.getPath() + "/" + code);
        clazz.setSort(req.sort() == null ? 0 : req.sort());
        clazz.setStatus(normalizeStatus(req.status()));
        clazz.setRemark(req.remark());
        clazz.setCodeRuleCode(normalizeCodeRuleCode(entity, req.codeRuleCode(), null));
        clazz.setVersion(0);
        clazz.setDeleted(false);
        clazz.setCreatedBy(userId);
        clazz.setCreatedAt(now);
        clazz.setUpdatedBy(userId);
        clazz.setUpdatedAt(now);
        classMapper.insert(clazz);
        return toNode(clazz, 0L, List.of());
    }

    /**
     * 修改分类：支持改名/排序/停用/改编码/换父级，编码或父级变化时整棵子树 path 前缀同步替换。
     */
    @Transactional
    public DocClassDtos.ClassNode update(Long id, DocClassDtos.ClassSaveReq req, Long userId) {
        DocClass existing = requireClass(id);
        String code = req.code() == null || req.code().isBlank() ? existing.getCode() : requireCode(req.code());
        ensureCodeUnique(existing.getEntityCode(), code, id);

        Long parentId = req.parentId();
        if (parentId != null && parentId.equals(id)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "分类不能作为自己的父级");
        }
        // 父级必须同属一个对象类型，避免把零部件分类挂到文档分类下（反之亦然）
        DocClass parent = parentId == null ? null : requireClass(parentId, existing.getEntityCode());
        if (parent != null && parent.getPath().startsWith(existing.getPath() + "/")) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不能把分类移动到自己的子分类下");
        }

        String oldPath = existing.getPath();
        String newPath = parent == null ? "/" + code : parent.getPath() + "/" + code;
        boolean pathChanged = !newPath.equals(oldPath);

        existing.setParentId(parent == null ? null : parent.getId());
        existing.setCode(code);
        existing.setName(req.name().trim());
        existing.setPath(newPath);
        existing.setSort(req.sort() == null ? existing.getSort() : req.sort());
        existing.setStatus(normalizeStatus(req.status() == null ? existing.getStatus() : req.status()));
        existing.setRemark(req.remark());
        // null=不改（前端编辑回传当前值时可省略），空串=解除关联
        existing.setCodeRuleCode(req.codeRuleCode() == null
                ? existing.getCodeRuleCode()
                : normalizeCodeRuleCode(existing.getEntityCode(), req.codeRuleCode(), id));
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(OffsetDateTime.now());
        classMapper.updateById(existing);

        if (pathChanged) {
            jdbcTemplate.update("""
                            UPDATE doc_class
                               SET path = ? || substring(path from ?), updated_by = ?, updated_at = now()
                             WHERE entity_code = ? AND id <> ? AND deleted = false
                               AND path LIKE ? ESCAPE '\\'
                            """,
                    newPath, oldPath.length() + 1, userId, existing.getEntityCode(), id,
                    escapeLike(oldPath) + "/%");
        }
        return toNode(existing, 0L, List.of());
    }

    @Transactional
    public void delete(Long id) {
        DocClass existing = requireClass(id);
        Long children = classMapper.selectCount(new LambdaQueryWrapper<DocClass>()
                .eq(DocClass::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该分类下仍有子分类，请先删除子分类");
        }
        // 分类下仍有对象时拒绝删除（表名来自白名单常量，不拼接外部输入）
        boolean itemClass = ENTITY_ITEM.equals(existing.getEntityCode());
        String table = itemClass ? "item_master" : "doc_master";
        String noun = itemClass ? "零部件" : "文档";
        Long used = jdbcTemplate.query("SELECT COUNT(1) FROM " + table + " WHERE class_id = ?",
                rs -> rs.next() ? rs.getLong(1) : 0L, id);
        if (used != null && used > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "该分类下仍有 " + used + " 个" + noun + "，不能删除");
        }
        for (DocClassAttr attr : attrMapper.selectList(new LambdaQueryWrapper<DocClassAttr>()
                .eq(DocClassAttr::getClassId, existing.getId()))) {
            attrMapper.deleteById(attr.getId());
        }
        classMapper.deleteById(id);
    }

    // ------------------------------------------------------------ 分类属性

    /** 生效属性：自身全部属性在前，祖先可继承属性在后，同编码以最近者为准。 */
    public List<DocClassDtos.AttrDef> attrs(Long classId) {
        DocClass current = requireClass(classId);
        Map<String, DocClassDtos.AttrDef> result = new LinkedHashMap<>();
        Set<Long> visited = new HashSet<>();
        DocClass cursor = current;
        int depth = 0;
        while (cursor != null && visited.add(cursor.getId()) && depth++ < MAX_DEPTH) {
            boolean inherited = !cursor.getId().equals(classId);
            for (DocClassAttr attr : attrMapper.selectList(new LambdaQueryWrapper<DocClassAttr>()
                    .eq(DocClassAttr::getClassId, cursor.getId())
                    .orderByAsc(DocClassAttr::getSort))) {
                if (inherited && !Boolean.TRUE.equals(attr.getInheritable())) {
                    continue;
                }
                result.putIfAbsent(attr.getCode(), new DocClassDtos.AttrDef(attr.getId(), attr.getCode(),
                        attr.getName(), attr.getValueType(), attr.getEnumCode(),
                        attr.getRequired(), attr.getInheritable(), attr.getDefaultValue(),
                        attr.getSort(), cursor.getId(), cursor.getName(), inherited));
            }
            cursor = cursor.getParentId() == null ? null : classMapper.selectById(cursor.getParentId());
        }
        return new ArrayList<>(result.values());
    }

    /** 全量替换分类自身属性（未出现在入参中的属性做逻辑删除）。 */
    @Transactional
    public List<DocClassDtos.AttrDef> saveAttrs(Long classId, List<DocClassDtos.AttrItem> items, Long userId) {
        requireClass(classId);
        Map<String, DocClassAttr> existing = new HashMap<>();
        for (DocClassAttr attr : attrMapper.selectList(new LambdaQueryWrapper<DocClassAttr>()
                .eq(DocClassAttr::getClassId, classId))) {
            existing.put(attr.getCode(), attr);
        }
        Set<String> keep = new HashSet<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (DocClassDtos.AttrItem item : items) {
            String type = requireValueType(item.valueType());
            String enumCode = item.enumCode() == null || item.enumCode().isBlank() ? null : item.enumCode().trim();
            if ("ENUM".equals(type)) {
                if (enumCode == null) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "属性「" + item.name() + "」为枚举类型，必须指定枚举编码");
                }
                metaService.getEnum(enumCode);
            } else {
                enumCode = null;
            }
            keep.add(item.code());
            DocClassAttr attr = existing.get(item.code());
            boolean insert = attr == null;
            if (insert) {
                attr = new DocClassAttr();
                attr.setId(IdWorker.getId());
                attr.setClassId(classId);
                attr.setCode(item.code());
                attr.setDeleted(false);
                attr.setCreatedBy(userId);
                attr.setCreatedAt(now);
            }
            attr.setName(item.name().trim());
            attr.setValueType(type);
            attr.setEnumCode(enumCode);
            attr.setRequired(Boolean.TRUE.equals(item.required()));
            attr.setInheritable(item.inheritable() == null || item.inheritable());
            attr.setDefaultValue(item.defaultValue());
            attr.setSort(item.sort() == null ? 0 : item.sort());
            attr.setUpdatedBy(userId);
            attr.setUpdatedAt(now);
            if (insert) {
                attrMapper.insert(attr);
            } else {
                attrMapper.updateById(attr);
            }
        }
        for (DocClassAttr old : existing.values()) {
            if (!keep.contains(old.getCode())) {
                attrMapper.deleteById(old.getId());
            }
        }
        return attrs(classId);
    }

    /**
     * 校验并归一化文档的分类属性值（保留旧方法名，供文档模块调用）。
     */
    public Map<String, String> normalizeDocAttrs(Long classId, Map<String, String> values) {
        return normalizeAttrs(classId, values);
    }

    /**
     * 校验并归一化对象的分类属性值：按生效属性做必填与值类型校验，未定义属性丢弃。
     */
    public Map<String, String> normalizeAttrs(Long classId, Map<String, String> values) {
        if (classId == null) {
            return Map.of();
        }
        List<DocClassDtos.AttrDef> defs = attrs(classId);
        Map<String, String> input = values == null ? Map.of() : values;
        Map<String, String> normalized = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (DocClassDtos.AttrDef def : defs) {
            String raw = input.get(def.code());
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                if (Boolean.TRUE.equals(def.required())) {
                    missing.add(def.name());
                }
                continue;
            }
            checkValue(def, value);
            normalized.put(def.code(), value);
        }
        if (!missing.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "分类必填属性未填写：" + String.join("、", missing));
        }
        return normalized;
    }

    // ------------------------------------------------------------ 内部

    private void checkValue(DocClassDtos.AttrDef def, String value) {
        String type = def.valueType() == null ? "STRING" : def.valueType();
        switch (type) {
            case "NUMBER" -> {
                try {
                    new BigDecimal(value);
                } catch (NumberFormatException ex) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "属性「" + def.name() + "」必须是数值：" + value);
                }
            }
            case "DATE" -> {
                try {
                    LocalDate.parse(value);
                } catch (Exception ex) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "属性「" + def.name() + "」必须是日期（yyyy-MM-dd）：" + value);
                }
            }
            case "BOOL" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "属性「" + def.name() + "」必须是 true/false：" + value);
                }
            }
            case "ENUM" -> {
                if (def.enumCode() == null) {
                    return;
                }
                boolean matched = metaService.getEnum(def.enumCode()).items().stream()
                        .anyMatch(item -> item.code().equals(value));
                if (!matched) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "属性「" + def.name() + "」取值不在枚举范围内：" + value);
                }
            }
            default -> {
                if (value.length() > 500) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                            "属性「" + def.name() + "」长度不能超过 500");
                }
            }
        }
    }

    /** 各分类下的对象数：文档看 doc_master，零部件看 item_master。 */
    private Map<Long, Long> objectCounts(String entityCode) {
        Map<Long, Long> counts = new HashMap<>();
        String table = ENTITY_ITEM.equals(entityCode) ? "item_master" : "doc_master";
        jdbcTemplate.query("SELECT class_id, COUNT(1) AS doc_count FROM " + table
                        + " WHERE class_id IS NOT NULL GROUP BY class_id",
                rs -> {
                    counts.put(rs.getLong("class_id"), rs.getLong("doc_count"));
                });
        return counts;
    }

    private void ensureCodeUnique(String entityCode, String code, Long excludeId) {
        LambdaQueryWrapper<DocClass> wrapper = new LambdaQueryWrapper<DocClass>()
                .eq(DocClass::getEntityCode, entityCode)
                .eq(DocClass::getCode, code);
        if (excludeId != null) {
            wrapper.ne(DocClass::getId, excludeId);
        }
        Long count = classMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "分类编码已存在：" + code);
        }
    }

    private String requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "分类编码不能为空");
        }
        String trimmed = code.trim();
        if (!CODE_PATTERN.matcher(trimmed).matches()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "分类编码需以字母开头，仅含字母数字下划线或短横线：" + trimmed);
        }
        return trimmed;
    }

    private String requireValueType(String valueType) {
        String type = valueType == null ? "" : valueType.trim().toUpperCase();
        if (!VALUE_TYPES.contains(type)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "不支持的值类型：" + valueType + "，可选：" + String.join("/", VALUE_TYPES));
        }
        return type;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "状态仅支持 ACTIVE / DISABLED");
        }
        return normalized;
    }

    private String normalizeEntity(String entityCode) {
        return entityCode == null || entityCode.isBlank() ? ENTITY_CODE : entityCode.trim();
    }

    /**
     * 校验并归一「关联编码」。
     * <p>空串 = 解除关联；非空 = 规则必须存在、启用、且对象类型一致；
     * 同一对象类型下一条规则只能挂一个分类，否则自动归类会产生歧义。</p>
     *
     * @param excludeClassId 更新时排除自身
     */
    private String normalizeCodeRuleCode(String entityCode, String raw, Long excludeClassId) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String ruleCode = raw.trim();
        List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT object_type, status FROM code_rule WHERE code = ?", ruleCode);
        if (rules.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "关联的编码规则不存在：" + ruleCode);
        }
        Map<String, Object> rule = rules.get(0);
        String objectType = rule.get("object_type") == null ? null : String.valueOf(rule.get("object_type"));
        if (objectType != null && !objectType.isBlank() && !objectType.equals(entityCode)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "编码规则「" + ruleCode + "」属于对象类型 " + objectType + "，不能关联到 " + entityCode + " 分类");
        }
        if (!"ACTIVE".equals(String.valueOf(rule.get("status")))) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "编码规则「" + ruleCode + "」已停用，不能关联");
        }
        Long conflict = excludeClassId == null
                ? jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM doc_class
                         WHERE entity_code = ? AND code_rule_code = ? AND deleted = false
                        """, Long.class, entityCode, ruleCode)
                : jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM doc_class
                         WHERE entity_code = ? AND code_rule_code = ? AND deleted = false AND id <> ?
                        """, Long.class, entityCode, ruleCode, excludeClassId);
        if (conflict != null && conflict > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT,
                    "编码规则「" + ruleCode + "」已关联到其它分类，同一规则只能对应一个分类");
        }
        return ruleCode;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private DocClassDtos.ClassNode toNode(DocClass clazz, Long docCount, List<DocClassDtos.ClassNode> children) {
        return new DocClassDtos.ClassNode(clazz.getId(), clazz.getParentId(), clazz.getCode(), clazz.getName(),
                clazz.getPath(), clazz.getSort(), clazz.getStatus(), clazz.getRemark(),
                clazz.getCodeRuleCode(), clazz.getCodePath(), docCount, children);
    }
}
