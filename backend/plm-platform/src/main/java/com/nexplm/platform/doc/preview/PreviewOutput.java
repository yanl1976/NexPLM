package com.nexplm.platform.doc.preview;

/**
 * 单个预览产物：格式、展示文件名、内容与页数。
 */
public record PreviewOutput(String format, String fileName, byte[] content, Integer pageCount) {
}
