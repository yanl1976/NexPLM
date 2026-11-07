package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * 批次 F-2/F-3/F-4 冒烟：系统广播、运行日志页、三员互斥。
 *
 * <p>取证与取舍见 docs/06 EXP-20261107-16。这里只验证"新系统真的做到了"
 * 四件事：广播能发能弹能记已读、登录/操作日志能查、三员互斥拦得住、
 * 审计类权限不能被随手发给业务角色。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysAdminSmokeTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------------- F-3

    @Test
    void loginLogsAndAuditLogsAreQueryable() throws Exception {
        String token = login();

        // 登录日志：本用例的登录本身就该出现在列表里
        JsonNode loginPage = call(get("/api/v1/sys/logs/login").queryParam("username", "admin"), token);
        assertThat(loginPage.path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(loginPage.path("items").get(0).path("result").asText()).isEqualTo("SUCCESS");

        // 失败登录也能查出来（含失败原因）
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "bad-password"))))
                .andExpect(status().isUnauthorized());
        JsonNode failed = call(get("/api/v1/sys/logs/login")
                .queryParam("username", "admin")
                .queryParam("result", "FAILED"), token);
        assertThat(failed.path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(failed.path("items").get(0).path("failReason").asText()).contains("密码");

        // 概览：哈希链未启用（AuditAspect 不写 prev_hash/hash）必须如实返回 false，不能伪装"校验通过"
        JsonNode summary = call(get("/api/v1/sys/logs/summary"), token);
        assertThat(summary.path("loginSuccess").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(summary.path("chainEnabled").asBoolean()).isFalse();
        assertThat(summary.path("chainMessage").asText()).contains("未启用");

        // 操作日志：模块/关键词过滤可用（登录本身不会写审计，这里只断言接口通且结构正确）
        JsonNode auditPage = call(get("/api/v1/sys/logs/audit").queryParam("module", "IAM"), token);
        assertThat(auditPage.has("items")).isTrue();
        assertThat(auditPage.has("total")).isTrue();
    }

    // ---------------------------------------------------------------- F-2

    @Test
    void broadcastPublishPopupAndRead() throws Exception {
        String token = login();
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);

        // 1. 建草稿：不会出现在别人的弹窗里
        Map<String, Object> form = new HashMap<>();
        form.put("title", "F-2 冒烟广播");
        form.put("content", "系统将于今晚 22:00 停机维护。");
        form.put("level", "URGENT");
        form.put("popup", true);
        form.put("publishAt", null);
        form.put("expireAt", null);
        String broadcastId = call(post("/api/v1/sys/broadcasts"), token, json(form)).asText();
        assertThat(pendingCount(token)).isZero();

        // 2. 草稿可编辑
        form.put("title", "F-2 冒烟广播（已修订）");
        call(put("/api/v1/sys/broadcasts/{id}", broadcastId), token, json(form));

        // 3. 发布 → 弹窗里能取到
        call(post("/api/v1/sys/broadcasts/{id}/publish", broadcastId), token, null);
        JsonNode pending = call(get("/api/v1/sys/broadcasts/pending"), token);
        assertThat(pending.size()).isEqualTo(1);
        assertThat(pending.get(0).path("title").asText()).contains("已修订");
        assertThat(pending.get(0).path("level").asText()).isEqualTo("URGENT");

        // 4. 已发布内容不许偷改（防止已读的人看到的内容被替换）
        mockMvc.perform(put("/api/v1/sys/broadcasts/{id}", broadcastId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(form)))
                .andExpect(status().isBadRequest());

        // 5. 标记已读 → 不再弹；重复标记幂等
        call(post("/api/v1/sys/broadcasts/{id}/read", broadcastId), token, null);
        assertThat(pendingCount(token)).isZero();
        call(post("/api/v1/sys/broadcasts/{id}/read", broadcastId), token, null);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_broadcast_read WHERE broadcast_id = ? AND user_id = ?",
                Long.class, Long.parseLong(broadcastId), adminId)).isEqualTo(1L);

        // 6. 撤回 → 管理列表状态变化；撤回后可删除
        call(post("/api/v1/sys/broadcasts/{id}/revoke", broadcastId), token, null);
        JsonNode page = call(get("/api/v1/sys/broadcasts").queryParam("state", "REVOKED"), token);
        assertThat(page.path("total").asLong()).isEqualTo(1);
        call(delete("/api/v1/sys/broadcasts/{id}", broadcastId), token, null);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_broadcast WHERE id = ?", Long.class,
                Long.parseLong(broadcastId))).isZero();

        // 7. 发布/撤回都留审计痕迹（F-3 的操作日志页因此有内容）
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM sys_audit_log
                 WHERE action IN ('BROADCAST_PUBLISH', 'BROADCAST_REVOKE', 'BROADCAST_DELETE')
                """, Long.class)).isGreaterThanOrEqualTo(3);
    }

    // ---------------------------------------------------------------- F-4

    @Test
    void threeRoleRolesAreMutuallyExclusive() throws Exception {
        String token = login();
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        Long sysAdminRole = roleId("SYS_ADMIN");
        Long auditorRole = roleId("AUDITOR");

        // 1. 同时给 admin 分配 系统管理员 + 审计员 → 拒绝（三权分立的硬约束）
        Map<String, Object> body = new HashMap<>();
        body.put("displayName", "系统管理员");
        body.put("roleIds", List.of(String.valueOf(sysAdminRole), String.valueOf(auditorRole)));
        mockMvc.perform(put("/api/v1/system/users/{id}", adminId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest());

        // 2. 单一三员角色可以保存
        body.put("roleIds", List.of(String.valueOf(sysAdminRole)));
        call(put("/api/v1/system/users/{id}", adminId), token, json(body));

        // 3. 三员视图：三角色齐备，且没有互斥违规
        JsonNode overview = call(get("/api/v1/sys/three-roles"), token);
        assertThat(overview.path("roles").size()).isEqualTo(3);
        assertThat(overview.path("violations").size()).isZero();
        assertThat(overview.path("note").asText()).contains("互斥");

        // 4. 审计类权限不可普通授权：把 sys.log.read 发给 BUSINESS 角色应被拒
        Long businessRole = roleId("BUSINESS");
        List<String> businessPerms = jdbcTemplate.queryForList(
                "SELECT permission_code FROM sys_role_permission WHERE role_id = ?",
                String.class, businessRole);
        List<String> escalated = new java.util.ArrayList<>(businessPerms);
        escalated.add("sys.log.read");
        mockMvc.perform(put("/api/v1/system/roles/{id}/permissions", businessRole)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("permissionCodes", escalated))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------ 工具

    private int pendingCount(String token) throws Exception {
        return call(get("/api/v1/sys/broadcasts/pending"), token).size();
    }

    private Long roleId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_role WHERE code = ?", Long.class, code);
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Device-Type", "PC")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "admin", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return call(builder, token, null);
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
