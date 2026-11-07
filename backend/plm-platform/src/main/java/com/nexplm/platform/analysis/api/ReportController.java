package com.nexplm.platform.analysis.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.analysis.service.ReportExportService;
import com.nexplm.platform.analysis.service.ReportService;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 复刻批次 D｜报表管理：数据集字典 + 模板 CRUD + 运行 + 导出。
 *
 * <p>与 P10-2 自定义看板同一安全边界：模板只引用服务端注册的数据集，接口不接受 SQL 文本。
 * 导出走 POST（参数是结构化 JSON），避免把筛选条件拆成十几个查询串参数。</p>
 */
@Tag(name = "分析-报表管理")
@RestController
@RequestMapping("/api/v1/ana/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService exportService;

    public ReportController(ReportService reportService, ReportExportService exportService) {
        this.reportService = reportService;
        this.exportService = exportService;
    }

    @Operation(summary = "可用数据集字典（白名单：列 + 参数 + 下拉选项）")
    @GetMapping("/datasets")
    @PreAuthorize("hasAuthority('ana.report.read')")
    public Result<List<ReportDtos.DatasetMeta>> datasets() {
        return Result.ok(reportService.datasets());
    }

    @Operation(summary = "报表模板列表（含内置模板）")
    @GetMapping
    @PreAuthorize("hasAuthority('ana.report.read')")
    public Result<PageResult<ReportDtos.TemplateRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dataset,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(reportService.page(CurrentUser.companyId(), keyword, dataset, page, size));
    }

    @Operation(summary = "报表模板详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ana.report.read')")
    public Result<ReportDtos.TemplateRow> detail(@PathVariable Long id) {
        return Result.ok(reportService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建报表模板（编码留空按 RPT-xxxxxx 取号）")
    @PostMapping
    @PreAuthorize("hasAuthority('ana.report.write')")
    @Audit(module = "ANA", action = "REPORT_CREATE", objectType = "RptTemplate")
    public Result<ReportDtos.TemplateRow> create(@Valid @RequestBody ReportDtos.TemplateSaveReq req) {
        return Result.ok(reportService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改报表模板（内置模板拒绝）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ana.report.write')")
    @Audit(module = "ANA", action = "REPORT_UPDATE", objectType = "RptTemplate")
    public Result<ReportDtos.TemplateRow> update(@PathVariable Long id,
                                                 @Valid @RequestBody ReportDtos.TemplateSaveReq req) {
        return Result.ok(reportService.update(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "删除报表模板（软删；内置模板拒绝）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ana.report.write')")
    @Audit(module = "ANA", action = "REPORT_DELETE", objectType = "RptTemplate")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }

    @Operation(summary = "运行报表（参数 + 分页）")
    @PostMapping("/{id}/run")
    @PreAuthorize("hasAuthority('ana.report.read')")
    public Result<ReportDtos.RunResult> run(@PathVariable Long id,
                                            @RequestBody(required = false) ReportDtos.RunReq req) {
        return Result.ok(reportService.run(id, CurrentUser.companyId(), req));
    }

    @Operation(summary = "导出 Excel（不带分页，取全部结果）")
    @PostMapping("/{id}/export.xlsx")
    @PreAuthorize("hasAuthority('ana.report.read')")
    public ResponseEntity<byte[]> exportXlsx(@PathVariable Long id,
                                             @RequestBody(required = false) ReportDtos.RunReq req) {
        ReportDtos.RunResult result = reportService.runForExport(id, CurrentUser.companyId(),
                req == null ? null : req.params());
        byte[] content = exportService.exportXlsx(result);
        return file(content, result.templateName() + "-" + LocalDate.now() + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Operation(summary = "导出 PDF（中文字体内嵌）")
    @PostMapping("/{id}/export.pdf")
    @PreAuthorize("hasAuthority('ana.report.read')")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id,
                                            @RequestBody(required = false) ReportDtos.RunReq req) {
        ReportDtos.RunResult result = reportService.runForExport(id, CurrentUser.companyId(),
                req == null ? null : req.params());
        byte[] content = exportService.exportPdf(result);
        return file(content, result.templateName() + "-" + LocalDate.now() + ".pdf", "application/pdf");
    }

    /** 文件名用 RFC 5987 编码，中文名不依赖容器默认字符集。 */
    private static ResponseEntity<byte[]> file(byte[] content, String fileName, String contentType) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
