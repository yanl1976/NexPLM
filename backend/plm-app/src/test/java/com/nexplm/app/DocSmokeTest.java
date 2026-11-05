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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

        // 17. 文档分类：分类树 + 属性（含继承）
        JsonNode classTree = call(get("/api/v1/doc-classes"), admin, null);
        assertThat(classTree.size()).isEqualTo(4);
        long mechId = 910001L;
        long assyId = 910002L;
        JsonNode assyAttrs = call(get("/api/v1/doc-classes/{id}/attributes", assyId), admin, null);
        assertThat(assyAttrs.size()).isEqualTo(2);
        assertThat(assyAttrs.get(0).path("code").asText()).isEqualTo("sheetSize");
        assertThat(assyAttrs.get(0).path("required").asBoolean()).isTrue();

        // 18. 按分类建文档：必填属性缺失 → 400
        mockMvc.perform(post("/api/v1/docs")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "装配图-未填属性", "docType", "DESIGN", "classId", assyId))))
                .andExpect(status().isBadRequest());

        // 19. 枚举值越界 → 400
        mockMvc.perform(post("/api/v1/docs")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "装配图-图幅越界", "docType", "DESIGN", "classId", assyId,
                                "classAttrs", Map.of("sheetSize", "A9")))))
                .andExpect(status().isBadRequest());

        // 20. 填齐属性 → 建文档成功，详情回带分类与属性值
        JsonNode classified = call(post("/api/v1/docs"), admin, json(Map.of(
                "name", "主传动装配图 V2", "docType", "DESIGN", "classId", assyId,
                "classAttrs", Map.of("sheetSize", "A1", "material", "40Cr"))));
        assertThat(classified.path("classId").asLong()).isEqualTo(assyId);
        assertThat(classified.path("className").asText()).isEqualTo("装配图");
        assertThat(classified.path("classAttrs").path("sheetSize").asText()).isEqualTo("A1");
        assertThat(classified.path("classAttrs").path("material").asText()).isEqualTo("40Cr");

        // 21. 按分类检索（含子分类）：MECH 子树命中新建的 1 个文档
        JsonNode filtered = call(get("/api/v1/docs?page=1&size=20&classId=" + mechId), admin, null);
        assertThat(filtered.path("total").asLong()).isEqualTo(1);
        assertThat(filtered.path("items").get(0).path("className").asText()).isEqualTo("装配图");

        // 22. 分类下仍有文档时不允许删除
        mockMvc.perform(delete("/api/v1/doc-classes/{id}", assyId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 23. 全文检索：中文由 ILIKE 兜底，两个「装配图」文档命中并返回高亮片段
        JsonNode searched = call(get("/api/v1/docs?page=1&size=20&q=装配"), admin, null);
        assertThat(searched.path("total").asLong()).isEqualTo(2);
        assertThat(searched.path("items").get(0).path("name").asText()).contains("装配");
        assertThat(searched.path("items").get(0).path("highlight").asText()).isNotBlank();

        // 24. 保存高级查询：可保存、可共享、他人可见但不可删
        JsonNode saved = call(post("/api/v1/doc-queries"), admin, json(Map.of(
                "name", "我的装配图查询",
                "conditionJson", "{\"q\":\"装配\",\"docType\":\"DESIGN\"}",
                "shared", true)));
        long queryId = saved.path("id").asLong();
        assertThat(queryId).isPositive();
        JsonNode queries = call(get("/api/v1/doc-queries"), admin, null);
        assertThat(queries.size()).isEqualTo(1);
        assertThat(queries.get(0).path("shared").asBoolean()).isTrue();
        assertThat(queries.get(0).path("mine").asBoolean()).isTrue();

        JsonNode otherQueries = call(get("/api/v1/doc-queries"), other, null);
        assertThat(otherQueries.size()).isEqualTo(1);
        assertThat(otherQueries.get(0).path("mine").asBoolean()).isFalse();
        mockMvc.perform(delete("/api/v1/doc-queries/{id}", queryId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());

        // 25. 批量下载：两个版本各取主文件打包，重名自动加序号
        MvcResult zipResult = mockMvc.perform(post("/api/v1/docs/batch/download")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", new long[]{docId, newId}))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(zipResult.getResponse().getContentType()).contains("zip");
        byte[] zipBytes = zipResult.getResponse().getContentAsByteArray();
        assertThat(zipBytes.length).isPositive();
        int entries = 0;
        try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipBytes),
                StandardCharsets.UTF_8)) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                assertThat(entry.getName()).startsWith("DOC-000001_");
                entries++;
                entry = zipIn.getNextEntry();
            }
        }
        assertThat(entries).isEqualTo(2);

        // 26. 批量授权：逐条返回结果，ACL 可查到
        JsonNode grantResults = call(post("/api/v1/docs/batch/grant"), admin, json(Map.of(
                "ids", new long[]{docId, newId}, "actorType", "ALL", "actions", "READ")));
        assertThat(grantResults.size()).isEqualTo(2);
        assertThat(grantResults.get(0).path("ok").asBoolean()).isTrue();
        JsonNode aclAfter = call(get("/api/v1/docs/{id}/acl", newId), admin, null);
        assertThat(aclAfter.size()).isEqualTo(1);
        assertThat(aclAfter.get(0).path("actorType").asText()).isEqualTo("ALL");
        assertThat(aclAfter.get(0).path("actions").asText()).isEqualTo("READ");

        // 27. 预览：DWG 无可用转换器 → SKIPPED 并给出原因（不当作失败）
        JsonNode previewNone = call(get("/api/v1/docs/{id}/preview", docId), admin, null);
        assertThat(previewNone.path("status").asText()).isEqualTo("NONE");
        JsonNode previewSkipped = call(post("/api/v1/docs/{id}/preview", docId), admin, null);
        assertThat(previewSkipped.path("status").asText()).isEqualTo("SKIPPED");
        assertThat(previewSkipped.path("message").asText()).contains("暂不支持");

        // 28. 预览：图片直通 → READY，产物可从文件仓下载且内容一致
        JsonNode pngDoc = call(post("/api/v1/docs"), admin, json(Map.of(
                "name", "示意图文档", "docType", "DESIGN")));
        long pngDocId = pngDoc.path("id").asLong();
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        MvcResult pngUpload = mockMvc.perform(multipart("/api/v1/docs/{id}/files", pngDocId)
                        .file(new MockMultipartFile("file", "示意图.png", "image/png", png))
                        .param("fileRole", "MAIN")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        long pngFileId = body(pngUpload).path("data").path("fileId").asLong();
        JsonNode previewReady = call(
                post("/api/v1/docs/{id}/preview?fileId=" + pngFileId, pngDocId), admin, null);
        assertThat(previewReady.path("status").asText()).isEqualTo("READY");
        assertThat(previewReady.path("format").asText()).isEqualTo("PNG");
        assertThat(previewReady.path("previewFileId").asLong()).isPositive();
        long previewFileId = previewReady.path("previewFileId").asLong();
        MvcResult previewDownload = mockMvc.perform(get("/api/v1/files/{fileId}/content", previewFileId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(previewDownload.getResponse().getContentAsByteArray()).isEqualTo(png);

        // 29. 批量导入 CSV：2 行成功、2 行失败（缺名称 / 分类编码不存在），单行失败不影响其它行
        String csv = "code,name,docType,source,classCode,keyword,description\n"
                + ",导入-电气原理图甲,DESIGN,NORMAL,ELEC,导入,ok\n"
                + ",导入-无分类文档,TECH,,,,\n"
                + ",,OTHER,,,,缺名称应报错\n"
                + ",导入-坏分类,OTHER,,MECH-NOTEXIST,,\n";
        MvcResult importResult = mockMvc.perform(multipart("/api/v1/docs/batch/import")
                        .file(new MockMultipartFile("file", "import.csv", "text/csv",
                                csv.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode imported = body(importResult).path("data");
        assertThat(imported.path("total").asInt()).isEqualTo(4);
        assertThat(imported.path("success").asInt()).isEqualTo(2);
        assertThat(imported.path("failed").asInt()).isEqualTo(2);
        assertThat(imported.path("errors").size()).isEqualTo(2);
        assertThat(imported.path("errors").get(0).path("line").asInt()).isEqualTo(4);
        assertThat(imported.path("createdIds").size()).isEqualTo(2);
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
