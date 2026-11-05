package com.nexplm.platform.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文档分类（树）：同一对象类型内以 code 唯一，path 为「/父编码/子编码」枚举。
 */
@Data
@TableName("doc_class")
public class DocClass {

    @TableId
    private Long id;
    private Long parentId;
    private String entityCode;
    private String code;
    private String name;
    private String path;
    private Integer sort;
    private String status;
    private String remark;

    @Version
    private Integer version;
    private Boolean deleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
