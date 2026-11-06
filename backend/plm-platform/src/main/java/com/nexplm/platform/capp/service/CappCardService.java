package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappCardDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工艺卡片（P6-4）：模板（表格结构）+ 卡片实例（数据绑定 + 行快照）。
 *
 * <p>关键规则：</p>
 * <ul>
 *   <li><b>模板同码多版本</b>：唯一键 {@code (company_id, code, version_no)}，发布只把同族其它
 *       {@code RELEASED} 置 {@code ARCHIVED}，不覆盖旧行；改版走「另存新版本」而不是原地改已发布版本。</li>
 *   <li><b>卡片双快照</b>：落地时既记 {@code template_code/version}（追溯），也把 {@code layout_json}
 *       复制进卡片（版式不串）。之后模板再改版，历史卡片导出结果不变。</li>
 *   <li><b>只用已发布模板生成卡片</b>：草稿模板的版式还在改，生成出来的卡片没有稳定的版式基线。</li>
 *   <li><b>行快照</b>：生成时把工序按列 {@code field} 投影成字符串行（数值已格式化），
 *       末行追加合计行（{@code __summary__}），导出时加粗；路线后续改动需点「重新生成」刷新。</li>
 * </ul>
 */
@Service
public class CappCardService {

    /** 模板状态：草稿（可改）/ 已发布（可生成卡片）/ 已归档（历史版本）。 */
    private static final Set<String> TEMPLATE_STATUSES = Set.of("DRAFT", "RELEASED", "ARCHIVED");
    /** 卡片类型：工艺卡 / 工序卡 / 检验卡。 */
    private static final Set<String> CARD_TYPES = Set.of("PROCESS", "CRAFT", "INSPECT");
    private static final Map<String, String> OP_TYPE_LABELS = Map.of(
            "MACHINING", "机加工",
            "ASSEMBLY", "装配",
            "INSPECTION", "检验",
            "HEAT", "热处理",
            "SURFACE", "表面处理",
            "OTHER", "其它");
    /** 合计行标记（导出时加粗、不计入行数）。 */
    private static final String SUMMARY_KEY = "__summary__";

    /** 明细行可用变量（模板列 {@code field} 的取值域）。 */
    public static final Map<String, String> STEP_FIELDS = new LinkedHashMap<>();
    /** 表头可用变量（模板 {@code headerFields.field} 与卡片 {@code fieldValues} 的取值域）。 */
    public static final Map<String, String> CARD_FIELDS = new LinkedHashMap<>();

    static {
        STEP_FIELDS.put("seqNo", "工序号");
        STEP_FIELDS.put("name", "工序名称");
        STEP_FIELDS.put("opType", "工序类型");
        STEP_FIELDS.put("workCenter", "工作中心");
        STEP_FIELDS.put("resourceCode", "工艺资源编码");
        STEP_FIELDS.put("resourceName", "工艺资源名称");
        STEP_FIELDS.put("resourceSpec", "工艺资源规格");
        STEP_FIELDS.put("setupMinutes", "准备工时");
        STEP_FIELDS.put("runMinutes", "单件工时");
        STEP_FIELDS.put("totalMinutes", "工序工时合计");
        STEP_FIELDS.put("remark", "备注");

        CARD_FIELDS.put("objectCode", "零件编码");
        CARD_FIELDS.put("objectName", "零件名称");
        CARD_FIELDS.put("objectSpec", "规格型号");
        CARD_FIELDS.put("objectMaterial", "材料");
        CARD_FIELDS.put("objectUnit", "计量单位");
        CARD_FIELDS.put("routeCode", "路线编码");
        CARD_FIELDS.put("routeName", "路线名称");
        CARD_FIELDS.put("routeVersion", "路线版本");
        CARD_FIELDS.put("routeStatus", "路线状态");
        CARD_FIELDS.put("applicability", "适用性");
        CARD_FIELDS.put("ownerName", "工艺负责人");
        CARD_FIELDS.put("routeString", "路线串");
        CARD_FIELDS.put("totalMinutes", "总工时");
        CARD_FIELDS.put("cardCode", "卡片编码");
        CARD_FIELDS.put("templateName", "卡片模板");
    }

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public CappCardService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    // ------------------------------------------------------------------ 模板

