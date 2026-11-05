package com.nexplm.platform.doc.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 图文档子表（与 {@code plm_object} 1:1）。只放结构性统计，业务字段在 {@code plm_object.ext_json}。
 */
@Data
@TableName("doc_master")
public class DocMaster {

    @TableId(type = IdType.INPUT)
    private Long objectId;
    private String docType;
    private Long classId;
    private Long vaultId;
    private String fileExt;
    private Integer fileCount;
    private Integer pageCount;
    private String source;
    private Boolean isDerived;
    private Long mainFileId;
    private OffsetDateTime updatedAt;
}
