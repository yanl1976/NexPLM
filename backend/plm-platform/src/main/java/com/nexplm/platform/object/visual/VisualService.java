package com.nexplm.platform.object.visual;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.file.service.FileService;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.service.ObjectAccessGuard;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 在线可视化：判断对象主文件该用哪种查看器，并把旧系统的「扩展名 → 查看器/转换命令」映射迁移过来。
 *
 * <p>映射来源：旧系统 {@code DistributedFileServer/FileServer/Config/FileServiceConfig.xml} 的
 * {@code BrowseType} 节点（{@code ViewTool} + {@code TransCommand} + {@code TransCheck}）：</p>
 * <ul>
 *   <li>{@code ViewTool=SView} + {@code TransCommand=FileToSvl} + {@code TransCheck=*.svlx}
 *       —— 三维模型交给 SView（HTML5 插件，已迁移到前端 {@code public/sview}）；</li>
 *   <li>{@code ViewTool=ImageView} + {@code TransCommand=FileToPDF} + {@code TransCheck=*.pdf}
 *       —— 二维图纸与 Office 先转 PDF 再查看。</li>
 * </ul>
 *
 * <p>转换本身不在 Java 里做：旧系统由 Windows CAD 工作站上的 HoteamsoftCommandService 执行，
 * 新系统沿用「转换代理」架构（docs/01 ADR-10），本服务只负责判定与提示。</p>
 */
@Service
public class VisualService {

    public static final String TYPE_DOC = "PdmDoc";
    public static final String TYPE_ITEM = "PdmItem";

    /** 可直接用 SView 打开的三维轻量化文件。 */
    private static final Set<String> SVIEW_READY = Set.of("svlx", "svl");

    /** 需要 FileToSvl 转换的三维源文件（旧系统 BrowseType 中 ViewTool=SView 的 Ext）。 */
    private static final Set<String> SVIEW_SOURCE = Set.of("par", "asm", "iam", "ipt", "prt",
            "sldprt", "sldasm", "catproduct", "catpart", "casm", "cprt", "stp", "step");

    /**
     * 浏览器端可直接渲染的 Word（.docx）：前端用 docx-preview 解析到 DOM，不必走转换代理。
     * 注意 .doc（97-2003 二进制格式）前端库不支持，仍归入 {@link #TO_PDF}。
     */
    private static final Set<String> DOCX = Set.of("docx");

    /** 浏览器端可直接渲染的电子表格：前端用 SheetJS 解析成多个 sheet 以表格展示。 */
    private static final Set<String> SHEET = Set.of("xlsx", "xlsm", "xls");

    /** 需要 FileToPDF 转换的二维图纸与旧版 Office（旧系统 BrowseType 中 ViewTool=ImageView 的 Ext）。 */
    private static final Set<String> TO_PDF = Set.of("dwg", "drw", "idw", "cdrw", "slddrw", "catdrawing",
            "dft", "exb", "elk", "doc", "ppt", "pptx", "xps", "report");

    /**
     * 浏览器原生可显示的位图。TIFF 不在其中：主流浏览器无法渲染，且 ImagePreviewConverter
     * 也只做直通（不转 PNG），放进来会让前端拿到打不开的流 → 归入 NONE「下载后本地打开」，
     * 待接入转换代理后按需转 PNG（EXP-20261108-04 第 6 条）。
     */
    private static final Set<String> IMAGE = Set.of("jpg", "jpeg", "png", "gif", "bmp", "ico", "webp");

    /** 纯文本：浏览器按文本渲染（前端做 GBK/UTF-8 编码自适应，避免国内 csv/txt 乱码）。 */
    private static final Set<String> PLAIN_TEXT = Set.of("txt", "md", "csv", "log", "json", "xml", "yml", "yaml");

    private static final String COMMAND_TO_SVL = "FileToSvl";
    private static final String COMMAND_TO_PDF = "FileToPDF";

    private final ObjectAccessGuard objectAccessGuard;
    private final FileService fileService;

    public VisualService(ObjectAccessGuard objectAccessGuard, FileService fileService) {
        this.objectAccessGuard = objectAccessGuard;
        this.fileService = fileService;
    }

    /** 文档的在线可视化信息（按主文件判定）。 */
    public VisualDtos.VisualInfo forDoc(Long docId) {
        return forDoc(docId, null);
    }

    /**
     * 文档的在线可视化信息。
     *
     * @param fileId 指定要查看的文件；为空时按主文件判定。文档详情页文件表里逐行「预览」
     *               必须带上该行的 fileId，否则主文件类型会盖住用户真正要看的那份附件。
     */
    public VisualDtos.VisualInfo forDoc(Long docId, Long fileId) {
        return resolve(docId, TYPE_DOC, "文档", fileId);
    }

    /** 零部件的在线可视化信息（零部件挂设计文档/三维模型，与文档同一套判定）。 */
    public VisualDtos.VisualInfo forItem(Long itemId) {
        return resolve(itemId, TYPE_ITEM, "零部件", null);
    }

