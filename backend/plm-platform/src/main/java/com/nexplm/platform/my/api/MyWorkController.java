package com.nexplm.platform.my.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.my.service.MyWorkService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 个人工作区接口，对齐旧系统「000 个人工作区 / 001 常用功能」：
 * 我的检出（{@code MyCheckOut}）、收藏夹（{@code Favorite}）、最近访问（{@code AccessRecord}）、
 * 站内消息（{@code InternalMail}）、高级查询（{@code QueryMachine}）。
 *
 * <p><b>为什么不加 {@code @PreAuthorize}</b>：这些都是「我的」数据，范围由
 * {@code user_id} 自身限定；旧系统同样对所有登录用户开放。再加功能权限点会造出
 * 「能登录却看不到自己收藏」的怪现象（决策见 docs/11 批次 A）。</p>
 */
@Tag(name = "个人工作区")
@RestController
@RequestMapping("/api/v1/my")
public class MyWorkController {

    private final MyWorkService myWorkService;

    public MyWorkController(MyWorkService myWorkService) {
        this.myWorkService = myWorkService;
    }

    @Operation(summary = "我的检出（对象类型/编码/检出时间，按检出时间倒序）")
    @GetMapping("/checkouts")
    public Result<List<MyDtos.CheckoutRow>> checkouts() {
        return Result.ok(myWorkService.listCheckouts(CurrentUser.userId()));
    }

    @Operation(summary = "撤销我的检出（他人检出需走对象强制解锁）")
    @PostMapping("/checkouts/{objectId}/cancel")
    public Result<Void> cancelCheckout(@PathVariable Long objectId) {
        myWorkService.cancelCheckout(objectId, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "收藏列表（可按对象类型过滤）")
    @GetMapping("/favorites")
    public Result<List<MyDtos.FavoriteRow>> favorites(
            @RequestParam(value = "objectType", required = false) String objectType) {
        return Result.ok(myWorkService.listFavorites(CurrentUser.userId(), objectType));
    }

    @Operation(summary = "收藏（重复收藏幂等成功）")
    @PostMapping("/favorites")
    public Result<Long> addFavorite(@Valid @RequestBody MyDtos.FavoriteReq req) {
        return Result.ok(myWorkService.addFavorite(
                CurrentUser.userId(), CurrentUser.companyId(), req));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorites/{id}")
    public Result<Void> removeFavorite(@PathVariable Long id) {
        myWorkService.removeFavorite(CurrentUser.userId(), id);
        return Result.ok();
    }

    @Operation(summary = "最近访问（30 分钟内重复访问只更新时间）")
    @GetMapping("/recent")
    public Result<List<MyDtos.RecentRow>> recent(
            @RequestParam(value = "objectType", required = false) String objectType,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return Result.ok(myWorkService.listRecent(CurrentUser.userId(), objectType, limit));
    }

    @Operation(summary = "记一条访问记录（详情页打开时调用）")
    @PostMapping("/recent")
    public Result<Void> recordAccess(@Valid @RequestBody MyDtos.AccessReq req) {
        myWorkService.recordAccess(CurrentUser.userId(), CurrentUser.companyId(), req);
        return Result.ok();
    }

    @Operation(summary = "站内消息收件箱")
    @GetMapping("/messages")
    public Result<List<MyDtos.MessageRow>> messages(
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return Result.ok(myWorkService.inbox(CurrentUser.companyId(), unreadOnly, limit));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/messages/unread-count")
    public Result<Map<String, Integer>> unreadCount() {
        return Result.ok(Map.of("unread", myWorkService.unreadCount(CurrentUser.companyId())));
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/messages/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        myWorkService.markRead(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "高级查询（跨图文档/零部件/变更单）")
    @GetMapping("/search")
    public Result<List<MyDtos.SearchRow>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "objectType", required = false) String objectType,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return Result.ok(myWorkService.search(CurrentUser.companyId(), keyword, objectType, limit));
    }

    @Operation(summary = "对象类型字典（含中文名）")
    @GetMapping("/object-types")
    public Result<Map<String, Object>> objectTypes() {
        return Result.ok(Map.of(
                "types", myWorkService.objectTypes(CurrentUser.companyId()),
                "labels", myWorkService.typeLabels()));
    }
}