    public PageResult<CappCardDtos.TemplateRow> templateList(Long companyId, String keyword, String cardType,
                                                             String status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE t.company_id = ? AND t.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (cardType != null && !cardType.isBlank()) {
            where.append(" AND t.card_type = ?");
            args.add(normalizeCardType(cardType));
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND t.status = ?");
            args.add(normalizeTemplateStatus(status));
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (t.code ILIKE ? OR t.name ILIKE ? OR t.title ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 1);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_card_template t" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<CappCardDtos.TemplateRow> items = jdbcTemplate.query("""
                        SELECT t.id, t.code, t.name, t.card_type, t.version_no, t.status, t.title, t.remark,
                               (SELECT COUNT(1) FROM capp_card c
                                 WHERE c.template_id = t.id AND c.deleted = false) AS card_count,
                               t.released_at, t.created_at, t.updated_at
                          FROM capp_card_template t
                        """ + where + " ORDER BY t.code, t.version_no DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new CappCardDtos.TemplateRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("card_type"), rs.getInt("version_no"),
                        rs.getString("status"), rs.getString("title"), rs.getString("remark"),
                        rs.getInt("card_count"),
                        rs.getObject("released_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePage, safeSize);
    }

    public CappCardDtos.TemplateDetail template(Long id, Long companyId) {
        List<CappCardDtos.TemplateDetail> rows = jdbcTemplate.query("""
                        SELECT t.id, t.code, t.name, t.card_type, t.version_no, t.status, t.title,
                               t.layout_json::text AS layout_text, t.remark,
                               (SELECT COUNT(1) FROM capp_card c
                                 WHERE c.template_id = t.id AND c.deleted = false) AS card_count,
                               t.released_at, t.created_at, t.updated_at
                          FROM capp_card_template t
                         WHERE t.id = ? AND t.company_id = ? AND t.deleted = false
                        """,
                (rs, rowNum) -> new CappCardDtos.TemplateDetail(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("card_type"), rs.getInt("version_no"),
                        rs.getString("status"), rs.getString("title"),
                        readLayout(rs.getString("layout_text")), rs.getString("remark"),
                        rs.getInt("card_count"),
                        rs.getObject("released_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_TEMPLATE_NOT_FOUND, "工艺卡片模板不存在：" + id);
        }
        return rows.get(0);
    }

    @Transactional
    public CappCardDtos.TemplateDetail createTemplate(CappCardDtos.TemplateSaveReq req, Long companyId,
                                                      Long userId) {
        CappCardDtos.CardLayout layout = normalizeLayout(req.layout(), req.title());
        String code = nextCode("CappCardTemplate", "CT");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_card_template (id, company_id, code, name, card_type, version_no,
                                                        status, title, layout_json, remark,
                                                        created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, 1, 'DRAFT', ?, CAST(? AS jsonb), ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), normalizeCardType(req.cardType()),
                blankToNull(layout.title()), toJson(layout), blankToNull(req.remark()), userId, userId);
        return template(id, companyId);
    }

    @Transactional
    public CappCardDtos.TemplateDetail updateTemplate(Long id, CappCardDtos.TemplateSaveReq req,
                                                      Long companyId, Long userId) {
        CappCardDtos.TemplateDetail current = template(id, companyId);
        requireDraft(current.status(), "只有草稿状态的模板可以修改，已发布模板请先「另存新版本」");
        CappCardDtos.CardLayout layout = normalizeLayout(req.layout(), req.title());
        jdbcTemplate.update("""
                        UPDATE capp_card_template
                           SET name = ?, card_type = ?, title = ?, layout_json = CAST(? AS jsonb),
                               remark = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeCardType(req.cardType()), blankToNull(layout.title()),
                toJson(layout), blankToNull(req.remark()), userId, id, companyId);
        return template(id, companyId);
    }

