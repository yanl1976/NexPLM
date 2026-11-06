package com.nexplm.platform.integration.kingdee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 金蝶云星空适配器·源报文组装（P9-2）。
 *
 * <p>只负责"本地对象 → 源报文"，字段名的转换交给 {@code itg_mapping}：
 * 客户的物料字段叫 FNumber 还是 Number、要不要带 F 前缀，各账套不一样，
 * 写死在代码里就意味着换客户要改代码重新发版。</p>
 *
 * <p><b>幂等键口径</b>：把修订号/版本号带进 bizKey（{@code PN-000001@R2}、{@code RT-000001@V3}）。
 * 同一版本重复推送会命中幂等返回原任务，升版后是新的 bizKey 才能再发——
 * 否则一次升版后永远发不出去。</p>
 *
 * <p><b>请购单</b>：本系统没有请购单实体（请购发生在 ERP 侧），
 * 故 {@code Requisition} 明确报错而不是伪造报文；等 PLM 侧补齐请购模块再实现。</p>
 */
@Service
public class KingdeePayloadService {

    /** 组装结果：源报文 + 幂等键（同一个本地版本只推一次）。 */
    public record SourcePayload(String bizType, String bizKey, ObjectNode payload) {
    }

    private static final Set<String> SUPPORTED =
            Set.of("Material", "Bom", "Doc", "Route");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KingdeePayloadService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public SourcePayload build(Long companyId, String bizType, Long bizId, String viewType) {
        String type = bizType == null ? "" : bizType.trim();
        return switch (type) {
            case "Material" -> material(companyId, bizId);
            case "Bom" -> bom(companyId, bizId, viewType);
            case "Doc" -> doc(companyId, bizId);
            case "Route" -> route(companyId, bizId);
            case "Requisition" -> throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                    "本系统暂无请购单实体，无法下发 Requisition（请购在 ERP 侧发起）");
            default -> throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                    "不支持的业务类型：" + bizType + "（可选 " + String.join("/", SUPPORTED)
                            + "，Requisition 暂不支持）");
        };
    }

    /* ---------------------------------------------------------------- 物料 */

    private SourcePayload material(Long companyId, Long bizId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT o.id, o.code, o.name, o.revision, o.lifecycle_state, o.ext_json,
                       m.item_type, m.specification, m.material, m.unit, m.weight,
                       m.source_type, m.std_code, m.remark
                  FROM plm_object o
                  LEFT JOIN item_master m ON m.object_id = o.id
                 WHERE o.id = ? AND o.object_type = 'PdmItem' AND o.company_id = ?
                   AND o.deleted = false
                """, bizId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "物料不存在：" + bizId);
        }
        Map<String, Object> row = rows.get(0);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("code", str(row.get("code")));
        payload.put("name", str(row.get("name")));
        payload.put("revision", intOf(row.get("revision")));
        payload.put("lifecycleState", str(row.get("lifecycle_state")));
        payload.put("itemType", str(row.get("item_type")));
        payload.put("specification", str(row.get("specification")));
        payload.put("material", str(row.get("material")));
        payload.put("unit", str(row.get("unit")));
        payload.put("weight", decimal(row.get("weight")));
        payload.put("sourceType", str(row.get("source_type")));
        payload.put("stdCode", str(row.get("std_code")));
        payload.put("remark", str(row.get("remark")));
        int revision = intOf(row.get("revision"));
        return new SourcePayload("Material", str(row.get("code")) + "@R" + revision, payload);
    }

    /* ---------------------------------------------------------------- BOM */

    private SourcePayload bom(Long companyId, Long bizId, String viewType) {
        String view = viewType == null || viewType.isBlank() ? "DESIGN"
                : viewType.trim().toUpperCase(Locale.ROOT);
        List<Map<String, Object>> parents = jdbcTemplate.queryForList("""
                SELECT id, code, name, revision FROM plm_object
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, bizId, companyId);
        if (parents.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "BOM 根件不存在：" + bizId);
        }
        Map<String, Object> parent = parents.get(0);
        List<Map<String, Object>> lines = jdbcTemplate.queryForList("""
                SELECT l.line_no, l.qty, l.unit, l.loss_rate, l.ref_designator, l.remark,
                       c.code AS child_code, c.name AS child_name, c.revision AS child_revision
                  FROM bom_line l
                  JOIN plm_object c ON c.id = l.child_object_id
                 WHERE l.parent_object_id = ? AND l.view_type = ? AND l.deleted = false
                 ORDER BY l.line_no
                """, bizId, view);
        if (lines.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                    "该 BOM 在 " + view + " 视图下没有子件，无可下发内容");
        }
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("parentCode", str(parent.get("code")));
        payload.put("parentName", str(parent.get("name")));
        payload.put("parentRevision", intOf(parent.get("revision")));
        payload.put("viewType", view);
        ArrayNode array = payload.putArray("lines");
        for (Map<String, Object> line : lines) {
            ObjectNode node = array.addObject();
            node.put("lineNo", intOf(line.get("line_no")));
            node.put("childCode", str(line.get("child_code")));
            node.put("childName", str(line.get("child_name")));
            node.put("childRevision", intOf(line.get("child_revision")));
            node.put("qty", decimal(line.get("qty")));
            node.put("unit", str(line.get("unit")));
            node.put("lossRate", decimal(line.get("loss_rate")));
            node.put("refDesignator", str(line.get("ref_designator")));
            node.put("remark", str(line.get("remark")));
        }
        String bizKey = str(parent.get("code")) + "@" + view + "@R"
                + intOf(parent.get("revision"));
        return new SourcePayload("Bom", bizKey, payload);
    }

    /* ---------------------------------------------------------------- 文档 */

    private SourcePayload doc(Long companyId, Long bizId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT o.id, o.code, o.name, o.revision, o.lifecycle_state,
                       d.doc_type, d.source, d.file_ext, d.file_count, d.main_file_id
                  FROM plm_object o
                  LEFT JOIN doc_master d ON d.object_id = o.id
                 WHERE o.id = ? AND o.object_type = 'PdmDoc' AND o.company_id = ?
                   AND o.deleted = false
                """, bizId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "图文档不存在：" + bizId);
        }
        Map<String, Object> row = rows.get(0);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("code", str(row.get("code")));
        payload.put("name", str(row.get("name")));
        payload.put("revision", intOf(row.get("revision")));
        payload.put("lifecycleState", str(row.get("lifecycle_state")));
        payload.put("docType", str(row.get("doc_type")));
        payload.put("source", str(row.get("source")));
        payload.put("fileExt", str(row.get("file_ext")));
        payload.put("fileCount", intOf(row.get("file_count")));
        payload.put("mainFileId", row.get("main_file_id") == null ? null
                : longOf(row.get("main_file_id")));
        return new SourcePayload("Doc", str(row.get("code")) + "@R" + intOf(row.get("revision")),
                payload);
    }

    /* ---------------------------------------------------------------- 工艺路线 */

    private SourcePayload route(Long companyId, Long bizId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT r.*, o.code AS object_code, o.name AS object_name
                  FROM capp_routing r
                  LEFT JOIN plm_object o ON o.id = r.object_id
                 WHERE r.id = ? AND r.company_id = ? AND r.deleted = false
                """, bizId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "工艺路线不存在：" + bizId);
        }
        Map<String, Object> row = rows.get(0);
        if (!"RELEASED".equals(str(row.get("status")))) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                    "只有已发布的工艺路线可以下发（当前 " + str(row.get("status")) + "）");
        }
        List<Map<String, Object>> steps = jdbcTemplate.queryForList("""
                SELECT seq_no, name, op_type, work_center, setup_minutes, run_minutes, remark
                  FROM capp_routing_step WHERE routing_id = ? ORDER BY seq_no
                """, bizId);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("code", str(row.get("code")));
        payload.put("name", str(row.get("name")));
        payload.put("versionNo", intOf(row.get("version_no")));
        payload.put("status", str(row.get("status")));
        payload.put("objectCode", str(row.get("object_code")));
        payload.put("objectName", str(row.get("object_name")));
        payload.put("applicability", str(row.get("applicability")));
        ArrayNode array = payload.putArray("steps");
        for (Map<String, Object> step : steps) {
            ObjectNode node = array.addObject();
            node.put("seqNo", intOf(step.get("seq_no")));
            node.put("name", str(step.get("name")));
            node.put("opType", str(step.get("op_type")));
            node.put("workCenter", str(step.get("work_center")));
            node.put("setupMinutes", decimal(step.get("setup_minutes")));
            node.put("runMinutes", decimal(step.get("run_minutes")));
            node.put("remark", str(step.get("remark")));
        }
        return new SourcePayload("Route",
                str(row.get("code")) + "@V" + intOf(row.get("version_no")), payload);
    }

    /* ---------------------------------------------------------------- 工具 */

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return null;
    }
}
