package com.nexplm.platform.doc.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.doc.service.DocQueryService;
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
 * 高级查询条件：保存、复用、共享。
 */
@Tag(name = "PDM-高级查询条件")
@RestController
@RequestMapping("/api/v1/doc-queries")
public class DocQueryController {

    private final DocQueryService docQueryService;

    public DocQueryController(DocQueryService docQueryService) {
        this.docQueryService = docQueryService;
    }

    @Operation(summary = "我的查询条件（含他人共享）")
    @GetMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<List<DocQueryDtos.SavedQuery>> list() {
        return Result.ok(docQueryService.list(CurrentUser.userId()));
    }

    @Operation(summary = "保存查询条件")
    @PostMapping
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    @Audit(module = "PDM", action = "DOC_QUERY_SAVE", objectType = "DocSavedQuery")
    public Result<DocQueryDtos.SavedQuery> create(@Valid @RequestBody DocQueryDtos.SavedQueryReq req) {
        return Result.ok(docQueryService.create(req, CurrentUser.userId()));
    }

    @Operation(summary = "修改查询条件（仅本人）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<DocQueryDtos.SavedQuery> update(@PathVariable Long id,
                                                  @Valid @RequestBody DocQueryDtos.SavedQueryReq req) {
        return Result.ok(docQueryService.update(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除查询条件（仅本人）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<Void> delete(@PathVariable Long id) {
        docQueryService.delete(id, CurrentUser.userId());
        return Result.ok();
    }
}
