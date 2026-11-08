package com.nexplm.platform.supplier.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.supplier.api.SupRatingDtos;
import org.springframework.dao.DuplicateKeyException;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 供应商评级（P8-5-1）：维度可配方案 + 评级快照 + 等级回写。
 *
 * <p>刻意的取舍：</p>
 * <ul>
 *   <li><b>维度不写死</b>：不同类别供应商（外协/物流/服务）考核维度不同，写死字段就只能改代码；
 *       方案把"评什么、各占多少分"变成数据，权重合计强制 100，避免总分口径随方案漂移；</li>
 *   <li><b>评级存维度快照</b>：{@code sup_rate_item} 冗余 code/name/weight/max_score，
 *       方案事后改权重或删维度都不影响历史评级的分数与可解释性；</li>
 *   <li><b>总分 = Σ(得分/满分 × 权重)</b>：满分可配（有的维度是 5 分制），用比例折算才可比；</li>
 *   <li><b>确认才回写供应商</b>：草稿阶段不动主档；确认时写 grade/rating_score/rated_at，
 *       并只在"低于等于暂停档"时把状态推成 SUSPENDED，<b>不自动恢复</b>——解除暂停要走准入/人工判断。</li>
 * </ul>
 */
@Service
public class SupplierRatingService {

    private static final Set<String> CATEGORIES =
            Set.of("MATERIAL", "OUTSOURCE", "SERVICE", "LOGISTICS");
    private static final Set<String> SCHEME_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** 周期口径：2026 / 2026-Q4 / 2026-11。 */
    private static final Pattern PERIOD = Pattern.compile("^\\d{4}(-(Q[1-4]|0[1-9]|1[0-2]))?$");

    private static final String SCHEME_SELECT = """
            SELECT s.*,
                   (SELECT COUNT(1) FROM sup_rate_dim d
                     WHERE d.scheme_id = s.id AND d.deleted = false AND d.enabled = true) AS dim_count,
                   (SELECT COALESCE(SUM(d.weight), 0) FROM sup_rate_dim d
                     WHERE d.scheme_id = s.id AND d.deleted = false AND d.enabled = true) AS weight_sum
              FROM sup_rate_scheme s
            """;

