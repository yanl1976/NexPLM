package com.nexplm.platform.quality.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** P8-3-1 IATF16949 体系文件：模板 + 文件夹 + 版本 + 审批发布。 */
public final class SysDocDtos {

    private SysDocDtos() {
    }

    // ---------------------------------------------------------------- 文件夹

    /** 扁平返回（前端自行组树），{@code path} 用于子树过滤，{@code docCount} 是直接挂载的文件数。 */
    public record FolderRow(Long id, Long parentId, String code, String name, String path,
                            int sortNo, int docCount) {
    }

    /** {@code code} 创建后不可改（path 由它拼成，改 code 要重算整棵子树）。 */
    public record FolderSaveReq(@NotBlank @Size(max = 64) String code,
                               @NotBlank @Size(max = 200) String name,
                               Long parentId,
                               Integer sortNo) {
    }

    // ---------------------------------------------------------------- 模板

    /** 章节骨架：{@code key} 模板内唯一，{@code required} 的章节提交审批前必须填内容。 */
    public record SectionTemplate(@Size(max = 64) String key,
                                  @Size(max = 200) String title,
                                  @Size(max = 500) String hint,
                                  Boolean required) {
    }

    public record TemplateRow(Long id, String code, String name, String docLevel, Long folderId,
                              String folderName, int defaultReviewMonths, boolean enabled,
                              int sectionCount, OffsetDateTime updatedAt) {
    }

    public record TemplateDetail(Long id, String code, String name, String docLevel, Long folderId,
                                 String folderName, int defaultReviewMonths, boolean enabled,
                                 String remark, List<SectionTemplate> sections) {
    }

    public record TemplateSaveReq(@NotBlank @Size(max = 200) String name,
                                  @Size(max = 16) String docLevel,
                                  Long folderId,
                                  Integer defaultReviewMonths,
                                  Boolean enabled,
                                  @Size(max = 500) String remark,
                                  List<SectionTemplate> sections) {
    }

    // ---------------------------------------------------------------- 文件

    public record DocRow(Long id, String code, String name, String docLevel, Long folderId,
                         String folderName, Long templateId, String templateCode,
                         int versionNo, String status, Long ownerId, String ownerName,
                         LocalDate effectiveDate, LocalDate nextReviewDate, int reviewMonths,
                         boolean dueReview, OffsetDateTime updatedAt) {
    }

    public record DocSection(String key, String title, String content, Boolean required) {
    }

    public record VersionRow(Long id, int versionNo, String changeNote, String status,
                             OffsetDateTime releasedAt, String releasedByName, int sectionCount) {
    }

    public record DocDetail(Long id, String code, String name, String docLevel, Long folderId,
                            String folderName, Long templateId, String templateCode,
                            int versionNo, String status, Long ownerId, String ownerName,
                            int reviewMonths, LocalDate effectiveDate, LocalDate nextReviewDate,
                            String summary, Long fileId, String approveNote,
                            OffsetDateTime approvedAt, OffsetDateTime obsoleteAt,
                            List<DocSection> sections, List<VersionRow> versions) {
    }

    public record DocSaveReq(@NotBlank @Size(max = 64) String code,
                             @NotBlank @Size(max = 200) String name,
                             @Size(max = 16) String docLevel,
                             Long folderId,
                             Long ownerId,
                             Integer reviewMonths,
                             @Size(max = 1000) String summary,
                             List<DocSection> sections) {
    }

    /** 按模板生成：模板给层级/文件夹/评审周期/章节骨架，编号与名称由业务填。 */
    public record DocFromTemplateReq(@NotNull Long templateId,
                                     @NotBlank @Size(max = 64) String code,
                                     @NotBlank @Size(max = 200) String name,
                                     Long ownerId) {
    }

    /** 审批（通过/驳回）：驳回必须写原因，否则编制人不知道要改什么。 */
    public record DocApproveReq(@NotNull Boolean passed, @Size(max = 1000) String note) {
    }

    /** 修订：新版本号 + 修订说明（写进新版本的 change_note）。 */
    public record DocReviseReq(@Size(max = 1000) String changeNote) {
    }
}
