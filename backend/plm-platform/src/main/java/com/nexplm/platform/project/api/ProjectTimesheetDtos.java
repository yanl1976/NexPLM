package com.nexplm.platform.project.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P7-5 工时填报与审批 DTO。 */
public final class ProjectTimesheetDtos {

    private ProjectTimesheetDtos() {
    }

    /** 工时行（带项目/任务/填报人/审批人名称，列表直接展示，不必二次查询）。 */
    public record TimesheetRow(Long id, Long projectId, String projectCode, String projectName,
                               Long taskId, String taskWbsCode, String taskName,
                               Long userId, String userName, LocalDate workDate,
                               BigDecimal hours, String workType, String status, String remark,
                               OffsetDateTime submittedAt, String approverName,
                               OffsetDateTime approvedAt, String rejectReason,
                               OffsetDateTime createdAt) {
    }

    /** 填报/修改：任务必须属于当前项目（服务端从 task 反查 project，不接受前端传 projectId）。 */
    public record TimesheetSaveReq(@NotNull Long taskId,
                                   @NotNull LocalDate workDate,
                                   @NotNull @DecimalMin("0.5") @DecimalMax("24") BigDecimal hours,
                                   @Size(max = 16) String workType,
                                   @Size(max = 500) String remark) {
    }

    /** 分页结果；{@code totalHours} 是当前过滤条件下的总工时（用于页面汇总）。 */
    public record TimesheetPage(List<TimesheetRow> items, long total, int page, int size,
                               BigDecimal totalHours) {
    }

    public record UserHoursRow(Long userId, String userName,
                               BigDecimal approvedHours, BigDecimal pendingHours) {
    }

    public record TaskHoursRow(Long taskId, String wbsCode, String taskName,
                               BigDecimal approvedHours, BigDecimal pendingHours) {
    }

    /** 周起始（周一）。 */
    public record WeekHoursRow(LocalDate weekStart, BigDecimal approvedHours) {
    }

    /** 项目工时汇总：实际工时只认 APPROVED，SUBMITTED 单列为待审批。 */
    public record TimesheetSummary(Long projectId, String projectCode, String projectName,
                                   LocalDate from, LocalDate to,
                                   BigDecimal approvedHours, BigDecimal pendingHours,
                                   BigDecimal rejectedHours,
                                   int approvedCount, int pendingCount,
                                   List<UserHoursRow> byUser, List<TaskHoursRow> byTask,
                                   List<WeekHoursRow> byWeek) {
    }

    /** 资源负荷（简化）：某人一周的已审批工时，前端按 40h/周算负荷率。 */
    public record ResourceLoadRow(Long userId, String userName, BigDecimal approvedHours,
                                  List<WeekHoursRow> weeks) {
    }

    public record ResourceLoad(Long projectId, LocalDate from, LocalDate to, int weekCount,
                               BigDecimal capacityHours, List<ResourceLoadRow> rows) {
    }

    /** 批量审批：整批原子，任何一条不合法就整批回滚（避免"部分通过"对不上账）。 */
    public record BatchApproveReq(@NotEmpty List<Long> ids) {
    }

    /** 打回：必须写原因。 */
    public record TimesheetRejectReq(@jakarta.validation.constraints.NotBlank @Size(max = 500)
                                     String reason) {
    }
}
