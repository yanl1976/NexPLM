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

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台冒烟测试：真实 PostgreSQL（嵌入式）+ Flyway 迁移 + 登录 + 组织/角色/元数据链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlatformSmokeTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动嵌入式 PostgreSQL 失败", ex);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
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
    void loginAndCoreApisWork() throws Exception {
        String token = login();
        assertThat(token).isNotBlank();

        // 1. 内置角色与权限点已由 Flyway 初始化
        JsonNode roles = call(get("/api/v1/system/roles"), token);
        assertThat(roles.size()).isGreaterThanOrEqualTo(4);

        // 2. 组织树为空，可创建根组织
        JsonNode orgs = call(get("/api/v1/system/orgs"), token);
        assertThat(orgs.size()).isZero();

        MvcResult created = mockMvc.perform(post("/api/v1/system/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "code", "RD",
                                "name", "研发中心",
                                "orgType", "DEPT",
                                "sort", 10,
                                "status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andReturn();
        String orgId = body(created).path("data").asText();
        assertThat(orgId).isNotBlank();

        JsonNode orgsAfter = call(get("/api/v1/system/orgs"), token);
        assertThat(orgsAfter.size()).isEqualTo(1);
        assertThat(orgsAfter.get(0).path("name").asText()).isEqualTo("研发中心");

        // 3. 用户管理：新建用户并赋予业务角色
        JsonNode roleList = call(get("/api/v1/system/roles"), token);
        long businessRoleId = 0;
        for (JsonNode role : roleList) {
            if ("BUSINESS".equals(role.path("code").asText())) {
                businessRoleId = role.path("id").asLong();
            }
        }
        assertThat(businessRoleId).isPositive();
        mockMvc.perform(post("/api/v1/system/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "zhangsan",
                                "password", "Passw0rd!",
                                "displayName", "张三",
                                "securityLevel", 1,
                                "roleIds", new long[]{businessRoleId}))))
                .andExpect(status().isOk());

        JsonNode users = call(get("/api/v1/system/users?page=1&size=10"), token).path("items");
        assertThat(users.size()).isEqualTo(2);

        // 4. 元数据建模：创建对象类型 → 加字段 → 发布 → 查询详情
        mockMvc.perform(post("/api/v1/meta/entities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "code", "DemoItem",
                                "name", "示例对象",
                                "versionPolicy", "LETTER",
                                "workflowAllowed", true,
                                "capabilities", new String[]{"createPermission", "advanceQuery"},
                                "fields", new Object[]{
                                        Map.of("code", "name", "name", "名称", "valueType", "DefaultObjectValue"),
                                        Map.of("code", "docType", "name", "类型", "valueType", "EnumValue",
                                                "enumCode", "DocType"),
                                        Map.of("code", "weight", "name", "重量", "valueType", "DefaultObjectValue",
                                                "dataType", "DECIMAL")
                                }))))
                .andExpect(status().isOk());

        JsonNode detail = call(get("/api/v1/meta/entities/DemoItem"), token);
        assertThat(detail.path("fields").size()).isEqualTo(3);
        assertThat(detail.path("status").asText()).isEqualTo("DRAFT");

        mockMvc.perform(post("/api/v1/meta/entities/DemoItem/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode published = call(get("/api/v1/meta/entities/DemoItem"), token);
        assertThat(published.path("status").asText()).isEqualTo("PUBLISHED");
    }

    @Test
    void unauthorizedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/system/orgs")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                          String token) throws Exception {
        MvcResult result = mockMvc.perform(builder.header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data");
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
