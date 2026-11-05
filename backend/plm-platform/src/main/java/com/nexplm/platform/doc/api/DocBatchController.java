package com.nexplm.platform.doc.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.service.DocBatchService;
import com.nexplm.platform.doc.service.DocImportService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 图文档批量操作：打包下载、批量授权。
 */
@Tag(name = "PDM-批量操作")
@RestController
@RequestMapping("/api/v1/docs/batch")
public class DocBatchController {

    private static final long MAX_IMPORT_BYTES = 2L * 1024 * 1024;

    private final DocBatchService docBatchService;
    private final DocImportService docImportService;

    public DocBatchController(DocBatchService docBatchService, DocImportService docImportService) {
        this.docBatchService = docBatchService;
        this.docImportService = docImportService;
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

    @Operation(summary = "批量导入 CSV（逐行返回结果，单行失败不影响其它行）")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_BATCH_IMPORT", objectType = "PdmDoc", recordArgs = false)
    public Result<DocImportService.ImportResult> importCsv(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请选择要导入的 CSV 文件");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "CSV 文件不能超过 2MB");
        }
        return Result.ok(docImportService.importCsv(file.getInputStream(), CurrentUser.userId()));
    }

    @Operation(summary = "下载导入模板（CSV，UTF-8）")
    @GetMapping(value = "/import/template", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] content = docImportService.template().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"doc-import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }
}
