package com.nexplm.platform.doc.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    /**
     * 转换器名称：仅内存态，供 generate 的响应回填，不落库（表无该列）。
     * 若要 status 接口也持久返回，需加列 + 迁移脚本（见 docs/06 EXP-20261108-04 遗留）。
     */
    @TableField(exist = false)
    private String converter;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
