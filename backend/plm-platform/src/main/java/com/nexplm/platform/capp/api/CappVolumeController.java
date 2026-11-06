package com.nexplm.platform.capp.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.capp.service.CappCardPdfService;
import com.nexplm.platform.capp.service.CappCardService;
import com.nexplm.platform.capp.service.CappVolumeService;
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

import java.util.ArrayList;
import java.util.List;

/** 工艺成册与齐套性检查（P6-5）。 */
@Tag(name = "工艺-成册与齐套")
@RestController
@RequestMapping("/api/v1")
public class CappVolumeController {

    private final CappVolumeService volumeService;
    private final CappCardService cardService;
    private final CappCardPdfService pdfService;

    public CappVolumeController(CappVolumeService volumeService, CappCardService cardService,
                                CappCardPdfService pdfService) {
        this.volumeService = volumeService;
        this.cardService = cardService;
        this.pdfService = pdfService;
    }

    @Operation(summary = "齐套性检查（零件维度的路线/工序/工步/卡片完整性）")
    @GetMapping("/capp/completeness")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<CappVolumeDtos.CompletenessResult> completeness(@RequestParam Long objectId) {
        return Result.ok(volumeService.completeness(objectId, CurrentUser.companyId()));
    }

    @Operation(summary = "工艺成册列表")
    @GetMapping("/capp/volumes")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<List<CappVolumeDtos.VolumeRow>> list(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long objectId) {
        return Result.ok(volumeService.list(CurrentUser.companyId(), keyword, objectId));
    }

    @Operation(summary = "工艺成册详情（含页序卡片清单）")
    @GetMapping("/capp/volumes/{id}")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public Result<CappVolumeDtos.VolumeDetail> detail(@PathVariable Long id) {
        return Result.ok(volumeService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建工艺成册（cardIds 的顺序即页序，自动生成 CV-xxxxxx）")
    @PostMapping("/capp/volumes")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "VOLUME_CREATE", objectType = "CappCardVolume")
    public Result<CappVolumeDtos.VolumeDetail> create(
            @Valid @RequestBody CappVolumeDtos.VolumeSaveReq req) {
        return Result.ok(volumeService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改工艺成册（仅草稿；整表替换页序）")
    @PutMapping("/capp/volumes/{id}")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "VOLUME_UPDATE", objectType = "CappCardVolume")
    public Result<CappVolumeDtos.VolumeDetail> update(
            @PathVariable Long id, @Valid @RequestBody CappVolumeDtos.VolumeSaveReq req) {
        return Result.ok(volumeService.update(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "发布工艺成册（仅草稿，且册内至少一张卡片）")
    @PostMapping("/capp/volumes/{id}/release")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "VOLUME_RELEASE", objectType = "CappCardVolume")
    public Result<CappVolumeDtos.VolumeDetail> release(@PathVariable Long id) {
        return Result.ok(volumeService.release(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除工艺成册（仅草稿）")
    @DeleteMapping("/capp/volumes/{id}")
    @PreAuthorize("hasAuthority('capp.card.write')")
    @Audit(module = "CAPP", action = "VOLUME_DELETE", objectType = "CappCardVolume")
    public Result<Void> delete(@PathVariable Long id) {
        volumeService.delete(id, CurrentUser.companyId());
        return Result.ok();
    }

    @Operation(summary = "导出成册 PDF（封页 + 目录 + 每张卡分页）")
    @GetMapping("/capp/volumes/{id}/export.pdf")
    @PreAuthorize("hasAuthority('capp.card.read')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        CappVolumeDtos.VolumeDetail volume = volumeService.detail(id, CurrentUser.companyId());
        List<CappCardDtos.CardDetail> cards = new ArrayList<>();
        for (CappVolumeDtos.VolumeCardRow row : volume.cards()) {
            cards.add(cardService.card(row.cardId(), CurrentUser.companyId()));
        }
        String objectLabel = (volume.objectCode() == null ? "" : volume.objectCode() + " ")
                + (volume.objectName() == null ? "" : volume.objectName());
        byte[] content = pdfService.exportVolume(volume.name(), objectLabel.trim(), cards);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"volume-" + volume.code() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }
}
