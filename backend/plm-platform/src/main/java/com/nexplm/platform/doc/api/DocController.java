package com.nexplm.platform.doc.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.service.DocService;
import com.nexplm.platform.doc.service.DocViewService;
import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 图文档接口：文档 CRUD、文件绑定、检入检出、版本历史。
 */
@Tag(name = "PDM-图文档")
@RestController
@RequestMapping("/api/v1/docs")
public class DocController {

    private final DocService docService;
    private final DocViewService docViewService;

    public DocController(DocService docService, DocViewService docViewService) {
        this.docService = docService;
        this.docViewService = docViewService;
    }

    @Operation(summary = "新建文档")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CREATE", objectType = "PdmDoc")
    public Result<DocDtos.DocDetail> create(@Valid @RequestBody DocDtos.DocSaveReq req) {
        return Result.ok(docService.create(req, CurrentUser.userId()));
    }

    @Operation(summary = "修改文档（已发布或他人检出时拒绝）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_UPDATE", objectType = "PdmDoc")
    public Result<DocDtos.DocDetail> update(@PathVariable Long id,
                                            @Valid @RequestBody DocDtos.DocSaveReq req) {
        return Result.ok(docService.update(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "文档详情（含文件与检出状态）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<DocDtos.DocDetail> detail(@PathVariable Long id) {
        return Result.ok(docService.detail(id));
    }

    @Operation(summary = "文档列表（仅最新版本，按数据范围过滤）")
    @GetMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<PageResult<DocDtos.DocSummary>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "lifecycleState", required = false) String lifecycleState,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "fileExt", required = false) String fileExt,
            @RequestParam(value = "excludeFileExt", required = false) String excludeFileExt,
            @RequestParam(value = "q", required = false) String fullText) {
        return Result.ok(docService.page(page, size, keyword, docType, lifecycleState, classId,
                fullText, fileExt, excludeFileExt));
    }

    @Operation(summary = "版本历史")
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<DocDtos.DocVersion>> versions(@PathVariable Long id) {
        return Result.ok(docService.versions(id));
    }

    @Operation(summary = "关联对象（关系双向合并）")
    @GetMapping("/{id}/relations")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<DocDtos.RelationView>> relations(@PathVariable Long id) {
        return Result.ok(docViewService.relations(id));
    }

    @Operation(summary = "操作日志（状态流转 + 审计事件）")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<DocDtos.LogView>> logs(@PathVariable Long id,
                                              @RequestParam(value = "limit", defaultValue = "100") int limit) {
        return Result.ok(docViewService.logs(id, limit));
    }

    @Operation(summary = "实例权限（只读，授予撤销走 /security/acl）")
    @GetMapping("/{id}/acl")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<SecurityDtos.AclRow>> acl(@PathVariable Long id) {
        return Result.ok(docViewService.acl(id));
    }

    @Operation(summary = "上传文档文件（主文件/附件）")
    @PostMapping(value = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file.upload')")
    @Audit(module = "PDM", action = "DOC_UPLOAD", objectType = "PdmDoc")
    public Result<FileService.UploadResult> uploadFile(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "fileRole", required = false) String fileRole,
            @RequestParam(value = "sort", required = false) Integer sort) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        try (InputStream in = file.getInputStream()) {
            return Result.ok(docService.uploadFile(id, file.getOriginalFilename(), file.getContentType(),
                    fileRole, sort, in, CurrentUser.userId()));
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "读取上传内容失败：" + ex.getMessage());
        }
    }

    @Operation(summary = "解绑文档文件")
    @DeleteMapping("/{id}/files/{linkId}")
    @PreAuthorize("hasAuthority('file.upload')")
    @Audit(module = "PDM", action = "DOC_UNBIND_FILE", objectType = "PdmDoc")
    public Result<Void> unbindFile(@PathVariable Long id, @PathVariable Long linkId) {
        docService.unbindFile(id, linkId, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "检出（软锁，超时自动释放）")
    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CHECKOUT", objectType = "PdmDoc")
    public Result<DocDtos.DocDetail> checkout(@PathVariable Long id,
                                              @RequestBody(required = false) DocDtos.CheckoutReq req,
                                              HttpServletRequest request) {
        return Result.ok(docService.checkout(id, req == null ? null : req.remark(), CurrentUser.userId(),
                clientIp(request), request.getHeader("User-Agent")));
    }

    @Operation(summary = "检入（生成新小版本）")
    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CHECKIN", objectType = "PdmDoc")
    public Result<DocDtos.DocDetail> checkin(@PathVariable Long id,
                                             @RequestBody(required = false) DocDtos.CheckinReq req) {
        return Result.ok(docService.checkin(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "取消检出（不产生新版本）")
    @PostMapping("/{id}/cancel-checkout")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CANCEL_CHECKOUT", objectType = "PdmDoc")
    public Result<Void> cancelCheckout(@PathVariable Long id) {
        docService.cancelCheckout(id, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "强制解锁（管理员，留审计）")
    @PostMapping("/{id}/force-unlock")
    @PreAuthorize("hasAuthority('pdm.doc.release')")
    @Audit(module = "PDM", action = "DOC_FORCE_UNLOCK", objectType = "PdmDoc")
    public Result<Long> forceUnlock(@PathVariable Long id) {
        return Result.ok(docService.forceUnlock(id, CurrentUser.userId()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
