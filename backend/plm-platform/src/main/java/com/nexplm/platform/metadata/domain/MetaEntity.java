package com.nexplm.platform.metadata.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexplm.platform.support.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对象类型元数据（等价旧系统 .model 的 EntityType）。
 * capabilities 为能力位 JSON 数组，决定该类型是否参与新建权限、数据隔离、高级查询、规则条件等。
 */
@Data
@TableName(value = "meta_entity", autoResultMap = true)
public class MetaEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String nameEn;
    private String tableName;
    private String parentCode;
    private Boolean abstractFlag;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String capabilities;
    private String versionPolicy;
    private String iterationPolicy;
    private String lifecycleCode;
    private String phaseCode;
    private Boolean workflowAllowed;
    private Integer securityLevel;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String schemaJson;
    private Integer versionNo;
    private String status;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
