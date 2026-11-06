package com.nexplm.platform.object.visual;

import java.util.List;

/** 可视化（在线查看）接口的返回结构。 */
public final class VisualDtos {

    private VisualDtos() {
    }

    /** 可视化引擎：三维走 SView（旧系统插件迁移），二维/Office 走 PDF，位图走 IMAGE，纯文本走 TEXT。 */
    public static final class Engine {
        public static final String SVIEW = "SVIEW";
        public static final String PDF = "PDF";
        public static final String IMAGE = "IMAGE";
        public static final String TEXT = "TEXT";
        public static final String NONE = "NONE";

        private Engine() {
        }
    }

    /** 候选文件（对象下最新版本的文件，按角色排序）。 */
    public record VisualFile(Long fileId, String fileName, String ext, String fileRole, Long size) {
    }

    /**
     * 可视化信息。
     *
     * @param engine         见 {@link Engine}
     * @param needsConvert   原文件不能直接查看、需转换代理产出可视化文件（.svlx 或 .pdf）
     * @param convertCommand 需转换时对应的旧系统转换命令名（FileToSvl / FileToPDF），便于对接转换代理
     * @param hint           给前端直接展示的说明文案
     * @param file           当前用于查看的文件（needsConvert 时为原文件，供转换代理取源）
     * @param files          对象下全部最新文件，前端可切换查看
     */
    public record VisualInfo(String objectType, Long objectId, String objectCode, String objectName,
                             String engine, boolean needsConvert, String convertCommand, String hint,
                             VisualFile file, List<VisualFile> files) {
    }
}
