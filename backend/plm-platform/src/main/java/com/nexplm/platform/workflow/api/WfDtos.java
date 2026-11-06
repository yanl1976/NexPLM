package com.nexplm.platform.workflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流 DTO。
 */
public final class WfDtos {

    private WfDtos() {
    }

    /** 发起流程：approver 为初审人，approverIds 为会签人（需全部签核）。 */
    public record StartReq(
            @NotBlank(message = "对象类型不能为空") String objectType,
            @NotNull(message = "对象 id 不能为空") Long objectId,
            String processKey,
            @NotNull(message = "初审人不能为空") Long approverId,
            @NotEmpty(message = "会签人不能为空") List<Long> approverIds,
            @Size(max = 500) String remark) {
    }

    public record StartResp(String processInstanceId, String processKey, String businessKey,
                            List<TaskRow> activeTasks) {
    }

    /** 待办行：附带业务对象信息，前端可直接展示与跳转。 */
    public record TaskRow(String taskId, String taskKey, String name, String assignee,
                          String processInstanceId, String objectType, Long objectId,
                          String objectCode, String objectName, OffsetDateTime createTime) {
    }

    public record CompleteReq(@Size(max = 2000) String comment, Map<String, Object> variables) {
    }

    public record RejectReq(@NotBlank(message = "驳回目标节点不能为空") String targetActivityId,
                            @Size(max = 2000) String comment) {
    }

    public record WithdrawReq(@Size(max = 500) String reason) {
    }

    public record CommentRow(String taskId, String taskKey, String action, String comment,
                             Long userId, OffsetDateTime createdAt) {
    }

    public record InstanceDetail(String processInstanceId, String processKey, String businessKey,
                                 String objectType, Long objectId, String objectCode, String objectName,
                                 String status, Long startedBy, OffsetDateTime startedAt, OffsetDateTime endedAt,
                                 List<TaskRow> activeTasks, List<CommentRow> comments) {
    }

    public record PageResult<T>(long total, List<T> items) {
    }

    public record BindingRow(Long id, String objectType, String category, String processKey,
                             String name, Integer priority, Boolean enabled) {
    }
}
