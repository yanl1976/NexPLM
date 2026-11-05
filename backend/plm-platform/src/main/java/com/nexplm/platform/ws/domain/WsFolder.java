package com.nexplm.platform.ws.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 工作空间文件夹。{@code path} 为物化路径（/{id}/{id}/），便于子树查询与前缀重写。
 */
@Data
@TableName("ws_folder")
public class WsFolder {

    public static final String SPACE_MY = "MY";
    public static final String SPACE_PUBLIC = "PUBLIC";
    public static final String SPACE_GROUP = "GROUP";

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long companyId;
    private Long orgId;
    private String spaceType;
    private Long parentId;
    private String path;
    private String name;
    private Long ownerId;
    private Long groupId;
    private Boolean aclInherit;
    private Integer sort;
    @TableLogic
    private Boolean deleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
