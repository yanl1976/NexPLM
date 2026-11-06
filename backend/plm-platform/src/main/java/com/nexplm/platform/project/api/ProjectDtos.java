package com.nexplm.platform.project.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P7 项目与任务 DTO（P7-1：项目主档 + EPS + 成员）。 */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    // ---------------------------------------------------------------- EPS

    /** EPS 树节点（{@code projectCount} 只统计直挂本节点的项目，不含子树）。 */
    public record EpsNode(Long id, Long parentId, String code, String name, String path, int sortNo,
                          String remark, int projectCount, List<EpsNode> children) {
    }

    /** EPS 保存入参：{@code parentId} 为空表示根节点。 */
    public record EpsSaveReq(@NotBlank @Size(max = 64) String code,
                             @NotBlank @Size(max = 200) String name,
                             Long parentId,
                             Integer sortNo,
                             @Size(max = 500) String remark) {
    }

    // ---------------------------------------------------------------- 成员

    public record MemberRow(Long userId, String username, String displayName, String role) {
    }

    public record MemberSaveReq(@NotNull Long userId, @Size(max = 32) String role) {
    }

    // ---------------------------------------------------------------- 项目

    /** 项目列表行。 */
    public record ProjectRow(Long id, String code, String name, Long epsId, String epsName,
                             Long managerId, String managerName, String type, String status,
                             int priority, LocalDate planStart, LocalDate planEnd,
                             LocalDate actualStart, LocalDate actualEnd, int progress,
                             int memberCount,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 项目详情（多带备注与成员清单）。 */
    public record ProjectDetail(Long id, String code, String name, Long epsId, String epsName,
                                Long managerId, String managerName, String type, String status,
                                int priority, LocalDate planStart, LocalDate planEnd,
                                LocalDate actualStart, LocalDate actualEnd, int progress,
                                String remark,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt,
                                List<MemberRow> members) {
    }

    /**
     * 项目保存入参。
     *
     * <p>创建即草稿（{@code DRAFT}），状态只能经 activate/pause/close 动作迁移，
     * 因此这里没有 {@code status} 字段——避免前端直接改状态绕过校验。</p>
     */
    public record ProjectSaveReq(@NotBlank @Size(max = 200) String name,
                                 Long epsId,
                                 Long managerId,
                                 @Size(max = 32) String type,
                                 @Min(1) @Max(5) Integer priority,
                                 LocalDate planStart,
                                 LocalDate planEnd,
                                 @Min(0) @Max(100) Integer progress,
                                 @Size(max = 500) String remark,
                                 @Valid List<MemberSaveReq> members) {
    }
}
