package com.nexplm.platform.doc.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.doc.service.DocClassService;
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

/**
 * 文档分类接口：分类树、分类属性（含继承）、按分类建文档所需的属性定义。
 */
@Tag(name = "PDM-文档分类")
@RestController
@RequestMapping("/api/v1/doc-classes")
public class DocClassController {

    private final DocClassService docClassService;

    public DocClassController(DocClassService docClassService) {
        this.docClassService = docClassService;
    }

    @Operation(summary = "分类树（含各分类文档数）")
    @GetMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<DocClassDtos.ClassNode>> tree(
            @RequestParam(value = "entityCode", required = false) String entityCode) {
        return Result.ok(docClassService.tree(entityCode));
    }

    @Operation(summary = "新建分类")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CLASS_CREATE", objectType = "DocClass")
    public Result<DocClassDtos.ClassNode> create(@Valid @RequestBody DocClassDtos.ClassSaveReq req) {
        return Result.ok(docClassService.create(req, CurrentUser.userId()));
    }

    @Operation(summary = "修改分类（支持改编码/换父级，子树路径同步）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CLASS_UPDATE", objectType = "DocClass")
    public Result<DocClassDtos.ClassNode> update(@PathVariable Long id,
                                                 @Valid @RequestBody DocClassDtos.ClassSaveReq req) {
        return Result.ok(docClassService.update(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除分类（需无子分类且无文档引用）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CLASS_DELETE", objectType = "DocClass")
    public Result<Void> delete(@PathVariable Long id) {
        docClassService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "分类的生效属性（自身 + 祖先可继承属性）")
    @GetMapping("/{id}/attributes")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<DocClassDtos.AttrDef>> attributes(@PathVariable Long id) {
        return Result.ok(docClassService.attrs(id));
    }

    @Operation(summary = "保存分类自身属性（全量替换）")
    @PutMapping("/{id}/attributes")
    @PreAuthorize("hasAuthority('pdm.doc.write')")
    @Audit(module = "PDM", action = "DOC_CLASS_ATTR_SAVE", objectType = "DocClass")
    public Result<List<DocClassDtos.AttrDef>> saveAttributes(@PathVariable Long id,
                                                             @Valid @RequestBody DocClassDtos.AttrSaveReq req) {
        return Result.ok(docClassService.saveAttrs(id, req.items(), CurrentUser.userId()));
    }
}
