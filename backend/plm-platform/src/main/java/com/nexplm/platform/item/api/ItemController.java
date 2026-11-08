package com.nexplm.platform.item.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.item.service.ItemBatchService;
import com.nexplm.platform.item.service.ItemExcelService;
import com.nexplm.platform.item.service.ItemLifecycleService;
import com.nexplm.platform.item.service.ItemService;
import com.nexplm.platform.item.service.ItemViewService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 零部件接口：列表、详情、增删改、批量、导入导出、检出检入、修订、视图、页签数据（8 种类型共用）。
 * <p>页面口径对齐旧系统 PSEITEM（见 docs/10-深度探查-零部件管理.md）。</p>
 */
@Tag(name = "PSE-零部件")
@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ItemService itemService;
    private final ItemLifecycleService lifecycleService;
    private final ItemViewService viewService;
    private final ItemExcelService excelService;
    private final ItemBatchService batchService;

    public ItemController(ItemService itemService, ItemLifecycleService lifecycleService,
                          ItemViewService viewService, ItemExcelService excelService,
                          ItemBatchService batchService) {
        this.itemService = itemService;
        this.lifecycleService = lifecycleService;
        this.viewService = viewService;
        this.excelService = excelService;
        this.batchService = batchService;
    }

    // ------------------------------------------------------------ 列表与详情

    @Operation(summary = "编码预览（不消耗流水）：新建表单选完分类后显示将生成的编码")
    @GetMapping("/code-preview")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<ItemDtos.CodePreview> codePreview(@RequestParam(value = "classId", required = false) Long classId,
                                                    @RequestParam(value = "itemType", required = false) String itemType,
                                                    @RequestParam(value = "dwgNo", required = false) String dwgNo) {
        return Result.ok(itemService.previewCode(classId, itemType, dwgNo));
    }

    @Operation(summary = "零部件列表（名称/编号/类型/使用状态/生命周期/流程状态/创建人/创建与发布时间区间/材料/规格）")
    @GetMapping
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<PageResult<ItemDtos.ItemSummary>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "usingState", required = false) String usingState,
            @RequestParam(value = "phaseState", required = false) String phaseState,
            @RequestParam(value = "workflowState", required = false) String workflowState,
            @RequestParam(value = "createdBy", required = false) Long createdBy,
            @RequestParam(value = "createdFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(value = "createdTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(value = "releasedFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releasedFrom,
            @RequestParam(value = "releasedTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releasedTo,
            @RequestParam(value = "material", required = false) String material,
            @RequestParam(value = "specification", required = false) String specification,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "includeChildren", required = false) Boolean includeChildren,
            @RequestParam java.util.Map<String, String> params) {
        // 分类属性过滤走 attr.<属性编码>=值（避免为每个属性开一个参数）
        java.util.Map<String, String> attrFilters = new java.util.LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key != null && key.startsWith("attr.") && key.length() > 5) {
                attrFilters.put(key.substring(5), value);
            }
        });
        return Result.ok(itemService.page(page, size, keyword, itemType, sourceType, usingState,
                phaseState, workflowState, createdBy, createdFrom, createdTo,
                releasedFrom, releasedTo, material, specification, classId, includeChildren, attrFilters));
    }

    @Operation(summary = "零部件详情（含设计属性与检出信息）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<ItemDtos.ItemDetail> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id));
    }

    @Operation(summary = "读取零部件分类与分类属性值（分类树见 /item-classes）")
    @GetMapping("/{id}/classify")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<ItemDtos.ItemClassifyInfo> classify(@PathVariable Long id) {
        return Result.ok(itemService.classifyInfo(id));
    }

    @Operation(summary = "按编码自动匹配分类的建议（仅未归类时给出）")
    @GetMapping("/{id}/classify/suggestion")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<ItemDtos.ClassifySuggestion> classifySuggestion(@PathVariable Long id) {
        return Result.ok(itemService.classifySuggestion(id));
    }

    @Operation(summary = "零部件归类/取消归类（classId 为空表示取消；属性按分类生效属性校验）")
    @PutMapping("/{id}/classify")
    @PreAuthorize("hasAuthority('pse.item.write')")
    public Result<ItemDtos.ItemClassifyInfo> assignClass(@PathVariable Long id,
                                                        @RequestBody ItemDtos.ClassAssignReq req) {
        return Result.ok(itemService.assignClass(id, req, CurrentUser.userId()));
    }

    // ------------------------------------------------------------ 增删改

    @Operation(summary = "新建零部件（编码留空自动取号 PN-000001）")
    @PostMapping
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_CREATE", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> create(@Valid @RequestBody ItemDtos.ItemSaveReq req) {
        return Result.ok(itemService.create(req, CurrentUser.userId()));
    }

    @Operation(summary = "修改零部件（已发布/已废弃需先修订）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_UPDATE", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> update(@PathVariable Long id,
                                              @Valid @RequestBody ItemDtos.ItemSaveReq req) {
        return Result.ok(itemService.update(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除零部件（存在 BOM 引用时拒绝）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pse.item.delete')")
    @Audit(module = "PSE", action = "ITEM_DELETE", objectType = "PdmItem")
    public Result<Void> delete(@PathVariable Long id) {
        itemService.delete(id, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "批量删除零部件（部分成功语义）")
    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('pse.item.delete')")
    @Audit(module = "PSE", action = "ITEM_BATCH_DELETE", objectType = "PdmItem")
    public Result<ItemDtos.BatchResult> batchDelete(@RequestBody BatchIdsReq req) {
        return Result.ok(itemService.batchDelete(req.ids(), CurrentUser.userId()));
    }

    public record BatchIdsReq(List<Long> ids) {
    }

    @Operation(summary = "批量新建零部件（逐条独立，部分成功语义）")
    @PostMapping("/batch-create")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_BATCH_CREATE", objectType = "PdmItem")
    public Result<ItemDtos.BatchResult> batchCreate(@RequestBody ItemDtos.BatchCreateReq req) {
        return Result.ok(batchService.batchCreate(req.items(), CurrentUser.userId()));
    }

    @Operation(summary = "批量改属性（补丁字段 null 表示不修改）")
    @PutMapping("/batch-update")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_BATCH_UPDATE", objectType = "PdmItem")
    public Result<ItemDtos.BatchResult> batchUpdate(@Valid @RequestBody ItemDtos.BatchUpdateReq req) {
        return Result.ok(batchService.batchUpdate(req.ids(), req.patch(), CurrentUser.userId()));
    }

    @Operation(summary = "转交所有权（同公司内变更属主）")
    @PostMapping("/transfer-owner")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_TRANSFER_OWNER", objectType = "PdmItem")
    public Result<ItemDtos.BatchResult> transferOwner(@Valid @RequestBody ItemDtos.TransferOwnerReq req) {
        return Result.ok(batchService.transferOwner(req.ids(), req.targetUserId(), CurrentUser.userId()));
    }

    @Operation(summary = "迁移使用状态（Using 在用 / StopUsing 停用 / GiveUp 废弃）")
    @PutMapping("/{id}/using-state")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_USING_STATE", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> changeUsingState(@PathVariable Long id,
                                                        @Valid @RequestBody ItemDtos.UsingStateReq req) {
        return Result.ok(itemService.changeUsingState(id, req.usingState(), CurrentUser.userId()));
    }

    // ------------------------------------------------------------ 检出 / 检入 / 修订 / 历史

    @Operation(summary = "检出零部件")
    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_CHECKOUT", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> checkout(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(lifecycleService.checkout(id, CurrentUser.userId(), clientIp(request), null));
    }

    @Operation(summary = "检入零部件（生成新小版本）")
    @PostMapping("/{id}/checkin")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_CHECKIN", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> checkin(@PathVariable Long id) {
        return Result.ok(lifecycleService.checkin(id, CurrentUser.userId()));
    }

    @Operation(summary = "撤销检出")
    @PostMapping("/{id}/cancel-checkout")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_CANCEL_CHECKOUT", objectType = "PdmItem")
    public Result<Void> cancelCheckout(@PathVariable Long id) {
        lifecycleService.cancelCheckout(id, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "修订升版（大版本 +1，回到草稿）")
    @PostMapping("/{id}/revise")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_REVISE", objectType = "PdmItem")
    public Result<ItemDtos.ItemDetail> revise(@PathVariable Long id) {
        return Result.ok(lifecycleService.revise(id, CurrentUser.userId()));
    }

    @Operation(summary = "版本历史")
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<ItemDtos.VersionRow>> versions(@PathVariable Long id) {
        return Result.ok(lifecycleService.versions(id));
    }

    @Operation(summary = "版本逐字段比较（限同一零部件的两个版本）")
    @GetMapping("/{id}/versions/compare")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<ItemDtos.VersionCompare> compareVersions(
            @PathVariable Long id,
            @RequestParam(value = "baseId") Long baseId,
            @RequestParam(value = "targetId") Long targetId) {
        return Result.ok(lifecycleService.compare(id, baseId, targetId));
    }

    // ------------------------------------------------------------ 视图（旧系统 PSEITEMVIEW）

    @Operation(summary = "零件视图列表")
    @GetMapping("/{id}/views")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<ItemDtos.ItemViewRow>> views(@PathVariable Long id) {
        return Result.ok(itemService.listViews(id));
    }

    @Operation(summary = "新建零件视图（可复制来源视图的 BOM 行）")
    @PostMapping("/{id}/views")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_VIEW_CREATE", objectType = "PdmItem")
    public Result<ItemDtos.ItemViewRow> createView(@PathVariable Long id,
                                                   @RequestBody ItemDtos.ItemViewReq req) {
        return Result.ok(itemService.createView(id, req, CurrentUser.userId()));
    }

    @Operation(summary = "删除零件视图（默认视图与有 BOM 行的视图不可删）")
    @DeleteMapping("/{id}/views/{viewId}")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_VIEW_DELETE", objectType = "PdmItem")
    public Result<Void> deleteView(@PathVariable Long id, @PathVariable Long viewId) {
        itemService.deleteView(id, viewId, CurrentUser.userId());
        return Result.ok();
    }

    // ------------------------------------------------------------ 页签数据

    @Operation(summary = "设计文档（零件关联的图文档）")
    @GetMapping("/{id}/docs")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<ItemDtos.ItemDocRow>> docs(@PathVariable Long id) {
        return Result.ok(viewService.docs(id));
    }

    @Operation(summary = "操作日志（状态流转 + 审计事件）")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<ItemDtos.LogRow>> logs(@PathVariable Long id,
                                              @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.ok(viewService.logs(id, limit));
    }

    @Operation(summary = "实例权限（只读，授予撤销走 /security/acl）")
    @GetMapping("/{id}/acl")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public Result<List<SecurityDtos.AclRow>> acl(@PathVariable Long id) {
        return Result.ok(viewService.acl(id));
    }

    // ------------------------------------------------------------ 导入导出

    @Operation(summary = "导出零部件（按当前筛选条件全量导出 xlsx）")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('pse.item.read')")
    @Audit(module = "PSE", action = "ITEM_EXPORT", objectType = "PdmItem")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "itemType", required = false) String itemType,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            @RequestParam(value = "usingState", required = false) String usingState,
            @RequestParam(value = "phaseState", required = false) String phaseState,
            @RequestParam(value = "workflowState", required = false) String workflowState,
            @RequestParam(value = "createdBy", required = false) Long createdBy) {
        byte[] bytes = excelService.export(keyword, itemType, sourceType, usingState,
                phaseState, workflowState, createdBy);
        String fileName = "零部件_" + LocalDateTime.now().format(FILE_STAMP) + ".xlsx";
        return download(bytes, fileName);
    }

    @Operation(summary = "下载零部件导入模板")
    @GetMapping("/import/template")
    @PreAuthorize("hasAuthority('pse.item.read')")
    public ResponseEntity<byte[]> template() {
        return download(excelService.template(), "零部件导入模板.xlsx");
    }

    @Operation(summary = "导入预演（只检查不落库）")
    @PostMapping("/import/preview")
    @PreAuthorize("hasAuthority('pse.item.write')")
    public Result<Map<String, Object>> preview(@RequestPart("file") MultipartFile file) throws IOException {
        return Result.ok(excelService.preview(file.getInputStream()));
    }

    @Operation(summary = "导入零部件（增量合并：编码存在则更新，否则新建）")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('pse.item.write')")
    @Audit(module = "PSE", action = "ITEM_IMPORT", objectType = "PdmItem")
    public Result<Map<String, Object>> importItems(@RequestPart("file") MultipartFile file) throws IOException {
        return Result.ok(excelService.apply(file.getInputStream(), CurrentUser.userId()));
    }

    // ------------------------------------------------------------ 内部

    private ResponseEntity<byte[]> download(byte[] bytes, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
