package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.capp.service.CappToolingService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
import java.util.Map;

/**
 * 工装管理（台账 / 申请单 / 借用占用），对齐旧系统「工艺管理-工装」（{@code CAPP/Tooling}）。
 *
 * <p>列表返回原始列（snake_case）：这三个视图是台账型表格，前端按列名直取更省事，
 * 也便于后续把 {@code ToolingImpactAnalysis} 的关联查询直接挂上来。</p>
 */
@Tag(name = "工装管理")
@RestController
@RequestMapping("/api/v1/capp")
public class CappToolingController {

    private final CappToolingService service;

    public CappToolingController(CappToolingService service) {
        this.service = service;
    }

    @Operation(summary = "工装台账")
    @GetMapping("/toolings")
    public Result<List<Map<String, Object>>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "toolingType", required = false) String toolingType,
            @RequestParam(value = "status", required = false) String status) {
        return Result.ok(service.list(CurrentUser.companyId(), keyword, toolingType, status));
    }

    @Operation(summary = "新建工装（编码为空按规则 CappTooling 取号）")
    @PostMapping("/toolings")
    public Result<Long> create(@Valid @RequestBody CappToolingService.ToolingReq req) {
        return Result.ok(service.create(CurrentUser.companyId(), CurrentUser.userId(), req));
    }

    @Operation(summary = "修改工装")
    @PutMapping("/toolings/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CappToolingService.ToolingReq req) {
        service.update(id, CurrentUser.companyId(), req);
        return Result.ok();
    }

    @Operation(summary = "删除工装（借用中拒绝）")
    @DeleteMapping("/toolings/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "工装影响分析（被哪些路线的哪道工序引用）")
    @GetMapping("/toolings/{id}/usages")
    public Result<List<Map<String, Object>>> usages(@PathVariable Long id) {
        return Result.ok(service.usages(id, CurrentUser.companyId()));
    }

    @Operation(summary = "工装申请单列表")
    @GetMapping("/tooling-applies")
    public Result<List<Map<String, Object>>> applies(
            @RequestParam(value = "status", required = false) String status) {
        return Result.ok(service.applies(CurrentUser.companyId(), status));
    }

    @Operation(summary = "新建工装申请单（草稿）")
    @PostMapping("/tooling-applies")
    public Result<Long> createApply(@Valid @RequestBody CappToolingService.ApplyReq req) {
        return Result.ok(service.createApply(CurrentUser.companyId(), CurrentUser.userId(), req));
    }

    @Operation(summary = "提交申请单")
    @PostMapping("/tooling-applies/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        service.submitApply(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "审批申请单（批准时自动生成工装台账）")
    @PostMapping("/tooling-applies/{id}/approve")
    public Result<Map<String, Object>> approve(@PathVariable Long id, @Valid @RequestBody ApproveReq req) {
        return Result.ok(service.approveApply(id, CurrentUser.companyId(), CurrentUser.userId(),
                req.approved(), req.comment(), req.toolingCode()));
    }

    @Operation(summary = "借用记录")
    @GetMapping("/tooling-borrows")
    public Result<List<Map<String, Object>>> borrows(
            @RequestParam(value = "toolingId", required = false) Long toolingId) {
        return Result.ok(service.borrows(CurrentUser.companyId(), toolingId));
    }

    @Operation(summary = "借出工装")
    @PostMapping("/tooling-borrows")
    public Result<Long> borrow(@Valid @RequestBody BorrowReq req) {
        return Result.ok(service.borrow(CurrentUser.companyId(), CurrentUser.userId(),
                req.toolingId(), req.routingId(), req.remark()));
    }

    @Operation(summary = "归还工装")
    @PostMapping("/tooling-borrows/{id}/return")
    public Result<Void> giveBack(@PathVariable Long id) {
        service.giveBack(id, CurrentUser.companyId());
        return Result.ok();
    }

    public record ApproveReq(boolean approved, @Size(max = 500) String comment, String toolingCode) {
    }

    public record BorrowReq(Long toolingId, Long routingId, @Size(max = 500) String remark) {
    }
}