    private VisualDtos.VisualInfo resolve(Long objectId, String objectType, String label, Long pinnedFileId) {
        PlmObject object = objectAccessGuard.requireType(objectId, objectType, label);
        // 预览产物（file_role=VIEW，如 preview-xxx.pdf）是 PreviewService 生成的派生物，
        // 不参与「用什么查看器」的判定：否则文档生成过预览后，判定会从原始格式漂移到产物格式，
        // 且派生物会被当成「可切换查看的文件」暴露给前端（EXP-20261108-04 第 1 条）。
        List<FileService.FileView> latest = fileService.listByOwner(objectId, true).stream()
                .filter(file -> !FileService.ROLE_VIEW.equals(file.fileRole()))
                .toList();
        List<VisualDtos.VisualFile> files = latest.stream()
                .sorted(Comparator.comparing((FileService.FileView f) -> !FileService.ROLE_MAIN.equals(f.fileRole()))
                        .thenComparing(f -> f.ext() == null ? "" : f.ext()))
                .map(f -> new VisualDtos.VisualFile(f.fileId(), f.fileName(), lower(f.ext()), f.fileRole(), f.size()))
                .toList();

        if (files.isEmpty()) {
            return info(object, objectType, VisualDtos.Engine.NONE, false, null,
                    "该对象还没有文件，无法预览", null, files);
        }

        // 指定了文件时以该文件为准：文件表逐行「预览」的场景，不能被主文件的类型盖住
        VisualDtos.VisualFile pinned = null;
        if (pinnedFileId != null) {
            pinned = files.stream()
                    .filter(f -> pinnedFileId.equals(f.fileId()))
                    .findFirst()
                    .orElseThrow(() -> BizException.of(ErrorCode.COMMON_NOT_FOUND,
                            "文件不属于该对象或不存在：" + pinnedFileId));
        }

        // 1) 已有轻量化产物（转换过一次后再看直接命中，不必再走转换代理）。
        //    未指定文件时才做全局命中，否则会把用户要看的那份文件换成别的产物。
        VisualDtos.VisualFile ready = pinned != null
                ? (SVIEW_READY.contains(lower(pinned.ext())) ? pinned : null)
                : files.stream().filter(f -> SVIEW_READY.contains(lower(f.ext()))).findFirst().orElse(null);
        if (ready != null) {
            return info(object, objectType, VisualDtos.Engine.SVIEW, false, null,
                    "三维轻量化模型（SView）", ready, files);
        }

        // 2) 没有产物：指定文件优先，否则按主文件（缺失则取第一个）判定原始格式
        VisualDtos.VisualFile primary = pinned != null ? pinned
                : files.stream()
                        .filter(f -> FileService.ROLE_MAIN.equals(f.fileRole()))
                        .findFirst()
                        .orElse(files.get(0));
        String ext = primary.ext() == null ? "" : primary.ext();

        if (SVIEW_SOURCE.contains(ext)) {
            return info(object, objectType, VisualDtos.Engine.SVIEW, true, COMMAND_TO_SVL,
                    "主文件为 ." + ext + "，需转换代理生成 .svlx 后在线查看", primary, files);
        }
        if ("pdf".equals(ext)) {
            return info(object, objectType, VisualDtos.Engine.PDF, false, null,
                    "PDF 文档（浏览器内置查看器）", primary, files);
        }
        if (IMAGE.contains(ext)) {
            return info(object, objectType, VisualDtos.Engine.IMAGE, false, null,
                    "图片（." + ext + "）", primary, files);
        }
        if (DOCX.contains(ext)) {
            return info(object, objectType, VisualDtos.Engine.DOCX, false, null,
                    "Word 文档（." + ext + "），浏览器内直接渲染", primary, files);
        }
        if (SHEET.contains(ext)) {
            return info(object, objectType, VisualDtos.Engine.SHEET, false, null,
                    "电子表格（." + ext + "），浏览器内直接渲染", primary, files);
        }
        if (PLAIN_TEXT.contains(ext)) {
            return info(object, objectType, VisualDtos.Engine.TEXT, false, null,
                    "纯文本（." + ext + "）", primary, files);
        }
        if (TO_PDF.contains(ext)) {
            return info(object, objectType, VisualDtos.Engine.PDF, true, COMMAND_TO_PDF,
                    "主文件为 ." + ext + "，需转换代理生成 PDF 后在线查看", primary, files);
        }
        return info(object, objectType, VisualDtos.Engine.NONE, false, null,
                "暂不支持在线查看 ." + ext + "（可下载后本地打开）", primary, files);
    }

    private VisualDtos.VisualInfo info(PlmObject object, String objectType, String engine, boolean needsConvert,
                                       String convertCommand, String hint, VisualDtos.VisualFile file,
                                       List<VisualDtos.VisualFile> files) {
        return new VisualDtos.VisualInfo(objectType, object.getId(), object.getCode(), object.getName(),
                engine, needsConvert, convertCommand, hint, file, files);
    }

    private static String lower(String ext) {
        return ext == null ? null : ext.toLowerCase(Locale.ROOT);
    }
}
