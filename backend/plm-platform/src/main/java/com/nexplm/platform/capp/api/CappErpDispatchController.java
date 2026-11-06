package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappErpDispatchService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工艺下发【预留】（P6-9）。
 *
 * <p>只提供「生成载荷 / 查询记录 / 取原始 JSON / 回执」四类端点，真正发送由 P9 的 ERP 适配器完成。
 * 权限复用 {@code capp.routing.read/write}。</p>
 */
@Tag(name = "工艺-ERP下发（预留）")
@RestController
@RequestMapping("/api/v1")
public class CappErpDispatchController {

    private final CappErpDispatchService dispatchService;

    public CappErpDispatchController(CappErpDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Operation(summary = "生成下发载荷（仅已发布路线；载荷快照落库，不真正发送）")
    @PostMapping("/capp/erp-dispatches")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ERP_DISPATCH_CREATE", objectType = "CappErpDispatch")
    public Result<CappErpDtos.DispatchDetail> create(
            @Valid @RequestBody CappErpDtos.DispatchReq req) {
        return Result.ok(dispatchService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "下发记录列表（按路线/状态筛选）")
    @GetMapping("/capp/erp-dispatches")
    @PreAuthorize("hasAuthority('capp.routing.read')")
    public Result<List<CappErpDtos.DispatchRow>> list(@RequestParam(required = false) Long routingId,
                                                      @RequestParam(required = false) String status) {
        return Result.ok(dispatchService.list(CurrentUser.companyId(), routingId, status));
    }

    @Operation(summary = "下发记录详情（含载荷快照）")
    @GetMapping("/capp/erp-dispatches/{id}")
    @PreAuthorize("hasAuthority('capp.routing.read')")
    public Result<CappErpDtos.DispatchDetail> detail(@PathVariable Long id) {
        return Result.ok(dispatchService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "取原始载荷 JSON（P9 适配器取数入口，可直接下载交给 ERP）")
    @GetMapping(value = "/capp/erp-dispatches/{id}/payload", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('capp.routing.read')")
    public ResponseEntity<String> payload(@PathVariable Long id) {
        CappErpDtos.DispatchDetail detail = dispatchService.detail(id, CurrentUser.companyId());
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"dispatch-" + detail.code() + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dispatchService.payload(id, CurrentUser.companyId()));
    }

    @Operation(summary = "回执（P9 适配器发送结果回写；本阶段可人工标记试跑）")
    @PostMapping("/capp/erp-dispatches/{id}/callback")
    @PreAuthorize("hasAuthority('capp.routing.write')")
    @Audit(module = "CAPP", action = "ERP_DISPATCH_CALLBACK", objectType = "CappErpDispatch")
    public Result<CappErpDtos.DispatchDetail> callback(
            @PathVariable Long id, @RequestBody CappErpDtos.DispatchCallbackReq req) {
        return Result.ok(dispatchService.callback(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }
}
