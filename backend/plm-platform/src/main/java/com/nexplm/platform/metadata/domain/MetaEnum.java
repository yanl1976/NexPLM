package com.nexplm.platform.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 枚举字典（值列表）。
 */
@Data
@TableName("meta_enum")
public class MetaEnum {

    @TableId
    private String code;
    private String name;
    private String status;
    private OffsetDateTime createdAt;
}
