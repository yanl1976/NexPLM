package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.iam.domain.PermissionRule;
import com.nexplm.platform.iam.mapper.PermissionRuleMapper;
import com.nexplm.platform.security.rule.ConditionNode;
import com.nexplm.platform.security.rule.RuleContext;
import com.nexplm.platform.security.rule.RuleEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 规则权限服务：规则的增删改查、试算，以及按对象类型匹配用户应得的权限。
 */
@Service
public class PermissionRuleService {

    public static final Set<String> ACTIONS = Set.of(
            "READ", "WRITE", "DELETE", "GRANT", "CHECKOUT", "RELEASE", "APPROVE");

    private static final Set<String> EFFECTS = Set.of("ALLOW", "DENY");

    private final PermissionRuleMapper ruleMapper;
    private final RuleEvaluator ruleEvaluator;
    private final ObjectMapper objectMapper;

    public PermissionRuleService(PermissionRuleMapper ruleMapper, RuleEvaluator ruleEvaluator,
                                 ObjectMapper objectMapper) {
        this.ruleMapper = ruleMapper;
        this.ruleEvaluator = ruleEvaluator;
        this.objectMapper = objectMapper;
    }

    public List<SecurityDtos.RuleRow> list(String objectType) {
        LambdaQueryWrapper<PermissionRule> query = new LambdaQueryWrapper<PermissionRule>()
                .orderByAsc(PermissionRule::getPriority)
                .orderByAsc(PermissionRule::getId);
        if (objectType != null && !objectType.isBlank()) {
            query.eq(PermissionRule::getObjectType, objectType);
        }
        return ruleMapper.selectList(query).stream().map(this::toRow).toList();
    }

    @Transactional
    public Long save(SecurityDtos.RuleSaveReq req, boolean create) {
        validateExpr(req.exprJson());
        String actions = normalizeActions(req.actions());
        String effect = req.effect() == null || req.effect().isBlank() ? "ALLOW" : req.effect().toUpperCase();
        if (!EFFECTS.contains(effect)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "effect 仅支持 ALLOW / DENY");
        }
        PermissionRule rule;
        if (create) {
            if (ruleMapper.selectCount(new LambdaQueryWrapper<PermissionRule>()
                    .eq(PermissionRule::getCode, req.code())) > 0) {
                throw BizException.of(ErrorCode.COMMON_CONFLICT, "规则编码已存在：" + req.code());
            }
            rule = new PermissionRule();
            rule.setCode(req.code().trim());
            rule.setCreatedAt(OffsetDateTime.now());
        } else {
            rule = requireByCode(req.code());
        }
        rule.setName(req.name().trim());
        rule.setObjectType(req.objectType().trim());
        rule.setExprJson(req.exprJson());
        rule.setActions(actions);
        rule.setPriority(req.priority() == null ? 100 : req.priority());
        rule.setEffect(effect);
        rule.setEnabled(req.enabled() == null || req.enabled());
        rule.setRemark(req.remark());
        rule.setUpdatedAt(OffsetDateTime.now());
        if (create) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
        return rule.getId();
    }

    @Transactional
    public void delete(Long id) {
        if (ruleMapper.selectById(id) == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "规则不存在");
        }
        ruleMapper.deleteById(id);
    }

    public SecurityDtos.RuleTestResp test(SecurityDtos.RuleTestReq req) {
        List<ConditionNode> nodes = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        RuleContext context = new RuleContext(
                req.currentUserId() == null ? "0" : req.currentUserId(),
                req.currentCompanyId() == null ? "0" : req.currentCompanyId(),
                req.attributes() == null ? java.util.Map.of() : req.attributes());
        if (req.ruleId() != null) {
            PermissionRule rule = ruleMapper.selectById(req.ruleId());
            if (rule == null) {
                throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "规则不存在");
            }
            nodes.add(parse(rule.getExprJson()));
            if (ruleEvaluator.evaluate(nodes.get(0), context)) {
                matched.add(rule.getCode());
            }
        } else {
            if (req.exprJson() == null || req.exprJson().isBlank()) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "ruleId 与 exprJson 至少提供一个");
            }
            ConditionNode node = parse(req.exprJson());
            nodes.add(node);
            if (ruleEvaluator.evaluate(node, context)) {
                matched.add("INLINE");
            }
        }
        return new SecurityDtos.RuleTestResp(!matched.isEmpty(), matched);
    }

    /**
     * 计算用户在指定对象类型上由规则获得的权限（供 ACL 判定合并使用）。
     */
    public Set<String> resolveActions(String objectType, RuleContext context) {
        Set<String> actions = new LinkedHashSet<>();
        for (PermissionRule rule : ruleMapper.selectEnabledByType(objectType)) {
            if (!ruleEvaluator.evaluate(parse(rule.getExprJson()), context)) {
                continue;
            }
            for (String action : rule.getActions().split(",")) {
                String trimmed = action.trim().toUpperCase();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if ("DENY".equals(rule.getEffect())) {
                    actions.remove(trimmed);
                } else {
                    actions.add(trimmed);
                }
            }
        }
        return actions;
    }

    private void validateExpr(String exprJson) {
        parse(exprJson);
    }

    private ConditionNode parse(String exprJson) {
        try {
            return ConditionNode.from(objectMapper.readTree(exprJson));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "条件表达式解析失败：" + ex.getMessage());
        }
    }

    private String normalizeActions(String actions) {
        List<String> normalized = new ArrayList<>();
        for (String action : actions.split(",")) {
            String trimmed = action.trim().toUpperCase();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ACTIONS.contains(trimmed)) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "不支持的操作：" + trimmed);
            }
            normalized.add(trimmed);
        }
        if (normalized.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "至少需要授予一个操作");
        }
        return String.join(",", normalized);
    }

    private PermissionRule requireByCode(String code) {
        PermissionRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<PermissionRule>()
                .eq(PermissionRule::getCode, code).last("limit 1"));
        if (rule == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "规则不存在：" + code);
        }
        return rule;
    }

    private SecurityDtos.RuleRow toRow(PermissionRule rule) {
        return new SecurityDtos.RuleRow(rule.getId(), rule.getCode(), rule.getName(), rule.getObjectType(),
                rule.getExprJson(), rule.getActions(), rule.getPriority(), rule.getEffect(),
                rule.getEnabled(), rule.getRemark(), rule.getUpdatedAt());
    }
}
