package com.nexplm.platform.file.storage;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储（开发/小规模部署默认实现）。
 * <p>目录结构：{root}/{sha[0:2]}/{sha[2:4]}/{sha}，与对象存储的 key 规则保持一致，便于后续迁移。</p>
 */
@Component
@ConditionalOnProperty(name = "nexplm.file.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${nexplm.file.root:./data/files}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建文件存储目录：" + this.root, ex);
        }
    }

    @Override
    public String put(String key, InputStream inputStream, long size) {
        Path target = resolveSafe(key);
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.COMMON_INTERNAL, "文件写入失败：" + ex.getMessage());
        }
    }

    @Override
    public InputStream get(String key) {
        Path target = resolveSafe(key);
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "文件不存在：" + key);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolveSafe(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveSafe(key));
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.COMMON_INTERNAL, "文件删除失败：" + ex.getMessage());
        }
    }

    @Override
    public String type() {
        return "local";
    }

    private Path resolveSafe(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "非法的存储键");
        }
        return target;
    }
}
