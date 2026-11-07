package com.nexplm.platform.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 字段映射引擎：把源对象按规则转成目标报文。
 *
 * <p>规则形如
 * {@code [{source:"code", target:"Number", transform:"UPPER", required:true}]}，
 * 路径用点号分隔（{@code material.name}），写回时自动创建中间节点。</p>
 *
 * <p>为 ERP 三段映射扩了三项能力（见 {@code docs/06} EXP-20261107-17）：</p>
 * <ol>
 *   <li><b>嵌套 / 数组路径</b>：目标可写 {@code Model.SubHeadEntity.FErpClsID}、
 *       {@code Model.FBarCodeEntity_CMK[0].FUnitId_CMK.FNUMBER}；以 {@code [].} 开头的目标
 *       表示"逐条分录"，源用 {@code lines[].childCode} 这种数组路径取值。</li>
 *   <li><b>特殊占位符</b>：{@code System$CurrentDate}（当天日期）、
 *       {@code USERINFO$ECODE}（当前用户编码）、{@code Parent$XXX}（父/根节点字段）。</li>
 *   <li><b>来源别名</b>：{@code sourcePath} 与 {@code source} 分开存——{@code source} 保真旧 PDM 列名，
 *       {@code sourcePath} 是新系统载荷路径；为空表示"新系统没有数据源"（缺口）。</li>
 * </ol>
 *
 * <p>刻意保持"声明式 + 无脚本"：接口对接最怕在配置里塞一段脚本，
 * 出了问题既没法静态检查也没法在界面上解释；这里只支持有限几种转换，
 * 真要复杂逻辑就应当在适配器里写代码。</p>
 */
public final class MappingEngine {

    /**
     * 一条映射规则。
     *
     * @param source     源字段（旧 PDM 列名或新系统载荷字段名）
     * @param target     目标路径（ERP 报文里的 SendPath）
     * @param transform  UPPER/LOWER/TRIM/STRING/NUMBER/BOOLEAN
     * @param required   必填；缺失直接失败（试算模式下改为记录缺口）
     * @param sourcePath 解析后的新系统取值路径；null 表示新系统无数据源
     * @param note       缺口/待确认说明，试算与页面展示用
     */
    public record Rule(String source, String target, String transform, boolean required,
                       String sourcePath, String note) {

        public Rule(String source, String target, String transform, boolean required) {
            this(source, target, transform, required, null, null);
        }

        /** 实际用于取值/写报文的路径：有 sourcePath 用 sourcePath，否则退回 source。 */
        public String effectiveSource() {
            return sourcePath != null && !sourcePath.isBlank() ? sourcePath : source;
        }
    }

    /** 试算结果：报文 + 未取到值的字段清单（不静默丢字段）。 */
    public record Applied(ObjectNode payload, List<Missing> missing) {
    }

    /** 缺失字段：目标路径 + 来源 + 原因。 */
    public record Missing(String target, String source, String reason) {
    }

    /** 分录数组在报文里的键名；ERP 侧真正的分录键名待真机联调确认（见 docs/06 EXP-20261107-18）。 */
    public static final String ENTRY_KEY = "entries";

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private MappingEngine() {
    }

