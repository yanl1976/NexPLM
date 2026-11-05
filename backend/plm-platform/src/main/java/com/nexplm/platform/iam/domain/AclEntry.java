package com.nexplm.platform.iam.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对象实例 ACL（等价旧系统 OBJECTPERMISSION）。
 * actorType: USER / ROLE / GROUP / GROUP_ROLE / CREATOR / OWNER / ALL。
 */
@Data
@TableName("sys_acl_entry")
public class AclEntry {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long objectId;
    private String actorType;
    private Long actorId;
    private String actions;
    private String effect;
    private OffsetDateTime expireAt;
    private String source;
    private Long sourceObjectId;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
