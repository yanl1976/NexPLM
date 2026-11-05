package com.nexplm.platform.doc.api;

import java.time.OffsetDateTime;

/**
 * 文档预览 DTO。
 */
public final class DocPreviewDtos {

    private DocPreviewDtos() {
    }

    /**
     * 预览状态。
     * <p>status：NONE 未生成、PENDING 处理中、READY 就绪、FAILED 失败可重试、SKIPPED 环境不支持。</p>
     */
    public record PreviewStatus(Long id, Long objectId, Long sourceFileId, Long previewFileId,
                                String format, String status, String message, Integer pageCount,
                                String fileName, String converter, OffsetDateTime updatedAt) {
    }
}
