package com.nexplm.platform.iam.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 角色。role_type 区分内置管理员角色（SYS_ADMIN/SEC_ADMIN/AUDITOR）与业务角色。
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long companyId;
    private String code;
    private String name;
    private String roleType;
    private String dataScope;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
