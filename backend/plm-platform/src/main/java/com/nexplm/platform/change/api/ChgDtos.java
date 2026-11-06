package com.nexplm.platform.change.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P5 变更管理 DTO。 */
public final class ChgDtos {

    private ChgDtos() {
    }

    /** ECR 列表行。 */
    public record EcrRow(Long id, String code, String title, String changeType, String category,
                         String priority, String status, Long requesterId, String requesterName,
                         Long receiverId, String projectNo, LocalDate targetDate,
                         OffsetDateTime createdAt, int affectedCount) {
    }

    /** ECR 详情：主信息 + 关联对象 + 派生通知。 */
    public record EcrDetail(Long id, String code, String title, String changeType, String category,
                            String reason, String feasibility, String priority, String status,
                            Long requesterId, String requesterName, Long receiverId, String receiverName,
                            String projectNo, LocalDate targetDate, String remark,
                            OffsetDateTime approvedAt, OffsetDateTime closedAt,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt,
                            List<ObjectRow> objects, List<EcnRow> ecns) {
    }

    /** 新建/编辑 ECR。 */
    public record EcrSaveReq(@NotBlank(message = "标题不能为空") @Size(max = 200) String title,
                             @NotBlank(message = "变更类型不能为空") String changeType,
                             @Size(max = 64) String category,
                             @Size(max = 2000) String reason,
                             @Size(max = 1000) String feasibility,
                             @Size(max = 16) String priority,
                             Long receiverId,
                             @Size(max = 64) String projectNo,
                             LocalDate targetDate,
                             @Size(max = 500) String remark) {
    }

    /** 关联对象行（含对象编码与名称，前端直接可读）。 */
    public record ObjectRow(Long id, String objectType, Long objectId, String objectCode, String objectName,
                            String relation, String action, Integer revisionFrom, Integer revisionTo,
                            String execStatus, String remark) {
    }

    /** 新增关联对象。relation 缺省 AFFECTED，此时 action 必填（REVISE/OBSOLETE/RELEASE）。 */
    public record ObjectAddReq(@NotBlank(message = "对象类型不能为空") String objectType,
                               @NotNull(message = "对象 id 不能为空") Long objectId,
                               String relation,
                               String action,
                               @Size(max = 500) String remark) {
    }

    /** 变更通知行。 */
    public record EcnRow(Long id, String code, Long ecrId, String title, String status,
                         Long executorId, String executorName, LocalDate planDate,
                         OffsetDateTime issuedAt, OffsetDateTime closedAt) {
    }

    /** 状态流转请求（后续接流程审批时由引擎回调触发）。 */
    public record StatusReq(@Size(max = 500) String comment) {
    }

    /** 新建变更通知（ECN）。 */
    public record EcnSaveReq(@NotBlank(message = "标题不能为空") @Size(max = 200) String title,
                             Long executorId,
                             LocalDate planDate,
                             @Size(max = 500) String remark) {
    }

    /** 变更问题行。 */
    public record ProblemRow(Long id, String code, Long ecnId, String description, String status,
                             Long ownerId, String ownerName, LocalDate dueDate,
                             OffsetDateTime closedAt, OffsetDateTime createdAt) {
    }

    /** 新建变更问题。 */
    public record ProblemReq(@NotBlank(message = "问题描述不能为空") @Size(max = 2000) String description,
                             Long ownerId,
                             LocalDate dueDate) {
    }

    /** 变更执行结果。 */
    public record ExecuteResult(int total, int succeeded, int skipped, List<String> messages) {
    }

    /**
     * 影响分析行：从「受影响」对象出发的 BOM 向上/向下展开结果。
     * <p>versionLabel 由 revision/iteration 拼装（与列表页版本展示一致）。</p>
     */
    public record ImpactRow(String direction, int depth, String objectType, Long objectId,
                            String code, String name, String lifecycleState,
                            int revision, int iteration, boolean linked) {
    }

    /**
     * 变更追踪时间线事件（申请 → 评审 → 通知 → 执行 → 关闭）。
     * <p>{@code stage} 供前端配色：CREATE / REVIEW / APPROVE / REJECT / WITHDRAW /
     * NOTICE / PROBLEM / EXECUTE / CLOSE。</p>
     */
    public record TimelineEvent(OffsetDateTime time, String stage, String title,
                                String actorName, String detail) {
    }
}