    /** 另存新版本（同 code，version_no + 1，草稿）：已发布模板的改版入口。 */
    @Transactional
    public CappCardDtos.TemplateDetail newTemplateVersion(Long id, Long companyId, Long userId) {
        CappCardDtos.TemplateDetail source = template(id, companyId);
        Integer maxVersion = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version_no), 0) FROM capp_card_template
                 WHERE company_id = ? AND code = ? AND deleted = false
                """, Integer.class, companyId, source.code());
        long newId = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_card_template (id, company_id, code, name, card_type, version_no,
                                                        status, title, layout_json, remark,
                                                        created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, CAST(? AS jsonb), ?, ?, ?)
                        """,
                newId, companyId, source.code(), source.name(), source.cardType(),
                (maxVersion == null ? 0 : maxVersion) + 1, source.title(),
                toJson(source.layout()), source.remark(), userId, userId);
        return template(newId, companyId);
    }

    /** 发布（同族其它已发布版本自动归档）。 */
    @Transactional
    public CappCardDtos.TemplateDetail releaseTemplate(Long id, Long companyId, Long userId) {
        CappCardDtos.TemplateDetail current = template(id, companyId);
        requireDraft(current.status(), "只有草稿状态的模板可以发布");
        if (current.layout().columns() == null || current.layout().columns().isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "模板没有明细列，不能发布");
        }
        jdbcTemplate.update("""
                UPDATE capp_card_template
                   SET status = 'ARCHIVED', updated_by = ?, updated_at = now()
                 WHERE company_id = ? AND code = ? AND id <> ?
                   AND status = 'RELEASED' AND deleted = false
                """, userId, companyId, current.code(), id);
        jdbcTemplate.update("""
                UPDATE capp_card_template
                   SET status = 'RELEASED', released_at = now(), updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
        return template(id, companyId);
    }

    /** 删除（仅草稿；已生成卡片的版本不能删，避免卡片失去模板追溯）。 */
    @Transactional
    public void deleteTemplate(Long id, Long companyId) {
        CappCardDtos.TemplateDetail current = template(id, companyId);
        requireDraft(current.status(), "只有草稿状态的模板可以删除");
        if (current.cardCount() > 0) {
            throw BizException.of(ErrorCode.COMMON_CONFLICT,
                    "该模板版本已被 " + current.cardCount() + " 张卡片引用，不能删除");
        }
        jdbcTemplate.update("""
                UPDATE capp_card_template SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ------------------------------------------------------------------ 卡片

    public PageResult<CappCardDtos.CardRow> cardList(Long companyId, String keyword, Long routingId,
                                                     Long templateId, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE c.company_id = ? AND c.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (routingId != null) {
            where.append(" AND c.routing_id = ?");
            args.add(routingId);
        }
        if (templateId != null) {
            where.append(" AND c.template_id = ?");
            args.add(templateId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (c.code ILIKE ? OR c.title ILIKE ? OR c.object_code ILIKE ?"
                    + " OR c.routing_code ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 1);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_card c" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add((long) (safePage - 1) * safeSize);
        List<CappCardDtos.CardRow> items = jdbcTemplate.query("""
                        SELECT c.id, c.code, c.title, c.template_id, c.template_code, c.template_version,
                               c.template_name, c.routing_id, c.routing_code, c.object_id, c.object_code,
                               o.name AS object_name,
                               (SELECT COUNT(1) FROM jsonb_array_elements(c.rows_json) e
                                 WHERE NOT (e @> '{"__summary__":"true"}'::jsonb)) AS row_count,
                               c.created_at, c.updated_at
                          FROM capp_card c
                          LEFT JOIN plm_object o ON o.id = c.object_id
                        """ + where + " ORDER BY c.updated_at DESC, c.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new CappCardDtos.CardRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("title"), rs.getLong("template_id"), rs.getString("template_code"),
                        rs.getInt("template_version"), rs.getString("template_name"),
                        rs.getLong("routing_id"), rs.getString("routing_code"),
                        rs.getLong("object_id"), rs.getString("object_code"), rs.getString("object_name"),
                        rs.getInt("row_count"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return new PageResult<>(items, total == null ? 0L : total, safePage, safeSize);
    }

    public CappCardDtos.CardDetail card(Long id, Long companyId) {
        List<CappCardDtos.CardDetail> rows = jdbcTemplate.query("""
                        SELECT c.id, c.code, c.title, c.template_id, c.template_code, c.template_version,
                               c.template_name, c.layout_json::text AS layout_text, c.routing_id,
                               c.routing_code, c.object_id, c.object_code, o.name AS object_name,
                               c.field_values::text AS field_text, c.rows_json::text AS rows_text,
                               c.remark, c.created_at, c.updated_at
                          FROM capp_card c
                          LEFT JOIN plm_object o ON o.id = c.object_id
                         WHERE c.id = ? AND c.company_id = ? AND c.deleted = false
                        """,
                (rs, rowNum) -> {
                    Map<String, String> fields = readStringMap(rs.getString("field_text"));
                    return new CappCardDtos.CardDetail(rs.getLong("id"), rs.getString("code"),
                            rs.getString("title"), rs.getLong("template_id"), rs.getString("template_code"),
                            rs.getInt("template_version"), rs.getString("template_name"),
                            readLayout(rs.getString("layout_text")),
                            rs.getLong("routing_id"), rs.getString("routing_code"),
                            rs.getLong("object_id"), rs.getString("object_code"), rs.getString("object_name"),
                            fields, readRowList(rs.getString("rows_text")),
                            rs.getString("remark"), fields.get("routeString"),
                            rs.getObject("created_at", OffsetDateTime.class),
                            rs.getObject("updated_at", OffsetDateTime.class));
                },
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_NOT_FOUND, "工艺卡片不存在：" + id);
        }
        return rows.get(0);
    }

    /** 生成卡片：已发布模板 + 工艺路线 → 版式快照 + 工序行快照 + 表头字段默认值。 */
    @Transactional
    public CappCardDtos.CardDetail createCard(CappCardDtos.CardSaveReq req, Long companyId, Long userId) {
        CappCardDtos.TemplateDetail template = template(req.templateId(), companyId);
        if (!"RELEASED".equals(template.status())) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID,
                    "只有已发布模板可以生成卡片（当前：" + template.status() + "）");
        }
        RoutingSnapshot routing = loadRouting(req.routingId(), companyId);
        Map<String, String> fields = buildFieldValues(template.name(), routing);
        if (req.fieldValues() != null) {
            req.fieldValues().forEach((key, value) -> {
                if (key != null && value != null) {
                    fields.put(key, value.trim());
                }
            });
        }
        List<Map<String, String>> rows = buildRows(routing.steps(), template.layout());
        String code = nextCode("CappCard", "CC");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_card (id, company_id, code, template_id, template_code,
                                               template_version, template_name, title, layout_json,
                                               routing_id, routing_code, object_id, object_code,
                                               field_values, rows_json, remark, created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS jsonb),
                                CAST(? AS jsonb), ?, ?, ?)
                        """,
                id, companyId, code, template.id(), template.code(), template.versionNo(),
                template.name(), blankToNull(template.layout().title()),
                toJson(template.layout()), routing.id(), routing.code(), routing.objectId(),
                routing.objectCode(), toJson(fields), toJson(rows), blankToNull(req.remark()),
                userId, userId);
        return card(id, companyId);
    }

    /** 重新生成行快照（路线工序改动后刷新；版式与模板版本保持不变）。 */
    @Transactional
    public CappCardDtos.CardDetail regenerateCard(Long id, Long companyId, Long userId) {
        CappCardDtos.CardDetail current = card(id, companyId);
        RoutingSnapshot routing = loadRouting(current.routingId(), companyId);
        Map<String, String> defaults = buildFieldValues(current.templateName(), routing);
        // 用户手填过的表头值保留，仅补齐缺失项
        Map<String, String> fields = new LinkedHashMap<>(defaults);
        if (current.fieldValues() != null) {
            current.fieldValues().forEach((key, value) -> {
                if (value != null && !value.isBlank()) {
                    fields.put(key, value);
                }
            });
        }
        List<Map<String, String>> rows = buildRows(routing.steps(), current.layout());
        jdbcTemplate.update("""
                        UPDATE capp_card
                           SET routing_code = ?, object_id = ?, object_code = ?,
                               field_values = CAST(? AS jsonb), rows_json = CAST(? AS jsonb),
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                routing.code(), routing.objectId(), routing.objectCode(), toJson(fields),
                toJson(rows), userId, id, companyId);
        return card(id, companyId);
    }

    @Transactional
    public void deleteCard(Long id, Long companyId) {
        card(id, companyId);
        jdbcTemplate.update("""
                UPDATE capp_card SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /** 可用变量字典（前端编辑模板时给出可选项，避免手打错字段名）。 */
    public Map<String, Object> fields() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stepFields", STEP_FIELDS);
        result.put("cardFields", CARD_FIELDS);
        result.put("cardTypes", Map.of("PROCESS", "工艺卡", "CRAFT", "工序卡", "INSPECT", "检验卡"));
        return result;
    }

    // ------------------------------------------------------------------ 内部：数据装配

    /** 表头字段默认值。 */
    private Map<String, String> buildFieldValues(String templateName, RoutingSnapshot routing) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("objectCode", nullToEmpty(routing.objectCode()));
        values.put("objectName", nullToEmpty(routing.objectName()));
        values.put("objectSpec", nullToEmpty(routing.specification()));
        values.put("objectMaterial", nullToEmpty(routing.material()));
        values.put("objectUnit", nullToEmpty(routing.unit()));
        values.put("routeCode", nullToEmpty(routing.code()));
        values.put("routeName", nullToEmpty(routing.name()));
        values.put("routeVersion", String.valueOf(routing.versionNo()));
        values.put("routeStatus", nullToEmpty(routing.status()));
        values.put("applicability", nullToEmpty(routing.applicability()));
        values.put("ownerName", nullToEmpty(routing.ownerName()));
        values.put("routeString", routeString(routing.steps()));
        values.put("totalMinutes", formatDecimal(totalMinutes(routing.steps())));
        values.put("cardCode", "");
        values.put("templateName", nullToEmpty(templateName));
        return values;
    }

    /** 按模板列把工序投影成字符串行，末行追加合计行。 */
    private List<Map<String, String>> buildRows(List<StepSnapshot> steps, CappCardDtos.CardLayout layout) {
        List<CappCardDtos.CardColumn> columns = layout == null || layout.columns() == null
                ? List.of() : layout.columns();
        List<Map<String, String>> rows = new ArrayList<>();
        BigDecimal setupTotal = BigDecimal.ZERO;
        BigDecimal runTotal = BigDecimal.ZERO;
        for (StepSnapshot step : steps) {
            Map<String, String> source = stepValues(step);
            Map<String, String> row = new LinkedHashMap<>();
            for (CappCardDtos.CardColumn column : columns) {
                row.put(columnKey(column), resolveCell(column, source));
            }
            rows.add(row);
            setupTotal = setupTotal.add(nullToZero(step.setupMinutes()));
            runTotal = runTotal.add(nullToZero(step.runMinutes()));
        }
        if (!rows.isEmpty()) {
            Map<String, String> summary = new LinkedHashMap<>();
            for (CappCardDtos.CardColumn column : columns) {
                String field = column.field();
                if ("setupMinutes".equals(field)) {
                    summary.put(columnKey(column), formatDecimal(setupTotal));
                } else if ("runMinutes".equals(field)) {
                    summary.put(columnKey(column), formatDecimal(runTotal));
                } else if ("totalMinutes".equals(field)) {
                    summary.put(columnKey(column), formatDecimal(setupTotal.add(runTotal)));
                } else {
                    summary.put(columnKey(column), "seqNo".equals(field) ? "合计" : "");
                }
            }
            summary.put(SUMMARY_KEY, "true");
            rows.add(summary);
        }
        return rows;
    }

    private Map<String, String> stepValues(StepSnapshot step) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("seqNo", String.valueOf(step.seqNo()));
        values.put("name", nullToEmpty(step.name()));
        values.put("opType", OP_TYPE_LABELS.getOrDefault(step.opType(), nullToEmpty(step.opType())));
        values.put("workCenter", nullToEmpty(step.workCenter()));
        values.put("resourceCode", nullToEmpty(step.resourceCode()));
        values.put("resourceName", nullToEmpty(step.resourceName()));
        values.put("resourceSpec", nullToEmpty(step.resourceSpec()));
        values.put("setupMinutes", formatDecimal(step.setupMinutes()));
        values.put("runMinutes", formatDecimal(step.runMinutes()));
        values.put("totalMinutes", formatDecimal(
                nullToZero(step.setupMinutes()).add(nullToZero(step.runMinutes()))));
        values.put("remark", nullToEmpty(step.remark()));
        return values;
    }

    private static String resolveCell(CappCardDtos.CardColumn column, Map<String, String> source) {
        String field = column.field();
        if (field == null || field.isBlank()) {
            return nullToEmpty(column.constValue());
        }
        return nullToEmpty(source.get(field.trim()));
    }

    /** 列在当前版本内的唯一键：优先 key，其次 field，最后退化为标题（导出与行快照共用）。 */
    public static String columnKey(CappCardDtos.CardColumn column) {
        if (column.key() != null && !column.key().isBlank()) {
            return column.key().trim();
        }
        if (column.field() != null && !column.field().isBlank()) {
            return column.field().trim();
        }
        return column.title();
    }

    private RoutingSnapshot loadRouting(Long routingId, Long companyId) {
        List<RoutingSnapshot> routings = jdbcTemplate.query("""
                        SELECT rt.id, rt.code, rt.name, rt.version_no, rt.status, rt.applicability,
                               rt.object_id, o.code AS object_code, o.name AS object_name,
                               m.specification, m.material, m.unit, u.display_name AS owner_name
                          FROM capp_routing rt
                          LEFT JOIN plm_object o ON o.id = rt.object_id
                          LEFT JOIN item_master m ON m.object_id = rt.object_id
                          LEFT JOIN sys_user u ON u.id = rt.owner_id
                         WHERE rt.id = ? AND rt.company_id = ? AND rt.deleted = false
                        """,
                (rs, rowNum) -> new RoutingSnapshot(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getInt("version_no"), rs.getString("status"),
                        rs.getString("applicability"), rs.getLong("object_id"),
                        rs.getString("object_code"), rs.getString("object_name"),
                        rs.getString("specification"), rs.getString("material"), rs.getString("unit"),
                        rs.getString("owner_name"), stepsOf(routingId, companyId)),
                routingId, companyId);
        if (routings.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_ROUTING_NOT_FOUND, "工艺路线不存在：" + routingId);
        }
        return routings.get(0);
    }

    private List<StepSnapshot> stepsOf(Long routingId, Long companyId) {
        return jdbcTemplate.query("""
                        SELECT s.seq_no, s.name, s.op_type, s.work_center, s.setup_minutes, s.run_minutes,
                               s.remark, r.code AS resource_code, r.name AS resource_name,
                               r.spec AS resource_spec
                          FROM capp_routing_step s
                          LEFT JOIN capp_resource r ON r.id = s.resource_id AND r.deleted = false
                         WHERE s.routing_id = ? AND s.company_id = ?
                         ORDER BY s.seq_no
                        """,
                (rs, rowNum) -> new StepSnapshot(rs.getInt("seq_no"), rs.getString("name"),
                        rs.getString("op_type"), rs.getString("work_center"),
                        rs.getBigDecimal("setup_minutes"), rs.getBigDecimal("run_minutes"),
                        rs.getString("remark"), rs.getString("resource_code"),
                        rs.getString("resource_name"), rs.getString("resource_spec")),
                routingId, companyId);
    }

    private static String routeString(List<StepSnapshot> steps) {
        return steps.stream()
                .map(step -> step.seqNo() + " " + nullToEmpty(step.name()))
                .reduce((left, right) -> left + " → " + right)
                .orElse("");
    }

    private static BigDecimal totalMinutes(List<StepSnapshot> steps) {
        return steps.stream()
                .map(step -> nullToZero(step.setupMinutes()).add(nullToZero(step.runMinutes())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ 内部：版式校验

    /** 版式规范化与校验：列的 field 必须在 STEP_FIELDS 内，表头 field 必须在 CARD_FIELDS 内。 */
    private CappCardDtos.CardLayout normalizeLayout(CappCardDtos.CardLayout layout, String fallbackTitle) {
        if (layout == null || layout.columns() == null || layout.columns().isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "模板至少要有一列明细");
        }
        List<CappCardDtos.CardColumn> columns = new ArrayList<>();
        Set<String> keys = new java.util.LinkedHashSet<>();
        for (CappCardDtos.CardColumn column : layout.columns()) {
            if (column == null) {
                continue;
            }
            String field = column.field() == null ? null : column.field().trim();
            if (field != null && !field.isEmpty() && !STEP_FIELDS.containsKey(field)) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知的明细变量：" + field
                        + "（可选 " + String.join("/", STEP_FIELDS.keySet()) + "）");
            }
            CappCardDtos.CardColumn normalized = new CappCardDtos.CardColumn(
                    column.key() == null ? null : column.key().trim(),
                    column.title() == null ? "" : column.title().trim(),
                    field == null || field.isEmpty() ? null : field,
                    column.constValue() == null ? null : column.constValue().trim(),
                    column.width() == null ? 12 : Math.min(Math.max(column.width(), 4), 60),
                    normalizeAlign(column.align()));
            if (!keys.add(columnKey(normalized))) {
                throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "明细列重复：" + columnKey(normalized));
            }
            columns.add(normalized);
        }
        List<CappCardDtos.HeaderField> headers = new ArrayList<>();
        if (layout.headerFields() != null) {
            for (CappCardDtos.HeaderField header : layout.headerFields()) {
                if (header == null) {
                    continue;
                }
                String field = header.field() == null ? null : header.field().trim();
                if (field != null && !field.isEmpty() && !CARD_FIELDS.containsKey(field)) {
                    throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知的表头变量：" + field
                            + "（可选 " + String.join("/", CARD_FIELDS.keySet()) + "）");
                }
                headers.add(new CappCardDtos.HeaderField(
                        header.label() == null ? "" : header.label().trim(),
                        field == null || field.isEmpty() ? null : field));
            }
        }
        String title = layout.title() == null || layout.title().isBlank()
                ? blankToNull(fallbackTitle) : layout.title().trim();
        return new CappCardDtos.CardLayout(title, headers, columns,
                layout.footerText() == null ? null : layout.footerText().trim());
    }

    private static String normalizeAlign(String align) {
        if (align == null || align.isBlank()) {
            return "LEFT";
        }
        String value = align.trim().toUpperCase();
        return Set.of("LEFT", "CENTER", "RIGHT").contains(value) ? value : "LEFT";
    }

    private static String normalizeCardType(String cardType) {
        if (cardType == null || cardType.isBlank()) {
            return "PROCESS";
        }
        String value = cardType.trim().toUpperCase();
        if (!CARD_TYPES.contains(value)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知卡片类型：" + cardType
                    + "（可选 " + String.join("/", CARD_TYPES) + "）");
        }
        return value;
    }

    private static String normalizeTemplateStatus(String status) {
        String value = status.trim().toUpperCase();
        if (!TEMPLATE_STATUSES.contains(value)) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "未知模板状态：" + status);
        }
        return value;
    }

    private static void requireDraft(String status, String message) {
        if (!"DRAFT".equals(status)) {
            throw BizException.of(ErrorCode.CAPP_TEMPLATE_STATE_INVALID,
                    message + "（当前状态：" + status + "）");
        }
    }

    // ------------------------------------------------------------------ 内部：工具

    private CappCardDtos.CardLayout readLayout(String text) {
        if (text == null || text.isBlank()) {
            return new CappCardDtos.CardLayout(null, List.of(), List.of(), null);
        }
        try {
            return jsonMapper.readValue(text, CappCardDtos.CardLayout.class);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "卡片版式解析失败：" + ex.getMessage());
        }
    }

    private Map<String, String> readStringMap(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> raw = jsonMapper.readValue(text,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                    });
            return raw == null ? new LinkedHashMap<>() : new LinkedHashMap<>(raw);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<Map<String, String>> readRowList(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, String>> rows = jsonMapper.readValue(text,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {
                    });
            return rows == null ? new ArrayList<>() : rows;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "卡片数据序列化失败：" + ex.getMessage());
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

    private static String formatDecimal(BigDecimal value) {
        return nullToZero(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record StepSnapshot(int seqNo, String name, String opType, String workCenter,
                                BigDecimal setupMinutes, BigDecimal runMinutes, String remark,
                                String resourceCode, String resourceName, String resourceSpec) {
    }

    private record RoutingSnapshot(Long id, String code, String name, int versionNo, String status,
                                   String applicability, Long objectId, String objectCode,
                                   String objectName, String specification, String material,
                                   String unit, String ownerName, List<StepSnapshot> steps) {
    }
}
