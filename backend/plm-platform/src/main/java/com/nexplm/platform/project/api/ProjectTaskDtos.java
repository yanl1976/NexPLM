package com.nexplm.platform.project.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** P7-2 WBS 任务与依赖 DTO。 */
public final class ProjectTaskDtos {

    private ProjectTaskDtos() {
    }

    /** 任务节点（{@code children} 为 WBS 子树，叶子任务 {@code childCount = 0}）。 */
    public record TaskRow(Long id, Long projectId, Long parentId, String wbsCode, String name,
                          String type, String status, int sortNo,
                          LocalDate planStart, LocalDate planEnd,
                          LocalDate actualStart, LocalDate actualEnd,
                          Integer durationDays, int progress,
                          Long ownerId, String ownerName, String remark, int childCount,
                          List<TaskRow> children) {
    }

    /**
     * 任务保存入参。
     *
     * <p>{@code wbsCode} 不在入参里——编码由服务端按「父节点下最大序号 +1」生成，
     * 前端改了顺序用 {@code tasks/resequence} 统一重排，避免手填编码撞唯一索引。</p>
     */
    public record TaskSaveReq(@NotBlank @Size(max = 200) String name,
                              Long parentId,
                              @Size(max = 16) String type,
                              @Size(max = 16) String status,
                              Integer sortNo,
                              LocalDate planStart,
                              LocalDate planEnd,
                              LocalDate actualStart,
                              LocalDate actualEnd,
                              Integer durationDays,
                              @Min(0) @Max(100) Integer progress,
                              Long ownerId,
                              @Size(max = 500) String remark) {
    }

    /** 依赖行（带任务名，甘特图直接画箭头用，不必再查一次任务）。 */
    public record DependencyRow(Long id, Long predTaskId, Long succTaskId, String depType, int lagDays,
                                String predName, String succName) {
    }

    /** 依赖保存入参：{@code depType} 缺省 FS。 */
    public record DependencySaveReq(@NotNull Long predTaskId,
                                    @NotNull Long succTaskId,
                                    @Size(max = 2) String depType,
                                    Integer lagDays) {
    }

    /**
     * WBS 看板：任务树 + 依赖 + 时间范围 + 项目汇总进度。
     *
     * <p>{@code rangeStart/rangeEnd} 取所有任务计划日期的并集（甘特图时间轴范围），
     * 没有日期时为空，前端自行兜底为今天起 30 天。</p>
     */
    public record TaskBoard(Long projectId, String projectCode, String projectName,
                            int projectProgress, LocalDate rangeStart, LocalDate rangeEnd,
                            List<TaskRow> tasks, List<DependencyRow> dependencies) {
    }
}
