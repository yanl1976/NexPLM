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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P10-5 安全加固回归：认证、越权、跨公司隔离、SQL 注入、XSS、下载文件名穿越。
 *
 * <p>这些点是"出过一次就够致命"的地方，因此放在一个用例里串起来跑，任何一条退化都会红。
 * 只测**服务端可控**的部分：XSS 的转义责任在前端（Vue 默认插值转义），服务端要保证的是
 * 不把用户输入当模板/脚本执行、且响应 Content-Type 是 JSON（不被浏览器当 HTML 渲染）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHardeningTest {

    private static final Path STORAGE_ROOT;
    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            STORAGE_ROOT = Files.createTempDirectory("plm-sec-files");
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
        registry.add("nexplm.file.storage", () -> "local");
        registry.add("nexplm.file.root", () -> STORAGE_ROOT.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void authenticationAuthorizationAndInputHardening() throws Exception {
        String admin = login("admin", "Admin@12345");
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);

        // ---- 1. 未登录访问受保护接口 → 401（不是 500，也不该漏出业务数据）
        mockMvc.perform(get("/api/v1/docs")).andExpect(status().isUnauthorized());

        // ---- 2. 篡改令牌 → 401
        mockMvc.perform(get("/api/v1/docs").header("Authorization", "Bearer " + admin + "tampered"))
                .andExpect(status().isUnauthorized());

        // ---- 3. 已登录但无任何角色 → 403（先建一个只有登录态的账号）
        String viewer = createViewerAndLogin(admin);

        mockMvc.perform(get("/api/v1/docs").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ana/dashboard").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ana/boards").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/system/users")
                        .header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "hacker", "password", "Hacker@12345",
                                "displayName", "越权创建"))))
                .andExpect(status().isForbidden());

        // ---- 4. 跨公司隔离：别家公司的文档不进列表、按 id 也取不到
        //   注意不能用 admin 验证：SYS_ADMIN 的数据作用域是 ALL（集团管理员要看全集团），
        //   必须用 COMPANY 作用域的普通业务用户才能证明隔离生效。
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                                lifecycle_state, is_latest, deleted)
                        VALUES (?, 'PdmDoc', ?, ?, ?, ?, 'Draft', true, false)
                        """,
                94_000_000_001L, 94_000_000_001L, "DOC-950001", "跨公司机密文档", 999L);
        jdbcTemplate.update("""
                        INSERT INTO doc_master (object_id, doc_type, source)
                        VALUES (?, 'DESIGN', 'NORMAL')
                        """, 94_000_000_001L);

        String companyUser = createCompanyUserAndLogin(admin);
        JsonNode leaked = call(get("/api/v1/docs?keyword=跨公司机密"), companyUser, null);
        assertThat(leaked.path("total").asLong()).isZero();

        mockMvc.perform(get("/api/v1/docs/{id}", 94_000_000_001L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());

        // 集团管理员（作用域 ALL）本就该看得到，用于反向确认这条数据确实存在、隔离断言不是因为数据没插进去
        JsonNode byAdmin = call(get("/api/v1/docs?keyword=跨公司机密"), admin, null);
        assertThat(byAdmin.path("total").asLong()).isEqualTo(1);

        // ---- 4b. 其余域按 id 直取同样受公司隔离（变更 / 项目 / 供应商）
        //    静态审计（docs/07 待整改 ③）显示三域主查询都带 company_id，这里用真实请求复验。
        jdbcTemplate.update("""
                        INSERT INTO chg_ecr (id, company_id, code, title, change_type, priority, status,
                                             requester_id, created_at, updated_at, deleted)
                        VALUES (?, 999, ?, ?, 'DESIGN', 'NORMAL', 'DRAFT', ?, now(), now(), false)
                        """, 94_000_000_002L, "ECR-950001", "跨公司机密变更", 1L);
        jdbcTemplate.update("""
                        INSERT INTO prj_project (id, company_id, code, name, type, status, priority,
                                                 progress, created_at, updated_at, deleted)
                        VALUES (?, 999, ?, ?, 'INTERNAL', 'PLANNING', 0, 0, now(), now(), false)
                        """, 94_000_000_003L, "PJ-950001", "跨公司机密项目");
        jdbcTemplate.update("""
                        INSERT INTO sup_supplier (id, company_id, code, name, category, status, currency,
                                                  created_at, updated_at, deleted)
                        VALUES (?, 999, ?, ?, 'MATERIAL', 'QUALIFIED', 'CNY', now(), now(), false)
                        """, 94_000_000_004L, "SUP-950001", "跨公司机密供应商");

        mockMvc.perform(get("/api/v1/chg/ecrs/{id}", 94_000_000_002L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/chg/ecrs/{id}/timeline", 94_000_000_002L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/prj/projects/{id}", 94_000_000_003L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/sup/suppliers/{id}", 94_000_000_004L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());

        // ---- 5. SQL 注入：关键字里的引号/OR 只是普通字符串，不能把结果集放大成全表
        JsonNode injected = call(get("/api/v1/docs?keyword=%27%20OR%201%3D1--"), admin, null);
        assertThat(injected.path("total").asLong()).isZero();

        JsonNode injectedLike = call(get("/api/v1/docs?keyword=%25%27%20OR%20%271%27%3D%271"), admin, null);
        assertThat(injectedLike.path("total").asLong()).isZero();

        // ---- 6. XSS：脚本串只当普通名称存储与返回，响应仍是 JSON（浏览器不会当 HTML 执行）
        String xssName = "<script>alert(1)</script>";
        JsonNode created = call(post("/api/v1/docs"), admin, json(Map.of(
                "name", xssName, "docType", "DESIGN", "source", "NORMAL")));
        long docId = created.path("id").asLong();
        assertThat(created.path("name").asText()).isEqualTo(xssName);

        MvcResult detail = mockMvc.perform(get("/api/v1/docs/{id}", docId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(detail.getResponse().getContentType()).contains("application/json");
        assertThat(objectMapper.readTree(detail.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("name").asText()).isEqualTo(xssName);

        // ---- 7. 下载文件名穿越与超长：只保留末段、限长，不能把 ../../ 带进响应头
        String fileId = uploadTextFile(admin);
        String traversal = downloadDisposition(fileId, "../../etc/passwd", admin);
        assertThat(traversal).contains("filename=\"passwd\"");
        assertThat(traversal).doesNotContain("..");
        assertThat(traversal).doesNotContain("%2F");

        String longName = "a".repeat(500) + ".txt";
        String longDisposition = downloadDisposition(fileId, longName, admin);
        // 头里 filename 与 filename* 各出现一次，故看"单次文件名不超过 200"而不是整头长度
        assertThat(longDisposition).doesNotContain("a".repeat(201));
        assertThat(longDisposition.length()).isLessThan(600);

        // 全控制字符的文件名退化时回退到按 sha256 生成的名字，而不是空文件名
        String controlOnly = downloadDisposition(fileId, "\r\n\t", admin);
        assertThat(controlOnly).contains("filename=\"file-");

        // ---- 8. 旁路接口同样受数据范围约束：关联/日志/预览/批量下载不能凭 id 越权
        mockMvc.perform(get("/api/v1/docs/{id}/relations", 94_000_000_001L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/docs/{id}/logs", 94_000_000_001L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/docs/{id}/preview", 94_000_000_001L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/docs/{id}/preview", 94_000_000_001L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/visual/docs/{id}", 94_000_000_001L)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());

        // 批量打包下载：看不见的文档直接被跳过，不能把内容装进 zip
        MvcResult zip = mockMvc.perform(post("/api/v1/docs/batch/download")
                        .header("Authorization", "Bearer " + companyUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", java.util.List.of(94_000_000_001L)))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(zipEntryCount(zip.getResponse().getContentAsByteArray())).isZero();

        // ---- 9. 文件旁路：挂文件/列文件/下载内容/解绑都不能绕过对象数据范围
        MockMultipartFile probe = new MockMultipartFile("file", "probe.txt", "text/plain",
                "probe".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/files").file(probe)
                        .param("ownerObjectId", String.valueOf(94_000_000_001L))
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/files")
                        .param("ownerObjectId", String.valueOf(94_000_000_001L))
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());

        // 管理员（作用域 ALL）把文件挂到跨公司文档上，普通用户仍不能下载、也不能解绑
        String foreignFileId = uploadToObject("foreign.txt", 94_000_000_001L, admin);
        mockMvc.perform(get("/api/v1/files/{id}/content", foreignFileId)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());
        Long foreignLinkId = jdbcTemplate.queryForObject(
                "SELECT id FROM file_link WHERE file_id = ?", Long.class, Long.valueOf(foreignFileId));
        mockMvc.perform(delete("/api/v1/files/links/{id}", foreignLinkId)
                        .header("Authorization", "Bearer " + companyUser))
                .andExpect(status().isNotFound());

        // ---- 10. 组织归属兜底：请求不传 orgId 时落库要取用户主组织，否则 ORG 范围用户读不到自己建的对象
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
        jdbcTemplate.update("""
                        INSERT INTO sys_user_org (user_id, org_id, is_primary) VALUES (?, ?, true)
                        ON CONFLICT (user_id, org_id) DO UPDATE SET is_primary = true
                        """, adminId, 77_000_001L);
        JsonNode ownDoc = call(post("/api/v1/docs"), admin, json(Map.of(
                "name", "组织归属兜底", "docType", "DESIGN", "source", "NORMAL")));
        assertThat(jdbcTemplate.queryForObject("SELECT org_id FROM plm_object WHERE id = ?",
                Long.class, ownDoc.path("id").asLong())).isEqualTo(77_000_001L);

        // ---- 11. 密码策略：弱密码拒绝、自助改密（旧密码校验 + 历史不可重用）
        mockMvc.perform(post("/api/v1/system/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "weak_user", "password", "12345678",
                                "displayName", "弱密码用户"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/system/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "pwd_user", "password", "Str0ng!Pass1",
                                "displayName", "改密用户"))))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE sys_user SET must_change_pwd = false WHERE username = 'pwd_user'");
        String pwdUser = login("pwd_user", "Str0ng!Pass1");

        // 旧密码错 → 401
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + pwdUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "Wrong@12345",
                                "newPassword", "Str0ng!Pass2"))))
                .andExpect(status().isUnauthorized());
        // 新密码强度不足 → 400
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + pwdUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "Str0ng!Pass1",
                                "newPassword", "aaaaaaaa"))))
                .andExpect(status().isBadRequest());
        // 正常改密 → 200
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + pwdUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "Str0ng!Pass1",
                                "newPassword", "Str0ng!Pass2"))))
                .andExpect(status().isOk());
        // 再改回旧密码 → 400（历史不可重用）
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + pwdUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "Str0ng!Pass2",
                                "newPassword", "Str0ng!Pass1"))))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_user_pwd_history WHERE user_id = "
                        + "(SELECT id FROM sys_user WHERE username = 'pwd_user')", Long.class))
                .isGreaterThanOrEqualTo(2);

        // ---- 12. 审计防篡改：内容被改后链校验必须报出该行，改回后恢复一致
        JsonNode chainOk = call(get("/api/v1/sys/logs/audit/verify"), admin, null);
        assertThat(chainOk.path("brokenCount").asInt()).isZero();
        assertThat(chainOk.path("checked").asLong()).isPositive();

        Long tamperId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_audit_log WHERE hash IS NOT NULL ORDER BY id DESC LIMIT 1", Long.class);
        String originalDetail = jdbcTemplate.queryForObject(
                "SELECT detail FROM sys_audit_log WHERE id = ?", String.class, tamperId);
        jdbcTemplate.update("UPDATE sys_audit_log SET detail = ? WHERE id = ?",
                "tampered-by-test", tamperId);

        JsonNode chainBroken = call(get("/api/v1/sys/logs/audit/verify"), admin, null);
        assertThat(chainBroken.path("brokenCount").asInt()).isEqualTo(1);
        assertThat(chainBroken.path("brokenIds").get(0).asLong()).isEqualTo(tamperId);

        jdbcTemplate.update("UPDATE sys_audit_log SET detail = ? WHERE id = ?", originalDetail, tamperId);
        JsonNode chainRestored = call(get("/api/v1/sys/logs/audit/verify"), admin, null);
        assertThat(chainRestored.path("brokenCount").asInt()).isZero();
    }

    // ------------------------------------------------------------------ 工具

    private String createViewerAndLogin(String admin) throws Exception {
        JsonNode created = call(post("/api/v1/system/users"), admin, json(Map.of(
                "username", "sec_viewer", "password", "Viewer@12345", "displayName", "安全测试只读")));
        long userId = created.asLong();
        // 首次登录强制改密会拦住后续调用，这里直接把标记清掉
        jdbcTemplate.update("UPDATE sys_user SET must_change_pwd = false WHERE id = ?", userId);
        return login("sec_viewer", "Viewer@12345");
    }

    /** 建一个只有 BUSINESS 角色的用户：作用域是 COMPANY，用来验证跨公司隔离。 */
    private String createCompanyUserAndLogin(String admin) throws Exception {
        Long roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = 'BUSINESS'", Long.class);
        JsonNode created = call(post("/api/v1/system/users"), admin, json(Map.of(
                "username", "sec_company", "password", "Company@12345", "displayName", "公司业务用户",
                "roleIds", java.util.List.of(roleId))));
        jdbcTemplate.update("UPDATE sys_user SET must_change_pwd = false WHERE id = ?",
                created.asLong());
        return login("sec_company", "Company@12345");
    }

    private String uploadTextFile(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain",
                "hello-hardening".getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/api/v1/files").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("fileId").asText();
    }

    /** 上传并直接绑定到指定对象（用于构造"别人的文件"场景）。 */
    private String uploadToObject(String fileName, Long ownerObjectId, String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, "text/plain",
                ("content-" + fileName).getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/api/v1/files").file(file)
                        .param("ownerObjectId", String.valueOf(ownerObjectId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("fileId").asText();
    }

    /** zip 内条目数：用于断言批量下载没有把无权对象装进去。 */
    private int zipEntryCount(byte[] zipBytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            int count = 0;
            while (zip.getNextEntry() != null) {
                count++;
            }
            return count;
        }
    }

    private String downloadDisposition(String fileId, String name, String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/files/{id}/content", fileId)
                        .param("name", name)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return String.valueOf(result.getResponse().getHeader("Content-Disposition"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
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
        MvcResult result = mockMvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus())
                .withFailMessage("HTTP %s body=%s", result.getResponse().getStatus(),
                        result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo(200);
        JsonNode root = body(result);
        return root.has("data") ? root.path("data") : root;
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
