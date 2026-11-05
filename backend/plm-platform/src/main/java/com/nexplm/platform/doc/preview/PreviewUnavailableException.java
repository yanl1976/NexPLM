package com.nexplm.platform.doc.preview;

/**
 * 运行环境不具备预览转换能力（缺命令/缺依赖），对应预览状态 SKIPPED 而非失败。
 */
public class PreviewUnavailableException extends RuntimeException {

    public PreviewUnavailableException(String message) {
        super(message);
    }
}
