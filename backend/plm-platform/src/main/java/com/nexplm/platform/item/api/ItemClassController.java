package com.nexplm.platform.item.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.doc.api.DocClassDtos;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 零部件分类接口（批次 H·T-H-01，复刻蓝本 {@code Classify/Edit} + {@code Classify/Attribute}）。
 *
 * <p>分类树与属性复用 {@code doc_class}/{@code doc_class_attr}（entity_code=PdmItem），
 * 写入统一走 {@link DocClassService}，本控制器只负责权限边界（ADR-0001）。</p>
 */
@Tag(name = "PSE-零部件分类")
@RestController
@RequestMapping("/api/v1/item-classes")
public class ItemClassController {

    private final DocClassService classService;

    public ItemClassController(DocClassService classService) {
        this.classService = classService;
    }

    @Operation(summary = "零部件分类树（含各分类下零件数）")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<DocClassDtos.ClassNode>> tree() {
        return Result.ok(classService.tree(DocClassService.ENTITY_ITEM));
    }

    @Operation(summary = "新建零部件分类")
    @PostMapping
    @PreAuthorize("hasAuthority('pse.classify.manage')")
    @Audit(module = "PSE", action = "ITEM_CLASS_CREATE", objectType = "DocClass")
    public Result<DocClassDtos.ClassNode> create(@Valid @RequestBody DocClassDtos.ClassSaveReq req) {
        return Result.ok(classService.create(DocClassService.ENTITY_ITEM, req, CurrentUser.userId()));
    }

    @Operation(summary = "修改零部件分类（支持改编码/换父级，子树路径同步）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.classify.manage')")
    @Audit(module = "PSE", action = "ITEM_CLASS_UPDATE", objectType = "DocClass")
    public Result<DocClassDtos.ClassNode> update(@PathVariable Long id,
                                                 @Valid @RequestBody DocClassDtos.ClassSaveReq req) {
        // 先校验归属，防止通过零件接口改到文档分类（反之亦然）
        classService.requireClass(id, DocClassService.ENTITY_ITEM);
        return Result.ok(classService.update(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除零部件分类（需无子分类且无零件引用）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.classify.manage')")
    @Audit(module = "PSE", action = "ITEM_CLASS_DELETE", objectType = "DocClass")
    public Result<Void> delete(@PathVariable Long id) {
        classService.requireClass(id, DocClassService.ENTITY_ITEM);
        classService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "零部件分类的生效属性（自身 + 祖先可继承属性）")
    @GetMapping("/{id}/attributes")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<DocClassDtos.AttrDef>> attributes(@PathVariable Long id) {
        classService.requireClass(id, DocClassService.ENTITY_ITEM);
        return Result.ok(classService.attrs(id));
    }

    @Operation(summary = "保存零部件分类自身属性（全量替换）")
    @PutMapping("/{id}/attributes")
    @PreAuthorize("hasAuthority('pse.classify.manage')")
    @Audit(module = "PSE", action = "ITEM_CLASS_ATTR_SAVE", objectType = "DocClass")
    public Result<List<DocClassDtos.AttrDef>> saveAttributes(@PathVariable Long id,
                                                             @Valid @RequestBody DocClassDtos.AttrSaveReq req) {
        classService.requireClass(id, DocClassService.ENTITY_ITEM);
        return Result.ok(classService.saveAttrs(id, req.items(), CurrentUser.userId()));
    }
}
