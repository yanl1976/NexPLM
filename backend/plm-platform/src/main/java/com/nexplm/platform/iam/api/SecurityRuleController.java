package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.PermissionRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统-规则权限")
@RestController
@RequestMapping("/api/v1/security/rules")
@PreAuthorize("hasAuthority('system.rule.manage')")
public class SecurityRuleController {

    private final PermissionRuleService ruleService;

    public SecurityRuleController(PermissionRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @Operation(summary = "规则列表")
    @GetMapping
    public Result<List<SecurityDtos.RuleRow>> list(@RequestParam(required = false) String objectType) {
        return Result.ok(ruleService.list(objectType));
    }

    @Operation(summary = "新建规则")
    @PostMapping
    @Audit(module = "SECURITY", action = "RULE_CREATE", objectType = "PermissionRule")
    public Result<Long> create(@Valid @RequestBody SecurityDtos.RuleSaveReq req) {
        return Result.ok(ruleService.save(req, true));
    }

    @Operation(summary = "修改规则")
    @PutMapping("/{code}")
    @Audit(module = "SECURITY", action = "RULE_UPDATE", objectType = "PermissionRule")
    public Result<Void> update(@PathVariable String code, @Valid @RequestBody SecurityDtos.RuleSaveReq req) {
        if (!code.equals(req.code())) {
            return Result.fail("SECURITY.RULE.CODE_MISMATCH", "路径中的规则编码与请求体不一致");
        }
        ruleService.save(req, false);
        return Result.ok();
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    @Audit(module = "SECURITY", action = "RULE_DELETE", objectType = "PermissionRule")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "规则试算（可视化调试）")
    @PostMapping("/test")
    public Result<SecurityDtos.RuleTestResp> test(@RequestBody SecurityDtos.RuleTestReq req) {
        return Result.ok(ruleService.test(req));
    }
}
