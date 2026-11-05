package com.nexplm.platform.doc.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文档预览登记：源文件 → 预览产物 + 状态。
 */
@Data
@TableName("doc_preview")
public class DocPreview {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long objectId;
    private Long sourceFileId;
    private Long previewFileId;
    private String format;
    private String status;
    private String message;
    private Integer pageCount;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
