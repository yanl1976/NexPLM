package com.nexplm.platform.item.service;

import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.iam.service.AclService;
import com.nexplm.platform.item.api.ItemDtos;
import com.nexplm.platform.object.version.VersionFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 零部件对象查看器的只读页签数据：设计文档、操作日志、实例权限。
 * <p>与图文档的 {@code DocViewService} 同构（那边限定 PdmDoc，这边限定 PdmItem）。</p>
 */
@Service
public class ItemViewService {

    private static final int DEFAULT_LOG_LIMIT = 100;
    private static final int MAX_LOG_LIMIT = 500;

    private final ItemService itemService;
    private final AclService aclService;
    private final JdbcTemplate jdbcTemplate;

    public ItemViewService(ItemService itemService, AclService aclService, JdbcTemplate jdbcTemplate) {
        this.itemService = itemService;
        this.aclService = aclService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 设计文档：零件与 PdmDoc 的关系（任一方向），对应旧系统「设计文档」页签。 */
    public List<ItemDtos.ItemDocRow> docs(Long itemId) {
        itemService.requireItem(itemId);
        return jdbcTemplate.query("""
                        SELECT o.id, o.code, o.name, o.lifecycle_state, o.revision, o.iteration,
                               o.updated_at, r.relation_type,
                               COALESCE(d.doc_type, o.ext_json ->> 'docType') AS doc_type
                          FROM plm_relation r
                          JOIN plm_object o
                            ON o.id = CASE WHEN r.src_id = ? THEN r.dst_id ELSE r.src_id END
                          LEFT JOIN doc_master d ON d.object_id = o.id
                         WHERE r.deleted = false
                           AND o.object_type = 'PdmDoc'
                           AND (r.src_id = ? OR r.dst_id = ?)
                         ORDER BY r.relation_type, o.code
                        """,
                (rs, rowNum) -> new ItemDtos.ItemDocRow(
                        rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("doc_type"), rs.getString("relation_type"),
                        rs.getString("lifecycle_state"),
                        VersionFormatter.label(rs.getInt("revision"), rs.getInt("iteration")),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                itemId, itemId, itemId);
    }

    /** 操作日志：状态流转（plm_state_log）+ 审计事件（sys_audit_log）。 */
    public List<ItemDtos.LogRow> logs(Long itemId, Integer limit) {
        itemService.requireItem(itemId);
        int size = limit == null ? DEFAULT_LOG_LIMIT : Math.min(Math.max(1, limit), MAX_LOG_LIMIT);
        return jdbcTemplate.query("""
                        SELECT t.log_type, t.action, t.from_state, t.to_state, t.detail, t.result,
                               t.operator_id, t.ip, t.created_at,
                               u.display_name AS operator_label
                          FROM (
                                SELECT 'STATE' AS log_type,
                                       COALESCE(action, '') AS action,
                                       from_state, to_state,
                                       reason AS detail,
                                       'SUCCESS' AS result,
                                       operator_id,
                                       CAST(NULL AS varchar(64)) AS ip,
                                       created_at
                                  FROM plm_state_log
                                 WHERE object_id = ?
                                UNION ALL
                                SELECT 'AUDIT', action, NULL, NULL, detail, result, user_id, ip, created_at
                                  FROM sys_audit_log
                                 WHERE object_type = ? AND object_id = ?
                               ) t
                          LEFT JOIN sys_user u ON u.id = t.operator_id
                         ORDER BY t.created_at DESC
                         LIMIT ?
                        """,
                (rs, rowNum) -> new ItemDtos.LogRow(
                        rs.getString("log_type"), rs.getString("action"),
                        rs.getString("from_state"), rs.getString("to_state"),
                        rs.getString("detail"), rs.getString("result"),
                        rs.getObject("operator_id", Long.class), rs.getString("operator_label"),
                        rs.getString("ip"), rs.getObject("created_at", OffsetDateTime.class)),
                itemId, ItemService.OBJECT_TYPE, itemId, size);
    }

    /** 实例权限（只读）：授予/撤销仍走 /security/acl。 */
    public List<SecurityDtos.AclRow> acl(Long itemId) {
        itemService.requireItem(itemId);
        return aclService.list(itemId);
    }
}
