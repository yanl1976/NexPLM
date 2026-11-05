package com.nexplm.platform.doc.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocDtos;
import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.iam.service.AclService;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.object.version.VersionFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 对象查看器的「非主数据」页签数据：关联对象、操作日志、实例权限。
 * <p>主数据（属性/文件/版本）仍由 {@link DocService} 提供，此处只读，不改变业务状态。</p>
 */
@Service
public class DocViewService {

    private static final int DEFAULT_LOG_LIMIT = 100;
    private static final int MAX_LOG_LIMIT = 500;

    private final PlmObjectMapper objectMapper;
    private final AclService aclService;
    private final JdbcTemplate jdbcTemplate;

    public DocViewService(PlmObjectMapper objectMapper, AclService aclService, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.aclService = aclService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 关联对象：把 plm_relation 的入边与出边合并成一张表，对方对象信息一并带出。
     */
    public List<DocDtos.RelationView> relations(Long docId) {
        requireDoc(docId);
        return jdbcTemplate.query("""
                        SELECT o.id, o.code, o.name, o.object_type, o.lifecycle_state,
                               o.revision, o.iteration,
                               r.relation_type, r.seq, r.qty,
                               CASE WHEN r.src_id = ? THEN 'OUT' ELSE 'IN' END AS direction
                          FROM plm_relation r
                          JOIN plm_object o
                            ON o.id = CASE WHEN r.src_id = ? THEN r.dst_id ELSE r.src_id END
                         WHERE r.deleted = false
                           AND (r.src_id = ? OR r.dst_id = ?)
                         ORDER BY r.relation_type, r.seq, o.code
                        """,
                (rs, rowNum) -> new DocDtos.RelationView(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("object_type"),
                        rs.getString("lifecycle_state"),
                        VersionFormatter.label(rs.getInt("revision"), rs.getInt("iteration")),
                        rs.getString("relation_type"),
                        rs.getString("direction"),
                        rs.getInt("seq"),
                        rs.getBigDecimal("qty")),
                docId, docId, docId, docId);
    }

    /**
     * 操作日志：状态流转日志（plm_state_log）与审计事件（sys_audit_log）合并后按时间倒序。
     */
    public List<DocDtos.LogView> logs(Long docId, Integer limit) {
        requireDoc(docId);
        int size = limit == null ? DEFAULT_LOG_LIMIT : Math.min(Math.max(1, limit), MAX_LOG_LIMIT);
        return jdbcTemplate.query("""
                        SELECT t.log_type, t.action, t.from_state, t.to_state, t.detail, t.result,
                               t.operator_id, t.ip, t.created_at,
                               u.display_name AS operator_label
                          FROM (
                                SELECT 'STATE' AS log_type,
                                       COALESCE(action, '') AS action,
                                       from_state,
                                       to_state,
                                       reason AS detail,
                                       'SUCCESS' AS result,
                                       operator_id,
                                       CAST(NULL AS varchar(64)) AS ip,
                                       created_at
                                  FROM plm_state_log
                                 WHERE object_id = ?
                                UNION ALL
                                SELECT 'AUDIT',
                                       action,
                                       NULL,
                                       NULL,
                                       detail,
                                       result,
                                       user_id,
                                       ip,
                                       created_at
                                  FROM sys_audit_log
                                 WHERE object_type = ? AND object_id = ?
                               ) t
                          LEFT JOIN sys_user u ON u.id = t.operator_id
                         ORDER BY t.created_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new DocDtos.LogView(
                        rs.getString("log_type"),
                        rs.getString("action"),
                        rs.getString("from_state"),
                        rs.getString("to_state"),
                        rs.getString("detail"),
                        rs.getString("result"),
                        rs.getObject("operator_id", Long.class),
                        rs.getString("operator_label"),
                        rs.getString("ip"),
                        rs.getObject("created_at", OffsetDateTime.class)),
                docId, DocService.OBJECT_TYPE, docId, size);
    }

    /** 实例权限（ACL）：只读展示，授予/撤销仍走 /security/acl。 */
    public List<SecurityDtos.AclRow> acl(Long docId) {
        requireDoc(docId);
        return aclService.list(docId);
    }

    private void requireDoc(Long docId) {
        if (docId == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "文档标识不能为空");
        }
        PlmObject object = objectMapper.selectById(docId);
        if (object == null || !DocService.OBJECT_TYPE.equals(object.getObjectType())) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "文档不存在：" + docId);
        }
    }
}
