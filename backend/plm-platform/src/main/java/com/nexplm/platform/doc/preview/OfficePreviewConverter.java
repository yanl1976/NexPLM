package com.nexplm.platform.doc.preview;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Office/文本转 PDF：调用外部 LibreOffice（soffice）无头模式。
 * <p>转换命令由 {@code plm.preview.office-command}（或环境变量 PLM_PREVIEW_OFFICE_COMMAND）注入，
 * 未配置时视为环境不具备能力（SKIPPED），不产生失败噪音。</p>
 */
@Component
public class OfficePreviewConverter implements PreviewConverter {

    private static final Set<String> EXTS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf", "txt", "csv");
    private static final int TIMEOUT_SECONDS = 90;

    private final String command;

    public OfficePreviewConverter(@Value("${plm.preview.office-command:}") String command) {
        this.command = command;
    }

    @Override
    public String name() {
        return "libreoffice";
    }

    @Override
    public boolean supports(String ext) {
        return ext != null && EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<PreviewOutput> convert(Path source, String ext, Path workDir) throws IOException {
        if (command == null || command.isBlank()) {
            throw new PreviewUnavailableException(
                    "未配置 LibreOffice 转换命令（plm.preview.office-command / PLM_PREVIEW_OFFICE_COMMAND），"
                            + "该格式暂不支持在线预览");
        }
        ProcessBuilder builder = new ProcessBuilder(command, "--headless", "--norestore",
                "--convert-to", "pdf", "--outdir", workDir.toString(), source.toString());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("预览转换被中断", ex);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("预览转换超时（" + TIMEOUT_SECONDS + " 秒）");
        }
        if (process.exitValue() != 0) {
            throw new IOException("预览转换失败（exit=" + process.exitValue() + "）：" + brief(output));
        }
        Path pdf = findPdf(workDir);
        if (pdf == null) {
            throw new IOException("预览转换未产出 PDF：" + brief(output));
        }
        return List.of(new PreviewOutput("PDF", "preview.pdf", Files.readAllBytes(pdf), null));
    }

    private Path findPdf(Path workDir) throws IOException {
        try (Stream<Path> stream = Files.list(workDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private String brief(String output) {
        if (output == null || output.isBlank()) {
            return "(无输出)";
        }
        String trimmed = output.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }
}
