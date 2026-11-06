package com.nexplm.platform.change.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 变更管理阶段状态机（P5 任务 7）：ECR / ECN / Problem 三套状态机取自元数据
 * {@code meta_lifecycle}（{@code type='CHG'}，定义见 V20 迁移），不再写死在 Java 常量里。
 *
 * <p>迁移语义：{@code (from, action) -> to}。{@code from == to} 的自迁移表示
 * 「该状态允许此动作但不改状态」（例如已通知状态下再次下发通知）。</p>
 *
 * <p>两条派生规则（避免在元数据里再维护一遍冗余标记，也让既有生命周期设计器
 * 保存时不会丢掉它们）：</p>
 * <ul>
 *   <li><b>可编辑主档</b> = 状态等于 {@code initial}（草稿）；</li>
 *   <li><b>可调整关联对象</b> = 状态等于 {@code initial}，或它是 {@code submit}/{@code review}
 *       迁移的终点（即已提交、评审中）。</li>
 * </ul>
 *
 * <p>不做缓存：状态机只在写操作时读一次元数据行（单行主键查询），换来的是设计器改完即时生效，
 * 不需要额外的缓存失效通道。</p>
 */
@Service
public class ChgStateMachine {

    /** 变更申请状态机编码（{@code meta_lifecycle.code}）。 */
    public static final String ECR = "CHG_ECR";
    /** 变更通知状态机编码。 */
    public static final String ECN = "CHG_ECN";
    /** 变更问题状态机编码。 */
    public static final String PROBLEM = "CHG_PROBLEM";

    private record Transition(String from, String to, String action) {
    }

    private record Definition(String initial, Set<String> states, List<Transition> transitions) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public ChgStateMachine(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    /** 初始状态（新建单据落库值）。 */
    public String initial(String code) {
        Definition definition = definition(code);
        if (definition.initial() == null || definition.initial().isBlank()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "状态机未定义初始状态：" + code);
        }
        return definition.initial();
    }

    /** 该状态下是否允许执行 action。 */
    public boolean can(String code, String from, String action) {
        return definition(code).transitions().stream()
                .anyMatch(item -> item.from().equals(from) && item.action().equalsIgnoreCase(action));
    }

    /** 执行 action 后的目标状态（调用前应先用 {@link #can} 判定）。 */
    public String target(String code, String from, String action) {
        return definition(code).transitions().stream()
                .filter(item -> item.from().equals(from) && item.action().equalsIgnoreCase(action))
                .map(Transition::to)
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.CHG_STATE_INVALID,
                        "状态「" + from + "」不允许执行操作：" + action));
    }

    /** 校验迁移合法并返回目标状态；不合法抛 409 并带调用方给的提示语。 */
    public String require(String code, String from, String action, String message) {
        if (!can(code, from, action)) {
            throw BizException.of(ErrorCode.CHG_STATE_INVALID, message + "（当前状态：" + from + "）");
        }
        return target(code, from, action);
    }

    /** 是否允许修改单据主档（= 草稿态）。 */
    public boolean mainEditable(String code, String state) {
        Definition definition = definition(code);
        return definition.initial() != null && definition.initial().equals(state);
    }

    /** 是否允许调整关联对象（草稿 / 已提交 / 评审中）。 */
    public boolean objectEditable(String code, String state) {
        Definition definition = definition(code);
        if (definition.initial() != null && definition.initial().equals(state)) {
            return true;
        }
        return definition.transitions().stream()
                .anyMatch(item -> item.to().equals(state)
                        && ("submit".equalsIgnoreCase(item.action()) || "review".equalsIgnoreCase(item.action())));
    }

    private Definition definition(String code) {
        List<String> rows = jdbcTemplate.query("""
                        SELECT definition_json::text
                          FROM meta_lifecycle
                         WHERE code = ?
                         LIMIT 1
                        """,
                (rs, rowNum) -> rs.getString(1), code);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "变更状态机未定义：" + code);
        }
        return parse(code, rows.get(0));
    }

    private Definition parse(String code, String definitionJson) {
        try {
            JsonNode root = jsonMapper.readTree(definitionJson);
            Set<String> states = new LinkedHashSet<>();
            for (JsonNode state : root.path("states")) {
                String stateCode = state.path("code").asText();
                if (!stateCode.isBlank()) {
                    states.add(stateCode);
                }
            }
            List<Transition> transitions = new ArrayList<>();
            for (JsonNode transition : root.path("transitions")) {
                String action = transition.path("action").asText();
                if (action.isBlank()) {
                    continue;
                }
                transitions.add(new Transition(transition.path("from").asText(),
                        transition.path("to").asText(), action));
            }
            String initial = root.path("initial").asText(states.isEmpty() ? null : states.iterator().next());
            return new Definition(initial, Set.copyOf(states), List.copyOf(transitions));
        } catch (Exception ex) {
            throw new IllegalStateException("变更状态机定义解析失败：" + code, ex);
        }
    }
}
