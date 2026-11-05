package com.nexplm.platform.iam.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 功能权限点（菜单/按钮级）。code 同时用于后端 @PreAuthorize 与前端 v-perm 指令。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId
    private String code;
    private String module;
    private String action;
    private String name;
    private String nameEn;
    private String permType;
    private String remark;
}
