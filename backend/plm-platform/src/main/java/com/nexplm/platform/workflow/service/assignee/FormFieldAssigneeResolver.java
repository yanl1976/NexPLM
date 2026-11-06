package com.nexplm.platform.workflow.service.assignee;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 表单字段：{@code expression} 为流程变量名，取变量值作为用户 id（支持单值、集合、逗号分隔字符串）。
 * <p>用于「审批人由申请单上的字段决定」的场景，如选择部门经理后自动带出审批人。</p>
 */
@Component
public class FormFieldAssigneeResolver implements AssigneeResolver {

    @Override
    public String type() {
        return "FORM_FIELD";
    }

    @Override
    public String label() {
        return "表单字段";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        if (context == null || context.variables() == null || spec.expression() == null
                || spec.expression().isBlank()) {
            return List.of();
        }
        return toUserIds(context.variables().get(spec.expression().trim()));
    }

    private List<Long> toUserIds(Object value) {
        if (value == null) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                Long id = parse(item);
                if (id != null) {
                    result.add(id);
                }
            }
            return result;
        }
        if (value instanceof Object[] array) {
            for (Object item : array) {
                Long id = parse(item);
                if (id != null) {
                    result.add(id);
                }
            }
            return result;
        }
        if (value instanceof String text) {
            for (String part : text.split(",")) {
                Long id = parse(part);
                if (id != null) {
                    result.add(id);
                }
            }
            return result;
        }
        Long single = parse(value);
        return single == null ? List.of() : List.of(single);
    }

    private Long parse(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
