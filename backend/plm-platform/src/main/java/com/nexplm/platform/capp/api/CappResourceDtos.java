package com.nexplm.platform.capp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;

/** P6-3 工艺资源库 DTO。 */
public final class CappResourceDtos {

    private CappResourceDtos() {
    }

    /** 资源分类行（扁平返回，前端按 parentId 组树）。 */
    public record ClassRow(Long id, Long parentId, String code, String name, int sortNo,
                           String remark, int resourceCount) {
    }

    public record ClassSaveReq(@NotBlank @Size(max = 64) String code,
                               @NotBlank @Size(max = 200) String name,
                               Long parentId,
                               Integer sortNo,
                               @Size(max = 500) String remark) {
    }

    /** 资源行；{@code attrs} 为扁平动态属性（值统一字符串）。 */
    public record ResourceRow(Long id, Long classId, String classCode, String className,
                              String code, String name, String spec, String unit, String status,
                              Map<String, String> attrs, String remark, int referenceCount,
                              OffsetDateTime updatedAt) {
    }

    public record ResourceSaveReq(@NotNull Long classId,
                                  @NotBlank @Size(max = 64) String code,
                                  @NotBlank @Size(max = 200) String name,
                                  @Size(max = 500) String spec,
                                  @Size(max = 32) String unit,
                                  @Size(max = 16) String status,
                                  Map<String, String> attrs,
                                  @Size(max = 500) String remark) {
    }

    /** 资源引用行：哪条工艺路线的哪道工序用了它。 */
    public record ResourceReferenceRow(Long routingId, String routingCode, String routingName,
                                       Long objectId, String objectCode, int seqNo, String stepName) {
    }

    /** 保存的查询条件。 */
    public record QueryRow(Long id, String name, Long classId, String keyword,
                           Map<String, String> attrs, OffsetDateTime createdAt) {
    }

    public record QuerySaveReq(@NotBlank @Size(max = 100) String name,
                               Long classId,
                               @Size(max = 200) String keyword,
                               Map<String, String> attrs) {
    }
}
