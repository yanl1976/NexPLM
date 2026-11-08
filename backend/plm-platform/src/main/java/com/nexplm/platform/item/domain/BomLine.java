package com.nexplm.platform.item.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    /** 单台用量（对应蓝本 {@code USEQTY}）。 */
    private BigDecimal qty;
    /** 总用量（对应蓝本 {@code TOTALQTY}）；为空表示按层级累乘计算。 */
    private BigDecimal totalQty;
    /** 剩余用量（蓝本 {@code SURPLUSQTY}）。 */
    private BigDecimal surplusQty;
    /** 子用量（蓝本 {@code SUBUSEQTY}）。 */
    private BigDecimal subUseQty;
    /** BOM 类型（蓝本 {@code LBOMTYPE}）：DESIGN / QUOTE / … */
    private String bomType;
    /** 工序路线（蓝本 {@code ITEMROUTE}）。 */
    private String itemRoute;
    /** 数据来源：MANUAL / IMPORT / ERP（蓝本 {@code DATASOURCE}）。 */
    private String dataSource;
    private String unit;
    private BigDecimal lossRate;
    /** 有效性区间（含边界）；为空表示该侧不限，即无区间 = 始终有效。 */
    private LocalDate effectFrom;
    private LocalDate effectTo;
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
