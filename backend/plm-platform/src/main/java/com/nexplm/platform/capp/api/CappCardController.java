package com.nexplm.platform.capp.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappCardExcelService;
import com.nexplm.platform.capp.service.CappCardPdfService;
import com.nexplm.platform.capp.service.CappCardService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** 工艺卡片（P6 任务 4）：模板版本化 + 卡片生成 + Excel 导出。 */
@Tag(name = "工艺-工艺卡片")
@RestController
@RequestMapping("/api/v1")
public class CappCardController {

    private final CappCardService cardService;
    private final CappCardExcelService excelService;
    private final CappCardPdfService pdfService;

    public CappCardController(CappCardService cardService, CappCardExcelService excelService,
                              CappCardPdfService pdfService) {
        this.cardService = cardService;
        this.excelService = excelService;
        this.pdfService = pdfService;
    }

    @Operation(summary = "可用变量字典（明细列 / 表头字段 / 卡片类型），供模板编辑器选择")
    @GetMapping("/capp/card-fields")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<Map<String, Object>> fields() {
        return Result.ok(cardService.fields());
    }

    // ------------------------------------------------------------------ 模板

    @Operation(summary = "卡片模板列表（同码多版本，按版本倒序）")
    @GetMapping("/capp/card-templates")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<PageResult<CappCardDtos.TemplateRow>> templateList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cardType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(cardService.templateList(CurrentUser.companyId(), keyword, cardType, status,
                page, size));
    }

    @Operation(summary = "卡片模板详情（含版式）")
    @GetMapping("/capp/card-templates/{id}")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<CappCardDtos.TemplateDetail> template(@PathVariable Long id) {
        return Result.ok(cardService.template(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建卡片模板（草稿 v1，自动生成 CT-xxxxxx）")
    @PostMapping("/capp/card-templates")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_TEMPLATE_CREATE", objectType = "CappCardTemplate")
    public Result<CappCardDtos.TemplateDetail> createTemplate(
            @Valid @RequestBody CappCardDtos.TemplateSaveReq req) {
        return Result.ok(cardService.createTemplate(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改卡片模板（仅草稿）")
    @PutMapping("/capp/card-templates/{id}")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_TEMPLATE_UPDATE", objectType = "CappCardTemplate")
    public Result<CappCardDtos.TemplateDetail> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody CappCardDtos.TemplateSaveReq req) {
        return Result.ok(cardService.updateTemplate(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "另存新版本（同编码，版本号 +1，草稿）")
    @PostMapping("/capp/card-templates/{id}/new-version")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_TEMPLATE_NEW_VERSION", objectType = "CappCardTemplate")
    public Result<CappCardDtos.TemplateDetail> newVersion(@PathVariable Long id) {
        return Result.ok(cardService.newTemplateVersion(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "发布卡片模板（同族其它已发布版本自动归档）")
    @PostMapping("/capp/card-templates/{id}/release")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_TEMPLATE_RELEASE", objectType = "CappCardTemplate")
    public Result<CappCardDtos.TemplateDetail> releaseTemplate(@PathVariable Long id) {
        return Result.ok(cardService.releaseTemplate(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除卡片模板（仅草稿且无卡片引用）")
    @DeleteMapping("/capp/card-templates/{id}")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_TEMPLATE_DELETE", objectType = "CappCardTemplate")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        cardService.deleteTemplate(id, CurrentUser.companyId());
        return Result.ok();
    }

    // ------------------------------------------------------------------ 卡片

    @Operation(summary = "工艺卡片列表（按路线/模板/关键字筛选）")
    @GetMapping("/capp/cards")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<PageResult<CappCardDtos.CardRow>> cardList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long routingId,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long objectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(cardService.cardList(CurrentUser.companyId(), keyword, routingId, templateId,
                objectId, page, size));
    }

    @Operation(summary = "工艺卡片详情（版式快照 + 工序行快照）")
    @GetMapping("/capp/cards/{id}")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<CappCardDtos.CardDetail> card(@PathVariable Long id) {
        return Result.ok(cardService.card(id, CurrentUser.companyId()));
    }

    @Operation(summary = "生成工艺卡片（已发布模板 + 工艺路线，自动取号 CC-xxxxxx）")
    @PostMapping("/capp/cards")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_CREATE", objectType = "CappCard")
    public Result<CappCardDtos.CardDetail> createCard(
            @Valid @RequestBody CappCardDtos.CardSaveReq req) {
        return Result.ok(cardService.createCard(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "重新生成行快照（路线工序改动后刷新，版式版本不变）")
    @PostMapping("/capp/cards/{id}/regenerate")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_REGENERATE", objectType = "CappCard")
    public Result<CappCardDtos.CardDetail> regenerate(@PathVariable Long id) {
        return Result.ok(cardService.regenerateCard(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除工艺卡片（软删除）")
    @DeleteMapping("/capp/cards/{id}")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "CARD_DELETE", objectType = "CappCard")
    public Result<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "导出工艺卡片（xlsx，按卡片自带的版式快照渲染）")
    @GetMapping("/capp/cards/{id}/export")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        CappCardDtos.CardDetail card = cardService.card(id, CurrentUser.companyId());
        byte[] content = excelService.export(card);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"card-" + card.code() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Operation(summary = "导出工艺卡片 PDF（嵌入中文字体；字体路径 nexplm.capp.pdf.font-path）")
    @GetMapping("/capp/cards/{id}/export.pdf")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        CappCardDtos.CardDetail card = cardService.card(id, CurrentUser.companyId());
        byte[] content = pdfService.export(card);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"card-" + card.code() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
