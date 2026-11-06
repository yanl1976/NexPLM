package com.nexplm.platform.project.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.project.service.ProjectTaskService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/** WBS 任务与依赖（P7-2）、日历与拖拽排期（P7-7）。 */
@Tag(name = "项目-WBS 任务")
@RestController
@RequestMapping("/api/v1")
public class ProjectTaskController {

    private final ProjectTaskService taskService;

    public ProjectTaskController(ProjectTaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "WBS 看板（任务树 + 依赖 + 时间范围 + 汇总进度）")
    @GetMapping("/prj/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectTaskDtos.TaskBoard> board(@PathVariable Long projectId) {
        return Result.ok(taskService.board(projectId, CurrentUser.companyId()));
    }

    @Operation(summary = "项目日历（与区间相交的已排期任务）")
    @GetMapping("/prj/projects/{projectId}/calendar")
    @PreAuthorize("hasAuthority('prj.project.read')")
    public Result<ProjectTaskDtos.CalendarBoard> calendar(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return Result.ok(taskService.calendar(projectId, from, to, CurrentUser.companyId()));
    }

    @Operation(summary = "拖拽改期（级联平移子树，不动祖先与依赖）")
    @PostMapping("/prj/tasks/{taskId}/reschedule")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_RESCHEDULE", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> reschedule(
            @PathVariable Long taskId,
            @Valid @RequestBody ProjectTaskDtos.TaskRescheduleReq req) {
        return Result.ok(taskService.reschedule(taskId, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "新建任务（WBS 编码服务端生成）")
    @PostMapping("/prj/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_CREATE", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> createTask(@PathVariable Long projectId,
                                                        @Valid @RequestBody ProjectTaskDtos.TaskSaveReq req) {
        return Result.ok(taskService.createTask(projectId, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改任务（进度变化会触发项目进度汇总）")
    @PutMapping("/prj/tasks/{taskId}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_UPDATE", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> updateTask(@PathVariable Long taskId,
                                                        @Valid @RequestBody ProjectTaskDtos.TaskSaveReq req) {
        return Result.ok(taskService.updateTask(taskId, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除任务（有子任务 409，连带清依赖）")
    @DeleteMapping("/prj/tasks/{taskId}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_DELETE", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> deleteTask(@PathVariable Long taskId) {
        return Result.ok(taskService.deleteTask(taskId, CurrentUser.companyId()));
    }

    @Operation(summary = "WBS 编码重排（按排序号重新生成 1 / 1.2 / 1.2.3）")
    @PostMapping("/prj/projects/{projectId}/tasks/resequence")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_RESEQUENCE", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> resequence(@PathVariable Long projectId) {
        return Result.ok(taskService.resequence(projectId, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "新建任务依赖（FS/SS/FF/SF，自动防环）")
    @PostMapping("/prj/projects/{projectId}/task-dependencies")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_DEP_CREATE", objectType = "PrjTaskDep")
    public Result<ProjectTaskDtos.TaskBoard> createDependency(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectTaskDtos.DependencySaveReq req) {
        return Result.ok(taskService.createDependency(projectId, req, CurrentUser.companyId()));
    }

    @Operation(summary = "删除任务依赖")
    @DeleteMapping("/prj/task-dependencies/{dependencyId}")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_DEP_DELETE", objectType = "PrjTaskDep")
    public Result<ProjectTaskDtos.TaskBoard> deleteDependency(@PathVariable Long dependencyId) {
        return Result.ok(taskService.deleteDependency(dependencyId, CurrentUser.companyId()));
    }

    // ---------------------------------------------------------------- 执行闭环（P7-3）

    @Operation(summary = "派工（设置执行人）")
    @PostMapping("/prj/tasks/{taskId}/assign")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_ASSIGN", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> assign(@PathVariable Long taskId,
                                                    @Valid @RequestBody ProjectTaskDtos.TaskAssignReq req) {
        return Result.ok(taskService.assign(taskId, req.ownerId(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "接收任务（执行人或项目经理）")
    @PostMapping("/prj/tasks/{taskId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TASK_ACCEPT", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> accept(@PathVariable Long taskId) {
        return Result.ok(taskService.accept(taskId, CurrentUser.companyId(), CurrentUser.userId(),
                canManage()));
    }

    @Operation(summary = "开始执行（未开始 → 进行中）")
    @PostMapping("/prj/tasks/{taskId}/start")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TASK_START", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> start(@PathVariable Long taskId) {
        return Result.ok(taskService.start(taskId, CurrentUser.companyId(), CurrentUser.userId(),
                canManage()));
    }

    @Operation(summary = "提交审核（进行中 → 已提交）")
    @PostMapping("/prj/tasks/{taskId}/submit")
    @PreAuthorize("isAuthenticated()")
    @Audit(module = "PRJ", action = "TASK_SUBMIT", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> submit(@PathVariable Long taskId,
                                                    @Valid @RequestBody(required = false)
                                                    ProjectTaskDtos.TaskSubmitReq req) {
        return Result.ok(taskService.submit(taskId, req == null ? null : req.note(),
                CurrentUser.companyId(), CurrentUser.userId(), canManage()));
    }

    @Operation(summary = "审核通过（已提交 → 已完成，进度 100%）")
    @PostMapping("/prj/tasks/{taskId}/approve")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_APPROVE", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> approve(@PathVariable Long taskId) {
        return Result.ok(taskService.approve(taskId, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "打回（已提交 → 进行中，记录原因）")
    @PostMapping("/prj/tasks/{taskId}/reject")
    @PreAuthorize("hasAuthority('prj.project.write')")
    @Audit(module = "PRJ", action = "TASK_REJECT", objectType = "PrjTask")
    public Result<ProjectTaskDtos.TaskBoard> reject(@PathVariable Long taskId,
                                                    @Valid @RequestBody ProjectTaskDtos.TaskRejectReq req) {
        return Result.ok(taskService.reject(taskId, req.reason(), CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "我的任务（按执行人跨项目，status 缺省查未完成）")
    @GetMapping("/prj/my-tasks")
    @PreAuthorize("isAuthenticated()")
    public Result<java.util.List<ProjectTaskDtos.MyTaskRow>> myTasks(
            @RequestParam(required = false) String status) {
        return Result.ok(taskService.myTasks(CurrentUser.companyId(), CurrentUser.userId(), status));
    }

    /** 当前登录人是否具备项目经理权限（服务端据此放宽「只能操作自己任务」的限制）。 */
    private boolean canManage() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "prj.project.write".equals(authority.getAuthority()));
    }
}
