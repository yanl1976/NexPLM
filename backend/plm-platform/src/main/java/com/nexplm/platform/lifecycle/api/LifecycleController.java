package com.nexplm.platform.lifecycle.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.lifecycle.service.LifecycleService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统-生命周期")
@RestController
@RequestMapping("/api/v1")
public class LifecycleController {

    private final LifecycleService lifecycleService;

    public LifecycleController(LifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    public record TransitionReq(@NotBlank(message = "操作不能为空") String action,
                                @Size(max = 500) String reason) {
    }

    public record DefinitionReq(@NotBlank(message = "定义不能为空") String definitionJson) {
    }

    public record TransitionResp(Long objectId, String lifecycleState) {
    }

    @Operation(summary = "生命周期定义列表")
    @GetMapping("/meta/lifecycles")
    @PreAuthorize("hasAuthority('system.lifecycle.manage')")
    public Result<List<LifecycleService.Definition>> list() {
        return Result.ok(lifecycleService.listDefinitions());
    }

    @Operation(summary = "生命周期定义详情")
    @GetMapping("/meta/lifecycles/{code}")
    @PreAuthorize("hasAuthority('system.lifecycle.manage')")
    public Result<LifecycleService.Definition> detail(@PathVariable String code) {
        return Result.ok(lifecycleService.definition(code));
    }

    @Operation(summary = "保存生命周期定义（设计器）")
    @org.springframework.web.bind.annotation.PutMapping("/meta/lifecycles/{code}")
    @PreAuthorize("hasAuthority('system.lifecycle.manage')")
    @Audit(module = "META", action = "LIFECYCLE_SAVE", objectType = "MetaLifecycle")
    public Result<Void> saveDefinition(@PathVariable String code, @Valid @RequestBody DefinitionReq req) {
        lifecycleService.updateDefinition(code, req.definitionJson());
        return Result.ok();
    }

    @Operation(summary = "对象状态迁移（生命周期唯一写入口）")
    @PostMapping("/objects/{id}/transitions")
    @Audit(module = "OBJ", action = "STATE_TRANSITION", objectType = "PlmObject")
    public Result<TransitionResp> transition(@PathVariable Long id,
                                             @Valid @RequestBody TransitionReq req) {
        var object = lifecycleService.transition(id, req.action(), req.reason(), CurrentUser.userId());
        return Result.ok(new TransitionResp(object.getId(), object.getLifecycleState()));
    }
}
