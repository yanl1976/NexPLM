package com.nexplm.platform.metadata.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.metadata.service.MetaService;
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

@Tag(name = "系统-元数据建模")
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    @Operation(summary = "对象类型列表")
    @GetMapping("/entities")
    public Result<List<MetaDtos.EntitySummary>> listEntities(@RequestParam(required = false) String keyword) {
        return Result.ok(metaService.listEntities(keyword));
    }

    @Operation(summary = "对象类型详情（含字段）")
    @GetMapping("/entities/{code}")
    public Result<MetaDtos.EntityDetail> getEntity(@PathVariable String code) {
        return Result.ok(metaService.getEntity(code));
    }

    @Operation(summary = "新建对象类型")
    @PostMapping("/entities")
    @PreAuthorize("hasAuthority('system.meta.manage')")
    @Audit(module = "META", action = "ENTITY_CREATE", objectType = "MetaEntity")
    public Result<Void> createEntity(@Valid @RequestBody MetaDtos.EntitySaveReq req) {
        metaService.saveEntity(req, true);
        return Result.ok();
    }

    @Operation(summary = "修改对象类型（含字段全量替换）")
    @PutMapping("/entities/{code}")
    @PreAuthorize("hasAuthority('system.meta.manage')")
    @Audit(module = "META", action = "ENTITY_UPDATE", objectType = "MetaEntity")
    public Result<Void> updateEntity(@PathVariable String code, @Valid @RequestBody MetaDtos.EntitySaveReq req) {
        if (!code.equals(req.code())) {
            return Result.fail("META.ENTITY.CODE_MISMATCH", "路径中的类型编码与请求体不一致");
        }
        metaService.saveEntity(req, false);
        return Result.ok();
    }

    @Operation(summary = "发布对象类型")
    @PostMapping("/entities/{code}/publish")
    @PreAuthorize("hasAuthority('system.meta.manage')")
    @Audit(module = "META", action = "ENTITY_PUBLISH", objectType = "MetaEntity")
    public Result<Void> publish(@PathVariable String code) {
        metaService.publish(code);
        return Result.ok();
    }

    @Operation(summary = "删除对象类型（仅草稿）")
    @DeleteMapping("/entities/{code}")
    @PreAuthorize("hasAuthority('system.meta.manage')")
    @Audit(module = "META", action = "ENTITY_DELETE", objectType = "MetaEntity")
    public Result<Void> deleteEntity(@PathVariable String code) {
        metaService.deleteEntity(code);
        return Result.ok();
    }

    @Operation(summary = "对象查看器页签（未配置时返回内置默认页签）")
    @GetMapping("/entities/{code}/tabs")
    public Result<List<MetaDtos.TabDef>> listTabs(@PathVariable String code) {
        return Result.ok(metaService.listTabs(code));
    }

    @Operation(summary = "枚举列表")
    @GetMapping("/enums")
    public Result<List<MetaDtos.EnumDetail>> listEnums() {
        return Result.ok(metaService.listEnums());
    }

    @Operation(summary = "枚举详情")
    @GetMapping("/enums/{code}")
    public Result<MetaDtos.EnumDetail> getEnum(@PathVariable String code) {
        return Result.ok(metaService.getEnum(code));
    }

    @Operation(summary = "保存枚举项（全量替换）")
    @PutMapping("/enums/{code}/items")
    @PreAuthorize("hasAuthority('system.meta.manage')")
    @Audit(module = "META", action = "ENUM_SAVE", objectType = "MetaEnum")
    public Result<Void> saveEnumItems(@PathVariable String code, @RequestBody MetaDtos.EnumItemsReq req) {
        metaService.saveEnumItems(code, req.items());
        return Result.ok();
    }
}
