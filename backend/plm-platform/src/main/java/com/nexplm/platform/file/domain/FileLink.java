package com.nexplm.platform.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对象与文件的关联（角色：主文件/附件/派生预览/派生格式）。
 */
@Data
@TableName("file_link")
public class FileLink {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long fileId;
    private Long ownerObjectId;
    private Integer versionNo;
    private String fileRole;
    private String fileName;
    private Boolean isLatest;
    private Integer sort;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
