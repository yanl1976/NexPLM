package com.nexplm.platform.file.storage;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;

/**
 * MinIO（S3 协议）对象存储实现，生产环境使用。
 * <p>启用方式：{@code nexplm.file.storage=minio}。启动时自动确保存储桶存在。
 * 支持预签名直链，让浏览器直接向对象存储取文件，避免应用服务器中转大文件。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.file.storage", havingValue = "minio")
public class MinioFileStorage implements FileStorage {

    private static final int MAX_PRESIGN_SECONDS = 7 * 24 * 60 * 60;

    private final MinioClient client;
    private final String bucket;

    public MinioFileStorage(@Value("${nexplm.file.minio.endpoint}") String endpoint,
                            @Value("${nexplm.file.minio.access-key}") String accessKey,
                            @Value("${nexplm.file.minio.secret-key}") String secretKey,
                            @Value("${nexplm.file.minio.bucket:nexplm}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("初始化对象存储桶失败：" + bucket, ex);
        }
    }

    @Override
    public String put(String key, InputStream inputStream, long size) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(inputStream, size, -1)
                    .build());
            return key;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "写入对象存储失败：" + ex.getMessage());
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.FILE_OBJECT_NOT_FOUND, "对象存储中不存在该文件：" + key);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (ErrorResponseException ex) {
            return false;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "查询对象存储失败：" + ex.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "删除对象存储文件失败：" + ex.getMessage());
        }
    }

    @Override
    public String type() {
        return "minio";
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        long seconds = ttl == null ? 3600L : ttl.toSeconds();
        int expiry = (int) Math.max(60L, Math.min(seconds, MAX_PRESIGN_SECONDS));
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry(expiry)
                    .build());
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.FILE_STORAGE_ERROR, "生成下载直链失败：" + ex.getMessage());
        }
    }
}
