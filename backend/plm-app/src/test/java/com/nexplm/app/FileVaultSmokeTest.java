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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件仓冒烟测试：内容寻址去重（秒传）、版本递增、Range 下载、分片上传合并、引用计数回收。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileVaultSmokeTest {

    private static final EmbeddedPostgres POSTGRES;
    private static final Path STORAGE_ROOT;

    private static final long OWNER_ID = 910001L;
    private static final int CHUNK_SIZE = 1024 * 1024;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
            STORAGE_ROOT = Files.createTempDirectory("nexplm-files-");
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
    void uploadDedupRangeDownloadAndChunkedMerge() throws Exception {
        String token = login();
        long adminId = currentUserId(token);
        createOwnerObject(adminId);

        byte[] content = "NexPLM 文件仓冒烟测试内容".getBytes(StandardCharsets.UTF_8);
        String sha = sha256Hex(content);

        // 1. 首次上传：真实落盘，非秒传
        JsonNode first = upload(token, content, "spec.txt", OWNER_ID);
        assertThat(first.path("dedup").asBoolean()).isFalse();
        assertThat(first.path("sha256").asText()).isEqualTo(sha);
        assertThat(first.path("size").asLong()).isEqualTo(content.length);
        assertThat(first.path("ext").asText()).isEqualTo("txt");
        assertThat(first.path("versionNo").asInt()).isEqualTo(1);
        long fileId = first.path("fileId").asLong();

        // 扫描钩子已执行（默认 noop → SKIPPED）
        assertThat(jdbcTemplate.queryForObject(
                "SELECT scan_status FROM file_object WHERE id = ?", String.class, fileId)).isEqualTo("SKIPPED");

        // 物理文件按内容寻址布局落盘
        Path stored = STORAGE_ROOT.resolve(sha.substring(0, 2)).resolve(sha.substring(2, 4)).resolve(sha);
        assertThat(Files.exists(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).isEqualTo(content);

        // 2. 同内容二次上传：命中秒传，复用同一物理文件，版本号递增
        JsonNode second = upload(token, content, "spec-副本.txt", OWNER_ID);
        assertThat(second.path("dedup").asBoolean()).isTrue();
        assertThat(second.path("fileId").asLong()).isEqualTo(fileId);
        assertThat(second.path("versionNo").asInt()).isEqualTo(2);
        assertThat(refCount(fileId)).isEqualTo(2);

        // 3. 秒传预检
        JsonNode precheck = call(get("/api/v1/files/precheck").param("sha256", sha), token);
        assertThat(precheck.path("exists").asBoolean()).isTrue();
        assertThat(precheck.path("fileId").asLong()).isEqualTo(fileId);

        // 4. 全量下载
        assertThat(download(token, fileId)).isEqualTo(content);

        // 5. Range 下载：只回前 6 字节，状态 206
        MvcResult ranged = mockMvc.perform(get("/api/v1/files/{id}/content", fileId)
                        .header("Authorization", "Bearer " + token)
                        .header("Range", "bytes=0-5"))
                .andExpect(status().isPartialContent())
                .andReturn();
        assertThat(ranged.getResponse().getContentAsByteArray())
                .isEqualTo(Arrays.copyOfRange(content, 0, 6));
        assertThat(ranged.getResponse().getHeader("Content-Range"))
                .isEqualTo("bytes 0-5/" + content.length);

        // 6. 列表：同角色仅一条最新，全量可见两条历史
        JsonNode latest = call(get("/api/v1/files").param("ownerObjectId", String.valueOf(OWNER_ID)), token);
        assertThat(latest.size()).isEqualTo(1);
        assertThat(latest.get(0).path("fileName").asText()).isEqualTo("spec-副本.txt");
        JsonNode all = call(get("/api/v1/files")
                .param("ownerObjectId", String.valueOf(OWNER_ID))
                .param("latestOnly", "false"), token);
        assertThat(all.size()).isEqualTo(2);

        // 7. 分片上传：3MB+17 字节按 1MB 切片，合并后内容一致
        byte[] big = new byte[3 * CHUNK_SIZE + 17];
        new Random(7).nextBytes(big);
        JsonNode session = call(post("/api/v1/files/uploads")
                .param("fileName", "big.bin")
                .param("totalSize", String.valueOf(big.length))
                .param("chunkSize", String.valueOf(CHUNK_SIZE))
                .param("ownerObjectId", String.valueOf(OWNER_ID)), token);
        String uploadId = session.path("uploadId").asText();
        assertThat(session.path("chunkTotal").asInt()).isEqualTo(4);

        for (int index = 0; index < 4; index++) {
            int from = index * CHUNK_SIZE;
            int to = Math.min(big.length, from + CHUNK_SIZE);
            mockMvc.perform(put("/api/v1/files/uploads/{uploadId}/chunks/{index}", uploadId, index)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .content(Arrays.copyOfRange(big, from, to)))
                    .andExpect(status().isOk());
        }
        JsonNode merged = call(post("/api/v1/files/uploads/{uploadId}/complete", uploadId), token);
        assertThat(merged.path("size").asLong()).isEqualTo(big.length);
        assertThat(merged.path("sha256").asText()).isEqualTo(sha256Hex(big));
        long bigFileId = merged.path("fileId").asLong();
        assertThat(download(token, bigFileId)).isEqualTo(big);

        // 合并后分片已清理，会话标记完成
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM file_upload_chunk WHERE session_id = ?", Integer.class, uploadId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM file_upload_session WHERE id = ?", String.class, uploadId)).isEqualTo("MERGED");

        // 8. 解绑：引用计数递减，归零后物理文件与元数据一起回收
        long secondLinkId = all.get(0).path("linkId").asLong();
        long firstLinkId = all.get(1).path("linkId").asLong();
        mockMvc.perform(delete("/api/v1/files/links/{id}", secondLinkId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(refCount(fileId)).isEqualTo(1);
        assertThat(Files.exists(stored)).isTrue();

        mockMvc.perform(delete("/api/v1/files/links/{id}", firstLinkId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM file_object WHERE id = ?", Integer.class, fileId)).isZero();
        assertThat(Files.exists(stored)).isFalse();
    }

    @Test
    void emptyUploadAndUnknownOwnerAreRejected() throws Exception {
        String token = login();

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)))
                        .param("ownerObjectId", "999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- 测试工具

    /**
     * 等保自查整改 ①：可执行/服务端脚本类扩展名在上传入口就被拒绝，
     * 常见 PLM 格式（step/dwg/docx…）不受影响。
     */
    @Test
    void dangerousExtensionsAreRejected() throws Exception {
        String token = login();
        createOwnerObject(currentUserId(token));
        byte[] content = "MZ-test".getBytes(StandardCharsets.UTF_8);

        for (String name : new String[]{"malware.exe", "shell.jsp", "page.html"}) {
            mockMvc.perform(multipart("/api/v1/files")
                            .file(new MockMultipartFile("file", name, "application/octet-stream", content))
                            .param("ownerObjectId", String.valueOf(OWNER_ID))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        // 分片上传在创建会话时就该被拒（不必等传完）
        mockMvc.perform(post("/api/v1/files/uploads")
                        .param("fileName", "payload.ps1")
                        .param("totalSize", "1024")
                        .param("ownerObjectId", String.valueOf(OWNER_ID))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        JsonNode ok = upload(token, content, "model.step", OWNER_ID);
        assertThat(ok.path("fileId").asLong()).isPositive();
    }

    /**
     * 等保整改 ① 的另一半：扩展名有确定魔数时，内容必须相符。
     * 覆盖：假 PDF（纯文本）被拒、真 PDF 通过、无签名格式（.step）不参与魔数校验。
     */
    @Test
    void fileContentMustMatchExtension() throws Exception {
        String token = login();
        // 独立 owner：本用例会成功上传 2 个文件，共用 OWNER_ID 会污染其它用例的"最新链接"断言
        long owner = OWNER_ID + 7;
        createOwnerObject(currentUserId(token), owner);

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "fake.pdf", "application/pdf",
                                "hello-not-a-pdf".getBytes(StandardCharsets.UTF_8)))
                        .param("ownerObjectId", String.valueOf(owner))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        byte[] pdf = "%PDF-1.4\n% test\n".getBytes(StandardCharsets.US_ASCII);
        JsonNode ok = upload(token, pdf, "real.pdf", owner);
        assertThat(ok.path("fileId").asLong()).isPositive();

        // 没有统一签名的格式不参与校验（否则会误杀）
        JsonNode step = upload(token, "ISO-10303-21;".getBytes(StandardCharsets.UTF_8), "part.step", owner);
        assertThat(step.path("fileId").asLong()).isPositive();
    }

    private JsonNode upload(String token, byte[] content, String fileName, long ownerObjectId) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", fileName, "text/plain", content))
                        .param("ownerObjectId", String.valueOf(ownerObjectId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data");
    }

    private byte[] download(String token, long fileId) throws Exception {
        return mockMvc.perform(get("/api/v1/files/{id}/content", fileId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private void createOwnerObject(long adminId) {
        createOwnerObject(adminId, OWNER_ID);
    }

    /** 每个用例用自己的 owner，避免"最新链接数"这类断言被其它用例上传的文件污染。 */
    private void createOwnerObject(long adminId, long ownerId) {
        jdbcTemplate.update("""
                        INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                                                lifecycle_state, revision, iteration, is_latest, version, deleted, ext_json)
                        VALUES (?, 'PdmDoc', ?, ?, '文件仓测试文档', 1, ?, 'Draft',
                                1, 1, true, 0, false, '{}'::jsonb)
                        ON CONFLICT DO NOTHING
                        """, ownerId, ownerId, "DOC-" + (9100 + (ownerId % 100)), adminId);
    }

    private int refCount(long fileId) {
        return jdbcTemplate.queryForObject("SELECT ref_count FROM file_object WHERE id = ?", Integer.class, fileId);
    }

    private long currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return body(result).path("data").path("id").asLong();
    }

    private String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "Admin@12345"))))
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
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
