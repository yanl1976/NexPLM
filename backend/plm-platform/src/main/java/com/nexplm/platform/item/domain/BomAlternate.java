package com.nexplm.platform.item.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * BOM 替代件关系：某个已在 BOM 中出现的子件，可以被哪些备选件替换。
 * <p>对齐旧系统 {@code ALTERNATESRELATION}：不改写 BOM 行，也不参与展开与汇总。</p>
 */
@Data
@TableName("bom_alternate")
public class BomAlternate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 被替换件（旧 PEID）。 */
    private Long itemObjectId;
    /** 备选件（旧 CEID）。 */
    private Long alternateObjectId;
    /** 局部替代限定的视图（旧 PVIEWID）。 */
    private String viewType;
    /** 全局替代（旧 ISGLOBAL）：所有视图可用。 */
    private Boolean isGlobal;
    /** 双向替代（旧 ISTWOWAY）。 */
    private Boolean isTwoWay;
    /** 优先级（旧 PRIORITYLEVEL，旧系统仅展示/排序）。 */
    private String priorityLevel;
    /** 排序（旧 LSEQUENCE）。 */
    private Integer lineNo;
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
