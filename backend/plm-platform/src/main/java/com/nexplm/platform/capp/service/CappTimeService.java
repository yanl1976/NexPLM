package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.capp.api.CappDtos;
import com.nexplm.platform.capp.api.CappTimeDtos;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 工时定额（P6-6）：参数化规则 + 只读试算。
 *
 * <p>核心取舍：<b>不落计算快照</b>。定额永远是「规则 × 当前工序工时」实时算出来的，
 * 所以规则一改、路线一改，重新查询即得新值（DoD「公式改动可重算」），也不会出现
 * 「路线改了但定额表还是旧值」的双份数据漂移。</p>
 *
 * <p>计算口径：</p>
 * <pre>
 *   工序准备工时 = setup_base_minutes + 工序准备工时 × setup_factor   （按批一次）
 *   工序单件工时 = 工序单件工时 × run_factor
 *   单件工时合计 = Σ 工序单件工时
 *   批量工时     = Σ 工序准备工时 + 单件工时合计 × 批量
 * </pre>
 */
@Service
public class CappTimeService {

    /** 未匹配到规则时的兜底：不做任何调整（系数 1、基准 0），并在结果里标 unmatchedSteps。 */
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final JdbcTemplate jdbcTemplate;
    private final CodeRuleService codeRuleService;
    private final CappRoutingService routingService;

