package com.nexplm.platform.project.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** P7-4 任务交付物与齐套性检查 DTO。 */
public final class ProjectDeliverableDtos {

    private ProjectDeliverableDtos() {
    }

    /**
     * 交付物行（对象信息实时回查，不存快照）。
     *
     * <p>{@code state} 是对象自身的状态：{@code OK} 已发布、{@code UNRELEASED} 存在但未发布、
     * {@code MISSING} 已被删除、{@code OBSOLETE} 已废弃。{@code ready} 只表示「这项成果可用」，
     * 是否拖累齐套还要看 {@code outputType/required}。</p>
     */
    public record DeliverableRow(Long id, Long taskId, String taskWbsCode, String taskName,
                                 String outputType, String objectType, Long objectId,
                                 String code, String name, String lifecycleState,
                                 String state, boolean required, boolean ready,
                                 String remark) {
    }

    /** 新增交付物：{@code outputType} 缺省 OUTPUT，{@code required} 缺省 true。 */
    public record DeliverableSaveReq(@NotBlank @Size(max = 16) String objectType,
                                     @NotNull Long objectId,
                                     @Size(max = 8) String outputType,
                                     Boolean required,
                                     @Size(max = 500) String remark) {
    }

    /** 修改交付物：只允许改方向/是否必需/备注，换对象请删了重建（唯一索引约束）。 */
    public record DeliverableUpdateReq(@Size(max = 8) String outputType,
                                       Boolean required,
                                       @Size(max = 500) String remark) {
    }

    /** 任务齐套行：{@code items} 含输入与输出全部交付物。 */
    public record TaskCompletenessRow(Long taskId, String wbsCode, String taskName, String status,
                                      int requiredCount, int readyCount,
                                      int missingCount, int unreleasedCount,
                                      boolean ready, List<DeliverableRow> items) {
    }

    /**
     * 项目齐套看板。
     *
     * <p>{@code ready = missingTotal == 0}——只把「必需输出项缺失/废弃」当不齐套，
     * 「未发布」计入 {@code unreleasedTotal} 但不算失败。</p>
     */
    public record DeliverableBoard(Long projectId, String projectCode, String projectName,
                                   int requiredTotal, int readyTotal,
                                   int missingTotal, int unreleasedTotal,
                                   boolean ready, List<TaskCompletenessRow> tasks) {
    }

    /** 可选对象（添加交付物时的候选列表）。 */
    public record CandidateRow(String objectType, Long objectId, String code, String name,
                               String lifecycleState) {
    }
}
