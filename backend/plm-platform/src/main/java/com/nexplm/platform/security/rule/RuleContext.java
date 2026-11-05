package com.nexplm.platform.security.rule;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则求值上下文：当前用户 + 目标对象属性。
 * <p>属性名支持点路径（如 ext_json.priority），对应对象的扩展属性。</p>
 */
public final class RuleContext {

    private final String currentUserId;
    private final String currentCompanyId;
    private final Map<String, Object> attributes;

    public RuleContext(String currentUserId, String currentCompanyId, Map<String, Object> attributes) {
        this.currentUserId = currentUserId;
        this.currentCompanyId = currentCompanyId;
        this.attributes = attributes == null ? Map.of() : new HashMap<>(attributes);
        // 内置变量，规则里可直接引用
        this.attributes.putIfAbsent("currentUserId", currentUserId);
        this.attributes.putIfAbsent("currentCompanyId", currentCompanyId);
    }

    public Object attr(String field) {
        if (field == null) {
            return null;
        }
        if (attributes.containsKey(field)) {
            return attributes.get(field);
        }
        int dot = field.indexOf('.');
        while (dot > 0) {
            String head = field.substring(0, dot);
            Object headValue = attributes.get(head);
            String tail = field.substring(dot + 1);
            if (headValue instanceof Map<?, ?> map) {
                return nested(map, tail);
            }
            dot = field.indexOf('.', dot + 1);
        }
        return null;
    }

    private Object nested(Map<?, ?> map, String path) {
        Object current = map;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(segment);
        }
        return current;
    }

    public String currentUserId() {
        return currentUserId;
    }

    public String currentCompanyId() {
        return currentCompanyId;
    }

    public Map<String, Object> attributes() {
        return Map.copyOf(attributes);
    }
}
