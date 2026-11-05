package com.nexplm.platform.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 物理文件（按 sha256 唯一）。多个对象的附件可共享同一物理文件。
 */
@Data
@TableName("file_object")
public class FileObject {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String sha256;
    private Long size;
    private String mime;
    private String ext;
    private String bucket;
    private String objectKey;
    private Integer refCount;
    private String scanStatus;
    private OffsetDateTime createdAt;
}
