package com.nexplm.platform.iam.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexplm.platform.support.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 规则权限：满足条件树的用户自动获得该对象类型上的一组操作权限（等价旧系统 ACTIONRULE，但支持条件嵌套）。
 */
@Data
@TableName(value = "sys_permission_rule", autoResultMap = true)
public class PermissionRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String objectType;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String exprJson;
    private String actions;
    private Integer priority;
    private String effect;
    private Boolean enabled;
    private String remark;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
