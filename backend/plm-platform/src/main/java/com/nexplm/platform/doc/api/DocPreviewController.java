package com.nexplm.platform.doc.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.doc.service.PreviewService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文档预览接口：查询状态、触发生成/重试。
 */
@Tag(name = "PDM-文档预览")
@RestController
@RequestMapping("/api/v1/docs/{id}/preview")
public class DocPreviewController {

    private final PreviewService previewService;

    public DocPreviewController(PreviewService previewService) {
        this.previewService = previewService;
    }

    @Operation(summary = "预览状态（NONE/PENDING/READY/FAILED/SKIPPED）")
    @GetMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<DocPreviewDtos.PreviewStatus> status(@PathVariable Long id) {
        return Result.ok(previewService.status(id));
    }

    @Operation(summary = "生成或重试预览（产物为 PDF/PNG，存文件仓 VIEW 角色）")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    @Audit(module = "PDM", action = "DOC_PREVIEW_GENERATE", objectType = "PdmDoc")
    public Result<DocPreviewDtos.PreviewStatus> generate(@PathVariable Long id,
                                                         @RequestParam(value = "fileId", required = false) Long fileId) {
        return Result.ok(previewService.generate(id, fileId, CurrentUser.userId()));
    }
}
