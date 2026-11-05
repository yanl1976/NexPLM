package com.nexplm.platform.doc.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.doc.service.DocBatchService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图文档批量操作：打包下载、批量授权。
 */
@Tag(name = "PDM-批量操作")
@RestController
@RequestMapping("/api/v1/docs/batch")
public class DocBatchController {

    private final DocBatchService docBatchService;

    public DocBatchController(DocBatchService docBatchService) {
        this.docBatchService = docBatchService;
    }

    @Operation(summary = "批量下载（主文件打包为 zip）")
    @PostMapping(value = "/download", produces = "application/zip")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public ResponseEntity<byte[]> download(@Valid @RequestBody DocBatchDtos.BatchDownloadReq req) {
        byte[] zip = docBatchService.exportZip(req.ids());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documents.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zip.length)
                .body(zip);
    }

    @Operation(summary = "批量授权（逐条返回结果）")
    @PostMapping("/grant")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_BATCH_GRANT", objectType = "PdmDoc")
    public Result<List<DocBatchDtos.BatchGrantResult>> grant(@Valid @RequestBody DocBatchDtos.BatchGrantReq req) {
        return Result.ok(docBatchService.grant(req, CurrentUser.userId()));
    }
}
