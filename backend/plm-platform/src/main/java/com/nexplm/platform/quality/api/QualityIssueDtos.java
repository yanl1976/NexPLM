package com.nexplm.platform.quality.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P8-1 质量问题闭环 DTO。 */
public final class QualityIssueDtos {

    private QualityIssueDtos() {
    }

    /** 列表行：带措施计数与超期标记，列表页不必再逐条查措施。 */
    public record IssueRow(Long id, String code, String title, String source, String severity,
                           String status, int roundNo, String objectType, Long objectId,
                           String objectCode, Long projectId, Long supplierId,
                           Long templateId, String templateCode,
                           Long ownerId, String ownerName, String reportedByName,
                           OffsetDateTime reportedAt, LocalDate dueDate, OffsetDateTime closedAt,
                           String verifyNote, int actionCount, int openActionCount, boolean overdue,
                           String remark, OffsetDateTime updatedAt) {
    }

    /** 措施行（{@code roundNo} 让前端按轮次分组展示，不必再算）。 */
    public record ActionRow(Long id, int roundNo, String actionType, String content,
                            Long ownerId, String ownerName, LocalDate dueDate, String status,
                            OffsetDateTime doneAt, OffsetDateTime createdAt) {
    }

    public record IssueDetail(IssueRow issue, List<ActionRow> actions) {
    }

    /**
     * 登记/修改入参。
     *
     * <p>没有 {@code status} 与 {@code roundNo}：状态只能走动作（assign/submit/verify），
     * 轮次由验证不通过时服务端 +1，前端改不了。</p>
     */
    public record IssueSaveReq(@NotBlank @Size(max = 200) String title,
                               @Size(max = 2000) String description,
                               @Size(max = 16) String source,
                               @Size(max = 16) String severity,
                               Long ownerId,
                               LocalDate dueDate,
                               @Size(max = 64) String objectType,
                               Long objectId,
                               @Size(max = 128) String objectCode,
                               Long projectId,
                               Long supplierId,
                               @Size(max = 1000) String remark) {
    }

    /** 分派：责任人必填，期限可顺带调整。 */
    public record IssueAssignReq(@NotNull Long ownerId, LocalDate dueDate) {
    }

    /** 措施：类型缺省纠正措施（CORRECTIVE）。 */
    public record ActionSaveReq(@NotBlank @Size(max = 2000) String content,
                                @Size(max = 16) String actionType,
                                Long ownerId,
                                LocalDate dueDate) {
    }

    /** 验证：{@code passed=false} 时 note 必填（要说清为什么不算整改完成）。 */
    public record IssueVerifyReq(boolean passed, @Size(max = 1000) String note) {
    }
}
