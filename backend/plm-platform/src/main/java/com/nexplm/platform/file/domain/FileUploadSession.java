package com.nexplm.platform.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 分片上传会话。分片本体在存储层（{@code _multipart/{sessionId}/{index}}），
 * 这里只记录协议参数与状态，用于断点续传与超时清理。
 */
@Data
@TableName("file_upload_session")
public class FileUploadSession {

    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_MERGED = "MERGED";
    public static final String STATUS_ABORTED = "ABORTED";

    @TableId(type = IdType.INPUT)
    private String id;
    private String fileName;
    private Long totalSize;
    private Integer chunkSize;
    private Integer chunkTotal;
    private String mime;
    private Long ownerObjectId;
    private String fileRole;
    private Integer sort;
    private String status;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
