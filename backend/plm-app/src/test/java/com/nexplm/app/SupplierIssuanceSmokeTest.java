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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * P8-5-3 数据发放冒烟：清单快照 + 打包导出（manifest + 主文件）+ 发出冻结 + 回执登记。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SupplierIssuanceSmokeTest {

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

    @Test
    void issuanceExportAndReceipt() throws Exception {
        String admin = login("admin", "Admin@12345");
        long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM sys_user WHERE username = 'admin'", Long.class);
        long itemId = insertItem(companyId, "ITEM-X1", "支架");
        long otherItemId = insertItem(companyId, "ITEM-X2", "衬套");

        JsonNode supplier = call(post("/api/v1/sup/suppliers"), admin,
                json(Map.of("name", "数据发放测试供应商")));
        long supplierId = supplier.path("id").asLong();

        // 1. 新建发放单：ISS- 取号、清单快照编码/名称/版本
        JsonNode draft = call(post("/api/v1/sup/issuances"), admin, json(body(supplierId,
                "设计资料首次发放", "DESIGN", List.of(item(itemId), item(otherItemId)))));
        long issuanceId = draft.path("id").asLong();
        assertThat(draft.path("code").asText()).startsWith("ISS-");
        assertThat(draft.path("status").asText()).isEqualTo("DRAFT");
        assertThat(draft.path("items")).hasSize(2);
        assertThat(draft.path("items").get(0).path("itemCode").asText()).isEqualTo("ITEM-X1");
        assertThat(draft.path("items").get(0).path("revision").asText()).isEqualTo("1");

        // 2. 参数校验：未知类型 / 空清单 / 零件不存在 / 零件重复 / 供应商不存在
        mockMvc.perform(post("/api/v1/sup/issuances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "标题", "WRONG", List.of(item(itemId))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/issuances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "标题", "DESIGN", List.of()))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/issuances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "标题", "DESIGN",
                                List.of(item(999999999L))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/issuances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "标题", "DESIGN",
                                List.of(item(itemId), item(itemId))))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/sup/issuances")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(999999999L, "标题", "DESIGN", List.of(item(itemId))))))
                .andExpect(status().isNotFound());

        // 3. 给一个零件挂主文件，导出应带上文件条目
        long fileId = upload("支架图纸.pdf", "%PDF-1.4\npdf-content".getBytes(StandardCharsets.UTF_8),
                admin, itemId);
        MvcResult zipResult = mockMvc.perform(
                        post("/api/v1/sup/issuances/{id}/export", issuanceId)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        List<String> entries = new ArrayList<>();
        String manifest = null;
        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(zipResult.getResponse().getContentAsByteArray()),
                StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                if ("manifest.csv".equals(entry.getName())) {
                    manifest = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        assertThat(entries).contains("manifest.csv");
        assertThat(entries).anyMatch(name -> name.startsWith("ITEM-X1_"));
        assertThat(manifest).contains("ITEM-X1").contains("ITEM-X2").contains("否");

        // 4. 草稿可改：清单整表替换
        JsonNode updated = call(put("/api/v1/sup/issuances/{id}", issuanceId), admin,
                json(body(supplierId, "设计资料首次发放(修订)", "DESIGN", List.of(item(itemId)))));
        assertThat(updated.path("items")).hasSize(1);
        assertThat(updated.path("title").asText()).contains("修订");

        // 5. 发出后冻结：再改/删/重复发出都 409
        JsonNode sent = call(post("/api/v1/sup/issuances/{id}/send", issuanceId), admin, null);
        assertThat(sent.path("status").asText()).isEqualTo("SENT");
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/send", issuanceId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/sup/issuances/{id}", issuanceId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body(supplierId, "改标题", "DESIGN", List.of(item(itemId))))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/sup/issuances/{id}", issuanceId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // 6. 回执登记：先上传文件（不绑对象）再登记；重复登记 409
        long receiptFileId = upload("回执确认单.pdf", "%PDF-1.4\nreceipt".getBytes(StandardCharsets.UTF_8),
                admin, null);
        JsonNode received = call(post("/api/v1/sup/issuances/{id}/receipt", issuanceId), admin,
                json(Map.of("fileId", receiptFileId, "fileName", "回执确认单.pdf",
                        "note", "已签收，版本确认无误")));
        assertThat(received.path("status").asText()).isEqualTo("RECEIVED");
        assertThat(received.path("receiptFileId").asLong()).isEqualTo(receiptFileId);
        assertThat(received.path("receiptFileName").asText()).isEqualTo("回执确认单.pdf");
        assertThat(received.path("receivedAt").asText()).isNotBlank();
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/receipt", issuanceId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fileId", receiptFileId))))
                .andExpect(status().isConflict());

        // 7. 回执文件不存在 → 404（对已发出的单登记）
        JsonNode second = call(post("/api/v1/sup/issuances"), admin, json(body(supplierId,
                "质量要求发放", "QUALITY", List.of(item(otherItemId)))));
        long secondId = second.path("id").asLong();
        call(post("/api/v1/sup/issuances/{id}/send", secondId), admin, null);
        mockMvc.perform(post("/api/v1/sup/issuances/{id}/receipt", secondId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fileId", 999999999L))))
                .andExpect(status().isNotFound());

        // 8. 列表/过滤/反查
        JsonNode all = call(get("/api/v1/sup/issuances?supplierId=" + supplierId), admin, null);
        assertThat(all.path("total").asInt()).isEqualTo(2);
        JsonNode receivedList = call(get("/api/v1/sup/issuances?supplierId=" + supplierId
                + "&status=RECEIVED"), admin, null);
        assertThat(receivedList.path("total").asInt()).isEqualTo(1);
        assertThat(call(get("/api/v1/sup/suppliers/{id}/issuances", supplierId), admin, null))
                .hasSize(2);
        mockMvc.perform(get("/api/v1/sup/issuances?type=WRONG")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // 9. 草稿可删
        JsonNode third = call(post("/api/v1/sup/issuances"), admin, json(body(supplierId,
                "临时发放", "OTHER", List.of(item(itemId)))));
        call(delete("/api/v1/sup/issuances/{id}", third.path("id").asLong()), admin, null);
        assertThat(call(get("/api/v1/sup/issuances?supplierId=" + supplierId), admin, null)
                .path("total").asInt()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ 工具

    private long insertItem(long companyId, String code, String name) {
        long id = System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO plm_object (id, object_type, master_id, code, name, company_id,
                                        lifecycle_state)
                VALUES (?, 'PdmItem', ?, ?, ?, ?, 'Draft')
                """, id, id, code, name, companyId);
        return id;
    }

    /** 上传文件（ownerObjectId 为空即不绑对象，与回执上传口径一致）。 */
    private long upload(String fileName, byte[] content, String token, Long ownerObjectId)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "application/octet-stream", content);
        var builder = multipart("/api/v1/files").file(file)
                .header("Authorization", "Bearer " + token);
        if (ownerObjectId != null) {
            builder.param("ownerObjectId", String.valueOf(ownerObjectId));
            builder.param("fileRole", "MAIN");
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        return body(result).path("data").path("fileId").asLong();
    }

    private Map<String, Object> body(long supplierId, String title, String type,
                                     List<Map<String, Object>> items) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("supplierId", supplierId);
        body.put("title", title);
        body.put("type", type);
        body.put("items", items);
        return body;
    }

    private Map<String, Object> item(long objectId) {
        return Map.of("objectId", objectId, "objectType", "PdmItem");
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
