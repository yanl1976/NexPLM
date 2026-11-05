package com.nexplm.platform.lifecycle.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexplm.platform.support.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 生命周期定义（等价旧系统 .life 文件）。
 */
@Data
@TableName(value = "meta_lifecycle", autoResultMap = true)
public class MetaLifecycle {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String type;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String definitionJson;
    private Integer versionNo;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
