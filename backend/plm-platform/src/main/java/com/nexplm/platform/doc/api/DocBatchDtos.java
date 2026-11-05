package com.nexplm.platform.doc.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 批量操作 DTO。
 */
public final class DocBatchDtos {

    private DocBatchDtos() {
    }

    public record BatchDownloadReq(
            @NotEmpty(message = "请选择要下载的文档")
            @Size(max = 200, message = "单次最多下载 200 个文档") List<Long> ids) {
    }

    /** 批量授权：按同一被授权者与操作集合批量下发。 */
    public record BatchGrantReq(
            @NotEmpty(message = "请选择要授权的文档")
            @Size(max = 200, message = "单次最多 200 个文档") List<Long> ids,
            @NotBlank(message = "被授权者类型不能为空") String actorType,
            Long actorId,
            @NotBlank(message = "操作集合不能为空") String actions,
            String effect,
            OffsetDateTime expireAt) {
    }

    /** 逐条结果：单条失败不影响其它文档。 */
    public record BatchGrantResult(Long id, Boolean ok, String message) {
    }
}
