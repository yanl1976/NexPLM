package com.nexplm.platform.doc.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocBatchDtos;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.iam.api.SecurityDtos;
import com.nexplm.platform.iam.service.AclService;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.service.ObjectAccessGuard;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 批量操作：打包下载（主文件）、批量授权。
 * <p>约定：单次最多 200 个对象；无文件的文档跳过（不生成空条目）；重名文件自动加序号。</p>
 */
@Service
public class DocBatchService {

    private static final int MAX_IDS = 200;
    private static final String ILLEGAL_NAME = "[\\\\/:*?\"<>|]";

    private final FileService fileService;
    private final AclService aclService;
    private final ObjectAccessGuard objectAccessGuard;

    public DocBatchService(FileService fileService, AclService aclService,
                           ObjectAccessGuard objectAccessGuard) {
        this.fileService = fileService;
        this.aclService = aclService;
        this.objectAccessGuard = objectAccessGuard;
    }

    /** 打包下载：每个文档取主文件（无主文件取第一个文件），条目名为「编码_文件名」。 */
    public byte[] exportZip(List<Long> ids) {
        List<Long> targets = normalizeIds(ids);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Set<String> used = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (Long id : targets) {
                // 不可见/不存在/非文档一律跳过：批量接口不能成为绕过数据范围的通道
                PlmObject object = objectAccessGuard.findType(id, DocService.OBJECT_TYPE).orElse(null);
                if (object == null) {
                    continue;
                }
                List<FileService.FileView> files = fileService.listByOwner(id, true);
                FileService.FileView picked = files.stream()
                        .filter(file -> FileService.ROLE_MAIN.equals(file.fileRole()))
                        .findFirst()
                        .orElse(files.isEmpty() ? null : files.get(0));
                if (picked == null) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(uniqueEntryName(used, object.getCode() + "_" + picked.fileName())));
                try (FileService.Opened opened = fileService.opened(picked.fileId())) {
                    opened.stream().transferTo(zip);
                }
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "打包下载失败：" + ex.getMessage());
        }
        return buffer.toByteArray();
    }

    /** 批量授权：逐条下发，返回每条成功/失败，避免一个失败中断整批。 */
    public List<DocBatchDtos.BatchGrantResult> grant(DocBatchDtos.BatchGrantReq req, Long operatorId) {
        List<Long> targets = normalizeIds(req.ids());
        List<DocBatchDtos.BatchGrantResult> results = new ArrayList<>();
        for (Long id : targets) {
            try {
                // 先过数据范围再做授权，避免对看不见的文档下发/覆盖 ACL
                objectAccessGuard.requireType(id, DocService.OBJECT_TYPE, "文档");
                aclService.grant(new SecurityDtos.AclGrantReq(id, req.actorType(), req.actorId(),
                        req.actions(), req.effect(), req.expireAt(), null), operatorId);
                results.add(new DocBatchDtos.BatchGrantResult(id, true, "已授权"));
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                results.add(new DocBatchDtos.BatchGrantResult(id, false, message));
            }
        }
        return results;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请选择要操作的文档");
        }
        List<Long> distinct = ids.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (distinct.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请选择要操作的文档");
        }
        if (distinct.size() > MAX_IDS) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "单次最多处理 " + MAX_IDS + " 个文档");
        }
        return distinct;
    }

    private String uniqueEntryName(Set<String> used, String raw) {
        String name = raw.replaceAll(ILLEGAL_NAME, "_");
        if (used.add(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String prefix = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int index = 2;
        while (!used.add(prefix + "(" + index + ")" + ext)) {
            index++;
        }
        return prefix + "(" + index + ")" + ext;
    }
}
