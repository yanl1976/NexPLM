package com.nexplm.platform.change.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.change.service.ChgService;
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

/** P5 变更管理：ECR 主档与关联对象。 */
@Tag(name = "变更管理")
@RestController
@RequestMapping("/api/v1/chg")
public class ChgController {

    private final ChgService chgService;
    private final com.nexplm.platform.change.service.ChgExecService execService;

    public ChgController(ChgService chgService,
                         com.nexplm.platform.change.service.ChgExecService execService) {
        this.chgService = chgService;
        this.execService = execService;
    }

    @Operation(summary = "变更申请列表（按状态/关键字筛选）")
    @GetMapping("/ecrs")
    @PreAuthorize("hasAuthority('chg.ecr.read')")
    public Result<PageResult<ChgDtos.EcrRow>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(chgService.list(CurrentUser.companyId(), keyword, status, page, size));
    }

    @Operation(summary = "变更申请详情（含关联对象与派生通知）")
    @GetMapping("/ecrs/{id}")
    @PreAuthorize("hasAuthority('chg.ecr.read')")
    public Result<ChgDtos.EcrDetail> detail(@PathVariable Long id) {
        return Result.ok(chgService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "变更追踪时间线（申请→评审→通知→执行→关闭）")
    @GetMapping("/ecrs/{id}/timeline")
    @PreAuthorize("hasAuthority('chg.ecr.read')")
    public Result<java.util.List<ChgDtos.TimelineEvent>> timeline(@PathVariable Long id) {
        return Result.ok(chgService.timeline(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建变更申请（自动生成 ECR-xxxxxx）")
    @PostMapping("/ecrs")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_CREATE", objectType = "ChgEcr")
    public Result<ChgDtos.EcrDetail> create(@Valid @RequestBody ChgDtos.EcrSaveReq req) {
        return Result.ok(chgService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改变更申请（仅草稿）")
    @PutMapping("/ecrs/{id}")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_UPDATE", objectType = "ChgEcr")
    public Result<ChgDtos.EcrDetail> update(@PathVariable Long id,
                                            @Valid @RequestBody ChgDtos.EcrSaveReq req) {
        return Result.ok(chgService.update(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "提交评审（需至少一个受影响对象）")
    @PostMapping("/ecrs/{id}/submit")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_SUBMIT", objectType = "ChgEcr")
    public Result<ChgDtos.EcrDetail> submit(@PathVariable Long id) {
        return Result.ok(chgService.submit(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "关闭变更申请")
    @PostMapping("/ecrs/{id}/close")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_CLOSE", objectType = "ChgEcr")
    public Result<ChgDtos.EcrDetail> close(@PathVariable Long id) {
        return Result.ok(chgService.close(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "添加关联对象（受影响/影响/参考）")
    @PostMapping("/ecrs/{id}/objects")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_OBJECT_ADD", objectType = "ChgEcr")
    public Result<ChgDtos.ObjectRow> addObject(@PathVariable Long id,
                                               @Valid @RequestBody ChgDtos.ObjectAddReq req) {
        return Result.ok(chgService.addObject(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "移除关联对象")
    @DeleteMapping("/ecrs/{id}/objects/{linkId}")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_OBJECT_REMOVE", objectType = "ChgEcr")
    public Result<Void> removeObject(@PathVariable Long id, @PathVariable Long linkId) {
        chgService.removeObject(id, linkId, CurrentUser.companyId());
        return Result.ok();
    }

    // ---------------------------------------------------------------- 变更执行（ECN / Problem / 执行）

    @Operation(summary = "下发变更通知（ECN，ECR 转「已通知」）")
    @PostMapping("/ecrs/{id}/ecns")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECN_CREATE", objectType = "ChgEcn")
    public Result<ChgDtos.EcnRow> createEcn(@PathVariable Long id,
                                            @Valid @RequestBody ChgDtos.EcnSaveReq req) {
        return Result.ok(execService.createEcn(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "签发变更通知（草稿 → 已签发）")
    @PostMapping("/ecns/{id}/issue")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECN_ISSUE", objectType = "ChgEcn")
    public Result<ChgDtos.EcnRow> issueEcn(@PathVariable Long id) {
        return Result.ok(execService.issueEcn(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "关闭变更通知（需无未关闭问题）")
    @PostMapping("/ecns/{id}/close")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECN_CLOSE", objectType = "ChgEcn")
    public Result<ChgDtos.EcnRow> closeEcn(@PathVariable Long id) {
        return Result.ok(execService.closeEcn(id, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "变更问题列表")
    @GetMapping("/ecns/{id}/problems")
    @PreAuthorize("hasAuthority('chg.ecr.read')")
    public Result<java.util.List<ChgDtos.ProblemRow>> problems(@PathVariable Long id) {
        return Result.ok(execService.problems(id, CurrentUser.companyId()));
    }

    @Operation(summary = "登记变更问题")
    @PostMapping("/ecns/{id}/problems")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "PROBLEM_CREATE", objectType = "ChgProblem")
    public Result<ChgDtos.ProblemRow> addProblem(@PathVariable Long id,
                                                 @Valid @RequestBody ChgDtos.ProblemReq req) {
        return Result.ok(execService.addProblem(id, req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "关闭变更问题")
    @PostMapping("/problems/{id}/close")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "PROBLEM_CLOSE", objectType = "ChgProblem")
    public Result<Void> closeProblem(@PathVariable Long id) {
        execService.closeProblem(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "影响分析（从受影响对象按 BOM 向上/向下展开）")
    @GetMapping("/ecrs/{id}/impact")
    @PreAuthorize("hasAuthority('chg.ecr.read')")
    public Result<java.util.List<ChgDtos.ImpactRow>> impact(@PathVariable Long id,
                                                            @RequestParam(defaultValue = "3") int depth,
                                                            @RequestParam(defaultValue = "DESIGN") String viewType) {
        return Result.ok(execService.impact(id, CurrentUser.companyId(), depth, viewType));
    }

    @Operation(summary = "执行变更（受影响对象批量升版/废弃/发布）")
    @PostMapping("/ecrs/{id}/execute")
    @PreAuthorize("hasAuthority('chg.ecr.write')")
    @Audit(module = "CHG", action = "ECR_EXECUTE", objectType = "ChgEcr")
    public Result<ChgDtos.ExecuteResult> execute(@PathVariable Long id) {
        return Result.ok(execService.execute(id, CurrentUser.companyId(), CurrentUser.userId()));
    }
}
