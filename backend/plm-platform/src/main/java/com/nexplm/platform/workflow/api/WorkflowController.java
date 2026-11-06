package com.nexplm.platform.workflow.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Operation(summary = "我的已办")
    @GetMapping("/tasks/done")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<WfDtos.PageResult<WfDtos.DoneTaskRow>> doneTasks(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.ok(workflowService.doneTasks(CurrentUser.userId(), page, size));
    }

    @Operation(summary = "我发起的流程")
    @GetMapping("/instances/started")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<WfDtos.PageResult<WfDtos.InstanceRow>> startedInstances(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(workflowService.startedInstances(CurrentUser.userId(), page, size));
    }

    @Operation(summary = "业务对象的流程实例列表（对象详情审批页签）")
    @GetMapping("/instances")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<List<WfDtos.InstanceRow>> objectInstances(@RequestParam String objectType,
                                                            @RequestParam Long objectId) {
        return Result.ok(workflowService.objectInstances(objectType, objectId));
    }

    @Operation(summary = "批量办理（逐条独立，失败项回带原因）")
    @PostMapping("/tasks/batch-complete")
    @PreAuthorize("hasAuthority('wf.task.audit')")
    @Audit(module = "WF", action = "TASK_BATCH_COMPLETE", objectType = "WfTask")
    public Result<WfDtos.BatchResult> batchComplete(@Valid @RequestBody WfDtos.BatchCompleteReq req) {
        return Result.ok(workflowService.batchComplete(req.taskIds(), req.comment(), CurrentUser.userId()));
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

    @Operation(summary = "已走过的用户任务节点（驳回目标候选）")
    @GetMapping("/instances/{id}/activities")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<List<WfDtos.ActivityRow>> activities(@PathVariable String id) {
        return Result.ok(workflowService.activities(id));
    }

    @Operation(summary = "流程绑定列表")
    @GetMapping("/bindings")
    @PreAuthorize("hasAuthority('wf.process.manage')")
    public Result<List<WfDtos.BindingRow>> bindings(@RequestParam(required = false) String objectType) {
        return Result.ok(workflowService.bindings(objectType));
    }

    @Operation(summary = "流程与生命周期一致性巡检（fix=true 时自动修正可判定的漂移）")
    @PostMapping("/consistency/check")
    @PreAuthorize("hasAuthority('wf.process.manage')")
    @Audit(module = "WF", action = "CONSISTENCY_CHECK", objectType = "WfInstance")
    public Result<WfDtos.ConsistencyReport> checkConsistency(@RequestParam(defaultValue = "false") boolean fix) {
        return Result.ok(workflowService.checkConsistency(fix));
    }

    @Operation(summary = "对象电子签名记录（含主文件摘要核验结果）")
    @GetMapping("/signatures")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<List<WfDtos.SignatureRow>> signatures(@RequestParam String objectType,
                                                        @RequestParam Long objectId) {
        return Result.ok(workflowService.signatures(objectType, objectId));
    }

    @Operation(summary = "执行人解析类型（用户/角色/组织/组/组+角色/发起人/表单字段/项目角色）")
    @GetMapping("/assignee-types")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<List<WfDtos.AssigneeTypeRow>> assigneeTypes() {
        return Result.ok(workflowService.assigneeTypes());
    }

    @Operation(summary = "可指派人员检索（选人用；普通审批人可用，不要求用户管理权限）")
    @GetMapping("/actors/users")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<List<WfDtos.ActorUserRow>> actorUsers(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(workflowService.searchActorUsers(CurrentUser.companyId(), keyword, limit));
    }

    @Operation(summary = "超时催办（扫描超过 N 小时未办理的任务并留痕，同窗口内幂等）")
    @PostMapping("/tasks/remind")
    @PreAuthorize("hasAuthority('wf.process.manage')")
    @Audit(module = "WF", action = "TASK_REMIND", objectType = "WfTask")
    public Result<WfDtos.RemindResult> remind(@RequestParam(defaultValue = "24") int hours) {
        return Result.ok(workflowService.remindOverdueTasks(hours, CurrentUser.userId()));
    }

    @Operation(summary = "加签（给会签节点追加审批人）")
    @PostMapping("/tasks/{taskId}/add-signer")
    @PreAuthorize("hasAuthority('wf.task.audit')")
    @Audit(module = "WF", action = "TASK_ADD_SIGNER", objectType = "WfTask")
    public Result<Void> addSigner(@PathVariable String taskId, @Valid @RequestBody WfDtos.AddSignerReq req) {
        workflowService.addSigner(taskId, req, CurrentUser.userId());
        return Result.ok();
    }

    @Operation(summary = "新建委托（生效区间内代办理）")
    @PostMapping("/delegates")
    @PreAuthorize("hasAuthority('wf.task.read')")
    @Audit(module = "WF", action = "DELEGATE_CREATE", objectType = "WfDelegate")
    public Result<WfDtos.DelegateRow> createDelegate(@Valid @RequestBody WfDtos.DelegateReq req) {
        return Result.ok(workflowService.createDelegate(req, CurrentUser.userId()));
    }

    @Operation(summary = "我的委托（我设置的 + 我收到的）")
    @GetMapping("/delegates")
    @PreAuthorize("hasAuthority('wf.task.read')")
    public Result<List<WfDtos.DelegateRow>> delegates() {
        return Result.ok(workflowService.delegates(CurrentUser.userId()));
    }

    @Operation(summary = "删除委托")
    @DeleteMapping("/delegates/{id}")
    @PreAuthorize("hasAuthority('wf.task.read')")
    @Audit(module = "WF", action = "DELEGATE_DELETE", objectType = "WfDelegate")
    public Result<Void> removeDelegate(@PathVariable Long id) {
        workflowService.removeDelegate(id, CurrentUser.userId());
        return Result.ok();
    }
}
