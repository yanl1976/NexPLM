package com.nexplm.platform.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.integration.api.IntegrationDtos;
import com.nexplm.platform.integration.kingdee.KingdeePayloadService;
import com.nexplm.platform.integration.support.MappingEngine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ERP 三段字段映射（批次 G）：把旧系统 {@code ERPConfig.xml} 的
 * 「物料下发 32 列 / BOM 下发 9 列 / 请购单下发 24 列」搬进 {@code itg_mapping}，
 * 支持在线编辑与下发前试算。
 *
 * <p><b>为什么不给 itg_mapping 加表</b>：三段映射本来就是"字段怎么转"，
 * 与既有 {@code itg_mapping.rules} 同源；额外属性（ERP 字段名、列标题、是否可空、
 * 解析后的取值路径）直接放在规则 JSON 里，引擎只认自己那几项，多余键不影响执行。</p>
 *
 * <p><b>缺口不隐藏</b>：{@code sourcePath} 为空即"新系统没有这个数据源"，
 * 试算时逐条列出来。字段落点（A 元数据落 ext_json / B 加物理列）未确认前，
 * 这是唯一诚实的状态——先让联调方看到差什么，而不是下发时才发现字段是空的。</p>
 */
@Service
public class ErpMappingService {

    /** 段定义：旧 XML 段名 + 中文名 + 是否支持试算。 */
    private static final Map<String, Segment> SEGMENTS = new LinkedHashMap<>();

    static {
        SEGMENTS.put("Material", new Segment("物料下发", "SendMaterialToErp", true,
                "物料主数据 32 列（含 14 列无数据源，见 EXP-20261107-17）"));
        SEGMENTS.put("Bom", new Segment("BOM 下发", "SendBomToErp", true,
                "BOM 父项 4 列 + 子项 5 列；子项走分录数组"));
        SEGMENTS.put("Requisition", new Segment("请购单下发", "SendPseitemSGDToERP", false,
                "本系统无请购单实体，24 列仅保留映射供参考"));
    }

    private record Segment(String label, String sourceSegment, boolean previewSupported, String remark) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final KingdeePayloadService payloadService;

