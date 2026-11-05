package com.nexplm.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 图文档冒烟测试：文档创建、文件绑定、检出冲突、检入生成新版本、版本历史、发布后只读、强制解锁。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocSmokeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final Path STORAGE_ROOT;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            STORAGE_ROOT = Files.createTempDirectory("nexplm-doc-");
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
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void documentLifecycleWithFilesAndCheckin() throws Exception {
        String admin = login("admin", "Admin@12345");
        String other = createBusinessUserAndLogin(admin);

        // 1. 创建文档：自动取号、初始状态取生命周期定义
        JsonNode created = call(post("/api/v1/docs"), admin,
                json(Map.of("name", "主传动装配图", "docType", "DESIGN", "source", "CAD", "keyword", "齿轮")));
        long docId = created.path("id").asLong();
        assertThat(created.path("code").asText()).isEqualTo("DOC-000001");
        assertThat(created.path("lifecycleState").asText()).isEqualTo("Draft");
        assertThat(created.path("revisionLabel").asText()).isEqualTo("A.1");
        assertThat(created.path("docType").asText()).isEqualTo("DESIGN");
        assertThat(created.path("fileCount").asInt()).isZero();

        // 2. 非法枚举值被拒绝
        mockMvc.perform(post("/api/v1/docs")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "非法类型", "docType", "NOT_EXIST"))))
                .andExpect(status().isBadRequest());

        // 3. 上传主文件与附件：子表统计随之刷新
        uploadFile(admin, docId, "装配图.dwg", "MAIN", "drawing-content");
        uploadFile(admin, docId, "技术要求.pdf", "ATTACHMENT", "pdf-content");
        JsonNode withFiles = call(get("/api/v1/docs/{id}", docId), admin, null);
        assertThat(withFiles.path("fileCount").asInt()).isEqualTo(2);
        assertThat(withFiles.path("fileExt").asText()).isEqualTo("dwg");
        assertThat(withFiles.path("mainFileId").asLong()).isPositive();
        assertThat(withFiles.path("files").size()).isEqualTo(2);

        // 4. 列表可见（仅最新版本）
        JsonNode page = call(get("/api/v1/docs?page=1&size=20"), admin, null);
        assertThat(page.path("total").asLong()).isEqualTo(1);

        // 5. 未检出直接检入 → 409
        mockMvc.perform(post("/api/v1/docs/{id}/checkin", docId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());

        // 6. 检出：详情带检出信息
        JsonNode checkedOut = call(post("/api/v1/docs/{id}/checkout", docId), admin, "{\"remark\":\"开始修改\"}");
        assertThat(checkedOut.path("checkout").isNull()).isFalse();
        assertThat(checkedOut.path("checkout").path("userId").asLong()).isPositive();

        // 7. 他人修改被拒（软锁），他人上传文件也被拒
        mockMvc.perform(put("/api/v1/docs/{id}", docId)
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "抢占改名", "docType", "DESIGN"))))
                .andExpect(status().isConflict());
        mockMvc.perform(multipart("/api/v1/docs/{id}/files", docId)
                        .file(new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isConflict());

        // 8. 检入：生成 A.2，文件链接随版本复制
        JsonNode checkedIn = call(post("/api/v1/docs/{id}/checkin", docId), admin,
                json(Map.of("pageCount", 12, "remark", "完成修改")));
        long newId = checkedIn.path("id").asLong();
        assertThat(newId).isNotEqualTo(docId);
        assertThat(checkedIn.path("revisionLabel").asText()).isEqualTo("A.2");
        assertThat(checkedIn.path("iteration").asInt()).isEqualTo(2);
        assertThat(checkedIn.path("isLatest").asBoolean()).isTrue();
        assertThat(checkedIn.path("fileCount").asInt()).isEqualTo(2);
        assertThat(checkedIn.path("pageCount").asInt()).isEqualTo(12);
        assertThat(checkedIn.path("files").size()).isEqualTo(2);

        // 9. 版本历史：A.2 最新、A.1 保留
        JsonNode versions = call(get("/api/v1/docs/{id}/versions", newId), admin, null);
        assertThat(versions.size()).isEqualTo(2);
        assertThat(versions.get(0).path("revisionLabel").asText()).isEqualTo("A.2");
        assertThat(versions.get(0).path("isLatest").asBoolean()).isTrue();
        assertThat(versions.get(0).path("id").asLong()).isEqualTo(newId);
        assertThat(versions.get(1).path("revisionLabel").asText()).isEqualTo("A.1");
        assertThat(versions.get(1).path("isLatest").asBoolean()).isFalse();

        // 10. 旧版本仍可独立读取文件
        JsonNode oldVersion = call(get("/api/v1/docs/{id}", docId), admin, null);
        assertThat(oldVersion.path("files").size()).isEqualTo(2);

        // 11. 强制解锁：检出后由管理员解锁，返回原持有者
        call(post("/api/v1/docs/{id}/checkout", newId), admin, "{}");
        JsonNode holder = call(post("/api/v1/docs/{id}/force-unlock", newId), admin, null);
        assertThat(holder.asLong()).isPositive();
        JsonNode afterUnlock = call(get("/api/v1/docs/{id}", newId), admin, null);
        assertThat(afterUnlock.path("checkout").isMissingNode() || afterUnlock.path("checkout").isNull())
                .isTrue();

        // 12. 发布后不可修改：Draft --submit--> InWorkflow --approve--> Released
        call(post("/api/v1/objects/{id}/transitions", newId), admin, json(Map.of("action", "submit")));
        call(post("/api/v1/objects/{id}/transitions", newId), admin, json(Map.of("action", "approve")));
        mockMvc.perform(put("/api/v1/docs/{id}", newId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "已发布后改名", "docType", "DESIGN"))))
                .andExpect(status().isConflict());

        // 13. 对象查看器页签：元数据驱动，PdmDoc 默认 7 个页签
        JsonNode tabs = call(get("/api/v1/meta/entities/PdmDoc/tabs"), admin, null);
        assertThat(tabs.size()).isEqualTo(7);
        assertThat(tabs.get(0).path("tabType").asText()).isEqualTo("PROPS");
        assertThat(tabs.get(6).path("tabType").asText()).isEqualTo("LOGS");

        // 14. 操作日志：审计事件可见（检出/检入/强制解锁）
        JsonNode logs = call(get("/api/v1/docs/{id}/logs?limit=50", newId), admin, null);
        assertThat(logs.size()).isPositive();
        assertThat(logs.findValuesAsText("logType")).contains("AUDIT");
        assertThat(logs.get(0).path("createdAt").asText()).isNotBlank();

        // 15. 关联对象：插入一条引用关系，源/目标两侧都可见
        jdbcTemplate.update("""
                        INSERT INTO plm_relation (id, relation_type, src_id, dst_id, seq, company_id, deleted)
                        SELECT ?, 'REFERENCE', ?, ?, 0, company_id, false FROM plm_object WHERE id = ?
                        """, 990001L, newId, docId, newId);
        JsonNode outgoing = call(get("/api/v1/docs/{id}/relations", newId), admin, null);
        assertThat(outgoing.size()).isEqualTo(1);
        assertThat(outgoing.get(0).path("direction").asText()).isEqualTo("OUT");
        assertThat(outgoing.get(0).path("code").asText()).isEqualTo("DOC-000001");
        assertThat(outgoing.get(0).path("revisionLabel").asText()).isEqualTo("A.1");
        JsonNode incoming = call(get("/api/v1/docs/{id}/relations", docId), admin, null);
        assertThat(incoming.size()).isEqualTo(1);
        assertThat(incoming.get(0).path("direction").asText()).isEqualTo("IN");

        // 16. 实例权限：默认无显式授权
        JsonNode aclRows = call(get("/api/v1/docs/{id}/acl", newId), admin, null);
        assertThat(aclRows.size()).isZero();
    }

    // ---------------------------------------------------------------- 工具

    private void uploadFile(String token, long docId, String fileName, String role, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/docs/{id}/files", docId)
                        .file(new MockMultipartFile("file", fileName, "application/octet-stream",
                                content.getBytes(StandardCharsets.UTF_8)))
                        .param("fileRole", role)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(body(result).path("data").path("fileId").asLong()).isPositive();
    }

    private String createBusinessUserAndLogin(String adminToken) throws Exception {
        JsonNode roles = call(get("/api/v1/system/roles"), adminToken, null);
        long businessRoleId = 0;
        for (JsonNode role : roles) {
            if ("BUSINESS".equals(role.path("code").asText())) {
                businessRoleId = role.path("id").asLong();
            }
        }
        assertThat(businessRoleId).isPositive();
        mockMvc.perform(post("/api/v1/system/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "zhangsan", "password", "Passw0rd!",
                                "displayName", "张三", "securityLevel", 1,
                                "roleIds", new long[]{businessRoleId}))))
                .andExpect(status().isOk());
        return login("zhangsan", "Passw0rd!");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                          String token, String bodyJson) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (bodyJson != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(bodyJson);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return body(result).path("data");
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
