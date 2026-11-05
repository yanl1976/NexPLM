package com.nexplm.platform.doc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 分类属性定义：必填、枚举、可继承（inheritable=true 时子分类可见并可填）。
 */
@Data
@TableName("doc_class_attr")
public class DocClassAttr {

    @TableId
    private Long id;
    private Long classId;
    private String code;
    private String name;
    private String valueType;
    private String enumCode;
    private Boolean required;
    private Boolean inheritable;
    private String defaultValue;
    private Integer sort;
    private Boolean deleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
