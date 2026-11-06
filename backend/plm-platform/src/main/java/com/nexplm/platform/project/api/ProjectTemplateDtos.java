package com.nexplm.platform.project.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P7-6 项目模板 DTO。 */
public final class ProjectTemplateDtos {

    private ProjectTemplateDtos() {
    }

    /** 模板列表行（带任务/目录数量，便于列表直接看出模板含量）。 */
    public record TemplateRow(Long id, String code, String name, String type, boolean enabled,
                              String remark, int taskCount, int folderCount,
                              OffsetDateTime updatedAt) {
    }

    /** 模板任务行：{@code wbsCode} 由服务端按数组顺序生成，{@code parentId} 是模板内父任务。 */
    public record TemplateTaskRow(Long id, Long parentId, String wbsCode, String name, String type,
                                  int startOffset, int durationDays, int sortNo, String remark) {
    }

    /** 目录骨架：{@code parentPath} 是模板内的逻辑路径（根为 /），立项时才换成物化路径。 */
    public record TemplateFolderRow(Long id, String parentPath, String name, int sortNo) {
    }

    public record TemplateDetail(Long id, String code, String name, String type, boolean enabled,
                                 String remark,
                                 List<TemplateTaskRow> tasks, List<TemplateFolderRow> folders) {
    }

    public record TemplateSaveReq(@NotBlank @Size(max = 200) String name,
                                  @Size(max = 32) String type,
                                  Boolean enabled,
                                  @Size(max = 500) String remark) {
    }

    /**
     * 任务整表替换。
     *
     * <p>不传 {@code wbsCode}/{@code sortNo}：编码按数组顺序生成（{@code parentWbsCode} 指定父级），
     * 顺序即数组顺序——前端拖一下数组就行，不必自己算编码。</p>
     */
    public record TemplateTaskSaveReq(@Size(max = 64) String parentWbsCode,
                                      @NotBlank @Size(max = 200) String name,
                                      @Size(max = 16) String type,
                                      Integer startOffset,
                                      Integer durationDays,
                                      @Size(max = 500) String remark) {
    }

    public record TemplateFolderSaveReq(@Size(max = 500) String parentPath,
                                        @NotBlank @Size(max = 200) String name,
                                        Integer sortNo) {
    }

    /** 从模板立项：{@code planStart} 必填，模板里的「偏移」全靠它落成真实日期。 */
    public record FromTemplateReq(@NotNull Long templateId,
                                  @NotBlank @Size(max = 200) String name,
                                  Long epsId,
                                  Long managerId,
                                  @NotNull LocalDate planStart,
                                  Integer priority,
                                  @Size(max = 500) String remark) {
    }

    /** 任务 + 目录批量保存入参（两个接口共用同一份请求体形状）。 */
    public record TemplateTasksReq(@Valid List<TemplateTaskSaveReq> tasks) {
    }

    public record TemplateFoldersReq(@Valid List<TemplateFolderSaveReq> folders) {
    }
}
