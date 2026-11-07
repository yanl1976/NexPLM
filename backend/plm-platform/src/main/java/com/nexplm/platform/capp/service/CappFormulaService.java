package com.nexplm.platform.capp.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.coding.service.CodeRuleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 材料定额（公式库），对齐旧系统 {@code Formula}：公式 = 表达式 + 参数定义，另带类型与说明。
 *
 * <p><b>为什么不用 SpEL / JS 引擎</b>：这些表达式由业务人员维护，若直接交给通用脚本引擎，
 * 一句 {@code T(java.lang.Runtime)...} 就是远程代码执行。这里用一个只认
 * 数字、参数名、四则运算、括号和 5 个白名单函数的递归下降解析器（约 100 行），
 * 能力够用且没有逃逸面。</p>
 */
@Service
public class CappFormulaService {

    private static final Set<String> TYPES = Set.of("MATERIAL", "TIME", "OTHER");

    private final JdbcTemplate jdbcTemplate;
    private final CodeRuleService codeRuleService;
    private final ObjectMapper objectMapper;

    public CappFormulaService(JdbcTemplate jdbcTemplate, CodeRuleService codeRuleService,
                              ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.codeRuleService = codeRuleService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------ 查询

    public List<Formula> list(Long companyId, String keyword, String formulaType) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, code, name, formula_type, expression, params, result_unit, explain,
                       enabled, updated_at
                  FROM capp_formula WHERE company_id = ? AND deleted = false
                """);
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (lower(code) LIKE ? OR lower(name) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
        }
        if (formulaType != null && !formulaType.isBlank()) {
            sql.append(" AND formula_type = ?");
            args.add(formulaType.trim().toUpperCase());
        }
        sql.append(" ORDER BY code");
        return jdbcTemplate.query(sql.toString(), (rs, i) -> new Formula(
                rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("formula_type"), rs.getString("expression"),
                parseParams(rs.getString("params")), rs.getString("result_unit"),
                rs.getString("explain"), rs.getBoolean("enabled"), null), args.toArray());
    }

    public Formula get(Long id, Long companyId) {
        List<Formula> rows = jdbcTemplate.query("""
                SELECT id, code, name, formula_type, expression, params, result_unit, explain, enabled
                  FROM capp_formula WHERE id = ? AND company_id = ? AND deleted = false
                """, (rs, i) -> new Formula(rs.getLong("id"), rs.getString("code"), rs.getString("name"),
                rs.getString("formula_type"), rs.getString("expression"),
                parseParams(rs.getString("params")), rs.getString("result_unit"),
                rs.getString("explain"), rs.getBoolean("enabled"), null), id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "公式不存在");
        }
        return rows.get(0);
    }

    // ------------------------------------------------------------ 维护

    @Transactional
    public Formula create(FormulaReq req, Long companyId, Long userId) {
        String expression = requireExpression(req.expression());
        String type = normalizeType(req.formulaType());
        String code = req.code() == null || req.code().isBlank()
                ? codeRuleService.next("CappFormula") : req.code().trim();
        Long dup = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM capp_formula WHERE company_id = ? AND code = ? AND deleted = false",
                Long.class, companyId, code);
        if (dup != null && dup > 0) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "公式编码已存在：" + code);
        }
        List<FormulaParam> params = normalizeParams(req.params());
        long id = IdWorker.getId();
        jdbcTemplate.update("""
                INSERT INTO capp_formula (id, company_id, code, name, formula_type, expression, params,
                       result_unit, explain, enabled, created_by)
                VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
                """, id, companyId, code, req.name(), type, expression, toJson(params),
                req.resultUnit(), req.explain(), req.enabled() == null || req.enabled(), userId);
        return get(id, companyId);
    }

    @Transactional
    public Formula update(Long id, FormulaReq req, Long companyId) {
        Formula old = get(id, companyId);
        String expression = req.expression() == null || req.expression().isBlank()
                ? old.expression() : requireExpression(req.expression());
        jdbcTemplate.update("""
                UPDATE capp_formula SET name = ?, formula_type = ?, expression = ?, params = CAST(? AS jsonb),
                       result_unit = ?, explain = ?, enabled = ?, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, req.name() == null || req.name().isBlank() ? old.name() : req.name(),
                normalizeType(req.formulaType() == null ? old.formulaType() : req.formulaType()),
                expression, toJson(normalizeParams(req.params() == null ? old.params() : req.params())),
                req.resultUnit(), req.explain(),
                req.enabled() == null ? old.enabled() : req.enabled(), id, companyId);
        return get(id, companyId);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        int deleted = jdbcTemplate.update(
                "UPDATE capp_formula SET deleted = true, updated_at = now() WHERE id = ? AND company_id = ?",
                id, companyId);
        if (deleted == 0) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND, "公式不存在");
        }
    }

    // ------------------------------------------------------------ 求值

    /**
     * 试算：参数缺省取参数定义的 {@code defaultValue}，仍缺则报错（不默认成 0，
     * 否则材料定额会算出"看起来合理"的错数）。
     */
    public BigDecimal evaluate(Long id, Long companyId, Map<String, BigDecimal> input) {
        Formula formula = get(id, companyId);
        Map<String, BigDecimal> variables = new HashMap<>();
        for (FormulaParam param : formula.params()) {
            BigDecimal value = input == null ? null : input.get(param.name());
            if (value == null) {
                value = param.defaultValue();
            }
            if (value == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "缺少参数：" + param.name());
            }
            variables.put(param.name(), value);
        }
        if (input != null) {
            input.forEach(variables::putIfAbsent);
        }
        return new Parser(formula.expression(), variables).parse().setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    // ------------------------------------------------------------ 内部工具

    private String requireExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "表达式必填");
        }
        // 保存前先按 0 参跑一遍语法：非法表达式当场挡，别等到业务算材料时才发现
        new Parser(expression, Map.of()).syntaxOnly();
        return expression.trim();
    }

    private static String normalizeType(String type) {
        String value = type == null || type.isBlank() ? "MATERIAL" : type.trim().toUpperCase();
        if (!TYPES.contains(value)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "公式类型只能是 MATERIAL/TIME/OTHER");
        }
        return value;
    }

    private List<FormulaParam> normalizeParams(List<FormulaParam> params) {
        if (params == null) {
            return List.of();
        }
        List<FormulaParam> result = new ArrayList<>();
        for (FormulaParam param : params) {
            if (param.name() == null || param.name().isBlank()) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "参数名不能为空");
            }
            if (!param.name().matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                        "参数名只能用字母/数字/下划线且不能以数字开头：" + param.name());
            }
            result.add(param);
        }
        return result;
    }

    private List<FormulaParam> parseParams(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FormulaParam>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String toJson(List<FormulaParam> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "参数定义序列化失败");
        }
    }

    public record FormulaParam(String name, String label, BigDecimal defaultValue, String unit) {
    }

    public record Formula(Long id, String code, String name, String formulaType, String expression,
                          List<FormulaParam> params, String resultUnit, String explain,
                          Boolean enabled, BigDecimal sample) {
    }

    public record FormulaReq(String code, String name, String formulaType, String expression,
                             List<FormulaParam> params, String resultUnit, String explain,
                             Boolean enabled) {
    }

    /**
     * 表达式解析器：只支持 + - * / ( )、数字、参数名、白名单函数
     * {@code min/max/round/ceil/floor}。未知标识符直接报错，没有反射/方法调用入口。
     */
    private static final class Parser {

        private final String source;
        private final Map<String, BigDecimal> variables;
        private int pos;

        Parser(String source, Map<String, BigDecimal> variables) {
            this.source = source;
            this.variables = variables;
        }

        /** 只校验语法（不求值）：用 0 填充所有变量。 */
        void syntaxOnly() {
            Map<String, BigDecimal> zeros = new HashMap<>();
            for (String name : extractIdentifiers(source)) {
                zeros.put(name, BigDecimal.ZERO);
            }
            new Parser(source, zeros).parse();
        }

        BigDecimal parse() {
            BigDecimal value = expression();
            skipSpaces();
            if (pos < source.length()) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                        "表达式在位置 " + pos + " 处有多余字符：" + source.substring(pos));
            }
            return value;
        }

        private BigDecimal expression() {
            BigDecimal left = term();
            while (true) {
                skipSpaces();
                if (eat('+')) {
                    left = left.add(term());
                } else if (eat('-')) {
                    left = left.subtract(term());
                } else {
                    return left;
                }
            }
        }

        private BigDecimal term() {
            BigDecimal left = factor();
            while (true) {
                skipSpaces();
                if (eat('*')) {
                    left = left.multiply(factor());
                } else if (eat('/')) {
                    BigDecimal divisor = factor();
                    if (divisor.signum() == 0) {
                        throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "表达式除数为 0");
                    }
                    left = left.divide(divisor, 10, RoundingMode.HALF_UP);
                } else {
                    return left;
                }
            }
        }

        private BigDecimal factor() {
            skipSpaces();
            if (eat('-')) {
                return factor().negate();
            }
            if (eat('+')) {
                return factor();
            }
            if (eat('(')) {
                BigDecimal value = expression();
                skipSpaces();
                if (!eat(')')) {
                    throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "表达式缺少右括号");
                }
                return value;
            }
            if (pos < source.length() && Character.isDigit(source.charAt(pos))) {
                return number();
            }
            String name = identifier();
            skipSpaces();
            if (eat('(')) {
                List<BigDecimal> args = new ArrayList<>();
                skipSpaces();
                if (!eat(')')) {
                    do {
                        args.add(expression());
                        skipSpaces();
                    } while (eat(','));
                    if (!eat(')')) {
                        throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "函数参数缺少右括号：" + name);
                    }
                }
                return call(name, args);
            }
            BigDecimal value = variables.get(name);
            if (value == null) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "表达式引用了未知参数：" + name);
            }
            return value;
        }

        private BigDecimal call(String name, List<BigDecimal> args) {
            return switch (name.toLowerCase()) {
                case "min" -> {
                    requireArgs(name, args, 2);
                    yield args.get(0).min(args.get(1));
                }
                case "max" -> {
                    requireArgs(name, args, 2);
                    yield args.get(0).max(args.get(1));
                }
                case "round" -> {
                    requireArgs(name, args, 1);
                    yield args.get(0).setScale(0, RoundingMode.HALF_UP);
                }
                case "ceil" -> {
                    requireArgs(name, args, 1);
                    yield args.get(0).setScale(0, RoundingMode.CEILING);
                }
                case "floor" -> {
                    requireArgs(name, args, 1);
                    yield args.get(0).setScale(0, RoundingMode.FLOOR);
                }
                default -> throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                        "不支持的函数：" + name + "（只允许 min/max/round/ceil/floor）");
            };
        }

        private static void requireArgs(String name, List<BigDecimal> args, int expected) {
            if (args.size() != expected) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                        "函数 " + name + " 需要 " + expected + " 个参数，实际 " + args.size());
            }
        }

        private BigDecimal number() {
            int start = pos;
            while (pos < source.length()
                    && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                pos++;
            }
            try {
                return new BigDecimal(source.substring(start, pos));
            } catch (NumberFormatException ex) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                        "非法数字：" + source.substring(start, pos));
            }
        }

        private String identifier() {
            skipSpaces();
            int start = pos;
            while (pos < source.length()
                    && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                pos++;
            }
            if (start == pos) {
                throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                        "表达式在位置 " + pos + " 处无法解析：" + source.substring(Math.min(pos, source.length())));
            }
            return source.substring(start, pos);
        }

        private boolean eat(char ch) {
            if (pos < source.length() && source.charAt(pos) == ch) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
                pos++;
            }
        }

        private static Set<String> extractIdentifiers(String text) {
            Set<String> names = new java.util.LinkedHashSet<>();
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_]*").matcher(text);
            while (matcher.find()) {
                String token = matcher.group();
                if (!Set.of("min", "max", "round", "ceil", "floor").contains(token.toLowerCase())) {
                    names.add(token);
                }
            }
            return names;
        }
    }
}
