package com.nexplm.platform.file.scan;

import com.nexplm.platform.file.domain.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV 扫描实现（{@code docs/07} 待整改 ① 的最后一块）：通过 clamd 的 INSTREAM 协议流式送检。
 *
 * <p>启用方式：{@code nexplm.file.scan=clamav}（默认 {@code noop}）。启用后
 * {@link NoopFileScanHook} 自动停用，新落盘的文件会送 clamd 扫描：</p>
 * <ul>
 *   <li>{@code stream: OK} → {@code CLEAN}</li>
 *   <li>{@code stream: <签名> FOUND} → {@code INFECTED}，{@code file_object.scan_status} 落该值，
 *       下载接口据此拒绝（{@code FileService.ensureDownloadable}）</li>
 *   <li>连接/超时/协议异常 → 抛异常，由 {@code FileService.scan} 记为 {@code ERROR} 并允许重扫，
 *       <b>不静默按 CLEAN 放行</b></li>
 * </ul>
 *
 * <p>流式送检：文件按 {@code chunk-size} 分块写入 socket，不把整个文件读进内存，
 * 所以 4GB 的分片合并结果也不会撑爆堆。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.file.scan", havingValue = "clamav")
public class ClamAvFileScanHook implements FileScanHook {

    private static final Logger log = LoggerFactory.getLogger(ClamAvFileScanHook.class);
    /** clamd 命令以 \0 结尾，{@code z} 前缀即表示"按 \0 结尾"。 */
    private static final byte[] COMMAND = "zINSTREAM\0".getBytes(StandardCharsets.US_ASCII);
    /** 长度 0 的块表示流结束。 */
    private static final byte[] END = new byte[]{0, 0, 0, 0};
    private static final String OK = "OK";
    private static final String FOUND = "FOUND";

    private final String host;
    private final int port;
    private final int timeoutMs;
    private final int chunkSize;

    public ClamAvFileScanHook(@Value("${nexplm.file.scan-clamav.host:127.0.0.1}") String host,
                              @Value("${nexplm.file.scan-clamav.port:3310}") int port,
                              @Value("${nexplm.file.scan-clamav.timeout-ms:30000}") int timeoutMs,
                              @Value("${nexplm.file.scan-clamav.chunk-size:8192}") int chunkSize) {
        this.host = host;
        this.port = port;
        // 低于 1s 的超时在大文件上必然误判，兜底锁 1s
        this.timeoutMs = Math.max(1000, timeoutMs);
        this.chunkSize = Math.min(Math.max(1024, chunkSize), 64 * 1024);
    }

    @Override
    public String scan(FileObject file, InputStream content) {
        Long fileId = file == null ? null : file.getId();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream out = socket.getOutputStream();
            out.write(COMMAND);
            byte[] buffer = new byte[chunkSize];
            int read;
            while ((read = content.read(buffer)) > 0) {
                out.write(ByteBuffer.allocate(4).putInt(read).array());
                out.write(buffer, 0, read);
            }
            out.write(END);
            out.flush();
            String response = readResponse(socket.getInputStream());
            if (response.contains(FOUND)) {
                log.warn("文件检出威胁：fileId={} response={}", fileId, response);
                return INFECTED;
            }
            if (response.contains(OK)) {
                return CLEAN;
            }
            throw new IllegalStateException("clamd 响应无法识别：" + response);
        } catch (IOException ex) {
            throw new IllegalStateException("clamd 扫描失败（" + host + ":" + port + "）：" + ex.getMessage(), ex);
        }
    }

    /** 读到 \0 或流末尾为止；clamd 每个响应都以 \0 收尾。 */
    private static String readResponse(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) >= 0) {
            if (b == 0) {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString().trim();
    }
}
