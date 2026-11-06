package com.nexplm.platform.integration.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.integration.service.WebhookService;
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

import java.util.Map;

/** P9-4 Webhook：事件订阅、投递记录、重试/重放。 */
@Tag(name = "集成-Webhook")
@RestController
@RequestMapping("/api/v1/itg/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Operation(summary = "支持的事件类型字典")
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('itg.webhook.read')")
    public Result<Map<String, String>> events() {
        return Result.ok(WebhookService.EVENT_LABELS);
    }

    @Operation(summary = "Webhook 订阅列表")
    @GetMapping
    @PreAuthorize("hasAuthority('itg.webhook.read')")
    public Result<PageResult<WebhookDtos.WebhookRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(webhookService.webhookPage(CurrentUser.companyId(), keyword, enabled,
                page, size));
    }

    @Operation(summary = "Webhook 订阅详情（密钥不回传）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('itg.webhook.read')")
    public Result<WebhookDtos.WebhookDetail> detail(@PathVariable Long id) {
        return Result.ok(webhookService.webhookDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建订阅（密钥留空则自动生成，明文只返回一次）")
    @PostMapping
    @PreAuthorize("hasAuthority('itg.webhook.write')")
    @Audit(module = "ITG", action = "WEBHOOK_CREATE", objectType = "ItgWebhook")
    public Result<WebhookDtos.WebhookSecret> create(
            @Valid @RequestBody WebhookDtos.WebhookSaveReq req) {
        return Result.ok(webhookService.createWebhook(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改订阅（secret 留空表示保持不变）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('itg.webhook.write')")
    @Audit(module = "ITG", action = "WEBHOOK_UPDATE", objectType = "ItgWebhook")
    public Result<WebhookDtos.WebhookDetail> update(@PathVariable Long id,
                                                    @Valid @RequestBody WebhookDtos.WebhookSaveReq req) {
        return Result.ok(webhookService.updateWebhook(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除订阅（软删，历史投递保留）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('itg.webhook.write')")
    @Audit(module = "ITG", action = "WEBHOOK_DELETE", objectType = "ItgWebhook")
    public Result<Void> delete(@PathVariable Long id) {
        webhookService.deleteWebhook(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }

    @Operation(summary = "轮换签名密钥（明文只返回一次，旧密钥立即失效）")
    @PostMapping("/{id}/rotate-secret")
    @PreAuthorize("hasAuthority('itg.webhook.write')")
    @Audit(module = "ITG", action = "WEBHOOK_ROTATE_SECRET", objectType = "ItgWebhook")
    public Result<WebhookDtos.WebhookSecret> rotateSecret(@PathVariable Long id) {
        return Result.ok(webhookService.rotateSecret(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "发送测试投递（验证地址与签名）")
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('itg.webhook.write')")
    @Audit(module = "ITG", action = "WEBHOOK_TEST", objectType = "ItgWebhook")
    public Result<WebhookDtos.DeliveryDetail> test(@PathVariable Long id) {
        return Result.ok(webhookService.testDelivery(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "投递记录列表")
    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasAuthority('itg.webhook.read')")
    public Result<PageResult<WebhookDtos.DeliveryRow>> deliveries(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(webhookService.deliveryPage(CurrentUser.companyId(), id, status, page, size));
    }

    @Operation(summary = "投递详情（含原始报文快照与签名）")
    @GetMapping("/deliveries/{id}")
    @PreAuthorize("hasAuthority('itg.webhook.read')")
    public Result<WebhookDtos.DeliveryDetail> delivery(@PathVariable Long id) {
        return Result.ok(webhookService.deliveryDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "重放投递（失败/死信，使用原报文快照）")
    @PostMapping("/deliveries/{id}/replay")
    @PreAuthorize("hasAuthority('itg.webhook.write')")
    @Audit(module = "ITG", action = "WEBHOOK_REPLAY", objectType = "ItgWebhookDelivery")
    public Result<WebhookDtos.DeliveryDetail> replay(@PathVariable Long id) {
        return Result.ok(webhookService.replayDelivery(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }
}
