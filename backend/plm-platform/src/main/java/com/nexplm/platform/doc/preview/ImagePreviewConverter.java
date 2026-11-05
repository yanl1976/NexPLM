package com.nexplm.platform.doc.preview;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 图片直通：位图本身即可在线预览，无需转换。
 */
@Component
public class ImagePreviewConverter implements PreviewConverter {

    private static final Set<String> EXTS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    @Override
    public String name() {
        return "image-passthrough";
    }

    @Override
    public boolean supports(String ext) {
        return ext != null && EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<PreviewOutput> convert(Path source, String ext, Path workDir) throws IOException {
        String normalized = ext.toLowerCase(Locale.ROOT);
        return List.of(new PreviewOutput(normalized.toUpperCase(Locale.ROOT),
                "preview." + normalized, Files.readAllBytes(source), 1));
    }
}
