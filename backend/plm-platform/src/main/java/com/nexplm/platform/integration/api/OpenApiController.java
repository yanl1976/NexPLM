package com.nexplm.platform.integration.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.domain.FileObject;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.integration.service.ApiKeyService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 对外开放接口（P9-3）：只读对象 + 文件上传下载。
 *
 * <p>刻意只开放"读对象 + 文件传输"：写对象要经过元数据校验、生命周期、权限与审计，
 * 直接给外部开一个 {@code createObject} 等于把内部规则绕过去了。
 * 需要创建对象的外部系统应当走集成任务（P9-1 的下发/回传），而不是开放写接口。</p>
 *
 * <p>所有查询都强制带 {@code company_id}（来自 API Key 所属公司），
 * 外部客户端看不到别的公司的数据。</p>
 */
@Tag(name = "开放平台-对外接口")
@RestController
@RequestMapping("/api/v1/open")
public class OpenApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final FileService fileService;
    private final ApiKeyService apiKeyService;

    public OpenApiController(JdbcTemplate jdbcTemplate, FileService fileService,
                             ApiKeyService apiKeyService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileService = fileService;
        this.apiKeyService = apiKeyService;
    }

    @Operation(summary = "当前 API 客户端信息")
    @GetMapping("/whoami")
    @PreAuthorize("isAuthenticated()")
    public Result<OpenApiDtos.WhoAmI> whoAmI() {
        var principal = CurrentUser.require();
        List<String> scopes = principal.permissions().stream()
                .filter(code -> code.startsWith("open."))
                .map(code -> code.substring("open.".length()))
                .sorted()
                .toList();
        String clientCode = principal.username() != null
                && principal.username().startsWith("apikey:")
                ? principal.username().substring("apikey:".length()) : principal.username();
        int rateLimit = 60;
        ApiKeyService.ApiClient client = apiKeyService.byCode(principal.companyId(), clientCode);
        if (client != null) {
            rateLimit = client.rateLimitPerMin();
        }
        return Result.ok(new OpenApiDtos.WhoAmI(clientCode, principal.displayName(), scopes,
                rateLimit));
    }

    @Operation(summary = "对象列表（只读，限定本公司）")
    @GetMapping("/objects")
    @PreAuthorize("hasAuthority('open.object.read')")
    public Result<PageResult<OpenApiDtos.OpenObjectRow>> objects(
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long companyId = CurrentUser.companyId();
        StringBuilder where = new StringBuilder(
                " WHERE company_id = ? AND is_latest = true AND deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (objectType != null && !objectType.isBlank()) {
            where.append(" AND object_type = ?");
            args.add(objectType.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM plm_object" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<OpenApiDtos.OpenObjectRow> items = jdbcTemplate.query("""
                        SELECT id, object_type, code, name, revision, lifecycle_state
                          FROM plm_object
                        """ + where + " ORDER BY object_type, code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new OpenApiDtos.OpenObjectRow(rs.getLong("id"),
                        rs.getString("object_type"), rs.getString("code"), rs.getString("name"),
                        rs.getInt("revision"), rs.getString("lifecycle_state")),
                pageArgs.toArray());
        return Result.ok(PageResult.of(items, total == null ? 0 : total, pageNo, pageSize));
    }

    @Operation(summary = "对象详情（限定本公司）")
    @GetMapping("/objects/{id}")
    @PreAuthorize("hasAuthority('open.object.read')")
    public Result<OpenApiDtos.OpenObjectDetail> object(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, object_type, code, name, revision, iteration, lifecycle_state, ext_json
                  FROM plm_object WHERE id = ? AND company_id = ? AND deleted = false
                """, id, CurrentUser.companyId());
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "对象不存在：" + id);
        }
        Map<String, Object> row = rows.get(0);
        return Result.ok(new OpenApiDtos.OpenObjectDetail(id, str(row.get("object_type")),
                str(row.get("code")), str(row.get("name")), intOf(row.get("revision")),
                intOf(row.get("iteration")), str(row.get("lifecycle_state")),
                str(row.get("ext_json"))));
    }

    @Operation(summary = "文件下载（按 fileId）")
    @GetMapping("/files/{fileId}/content")
    @PreAuthorize("hasAuthority('open.file.read')")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        FileObject file = fileService.meta(fileId);
        InputStream stream = fileService.opened(fileId).stream();
        String name = "file-" + fileId + (file.getExt() == null ? "" : "." + file.getExt());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, file.getMime() == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getMime())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(file.getSize() == null ? 0L : file.getSize())
                .body(new InputStreamResource(stream));
    }

    @Operation(summary = "文件上传（不绑定对象，返回 fileId）")
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('open.file.write')")
    public Result<FileService.UploadResult> upload(@RequestPart("file") MultipartFile file,
                                                   @RequestParam(required = false) String fileName) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        String name = fileName == null || fileName.isBlank()
                ? file.getOriginalFilename() : fileName;
        try (InputStream in = file.getInputStream()) {
            // 归属用户固定 0：API 客户端不是真实用户，审计里按 0 明确来源
            return Result.ok(fileService.upload(null, FileService.ROLE_ATTACHMENT, null, name,
                    file.getContentType(), in, 0L));
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR,
                    "读取上传内容失败：" + ex.getMessage());
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
