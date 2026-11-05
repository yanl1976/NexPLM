package com.nexplm.platform.file.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * 文件存储抽象。生产用 MinIO（S3 协议），开发可用本地磁盘，二者实现同一接口。
 * <p>存储键采用内容寻址（sha256），天然去重与秒传。</p>
 */
public interface FileStorage {

    /** 写入文件，返回存储键。实现需幂等：同 key 重复写入覆盖为相同内容。 */
    String put(String key, InputStream inputStream, long size);

    InputStream get(String key);

    boolean exists(String key);

    void delete(String key);

    /** 存储类型标识，便于运维排查（local / minio）。 */
    String type();

    /**
     * 预签名下载直链（对象存储专用，可让浏览器绕过应用服务器直取文件）。
     * <p>本地存储无法提供直链，返回 {@code null}，调用方回退为服务端流式下载。</p>
     */
    default String presignedGetUrl(String key, Duration ttl) {
        return null;
    }
}
