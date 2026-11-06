package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappTimeService;
import com.nexplm.platform.security.CurrentUser;
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

/** 工时定额（P6 任务 6）：规则维护 + 按路线试算。 */
@Tag(name = "工艺-工时定额")
@RestController
@RequestMapping("/api/v1")
public class CappTimeController {

    private final CappTimeService timeService;

    public CappTimeController(CappTimeService timeService) {
        this.timeService = timeService;
    }

    @Operation(summary = "工时定额规则列表（可按关键字/启用状态筛选）")
    @GetMapping("/capp/time-rules")
    @PreAuthorize("hasAuthority('capp.time.read')")
    public Result<List<CappTimeDtos.TimeRuleRow>> list(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Boolean enabled) {
        return Result.ok(timeService.listRules(CurrentUser.companyId(), keyword, enabled));
    }

    @Operation(summary = "新建工时定额规则（自动生成 TR-xxxxxx）")
    @PostMapping("/capp/time-rules")
    @PreAuthorize("hasAuthority('capp.time.write')")
    @Audit(module = "CAPP", action = "TIME_RULE_CREATE", objectType = "CappTimeRule")
    public Result<CappTimeDtos.TimeRuleRow> create(@Valid @RequestBody CappTimeDtos.TimeRuleSaveReq req) {
        return Result.ok(timeService.createRule(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改工时定额规则")
    @PutMapping("/capp/time-rules/{id}")
    @PreAuthorize("hasAuthority('capp.time.write')")
    @Audit(module = "CAPP", action = "TIME_RULE_UPDATE", objectType = "CappTimeRule")
    public Result<CappTimeDtos.TimeRuleRow> update(@PathVariable Long id,
                                                   @Valid @RequestBody CappTimeDtos.TimeRuleSaveReq req) {
        return Result.ok(timeService.updateRule(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除工时定额规则（软删除）")
    @DeleteMapping("/capp/time-rules/{id}")
    @PreAuthorize("hasAuthority('capp.time.write')")
    @Audit(module = "CAPP", action = "TIME_RULE_DELETE", objectType = "CappTimeRule")
    public Result<Void> delete(@PathVariable Long id) {
        timeService.deleteRule(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "按工艺路线试算工时定额（只读，不落库；规则改动后重查即重算）")
    @GetMapping("/capp/time-calc")
    @PreAuthorize("hasAuthority('capp.time.read')")
    public Result<CappTimeDtos.TimeCalcResult> calc(@RequestParam Long routingId,
                                                    @RequestParam(required = false) Integer batchQty) {
        return Result.ok(timeService.calc(routingId, batchQty, CurrentUser.companyId()));
    }
}
