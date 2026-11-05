package com.nexplm.platform.security.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RuleContext context(Map<String, Object> attrs) {
        return new RuleContext("1001", "1", attrs);
    }

    private ConditionNode parse(String json) throws Exception {
        return ConditionNode.from(objectMapper.readTree(json));
    }

    @Test
    void nestedAndOrWorks() throws Exception {
        ConditionNode node = parse("""
                {"op":"AND","children":[
                  {"op":"EQ","field":"ownerId","value":"${currentUserId}"},
                  {"op":"OR","children":[
                     {"op":"EQ","field":"lifecycleState","value":"Released"},
                     {"op":"GT","field":"ext_json.priority","value":3}
                  ]}
                ]}
                """);
        assertThat(evaluator.evaluate(node, context(Map.of(
                "ownerId", "1001", "lifecycleState", "Draft", "ext_json", Map.of("priority", 5)))))
                .isTrue();
        assertThat(evaluator.evaluate(node, context(Map.of(
                "ownerId", "1002", "lifecycleState", "Draft", "ext_json", Map.of("priority", 5)))))
                .isFalse();
        assertThat(evaluator.evaluate(node, context(Map.of(
                "ownerId", "1001", "lifecycleState", "Draft", "ext_json", Map.of("priority", 1)))))
                .isFalse();
    }

    @Test
    void comparisonAndMembershipWork() throws Exception {
        RuleContext context = context(Map.of("securityLevel", 3, "code", "DOC-2026-001", "tags", List.of("a", "b")));
        assertThat(evaluator.evaluate(parse("{\"op\":\"GE\",\"field\":\"securityLevel\",\"value\":3}"), context)).isTrue();
        assertThat(evaluator.evaluate(parse("{\"op\":\"LT\",\"field\":\"securityLevel\",\"value\":3}"), context)).isFalse();
        assertThat(evaluator.evaluate(parse("{\"op\":\"STARTS_WITH\",\"field\":\"code\",\"value\":\"DOC-2026\"}"), context)).isTrue();
        assertThat(evaluator.evaluate(parse("{\"op\":\"IN\",\"field\":\"lifecycleState\",\"value\":[\"Draft\",\"Released\"]}"),
                context(Map.of("lifecycleState", "Released")))).isTrue();
        assertThat(evaluator.evaluate(parse("{\"op\":\"CONTAINS\",\"field\":\"tags\",\"value\":\"b\"}"), context)).isTrue();
    }

    @Test
    void nullCheckAndNotWork() throws Exception {
        assertThat(evaluator.evaluate(parse("{\"op\":\"IS_NULL\",\"field\":\"email\"}"), context(Map.of()))).isTrue();
        assertThat(evaluator.evaluate(parse("{\"op\":\"NOT\",\"children\":[{\"op\":\"EQ\",\"field\":\"state\",\"value\":\"Obsolete\"}]}"),
                context(Map.of("state", "Draft")))).isTrue();
    }

    @Test
    void invalidOperatorsAreRejected() {
        assertThatThrownBy(() -> parse("{\"op\":\"DROP_TABLE\",\"field\":\"code\",\"value\":\"x\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的条件运算符");
        assertThatThrownBy(() -> parse("{\"op\":\"AND\",\"children\":[]}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少需要一个子条件");
        assertThatThrownBy(() -> parse("{\"op\":\"EQ\",\"field\":\"\" ,\"value\":1}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