    public ErpMappingService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                             KingdeePayloadService payloadService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.payloadService = payloadService;
    }

    public List<IntegrationDtos.ErpSegmentRow> segments(Long companyId) {
        List<IntegrationDtos.ErpSegmentRow> rows = new ArrayList<>();
        for (Map.Entry<String, Segment> entry : SEGMENTS.entrySet()) {
            Segment segment = entry.getValue();
            JsonNode rules = rulesOf(companyId, entry.getKey());
            List<IntegrationDtos.ErpRuleRow> parsed = parseRows(rules);
            rows.add(new IntegrationDtos.ErpSegmentRow(entry.getKey(), segment.label(),
                    segment.sourceSegment(), parsed.size(),
                    count(parsed, "OK"), count(parsed, "GAP"), count(parsed, "PENDING"),
                    segment.previewSupported(), segment.remark()));
        }
        return rows;
    }

    public IntegrationDtos.ErpMappingDetail detail(String objectCode, Long companyId) {
        Segment segment = segment(objectCode);
        Map<String, Object> row = mappingRow(objectCode, companyId);
        if (row == null) {
            throw BizException.of(ErrorCode.ITG_MAPPING_NOT_FOUND,
                    "ERP 字段映射模板缺失：" + objectCode + "（应由迁移 V69 写入）");
        }
        List<IntegrationDtos.ErpRuleRow> rules = parseRows(readJson(str(row.get("rules"))));
        return new IntegrationDtos.ErpMappingDetail(objectCode, segment.label(),
                segment.sourceSegment(), str(row.get("endpoint_code")),
                str(row.get("payload_version")), str(row.get("remark")), rules,
                count(rules, "OK"), count(rules, "GAP"), count(rules, "PENDING"),
                segment.previewSupported());
    }

    @Transactional
    public IntegrationDtos.ErpMappingDetail save(String objectCode,
                                                 IntegrationDtos.ErpMappingSaveReq req,
                                                 Long companyId, Long userId) {
        segment(objectCode);
        Map<String, Object> row = mappingRow(objectCode, companyId);
        if (row == null) {
            throw BizException.of(ErrorCode.ITG_MAPPING_NOT_FOUND,
                    "ERP 字段映射模板缺失：" + objectCode + "（应由迁移 V69 写入）");
        }
        Long id = ((Number) row.get("id")).longValue();
        ArrayNode rules = objectMapper.createArrayNode();
        int index = 0;
        for (IntegrationDtos.ErpRuleSaveReq rule : req.rules()) {
            index++;
            String target = rule.target() == null ? "" : rule.target().trim();
            if (target.isBlank() || target.contains(" ")) {
                throw BizException.of(ErrorCode.ITG_MAPPING_INVALID,
                        "第 " + index + " 行的下发路径非法：" + rule.target());
            }
            String sourcePath = blankToNull(rule.sourcePath());
            ObjectNode node = rules.addObject();
            node.put("sort", index * 10);
            node.put("erpName", blankToNull(rule.erpName()));
            node.put("caption", blankToNull(rule.caption()));
            node.put("pdmName", blankToNull(rule.pdmName()));
            node.put("pdmCaption", blankToNull(rule.pdmCaption()));
            node.put("source", rule.source().trim());
            node.put("sourcePath", sourcePath);
            node.put("target", target);
            node.put("transform", blankToNull(rule.transform()));
            boolean canNull = rule.canNull() == null || rule.canNull();
            node.put("canNull", canNull);
            node.put("sendDbValue", rule.sendDbValue() == null || rule.sendDbValue());
            node.put("required", !canNull);
            node.put("status", statusOf(sourcePath, rule.status()));
            node.put("note", blankToNull(rule.note()));
        }
        jdbcTemplate.update("""
                        UPDATE itg_mapping SET rules = ?::jsonb, remark = ?, updated_by = ?,
                                               updated_at = now()
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """,
                rules.toString(), blankToNull(req.remark()), userId, id, companyId);
        return detail(objectCode, companyId);
    }

    /**
     * 试算：按当前映射把一个真实对象组装成金蝶报文，并列出缺口。
     *
     * <p>只组装不落任务、不发送——联调前的"看报文"入口。</p>
     */
    public IntegrationDtos.ErpPreviewResult preview(String objectCode,
                                                    IntegrationDtos.ErpPreviewReq req,
                                                    Long companyId, Long userId) {
        Segment segment = segment(objectCode);
        if (!segment.previewSupported()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    segment.label() + " 暂不支持试算：" + segment.remark());
        }
        Map<String, Object> row = mappingRow(objectCode, companyId);
        if (row == null) {
            throw BizException.of(ErrorCode.ITG_MAPPING_NOT_FOUND,
                    "ERP 字段映射模板缺失：" + objectCode + "（应由迁移 V69 写入）");
        }
        KingdeePayloadService.SourcePayload source = payloadService.build(
                companyId, objectCode, req.bizId(), req.viewType());
        List<MappingEngine.Rule> rules = MappingEngine.parseRules(readJson(str(row.get("rules"))));
        // Parent$ 取"父/根节点"：BOM 的根件信息就在源报文顶层，直接回指源报文
        Map<String, Object> context = Map.of(
                "userCode", userCode(userId),
                "parent", source.payload());
        MappingEngine.Applied applied = MappingEngine.preview(rules, source.payload(), context);
        List<IntegrationDtos.ErpMissingRow> missing = applied.missing().stream()
                .map(item -> new IntegrationDtos.ErpMissingRow(item.target(), item.source(),
                        item.reason()))
                .toList();
        int total = rules.size();
        return new IntegrationDtos.ErpPreviewResult(objectCode, source.bizKey(),
                applied.payload(), missing, total - missing.size(), missing.size(),
                MappingEngine.ENTRY_KEY,
                "分录数组暂用固定键名 " + MappingEngine.ENTRY_KEY
                        + "，金蝶侧真实分录键名待真机联调确认");
    }

    // ------------------------------------------------------------ 内部

    private Segment segment(String objectCode) {
        if (objectCode == null || objectCode.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请指定映射段（Material/Bom/Requisition）");
        }
        String key = normalizeObjectCode(objectCode);
        Segment segment = SEGMENTS.get(key);
        if (segment == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "不支持的映射段：" + objectCode + "（可选 " + String.join("/", SEGMENTS.keySet()) + "）");
        }
        return segment;
    }

    private String normalizeObjectCode(String objectCode) {
        return switch (objectCode.trim().toLowerCase(Locale.ROOT)) {
            case "material" -> "Material";
            case "bom" -> "Bom";
            case "requisition", "pseitemsgd" -> "Requisition";
            default -> objectCode.trim();
        };
    }

    private Map<String, Object> mappingRow(String objectCode, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT m.id, m.rules::text AS rules, m.payload_version, m.remark,
                       e.code AS endpoint_code
                  FROM itg_mapping m
                  JOIN itg_endpoint e ON e.id = m.endpoint_id
                 WHERE m.company_id = ? AND m.direction = 'OUT' AND m.deleted = false
                   AND lower(m.object_code) = lower(?)
                 ORDER BY m.id
                 LIMIT 1
                """, companyId, objectCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private JsonNode rulesOf(Long companyId, String objectCode) {
        Map<String, Object> row = mappingRow(objectCode, companyId);
        return row == null ? objectMapper.createArrayNode() : readJson(str(row.get("rules")));
    }

    private List<IntegrationDtos.ErpRuleRow> parseRows(JsonNode rules) {
        List<IntegrationDtos.ErpRuleRow> rows = new ArrayList<>();
        if (rules == null || !rules.isArray()) {
            return rows;
        }
        for (JsonNode node : rules) {
            String sourcePath = text(node, "sourcePath");
            String source = text(node, "source");
            rows.add(new IntegrationDtos.ErpRuleRow(
                    node.path("sort").asInt(0),
                    text(node, "erpName"), text(node, "caption"),
                    text(node, "pdmName"), text(node, "pdmCaption"),
                    source, sourcePath, text(node, "target"),
                    text(node, "transform"),
                    !node.has("canNull") || node.path("canNull").asBoolean(true),
                    !node.has("sendDbValue") || node.path("sendDbValue").asBoolean(true),
                    node.path("required").asBoolean(false),
                    text(node, "status") == null ? statusOf(sourcePath, null) : text(node, "status"),
                    text(node, "note")));
        }
        return rows;
    }

    private int count(List<IntegrationDtos.ErpRuleRow> rows, String status) {
        return (int) rows.stream().filter(row -> status.equals(row.status())).count();
    }

    /** 状态口径：没取值路径就是缺口；有路径但语义待确认的保留 PENDING。 */
    private String statusOf(String sourcePath, String status) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return "GAP";
        }
        return "PENDING".equalsIgnoreCase(status) ? "PENDING" : "OK";
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.ITG_MAPPING_INVALID, "映射规则不是合法 JSON");
        }
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** USERINFO$ECODE 取当前登录用户的账号（金蝶侧的"用户编码"口径待联调确认）。 */
    private String userCode(Long userId) {
        if (userId == null) {
            return "";
        }
        List<String> codes = jdbcTemplate.query(
                "SELECT username FROM sys_user WHERE id = ?",
                (rs, rowNum) -> rs.getString(1), userId);
        return codes.isEmpty() || codes.get(0) == null ? "" : codes.get(0);
    }
}
