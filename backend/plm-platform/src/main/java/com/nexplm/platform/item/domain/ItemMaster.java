package com.nexplm.platform.item.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 零部件结构子表（与 plm_object 1:1）。
 */
@Data
@TableName("item_master")
public class ItemMaster {

    @TableId(type = IdType.INPUT)
    private Long objectId;
    private String itemType;
    private String specification;
    private String material;
    private String unit;
    private BigDecimal weight;
    private String sourceType;
    private String stdCode;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
