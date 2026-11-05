package com.nexplm.platform.item.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.item.service.ItemService;
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

/**
 * 零部件接口：列表、详情、增删改（8 种类型共用）。
 */
@Tag(name = "PSE-零部件")
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "零部件列表")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<PageResult<ItemDtos.ItemSummary>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "sourceType", required = false) String sourceType) {
        return Result.ok(itemService.page(page, size, keyword, itemType, sourceType));
    }

    @Operation(summary = "零部件详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<ItemDtos.ItemDetail> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id));
    }

    @Operation(summary = "新建零部件（编码留空自动取号 PN-000001）")
    @PostMapping
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_CREATE", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> create(@Valid @RequestBody ItemDtos.ItemSaveReq req) {
        return Result.ok(itemService.create(req, CurrentUser.userId()));
    }

    @Operation(summary = "修改零部件")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_UPDATE", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> update(@PathVariable Long id,
                                              @Valid @RequestBody ItemDtos.ItemSaveReq req) {
        return Result.ok(itemService.update(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除零部件（存在 BOM 引用时拒绝）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.item.delete')")
    @Audit(module = "PSE", action = "ITEM_DELETE", objectType = "PdmItem")
    public Result<Void> delete(@PathVariable Long id) {
        itemService.delete(id, CurrentUser.userId());
        return Result.ok();
    }
}
