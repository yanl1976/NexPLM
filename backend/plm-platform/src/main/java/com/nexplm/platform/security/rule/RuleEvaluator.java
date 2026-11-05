package com.nexplm.platform.security.rule;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

/**
 * 规则求值器：纯函数、无副作用，便于单元测试与复用（权限判定、查询过滤、通知条件）。
 * <p>类型比较策略：数值按 BigDecimal；布尔按 Boolean；其余统一按字符串比较（大小比较仅对数值/日期字符串有意义）。</p>
 */
@Component
public class RuleEvaluator {

    public boolean evaluate(ConditionNode node, RuleContext context) {
        Object expected = resolve(node.value(), context);
        return switch (node.op()) {
            case "AND" -> node.children().stream().allMatch(child -> evaluate(child, context));
            case "OR" -> node.children().stream().anyMatch(child -> evaluate(child, context));
            case "NOT" -> !evaluate(node.children().get(0), context);
            case "IS_NULL" -> context.attr(node.field()) == null;
            case "NOT_NULL" -> context.attr(node.field()) != null;
            case "EQ" -> compare(context.attr(node.field()), expected) == 0;
            case "NE" -> compare(context.attr(node.field()), expected) != 0;
            case "GT" -> compare(context.attr(node.field()), expected) > 0;
            case "GE" -> compare(context.attr(node.field()), expected) >= 0;
            case "LT" -> compare(context.attr(node.field()), expected) < 0;
            case "LE" -> compare(context.attr(node.field()), expected) <= 0;
            case "IN" -> contains(expected, context.attr(node.field()));
            case "NOT_IN" -> !contains(expected, context.attr(node.field()));
            case "CONTAINS" -> contains(context.attr(node.field()), expected);
            case "STARTS_WITH" -> {
                Object actual = context.attr(node.field());
                yield actual != null && String.valueOf(actual).startsWith(String.valueOf(expected));
            }
            default -> throw new IllegalArgumentException("不支持的条件运算符：" + node.op());
        };
    }

    /**
     * 解析内置变量占位符：值形如 ${currentUserId} 时从上下文取实际值。
     */
    private Object resolve(Object value, RuleContext context) {
        if (value instanceof String text && text.startsWith("${") && text.endsWith("}")) {
            return context.attr(text.substring(2, text.length() - 1));
        }
        return value;
    }

    private boolean contains(Object container, Object item) {
        if (container == null || item == null) {
            return false;
        }
        if (container instanceof Collection<?> collection) {
            return collection.stream().anyMatch(element -> compare(element, item) == 0);
        }
        return String.valueOf(container).contains(String.valueOf(item));
    }

    private int compare(Object left, Object right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        BigDecimal leftNumber = asNumber(left);
        BigDecimal rightNumber = asNumber(right);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber);
        }
        if (left instanceof Boolean leftBool && right instanceof Boolean rightBool) {
            return Boolean.compare(leftBool, rightBool);
        }
        if (left instanceof Collection<?> collection) {
            return collection.stream().anyMatch(element -> compare(element, right) == 0) ? 0 : -1;
        }
        return Objects.equals(String.valueOf(left), String.valueOf(right)) ? 0
                : String.valueOf(left).compareTo(String.valueOf(right));
    }

    private BigDecimal asNumber(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }
}
