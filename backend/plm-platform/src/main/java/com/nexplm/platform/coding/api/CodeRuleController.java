package com.nexplm.platform.coding.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.coding.service.CodeRuleService;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 编码管理（对齐旧系统「业务管理-编码管理」）：旧系统 13 个页面维护的其实是同一件事——
 * 编码由哪些段拼成、流水走到几、什么时候归零；这里收敛为「规则列表 + 编辑 + 试算/重置」。
 *
 * <p>规则是平台级配置（旧系统亦不分组织），故不带 {@code companyId}；
 * 写操作全部落 {@code code_rule_audit}。</p>
 */
@Tag(name = "编码管理")
@RestController
@RequestMapping("/api/v1/system/coding")
public class CodeRuleController {

    private final CodeRuleService codeRuleService;

    public CodeRuleController(CodeRuleService codeRuleService) {
        this.codeRuleService = codeRuleService;
    }

    @Operation(summary = "编码规则列表（含每条规则的下一个编码预览）")
    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<List<CodeRuleService.CodeRule>> rules() {
        return Result.ok(codeRuleService.withPreviews(codeRuleService.list()));
    }

    @Operation(summary = "单条编码规则")
    @GetMapping("/rules/{code}")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<CodeRuleService.CodeRule> rule(@PathVariable String code) {
        return Result.ok(codeRuleService.get(code).withPreview(codeRuleService.preview(code)));
    }

    @Operation(summary = "试算下一个编码（不消耗流水）")
    @GetMapping("/rules/{code}/preview")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<String> preview(@PathVariable String code) {
        return Result.ok(codeRuleService.preview(code));
    }

    @Operation(summary = "修改编码规则（段定义/归零策略/启停，落审计）")
    @PutMapping("/rules/{code}")
    @PreAuthorize("hasAuthority('system.coding.manage')")
    @Audit(module = "SYSTEM", action = "CODE_RULE_UPDATE", objectType = "CodeRule")
    public Result<CodeRuleService.CodeRule> update(@PathVariable String code,
                                                  @Valid @RequestBody RuleUpdateReq req) {
        CodeRuleService.CodeRule rule = codeRuleService.update(code, req.name(), req.segments(),
                req.resetPolicy(), req.enabled(), req.remark(), CurrentUser.userId());
        return Result.ok(rule.withPreview(codeRuleService.preview(code)));
    }

    @Operation(summary = "重置/跳号（必须填原因，落审计）")
    @PostMapping("/rules/{code}/reset")
    @PreAuthorize("hasAuthority('system.coding.manage')")
    @Audit(module = "SYSTEM", action = "CODE_RULE_RESET", objectType = "CodeRule")
    public Result<CodeRuleService.CodeRule> reset(@PathVariable String code,
                                                 @Valid @RequestBody RuleResetReq req) {
        CodeRuleService.CodeRule rule = codeRuleService.reset(code, req.toNo(), req.reason(),
                CurrentUser.userId());
        return Result.ok(rule.withPreview(codeRuleService.preview(code)));
    }

    public record RuleUpdateReq(
            @Size(max = 200) String name,
            List<CodeRuleService.Segment> segments,
            @Size(max = 16) String resetPolicy,
            Boolean enabled,
            @Size(max = 500) String remark) {
    }

    public record RuleResetReq(
            long toNo,
            @Size(max = 500) String reason) {
    }
}
