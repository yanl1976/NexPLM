package com.nexplm.platform.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexplm.platform.support.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 字段元数据（等价旧系统 .model 的 InfoType）。
 * valueType 是语义类型（31 种之一），dataType 是物理类型；客户化字段可落到 plm_object.ext_json。
 */
@Data
@TableName(value = "meta_field", autoResultMap = true)
public class MetaField {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String entityCode;
    private String code;
    private String name;
    private String nameEn;
    private String valueType;
    private String dataType;
    private Integer length;
    private Integer scale;
    private Boolean required;
    private String defaultValue;
    private String enumCode;
    private String refEntity;
    private String refField;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String uiJson;
    private Integer securityLevel;
    private Boolean indexed;
    private Boolean searchable;
    private Integer sort;
    private Integer versionNo;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
