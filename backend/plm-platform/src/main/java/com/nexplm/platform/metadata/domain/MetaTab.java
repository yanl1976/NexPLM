package com.nexplm.platform.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 对象查看器页签定义（按对象类型配置，前端按 sort 渲染）。
 */
@Data
@TableName("meta_tab")
public class MetaTab {

    @TableId
    private Long id;
    private String entityCode;
    private String code;
    private String name;
    private String tabType;
    private String permissionCode;
    private Integer sort;
    private Boolean enabled;
    private String configJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
