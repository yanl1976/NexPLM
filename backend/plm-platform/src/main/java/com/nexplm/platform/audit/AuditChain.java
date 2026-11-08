package com.nexplm.platform.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 审计行哈希的规范化与计算（`docs/07` 待整改 ⑤：审计防篡改）。
 *
 * <p>参与哈希的字段刻意<b>不含 id 与 created_at</b>：id 是自增列、created_at 由数据库存
 * timestamptz（微秒精度），两者在"写入时算"与"事后校验时算"之间都可能不一致（纳秒被截断），
 * 会导致所有行都判为篡改。业务字段（模块/动作/对象/结果/人/详情）足以覆盖"改内容"的场景。</p>
 *
 * <p>删除中间行由链本身发现：下一行的 {@code prev_hash} 指向被删行的 hash，校验时对不上。</p>
 */
public final class AuditChain {

    private AuditChain() {
    }

    /** 规范化载荷：字段间用 {@code |} 分隔，null 统一成空串（保证两端一致）。 */
    public static String payload(String module, String action, String objectType, Long objectId,
                                 String result, Long userId, String detail) {
        return String.join("|",
                nvl(module), nvl(action), nvl(objectType),
                objectId == null ? "" : objectId.toString(),
                nvl(result),
                userId == null ? "" : userId.toString(),
                nvl(detail));
    }

    /** {@code sha256(prevHash + "|" + payload)}，十六进制小写。 */
    public static String hash(String prevHash, String payload) {
        return sha256((prevHash == null ? "" : prevHash) + "|" + payload);
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            // JDK 必然支持 SHA-256，这里只是把受检异常挡掉
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
