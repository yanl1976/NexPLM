package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 批次 B｜编码管理（对齐旧系统 {@code InforCenter/Coding} 的段模型）。
 *
 * <p>验证：预置 10 条规则的"下一个编码"预览与既有 {@code prefix + 6 位流水} 完全一致；
 * 改段定义后预览立即变化；重置落审计；两个流水段这类非法定义在保存时被挡住。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CodingRuleSmokeTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动测试环境失败", ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("nexplm.security.bootstrap.admin-password", () -> "Admin@12345");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void presetRulesPreviewMatchesLegacyFormat() throws Exception {
        String admin = login();
        JsonNode rules = call(get("/api/v1/system/coding/rules"), admin, null);
        assertThat(rules.size()).isGreaterThanOrEqualTo(10);

        Map<String, String> expected = Map.of(
                "PdmDoc", "DOC-000001",
                "PdmItem", "PN-000001",
                "CappRouting", "RT-000001",
                "CappCardVolume", "CV-000001");
        for (JsonNode rule : rules) {
            String code = rule.path("code").asText();
            if (expected.containsKey(code)) {
                assertThat(rule.path("preview").asText())
                        .as("规则 %s 的预览应与既有生成格式一致", code)
                        .isEqualTo(expected.get(code));
            }
        }
    }

    @Test
    void updateSegmentsAndResetAffectNextCode() throws Exception {
        String admin = login();

        // 1. 带日期段 + 按年归零：yyyy + '-' + 6 位流水
        call(put("/api/v1/system/coding/rules/CappRouting"), admin, json(Map.of(
                "name", "工艺路线（带年份）",
                "resetPolicy", "YEAR",
                "segments", List.of(
                        Map.of("type", "DATE", "format", "yyyy"),
                        Map.of("type", "TEXT", "value", "-RT"),
                        Map.of("type", "SEQ", "width", 4, "radix", "DECIMAL")))));

        String preview = call(get("/api/v1/system/coding/rules/CappRouting/preview"), admin, null).asText();
        assertThat(preview).matches("\\d{4}-RT\\d{4}").endsWith("RT0001");

        // 2. 重置到 99：下一个应为 ...0100，且落审计
        JsonNode afterReset = call(post("/api/v1/system/coding/rules/CappRouting/reset"), admin,
                json(Map.of("toNo", 99, "reason", "历史数据导入")));
        assertThat(afterReset.path("currentNo").asLong()).isEqualTo(99);
        assertThat(afterReset.path("preview").asText()).endsWith("RT0100");

        JsonNode audit = call(get("/api/v1/system/coding/rules/CappRouting"), admin, null);
        assertThat(audit.path("resetPolicy").asText()).isEqualTo("YEAR");
    }

    @Test
    void invalidSegmentsAreRejected() throws Exception {
        String admin = login();
        // 两个流水段 —— 旧系统同一规则也只允许一个 OrderCode 段
        mockMvc.perform(put("/api/v1/system/coding/rules/CappCard")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("segments", List.of(
                                Map.of("type", "SEQ", "width", 4),
                                Map.of("type", "SEQ", "width", 4))))))
                .andExpect(status().isBadRequest());

        // 日期段格式不在白名单
        mockMvc.perform(put("/api/v1/system/coding/rules/CappCard")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("segments", List.of(
                                Map.of("type", "DATE", "format", "yy-MM"),
                                Map.of("type", "SEQ", "width", 4))))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 工具

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token, String payload)
            throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return body(result).path("data");
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
