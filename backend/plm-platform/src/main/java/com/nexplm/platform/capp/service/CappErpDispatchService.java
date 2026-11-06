package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappErpDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工艺下发【预留】（P6-9）：把工艺路线固化成一份可分发的载荷快照。
 *
 * <p><b>本类不负责真正发出去</b>。计划里写明「与 ERP 的工艺下发接口预留（P9 实现）」，
 * 所以这里只解决 PLM 侧的三件事：</p>
 * <ol>
 *   <li>定义载荷契约（{@code buildPayload}）——路线 + 工序 + 工步 + 资源 + 工时 + 合计；</li>
 *   <li>快照落库（{@code payload_json} + {@code payload_sha}），保证「当初发了什么」可追溯，
 *       路线后续被改/归档也不影响；</li>
 *   <li>状态位与回执入口（PENDING/SENT/FAILED/VOID + message），P9 的适配器接上后只需调
 *       {@link #callback} 回写结果，不用改表结构。</li>
 * </ol>
 *
 * <p>只允许下发 {@code RELEASED} 路线：草稿还在改，下发出去的工艺会被产线当权威执行。</p>
 */
@Service
public class CappErpDispatchService {

    private static final Set<String> STATUSES = Set.of("PENDING", "SENT", "FAILED", "VOID");
    private static final String PAYLOAD_VERSION = "1.0";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public CappErpDispatchService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public CappErpDtos.DispatchDetail create(CappErpDtos.DispatchReq req, Long companyId, Long userId) {
        Map<String, Object> routing = requireRouting(req.routingId(), companyId);
        String status = String.valueOf(routing.get("status"));
        if (!"RELEASED".equals(status)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                    "只有已发布的工艺路线可以下发（当前：" + status + "）");
        }
        // 载荷里要带零件编码/名称，典型工艺（object_id 为空）没有对应零件，不能下发
        if (routing.get("object_id") == null) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                    "典型工艺不绑定零件，不能下发到 ERP（请先「套用到零件」）");
        }
        Map<String, Object> payload = buildPayload(routing, req.routingId(), companyId);
        String payloadJson = toJson(payload);
        String code = nextCode("CappErpDispatch", "ED");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_erp_dispatch (id, company_id, code, routing_id, object_id,
                                                       status, payload_json, payload_sha, message,
                                                       created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, 'PENDING', CAST(? AS jsonb), ?, ?, ?, ?)
                        """,
                id, companyId, code, req.routingId(),
                ((Number) routing.get("object_id")).longValue(), payloadJson,
                sha256(payloadJson), "载荷已生成，等待 ERP 适配器发送（P9）", userId, userId);
        return detail(id, companyId);
    }

    public List<CappErpDtos.DispatchRow> list(Long companyId, Long routingId, String status) {
        StringBuilder where = new StringBuilder(" WHERE d.company_id = ? AND d.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (routingId != null) {
            where.append(" AND d.routing_id = ?");
            args.add(routingId);
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            if (!STATUSES.contains(normalized)) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知下发状态：" + status);
            }
            where.append(" AND d.status = ?");
            args.add(normalized);
        }
        return jdbcTemplate.query("""
                        SELECT d.id, d.code, d.routing_id, r.code AS routing_code, d.object_id,
                               o.code AS object_code, d.status, d.payload_sha, d.message,
                               d.created_at, d.updated_at
                          FROM capp_erp_dispatch d
                          LEFT JOIN capp_routing r ON r.id = d.routing_id
                          LEFT JOIN plm_object o ON o.id = d.object_id
                        """ + where + " ORDER BY d.created_at DESC, d.id DESC",
                args.toArray(),
                (rs, rowNum) -> new CappErpDtos.DispatchRow(rs.getLong("id"), rs.getString("code"),
                        rs.getLong("routing_id"), rs.getString("routing_code"),
                        rs.getLong("object_id"), rs.getString("object_code"), rs.getString("status"),
                        rs.getString("payload_sha"), rs.getString("message"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)));
    }

    public CappErpDtos.DispatchDetail detail(Long id, Long companyId) {
        List<CappErpDtos.DispatchDetail> rows = jdbcTemplate.query("""
                        SELECT d.id, d.code, d.routing_id, r.code AS routing_code, d.object_id,
                               o.code AS object_code, d.status, d.payload_sha, d.message,
                               d.payload_json::text AS payload_text, d.created_at, d.updated_at
                          FROM capp_erp_dispatch d
                          LEFT JOIN capp_routing r ON r.id = d.routing_id
                          LEFT JOIN plm_object o ON o.id = d.object_id
                         WHERE d.id = ? AND d.company_id = ? AND d.deleted = false
                        """,
                (rs, rowNum) -> new CappErpDtos.DispatchDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getLong("routing_id"), rs.getString("routing_code"),
                        rs.getLong("object_id"), rs.getString("object_code"), rs.getString("status"),
                        rs.getString("payload_sha"), rs.getString("message"),
                        readMap(rs.getString("payload_text")),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "下发记录不存在：" + id);
        }
        return rows.get(0);
    }

    /** 原始载荷 JSON（P9 适配器取数入口）。 */
    public String payload(Long id, Long companyId) {
        List<String> rows = jdbcTemplate.query("""
                        SELECT payload_json::text FROM capp_erp_dispatch
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """,
                (rs, rowNum) -> rs.getString(1), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "下发记录不存在：" + id);
        }
        return rows.get(0);
    }

    /** 回执：P9 的适配器发送成功/失败后回写状态（本阶段也可人工标记试跑结果）。 */
    @Transactional
    public CappErpDtos.DispatchDetail callback(Long id, CappErpDtos.DispatchCallbackReq req,
                                               Long companyId, Long userId) {
        CappErpDtos.DispatchDetail current = detail(id, companyId);
        String status = req.status() == null || req.status().isBlank()
                ? "SENT" : req.status().trim().toUpperCase();
        if (!STATUSES.contains(status)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知下发状态：" + req.status());
        }
        jdbcTemplate.update("""
                        UPDATE capp_erp_dispatch
                           SET status = ?, message = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                status, req.message() == null || req.message().isBlank()
                        ? current.message() : req.message().trim(), userId, id, companyId);
        return detail(id, companyId);
    }

    // ---------------------------------------------------------------- 载荷契约

    /** 下发载荷：ERP 侧消费的稳定结构，字段一旦发布不轻易改名（改名 = 换 version）。 */
    private Map<String, Object> buildPayload(Map<String, Object> routing, Long routingId,
                                             Long companyId) {
        List<Map<String, Object>> steps = jdbcTemplate.queryForList("""
                SELECT s.seq_no, s.name, s.op_type, s.work_center, s.setup_minutes, s.run_minutes,
                       s.remark, r.code AS resource_code, r.name AS resource_name,
                       r.spec AS resource_spec
                  FROM capp_routing_step s
                  LEFT JOIN capp_resource r ON r.id = s.resource_id AND r.deleted = false
                 WHERE s.routing_id = ? AND s.company_id = ?
                 ORDER BY s.seq_no
                """, routingId, companyId);
        List<Map<String, Object>> substeps = jdbcTemplate.queryForList("""
                SELECT step_seq_no, seq_no, name, content, setup_minutes, run_minutes
                  FROM capp_routing_substep
                 WHERE routing_id = ? AND company_id = ?
                 ORDER BY step_seq_no, seq_no
                """, routingId, companyId);

        BigDecimal setupTotal = BigDecimal.ZERO;
        BigDecimal runTotal = BigDecimal.ZERO;
        List<Map<String, Object>> operations = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            int seqNo = ((Number) step.get("seq_no")).intValue();
            BigDecimal setup = decimalOf(step.get("setup_minutes"));
            BigDecimal run = decimalOf(step.get("run_minutes"));
            setupTotal = setupTotal.add(setup);
            runTotal = runTotal.add(run);

            Map<String, Object> operation = new LinkedHashMap<>();
            operation.put("seqNo", seqNo);
            operation.put("name", step.get("name"));
            operation.put("opType", step.get("op_type"));
            operation.put("workCenter", step.get("work_center"));
            operation.put("setupMinutes", setup);
            operation.put("runMinutes", run);
            if (step.get("resource_code") != null) {
                Map<String, Object> resource = new LinkedHashMap<>();
                resource.put("code", step.get("resource_code"));
                resource.put("name", step.get("resource_name"));
                resource.put("spec", step.get("resource_spec"));
                operation.put("resource", resource);
            }
            List<Map<String, Object>> own = new ArrayList<>();
            for (Map<String, Object> substep : substeps) {
                if (((Number) substep.get("step_seq_no")).intValue() == seqNo) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("seqNo", substep.get("seq_no"));
                    item.put("name", substep.get("name"));
                    item.put("content", substep.get("content"));
                    item.put("setupMinutes", decimalOf(substep.get("setup_minutes")));
                    item.put("runMinutes", decimalOf(substep.get("run_minutes")));
                    own.add(item);
                }
            }
            if (!own.isEmpty()) {
                operation.put("substeps", own);
            }
            operations.add(operation);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "NEXPLM");
        payload.put("payloadVersion", PAYLOAD_VERSION);
        payload.put("generatedAt", OffsetDateTime.now().toString());
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("code", routing.get("code"));
        header.put("name", routing.get("name"));
        header.put("versionNo", routing.get("version_no"));
        header.put("status", routing.get("status"));
        header.put("applicability", routing.get("applicability"));
        header.put("objectCode", routing.get("object_code"));
        header.put("objectName", routing.get("object_name"));
        payload.put("routing", header);
        payload.put("operations", operations);
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("operationCount", operations.size());
        totals.put("setupMinutes", setupTotal);
        totals.put("runMinutes", runTotal);
        totals.put("totalMinutes", setupTotal.add(runTotal));
        payload.put("totals", totals);
        return payload;
    }

    private Map<String, Object> requireRouting(Long routingId, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT r.id, r.code, r.name, r.version_no, r.status, r.applicability, r.object_id,
                       o.code AS object_code, o.name AS object_name
                  FROM capp_routing r
                  LEFT JOIN plm_object o ON o.id = r.object_id
                 WHERE r.id = ? AND r.company_id = ? AND r.deleted = false
                """, routingId, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_ROUTING_NOT_FOUND, "工艺路线不存在：" + routingId);
        }
        return rows.get(0);
    }

    private static BigDecimal decimalOf(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> readMap(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(text,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "下发载荷序列化失败：" + ex.getMessage());
        }
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : digest) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private String nextCode(String ruleCode, String prefix) {
        Long no = jdbcTemplate.queryForObject("""
                        INSERT INTO code_sequence (id, rule_code, scope_key, current_no)
                        VALUES (?, ?, 'DEFAULT', 1)
                        ON CONFLICT (rule_code, scope_key)
                        DO UPDATE SET current_no = code_sequence.current_no + 1, updated_at = now()
                        RETURNING current_no
                        """, Long.class, IdWorker.getId(), ruleCode);
        return String.format("%s-%06d", prefix, no == null ? 1L : no);
    }
}
