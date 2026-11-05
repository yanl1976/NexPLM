package com.nexplm.platform.iam.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 组织节点（公司下可为工厂/部门/组）。path 形如 /1/12/1203/ 用于子树查询。
 */
@Data
@TableName("sys_org")
public class SysOrg {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long companyId;
    private Long parentId;
    private String path;
    private String code;
    private String name;
    private String orgType;
    private Integer sort;
    private Long leaderId;
    private String status;
    private String extJson;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
