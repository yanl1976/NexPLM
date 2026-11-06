package com.nexplm.platform.workflow.api;

import com.nexplm.platform.workflow.service.assignee.AssigneeSpec;
import jakarta.validation.constraints.NotBlank;
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

    /**
     * 发起流程。
     * <p>初审人可传 {@code approverId} 或 {@code initialAssignees}（执行人声明，解析后必须恰好 1 人）；
     * 会签人可传 {@code approverIds} 或 {@code countersignAssignees}（可解析出多人，全票通过）。
     * 两种写法二选一，同时传时以执行人声明为准。</p>
     */
    public record StartReq(
            @NotBlank(message = "对象类型不能为空") String objectType,
            @NotNull(message = "对象 id 不能为空") Long objectId,
            String processKey,
            Long approverId,
            List<Long> approverIds,
            List<AssigneeSpec> initialAssignees,
            List<AssigneeSpec> countersignAssignees,
            Map<String, Object> variables,
            @Size(max = 500) String remark) {
    }

    /** 执行人解析类型（供前端渲染选择器）。 */
    public record AssigneeTypeRow(String type, String label) {
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

    /** 加签：给当前会签节点再追加一名审批人（只对多实例节点有效）。 */
    public record AddSignerReq(@NotNull(message = "加签人不能为空") Long signerId,
                               @Size(max = 2000) String comment) {
    }

    /** 委托规则：owner 在生效区间内把待办委托给 delegate（processKey 为空=全部流程）。 */
    public record DelegateReq(Long ownerId,
                              @NotNull(message = "受托人不能为空") Long delegateId,
                              @NotNull(message = "生效时间不能为空") OffsetDateTime startAt,
                              @NotNull(message = "失效时间不能为空") OffsetDateTime endAt,
                              String processKey,
                              @Size(max = 500) String remark) {
    }

    public record DelegateRow(Long id, Long ownerId, String ownerName, Long delegateId, String delegateName,
                              OffsetDateTime startAt, OffsetDateTime endAt, String processKey,
                              Boolean enabled, String remark) {
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
