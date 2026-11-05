package com.nexplm.platform.doc.api;

import com.nexplm.platform.file.service.FileService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
            Integer securityLevel,
            Long classId,
            Map<String, String> classAttrs) {
    }

    public record DocSummary(Long id, String code, String name, String docType, String source,
                             String lifecycleState, Integer revision, Integer iteration,
                             String revisionLabel, Integer fileCount, String fileExt,
                             Long ownerId, OffsetDateTime updatedAt,
                             Long classId, String className) {
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
                            OffsetDateTime createdAt, OffsetDateTime updatedAt,
                            Long classId, String className, Map<String, String> classAttrs) {
    }

    /** 版本历史条目。 */
    public record DocVersion(Long id, Integer revision, Integer iteration, String revisionLabel,
                             String lifecycleState, Boolean isLatest,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    /** 关联对象：按关系双向合并，direction 为 OUT（本对象为源）或 IN（本对象为目标）。 */
    public record RelationView(Long id, String code, String name, String objectType,
                               String lifecycleState, String revisionLabel,
                               String relationType, String direction, Integer seq, BigDecimal qty) {
    }

    /** 操作日志：状态流转（STATE）与审计事件（AUDIT）合并，按时间倒序。 */
    public record LogView(String logType, String action, String fromState, String toState,
                          String detail, String result, Long operatorId, String operatorLabel,
                          String ip, OffsetDateTime createdAt) {
    }

    public record CheckoutReq(@Size(max = 500) String remark) {
    }

    /** 检入请求：可顺带更新页数与说明。 */
    public record CheckinReq(Integer pageCount, @Size(max = 500) String remark) {
    }
}
