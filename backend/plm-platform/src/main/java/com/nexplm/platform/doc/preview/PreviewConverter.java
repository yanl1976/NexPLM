package com.nexplm.platform.doc.preview;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 预览转换器 SPI：按扩展名声明能力，把源文件转换为可在线预览的产物（PDF/PNG 等）。
 * <p>新增格式支持只需实现本接口并注册为 Spring Bean，无需改动服务与接口。</p>
 */
public interface PreviewConverter {

    /** 转换器名称，用于日志与失败提示。 */
    String name();

    /** 是否支持该扩展名（小写、不含点，如 pdf / docx / png）。 */
    boolean supports(String ext);

    /**
     * 执行转换。
     *
     * @param source 源文件本地临时文件（扩展名保留）
     * @param ext    源文件扩展名（小写）
     * @param workDir 可用临时目录，转换产物可写在此处
     * @return 产物列表；PDF 通常 1 个，PNG 可能多页
     * @throws PreviewUnavailableException 运行环境不具备转换能力（缺依赖/未配置命令），视为 SKIPPED
     * @throws IOException                 转换执行失败，视为 FAILED 可重试
     */
    List<PreviewOutput> convert(Path source, String ext, Path workDir) throws IOException;
}
