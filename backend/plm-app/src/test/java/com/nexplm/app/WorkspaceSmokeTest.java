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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 工作空间冒烟测试：文件夹树、防环移动、子树路径重写、内容增删、空间隔离、收藏与最近访问。
 */
@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceSmokeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final Path STORAGE_ROOT;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            STORAGE_ROOT = Files.createTempDirectory("nexplm-ws-");
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

    @Test
    void folderTreeContentAndSpaceIsolation() throws Exception {
        String admin = login("admin", "Admin@12345");
        String other = createBusinessUserAndLogin(admin);

        // 1. 个人空间：根文件夹 + 子文件夹，物化路径正确
        JsonNode root = call(post("/api/v1/workspace/folders"), admin,
                json(Map.of("spaceType", "MY", "name", "我的图纸")));
        long rootId = root.path("id").asLong();
        assertThat(root.path("path").asText()).isEqualTo("/" + rootId + "/");

        JsonNode child = call(post("/api/v1/workspace/folders"), admin,
                json(Map.of("spaceType", "MY", "parentId", rootId, "name", "装配图")));
        long childId = child.path("id").asLong();
        assertThat(child.path("path").asText()).isEqualTo("/" + rootId + "/" + childId + "/");
        assertThat(child.path("parentId").asLong()).isEqualTo(rootId);

        JsonNode tree = call(get("/api/v1/workspace/folders?space=MY"), admin, null);
        assertThat(tree.size()).isEqualTo(2);

        // 2. 防环：父文件夹不能移到自己的子文件夹下
        mockMvc.perform(put("/api/v1/workspace/folders/{id}/parent", rootId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("parentId", childId))))
                .andExpect(status().isBadRequest());

        // 3. 子文件夹移到空间根
        mockMvc.perform(put("/api/v1/workspace/folders/{id}/parent", childId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isOk());
        JsonNode moved = call(get("/api/v1/workspace/folders?space=MY"), admin, null);
        JsonNode movedChild = findById(moved, childId);
        assertThat(movedChild.path("path").asText()).isEqualTo("/" + childId + "/");
        assertThat(movedChild.path("parentId").isMissingNode() || movedChild.path("parentId").isNull()).isTrue();

        // 4. 建文档并加入文件夹
        JsonNode doc = call(post("/api/v1/docs"), admin,
                json(Map.of("name", "工作空间测试文档", "docType", "TECH", "source", "NORMAL")));
        long docId = doc.path("id").asLong();
        call(post("/api/v1/workspace/folders/{id}/content", rootId), admin,
                json(Map.of("objectId", docId)));
        JsonNode content = call(get("/api/v1/workspace/folders/{id}/content", rootId), admin, null);
        assertThat(content.size()).isEqualTo(1);
        assertThat(content.get(0).path("objectId").asLong()).isEqualTo(docId);
        assertThat(content.get(0).path("code").asText()).startsWith("DOC-");

        // 5. 非空文件夹不可删除
        mockMvc.perform(delete("/api/v1/workspace/folders/{id}", rootId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 6. 内容移动到另一文件夹后再删除
        call(post("/api/v1/workspace/content/move"), admin,
                json(Map.of("objectId", docId, "fromFolderId", rootId, "toFolderId", childId)));
        assertThat(call(get("/api/v1/workspace/folders/{id}/content", rootId), admin, null).size()).isZero();
        assertThat(call(get("/api/v1/workspace/folders/{id}/content", childId), admin, null).size()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/workspace/folders/{id}", rootId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        assertThat(call(get("/api/v1/workspace/folders?space=MY"), admin, null).size()).isEqualTo(1);

        // 7. 个人空间隔离：他人读取我的文件夹内容 → 403
        mockMvc.perform(get("/api/v1/workspace/folders/{id}/content", childId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
        assertThat(call(get("/api/v1/workspace/folders?space=MY"), other, null).size()).isZero();

        // 8. 公共空间：同公司可见
        JsonNode publicFolder = call(post("/api/v1/workspace/folders"), admin,
                json(Map.of("spaceType", "PUBLIC", "name", "公司标准库")));
        assertThat(call(get("/api/v1/workspace/folders?space=PUBLIC"), other, null).size()).isEqualTo(1);
        assertThat(publicFolder.path("spaceType").asText()).isEqualTo("PUBLIC");

        // 9. 收藏与最近访问
        call(post("/api/v1/workspace/favorites/{objectId}", docId), admin, null);
        JsonNode favorites = call(get("/api/v1/workspace/favorites"), admin, null);
        assertThat(favorites.size()).isEqualTo(1);
        assertThat(favorites.get(0).path("objectId").asLong()).isEqualTo(docId);
        mockMvc.perform(delete("/api/v1/workspace/favorites/{objectId}", docId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        assertThat(call(get("/api/v1/workspace/favorites"), admin, null).size()).isZero();

        call(post("/api/v1/workspace/recent/{objectId}", docId), admin, null);
        JsonNode recent = call(get("/api/v1/workspace/recent?limit=10"), admin, null);
        assertThat(recent.size()).isEqualTo(1);
        assertThat(recent.get(0).path("objectId").asLong()).isEqualTo(docId);
        assertThat(call(get("/api/v1/workspace/recent?limit=10"), other, null).size()).isZero();
    }

    // ---------------------------------------------------------------- 工具

    private JsonNode findById(JsonNode array, long id) {
        for (JsonNode node : array) {
            if (node.path("id").asLong() == id) {
                return node;
            }
        }
        throw new AssertionError("未找到 id=" + id);
    }

    private String createBusinessUserAndLogin(String adminToken) throws Exception {
        JsonNode roles = call(get("/api/v1/system/roles"), adminToken, null);
        long businessRoleId = 0;
        for (JsonNode role : roles) {
            if ("BUSINESS".equals(role.path("code").asText())) {
                businessRoleId = role.path("id").asLong();
            }
        }
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
