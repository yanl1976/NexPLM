package com.nexplm.platform.file.scan;

import com.nexplm.platform.file.domain.FileObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 默认扫描实现：不接入任何杀毒引擎，直接标记为 SKIPPED。
 * <p>生产接入 ClamAV 等服务时，把 {@code nexplm.file.scan} 配置为其它值（如 clamav）
 * 并提供自己的 {@link FileScanHook} Bean，本实现即自动停用。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.file.scan", havingValue = "noop", matchIfMissing = true)
public class NoopFileScanHook implements FileScanHook {

    @Override
    public String scan(FileObject file, InputStream content) {
        return SKIPPED;
    }
}
