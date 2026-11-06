package com.nexplm.platform.quality.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.quality.service.SysDocService;
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

import java.util.List;

/** IATF16949 体系文件：模板 + 文件夹 + 版本 + 审批发布（P8-3-1）。 */
@Tag(name = "质量-体系文件")
@RestController
@RequestMapping("/api/v1")
public class SysDocController {

    private final SysDocService sysDocService;

    public SysDocController(SysDocService sysDocService) {
        this.sysDocService = sysDocService;
    }

    // ---------------------------------------------------------------- 文件夹

    @Operation(summary = "体系文件文件夹（扁平返回，含文件数）")
    @GetMapping("/qlt/sys-doc-folders")
    @PreAuthorize("hasAuthority('qlt.sysdoc.read')")
    public Result<List<SysDocDtos.FolderRow>> folders() {
        return Result.ok(sysDocService.folders(CurrentUser.companyId()));
    }

    @Operation(summary = "新建文件夹（code 创建后不可改）")
    @PostMapping("/qlt/sys-doc-folders")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_FOLDER_CREATE", objectType = "SysDocFolder")
    public Result<SysDocDtos.FolderRow> createFolder(
            @Valid @RequestBody SysDocDtos.FolderSaveReq req) {
        return Result.ok(sysDocService.createFolder(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改文件夹名称/排序")
    @PutMapping("/qlt/sys-doc-folders/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_FOLDER_UPDATE", objectType = "SysDocFolder")
    public Result<SysDocDtos.FolderRow> updateFolder(
            @PathVariable Long id, @Valid @RequestBody SysDocDtos.FolderSaveReq req) {
        return Result.ok(sysDocService.updateFolder(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除文件夹（有子文件夹或文件则拒绝）")
    @DeleteMapping("/qlt/sys-doc-folders/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_FOLDER_DELETE", objectType = "SysDocFolder")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        sysDocService.deleteFolder(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    // ---------------------------------------------------------------- 模板

    @Operation(summary = "体系文件模板列表")
    @GetMapping("/qlt/sys-doc-templates")
    @PreAuthorize("hasAuthority('qlt.sysdoc.read')")
    public Result<List<SysDocDtos.TemplateRow>> templates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return Result.ok(sysDocService.templates(CurrentUser.companyId(), keyword, enabled));
    }

    @Operation(summary = "模板详情（含章节骨架）")
    @GetMapping("/qlt/sys-doc-templates/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.read')")
    public Result<SysDocDtos.TemplateDetail> template(@PathVariable Long id) {
        return Result.ok(sysDocService.template(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建模板（自动取号 TPL-xxxxxx）")
    @PostMapping("/qlt/sys-doc-templates")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_TEMPLATE_CREATE", objectType = "SysDocTemplate")
    public Result<SysDocDtos.TemplateDetail> createTemplate(
            @Valid @RequestBody SysDocDtos.TemplateSaveReq req) {
        return Result.ok(sysDocService.createTemplate(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改模板（含章节骨架整表替换）")
    @PutMapping("/qlt/sys-doc-templates/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_TEMPLATE_UPDATE", objectType = "SysDocTemplate")
    public Result<SysDocDtos.TemplateDetail> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody SysDocDtos.TemplateSaveReq req) {
        return Result.ok(sysDocService.updateTemplate(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除模板（已生成的文件不受影响）")
    @DeleteMapping("/qlt/sys-doc-templates/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_TEMPLATE_DELETE", objectType = "SysDocTemplate")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        sysDocService.deleteTemplate(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    // ---------------------------------------------------------------- 体系文件

    @Operation(summary = "体系文件列表（文件夹含子树、可按状态/层级/到期评审过滤）")
    @GetMapping("/qlt/sys-docs")
    @PreAuthorize("hasAuthority('qlt.sysdoc.read')")
    public Result<PageResult<SysDocDtos.DocRow>> docs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String docLevel,
            @RequestParam(required = false) Boolean dueReviewOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(sysDocService.docs(CurrentUser.companyId(), keyword, folderId, status,
                docLevel, dueReviewOnly, page, size));
    }

    @Operation(summary = "体系文件详情（含章节内容与历史版本）")
    @GetMapping("/qlt/sys-docs/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.read')")
    public Result<SysDocDtos.DocDetail> doc(@PathVariable Long id) {
        return Result.ok(sysDocService.doc(id, CurrentUser.companyId()));
    }

    @Operation(summary = "按模板生成体系文件（层级/文件夹/评审周期/章节骨架取模板）")
    @PostMapping("/qlt/sys-docs/from-template")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_CREATE_FROM_TEMPLATE", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> createFromTemplate(
            @Valid @RequestBody SysDocDtos.DocFromTemplateReq req) {
        return Result.ok(sysDocService.createFromTemplate(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "手工新建体系文件（草稿）")
    @PostMapping("/qlt/sys-docs")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_CREATE", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> createDoc(
            @Valid @RequestBody SysDocDtos.DocSaveReq req) {
        return Result.ok(sysDocService.createDoc(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改体系文件（仅草稿）")
    @PutMapping("/qlt/sys-docs/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_UPDATE", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> updateDoc(
            @PathVariable Long id, @Valid @RequestBody SysDocDtos.DocSaveReq req) {
        return Result.ok(sysDocService.updateDoc(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除体系文件（仅草稿且无发布版本）")
    @DeleteMapping("/qlt/sys-docs/{id}")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_DELETE", objectType = "SysDoc")
    public Result<Void> deleteDoc(@PathVariable Long id) {
        sysDocService.deleteDoc(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "提交审批（必填章节必须已填写）")
    @PostMapping("/qlt/sys-docs/{id}/submit")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_SUBMIT", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> submit(@PathVariable Long id) {
        return Result.ok(sysDocService.submit(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "审批（通过即发布并生成版本快照；驳回须填原因）")
    @PostMapping("/qlt/sys-docs/{id}/approve")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_APPROVE", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> approve(
            @PathVariable Long id, @Valid @RequestBody SysDocDtos.DocApproveReq req) {
        return Result.ok(sysDocService.approve(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "发起修订（已发布 → 草稿，版本号 +1）")
    @PostMapping("/qlt/sys-docs/{id}/revise")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_REVISE", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> revise(
            @PathVariable Long id,
            @RequestBody(required = false) SysDocDtos.DocReviseReq req) {
        return Result.ok(sysDocService.revise(id, req == null ? null : req.changeNote(),
                CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "作废（已发布 → 作废，保留版本历史）")
    @PostMapping("/qlt/sys-docs/{id}/obsolete")
    @PreAuthorize("hasAuthority('qlt.sysdoc.write')")
    @Audit(module = "QLT", action = "SYSDOC_OBSOLETE", objectType = "SysDoc")
    public Result<SysDocDtos.DocDetail> obsolete(@PathVariable Long id) {
        return Result.ok(sysDocService.obsolete(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "到期评审清单（未来 N 天内到期或已逾期）")
    @GetMapping("/qlt/sys-docs/due-review")
    @PreAuthorize("hasAuthority('qlt.sysdoc.read')")
    public Result<List<SysDocDtos.DocRow>> dueReview(
            @RequestParam(required = false) Integer days) {
        return Result.ok(sysDocService.dueReview(CurrentUser.companyId(), days));
    }
}
