package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.capp.service.CappPositionService;
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

import java.util.List;

/**
 * 工厂结构（工位）与消耗定义。对齐旧系统「工艺管理-工位」（{@code PositionManager}）
 * 与「业务管理-消耗定义」（{@code ConsumptionStation}）。
 */
@Tag(name = "工厂结构与消耗定义")
@RestController
@RequestMapping("/api/v1/capp")
public class CappPositionController {

    private final CappPositionService service;

    public CappPositionController(CappPositionService service) {
        this.service = service;
    }

    @Operation(summary = "工位树（工厂/车间/产线/工位/工作地）")
    @GetMapping("/positions")
    public Result<List<CappPositionDtos.PositionNode>> tree() {
        return Result.ok(service.tree(CurrentUser.companyId()));
    }

    @Operation(summary = "新建工位（编码为空时按规则 CappPosition 取号）")
    @PostMapping("/positions")
    public Result<CappPositionDtos.PositionNode> create(@Valid @RequestBody CappPositionDtos.PositionReq req) {
        return Result.ok(service.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改工位")
    @PutMapping("/positions/{id}")
    public Result<CappPositionDtos.PositionNode> update(@PathVariable Long id,
                                                       @Valid @RequestBody CappPositionDtos.PositionReq req) {
        return Result.ok(service.update(id, req, CurrentUser.companyId()));
    }

    @Operation(summary = "删除工位（有下级或消耗定义时拒绝）")
    @DeleteMapping("/positions/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "消耗定义列表（可按工位过滤）")
    @GetMapping("/consumptions")
    public Result<List<CappPositionDtos.ConsumptionRow>> consumptions(
            @RequestParam(value = "positionId", required = false) Long positionId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(service.consumptions(CurrentUser.companyId(), positionId, keyword));
    }

    @Operation(summary = "新建消耗定义")
    @PostMapping("/consumptions")
    public Result<Long> createConsumption(@Valid @RequestBody CappPositionDtos.ConsumptionReq req) {
        return Result.ok(service.createConsumption(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改消耗定义")
    @PutMapping("/consumptions/{id}")
    public Result<Void> updateConsumption(@PathVariable Long id,
                                          @Valid @RequestBody CappPositionDtos.ConsumptionReq req) {
        service.updateConsumption(id, req, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "删除消耗定义")
    @DeleteMapping("/consumptions/{id}")
    public Result<Void> deleteConsumption(@PathVariable Long id) {
        service.deleteConsumption(id, CurrentUser.companyId());
        return Result.ok();
    }
}
