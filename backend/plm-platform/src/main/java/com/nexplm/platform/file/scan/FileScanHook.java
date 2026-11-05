package com.nexplm.platform.file.scan;

import com.nexplm.platform.file.domain.FileObject;

import java.io.InputStream;

/**
 * 文件安全扫描钩子。上传新物理文件后调用，结果写入 {@code file_object.scan_status}。
 * <p>约定返回：{@code CLEAN}（通过）、{@code INFECTED}（检出威胁，禁止下载）、
 * {@code SKIPPED}（未扫描，默认实现）。实现方阻塞式返回即可，平台按文件只扫一次。</p>
 */
public interface FileScanHook {

    String CLEAN = "CLEAN";
    String INFECTED = "INFECTED";
    String SKIPPED = "SKIPPED";

    /** 扫描文件内容，返回扫描状态；抛异常时平台记为 ERROR 并允许后续重扫。 */
    String scan(FileObject file, InputStream content);
}
