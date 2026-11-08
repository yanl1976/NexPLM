package com.nexplm.platform.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.platform.analysis.api.ReportDtos;
import org.springframework.dao.DuplicateKeyException;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 复刻批次 D｜报表管理：模板（数据集 + 参数）+ 运行 + 导出数据。
 *
 * <p><b>安全边界与 P10-2 一致</b>：数据集是编译期注册的白名单（{@link #DATASETS}），每个数据集
 * 一条固定 SQL 骨架；用户输入只以 {@code ?} 绑定参数进入，永远拼不进 SQL 文本，参数键也必须在
 * 白名单内。因此「能建报表模板」不等于「能跨公司读全库」或「能跑慢查询」。</p>
 *
 * <p><b>口径</b>：五张内置报表分别对齐文档/零部件/变更/工艺/项目模块的列表接口
 * （文档只取 {@code is_latest}、工时只认 {@code APPROVED}、工艺只算本公司未删除等），
 * 不在报表侧另立口径——否则同一张表两个页面两种数字。</p>
 */
@Service
public class ReportService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;
    /** 导出口径：单次最多 20000 行，超出的在导出页脚注明，不静默截断。 */
    public static final int MAX_EXPORT_ROWS = 20000;

    private static final Map<String, ReportDtos.DatasetMeta> DATASETS = buildDatasets();

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CodeRuleService codeRuleService;

    public ReportService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                         CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.codeRuleService = codeRuleService;
    }

    // ------------------------------------------------------------ 数据集字典

    public List<ReportDtos.DatasetMeta> datasets() {
        return List.copyOf(DATASETS.values());
    }

    // ------------------------------------------------------------ 模板 CRUD

    public PageResult<ReportDtos.TemplateRow> page(Long companyId, String keyword, String dataset,
                                                   Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        StringBuilder where = new StringBuilder(
                " WHERE deleted = false AND (company_id IS NULL OR company_id = ?)");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (dataset != null && !dataset.isBlank()) {
            where.append(" AND dataset = ?");
            args.add(dataset.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM rpt_template" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((long) (pageNo - 1) * pageSize);
        List<ReportDtos.TemplateRow> rows = jdbcTemplate.query("""
                        SELECT id, company_id, code, name, dataset, category, params, description,
                               built_in, sort_no, updated_at
                          FROM rpt_template
                        """ + where + " ORDER BY sort_no, code LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapRow(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                        rs.getString("dataset"), rs.getString("category"), rs.getString("params"),
                        rs.getString("description"), rs.getBoolean("built_in"), rs.getInt("sort_no"),
                        rs.getTimestamp("updated_at") == null ? null
                                : rs.getTimestamp("updated_at").toInstant()
                                .atOffset(java.time.ZoneOffset.UTC)),
                pageArgs.toArray());
        return PageResult.of(rows, total == null ? 0L : total, pageNo, pageSize);
    }

    public ReportDtos.TemplateRow detail(Long id, Long companyId) {
        return mapTemplateRow(requireTemplate(id, companyId));
    }

    @Transactional
    public ReportDtos.TemplateRow create(ReportDtos.TemplateSaveReq req, Long companyId, Long userId) {
        String dataset = requireDataset(req.dataset());
        List<ReportDtos.ParamReq> params = validateParams(dataset, req.params());
        String code = req.code() == null || req.code().isBlank()
                ? nextCode(companyId) : req.code().trim();
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO rpt_template (id, company_id, code, name, dataset, category,
                                   params, description, built_in, sort_no, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, false, ?, ?, ?)
                            """,
                    id, companyId, code, req.name().trim(), dataset,
                    blankToNull(req.category()) == null ? "OTHER" : req.category().trim().toUpperCase(),
                    toJson(params), blankToNull(req.description()),
                    req.sortNo() == null ? 100 : req.sortNo(), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ANA_REPORT_DUPLICATED, "报表编码已存在：" + code);
        }
        return detail(id, companyId);
    }

    @Transactional
    public ReportDtos.TemplateRow update(Long id, ReportDtos.TemplateSaveReq req, Long companyId,
                                         Long userId) {
        Map<String, Object> old = requireTemplate(id, companyId);
        requireEditable(old);
        String dataset = requireDataset(req.dataset());
        List<ReportDtos.ParamReq> params = validateParams(dataset, req.params());
        String code = req.code() == null || req.code().isBlank()
                ? String.valueOf(old.get("code")) : req.code().trim();
        try {
            jdbcTemplate.update("""
                            UPDATE rpt_template
                               SET code = ?, name = ?, dataset = ?, category = ?, params = CAST(? AS jsonb),
                                   description = ?, sort_no = ?, updated_by = ?, updated_at = now()
                             WHERE id = ? AND deleted = false
                            """,
                    code, req.name().trim(), dataset,
                    blankToNull(req.category()) == null ? "OTHER" : req.category().trim().toUpperCase(),
                    toJson(params), blankToNull(req.description()),
                    req.sortNo() == null ? 100 : req.sortNo(), userId, id);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ANA_REPORT_DUPLICATED, "报表编码已存在：" + code);
        }
        return detail(id, companyId);
    }

    /** 删除：内置模板不可删（全局共享），只能软删本公司自建模板。 */
    @Transactional
    public void delete(Long id, Long companyId, Long userId) {
        Map<String, Object> old = requireTemplate(id, companyId);
        requireEditable(old);
        jdbcTemplate.update("""
                UPDATE rpt_template SET deleted = true, updated_by = ?, updated_at = now()
                 WHERE id = ?
                """, userId, id);
    }

    // ------------------------------------------------------------ 运行

    public ReportDtos.RunResult run(Long id, Long companyId, ReportDtos.RunReq req) {
        Map<String, Object> raw = req == null || req.params() == null ? Map.of() : req.params();
        int pageNo = req == null || req.page() == null || req.page() < 1 ? 1 : req.page();
        int pageSize = req == null || req.size() == null || req.size() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(req.size(), MAX_PAGE_SIZE);
        return runTemplate(id, companyId, raw, pageNo, pageSize);
    }

    /** 导出用：一次取到上限行数，不分页（导出就是"全部结果"的语义）。 */
    public ReportDtos.RunResult runForExport(Long id, Long companyId, Map<String, Object> params) {
        return runTemplate(id, companyId, params == null ? Map.of() : params, 1, MAX_EXPORT_ROWS);
    }

    private ReportDtos.RunResult runTemplate(Long id, Long companyId, Map<String, Object> raw,
                                             int pageNo, int pageSize) {
        Map<String, Object> template = requireTemplate(id, companyId);
        String dataset = String.valueOf(template.get("dataset"));
        ReportDtos.DatasetMeta meta = DATASETS.get(dataset);
        if (meta == null) {
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                    "数据集已下线：" + dataset + "（请切换到可用数据集）");
        }
        Map<String, Object> params = normalizeValues(meta, raw);
        SqlSpec spec = buildQuery(dataset, companyId, params);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM (" + spec.select() + ") t", Long.class, spec.args().toArray());
        List<Object> pageArgs = new ArrayList<>(spec.args());
        pageArgs.add(pageSize);
        pageArgs.add((long) (pageNo - 1) * pageSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                spec.select() + spec.orderBy() + " LIMIT ? OFFSET ?", pageArgs.toArray());
        return new ReportDtos.RunResult(dataset, meta.label(),
                String.valueOf(template.get("code")), String.valueOf(template.get("name")),
                meta.columns(), rows, total == null ? 0L : total, pageNo, pageSize);
    }

    // ------------------------------------------------------------ 内部：模板

    private Map<String, Object> requireTemplate(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, company_id, code, name, dataset, category, params, description,
                       built_in, sort_no, updated_at
                  FROM rpt_template
                 WHERE id = ? AND deleted = false AND (company_id IS NULL OR company_id = ?)
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ANA_REPORT_NOT_FOUND, "报表模板不存在");
        }
        return rows.get(0);
    }

    /** 内置模板（company_id 为空）全局只读：改它等于改所有公司的报表。 */
    private void requireEditable(Map<String, Object> row) {
        if (Boolean.TRUE.equals(row.get("built_in"))) {
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                    "内置报表不可修改或删除（可复制编码后自建一张）");
        }
    }

    private ReportDtos.TemplateRow mapTemplateRow(Map<String, Object> row) {
        return mapRow(((Number) row.get("id")).longValue(), String.valueOf(row.get("code")),
                String.valueOf(row.get("name")), String.valueOf(row.get("dataset")),
                row.get("category") == null ? "OTHER" : String.valueOf(row.get("category")),
                row.get("params") == null ? null : String.valueOf(row.get("params")),
                row.get("description") == null ? null : String.valueOf(row.get("description")),
                Boolean.TRUE.equals(row.get("built_in")),
                row.get("sort_no") == null ? 100 : ((Number) row.get("sort_no")).intValue(),
                row.get("updated_at") == null ? null
                        : ((java.sql.Timestamp) row.get("updated_at")).toInstant()
                        .atOffset(java.time.ZoneOffset.UTC));
    }

    private ReportDtos.TemplateRow mapRow(Long id, String code, String name, String dataset,
                                          String category, String paramsJson, String description,
                                          boolean builtIn, int sortNo,
                                          java.time.OffsetDateTime updatedAt) {
        ReportDtos.DatasetMeta meta = DATASETS.get(dataset);
        List<ReportDtos.ParamMeta> params = resolveParams(meta, paramsJson);
        return new ReportDtos.TemplateRow(id, code, name, dataset,
                meta == null ? "(数据集已下线)" : meta.label(), category, params,
                description, builtIn, sortNo, updatedAt);
    }

    /** 模板里存的只是「用哪些参数 + 显示名」，类型与选项一律回填数据集字典。 */
    private List<ReportDtos.ParamMeta> resolveParams(ReportDtos.DatasetMeta meta, String paramsJson) {
        List<ReportDtos.ParamMeta> result = new ArrayList<>();
        if (meta == null || paramsJson == null || paramsJson.isBlank()) {
            return result;
        }
        try {
            JsonNode node = objectMapper.readTree(paramsJson);
            if (!node.isArray()) {
                return result;
            }
            for (JsonNode item : node) {
                String key = item.path("key").asText("");
                ReportDtos.ParamMeta metaParam = findParam(meta, key);
                if (metaParam == null) {
                    continue;
                }
                String label = item.path("label").asText("");
                result.add(new ReportDtos.ParamMeta(key, label.isBlank() ? metaParam.label() : label,
                        metaParam.type(), metaParam.options()));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return result;
    }

    private static ReportDtos.ParamMeta findParam(ReportDtos.DatasetMeta meta, String key) {
        return meta.params().stream().filter(p -> p.key().equals(key)).findFirst().orElse(null);
    }

    private String requireDataset(String dataset) {
        String key = dataset == null ? "" : dataset.trim();
        if (!DATASETS.containsKey(key)) {
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                    "未知数据集：" + key + "（可用 " + String.join(" / ", DATASETS.keySet()) + "）");
        }
        return key;
    }

    /** 参数白名单校验：只允许数据集声明的键，重复键直接拒绝。 */
    private List<ReportDtos.ParamReq> validateParams(String dataset,
                                                    List<ReportDtos.ParamReq> params) {
        ReportDtos.DatasetMeta meta = DATASETS.get(dataset);
        List<ReportDtos.ParamReq> result = new ArrayList<>();
        if (params == null || params.isEmpty()) {
            return result;
        }
        for (ReportDtos.ParamReq param : params) {
            if (param == null || param.key() == null || param.key().isBlank()) {
                throw BizException.of(ErrorCode.ANA_REPORT_INVALID, "参数 key 不能为空");
            }
            String key = param.key().trim();
            ReportDtos.ParamMeta metaParam = meta == null ? null : findParam(meta, key);
            if (metaParam == null) {
                throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                        "数据集 " + dataset + " 不支持参数：" + key);
            }
            boolean duplicated = result.stream().anyMatch(item -> item.key().equals(key));
            if (duplicated) {
                throw BizException.of(ErrorCode.ANA_REPORT_INVALID, "参数重复：" + key);
            }
            String label = param.label() == null || param.label().isBlank()
                    ? metaParam.label() : param.label().trim();
            result.add(new ReportDtos.ParamReq(key, label));
        }
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.ANA_REPORT_INVALID, "参数定义序列化失败");
        }
    }

    /**
     * 自建报表取号：RPT-000001（与内置的 RPT-DOC-LEDGER 这类可读编码不冲突）。
     * 走 code_rule（台账 B19）：原实现的 COUNT(*)+1 在删除模板后会回退重号，
     * 现在的流水单调递增，迁移已按既有 RPT- 数字最大值回填。
     */
    private String nextCode(Long companyId) {
        return codeRuleService.next("ReportTemplate");
    }

    // ------------------------------------------------------------ 内部：查询

    /** 参数值清洗：空白视为未填；日期必须是 yyyy-MM-dd（避免把脏串塞进 SQL 比较）。 */
    private Map<String, Object> normalizeValues(ReportDtos.DatasetMeta meta,
                                                Map<String, Object> raw) {
        // 运行时也只认数据集声明的键：多余键直接拒绝，避免前端/调用方误以为能扩展条件
        for (String key : raw.keySet()) {
            if (findParam(meta, key) == null) {
                throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                        "数据集 " + meta.key() + " 不支持参数：" + key);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (ReportDtos.ParamMeta param : meta.params()) {
            Object value = raw.get(param.key());
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                continue;
            }
            if ("DATE".equals(param.type())) {
                try {
                    result.put(param.key(), LocalDate.parse(text).toString());
                } catch (DateTimeParseException ex) {
                    throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                            "参数 " + param.label() + " 不是合法日期（yyyy-MM-dd）：" + text);
                }
            } else if ("SELECT".equals(param.type())) {
                boolean allowed = param.options() != null && param.options().stream()
                        .anyMatch(option -> option.value().equals(text));
                if (!allowed) {
                    throw BizException.of(ErrorCode.ANA_REPORT_INVALID,
                            "参数 " + param.label() + " 取值不在候选内：" + text);
                }
                result.put(param.key(), text);
            } else {
                result.put(param.key(), text);
            }
        }
        return result;
    }

    private record SqlSpec(String select, List<Object> args, String orderBy) {
    }

    private SqlSpec buildQuery(String dataset, Long companyId, Map<String, Object> p) {
        List<Object> args = new ArrayList<>();
        String select;
        String orderBy;
        switch (dataset) {
            case "doc.ledger" -> {
                select = """
                        SELECT o.code, o.name, dm.doc_type, o.revision, o.iteration,
                               o.lifecycle_state, o.released_at, u.display_name AS owner_name,
                               o.created_at
                          FROM plm_object o
                          LEFT JOIN doc_master dm ON dm.object_id = o.id
                          LEFT JOIN sys_user u ON u.id = o.owner_id
                         WHERE o.object_type = 'PdmDoc' AND o.company_id = ? AND o.deleted = false
                           AND o.is_latest = true
                        """;
                args.add(companyId);
                String keyword = text(p, "keyword");
                if (keyword != null) {
                    select += " AND (o.code ILIKE ? OR o.name ILIKE ?)";
                    args.add(like(keyword));
                    args.add(like(keyword));
                }
                String docType = text(p, "docType");
                if (docType != null) {
                    select += " AND dm.doc_type = ?";
                    args.add(docType);
                }
                String state = text(p, "state");
                if (state != null) {
                    select += " AND o.lifecycle_state = ?";
                    args.add(state);
                }
                String from = text(p, "dateFrom");
                if (from != null) {
                    select += " AND o.created_at >= CAST(? AS date)";
                    args.add(from);
                }
                String to = text(p, "dateTo");
                if (to != null) {
                    select += " AND o.created_at < CAST(? AS date) + 1";
                    args.add(to);
                }
                orderBy = " ORDER BY o.code";
            }
            case "bom.detail" -> {
                select = """
                        SELECT parent.code AS parent_code, parent.name AS parent_name, b.view_type,
                               b.line_no, child.code AS child_code, child.name AS child_name,
                               b.qty, b.unit, b.loss_rate, b.ref_designator
                          FROM bom_line b
                          JOIN plm_object parent ON parent.id = b.parent_object_id AND parent.deleted = false
                          JOIN plm_object child ON child.id = b.child_object_id AND child.deleted = false
                         WHERE b.deleted = false AND parent.company_id = ?
                        """;
                args.add(companyId);
                String keyword = text(p, "keyword");
                if (keyword != null) {
                    select += " AND (parent.code ILIKE ? OR parent.name ILIKE ?"
                            + " OR child.code ILIKE ? OR child.name ILIKE ?)";
                    for (int i = 0; i < 4; i++) {
                        args.add(like(keyword));
                    }
                }
                String viewType = text(p, "viewType");
                if (viewType != null) {
                    select += " AND b.view_type = ?";
                    args.add(viewType);
                }
                orderBy = " ORDER BY parent.code, b.view_type, b.line_no";
            }
            case "change.ecr" -> {
                select = """
                        SELECT e.code, e.title, e.change_type, e.priority, e.status,
                               rq.display_name AS requester_name, re.display_name AS receiver_name,
                               e.target_date, e.approved_at, e.closed_at, e.created_at
                          FROM chg_ecr e
                          LEFT JOIN sys_user rq ON rq.id = e.requester_id
                          LEFT JOIN sys_user re ON re.id = e.receiver_id
                         WHERE e.company_id = ? AND e.deleted = false
                        """;
                args.add(companyId);
                String status = text(p, "status");
                if (status != null) {
                    select += " AND e.status = ?";
                    args.add(status);
                }
                String changeType = text(p, "changeType");
                if (changeType != null) {
                    select += " AND e.change_type = ?";
                    args.add(changeType);
                }
                String from = text(p, "dateFrom");
                if (from != null) {
                    select += " AND e.created_at >= CAST(? AS date)";
                    args.add(from);
                }
                String to = text(p, "dateTo");
                if (to != null) {
                    select += " AND e.created_at < CAST(? AS date) + 1";
                    args.add(to);
                }
                orderBy = " ORDER BY e.created_at DESC, e.code";
            }
            case "capp.routing" -> {
                select = """
                        SELECT r.code, r.name, o.code AS object_code, o.name AS object_name,
                               r.version_no, r.status, r.typical,
                               (SELECT COUNT(1) FROM capp_routing_step s
                                 WHERE s.routing_id = r.id) AS step_count,
                               (SELECT COALESCE(SUM(s.setup_minutes + s.run_minutes), 0)
                                  FROM capp_routing_step s WHERE s.routing_id = r.id) AS total_minutes,
                               r.updated_at
                          FROM capp_routing r
                          LEFT JOIN plm_object o ON o.id = r.object_id AND o.deleted = false
                         WHERE r.company_id = ? AND r.deleted = false
                        """;
                args.add(companyId);
                String keyword = text(p, "keyword");
                if (keyword != null) {
                    select += " AND (r.code ILIKE ? OR r.name ILIKE ? OR o.code ILIKE ? OR o.name ILIKE ?)";
                    for (int i = 0; i < 4; i++) {
                        args.add(like(keyword));
                    }
                }
                String status = text(p, "status");
                if (status != null) {
                    select += " AND r.status = ?";
                    args.add(status);
                }
                String typical = text(p, "typical");
                if (typical != null) {
                    select += " AND r.typical = ?";
                    args.add(Boolean.parseBoolean(typical));
                }
                orderBy = " ORDER BY r.code";
            }
            case "project.timesheet" -> {
                select = """
                        SELECT p.code AS project_code, p.name AS project_name, t.wbs_code,
                               t.name AS task_name, u.display_name AS user_name,
                               SUM(ts.hours) AS hours, COUNT(1) AS entries
                          FROM prj_timesheet ts
                          JOIN prj_project p ON p.id = ts.project_id
                          JOIN prj_task t ON t.id = ts.task_id
                          LEFT JOIN sys_user u ON u.id = ts.user_id
                         WHERE ts.company_id = ? AND ts.deleted = false AND ts.status = 'APPROVED'
                        """;
                args.add(companyId);
                String projectCode = text(p, "projectCode");
                if (projectCode != null) {
                    select += " AND p.code ILIKE ?";
                    args.add(like(projectCode));
                }
                String from = text(p, "dateFrom");
                if (from != null) {
                    select += " AND ts.work_date >= CAST(? AS date)";
                    args.add(from);
                }
                String to = text(p, "dateTo");
                if (to != null) {
                    select += " AND ts.work_date <= CAST(? AS date)";
                    args.add(to);
                }
                select += " GROUP BY p.code, p.name, t.wbs_code, t.name, u.display_name";
                orderBy = " ORDER BY p.code, t.wbs_code, u.display_name";
            }
            default -> throw BizException.of(ErrorCode.ANA_REPORT_INVALID, "未知数据集：" + dataset);
        }
        return new SqlSpec(select, args, orderBy);
    }

    private static String text(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static String like(String value) {
        return "%" + value + "%";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // ------------------------------------------------------------ 数据集定义

    private static Map<String, ReportDtos.DatasetMeta> buildDatasets() {
        Map<String, ReportDtos.DatasetMeta> map = new LinkedHashMap<>();
        map.put("doc.ledger", new ReportDtos.DatasetMeta("doc.ledger", "文档台账", "DOC",
                List.of(
                        col("code", "编码", "TEXT"),
                        col("name", "名称", "TEXT"),
                        col("doc_type", "文档类型", "TEXT"),
                        col("revision", "版本", "NUMBER"),
                        col("iteration", "迭代", "NUMBER"),
                        col("lifecycle_state", "状态", "TEXT"),
                        col("released_at", "发布日", "DATETIME"),
                        col("owner_name", "负责人", "TEXT"),
                        col("created_at", "创建时间", "DATETIME")),
                List.of(
                        text("keyword", "编码/名称"),
                        select("docType", "文档类型", option("DESIGN", "设计图纸"),
                                option("TECH", "技术文件"), option("PROCESS", "工艺文件"),
                                option("STANDARD", "标准规范"), option("OTHER", "其他")),
                        text("state", "生命周期状态"),
                        date("dateFrom", "创建日期起"),
                        date("dateTo", "创建日期止"))));

        map.put("bom.detail", new ReportDtos.DatasetMeta("bom.detail", "BOM 明细", "ITEM",
                List.of(
                        col("parent_code", "父件编码", "TEXT"),
                        col("parent_name", "父件名称", "TEXT"),
                        col("view_type", "视图", "TEXT"),
                        col("line_no", "序号", "NUMBER"),
                        col("child_code", "子件编码", "TEXT"),
                        col("child_name", "子件名称", "TEXT"),
                        col("qty", "用量", "NUMBER"),
                        col("unit", "单位", "TEXT"),
                        col("loss_rate", "损耗率(%)", "NUMBER"),
                        col("ref_designator", "位号", "TEXT")),
                List.of(
                        text("keyword", "父/子件编码或名称"),
                        select("viewType", "视图", option("DESIGN", "设计"),
                                option("PROCESS", "工艺"), option("MANUFACTURE", "制造")))));

        map.put("change.ecr", new ReportDtos.DatasetMeta("change.ecr", "变更执行台账", "CHANGE",
                List.of(
                        col("code", "变更单号", "TEXT"),
                        col("title", "标题", "TEXT"),
                        col("change_type", "变更类型", "TEXT"),
                        col("priority", "优先级", "TEXT"),
                        col("status", "状态", "TEXT"),
                        col("requester_name", "申请人", "TEXT"),
                        col("receiver_name", "接收人", "TEXT"),
                        col("target_date", "期望完成日", "DATE"),
                        col("approved_at", "批准时间", "DATETIME"),
                        col("closed_at", "关闭时间", "DATETIME"),
                        col("created_at", "创建时间", "DATETIME")),
                List.of(
                        select("status", "状态", option("DRAFT", "草稿"),
                                option("SUBMITTED", "已提交"), option("REVIEWING", "评审中"),
                                option("APPROVED", "已批准"), option("NOTICED", "已通知"),
                                option("EXECUTING", "执行中"), option("CLOSED", "已关闭")),
                        select("changeType", "变更类型", option("DESIGN", "设计变更"),
                                option("PROCESS", "工艺变更")),
                        date("dateFrom", "创建日期起"),
                        date("dateTo", "创建日期止"))));

        map.put("capp.routing", new ReportDtos.DatasetMeta("capp.routing", "工艺路线汇总", "CAPP",
                List.of(
                        col("code", "路线编码", "TEXT"),
                        col("name", "路线名称", "TEXT"),
                        col("object_code", "零件编码", "TEXT"),
                        col("object_name", "零件名称", "TEXT"),
                        col("version_no", "版本", "NUMBER"),
                        col("status", "状态", "TEXT"),
                        col("typical", "典型工艺", "BOOL"),
                        col("step_count", "工序数", "NUMBER"),
                        col("total_minutes", "总工时(h)", "NUMBER"),
                        col("updated_at", "更新时间", "DATETIME")),
                List.of(
                        text("keyword", "路线/零件编码或名称"),
                        select("status", "状态", option("DRAFT", "草稿"),
                                option("RELEASED", "已发布"), option("ARCHIVED", "已归档")),
                        select("typical", "典型工艺", option("true", "是"), option("false", "否")))));

        map.put("project.timesheet", new ReportDtos.DatasetMeta("project.timesheet", "项目工时汇总",
                "PROJECT",
                List.of(
                        col("project_code", "项目编码", "TEXT"),
                        col("project_name", "项目名称", "TEXT"),
                        col("wbs_code", "WBS", "TEXT"),
                        col("task_name", "任务名称", "TEXT"),
                        col("user_name", "填报人", "TEXT"),
                        col("hours", "已审批工时(h)", "NUMBER"),
                        col("entries", "填报条数", "NUMBER")),
                List.of(
                        text("projectCode", "项目编码"),
                        date("dateFrom", "工作日期起"),
                        date("dateTo", "工作日期止"))));

        return Map.copyOf(map);
    }

    private static ReportDtos.ColumnMeta col(String key, String label, String type) {
        return new ReportDtos.ColumnMeta(key, label, type);
    }

    private static ReportDtos.ParamMeta text(String key, String label) {
        return new ReportDtos.ParamMeta(key, label, "TEXT", null);
    }

    private static ReportDtos.ParamMeta date(String key, String label) {
        return new ReportDtos.ParamMeta(key, label, "DATE", null);
    }

    private static ReportDtos.ParamMeta select(String key, String label,
                                               ReportDtos.ParamOption... options) {
        return new ReportDtos.ParamMeta(key, label, "SELECT", List.of(options));
    }

    private static ReportDtos.ParamOption option(String value, String label) {
        return new ReportDtos.ParamOption(value, label);
    }
}