    private static final String RATING_SELECT = """
            SELECT r.*, s.code AS supplier_code, s.name AS supplier_name
              FROM sup_rate r
              JOIN sup_supplier s ON s.id = r.supplier_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CodeRuleService codeRuleService;

    public SupplierRatingService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                                 CodeRuleService codeRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.codeRuleService = codeRuleService;
    }

    // ---------------------------------------------------------------- 评级方案

    public List<SupRatingDtos.SchemeRow> schemes(Long companyId, String category, String status) {
        StringBuilder where = new StringBuilder(" WHERE s.company_id = ? AND s.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (category != null && !category.isBlank()) {
            where.append(" AND (s.category = ? OR s.category IS NULL)");
            args.add(normalizeCategory(category));
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND s.status = ?");
            args.add(normalize(status, SCHEME_STATUSES, "方案状态"));
        }
        return jdbcTemplate.query(SCHEME_SELECT + where + " ORDER BY s.code",
                (rs, rowNum) -> new SupRatingDtos.SchemeRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("category"),
                        rs.getString("suspend_grade"), rs.getString("status"),
                        rs.getInt("dim_count"), rs.getBigDecimal("weight_sum"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                args.toArray());
    }

    public SupRatingDtos.SchemeDetail schemeDetail(Long id, Long companyId) {
        List<SupRatingDtos.SchemeRow> rows = jdbcTemplate.query(
                SCHEME_SELECT + " WHERE s.id = ? AND s.company_id = ? AND s.deleted = false",
                (rs, rowNum) -> new SupRatingDtos.SchemeRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("category"),
                        rs.getString("suspend_grade"), rs.getString("status"),
                        rs.getInt("dim_count"), rs.getBigDecimal("weight_sum"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_RATE_NOT_FOUND, "评级方案不存在：" + id);
        }
        SupRatingDtos.SchemeRow row = rows.get(0);
        Map<String, Object> raw = jdbcTemplate.queryForMap("""
                SELECT grade_rule, remark FROM sup_rate_scheme WHERE id = ?
                """, id);
        Integer ratingCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sup_rate
                 WHERE scheme_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        return new SupRatingDtos.SchemeDetail(row.id(), row.code(), row.name(), row.category(),
                readGradeRule(str(raw.get("grade_rule"))), row.suspendGrade(), row.status(),
                str(raw.get("remark")), ratingCount == null ? 0 : ratingCount,
                dims(id, companyId, false));
    }

    @Transactional
    public SupRatingDtos.SchemeDetail createScheme(SupRatingDtos.SchemeSaveReq req, Long companyId,
                                                   Long userId) {
        List<SupRatingDtos.GradeBand> bands = normalizeGradeRule(req.gradeRule());
        List<SupRatingDtos.DimSaveReq> dims = normalizeDims(req.dims());
        String suspendGrade = normalizeSuspendGrade(req.suspendGrade(), bands);
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO sup_rate_scheme (id, company_id, code, name, category, grade_rule,
                                                     suspend_grade, status, remark, created_by,
                                                     updated_by)
                        VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                        """,
                id, companyId, nextCode(), req.name().trim(), normalizeCategoryOrNull(req.category()),
                writeGradeRule(bands), suspendGrade,
                normalize(req.status() == null ? "ACTIVE" : req.status(), SCHEME_STATUSES,
                        "方案状态"),
                blankToNull(req.remark()), userId, userId);
        insertDims(id, companyId, dims);
        return schemeDetail(id, companyId);
    }

    /** 维度整表替换：方案只影响之后的评级，历史评级有快照。 */
    @Transactional
    public SupRatingDtos.SchemeDetail updateScheme(Long id, SupRatingDtos.SchemeSaveReq req,
                                                   Long companyId, Long userId) {
        requireScheme(id, companyId);
        List<SupRatingDtos.GradeBand> bands = normalizeGradeRule(req.gradeRule());
        List<SupRatingDtos.DimSaveReq> dims = normalizeDims(req.dims());
        String suspendGrade = normalizeSuspendGrade(req.suspendGrade(), bands);
        jdbcTemplate.update("""
                        UPDATE sup_rate_scheme
                           SET name = ?, category = ?, grade_rule = CAST(? AS jsonb),
                               suspend_grade = ?, status = ?, remark = ?,
                               updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), normalizeCategoryOrNull(req.category()), writeGradeRule(bands),
                suspendGrade,
                normalize(req.status() == null ? "ACTIVE" : req.status(), SCHEME_STATUSES,
                        "方案状态"),
                blankToNull(req.remark()), userId, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_rate_dim WHERE scheme_id = ?", id);
        insertDims(id, companyId, dims);
        return schemeDetail(id, companyId);
    }

    @Transactional
    public void deleteScheme(Long id, Long companyId) {
        requireScheme(id, companyId);
        Integer used = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sup_rate
                 WHERE scheme_id = ? AND company_id = ? AND deleted = false
                """, Integer.class, id, companyId);
        if (used != null && used > 0) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID,
                    "该方案已有 " + used + " 条评级记录，不能删除；如需停用请把状态改为 INACTIVE");
        }
        jdbcTemplate.update("""
                UPDATE sup_rate_scheme SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_rate_dim WHERE scheme_id = ?", id);
    }

    // ---------------------------------------------------------------- 评级记录

    public PageResult<SupRatingDtos.RatingRow> ratings(Long companyId, Long supplierId,
                                                       Long schemeId, String period, String status,
                                                       Integer page, Integer size) {
        StringBuilder where = new StringBuilder(" WHERE r.company_id = ? AND r.deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (supplierId != null) {
            where.append(" AND r.supplier_id = ?");
            args.add(supplierId);
        }
        if (schemeId != null) {
            where.append(" AND r.scheme_id = ?");
            args.add(schemeId);
        }
        if (period != null && !period.isBlank()) {
            where.append(" AND r.period = ?");
            args.add(period.trim());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND r.status = ?");
            args.add(normalize(status, Set.of("DRAFT", "CONFIRMED"), "评级状态"));
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sup_rate r" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SupRatingDtos.RatingRow> items = jdbcTemplate.query(
                RATING_SELECT + where + " ORDER BY r.period DESC, r.updated_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> ratingRow(rs), pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public List<SupRatingDtos.RatingRow> ratingsBySupplier(Long companyId, Long supplierId) {
        return jdbcTemplate.query(RATING_SELECT
                        + " WHERE r.company_id = ? AND r.supplier_id = ? AND r.deleted = false"
                        + " ORDER BY r.period DESC, r.updated_at DESC",
                (rs, rowNum) -> ratingRow(rs), companyId, supplierId);
    }

    public SupRatingDtos.RatingDetail ratingDetail(Long id, Long companyId) {
        List<SupRatingDtos.RatingRow> rows = jdbcTemplate.query(
                RATING_SELECT + " WHERE r.id = ? AND r.company_id = ? AND r.deleted = false",
                (rs, rowNum) -> ratingRow(rs), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_RATE_NOT_FOUND, "评级记录不存在：" + id);
        }
        SupRatingDtos.RatingRow row = rows.get(0);
        String remark = jdbcTemplate.queryForObject(
                "SELECT remark FROM sup_rate WHERE id = ?", String.class, id);
        return new SupRatingDtos.RatingDetail(row.id(), row.supplierId(), row.supplierCode(),
                row.supplierName(), row.schemeId(), row.schemeCode(), row.schemeName(),
                row.period(), row.status(), row.totalScore(), row.grade(), remark,
                row.confirmedAt(), items(id));
    }

    @Transactional
    public SupRatingDtos.RatingDetail createRating(SupRatingDtos.RatingSaveReq req, Long companyId,
                                                   Long userId) {
        Map<String, Object> supplier = requireSupplier(req.supplierId(), companyId);
        Map<String, Object> scheme = requireScheme(req.schemeId(), companyId);
        String period = normalizePeriod(req.period());
        if (!"ACTIVE".equals(str(scheme.get("status")))) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID, "评级方案已停用，不能发起评级");
        }
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO sup_rate (id, company_id, supplier_id, scheme_id, scheme_code,
                                                  scheme_name, period, status, rated_by, rated_at,
                                                  remark, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, now(), ?, ?, ?)
                            """,
                    id, companyId, req.supplierId(), req.schemeId(),
                    str(scheme.get("code")), str(scheme.get("name")), period, userId,
                    blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID,
                    "同一供应商在同一方案同一周期已有评级记录，请直接修改或删除草稿");
        }
        writeItems(id, companyId, req, req.schemeId());
        recompute(id, companyId);
        return ratingDetail(id, companyId);
    }

    @Transactional
    public SupRatingDtos.RatingDetail updateRating(Long id, SupRatingDtos.RatingSaveReq req,
                                                   Long companyId, Long userId) {
        Map<String, Object> rating = requireRating(id, companyId);
        if (!"DRAFT".equals(str(rating.get("status")))) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID,
                    "评级已确认，不能修改；如需重评请另开周期");
        }
        Long schemeId = longOf(rating.get("scheme_id"));
        requireScheme(schemeId, companyId);
        try {
            jdbcTemplate.update("""
                    UPDATE sup_rate SET period = ?, remark = ?, updated_by = ?, updated_at = now()
                     WHERE id = ? AND company_id = ?
                    """, normalizePeriod(req.period()), blankToNull(req.remark()), userId, id,
                    companyId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID,
                    "同一供应商在同一方案同一周期已有另一条评级记录");
        }
        jdbcTemplate.update("DELETE FROM sup_rate_item WHERE rating_id = ?", id);
        writeItems(id, companyId, req, schemeId);
        recompute(id, companyId);
        return ratingDetail(id, companyId);
    }

    /** 确认评级：写总分/等级到记录，并把等级与分数回写供应商主档。 */
    @Transactional
    public SupRatingDtos.RatingDetail confirmRating(Long id, Long companyId, Long userId) {
        Map<String, Object> rating = requireRating(id, companyId);
        if (!"DRAFT".equals(str(rating.get("status")))) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID, "评级已确认，无需重复确认");
        }
        Long schemeId = longOf(rating.get("scheme_id"));
        Map<String, Object> scheme = requireScheme(schemeId, companyId);
        jdbcTemplate.update("""
                UPDATE sup_rate
                   SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = now(),
                       updated_by = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, userId, id, companyId);
        SupRatingDtos.RatingDetail detail = ratingDetail(id, companyId);
        Long supplierId = detail.supplierId();
        jdbcTemplate.update("""
                UPDATE sup_supplier
                   SET grade = ?, rating_score = ?, rated_at = now(), updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, detail.grade(), detail.totalScore(), supplierId, companyId);
        List<SupRatingDtos.GradeBand> bands = readGradeRule(str(scheme.get("grade_rule")));
        String suspendGrade = str(scheme.get("suspend_grade"));
        if (shouldSuspend(detail.grade(), suspendGrade, bands)) {
            jdbcTemplate.update("""
                    UPDATE sup_supplier
                       SET status = 'SUSPENDED', updated_at = now()
                     WHERE id = ? AND company_id = ? AND status <> 'DISQUALIFIED'
                    """, supplierId, companyId);
        }
        return detail;
    }

    @Transactional
    public void deleteRating(Long id, Long companyId) {
        Map<String, Object> rating = requireRating(id, companyId);
        if (!"DRAFT".equals(str(rating.get("status")))) {
            throw BizException.of(ErrorCode.SUP_RATE_STATE_INVALID, "已确认的评级不能删除（审计留痕）");
        }
        jdbcTemplate.update("""
                UPDATE sup_rate SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
        jdbcTemplate.update("DELETE FROM sup_rate_item WHERE rating_id = ?", id);
    }

    // ---------------------------------------------------------------- 内部：方案

    private List<SupRatingDtos.DimRow> dims(Long schemeId, Long companyId, boolean enabledOnly) {
        return jdbcTemplate.query("""
                        SELECT * FROM sup_rate_dim
                         WHERE scheme_id = ? AND company_id = ? AND deleted = false
                           AND (? = false OR enabled = true)
                         ORDER BY sort_no, id
                        """,
                (rs, rowNum) -> new SupRatingDtos.DimRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getBigDecimal("weight"),
                        rs.getBigDecimal("max_score"), rs.getInt("sort_no"), rs.getBoolean("enabled")),
                schemeId, companyId, enabledOnly);
    }

    private void insertDims(Long schemeId, Long companyId, List<SupRatingDtos.DimSaveReq> dims) {
        int index = 0;
        for (SupRatingDtos.DimSaveReq dim : dims) {
            index++;
            jdbcTemplate.update("""
                            INSERT INTO sup_rate_dim (id, company_id, scheme_id, code, name, weight,
                                                      max_score, sort_no, enabled)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, schemeId,
                    dim.code() == null || dim.code().isBlank()
                            ? "D" + index : dim.code().trim().toUpperCase(),
                    dim.name().trim(), dim.weight(),
                    dim.maxScore() == null ? HUNDRED : dim.maxScore(), index,
                    dim.enabled() == null || dim.enabled());
        }
    }

    private List<SupRatingDtos.GradeBand> normalizeGradeRule(
            List<SupRatingDtos.GradeBand> bands) {
        List<SupRatingDtos.GradeBand> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (bands == null || bands.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID, "等级规则不能为空");
        }
        for (SupRatingDtos.GradeBand band : bands) {
            String grade = band.grade() == null ? "" : band.grade().trim().toUpperCase();
            if (grade.isEmpty()) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID, "等级档位不能为空");
            }
            if (!seen.add(grade)) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID, "等级档位重复：" + grade);
            }
            BigDecimal min = band.min();
            if (min == null || min.compareTo(BigDecimal.ZERO) < 0
                    || min.compareTo(HUNDRED) > 0) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                        "等级 " + grade + " 的最低分必须在 0~100 之间");
            }
            result.add(new SupRatingDtos.GradeBand(grade, min));
        }
        result.sort(Comparator.comparing(SupRatingDtos.GradeBand::min).reversed());
        return result;
    }

    private List<SupRatingDtos.DimSaveReq> normalizeDims(List<SupRatingDtos.DimSaveReq> dims) {
        if (dims == null || dims.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID, "评级维度不能为空");
        }
        BigDecimal sum = BigDecimal.ZERO;
        Set<String> codes = new HashSet<>();
        List<SupRatingDtos.DimSaveReq> result = new ArrayList<>();
        int index = 0;
        for (SupRatingDtos.DimSaveReq dim : dims) {
            index++;
            BigDecimal weight = dim.weight();
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0
                    || weight.compareTo(HUNDRED) > 0) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID, "维度权重必须在 0~100 之间");
            }
            BigDecimal maxScore = dim.maxScore() == null ? HUNDRED : dim.maxScore();
            if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID, "维度满分必须大于 0");
            }
            String code = dim.code() == null || dim.code().isBlank()
                    ? "D" + index : dim.code().trim().toUpperCase();
            if (!codes.add(code)) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID, "维度编码重复：" + code);
            }
            sum = sum.add(weight);
            result.add(new SupRatingDtos.DimSaveReq(code, dim.name(), weight, maxScore,
                    dim.enabled() == null || dim.enabled()));
        }
        if (sum.compareTo(HUNDRED) != 0) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                    "维度权重合计必须为 100，当前为 " + sum.stripTrailingZeros().toPlainString());
        }
        return result;
    }

    private String normalizeSuspendGrade(String suspendGrade,
                                         List<SupRatingDtos.GradeBand> bands) {
        if (suspendGrade == null || suspendGrade.isBlank()) {
            return null;
        }
        String grade = suspendGrade.trim().toUpperCase();
        boolean exists = bands.stream().anyMatch(band -> band.grade().equals(grade));
        if (!exists) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                    "自动暂停等级必须在等级规则内：" + suspendGrade);
        }
        return grade;
    }

    private String normalizeCategoryOrNull(String category) {
        return category == null || category.isBlank() ? null : normalizeCategory(category);
    }

    private static String normalizeCategory(String category) {
        return normalize(category, CATEGORIES, "供应商类别");
    }

    private String writeGradeRule(List<SupRatingDtos.GradeBand> bands) {
        try {
            return objectMapper.writeValueAsString(bands);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID, "等级规则序列化失败");
        }
    }

    private List<SupRatingDtos.GradeBand> readGradeRule(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SupRatingDtos.GradeBand>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Map<String, Object> requireScheme(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM sup_rate_scheme
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_RATE_NOT_FOUND, "评级方案不存在：" + id);
        }
        return rows.get(0);
    }

    // ---------------------------------------------------------------- 内部：评级

    /** 写维度打分（快照方案当前的维度定义），并校验覆盖完整、分数在区间内。 */
    private void writeItems(Long ratingId, Long companyId, SupRatingDtos.RatingSaveReq req,
                            Long schemeId) {
        List<SupRatingDtos.DimRow> dims = dims(schemeId, companyId, true);
        Map<Long, SupRatingDtos.RatingItemReq> given = new LinkedHashMap<>();
        for (SupRatingDtos.RatingItemReq item : req.items()) {
            if (given.put(item.dimId(), item) != null) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID, "维度重复打分：" + item.dimId());
            }
        }
        Set<Long> dimIds = new HashSet<>();
        for (SupRatingDtos.DimRow dim : dims) {
            dimIds.add(dim.id());
            if (!given.containsKey(dim.id())) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                        "维度「" + dim.name() + "」未打分");
            }
        }
        for (Long dimId : given.keySet()) {
            if (!dimIds.contains(dimId)) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                        "维度不属于该评级方案：" + dimId);
            }
        }
        for (SupRatingDtos.DimRow dim : dims) {
            SupRatingDtos.RatingItemReq item = given.get(dim.id());
            BigDecimal score = item.score();
            if (score.compareTo(BigDecimal.ZERO) < 0
                    || score.compareTo(dim.maxScore()) > 0) {
                throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                        "维度「" + dim.name() + "」得分必须在 0~"
                                + dim.maxScore().stripTrailingZeros().toPlainString() + " 之间");
            }
            jdbcTemplate.update("""
                            INSERT INTO sup_rate_item (id, company_id, rating_id, dim_id, dim_code,
                                                       dim_name, weight, max_score, score, comment,
                                                       sort_no)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, ratingId, dim.id(), dim.code(), dim.name(),
                    dim.weight(), dim.maxScore(), score, blankToNull(item.comment()),
                    dim.sortNo());
        }
    }

    /** 重算总分与等级（草稿阶段每次保存都重算，确认时值即为最终值）。 */
    private void recompute(Long ratingId, Long companyId) {
        List<SupRatingDtos.ItemRow> items = items(ratingId);
        BigDecimal total = BigDecimal.ZERO;
        for (SupRatingDtos.ItemRow item : items) {
            if (item.maxScore().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            total = total.add(item.score()
                    .multiply(item.weight())
                    .divide(item.maxScore(), 6, RoundingMode.HALF_UP));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        Long schemeId = jdbcTemplate.queryForObject(
                "SELECT scheme_id FROM sup_rate WHERE id = ?", Long.class, ratingId);
        Map<String, Object> scheme = requireScheme(schemeId, companyId);
        String grade = gradeOf(total, readGradeRule(str(scheme.get("grade_rule"))));
        jdbcTemplate.update("""
                UPDATE sup_rate SET total_score = ?, grade = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, total, grade, ratingId, companyId);
    }

    private static String gradeOf(BigDecimal total,
                                  List<SupRatingDtos.GradeBand> bands) {
        for (SupRatingDtos.GradeBand band : bands) {
            if (total.compareTo(band.min()) >= 0) {
                return band.grade();
            }
        }
        return bands.isEmpty() ? null : bands.get(bands.size() - 1).grade();
    }

    /** 等级"低于等于暂停档"即暂停（数组从高到低，index 越大越差）。 */
    private static boolean shouldSuspend(String grade, String suspendGrade,
                                         List<SupRatingDtos.GradeBand> bands) {
        if (grade == null || suspendGrade == null || suspendGrade.isBlank()) {
            return false;
        }
        int gradeIndex = indexOfGrade(bands, grade);
        int suspendIndex = indexOfGrade(bands, suspendGrade);
        return gradeIndex >= 0 && suspendIndex >= 0 && gradeIndex >= suspendIndex;
    }

    private static int indexOfGrade(List<SupRatingDtos.GradeBand> bands, String grade) {
        for (int i = 0; i < bands.size(); i++) {
            if (bands.get(i).grade().equalsIgnoreCase(grade)) {
                return i;
            }
        }
        return -1;
    }

    private List<SupRatingDtos.ItemRow> items(Long ratingId) {
        return jdbcTemplate.query("""
                        SELECT * FROM sup_rate_item WHERE rating_id = ? ORDER BY sort_no, id
                        """,
                (rs, rowNum) -> new SupRatingDtos.ItemRow(rs.getLong("id"),
                        rs.getString("dim_code"), rs.getString("dim_name"),
                        rs.getBigDecimal("weight"), rs.getBigDecimal("max_score"),
                        rs.getBigDecimal("score"), rs.getString("comment")),
                ratingId);
    }

    private SupRatingDtos.RatingRow ratingRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new SupRatingDtos.RatingRow(rs.getLong("id"), rs.getLong("supplier_id"),
                rs.getString("supplier_code"), rs.getString("supplier_name"),
                rs.getLong("scheme_id"), rs.getString("scheme_code"), rs.getString("scheme_name"),
                rs.getString("period"), rs.getString("status"), rs.getBigDecimal("total_score"),
                rs.getString("grade"), rs.getObject("rated_at", OffsetDateTime.class),
                rs.getObject("confirmed_at", OffsetDateTime.class));
    }

    private Map<String, Object> requireRating(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM sup_rate WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_RATE_NOT_FOUND, "评级记录不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireSupplier(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, code, name, status FROM sup_supplier
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SUP_NOT_FOUND, "供应商不存在：" + id);
        }
        return rows.get(0);
    }

    // ---------------------------------------------------------------- 内部：通用

    /** 取号统一走 code_rule（台账 B19）。 */
    private String nextCode() {
        return codeRuleService.next("SupRateScheme");
    }

    private static String normalizePeriod(String period) {
        String value = period == null ? "" : period.trim();
        if (!PERIOD.matcher(value).matches()) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                    "评级周期格式应为 2026 / 2026-Q4 / 2026-11，收到：" + period);
        }
        return value;
    }

    private static String normalize(String value, Set<String> allowed, String label) {
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw BizException.of(ErrorCode.SUP_RATE_INVALID,
                    "未知" + label + "：" + value + "（可选 " + String.join("/", allowed) + "）");
        }
        return normalized;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
