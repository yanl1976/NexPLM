package com.nexplm.platform.doc.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexplm.platform.support.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 保存的高级查询条件（个人或共享）。
 */
@Data
@TableName(value = "doc_saved_query", autoResultMap = true)
public class DocSavedQuery {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String entityCode;
    private Long ownerId;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String conditionJson;
    private Boolean shared;
    @TableLogic
    private Boolean deleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
