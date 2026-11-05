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

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
    void securityRulesAclAndLifecycleWork() throws Exception {
        String token = login();
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        long adminId = body(loginResult).path("data").path("user").path("id").asLong();

        // 1. 规则权限试算：内置变量 ${currentUserId} + 嵌套 AND/OR
        JsonNode ruleTest = call2(post("/api/v1/security/rules/test"),
                token,
                json(Map.of(
                        "currentUserId", String.valueOf(adminId),
                        "exprJson", """
                                {"op":"AND","children":[
                                  {"op":"EQ","field":"ownerId","value":"${currentUserId}"},
                                  {"op":"OR","children":[
                                     {"op":"EQ","field":"lifecycleState","value":"Released"},
                                     {"op":"GT","field":"ext_json.level","value":2}
                                  ]}
                                ]}
                                """,
                        "attributes", Map.of("ownerId", String.valueOf(adminId),
                                "lifecycleState", "Draft", "ext_json", Map.of("level", 3)))));
        assertThat(ruleTest.path("matched").asBoolean()).isTrue();

        // 2. 对象 ACL：显式授权 → 命中；DENY 优先；未授权默认拒绝
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                                                lifecycle_state, revision, iteration, is_latest, version, deleted, ext_json)
                        VALUES (900001, 'PdmDoc', 900001, 'DOC-0001', '冒烟测试文档', 1, ?, 'Draft',
                                1, 1, true, 0, false, '{}'::jsonb)
                        ON CONFLICT DO NOTHING
                        """, adminId);

        mockMvc.perform(post("/api/v1/security/acl")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("objectId", 900001, "actorType", "USER",
                                "actorId", adminId, "actions", "READ,WRITE"))))
                .andExpect(status().isOk());

        JsonNode allowed = call2(post("/api/v1/security/acl/check"), token,
                json(Map.of("objectId", 900001, "action", "READ", "userId", adminId)));
        assertThat(allowed.path("allowed").asBoolean()).isTrue();

        // 属主可删自己的对象，但审批权限不在属主隐式权限内（防自审）
        JsonNode notGranted = call2(post("/api/v1/security/acl/check"), token,
                json(Map.of("objectId", 900001, "action", "APPROVE", "userId", adminId)));
        assertThat(notGranted.path("allowed").asBoolean()).isFalse();

        // 无关用户默认拒绝
        JsonNode stranger = call2(post("/api/v1/security/acl/check"), token,
                json(Map.of("objectId", 900001, "action", "READ", "userId", 999999L)));
        assertThat(stranger.path("allowed").asBoolean()).isFalse();

        mockMvc.perform(post("/api/v1/security/acl")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("objectId", 900001, "actorType", "ALL",
                                "actions", "READ", "effect", "DENY"))))
                .andExpect(status().isOk());
        JsonNode denied = call2(post("/api/v1/security/acl/check"), token,
                json(Map.of("objectId", 900001, "action", "READ", "userId", adminId)));
        assertThat(denied.path("allowed").asBoolean()).isFalse();

        // 3. 生命周期：Draft --submit--> InWorkflow --approve--> Released；非法迁移 409
        JsonNode afterSubmit = call2(post("/api/v1/objects/900001/transitions"), token,
                json(Map.of("action", "submit", "reason", "提交审批")));
        assertThat(afterSubmit.path("lifecycleState").asText()).isEqualTo("InWorkflow");

        JsonNode afterApprove = call2(post("/api/v1/objects/900001/transitions"), token,
                json(Map.of("action", "approve")));
        assertThat(afterApprove.path("lifecycleState").asText()).isEqualTo("Released");

        mockMvc.perform(post("/api/v1/objects/900001/transitions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("action", "submit"))))
                .andExpect(status().isConflict());

        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM plm_state_log WHERE object_id = 900001", Integer.class);
        assertThat(logCount).isEqualTo(2);
    }

    private JsonNode call2(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                           String token, String bodyJson) throws Exception {
        MvcResult result = mockMvc.perform(builder
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data");
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
