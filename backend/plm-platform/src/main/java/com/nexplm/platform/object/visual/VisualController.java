package com.nexplm.platform.object.visual;

import com.nexplm.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在线可视化：前端「可视化」页签先问这里该用哪种查看器，再按返回的 fileId 取文件流。
 * <p>文件流仍走 {@code GET /files/{fileId}/content}（该接口已校验归属对象的数据范围）。</p>
 */
@Tag(name = "在线可视化")
@RestController
@RequestMapping("/api/v1/visual")
public class VisualController {

    private final VisualService visualService;

    public VisualController(VisualService visualService) {
        this.visualService = visualService;
    }

    @Operation(summary = "文档可视化信息（引擎/是否需要转换/候选文件）")
    @GetMapping("/docs/{id}")
    @PreAuthorize("hasAuthority('pdm.doc.read')")
    public Result<VisualDtos.VisualInfo> docVisual(@PathVariable Long id,
                                                   @RequestParam(value = "fileId", required = false) Long fileId) {
        return Result.ok(visualService.forDoc(id, fileId));
    }

    @Operation(summary = "零部件可视化信息（三维模型走 SView，二维图纸走 PDF）")
    @GetMapping("/items/{id}")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<VisualDtos.VisualInfo> itemVisual(@PathVariable Long id) {
        return Result.ok(visualService.forItem(id));
    }
}
