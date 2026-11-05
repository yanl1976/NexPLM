package com.nexplm.platform.doc.api;

import com.nexplm.platform.file.service.FileService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 图文档 DTO。
 */
public final class DocDtos {

    private DocDtos() {
    }

    /** 新建/修改请求。创建时 code 可空（自动取号），修改时忽略 code。 */
    public record DocSaveReq(
            @Size(max = 128, message = "编码不能超过 128 个字符") String code,
            @NotBlank(message = "文档名称不能为空") @Size(max = 500) String name,
            @NotBlank(message = "文档类型不能为空") String docType,
            String source,
            @Size(max = 500) String keyword,
            @Size(max = 4000) String description,
            Long orgId,
            Integer securityLevel) {
    }

    public record DocSummary(Long id, String code, String name, String docType, String source,
                             String lifecycleState, Integer revision, Integer iteration,
                             String revisionLabel, Integer fileCount, String fileExt,
                             Long ownerId, OffsetDateTime updatedAt) {
    }

    public record CheckoutInfo(Long userId, String userLabel, OffsetDateTime checkedOutAt,
                               OffsetDateTime expireAt, Boolean expired) {
    }

    public record DocDetail(Long id, String code, String name, String docType, String source,
                            String keyword, String description,
                            String lifecycleState, String phaseState, String workflowState,
                            Integer revision, Integer iteration, String revisionLabel,
                            Boolean isLatest, Long masterId, Long ownerId, Long orgId,
                            Integer securityLevel, Long mainFileId, String fileExt,
                            Integer fileCount, Integer pageCount,
                            CheckoutInfo checkout, List<FileService.FileView> files,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 版本历史条目。 */
    public record DocVersion(Long id, Integer revision, Integer iteration, String revisionLabel,
                             String lifecycleState, Boolean isLatest,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record CheckoutReq(@Size(max = 500) String remark) {
    }

    /** 检入请求：可顺带更新页数与说明。 */
    public record CheckinReq(Integer pageCount, @Size(max = 500) String remark) {
    }
}
