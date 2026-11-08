package com.nexplm.platform.coding.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
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
        String policy = normalizePolicy(String.valueOf(row.get("reset_policy")));
        String period = period(policy);
        String lastPeriod = row.get("last_period") == null ? null : String.valueOf(row.get("last_period"));
        long current = ((Number) row.get("current_no")).longValue();
        boolean newPeriod = period != null && lastPeriod != null && !period.equals(lastPeriod);
        long nextNo = newPeriod ? 1 : current + 1;
        jdbcTemplate.update("""
                UPDATE code_rule SET current_no = ?, last_period = COALESCE(?, last_period), updated_at = now()
                 WHERE code = ?
                """, nextNo, period, code);
        return render(parse(String.valueOf(row.get("segments_json"))), nextNo);
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
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            switch (segment.type() == null ? "" : segment.type().toUpperCase()) {
                case "TEXT" -> sb.append(segment.value() == null ? "" : segment.value());
                case "DATE" -> sb.append(datePart(segment.format()));
                case "SEQ" -> sb.append(seqPart(no, segment));
                default -> throw BizException.of(ErrorCode.CODE_RULE_INVALID,
                        "未知段类型：" + segment.type() + "（只支持 TEXT/DATE/SEQ）");
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
