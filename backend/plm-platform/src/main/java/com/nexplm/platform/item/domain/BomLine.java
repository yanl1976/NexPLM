package com.nexplm.platform.item.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * BOM 行：父件 → 子件，按视图（设计/工艺/制造）区分。
 */
@Data
@TableName("bom_line")
public class BomLine {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentObjectId;
    private Long childObjectId;
    private String viewType;
    private Integer lineNo;
    private BigDecimal qty;
    private String unit;
    private BigDecimal lossRate;
    private String refDesignator;
    private String remark;
    @Version
    private Integer version;
    @TableLogic
    private Boolean deleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
