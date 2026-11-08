package com.nexplm.platform.coding.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.platform.coding.service.CodeRuleService;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "编码发放流水（谁在什么时候取了什么码；按规则/细分类型/编码过滤）")
    @GetMapping("/issues")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<PageResult<CodeRuleService.IssueRow>> issues(@RequestParam(required = false) String ruleCode,
                                                               @RequestParam(required = false) String objectType,
                                                               @RequestParam(required = false) String code,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return Result.ok(codeRuleService.pageIssues(ruleCode, objectType, code, page, size));
    }

    @Operation(summary = "编码种类列表（对应蓝本 CODEKIND；含码值数与已绑定分类数）")
    @GetMapping("/kinds")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<List<CodeRuleService.KindRow>> kinds() {
        return Result.ok(codeRuleService.listKinds());
    }

    @Operation(summary = "某个编码种类的码段（大类/小类/流水码，含段长与父段）")
    @GetMapping("/kinds/{code}/segments")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<List<CodeRuleService.SegmentRow>> kindSegments(@PathVariable String code) {
        return Result.ok(codeRuleService.listSegments(code));
    }

    @Operation(summary = "某个编码种类下的码值（扁平返回，前端按 valuePath 拼成码值树）")
    @GetMapping("/kinds/{code}/values")
    @PreAuthorize("hasAuthority('system.coding.read')")
    public Result<List<CodeRuleService.ValueRow>> kindValues(@PathVariable String code) {
        return Result.ok(codeRuleService.listValues(code));
    }

    @Operation(summary = "新增码值（种类下；parentPath 为空表示大类）")
    @PostMapping("/kinds/{code}/values")
    @PreAuthorize("hasAuthority('system.coding.manage')")
    @Audit(module = "SYSTEM", action = "CODING_VALUE_CREATE", objectType = "CodeRule")
    public Result<CodeRuleService.ValueRow> createValue(@PathVariable String code,
                                                        @Valid @RequestBody ValueCreateReq req) {
        return Result.ok(codeRuleService.createValue(code, req.codeValue(), req.name(), req.parentPath()));
    }

    @Operation(summary = "改码值（只允许改名与停用/启用，不允许改值）")
    @PutMapping("/kinds/{code}/values")
    @PreAuthorize("hasAuthority('system.coding.manage')")
    @Audit(module = "SYSTEM", action = "CODING_VALUE_UPDATE", objectType = "CodeRule")
    public Result<Void> updateValue(@PathVariable String code, @Valid @RequestBody ValueUpdateReq req) {
        codeRuleService.updateValue(code, req.valuePath(), req.name(), req.deleted());
        return Result.ok();
    }

    /** 新增码值请求；{@code parentPath} 形如 {@code 1001}，为空表示大类。 */
    public record ValueCreateReq(@NotBlank(message = "码值不能为空") String codeValue,
                                 String name, String parentPath) {
    }

    /** 改码值请求：只有名称与停用标记，**没有 codeValue**（结构上禁止改值）。 */
    public record ValueUpdateReq(@NotBlank(message = "码值路径不能为空") String valuePath,
                                 String name, Boolean deleted) {
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
