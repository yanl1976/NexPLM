package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappVolumeDtos;
import com.nexplm.platform.object.service.ObjectAccessGuard;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工艺成册（P6-5）：把同一零件的多张工艺卡片按页序装订成一册。
 *
 * <p>关键规则：</p>
 * <ul>
 *   <li><b>册与卡片是多对多</b>：同一张卡片可出现在不同的册里，因此页序放在
 *       {@code capp_card_volume_item}，不给卡片挂单值册号。</li>
 *   <li><b>册内卡片必须属于同一零件</b>：成册是「一个零件的工艺包」，混入别的零件会在打印时
 *       出现零件信息与卡片对不上的低级错误，保存时直接拒绝。</li>
 *   <li><b>页序 10/20/30</b>：与工序号同一套步进习惯，便于插入半步。</li>
 * </ul>
 *
 * <p>另含<b>齐套性检查</b>：只读聚合某零件的路线/工序/工步/卡片情况，给出 PASS/WARN/FAIL。
 * 只有「没有路线」「没有工序」算 FAIL，工时未填、资源未绑、卡片未生成都只是 WARN——
 * 它们表示「工艺还没写完」，不是数据错误。</p>
 */
@Service
public class CappVolumeService {

    /** 成册状态：草稿（可改）/ 已发布（装订确定）。 */
    private static final Set<String> STATUSES = Set.of("DRAFT", "RELEASED");
    private static final String PASS = "PASS";
    private static final String WARN = "WARN";
    private static final String FAIL = "FAIL";

    private final JdbcTemplate jdbcTemplate;
    private final CodeRuleService codeRuleService;
    private final ObjectAccessGuard objectAccessGuard;

