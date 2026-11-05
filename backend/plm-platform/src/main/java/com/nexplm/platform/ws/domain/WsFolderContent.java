package com.nexplm.platform.ws.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文件夹内容：文件夹与业务对象的多对多引用（对象本身不归属于文件夹，可被多处引用）。
 */
@Data
@TableName("ws_folder_content")
public class WsFolderContent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long folderId;
    private Long objectId;
    private Integer sort;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
