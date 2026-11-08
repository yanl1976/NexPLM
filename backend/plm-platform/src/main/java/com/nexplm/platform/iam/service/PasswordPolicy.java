package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.domain.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码策略（{@code docs/07} 待整改 ④）：强度、历史不可重用、有效期。
 *
 * <p>默认值刻意"安全但不锁死"：强度 8 位 + 3 类字符、历史 3 条、<b>有效期 0 天（不过期）</b>——
 * 现有账号的 {@code pwd_updated_at} 多为空，若默认开启过期会让所有人登录即被拒。</p>
 *
 * <p>三项都可在 {@code application.yml} 调整：{@code nexplm.security.password.*}。</p>
 */
@Component
public class PasswordPolicy {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9]");
    private static final int MAX_LENGTH = 64;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final int minLength;
    private final int requiredClasses;
    private final int historySize;
    private final int expireDays;

    public PasswordPolicy(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
                          @Value("${nexplm.security.password.min-length:8}") int minLength,
                          @Value("${nexplm.security.password.required-classes:3}") int requiredClasses,
                          @Value("${nexplm.security.password.history-size:3}") int historySize,
                          @Value("${nexplm.security.password.expire-days:0}") int expireDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.minLength = Math.max(8, minLength);
        this.requiredClasses = Math.min(4, Math.max(1, requiredClasses));
        this.historySize = Math.max(0, historySize);
        this.expireDays = Math.max(0, expireDays);
    }

    /** 强度校验：长度 + 字符类数量 + 不得与用户名相同。 */
    public void validate(String raw, String username) {
        if (raw == null || raw.length() < minLength || raw.length() > MAX_LENGTH) {
            throw BizException.of(ErrorCode.AUTH_PASSWORD_WEAK,
                    "密码长度需 " + minLength + "~" + MAX_LENGTH + " 位");
        }
        int classes = 0;
        if (UPPER.matcher(raw).find()) {
            classes++;
        }
        if (LOWER.matcher(raw).find()) {
            classes++;
        }
        if (DIGIT.matcher(raw).find()) {
            classes++;
        }
        if (SYMBOL.matcher(raw).find()) {
            classes++;
        }
        if (classes < requiredClasses) {
            throw BizException.of(ErrorCode.AUTH_PASSWORD_WEAK,
                    "密码需包含大写字母、小写字母、数字、符号中的至少 " + requiredClasses + " 类");
        }
        if (username != null && !username.isBlank() && raw.equalsIgnoreCase(username)) {
            throw BizException.of(ErrorCode.AUTH_PASSWORD_WEAK, "密码不能与用户名相同");
        }
    }

    /** 历史密码不可重用：与最近 {@code history-size} 条历史哈希比对。 */
    public void assertNotReused(Long userId, String raw) {
        if (historySize <= 0 || userId == null || raw == null) {
            return;
        }
        List<String> hashes = jdbcTemplate.queryForList(
                "SELECT password_hash FROM sys_user_pwd_history WHERE user_id = ? "
                        + "ORDER BY created_at DESC LIMIT ?",
                String.class, userId, historySize);
        for (String hash : hashes) {
            if (passwordEncoder.matches(raw, hash)) {
                throw BizException.of(ErrorCode.AUTH_PASSWORD_REUSED,
                        "新密码不能与最近 " + historySize + " 次使用过的密码相同");
            }
        }
    }

    /** 记录一次成功设置的新密码（只存哈希），并清理过旧记录（保留 {@code max(2N,10)} 条）。 */
    public void record(Long userId, String passwordHash) {
        if (historySize <= 0 || userId == null || passwordHash == null) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO sys_user_pwd_history (id, user_id, password_hash)
                VALUES (?, ?, ?)
                """, IdWorker.getId(), userId, passwordHash);
        int keep = Math.max(historySize * 2, 10);
        jdbcTemplate.update("""
                DELETE FROM sys_user_pwd_history
                 WHERE user_id = ? AND id NOT IN (
                       SELECT id FROM sys_user_pwd_history WHERE user_id = ?
                        ORDER BY created_at DESC LIMIT ?)
                """, userId, userId, keep);
    }

    /** 有效期：{@code expire-days<=0} 表示不过期（默认，避免锁死历史账号）。 */
    public void assertNotExpired(SysUser user) {
        if (expireDays <= 0 || user == null) {
            return;
        }
        OffsetDateTime at = user.getPwdUpdatedAt();
        if (at == null || at.isBefore(OffsetDateTime.now().minusDays(expireDays))) {
            throw BizException.of(ErrorCode.AUTH_PASSWORD_EXPIRED);
        }
    }

    public int historySize() {
        return historySize;
    }
}
