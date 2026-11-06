package com.nexplm.platform.project.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.project.service.ProjectDeliverableService;
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

/** 任务交付物与齐套性检查（P7-4）。 */
@Tag(name = "项目-交付物与齐套")
@RestController
@RequestMapping("/api/v1")
public class ProjectDeliverableController {

    private final ProjectDeliverableService deliverableService;

    public ProjectDeliverableController(ProjectDeliverableService deliverableService) {
        this.deliverableService = deliverableService;
    }

    @Operation(summary = "项目交付物齐套看板（任务 × 输入/输出对象 + 汇总）")
    @GetMapping("/prj/projects/{projectId}/deliverables")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectDeliverableDtos.DeliverableBoard> board(@PathVariable Long projectId) {
        return Result.ok(deliverableService.board(projectId, CurrentUser.companyId()));
    }

    @Operation(summary = "给任务挂交付物（对象存在性服务端校验）")
    @PostMapping("/prj/tasks/{taskId}/deliverables")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "DELIVERABLE_CREATE", objectType = "PrjTaskOutput")
    public Result<ProjectDeliverableDtos.DeliverableBoard> add(
            @PathVariable Long taskId,
            @Valid @RequestBody ProjectDeliverableDtos.DeliverableSaveReq req) {
        return Result.ok(deliverableService.add(taskId, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改交付物（方向/是否必需/备注）")
    @PutMapping("/prj/deliverables/{outputId}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "DELIVERABLE_UPDATE", objectType = "PrjTaskOutput")
    public Result<ProjectDeliverableDtos.DeliverableBoard> update(
            @PathVariable Long outputId,
            @Valid @RequestBody ProjectDeliverableDtos.DeliverableUpdateReq req) {
        return Result.ok(deliverableService.update(outputId, req, CurrentUser.companyId()));
    }

    @Operation(summary = "移除交付物（软删）")
    @DeleteMapping("/prj/deliverables/{outputId}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "DELIVERABLE_DELETE", objectType = "PrjTaskOutput")
    public Result<ProjectDeliverableDtos.DeliverableBoard> remove(@PathVariable Long outputId) {
        return Result.ok(deliverableService.remove(outputId, CurrentUser.companyId()));
    }

    @Operation(summary = "可选交付对象（文档/零部件最新版本、工艺卡片）")
    @GetMapping("/prj/deliverable-candidates")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<List<ProjectDeliverableDtos.CandidateRow>> candidates(
            @RequestParam String objectType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.ok(deliverableService.candidates(objectType, keyword, limit,
                CurrentUser.companyId()));
    }
}
