package com.nexplm.platform.object.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.nexplm.platform.support.mybatis.JsonbStringTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 业务对象通用表（所有 PLM 对象的根）。
 * <p>master 表示版本族；revision 为大版本；iteration 为检入产生的小版本；is_latest 标记族内最新。</p>
 */
@Data
@TableName(value = "plm_object", autoResultMap = true)
public class PlmObject {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String objectType;
    private Long masterId;
    private String code;
    private String name;
    private Long companyId;
    private Long orgId;
    private Long ownerId;
    private Long ownerGroupId;
    private Integer securityLevel;
    private String lifecycleState;
    private String phaseState;
    private String workflowState;
    private Integer revision;
    private Integer iteration;
    private Boolean isLatest;
    private OffsetDateTime releasedAt;
    private OffsetDateTime obsoleteAt;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String extJson;
    @Version
    private Integer version;
    @TableLogic
    private Boolean deleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}
