package com.nexplm.platform.security;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 签发与校验。
 * <p>access token 短期有效，refresh token 仅用于换发；两者用 typ 声明区分，防止混用。</p>
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(@Value("${nexplm.security.jwt.secret}") String secret,
                      @Value("${nexplm.security.jwt.access-ttl:PT30M}") Duration accessTtl,
                      @Value("${nexplm.security.jwt.refresh-ttl:P7D}") Duration refreshTtl) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("nexplm.security.jwt.secret 至少需要 32 字节（HS256）");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String issueAccessToken(Long userId, String username, Long companyId, Long sessionId) {
        return issue(userId, username, companyId, TYPE_ACCESS, accessTtl, sessionId);
    }

    public String issueRefreshToken(Long userId, String username, Long companyId, Long sessionId) {
        return issue(userId, username, companyId, TYPE_REFRESH, refreshTtl, sessionId);
    }

    /**
     * 会话 id（批次 F-1）：登录时落 {@code sys_user_session} 并把 id 写进令牌，
     * 认证过滤器据此校验「这条会话是否已被强制下线」。SSO 等无会话场景为 null。
     */
    public Long sessionId(Claims claims) {
        Object raw = claims.get("sid");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public long accessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    private String issue(Long userId, String username, Long companyId, String type, Duration ttl,
                         Long sessionId) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("companyId", companyId == null ? 0L : companyId);
        claims.put("typ", type);
        if (sessionId != null) {
            claims.put("sid", sessionId);
        }
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException ex) {
            throw BizException.of(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw BizException.of(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    public Claims parseAndRequireType(String token, String expectedType) {
        Claims claims = parse(token);
        String type = claims.get("typ", String.class);
        if (!expectedType.equals(type)) {
            throw BizException.of(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return claims;
    }

    public Long userId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }
}
