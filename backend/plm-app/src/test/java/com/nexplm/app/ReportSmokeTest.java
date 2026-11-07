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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 复刻批次 D｜报表管理：数据集白名单 + 模板 CRUD + 运行 + 导出。
 *
 * <p>重点覆盖三条边界：① 参数只能取数据集声明的键（拼不进 SQL）；② 内置模板只读；
 * ③ 运行结果列顺序由数据集定义（前端不需要猜列）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportSmokeTest {

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
    void datasetDictionaryTemplateCrudRunAndExport() throws Exception {
        String admin = login();
        long itemId = createItem(admin, "报表零件");
        JsonNode routing = call(post("/api/v1/capp/routings"), admin,
                json(routing("报表工艺路线", itemId)));
        String routingCode = routing.path("code").asText();

        // 1. 数据集字典：5 张内置数据集，列与参数都带定义
        JsonNode datasets = call(get("/api/v1/ana/reports/datasets"), admin, null);
        assertThat(datasets.size()).isEqualTo(5);
        JsonNode cappDataset = null;
        for (JsonNode node : datasets) {
            if ("capp.routing".equals(node.path("key").asText())) {
                cappDataset = node;
            }
        }
        assertThat(cappDataset).isNotNull();
        assertThat(cappDataset.path("columns").get(0).path("key").asText()).isEqualTo("code");
        assertThat(cappDataset.path("params").size()).isEqualTo(3);

        // 2. 内置模板：5 张，均标记 builtIn
        JsonNode list = call(get("/api/v1/ana/reports"), admin, null);
        assertThat(list.path("total").asLong()).isEqualTo(5);
        long routingTemplateId = -1;
        for (JsonNode row : list.path("items")) {
            assertThat(row.path("builtIn").asBoolean()).isTrue();
            if ("RPT-CAPP-ROUTING".equals(row.path("code").asText())) {
                routingTemplateId = row.path("id").asLong();
                assertThat(row.path("datasetLabel").asText()).isEqualTo("工艺路线汇总");
                assertThat(row.path("params").size()).isEqualTo(3);
            }
        }
        assertThat(routingTemplateId).isPositive();

        // 3. 运行：按关键字过滤到刚建的那条路线，列顺序来自数据集定义
        JsonNode run = call(post("/api/v1/ana/reports/{id}/run", routingTemplateId), admin,
                json(Map.of("params", Map.of("keyword", routingCode))));
        assertThat(run.path("total").asLong()).isEqualTo(1);
        assertThat(run.path("columns").get(0).path("key").asText()).isEqualTo("code");
        assertThat(run.path("rows").get(0).path("code").asText()).isEqualTo(routingCode);
        assertThat(run.path("rows").get(0).path("step_count").asInt()).isEqualTo(1);

        // 4. 参数白名单：数据集没声明的键直接 400（用户输入永远拼不进 SQL）
        mockMvc.perform(post("/api/v1/ana/reports/{id}/run", routingTemplateId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("params", Map.of("sql", "1=1")))))
                .andExpect(status().isBadRequest());

        // 5. 自建模板：编码自动 RPT-xxxxxx，参数按提交顺序解析（类型来自数据集字典）
        JsonNode created = call(post("/api/v1/ana/reports"), admin, json(Map.of(
                "name", "我的工艺路线报表", "dataset", "capp.routing",
                "category", "CAPP", "description", "只看已发布",
                "params", List.of(Map.of("key", "keyword", "label", "关键字"),
                        Map.of("key", "status", "label", "状态")))));
        long customId = created.path("id").asLong();
        assertThat(created.path("code").asText()).startsWith("RPT-");
        assertThat(created.path("builtIn").asBoolean()).isFalse();
        assertThat(created.path("params").get(1).path("type").asText()).isEqualTo("SELECT");
        assertThat(created.path("params").get(1).path("options").size()).isEqualTo(3);

        // 6. 数据集未声明的参数在保存时也拒绝
        mockMvc.perform(post("/api/v1/ana/reports")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法参数报表", "dataset", "capp.routing",
                                "params", List.of(Map.of("key", "sql", "label", "注入"))))))
                .andExpect(status().isBadRequest());

        // 7. 内置模板只读
        mockMvc.perform(put("/api/v1/ana/reports/{id}", routingTemplateId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "改内置", "dataset", "capp.routing"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/ana/reports/{id}", routingTemplateId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 8. 导出 xlsx：返回二进制，签名是 PK（zip/xlsx）
        byte[] excel = mockMvc.perform(post("/api/v1/ana/reports/{id}/export.xlsx", customId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("params", Map.of()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(excel.length).isGreaterThan(0);
        assertThat(excel[0]).isEqualTo((byte) 'P');
        assertThat(excel[1]).isEqualTo((byte) 'K');

        // 9. 修改自建模板后删除（软删）
        JsonNode updated = call(put("/api/v1/ana/reports/{id}", customId), admin, json(Map.of(
                "name", "我的工艺路线报表（改）", "dataset", "capp.routing", "sortNo", 5)));
        assertThat(updated.path("name").asText()).isEqualTo("我的工艺路线报表（改）");
        assertThat(updated.path("sortNo").asInt()).isEqualTo(5);
        call(delete("/api/v1/ana/reports/{id}", customId), admin, null);
        assertThat(call(get("/api/v1/ana/reports"), admin, null).path("total").asLong()).isEqualTo(5);
    }

    // ------------------------------------------------------------ 工具

    private Map<String, Object> routing(String name, long objectId) {
        Map<String, Object> step = new HashMap<>();
        step.put("seqNo", 10);
        step.put("name", "下料");
        step.put("opType", "MACHINING");
        step.put("setupMinutes", 0);
        step.put("runMinutes", 1);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("objectId", objectId);
        body.put("steps", List.of(step));
        return body;
    }

    private long createItem(String token, String name) throws Exception {
        return call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA")))
                .path("id").asLong();
    }

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
