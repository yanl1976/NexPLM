package com.nexplm.platform.integration.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 字段映射引擎：把源对象按规则转成目标报文。
 *
 * <p>规则形如 {@code [{source:"code", target:"Number", transform:"UPPER"}]}，
 * 路径用点号分隔（{@code material.name}），写回时自动创建中间节点。</p>
 *
 * <p>刻意保持"声明式 + 无脚本"：接口对接最怕在配置里塞一段脚本，
 * 出了问题既没法静态检查也没法在界面上解释；这里只支持有限几种转换，
 * 真要复杂逻辑就应当在适配器里写代码。</p>
 */
public final class MappingEngine {

    public record Rule(String source, String target, String transform, boolean required) {
    }

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
            parsed.add(new Rule(normalizePath(source), normalizePath(target),
                    transform == null || transform.isBlank() ? null
                            : transform.trim().toUpperCase(Locale.ROOT),
                    node.path("required").asBoolean(false)));
        }
        return parsed;
    }

    /** 按规则生成目标报文；required 字段缺失即失败（不产生"少一个必填字段"的半成品报文）。 */
    public static ObjectNode apply(List<Rule> rules, JsonNode source) {
        ObjectNode target = NODES.objectNode();
        for (Rule rule : rules) {
            JsonNode value = read(source, rule.source());
            if (value == null || value.isMissingNode() || value.isNull()
                    || (value.isTextual() && value.asText().isBlank())) {
                if (rule.required()) {
                    throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                            "必填字段缺失：" + rule.source() + " → " + rule.target());
                }
                continue;
            }
            JsonNode converted = convert(value, rule.transform(), rule.target());
            if (converted != null) {
                write(target, rule.target(), converted);
            }
        }
        return target;
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

    private static JsonNode read(JsonNode source, String path) {
        if (source == null) {
            return null;
        }
        JsonNode current = source;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }

    private static void write(ObjectNode target, String path, JsonNode value) {
        String[] segments = path.split("\\.");
        ObjectNode current = target;
        for (int i = 0; i < segments.length - 1; i++) {
            JsonNode child = current.get(segments[i]);
            if (child == null || !child.isObject()) {
                child = NODES.objectNode();
                current.set(segments[i], child);
            }
            current = (ObjectNode) child;
        }
        current.set(segments[segments.length - 1], value);
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
    public static List<Map<String, Object>> preview(JsonNode rules) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (rules == null || !rules.isArray()) {
            return rows;
        }
        for (JsonNode node : rules) {
            rows.add(Map.of(
                    "source", text(node, "source") == null ? "" : text(node, "source"),
                    "target", text(node, "target") == null ? "" : text(node, "target"),
                    "transform", text(node, "transform") == null ? "" : text(node, "transform"),
                    "required", node.path("required").asBoolean(false)));
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
