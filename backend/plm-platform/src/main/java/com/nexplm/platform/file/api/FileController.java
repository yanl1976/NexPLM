package com.nexplm.platform.file.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.domain.FileLink;
import com.nexplm.platform.file.domain.FileObject;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.file.service.MultipartUploadService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

/**
 * 文件仓接口：上传（去重/秒传）、下载（支持 Range）、对象附件列表、绑定与解绑。
 */
@Tag(name = "平台-文件仓")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    /** 下载文件名上限：够放中文长名，又不至于把响应头撑爆。 */
    private static final int MAX_DOWNLOAD_NAME = 200;

    private final FileService fileService;
    private final MultipartUploadService multipartUploadService;

    public FileController(FileService fileService, MultipartUploadService multipartUploadService) {
        this.fileService = fileService;
        this.multipartUploadService = multipartUploadService;
    }

    public record LinkReq(@NotNull(message = "归属对象不能为空") Long ownerObjectId,
                          @NotBlank(message = "文件名不能为空") String fileName,
                          String fileRole,
                          Integer sort) {
    }

    @Operation(summary = "上传文件（内容寻址去重，可选绑定到对象）")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('file.upload')")
    @Audit(module = "FILE", action = "FILE_UPLOAD", objectType = "FileObject")
    public Result<FileService.UploadResult> upload(@RequestPart("file") MultipartFile file,
                                                   @RequestParam(value = "ownerObjectId", required = false) Long ownerObjectId,
                                                   @RequestParam(value = "fileRole", required = false) String fileRole,
                                                   @RequestParam(value = "sort", required = false) Integer sort,
                                                   @RequestParam(value = "fileName", required = false) String fileName) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.FILE_UPLOAD_EMPTY);
        }
        String name = (fileName == null || fileName.isBlank()) ? file.getOriginalFilename() : fileName;
        try (InputStream in = file.getInputStream()) {
            return Result.ok(fileService.upload(ownerObjectId, fileRole, sort, name,
                    file.getContentType(), in, CurrentUser.userId()));
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "读取上传内容失败：" + ex.getMessage());
        }
    }

    @Operation(summary = "秒传预检（按 sha256 判断是否已存在）")
    @GetMapping("/precheck")
    @PreAuthorize("hasAuthority('file.upload')")
    public Result<FileService.PrecheckResult> precheck(@RequestParam("sha256") String sha256) {
        return Result.ok(fileService.precheck(sha256));
    }

    @Operation(summary = "文件元数据")
    @GetMapping("/{fileId}")
    @PreAuthorize("hasAuthority('file.download')")
    public Result<FileObject> meta(@PathVariable Long fileId) {
        return Result.ok(fileService.meta(fileId));
    }

    @Operation(summary = "对象下的文件列表")
    @GetMapping
    @PreAuthorize("hasAuthority('file.download')")
    public Result<List<FileService.FileView>> list(@RequestParam("ownerObjectId") Long ownerObjectId,
                                                   @RequestParam(value = "latestOnly", defaultValue = "true") boolean latestOnly) {
        return Result.ok(fileService.listByOwner(ownerObjectId, latestOnly));
    }

    @Operation(summary = "下载文件内容（支持 Range 断点续传）")
    @GetMapping("/{fileId}/content")
    @PreAuthorize("hasAuthority('file.download')")
    @Audit(module = "FILE", action = "FILE_DOWNLOAD", objectType = "FileObject", recordArgs = false)
    public ResponseEntity<Resource> download(
            @PathVariable Long fileId,
            @RequestParam(value = "name", required = false) String name,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        FileObject file = fileService.meta(fileId);
        long total = file.getSize() == null ? 0L : file.getSize();

        long start = 0L;
        long length = total;
        boolean partial = false;
        if (range != null && !range.isBlank() && total > 0) {
            HttpRange httpRange;
            try {
                List<HttpRange> ranges = HttpRange.parseRanges(range);
                if (ranges.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
                }
                httpRange = ranges.get(0);
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
            start = httpRange.getRangeStart(total);
            length = httpRange.getRangeEnd(total) - start + 1;
            if (start >= total || length <= 0) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
            partial = true;
        }

        InputStream stream = fileService.opened(fileId).stream();
        try {
            skipFully(stream, start);
        } catch (IOException ex) {
            closeQuietly(stream);
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "读取文件内容失败：" + ex.getMessage());
        }

        String downloadName = safeFileName(name);
        if (downloadName == null) {
            downloadName = "file-" + file.getSha256().substring(0, 12)
                    + (file.getExt() == null ? "" : "." + file.getExt());
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(partial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, file.getMime() == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getMime())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(downloadName))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(length);
        if (partial) {
            builder.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + (start + length - 1) + "/" + total);
        }
        return builder.body(new InputStreamResource(new LimitedInputStream(stream, length)));
    }

    @Operation(summary = "获取下载地址（对象存储预签名直链，本地存储回退为流式下载）")
    @GetMapping("/{fileId}/url")
    @PreAuthorize("hasAuthority('file.download')")
    public Result<FileService.DownloadUrl> downloadUrl(
            @PathVariable Long fileId,
            @RequestParam(value = "ttlSeconds", required = false) Long ttlSeconds) {
        long ttl = ttlSeconds == null ? 3600L : Math.max(60L, Math.min(ttlSeconds, 604800L));
        return Result.ok(fileService.downloadUrl(fileId, Duration.ofSeconds(ttl)));
    }

    @Operation(summary = "创建分片上传会话（大文件断点续传）")
    @PostMapping("/uploads")
    @PreAuthorize("hasAuthority('file.upload')")
    public Result<MultipartUploadService.CreateResult> createUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("totalSize") long totalSize,
            @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
            @RequestParam(value = "mime", required = false) String mime,
            @RequestParam(value = "ownerObjectId", required = false) Long ownerObjectId,
            @RequestParam(value = "fileRole", required = false) String fileRole,
            @RequestParam(value = "sort", required = false) Integer sort) {
        return Result.ok(multipartUploadService.create(fileName, totalSize, chunkSize, mime,
                ownerObjectId, fileRole, sort, CurrentUser.userId()));
    }

    @Operation(summary = "上传单个分片（重复上传同一序号为幂等覆盖）")
    @PutMapping("/uploads/{uploadId}/chunks/{index}")
    @PreAuthorize("hasAuthority('file.upload')")
    public Result<Void> uploadChunk(@PathVariable String uploadId,
                                    @PathVariable int index,
                                    HttpServletRequest request) {
        long size = request.getContentLengthLong();
        Path spooled = null;
        try (InputStream raw = request.getInputStream()) {
            InputStream in = raw;
            if (size < 0) {
                // 分块传输（chunked）时无 Content-Length，先落暂存再上传
                spooled = Files.createTempFile("plm-chunk-", ".tmp");
                Files.copy(raw, spooled, StandardCopyOption.REPLACE_EXISTING);
                size = Files.size(spooled);
                in = Files.newInputStream(spooled);
            }
            try {
                multipartUploadService.saveChunk(uploadId, index, in, size);
            } finally {
                if (in != raw) {
                    in.close();
                }
            }
            return Result.ok();
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "读取分片失败：" + ex.getMessage());
        } finally {
            if (spooled != null) {
                try {
                    Files.deleteIfExists(spooled);
                } catch (IOException ignored) {
                    // 暂存清理失败不影响业务
                }
            }
        }
    }

    @Operation(summary = "合并分片并提交为正式文件")
    @PostMapping("/uploads/{uploadId}/complete")
    @PreAuthorize("hasAuthority('file.upload')")
    @Audit(module = "FILE", action = "FILE_UPLOAD_COMPLETE", objectType = "FileObject")
    public Result<FileService.UploadResult> completeUpload(@PathVariable String uploadId) {
        return Result.ok(multipartUploadService.complete(uploadId, CurrentUser.userId()));
    }

    @Operation(summary = "查询分片上传会话（含已收分片索引，用于续传）")
    @GetMapping("/uploads/{uploadId}")
    @PreAuthorize("hasAuthority('file.upload')")
    public Result<MultipartUploadService.SessionView> uploadSession(@PathVariable String uploadId) {
        return Result.ok(multipartUploadService.view(uploadId));
    }

    @Operation(summary = "取消分片上传会话并清理分片")
    @DeleteMapping("/uploads/{uploadId}")
    @PreAuthorize("hasAuthority('file.upload')")
    public Result<Void> abortUpload(@PathVariable String uploadId) {
        multipartUploadService.abort(uploadId);
        return Result.ok();
    }

    @Operation(summary = "把已有物理文件绑定到对象（秒传贴链接）")
    @PostMapping("/{fileId}/links")
    @PreAuthorize("hasAuthority('file.upload')")
    @Audit(module = "FILE", action = "FILE_LINK", objectType = "FileLink")
    public Result<FileLink> link(@PathVariable Long fileId, @Valid @RequestBody LinkReq req) {
        return Result.ok(fileService.attach(req.ownerObjectId(), fileId, req.fileName(),
                req.fileRole(), req.sort(), CurrentUser.userId()));
    }

    @Operation(summary = "解除对象与文件的关联")
    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('file.upload')")
    @Audit(module = "FILE", action = "FILE_UNLINK", objectType = "FileLink")
    public Result<Void> unlink(@PathVariable Long linkId) {
        fileService.detach(linkId);
        return Result.ok();
    }

    // ---------------------------------------------------------------- 内部工具

    private void skipFully(InputStream in, long offset) throws IOException {
        long remaining = offset;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                if (in.read() < 0) {
                    throw new IOException("请求区间超出文件大小");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // 关闭失败无需向上传播
        }
    }

    /** 限长输入流：Range 下载时只暴露区间内的字节。 */
    private static final class LimitedInputStream extends FilterInputStream {

        private long remaining;

        private LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public long skip(long count) throws IOException {
            long skipped = super.skip(Math.min(count, remaining));
            if (skipped > 0) {
                remaining -= skipped;
            }
            return skipped;
        }
    }

    /**
     * 下载文件名净化（P10-5 安全加固）。
     *
     * <p>{@code name} 来自查询参数，是用户可控输入，直接放进 Content-Disposition 有三个问题：
     * 客户端按 {@code ../../x} 落盘会穿越目录、超长文件名会撑大响应头、控制字符会污染头部。
     * 这里只取最后一段、去控制字符并限长；净化后为空则回退到按 sha256 生成的名字。</p>
     */
    private String safeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim().replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            return null;
        }
        if (name.length() > MAX_DOWNLOAD_NAME) {
            int dot = name.lastIndexOf('.');
            String ext = (dot > 0 && name.length() - dot <= 16) ? name.substring(dot) : "";
            name = name.substring(0, Math.max(1, MAX_DOWNLOAD_NAME - ext.length())) + ext;
        }
        return name;
    }

    /** 下载文件名统一按 RFC 5987 编码，避免中文乱码。 */
    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;
    }
}