    public CappTimeService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService, CappRoutingService routingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
        this.routingService = routingService;
    }

    // ------------------------------------------------------------------ 规则 CRUD

    public List<CappTimeDtos.TimeRuleRow> listRules(Long companyId, String keyword, Boolean enabled) {
        StringBuilder where = new StringBuilder(" WHERE company_id = ? AND deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (enabled != null) {
            where.append(" AND enabled = ?");
            args.add(enabled);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ? OR op_type ILIKE ? OR work_center ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return jdbcTemplate.query("""
                        SELECT id, code, name, op_type, work_center, setup_base_minutes, setup_factor,
                               run_factor, batch_qty, priority, enabled, remark, created_at, updated_at
                          FROM capp_time_rule
                        """ + where + " ORDER BY priority, code",
                (rs, rowNum) -> new CappTimeDtos.TimeRuleRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("op_type"), rs.getString("work_center"),
                        rs.getBigDecimal("setup_base_minutes"), rs.getBigDecimal("setup_factor"),
                        rs.getBigDecimal("run_factor"), rs.getInt("batch_qty"), rs.getInt("priority"),
                        rs.getBoolean("enabled"), rs.getString("remark"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                args.toArray());
    }

    @Transactional
    public CappTimeDtos.TimeRuleRow createRule(CappTimeDtos.TimeRuleSaveReq req, Long companyId,
                                               Long userId) {
        validate(req);
        String code = nextCode("CappTimeRule", "TR");
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                        INSERT INTO capp_time_rule (id, company_id, code, name, op_type, work_center,
                                                    setup_base_minutes, setup_factor, run_factor,
                                                    batch_qty, priority, enabled, remark,
                                                    created_by, updated_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, companyId, code, req.name().trim(), upperOrNull(req.opType()),
                blankToNull(req.workCenter()), nullToZero(req.setupBaseMinutes()),
                factor(req.setupFactor()), factor(req.runFactor()),
                req.batchQty() == null || req.batchQty() <= 0 ? 1 : req.batchQty(),
                req.priority() == null ? 100 : req.priority(),
                req.enabled() == null || req.enabled(), blankToNull(req.remark()), userId, userId);
        return rule(id, companyId);
    }

    @Transactional
    public CappTimeDtos.TimeRuleRow updateRule(Long id, CappTimeDtos.TimeRuleSaveReq req, Long companyId,
                                               Long userId) {
        rule(id, companyId);
        validate(req);
        jdbcTemplate.update("""
                        UPDATE capp_time_rule
                           SET name = ?, op_type = ?, work_center = ?, setup_base_minutes = ?,
                               setup_factor = ?, run_factor = ?, batch_qty = ?, priority = ?,
                               enabled = ?, remark = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                req.name().trim(), upperOrNull(req.opType()), blankToNull(req.workCenter()),
                nullToZero(req.setupBaseMinutes()), factor(req.setupFactor()), factor(req.runFactor()),
                req.batchQty() == null || req.batchQty() <= 0 ? 1 : req.batchQty(),
                req.priority() == null ? 100 : req.priority(),
                req.enabled() == null || req.enabled(), blankToNull(req.remark()), userId, id, companyId);
        return rule(id, companyId);
    }

    @Transactional
    public void deleteRule(Long id, Long companyId) {
        rule(id, companyId);
        jdbcTemplate.update("""
                UPDATE capp_time_rule SET deleted = true, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    // ------------------------------------------------------------------ 试算

    /**
     * 按工艺路线试算定额。
     *
     * <p>只读：不写任何表。{@code batchQty} 为空或 <= 0 时按 1 计算。</p>
     */
    public CappTimeDtos.TimeCalcResult calc(Long routingId, Integer batchQty, Long companyId) {
        CappDtos.RoutingDetail routing = routingService.detail(routingId, companyId);
        int qty = batchQty == null || batchQty <= 0 ? 1 : batchQty;
        List<CappTimeDtos.TimeRuleRow> rules = listRules(companyId, null, true);

        List<CappTimeDtos.TimeStepRow> steps = new ArrayList<>();
        BigDecimal totalSetup = BigDecimal.ZERO;
        BigDecimal totalRun = BigDecimal.ZERO;
        int matched = 0;
        for (CappDtos.StepRow step : routing.steps()) {
            CappTimeDtos.TimeRuleRow rule = match(rules, step.opType(), step.workCenter());
            BigDecimal rawSetup = nullToZero(step.setupMinutes());
            BigDecimal rawRun = nullToZero(step.runMinutes());
            BigDecimal setupEffective = rule == null
                    ? rawSetup
                    : nullToZero(rule.setupBaseMinutes())
                            .add(rawSetup.multiply(rule.setupFactor()));
            BigDecimal runEffective = rule == null ? rawRun : rawRun.multiply(rule.runFactor());
            setupEffective = scale(setupEffective);
            runEffective = scale(runEffective);
            totalSetup = totalSetup.add(setupEffective);
            totalRun = totalRun.add(runEffective);
            if (rule != null) {
                matched++;
            }
            steps.add(new CappTimeDtos.TimeStepRow(step.seqNo(), step.name(), step.opType(),
                    step.workCenter(), scale(rawSetup), scale(rawRun), setupEffective, runEffective,
                    rule == null ? null : rule.code(), rule == null ? null : rule.name()));
        }
        BigDecimal singlePiece = scale(totalRun);
        BigDecimal batchMinutes = scale(totalSetup.add(singlePiece.multiply(BigDecimal.valueOf(qty))));
        return new CappTimeDtos.TimeCalcResult(routing.id(), routing.code(), routing.objectCode(),
                routing.objectName(), qty, steps, scale(totalSetup), singlePiece, batchMinutes,
                matched, steps.size() - matched);
    }

    /**
     * 规则匹配：工作中心精确 > 工序类型精确 > 通配，同档按 priority 升序、其次按 id 稳定排序。
     *
     * <p>打分为「工作中心精确 +2、工序类型精确 +1」，因此 work_center+op_type 都给全的规则
     * 得分最高；两者都空的规则得 0 分，只在没有更具体的规则时生效。</p>
     */
    private static CappTimeDtos.TimeRuleRow match(List<CappTimeDtos.TimeRuleRow> rules, String opType,
                                                  String workCenter) {
        String normalizedOp = opType == null ? null : opType.trim().toUpperCase();
        String normalizedCenter = workCenter == null || workCenter.isBlank() ? null : workCenter.trim();
        return rules.stream()
                .filter(rule -> rule.opType() == null || rule.opType().equals(normalizedOp))
                .filter(rule -> rule.workCenter() == null
                        || rule.workCenter().equals(normalizedCenter))
                .max(Comparator
                        .comparingInt((CappTimeDtos.TimeRuleRow rule) -> score(rule, normalizedOp, normalizedCenter))
                        // 同分时 priority 越小越优先 → 用反转后的负值参与 max
                        .thenComparingInt(rule -> -rule.priority()))
                .orElse(null);
    }

    private static int score(CappTimeDtos.TimeRuleRow rule, String opType, String workCenter) {
        int score = 0;
        if (rule.opType() != null && rule.opType().equals(opType)) {
            score += 1;
        }
        if (rule.workCenter() != null && rule.workCenter().equals(workCenter)) {
            score += 2;
        }
        return score;
    }

    // ------------------------------------------------------------------ 内部

    private CappTimeDtos.TimeRuleRow rule(Long id, Long companyId) {
        List<CappTimeDtos.TimeRuleRow> rows = jdbcTemplate.query("""
                        SELECT id, code, name, op_type, work_center, setup_base_minutes, setup_factor,
                               run_factor, batch_qty, priority, enabled, remark, created_at, updated_at
                          FROM capp_time_rule
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """,
                (rs, rowNum) -> new CappTimeDtos.TimeRuleRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("op_type"), rs.getString("work_center"),
                        rs.getBigDecimal("setup_base_minutes"), rs.getBigDecimal("setup_factor"),
                        rs.getBigDecimal("run_factor"), rs.getInt("batch_qty"), rs.getInt("priority"),
                        rs.getBoolean("enabled"), rs.getString("remark"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.CAPP_TIME_RULE_NOT_FOUND, "工时定额规则不存在：" + id);
        }
        return rows.get(0);
    }

    private static void validate(CappTimeDtos.TimeRuleSaveReq req) {
        if (nullToZero(req.setupBaseMinutes()).signum() < 0) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "准备工时基准不能为负数");
        }
        // 显式传 0/负数必须拒绝：不能先兜底成 1 再校验，否则非法输入被静默吞掉
        if (req.setupFactor() != null && req.setupFactor().signum() <= 0) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "准备工时系数必须大于 0");
        }
        if (req.runFactor() != null && req.runFactor().signum() <= 0) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "单件工时系数必须大于 0");
        }
        if (req.batchQty() != null && req.batchQty() <= 0) {
            throw BizException.of(ErrorCode.CAPP_CARD_INVALID, "批量必须大于 0");
        }
    }

    private static BigDecimal factor(BigDecimal value) {
        return value == null || value.signum() <= 0 ? ONE : value;
    }

    private static BigDecimal scale(BigDecimal value) {
        return nullToZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String upperOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 统一由编码规则引擎取号（批次 B2）：格式来自 code_rule.segments_json。 */
    private String nextCode(String ruleCode, String prefix) {
        return codeRuleService.next(ruleCode);
    }
}
