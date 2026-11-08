package com.nexplm.platform.file.scan;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ClamAV 扫描实现（等保整改 ①）：INSTREAM 协议送检、毒签判定、异常不静默放行。
 */
class ClamAvFileScanHookTest {

    @Test
    void cleanFileIsReportedAsClean() throws Exception {
        try (FakeClamd clamd = new FakeClamd("stream: OK\0")) {
            ClamAvFileScanHook hook = new ClamAvFileScanHook("127.0.0.1", clamd.port(), 3000, 4096);
            assertThat(hook.scan(null, stream("hello"))).isEqualTo(FileScanHook.CLEAN);
        }
    }

    @Test
    void infectedFileIsReportedAsInfected() throws Exception {
        try (FakeClamd clamd = new FakeClamd("stream: Eicar-Test-Signature FOUND\0")) {
            ClamAvFileScanHook hook = new ClamAvFileScanHook("127.0.0.1", clamd.port(), 3000, 4096);
            assertThat(hook.scan(null, stream("X5O!P%@AP"))).isEqualTo(FileScanHook.INFECTED);
        }
    }

    /** 连不上 clamd 必须抛异常（由 FileService 记 ERROR），不能当 CLEAN 放行。 */
    @Test
    void unreachableClamdThrowsInsteadOfPassing() {
        ClamAvFileScanHook hook = new ClamAvFileScanHook("127.0.0.1", 1, 1000, 4096);
        assertThatThrownBy(() -> hook.scan(null, stream("data")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clamd");
    }

    /** 内容大于块大小时要分多块送完：8KB 内容 / 1KB 块。 */
    @Test
    void largeContentIsChunkedAndFullySent() throws Exception {
        try (FakeClamd clamd = new FakeClamd("stream: OK\0")) {
            ClamAvFileScanHook hook = new ClamAvFileScanHook("127.0.0.1", clamd.port(), 5000, 1024);
            byte[] big = new byte[8 * 1024];
            Arrays.fill(big, (byte) 7);
            assertThat(hook.scan(null, new ByteArrayInputStream(big))).isEqualTo(FileScanHook.CLEAN);
            assertThat(clamd.receivedBytes()).isEqualTo(big.length);
        }
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    /** 极简 clamd 替身：accept 一次，读完 INSTREAM 载荷后回固定响应，并统计收到的字节数。 */
    private static final class FakeClamd implements AutoCloseable {

        private final ServerSocket server;
        private final CompletableFuture<Void> done = new CompletableFuture<>();
        private int received;

        FakeClamd(String response) throws Exception {
            this.server = new ServerSocket(0);
            byte[] payload = response.getBytes(StandardCharsets.US_ASCII);
            Thread thread = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    InputStream in = socket.getInputStream();
                    skipCommand(in);
                    while (true) {
                        int length = readLength(in);
                        if (length <= 0) {
                            break;
                        }
                        received += in.readNBytes(length).length;
                    }
                    socket.getOutputStream().write(payload);
                    socket.getOutputStream().flush();
                } catch (Exception ignored) {
                    // 测试替身：连接被关闭属正常退出
                } finally {
                    done.complete(null);
                }
            }, "fake-clamd");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return server.getLocalPort();
        }

        int receivedBytes() throws Exception {
            done.get(5, TimeUnit.SECONDS);
            return received;
        }

        @Override
        public void close() throws Exception {
            server.close();
            done.get(5, TimeUnit.SECONDS);
        }

        private static void skipCommand(InputStream in) throws Exception {
            int b;
            while ((b = in.read()) > 0) {
                // 读到 \0 为止
            }
        }

        private static int readLength(InputStream in) throws Exception {
            byte[] buf = in.readNBytes(4);
            if (buf.length < 4) {
                return 0;
            }
            return ((buf[0] & 0xFF) << 24) | ((buf[1] & 0xFF) << 16)
                    | ((buf[2] & 0xFF) << 8) | (buf[3] & 0xFF);
        }
    }
}
