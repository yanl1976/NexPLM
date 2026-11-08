package com.nexplm.platform.coding.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 编码规则引擎，对齐旧系统 {@code InforCenter/Coding}（{@code Coding.coderule} 的段模型）。
 *
 * <p>编码 = 若干段顺序拼接。段类型收敛为三类（旧系统 5 类）：</p>
 * <ul>
 *   <li>{@code TEXT}：固定字面量（旧 Char/NumberOrChar 的常量用法）</li>
 *   <li>{@code DATE}：{@code yyyy} / {@code yyyyMM} / {@code yyyyMMdd}</li>
 *   <li>{@code SEQ}：流水码，{@code width} 位 + {@code radix}
 *       （{@code DECIMAL}=0-9、{@code ALNUM}=0-9A-Z，对应旧 OrderCode 的两种值域）</li>
 * </ul>
 *
 * <p><b>复用 V1 基线的 {@code code_rule} 表</b>（列：{@code segments_json} / {@code reset_policy} /
 * {@code status}），只 ALTER 补了 {@code current_no} / {@code last_period} / {@code object_type} /
 * {@code remark}；计数器放在规则行上，因为"按年/月/日归零"需要期间信息，而 {@code code_sequence}
 * 只有单调计数。</p>
 */
@Service
public class CodeRuleService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> DATE_FORMATS = Set.of("yyyy", "yyyyMM", "yyyyMMdd");
    private static final Set<String> RADIX = Set.of("DECIMAL", "ALNUM");
    private static final Set<String> RESET_POLICIES = Set.of("NEVER", "YEAR", "MONTH", "DAY");
    private static final String ALNUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String SELECT_COLUMNS = """
            SELECT id, code, name, object_type, segments_json, reset_policy, status,
                   current_no, last_period, remark, updated_at
              FROM code_rule
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CodeRuleService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------ 查询

    public List<CodeRule> list() {
        return jdbcTemplate.query(SELECT_COLUMNS + " ORDER BY code", (rs, i) -> map(
                rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("object_type"), rs.getString("segments_json"),
                rs.getString("reset_policy"), rs.getString("status"), rs.getLong("current_no"),
                rs.getString("last_period"), rs.getString("remark"), timestamp(rs.getTimestamp("updated_at"))));
    }

    public CodeRule get(String code) {
        List<CodeRule> rows = jdbcTemplate.query(SELECT_COLUMNS + " WHERE code = ?", (rs, i) -> map(
                rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("object_type"), rs.getString("segments_json"),
                rs.getString("reset_policy"), rs.getString("status"), rs.getLong("current_no"),
                rs.getString("last_period"), rs.getString("remark"), timestamp(rs.getTimestamp("updated_at"))),
                code);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "编码规则不存在：" + code);
        }
        return rows.get(0);
    }

    /**
     * 按编码反查产出它的规则（旧系统 {@code GetPathByCode} 的替代实现）。
     *
     * <p>规则段只有 TEXT/DATE/SEQ，其中 DATE 与 SEQ 无法从编码反推，因此用<b>字面量前缀</b>
     * （规则开头连续的 TEXT 段拼接）做最长匹配：</p>
     * <ul>
     *   <li>命中多条且最长前缀等长 → 歧义，返回空（不猜，避免归错分类）；</li>
     *   <li>前缀为空（规则以 DATE/SEQ 开头）→ 跳过，否则会匹配所有编码。</li>
     * </ul>
     *
     * @param objectType 对象类型（如 {@code PdmItem}）；空表示不限
     */
    /** 取号预览结果（不消耗流水，供新建表单实时显示"将生成的编码"）。 */
    public record Preview(String code, String ruleCode, String ruleName, String source) {
    }

    /**
     * 预览某条规则的下一个编码（不写流水、不动计数器）。
     *
     * @param classCode 规则含 {@code CLASS} 段时必填
     */
    public Preview preview(String ruleCode, String classCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT name, segments_json, current_no, last_period, reset_policy, status
                  FROM code_rule WHERE code = ?
                """, ruleCode);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "编码规则不存在：" + ruleCode);
        }
        Map<String, Object> row = rows.get(0);
        if (!"ACTIVE".equals(String.valueOf(row.get("status")))) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码规则已停用：" + ruleCode);
        }
        List<Segment> segments = parse(String.valueOf(row.get("segments_json")));
        boolean byClass = segments.stream().anyMatch(s -> "CLASS".equalsIgnoreCase(s.type()));
        long nextNo;
        if (byClass) {
            nextNo = peekClassSeq(ruleCode, classCode);
        } else {
            String policy = normalizePolicy(String.valueOf(row.get("reset_policy")));
            String period = period(policy);
            String lastPeriod = row.get("last_period") == null ? null : String.valueOf(row.get("last_period"));
            long current = ((Number) row.get("current_no")).longValue();
            boolean newPeriod = period != null && lastPeriod != null && !period.equals(lastPeriod);
            nextNo = newPeriod ? 1 : current + 1;
        }
        return new Preview(render(segments, nextNo, classCode), ruleCode,
                row.get("name") == null ? ruleCode : String.valueOf(row.get("name")), "CLASS_CODE");
    }

    /** 预览按分类编码路径取号的下一个编码（不动 {@code code_sequence}）。 */
    public Preview previewByPath(String fullPath, String scopeKey) {
        if (fullPath == null || fullPath.isBlank()) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "分类未绑定编码路径");
        }
        String[] parts = fullPath.split("/");
        String ruleCode = parts[0].trim();
        List<String> values = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty() || "*".equals(part)) {
                continue;
            }
            values.add(part);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT name, segments_json, status FROM code_rule WHERE code = ?
                """, ruleCode);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "编码规则不存在：" + ruleCode);
        }
        if (!"ACTIVE".equals(String.valueOf(rows.get(0).get("status")))) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码规则已停用：" + ruleCode);
        }
        List<Segment> segments = parse(String.valueOf(rows.get(0).get("segments_json")));
        long nextNo = peekClassSeq(ruleCode, scopeKey);
        return new Preview(renderByPath(segments, values, nextNo), ruleCode,
                rows.get(0).get("name") == null ? ruleCode : String.valueOf(rows.get(0).get("name")),
                "CLASS_PATH");
    }

    /** 按细分对象类型预览（命中专属规则用它，否则回退）。 */
    public Preview previewForObjectType(String objectType, String fallbackRuleCode) {
        if (objectType != null && !objectType.isBlank()) {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT code FROM code_rule WHERE object_type = ? AND status = 'ACTIVE' ORDER BY code LIMIT 1",
                    String.class, objectType.trim());
            if (!codes.isEmpty()) {
                Preview hit = preview(codes.get(0), null);
                return new Preview(hit.code(), hit.ruleCode(), hit.ruleName(), "TYPE_RULE");
            }
        }
        Preview fallback = preview(fallbackRuleCode, null);
        return new Preview(fallback.code(), fallback.ruleCode(), fallback.ruleName(), "TYPE_RULE");
    }

    /** 只读下一个分类流水（不占用）；已迁移的历史编码也算进去。 */
    private long peekClassSeq(String ruleCode, String scopeKey) {
        String key = scopeKey == null || scopeKey.isBlank() ? "DEFAULT" : scopeKey.trim();
        long used = maxUsedNo(key);
        List<Long> rows = jdbcTemplate.queryForList(
                "SELECT current_no FROM code_sequence WHERE rule_code = ? AND scope_key = ?",
                Long.class, ruleCode, key);
        long current = rows.isEmpty() || rows.get(0) == null ? 0 : rows.get(0);
        return Math.max(current, used) + 1;
    }

    /**
     * 该分类前缀下**实际已存在**的最大流水号。
     *
     * <p>迁移进来的零件编码是蓝本原值（如 {@code 10010100001}），但
     * {@code code_sequence} 是空的；若只按计数器从 1 起，就会撞上历史编码
     * （`uk_plm_object_code`）。取号与预览都必须以两者较大者为基准。</p>
     */
    private long maxUsedNo(String scopeKey) {
        if (scopeKey == null || scopeKey.isBlank() || "DEFAULT".equals(scopeKey)) {
            return 0;
        }
        Long used = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(RIGHT(o.code, 5) AS BIGINT)), 0)
                  FROM plm_object o
                 WHERE o.object_type = 'PdmItem' AND o.deleted = false
                   AND o.code ~ '^[0-9]{11}$'
                   AND LEFT(o.code, LENGTH(?)) = ?
                """, Long.class, scopeKey, scopeKey);
        return used == null ? 0 : used;
    }

    /** 编码种类行（对应蓝本 {@code CODEKIND}）。 */
    public record KindRow(String code, String name, String ruleCode, String legacyId,
                          long valueCount, long classCount) {
    }

    /**
     * 码值行（对应蓝本 {@code CODEVALUE}）；{@code valuePath} 形如 {@code 1001} 或 {@code 1001/02}。
     * <p>{@code deleted=true} 表示已停用：不再允许新绑定，但历史编码仍引用它。</p>
     */
    public record ValueRow(String valuePath, String codeValue, String name, String segment, int sort,
                           boolean deleted, String majorName) {
    }

    /** 编码种类列表（含码值数与已绑定分类数）。 */
    public List<KindRow> listKinds() {
        return jdbcTemplate.query("""
                SELECT k.code, k.name, k.rule_code, k.legacy_id,
                       (SELECT COUNT(1) FROM code_segment_value v
                         WHERE v.kind_code = k.code AND v.deleted = false) AS value_count,
                       (SELECT COUNT(1) FROM doc_class c
                         WHERE c.entity_code = 'PdmItem' AND c.deleted = false
                           AND c.code_path LIKE k.code || '/%') AS class_count
                  FROM code_kind k
                 ORDER BY k.sort, k.code
                """,
                (rs, rowNum) -> new KindRow(rs.getString("code"), rs.getString("name"),
                        rs.getString("rule_code"), rs.getString("legacy_id"),
                        rs.getLong("value_count"), rs.getLong("class_count")));
    }

    /**
     * 新增码值（种类下）。{@code parentPath} 为空表示大类，否则挂在该父码值下。
     *
     * <p>码值只增不改值（改值等于换语义），重复的 {@code valuePath} 直接拒绝。</p>
     */
    @Transactional
    public ValueRow createValue(String kindCode, String codeValue, String name, String parentPath) {
        String kind = requireKind(kindCode);
        String value = normalizeValueToken(codeValue);
        String parent = parentPath == null ? "" : parentPath.trim();
        if (!parent.isEmpty()) {
            Long exists = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM code_segment_value
                     WHERE kind_code = ? AND value_path = ? AND deleted = false
                    """, Long.class, kind, parent);
            if (exists == null || exists == 0) {
                throw BizException.of(ErrorCode.CODE_RULE_INVALID, "父码值不存在：" + parent);
            }
        }
        String path = parent.isEmpty() ? value : parent + "/" + value;
        Long dup = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM code_segment_value WHERE kind_code = ? AND value_path = ?
                """, Long.class, kind, path);
        if (dup != null && dup > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT, "码值已存在：" + path);
        }
        Integer maxSort = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(sort), 0) FROM code_segment_value WHERE kind_code = ?
                """, Integer.class, kind);
        int sort = (maxSort == null ? 0 : maxSort) + 1;
        String segment = parent.isEmpty() ? "大类" : "小类";
        jdbcTemplate.update("""
                INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), kind, segment, path, value,
                name == null || name.isBlank() ? null : name.trim(), sort);
        return new ValueRow(path, value, name, segment, sort, false, null);
    }

    /**
     * 改码值：**只允许改名与停用/启用，不允许改 {@code code_value}**（改值等于换语义）。
     * 需要换值的场景是"停用旧值 + 新增新值"，历史编码引用旧值仍然成立。
     */
    @Transactional
    public void updateValue(String kindCode, String valuePath, String name, Boolean deleted) {
        String kind = requireKind(kindCode);
        Long exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM code_segment_value
                 WHERE kind_code = ? AND value_path = ?
                """, Long.class, kind, valuePath == null ? "" : valuePath.trim());
        if (exists == null || exists == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "码值不存在：" + valuePath);
        }
        jdbcTemplate.update("""
                UPDATE code_segment_value
                   SET name = COALESCE(?, name), deleted = COALESCE(?, deleted), updated_at = now()
                 WHERE kind_code = ? AND value_path = ?
                """, name == null || name.isBlank() ? null : name.trim(), deleted,
                kind, valuePath.trim());
    }

    private String requireKind(String kindCode) {
        if (kindCode == null || kindCode.isBlank()) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码种类不能为空");
        }
        Long exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM code_kind WHERE code = ?", Long.class, kindCode.trim());
        if (exists == null || exists == 0) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "编码种类不存在：" + kindCode);
        }
        return kindCode.trim();
    }

    private static String normalizeValueToken(String codeValue) {
        String value = codeValue == null ? "" : codeValue.trim();
        if (!value.matches("[A-Za-z0-9._-]{1,32}")) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID,
                    "码值只能是字母/数字/._-，长度 1~32");
        }
        return value;
    }

    /** 码段行（对应蓝本 {@code CODERULE}）：段名/段序/段长/父段。 */
    public record SegmentRow(String code, String name, int orderNo, Integer length,
                             String parentCode, String dropMode, String status) {
    }

    /** 某个编码种类的码段（按段序升序，{@code parentCode} 表达码段树）。 */
    public List<SegmentRow> listSegments(String kindCode) {
        if (kindCode == null || kindCode.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT s.code, s.name, s.order_no, s.length, s.parent_code, s.drop_mode, s.status
                  FROM code_segment s
                  JOIN code_kind k ON k.rule_code = s.rule_code
                 WHERE k.code = ?
                 ORDER BY s.order_no, s.code
                """,
                (rs, rowNum) -> new SegmentRow(rs.getString("code"), rs.getString("name"),
                        rs.getInt("order_no"), (Integer) rs.getObject("length"),
                        rs.getString("parent_code"), rs.getString("drop_mode"),
                        rs.getString("status")),
                kindCode.trim());
    }

    /** 某个种类下的全部码值（扁平，前端按 {@code valuePath} 拼成树）。 */
    public List<ValueRow> listValues(String kindCode) {
        if (kindCode == null || kindCode.isBlank()) {
            return List.of();
        }
        // 停用的码值也要返回（前端才能"启用"）；名称缺失时用分类树里的同名节点兜底：
        // `1001` 取 doc_class(1001)、`1001/01` 取 doc_class(100101)、大类名取 doc_class(10)
        return jdbcTemplate.query("""
                SELECT v.value_path, v.code_value,
                       COALESCE(NULLIF(v.name, ''), dc.name) AS name,
                       v.segment, v.sort, v.deleted,
                       major.name AS major_name
                  FROM code_segment_value v
                  LEFT JOIN doc_class dc
                         ON dc.entity_code = 'PdmItem' AND dc.deleted = false
                        AND dc.code = replace(v.value_path, '/', '')
                  LEFT JOIN doc_class major
                         ON major.entity_code = 'PdmItem' AND major.deleted = false
                        AND major.code = left(v.value_path, 2)
                 WHERE v.kind_code = ?
                 ORDER BY v.sort, v.code_value
                """,
                (rs, rowNum) -> new ValueRow(rs.getString("value_path"), rs.getString("code_value"),
                        rs.getString("name"), rs.getString("segment"), rs.getInt("sort"),
                        rs.getBoolean("deleted"), rs.getString("major_name")),
                kindCode.trim());
    }

    /** 发放流水行（{@code code_issue} + 操作人姓名）。 */
    public record IssueRow(Long id, String ruleCode, String code, String objectType,
                           Long issuedBy, String issuedByName, OffsetDateTime issuedAt) {
    }

    /**
     * 分页查发放流水（T-CORE-02）：按规则、细分类型、编码模糊过滤，按发放时间倒序。
     */
    public PageResult<IssueRow> pageIssues(String ruleCode, String objectType,
                                           String code, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (ruleCode != null && !ruleCode.isBlank()) {
            where.append(" AND i.rule_code = ?");
            args.add(ruleCode.trim());
        }
        if (objectType != null && !objectType.isBlank()) {
            where.append(" AND i.object_type = ?");
            args.add(objectType.trim());
        }
        if (code != null && !code.isBlank()) {
            where.append(" AND i.code ILIKE ?");
            args.add("%" + code.trim() + "%");
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM code_issue i" + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<IssueRow> items = jdbcTemplate.query("""
                        SELECT i.id, i.rule_code, i.code, i.object_type, i.issued_by,
                               u.display_name AS issued_by_name, i.issued_at
                          FROM code_issue i
                          LEFT JOIN sys_user u ON u.id = i.issued_by
                        """ + where + " ORDER BY i.issued_at DESC, i.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new IssueRow(rs.getLong("id"), rs.getString("rule_code"),
                        rs.getString("code"), rs.getString("object_type"),
                        (Long) rs.getObject("issued_by"), rs.getString("issued_by_name"),
                        rs.getObject("issued_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return com.nexplm.common.api.PageResult.of(items, total == null ? 0L : total, safePage, safeSize);
    }

    /**
     * 按「细分对象类型」选规则取号（台账 T-CORE-01）。
     *
     * <p>命中 {@code code_rule.object_type = objectType} 的启用规则就用它，否则回退
     * {@code fallbackRuleCode}。调用方传 {@code PdmItem:PART} 这类细分类型，
     * 规则由编码管理页维护——11 类物料可以各配一套，不必改代码。</p>
     *
     * <p>多条命中时取 {@code code} 最小的那条，保证同一环境行为稳定（不随机）。</p>
     */
    public String nextForObjectType(String objectType, String fallbackRuleCode) {
        if (objectType != null && !objectType.isBlank()) {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT code FROM code_rule WHERE object_type = ? AND status = 'ACTIVE' ORDER BY code LIMIT 1",
                    String.class, objectType.trim());
            if (!codes.isEmpty()) {
                return next(codes.get(0), objectType);
            }
        }
        return next(fallbackRuleCode, objectType);
    }

    public java.util.Optional<String> matchRuleCode(String objectType, String code) {
        if (code == null || code.isBlank()) {
            return java.util.Optional.empty();
        }
        String target = code.trim();
        boolean filterType = objectType != null && !objectType.isBlank();
        String sql = SELECT_COLUMNS + " WHERE status = 'ACTIVE'"
                + (filterType ? " AND object_type = ?" : "") + " ORDER BY code";
        Object[] args = filterType ? new Object[]{objectType} : new Object[]{};
        List<CodeRule> rules = jdbcTemplate.query(sql, (rs, i) -> map(
                rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("object_type"), rs.getString("segments_json"),
                rs.getString("reset_policy"), rs.getString("status"), rs.getLong("current_no"),
                rs.getString("last_period"), rs.getString("remark"), timestamp(rs.getTimestamp("updated_at"))),
                args);

        String best = null;
        int bestLength = 0;
        boolean ambiguous = false;
        for (CodeRule rule : rules) {
            String prefix = literalPrefix(rule.segments());
            if (prefix.isEmpty() || !target.startsWith(prefix)) {
                continue;
            }
            if (prefix.length() > bestLength) {
                bestLength = prefix.length();
                best = rule.code();
                ambiguous = false;
            } else if (prefix.length() == bestLength) {
                ambiguous = true;
            }
        }
        return ambiguous ? java.util.Optional.empty() : java.util.Optional.ofNullable(best);
    }

    /** 规则开头连续的 TEXT 段拼成的字面量前缀（遇到 DATE/SEQ 即停止）。 */
    private static String literalPrefix(List<Segment> segments) {
        StringBuilder prefix = new StringBuilder();
        for (Segment segment : segments) {
            if (!"TEXT".equalsIgnoreCase(segment.type())) {
                break;
            }
            if (segment.value() != null) {
                prefix.append(segment.value());
            }
        }
        return prefix.toString();
    }

    // ------------------------------------------------------------ 维护

    /** 修改规则：段定义 / 归零策略 / 名称 / 启停，改动一律留审计。 */
    @Transactional
    public CodeRule update(String code, String name, List<Segment> segments, String resetPolicy,
                           Boolean enabled, String remark, Long operatorId) {
        CodeRule old = get(code);
        List<Segment> target = segments == null || segments.isEmpty() ? old.segments() : validate(segments);
        String policy = normalizePolicy(resetPolicy == null || resetPolicy.isBlank()
                ? old.resetPolicy() : resetPolicy);
        if (!RESET_POLICIES.contains(policy)) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "归零策略只能是 NEVER/YEAR/MONTH/DAY");
        }
        boolean enable = enabled == null ? old.enabled() : enabled;
        jdbcTemplate.update("""
                UPDATE code_rule SET name = ?, segments_json = CAST(? AS jsonb), reset_policy = ?,
                       status = ?, remark = ?, updated_at = now()
                 WHERE code = ?
                """, name == null || name.isBlank() ? old.name() : name, toJson(target), policy,
                enable ? "ACTIVE" : "DISABLED", remark, code);
        audit(code, "UPDATE_SEGMENTS", old.currentNo(), old.currentNo(), operatorId,
                "segments=" + toJson(target) + ",policy=" + policy + ",enabled=" + enable);
        return get(code);
    }

    /** 手动重置/跳号：编码连续性属审计关注点，必须带原因。 */
    @Transactional
    public CodeRule reset(String code, long toNo, String reason, Long operatorId) {
        if (toNo < 0) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "起始流水不能为负");
        }
        if (reason == null || reason.isBlank()) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "重置/跳号必须填写原因（写入审计）");
        }
        CodeRule old = get(code);
        jdbcTemplate.update(
                "UPDATE code_rule SET current_no = ?, updated_at = now() WHERE code = ?", toNo, code);
        audit(code, toNo > old.currentNo() ? "SKIP" : "RESET", old.currentNo(), toNo, operatorId, reason);
        return get(code);
    }

    // ------------------------------------------------------------ 生成

    /** 试算下一个编码（不消耗流水）：管理页"下一个编码"与改规则后的即时校验。 */
    public String preview(String code) {
        CodeRule rule = get(code);
        return render(rule.segments(), nextNo(rule));
    }

    /**
     * 取号：原子自增并返回渲染后的编码。
     *
     * <p>{@code reset_policy != NEVER} 且进入新期间时，先把流水记为 1（而非 current+1）。</p>
     */
    @Transactional
    public String next(String code) {
        return next(code, null, null);
    }

    /**
     * 取号并落**发放流水**（{@code code_issue}，蓝本 {@code CODERES} 的等价物）。
     *
     * @param objectType 细分对象类型（如 {@code PdmItem:PART}），只用于追溯，可为空
     */
    @Transactional
    public String next(String code, String objectType) {
        return next(code, objectType, null);
    }

    /**
     * 取号（支持「按分类独立流水」）。
     *
     * <p>规则含 {@code CLASS} 段时，流水**按分类分别计数**（用 {@code code_sequence} 的
     * {@code scope_key=分类编码}），复刻蓝本「`10010100001`、`10010100002`… 每个分类各自从 1 起」；
     * 不含 {@code CLASS} 段时沿用 {@code code_rule.current_no} 的单一计数。</p>
     *
     * @param classCode 对象所属分类编码；规则含 {@code CLASS} 段时必填
     */
    @Transactional
    public String next(String code, String objectType, String classCode) {
        List<Map<String, Object>> locked = jdbcTemplate.queryForList("""
                SELECT segments_json, current_no, last_period, reset_policy, status
                  FROM code_rule WHERE code = ? FOR UPDATE
                """, code);
        if (locked.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "编码规则不存在：" + code);
        }
        Map<String, Object> row = locked.get(0);
        if (!"ACTIVE".equals(String.valueOf(row.get("status")))) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码规则已停用：" + code);
        }
        List<Segment> segments = parse(String.valueOf(row.get("segments_json")));
        boolean byClass = segments.stream()
                .anyMatch(s -> "CLASS".equalsIgnoreCase(s.type()));
        long nextNo;
        if (byClass) {
            if (classCode == null || classCode.isBlank()) {
                throw BizException.of(ErrorCode.CODE_RULE_INVALID,
                        "编码规则 " + code + " 含分类段，但对象尚未归类，无法取号");
            }
            nextNo = nextClassSeq(code, classCode.trim());
        } else {
            String policy = normalizePolicy(String.valueOf(row.get("reset_policy")));
            String period = period(policy);
            String lastPeriod = row.get("last_period") == null ? null : String.valueOf(row.get("last_period"));
            long current = ((Number) row.get("current_no")).longValue();
            boolean newPeriod = period != null && lastPeriod != null && !period.equals(lastPeriod);
            nextNo = newPeriod ? 1 : current + 1;
            jdbcTemplate.update("""
                    UPDATE code_rule SET current_no = ?, last_period = COALESCE(?, last_period), updated_at = now()
                     WHERE code = ?
                    """, nextNo, period, code);
        }
        String rendered = render(segments, nextNo, classCode);
        recordIssue(code, rendered, objectType, null);
        return rendered;
    }

    /**
     * 按**分类编码路径**取号（蓝本 {@code CLASSCODEPATH} 口径，`EXP-20261108-44`）。
     *
     * <p>路径形如 {@code Kind_61a20f96312e419c/1001/02/*}：首段是 {@code code_rule.code}，
     * 中间是各 {@code VALUE} 段的码值，末尾 {@code *} 是流水占位。
     * 例：{@code 1001} + {@code 02} + {@code 00001} = {@code 10010200001}。</p>
     *
     * @param fullPath 分类的 {@code code_path}
     * @param scopeKey 流水作用域（一般是分类编码），按它独立计数
     */
    @Transactional
    public String nextByPath(String fullPath, String scopeKey) {
        return nextByPath(fullPath, scopeKey, 1).get(0);
    }

    /**
     * 按分类编码路径**批量**取号（蓝本 {@code GeneratorCount}，CODE-01）。
     *
     * <p>一次把 {@code code_sequence} 推进 {@code count}，返回连续 {@code count} 个码；
     * 每条都落发放流水（与单条取号一致）。</p>
     */
    @Transactional
    public List<String> nextByPath(String fullPath, String scopeKey, int count) {
        if (fullPath == null || fullPath.isBlank()) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "分类未绑定编码路径，不能自动取号");
        }
        if (count < 1 || count > 100) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "批量取号数量必须在 1~100 之间");
        }
        String[] parts = fullPath.split("/");
        String ruleCode = parts[0].trim();
        List<String> values = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty() || "*".equals(part)) {
                continue;
            }
            values.add(part);
        }
        List<Map<String, Object>> locked = jdbcTemplate.queryForList("""
                SELECT segments_json, status FROM code_rule WHERE code = ? FOR UPDATE
                """, ruleCode);
        if (locked.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "编码规则不存在：" + ruleCode);
        }
        if (!"ACTIVE".equals(String.valueOf(locked.get(0).get("status")))) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码规则已停用：" + ruleCode);
        }
        List<Segment> segments = parse(String.valueOf(locked.get(0).get("segments_json")));
        String scope = scopeKey == null || scopeKey.isBlank() ? "DEFAULT" : scopeKey.trim();
        long last = nextClassSeq(ruleCode, scope, count);
        long base = last - count + 1;
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String rendered = renderByPath(segments, values, base + i);
            // 留路径快照：分类绑定后续变更不影响已发编码的追溯（T-CORE-10）
            recordIssue(ruleCode, rendered, null, fullPath);
            codes.add(rendered);
        }
        return codes;
    }

    /** 按「路径码值 + 流水」渲染；{@code VALUE} 段依次消费 {@code values}。 */
    private String renderByPath(List<Segment> segments, List<String> values, long no) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Segment segment : segments) {
            switch (segment.type() == null ? "" : segment.type().toUpperCase()) {
                case "TEXT" -> sb.append(segment.value() == null ? "" : segment.value());
                case "DATE" -> sb.append(datePart(segment.format()));
                case "CLASS" -> sb.append("");
                case "VALUE" -> {
                    if (index >= values.size()) {
                        throw BizException.of(ErrorCode.CODE_RULE_INVALID,
                                "编码路径的码值不足（规则需要的 VALUE 段多于路径提供的码值）");
                    }
                    sb.append(values.get(index++));
                }
                case "SEQ" -> sb.append(seqPart(no, segment));
                default -> throw BizException.of(ErrorCode.CODE_RULE_INVALID,
                        "未知段类型：" + segment.type());
            }
        }
        return sb.toString();
    }

    /**
     * 把某个编码的发放流水置为**失效**（蓝本 `SetCodeState('LoseEfficacy')`，CODE-04）。
     *
     * <p>只改状态，不删记录——历史仍可追溯"这个码曾经发过、后来被换掉了"。</p>
     *
     * @return 受影响的流水条数（同一编码可能发放过多次）
     */
    @Transactional
    public int markLost(String code) {
        if (code == null || code.isBlank()) {
            return 0;
        }
        return jdbcTemplate.update("""
                UPDATE code_issue SET state = 'LOSE_EFFICACY'
                 WHERE code = ? AND state = 'ACTIVE'
                """, code.trim());
    }

    /** 分类内独立流水：{@code (rule_code, scope_key)} 原子自增，返回本次分配到的**最后一个**号。 */
    private long nextClassSeq(String ruleCode, String classCode) {
        return nextClassSeq(ruleCode, classCode, 1);
    }

    private long nextClassSeq(String ruleCode, String scopeKey, int count) {
        long used = maxUsedNo(scopeKey);
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = GREATEST(code_sequence.current_no, ?) + ?,
                                      updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId(), ruleCode, scopeKey,
                used + count, used, count);
        return no == null ? used + count : no;
    }

    /**
     * 落发放流水。失败直接抛出：宁可不发号，也不要"号发了却查不到"。
     * 无登录上下文（定时任务/测试直调）时 {@code issued_by} 留空，不阻断。
     */
    private void recordIssue(String ruleCode, String issued, String objectType, String generatorPath) {
        var principal = CurrentUser.orNull();
        Long operatorId = principal == null ? null : principal.userId();
        jdbcTemplate.update("""
                INSERT INTO code_issue (id, rule_code, code, object_type, issued_by, generator_path)
                VALUES (?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), ruleCode, issued,
                objectType == null || objectType.isBlank() ? null : objectType.trim(), operatorId,
                generatorPath == null || generatorPath.isBlank() ? null : generatorPath.trim());
    }

    /**
     * 下一个流水号。仅在「已记录过期间且期间变了」时才归零：
     * {@code last_period} 为空说明这条规则从未按期间取过号（含老数据），此时应接着 current+1，
     * 否则会把历史流水直接抹回 1。
     */
    private long nextNo(CodeRule rule) {
        String period = period(rule.resetPolicy());
        boolean newPeriod = period != null && rule.lastPeriod() != null
                && !period.equals(rule.lastPeriod());
        return newPeriod ? 1 : rule.currentNo() + 1;
    }

    // ------------------------------------------------------------ 渲染

    public String render(List<Segment> segments, long no) {
        return render(segments, no, null);
    }

    /**
     * 渲染编码。
     *
     * <p>{@code CLASS} 段渲染为「对象所属分类的编码」——这是蓝本编码体系的核心：
     * 实测 `CODERES` 里 `CODES` 形如 `10010100001` = 分类码 `100101` + 该分类内 5 位流水，
     * 16,343 条零件-分类关系中 **15,476 条（94.7%）的零件编码以分类码开头**
     * （见 `docs/06` `EXP-20261108-43`）。</p>
     *
     * <p>预览/校验时没有分类，{@code CLASS} 段渲染为字面量 {@code {CLASS}}，便于人读；
     * 真正取号时若仍未归类会抛错（不猜）。</p>
     */
    public String render(List<Segment> segments, long no, String classCode) {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            switch (segment.type() == null ? "" : segment.type().toUpperCase()) {
                case "TEXT" -> sb.append(segment.value() == null ? "" : segment.value());
                case "DATE" -> sb.append(datePart(segment.format()));
                case "CLASS" -> sb.append(classCode == null || classCode.isBlank() ? "{CLASS}" : classCode);
                // VALUE 段的值来自分类编码路径（见 nextByPath）；不带路径时渲染为空，仅用于校验/预览
                case "VALUE" -> sb.append("");
                case "SEQ" -> sb.append(seqPart(no, segment));
                default -> throw BizException.of(ErrorCode.CODE_RULE_INVALID,
                        "未知段类型：" + segment.type() + "（只支持 TEXT/DATE/CLASS/VALUE/SEQ）");
            }
        }
        return sb.toString();
    }

    private String datePart(String format) {
        String fmt = format == null || format.isBlank() ? "yyyyMM" : format;
        if (!DATE_FORMATS.contains(fmt)) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "日期段格式只支持 yyyy/yyyyMM/yyyyMMdd");
        }
        LocalDate today = LocalDate.now();
        return switch (fmt) {
            case "yyyy" -> String.valueOf(today.getYear());
            case "yyyyMMdd" -> today.format(DATE_FMT);
            default -> String.format("%04d%02d", today.getYear(), today.getMonthValue());
        };
    }

    private String seqPart(long no, Segment segment) {
        int width = segment.width() == null ? 6 : segment.width();
        if (width < 1 || width > 12) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "流水段位数须在 1~12 之间");
        }
        String radix = segment.radix() == null || segment.radix().isBlank() ? "DECIMAL" : segment.radix();
        if (!RADIX.contains(radix)) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "流水段进制只支持 DECIMAL/ALNUM");
        }
        String body = "DECIMAL".equals(radix) ? Long.toString(no) : toBase36(no);
        return "0".repeat(Math.max(0, width - body.length())) + body;
    }

    private static String toBase36(long value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        long rest = value;
        while (rest > 0) {
            sb.append(ALNUM.charAt((int) (rest % 36)));
            rest /= 36;
        }
        return sb.reverse().toString();
    }

    /** 期间串：按归零策略取"哪一级时间"。 */
    private static String period(String policy) {
        String p = normalizePolicy(policy);
        LocalDate today = LocalDate.now();
        return switch (p) {
            case "YEAR" -> String.valueOf(today.getYear());
            case "MONTH" -> String.format("%04d%02d", today.getYear(), today.getMonthValue());
            case "DAY" -> today.format(DATE_FMT);
            default -> null;
        };
    }

    private static String normalizePolicy(String policy) {
        String p = policy == null || policy.isBlank() ? "NEVER" : policy.trim().toUpperCase();
        return "NONE".equals(p) ? "NEVER" : p;
    }

    // ------------------------------------------------------------ 内部工具

    private List<Segment> validate(List<Segment> segments) {
        if (segments.isEmpty()) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码规则至少要有一段");
        }
        long seqCount = segments.stream().filter(s -> "SEQ".equalsIgnoreCase(s.type())).count();
        if (seqCount != 1) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "编码规则必须且只能有一个流水段（SEQ）");
        }
        // 先渲染一次：非法段类型/位数/格式在保存时就挡住，而不是等取号时才炸
        render(segments, 1);
        return segments;
    }

    private List<Segment> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Segment>>() {
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "规则段定义解析失败：" + ex.getMessage());
        }
    }

    private String toJson(List<Segment> segments) {
        try {
            return objectMapper.writeValueAsString(segments);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.CODE_RULE_INVALID, "规则段定义序列化失败：" + ex.getMessage());
        }
    }

    private void audit(String code, String action, Long from, Long to, Long operatorId, String reason) {
        jdbcTemplate.update("""
                INSERT INTO code_rule_audit (id, rule_code, action, from_no, to_no, operator_id, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, IdWorker.getId(), code, action, from, to, operatorId, reason);
    }

    private static OffsetDateTime timestamp(java.sql.Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
    }

    private CodeRule map(Long id, String code, String name, String objectType, String segmentsJson,
                         String resetPolicy, String status, long currentNo, String lastPeriod,
                         String remark, OffsetDateTime updatedAt) {
        return new CodeRule(id, code, name, objectType, parse(segmentsJson), normalizePolicy(resetPolicy),
                "ACTIVE".equals(status), currentNo, lastPeriod, remark, updatedAt, null);
    }

    /** 列表返回时补「下一个编码」预览；单条规则非法不影响整页。 */
    public List<CodeRule> withPreviews(List<CodeRule> rules) {
        List<CodeRule> result = new ArrayList<>(rules.size());
        for (CodeRule rule : rules) {
            String next;
            try {
                next = render(rule.segments(), nextNo(rule));
            } catch (RuntimeException ex) {
                next = "规则非法：" + ex.getMessage();
            }
            result.add(rule.withPreview(next));
        }
        return result;
    }

    /** 段定义：{@code TEXT} 用 value；{@code DATE} 用 format；{@code SEQ} 用 width + radix。 */
    public record Segment(String type, String value, String format, Integer width, String radix) {
    }

    /** 规则视图；{@code preview} 由上层按需补。 */
    public record CodeRule(Long id, String code, String name, String objectType, List<Segment> segments,
                           String resetPolicy, boolean enabled, long currentNo, String lastPeriod,
                           String remark, OffsetDateTime updatedAt, String preview) {

        public CodeRule withPreview(String value) {
            return new CodeRule(id, code, name, objectType, segments, resetPolicy, enabled,
                    currentNo, lastPeriod, remark, updatedAt, value);
        }
    }
}