    /** 把 jsonb 里的规则数组解析成强类型规则，顺带做基本校验。 */
    public static List<Rule> parseRules(JsonNode rules) {
        if (rules == null || !rules.isArray()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "映射规则必须是数组");
        }
        List<Rule> parsed = new ArrayList<>();
        for (JsonNode node : rules) {
            String target = text(node, "target");
            if (target == null || target.isBlank()) {
                throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "映射规则缺少 target");
            }
            String source = text(node, "source");
            if (source == null || source.isBlank()) {
                throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                        "映射规则缺少 source：" + target);
            }
            String transform = text(node, "transform");
            String sourcePath = text(node, "sourcePath");
            parsed.add(new Rule(normalizePath(source), normalizePath(target),
                    transform == null || transform.isBlank()
                    ? null : transform.trim().toUpperCase(Locale.ROOT),
                    node.path("required").asBoolean(false),
                    sourcePath == null || sourcePath.isBlank() ? null : sourcePath.trim(),
                    text(node, "note")));
        }
        return parsed;
    }

    /** 严格模式：required 字段缺失即失败（不产生"少一个必填字段"的半成品报文）。 */
    public static ObjectNode apply(List<Rule> rules, JsonNode source) {
        return applyInternal(rules, source, Map.of(), true).payload();
    }

    /** 带上下文的严格模式：上下文提供当前用户编码、父/根节点（BOM 根件）等占位符取值。 */
    public static ObjectNode apply(List<Rule> rules, JsonNode source, Map<String, Object> context) {
        return applyInternal(rules, source, context == null ? Map.of() : context, true).payload();
    }

    /** 试算模式：缺失字段只记录不抛错，用于"下发前先看缺口"。 */
    public static Applied preview(List<Rule> rules, JsonNode source, Map<String, Object> context) {
        return applyInternal(rules, source, context == null ? Map.of() : context, false);
    }

    private static Applied applyInternal(List<Rule> rules, JsonNode source,
                                         Map<String, Object> context, boolean strict) {
        ObjectNode payload = NODES.objectNode();
        List<Missing> missing = new ArrayList<>();
        List<Rule> entryRules = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.target().startsWith("[]")) {
                entryRules.add(rule);
                continue;
            }
            JsonNode value = resolve(rule, source, context, null);
            if (isBlank(value)) {
                handleMissing(rule, strict, missing);
                continue;
            }
            write(payload, rule.target(), convert(value, rule.transform(), rule.target()));
        }
        if (!entryRules.isEmpty()) {
            writeEntries(payload, entryRules, source, context, strict, missing);
        }
        return new Applied(payload, missing);
    }

    /** 分录规则：源路径形如 {@code lines[].childCode}，逐条组装到 {@code entries} 数组。 */
    private static void writeEntries(ObjectNode payload, List<Rule> rules, JsonNode source,
                                     Map<String, Object> context, boolean strict,
                                     List<Missing> missing) {
        // 按源数组根分组（默认 lines）；同一份报文里通常只有一个分录数组
        Map<String, List<Rule>> byRoot = new LinkedHashMap<>();
        for (Rule rule : rules) {
            byRoot.computeIfAbsent(arrayRoot(rule.effectiveSource()), key -> new ArrayList<>()).add(rule);
        }
        for (Map.Entry<String, List<Rule>> group : byRoot.entrySet()) {
            JsonNode array = read(source, group.getKey());
            ArrayNode entries = payload.withArray(ENTRY_KEY);
            if (array == null || !array.isArray() || array.isEmpty()) {
                for (Rule rule : group.getValue()) {
                    handleMissing(rule, strict, missing);
                }
                continue;
            }
            while (entries.size() < array.size()) {
                entries.addObject();
            }
            for (int i = 0; i < array.size(); i++) {
                ObjectNode item = (ObjectNode) entries.get(i);
                JsonNode element = array.get(i);
                for (Rule rule : group.getValue()) {
                    JsonNode value = resolve(rule, source, context, element);
                    if (isBlank(value)) {
                        handleMissing(rule, strict, missing);
                        continue;
                    }
                    write(item, stripEntryPrefix(rule.target()),
                            convert(value, rule.transform(), rule.target()));
                }
            }
        }
    }

    /** 取一条规则的值：优先数组元素（分录），其次占位符，最后按路径读源。 */
    private static JsonNode resolve(Rule rule, JsonNode source, Map<String, Object> context,
                                    JsonNode element) {
        String path = rule.effectiveSource();
        // 客制字段口径写 ext_json.F_XXX，而源报文里已摊平（见 KingdeePayloadService#copyCustomFields），
        // 这里去掉前缀直接按同名键读，避免报文多一层嵌套
        if (path.startsWith("ext_json.")) {
            path = path.substring("ext_json.".length());
        }
        JsonNode placeholder = placeholder(path, context);
        if (placeholder != null) {
            return placeholder;
        }
        if (element != null) {
            String field = fieldAfterArray(path);
            return read(element, field);
        }
        if (path.contains("[]")) {
            // 分录规则却拿不到元素：交给调用方按缺口处理
            return null;
        }
        return read(source, path);
    }

    /** 占位符取值：System$ / USERINFO$ / Parent$。取不到返回 null（走缺口/必填逻辑）。 */
    private static JsonNode placeholder(String path, Map<String, Object> context) {
        if (path.startsWith("System$")) {
            String name = path.substring("System$".length()).trim();
            return switch (name) {
                case "CurrentDate", "Today" -> NODES.textNode(LocalDate.now().toString());
                default -> null;
            };
        }
        if (path.startsWith("USERINFO$")) {
            Object userCode = context.get("userCode");
            return userCode == null ? null : NODES.textNode(String.valueOf(userCode));
        }
        if (path.startsWith("Parent$")) {
            String field = path.substring("Parent$".length()).trim();
            JsonNode parent = context.get("parent") instanceof JsonNode node ? node : null;
            return parent == null ? null : read(parent, field);
        }
        return null;
    }

    private static void handleMissing(Rule rule, boolean strict, List<Missing> missing) {
        String reason = rule.note() != null && !rule.note().isBlank()
                ? rule.note()
                : (rule.sourcePath() == null ? "新系统无数据源" : "源数据为空");
        if (strict && rule.required()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                    "必填字段缺失：" + rule.source() + " → " + rule.target() + "（" + reason + "）");
        }
        missing.add(new Missing(rule.target(), rule.source(), reason));
    }

    private static JsonNode convert(JsonNode value, String transform, String target) {
        if (transform == null) {
            return value;
        }
        return switch (transform) {
            case "UPPER" -> NODES.textNode(value.asText().toUpperCase(Locale.ROOT));
            case "LOWER" -> NODES.textNode(value.asText().toLowerCase(Locale.ROOT));
            case "TRIM" -> NODES.textNode(value.asText().trim());
            case "STRING" -> NODES.textNode(value.asText());
            case "NUMBER" -> {
                try {
                    yield NODES.numberNode(new BigDecimal(value.asText().trim()));
                } catch (RuntimeException ex) {
                    throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                            "字段无法转成数值：" + target + "=" + value.asText());
                }
            }
            case "BOOLEAN" -> NODES.booleanNode(Boolean.parseBoolean(value.asText().trim()));
            default -> throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                    "不支持的转换方式：" + transform);
        };
    }

    /**
     * 读路径：支持 {@code a.b}、{@code a[0].b}。
     * <p>含 {@code []} 的数组遍历路径不在这里处理（由分录逻辑逐元素读）。</p>
     */
    private static JsonNode read(JsonNode source, String path) {
        if (source == null || path == null || path.isBlank() || path.contains("[]")) {
            return null;
        }
        JsonNode current = source;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            int bracket = segment.indexOf('[');
            if (bracket > 0) {
                String name = segment.substring(0, bracket);
                int index = indexOf(segment);
                current = current.get(name);
                if (current == null || !current.isArray() || index < 0 || current.size() <= index) {
                    return null;
                }
                current = current.get(index);
            } else {
                current = current.get(segment);
            }
        }
        return current;
    }

    /**
     * 写路径：自动创建中间对象；{@code [n]} 按索引补位（ERP 报文里偶见固定下标数组）。
     */
    private static void write(ObjectNode root, String path, JsonNode value) {
        String[] segments = path.split("\\.");
        JsonNode current = root;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean last = i == segments.length - 1;
            int bracket = segment.indexOf('[');
            String name = bracket < 0 ? segment : segment.substring(0, bracket);
            Integer index = bracket < 0 ? null : indexOf(segment);
            if (name.isBlank()) {
                throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "非法的目标路径：" + path);
            }
            ObjectNode container = (ObjectNode) current;
            if (last && index == null) {
                container.set(name, value);
                return;
            }
            if (index == null) {
                JsonNode child = container.get(name);
                if (child == null || !child.isObject()) {
                    child = NODES.objectNode();
                    container.set(name, child);
                }
                current = child;
            } else {
                JsonNode child = container.get(name);
                ArrayNode array = child != null && child.isArray() ? (ArrayNode) child : NODES.arrayNode();
                while (array.size() <= index) {
                    array.add(NODES.objectNode());
                }
                container.set(name, array);
                if (last) {
                    array.set(index, value);
                    return;
                }
                JsonNode element = array.get(index);
                if (!element.isObject()) {
                    element = NODES.objectNode();
                    array.set(index, element);
                }
                current = element;
            }
        }
    }

    private static int indexOf(String segment) {
        int open = segment.indexOf('[');
        int close = segment.indexOf(']', open + 1);
        if (open < 0 || close < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(segment.substring(open + 1, close).trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** 源路径的数组根：{@code lines[].childCode} → {@code lines}；无数组取默认 lines。 */
    private static String arrayRoot(String path) {
        int bracket = path.indexOf("[]");
        if (bracket <= 0) {
            return "lines";
        }
        String before = path.substring(0, bracket);
        int dot = before.lastIndexOf('.');
        return dot < 0 ? before : before.substring(dot + 1);
    }

    /** 元素内字段：{@code lines[].childCode} → {@code childCode}；无数组则原样（元素自身的字段）。 */
    private static String fieldAfterArray(String path) {
        int bracket = path.indexOf("[]");
        if (bracket < 0) {
            return path;
        }
        String rest = path.substring(bracket + 2);
        return rest.startsWith(".") ? rest.substring(1) : rest;
    }

    private static String stripEntryPrefix(String target) {
        String rest = target.substring(2);
        return rest.startsWith(".") ? rest.substring(1) : rest;
    }

    private static boolean isBlank(JsonNode value) {
        return value == null || value.isMissingNode() || value.isNull()
                || (value.isTextual() && value.asText().isBlank());
    }

    private static String normalizePath(String path) {
        String value = path.trim();
        return value.startsWith("$.") ? value.substring(2) : value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** 供 DTO 层做规则回显：保持原样（不抛异常），空/非法一律返回空数组。 */
    public static List<Map<String, Object>> ruleRows(JsonNode rules) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (rules == null || !rules.isArray()) {
            return rows;
        }
        for (JsonNode node : rules) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", text(node, "source") == null ? "" : text(node, "source"));
            row.put("target", text(node, "target") == null ? "" : text(node, "target"));
            row.put("transform", text(node, "transform") == null ? "" : text(node, "transform"));
            row.put("required", node.path("required").asBoolean(false));
            row.put("sourcePath", text(node, "sourcePath"));
            row.put("note", text(node, "note"));
            rows.add(row);
        }
        return rows;
    }

    /** 便于测试与适配器断言：源数据若是数组，逐条映射。 */
    public static ArrayNode applyToList(List<Rule> rules, JsonNode sourceArray) {
        ArrayNode array = NODES.arrayNode();
        if (sourceArray != null && sourceArray.isArray()) {
            for (JsonNode item : sourceArray) {
                array.add(apply(rules, item));
            }
        }
        return array;
    }
}
