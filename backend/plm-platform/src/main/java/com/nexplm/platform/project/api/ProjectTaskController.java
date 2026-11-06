package com.nexplm.platform.project.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.project.service.ProjectTaskService;
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

/** WBS 任务与依赖（P7-2）。 */
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
}
