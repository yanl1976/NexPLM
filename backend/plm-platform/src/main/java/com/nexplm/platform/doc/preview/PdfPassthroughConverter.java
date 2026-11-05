package com.nexplm.platform.doc.preview;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * PDF 直通：源文件已是可预览格式，直接复用。
 */
@Component
public class PdfPassthroughConverter implements PreviewConverter {

    @Override
    public String name() {
        return "pdf-passthrough";
    }

    @Override
    public boolean supports(String ext) {
        return "pdf".equalsIgnoreCase(ext);
    }

    @Override
    public List<PreviewOutput> convert(Path source, String ext, Path workDir) throws IOException {
        return List.of(new PreviewOutput("PDF", "preview.pdf", Files.readAllBytes(source), null));
    }
}
