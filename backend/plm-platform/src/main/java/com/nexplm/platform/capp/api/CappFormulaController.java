package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.capp.service.CappFormulaService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 材料定额（公式库）。对齐旧系统「业务管理-材料定额」（{@code Formula}）：
 * 维护表达式与参数，供材料用量/工时计算调用。
 */
@Tag(name = "材料定额")
@RestController
@RequestMapping("/api/v1/capp/formulas")
public class CappFormulaController {

    private final CappFormulaService service;

    public CappFormulaController(CappFormulaService service) {
        this.service = service;
    }

    @Operation(summary = "公式列表")
    @GetMapping
    public Result<List<CappFormulaService.Formula>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "formulaType", required = false) String formulaType) {
        return Result.ok(service.list(CurrentUser.companyId(), keyword, formulaType));
    }

    @Operation(summary = "公式明细")
    @GetMapping("/{id}")
    public Result<CappFormulaService.Formula> get(@PathVariable Long id) {
        return Result.ok(service.get(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建公式（编码为空时按规则 CappFormula 取号）")
    @PostMapping
    public Result<CappFormulaService.Formula> create(
            @Valid @RequestBody CappFormulaService.FormulaReq req) {
        return Result.ok(service.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改公式（保存前校验表达式语法）")
    @PutMapping("/{id}")
    public Result<CappFormulaService.Formula> update(
            @PathVariable Long id, @Valid @RequestBody CappFormulaService.FormulaReq req) {
        return Result.ok(service.update(id, req, CurrentUser.companyId()));
    }

    @Operation(summary = "删除公式（软删）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "试算（缺参报错，不默认成 0）")
    @PostMapping("/{id}/evaluate")
    public Result<Map<String, Object>> evaluate(@PathVariable Long id,
                                                @RequestBody(required = false) Map<String, BigDecimal> input) {
        BigDecimal value = service.evaluate(id, CurrentUser.companyId(), input);
        return Result.ok(Map.of("value", value, "display", value.toPlainString()));
    }
}
