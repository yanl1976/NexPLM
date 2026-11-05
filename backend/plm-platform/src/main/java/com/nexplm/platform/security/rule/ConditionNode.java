package com.nexplm.platform.security.rule;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 权限规则条件树节点。
 * <p>与旧系统扁平 AND/OR 的区别：支持任意层嵌套 AND / OR / NOT。</p>
 * <pre>
 * {"op":"AND","children":[
 *   {"op":"EQ","field":"ownerId","value":"${currentUserId}"},
 *   {"op":"OR","children":[{"op":"EQ","field":"lifecycleState","value":"Released"}]}
 * ]}
 * </pre>
 */
public record ConditionNode(String op, String field, Object value, List<ConditionNode> children) {

    /** 支持的运算符白名单。 */
    public static final Set<String> OPS = Set.of(
            "AND", "OR", "NOT",
            "EQ", "NE", "IN", "NOT_IN",
            "GT", "GE", "LT", "LE",
            "CONTAINS", "STARTS_WITH", "IS_NULL", "NOT_NULL");

    private static final Set<String> LOGIC_OPS = Set.of("AND", "OR", "NOT");

    public static ConditionNode from(JsonNode node) {
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("条件节点不能为空");
        }
        String op = node.path("op").asText("AND").toUpperCase();
        if (!OPS.contains(op)) {
            throw new IllegalArgumentException("不支持的条件运算符：" + op);
        }
        String field = node.hasNonNull("field") ? node.get("field").asText() : null;
        Object value = node.has("value") ? toJava(node.get("value")) : null;
        List<ConditionNode> children = new ArrayList<>();
        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                children.add(from(child));
            }
        }
        if (LOGIC_OPS.contains(op)) {
            if (children.isEmpty()) {
                throw new IllegalArgumentException(op + " 至少需要一个子条件");
            }
            if ("NOT".equals(op) && children.size() != 1) {
                throw new IllegalArgumentException("NOT 只允许一个子条件");
            }
        } else {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException(op + " 必须指定 field");
            }
            if (!"IS_NULL".equals(op) && !"NOT_NULL".equals(op) && value == null) {
                throw new IllegalArgumentException(op + " 必须指定 value");
            }
        }
        return new ConditionNode(op, field, value, List.copyOf(children));
    }

    private static Object toJava(JsonNode value) {
        if (value.isArray()) {
            List<Object> list = new ArrayList<>();
            value.forEach(item -> list.add(toJava(item)));
            return list;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