    public CappVolumeService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService, ObjectAccessGuard objectAccessGuard) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
        this.objectAccessGuard = objectAccessGuard;
    }

    // ------------------------------------------------------------------ 成册

    public List<CappVolumeDtos.VolumeRow> list(Long companyId, String keyword, Long objectId) {
        StringBuilder where = new StringBuilder(" WHERE v.company_id = ? AND v.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (objectId != null) {
            where.append(" AND v.object_id = ?");
            args.add(objectId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (v.code ILIKE ? OR v.name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        return jdbcTemplate.query("""
                        SELECT v.id, v.code, v.name, v.object_id, o.code AS object_code,
                               o.name AS object_name, v.status, v.remark,
                               (SELECT COUNT(1) FROM capp_card_volume_item i WHERE i.volume_id = v.id)
                                   AS card_count,
                               v.created_at, v.updated_at
                          FROM capp_card_volume v
                          LEFT JOIN plm_object o ON o.id = v.object_id
                        """ + where + " ORDER BY v.updated_at DESC, v.id DESC",
                args.toArray(),
                (rs, rowNum) -> new CappVolumeDtos.VolumeRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getLong("object_id"), rs.getString("object_code"),
                        rs.getString("object_name"), rs.getString("status"), rs.getInt("card_count"),
                        rs.getString("remark"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)));
    }

    public CappVolumeDtos.VolumeDetail detail(Long id, Long companyId) {
        List<CappVolumeDtos.VolumeDetail> rows = jdbcTemplate.query("""
                        SELECT v.id, v.code, v.name, v.object_id, o.code AS object_code,
                               o.name AS object_name, v.status, v.remark, v.created_at, v.updated_at
                          FROM capp_card_volume v
                          LEFT JOIN plm_object o ON o.id = v.object_id
                         WHERE v.id = ? AND v.company_id = ? AND v.deleted = false
                        """,
                (rs, rowNum) -> new CappVolumeDtos.VolumeDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getLong("object_id"), rs.getString("object_code"),
                        rs.getString("object_name"), rs.getString("status"), rs.getString("remark"),
                        cardsOf(id),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_NOT_FOUND, "工艺成册不存在：" + id);
        }
        return rows.get(0);
    }

    private List<CappVolumeDtos.VolumeCardRow> cardsOf(Long volumeId) {
        return jdbcTemplate.query("""
                        SELECT i.seq_no, c.id AS card_id, c.code AS card_code, c.title AS card_title,
                               c.template_name, c.template_version, c.routing_code,
                               (SELECT COUNT(1) FROM jsonb_array_elements(c.rows_json) e
                                 WHERE NOT (e @> '{"__summary__":"true"}'::jsonb)) AS row_count
                          FROM capp_card_volume_item i
                          JOIN capp_card c ON c.id = i.card_id AND c.deleted = false
                         WHERE i.volume_id = ?
                         ORDER BY i.seq_no
                        """,
                (rs, rowNum) -> new CappVolumeDtos.VolumeCardRow(rs.getInt("seq_no"),
                        rs.getLong("card_id"), rs.getString("card_code"), rs.getString("card_title"),
                        rs.getString("template_name"), rs.getInt("template_version"),
                        rs.getString("routing_code"), rs.getInt("row_count")),
                volumeId);
    }

    @Transactional
    public CappVolumeDtos.VolumeDetail create(CappVolumeDtos.VolumeSaveReq req, Long companyId,
                                              Long userId) {
        requireObject(req.objectId());
        List<Long> cardIds = normalizeCards(req.cardIds(), req.objectId(), companyId);
        String code = nextCode("CappCardVolume", "CV");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_card_volume (id, company_id, code, name, object_id, status,
                                                      remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), req.objectId(), blankToNull(req.remark()),
                userId, userId);
        writeItems(id, companyId, cardIds);
        return detail(id, companyId);
    }

    @Transactional
    public CappVolumeDtos.VolumeDetail update(Long id, CappVolumeDtos.VolumeSaveReq req, Long companyId,
                                              Long userId) {
        CappVolumeDtos.VolumeDetail current = detail(id, companyId);
        requireDraft(current.status());
        requireObject(req.objectId());
        List<Long> cardIds = normalizeCards(req.cardIds(), req.objectId(), companyId);
        jdbcTemplate.update("""
                        UPDATE capp_card_volume
                           SET name = ?, object_id = ?, remark = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), req.objectId(), blankToNull(req.remark()), userId, id, companyId);
        jdbcTemplate.update("DELETE FROM capp_card_volume_item WHERE volume_id = ?", id);
        writeItems(id, companyId, cardIds);
        return detail(id, companyId);
    }

    @Transactional
    public CappVolumeDtos.VolumeDetail release(Long id, Long companyId, Long userId) {
        CappVolumeDtos.VolumeDetail current = detail(id, companyId);
        requireDraft(current.status());
        if (current.cards().isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "成册里还没有卡片，不能发布");
        }
        jdbcTemplate.update("""
                UPDATE capp_card_volume SET status = 'RELEASED', updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
        return detail(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        CappVolumeDtos.VolumeDetail current = detail(id, companyId);
        requireDraft(current.status());
        jdbcTemplate.update("DELETE FROM capp_card_volume_item WHERE volume_id = ?", id);
        jdbcTemplate.update("""
                UPDATE capp_card_volume SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ------------------------------------------------------------------ 齐套检查

    public CappVolumeDtos.CompletenessResult completeness(Long objectId, Long companyId) {
        requireObject(objectId);
        Map<String, Object> object = objectInfo(objectId);
        Map<String, Object> counts = jdbcTemplate.queryForMap("""
                        SELECT
                          (SELECT COUNT(1) FROM capp_routing r
                            WHERE r.company_id = ? AND r.object_id = ? AND r.deleted = false
                              AND r.status <> 'ARCHIVED') AS route_count,
                          (SELECT COUNT(1) FROM capp_routing r
                            WHERE r.company_id = ? AND r.object_id = ? AND r.deleted = false
                              AND r.status = 'RELEASED') AS released_count,
                          (SELECT COUNT(1) FROM capp_routing_step s
                             JOIN capp_routing r ON r.id = s.routing_id
                            WHERE r.company_id = ? AND r.object_id = ? AND r.deleted = false
                              AND r.status <> 'ARCHIVED') AS step_count,
                          (SELECT COUNT(1) FROM capp_routing_step s
                             JOIN capp_routing r ON r.id = s.routing_id
                            WHERE r.company_id = ? AND r.object_id = ? AND r.deleted = false
                              AND r.status <> 'ARCHIVED'
                              AND (COALESCE(s.setup_minutes, 0) + COALESCE(s.run_minutes, 0)) = 0)
                              AS zero_steps,
                          (SELECT COUNT(1) FROM capp_routing_step s
                             JOIN capp_routing r ON r.id = s.routing_id
                            WHERE r.company_id = ? AND r.object_id = ? AND r.deleted = false
                              AND r.status <> 'ARCHIVED'
                              AND s.resource_id IS NULL) AS no_resource_steps,
                          (SELECT COUNT(1) FROM capp_routing_substep ss
                             JOIN capp_routing r ON r.id = ss.routing_id
                            WHERE r.company_id = ? AND r.object_id = ? AND r.deleted = false
                              AND r.status <> 'ARCHIVED')
                              AS substep_count,
                          (SELECT COUNT(1) FROM capp_card c
                            WHERE c.company_id = ? AND c.object_id = ? AND c.deleted = false)
                              AS card_count
                        """,
                companyId, objectId, companyId, objectId, companyId, objectId,
                companyId, objectId, companyId, objectId, companyId, objectId,
                companyId, objectId);

        int routeCount = intOf(counts.get("route_count"));
        int releasedCount = intOf(counts.get("released_count"));
        int stepCount = intOf(counts.get("step_count"));
        int zeroSteps = intOf(counts.get("zero_steps"));
        int noResourceSteps = intOf(counts.get("no_resource_steps"));
        int substepCount = intOf(counts.get("substep_count"));
        int cardCount = intOf(counts.get("card_count"));

        List<CappVolumeDtos.CheckItem> checks = new ArrayList<>();
        checks.add(new CappVolumeDtos.CheckItem("ROUTE_EXISTS", "工艺路线", routeCount > 0 ? PASS : FAIL,
                routeCount > 0 ? "共 " + routeCount + " 条路线" : "该零件还没有工艺路线"));
        checks.add(new CappVolumeDtos.CheckItem("ROUTE_RELEASED", "路线已发布",
                releasedCount > 0 ? PASS : WARN,
                releasedCount > 0 ? "已发布 " + releasedCount + " 条" : "只有草稿路线，尚未发布投产口径"));
        checks.add(new CappVolumeDtos.CheckItem("STEP_EXISTS", "工序完整", stepCount > 0 ? PASS : FAIL,
                stepCount > 0 ? "共 " + stepCount + " 道工序" : "路线里没有任何工序"));
        checks.add(new CappVolumeDtos.CheckItem("STEP_MINUTES", "工序工时",
                zeroSteps == 0 ? PASS : WARN,
                zeroSteps == 0 ? "所有工序都填了工时" : zeroSteps + " 道工序工时为 0"));
        checks.add(new CappVolumeDtos.CheckItem("STEP_RESOURCE", "工艺资源",
                noResourceSteps == 0 ? PASS : WARN,
                noResourceSteps == 0 ? "所有工序都绑定了资源" : noResourceSteps + " 道工序未绑工艺资源"));
        checks.add(new CappVolumeDtos.CheckItem("SUBSTEP_EXISTS", "工步拆分",
                substepCount > 0 ? PASS : WARN,
                substepCount > 0 ? "共 " + substepCount + " 个工步" : "还没有拆工步"));
        checks.add(new CappVolumeDtos.CheckItem("CARD_EXISTS", "工艺卡片",
                cardCount > 0 ? PASS : WARN,
                cardCount > 0 ? "已生成 " + cardCount + " 张卡片" : "还没有生成工艺卡片"));

        int pass = (int) checks.stream().filter(item -> PASS.equals(item.status())).count();
        int warn = (int) checks.stream().filter(item -> WARN.equals(item.status())).count();
        int fail = (int) checks.stream().filter(item -> FAIL.equals(item.status())).count();
        return new CappVolumeDtos.CompletenessResult(objectId, strOf(object.get("code")),
                strOf(object.get("name")), routeCount, releasedCount, stepCount, substepCount,
                cardCount, zeroSteps, noResourceSteps, checks, pass, warn, fail, fail == 0);
    }

    // ------------------------------------------------------------------ 内部

    private void writeItems(long volumeId, Long companyId, List<Long> cardIds) {
        int seqNo = 10;
        for (Long cardId : cardIds) {
            jdbcTemplate.update("""
                            INSERT INTO capp_card_volume_item (id, company_id, volume_id, card_id, seq_no)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, volumeId, cardId, seqNo);
            seqNo += 10;
        }
    }

    /** 卡片去重 + 校验「同一个零件」+ 校验卡片存在。 */
    private List<Long> normalizeCards(List<Long> cardIds, Long objectId, Long companyId) {
        if (cardIds == null || cardIds.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new HashSet<>();
        List<Long> normalized = new ArrayList<>();
        for (Long cardId : cardIds) {
            if (cardId == null || !seen.add(cardId)) {
                continue;
            }
            List<Long> owners = jdbcTemplate.queryForList("""
                    SELECT object_id FROM capp_card
                     WHERE id = ? AND company_id = ? AND deleted = false
                    """, Long.class, cardId, companyId);
            if (owners.isEmpty()) {
                throw BizException.of(ErrorCode.CAPP_CARD_NOT_FOUND, "工艺卡片不存在：" + cardId);
            }
            if (!objectId.equals(owners.get(0))) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                        "卡片 " + cardId + " 属于其它零件，不能放进本册");
            }
            normalized.add(cardId);
        }
        return normalized;
    }

    /** 成册按零件 id 直取，需与列表同一套数据范围，避免跨公司指定对象。 */
    private void requireObject(Long objectId) {
        objectAccessGuard.require(objectId);
    }

    private Map<String, Object> objectInfo(Long objectId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name FROM plm_object WHERE id = ? AND deleted = false
                """, objectId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "对象不存在：" + objectId);
        }
        return rows.get(0);
    }

    private void requireDraft(String status) {
        if (!"DRAFT".equals(status)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                    "只有草稿状态的成册可以修改（当前：" + status + "）");
        }
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String strOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 统一由编码规则引擎取号（批次 B2）：格式来自 code_rule.segments_json。 */
    private String nextCode(String ruleCode, String prefix) {
        return codeRuleService.next(ruleCode);
    }
}
