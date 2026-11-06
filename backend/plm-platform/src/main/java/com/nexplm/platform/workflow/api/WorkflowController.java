package com.nexplm.platform.workflow.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工作流接口：发起、待办、办理、驳回、撤回、详情、绑定查询。
 */
@Tag(name = "工作流")
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Operation(summary = "发起流程（初审 + 会签人）")
    @PostMapping("/instances")
    @PreAuthorize("hasAuthority('wf.process.start')")
    @Audit(module = "WF", action = "PROCESS_START", objectType = "PlmObject")
    public Result<WfDtos.StartResp> start(@Valid @RequestBody WfDtos.StartReq req) {
        return Result.ok(workflowService.start(req, CurrentUser.userId()));
    }

    @Operation(summary = "我的待办")
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<WfDtos.PageResult<WfDtos.TaskRow>> myTasks(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return Result.ok(workflowService.myTasks(CurrentUser.userId(), page, size));
    }

    @Operation(summary = "办理任务（通过）")
    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasAuthority('wf.task.audit')")
    @Audit(module = "WF", action = "TASK_COMPLETE", objectType = "WfTask")
    public Result<Void> complete(@PathVariable String taskId,
                                 @Valid @RequestBody(required = false) WfDtos.CompleteReq req) {
        workflowService.complete(taskId, req, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "驳回任务到指定节点")
    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize("hasAuthority('wf.task.audit')")
    @Audit(module = "WF", action = "TASK_REJECT", objectType = "WfTask")
    public Result<Void> reject(@PathVariable String taskId, @Valid @RequestBody WfDtos.RejectReq req) {
        workflowService.reject(taskId, req, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "撤回流程（仅发起人）")
    @PostMapping("/instances/{id}/withdraw")
    @PreAuthorize("hasAuthority('wf.process.start')")
    @Audit(module = "WF", action = "PROCESS_WITHDRAW", objectType = "WfInstance")
    public Result<Void> withdraw(@PathVariable String id,
                                 @RequestBody(required = false) WfDtos.WithdrawReq req) {
        workflowService.withdraw(id, req, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "流程实例详情（含活动任务与审批意见）")
    @GetMapping("/instances/{id}")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<WfDtos.InstanceDetail> instance(@PathVariable String id) {
        return Result.ok(workflowService.instance(id));
    }

    @Operation(summary = "流程绑定列表")
    @GetMapping("/bindings")
    @PreAuthorize("hasAuthority('wf.process.manage')")
    public Result<List<WfDtos.BindingRow>> bindings(@RequestParam(required = false) String objectType) {
        return Result.ok(workflowService.bindings(objectType));
    }
}
