package com.nexplm.platform.workflow.service.assignee;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 执行人解析注册表：收集所有 {@link AssigneeResolver} 实现，按类型分发并统一去重/报错。
 */
@Component
public class AssigneeResolverRegistry {

    public record Info(String type, String label) {
    }

    private final Map<String, AssigneeResolver> resolvers = new HashMap<>();

    public AssigneeResolverRegistry(List<AssigneeResolver> resolverList) {
        for (AssigneeResolver resolver : resolverList) {
            resolvers.put(normalize(resolver.type()), resolver);
        }
    }

    /**
     * 批量解析并合并去重；任一声明解析不到人即报错（避免流程跳过审批节点）。
     */
    public List<Long> resolve(List<AssigneeSpec> specs, AssigneeContext context) {
        if (specs == null || specs.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> users = new LinkedHashSet<>();
        for (AssigneeSpec spec : specs) {
            AssigneeResolver resolver = resolvers.get(normalize(spec.type()));
            if (resolver == null) {
                throw BizException.of(ErrorCode.WF_VARIABLE_INVALID, "不支持的执行人类型：" + spec.type());
            }
            List<Long> resolved = resolver.resolve(spec, context);
            if (resolved == null || resolved.isEmpty()) {
                throw BizException.of(ErrorCode.WF_VARIABLE_INVALID,
                        "执行人解析为空（" + resolver.label() + "），请检查配置");
            }
            users.addAll(resolved);
        }
        return List.copyOf(users);
    }

    /** 已注册的解析类型，供前端渲染选择器。 */
    public List<Info> supported() {
        List<Info> list = new ArrayList<>();
        resolvers.forEach((type, resolver) -> list.add(new Info(type, resolver.label())));
        list.sort(Comparator.comparing(Info::type));
        return list;
    }

    public static String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }
}
