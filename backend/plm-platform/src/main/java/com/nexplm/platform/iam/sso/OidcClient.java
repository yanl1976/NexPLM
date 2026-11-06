package com.nexplm.platform.iam.sso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OIDC 授权码 + PKCE 客户端。
 *
 * <p>三件事必须做对，否则 SSO 只是"看起来能登"：</p>
 * <ol>
 *   <li><b>state 自签</b>：不放服务端会话（本系统无状态），把 provider/verifier/nonce/过期时间
 *       用 HMAC 签成一个串；回调时验签 + 验过期，防 CSRF 与伪造回调。</li>
 *   <li><b>id_token 验签</b>：按 header.kid 从 JWKS 取 RSA 公钥，校验签名与 iss/aud/exp；
 *       不验签等于信任任何能访问回调地址的人。</li>
 *   <li><b>PKCE</b>：即使客户端密钥泄漏，没有 verifier 也换不出令牌。</li>
 * </ol>
 */
@Component
public class OidcClient {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration JWKS_TTL = Duration.ofMinutes(10);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8);

    private final ObjectMapper jsonMapper;
    private final String stateKey;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final Map<String, CachedJwks> jwksCache = new ConcurrentHashMap<>();

    public OidcClient(ObjectMapper jsonMapper,
                      @Value("${nexplm.security.jwt.secret}") String jwtSecret) {
        this.jsonMapper = jsonMapper;
        // 状态签名用从 JWT 密钥派生出的独立子密钥，避免与令牌签名共用同一份材料
        this.stateKey = hmacHex(jwtSecret, "nexplm-sso-state");
    }

    /** 身份提供方配置（OIDC）。 */
    public record Config(String issuer, String authorizationEndpoint, String tokenEndpoint,
                         String jwksEndpoint, String clientId, String clientSecret,
                         String redirectUri, String scope, String usernameClaim,
                         String displayNameClaim, String emailClaim) {
    }

    /** 从 IdP 拿到的人身份（不含任何本站权限）。 */
    public record Identity(String subject, String username, String displayName, String email) {
    }

    /** 自签 state 解出来的内容（回调时需要 verifier 与 nonce）。 */
    public record State(String providerCode, String codeVerifier, String nonce, long expireAt) {
    }

    private record CachedJwks(JsonNode keys, long fetchedAt) {
    }

    /** 生成跳转地址：带 PKCE 挑战与自签 state。 */
    public Map.Entry<String, String> authorizeUrl(Config config, String providerCode) {
        String verifier = randomToken(32);
        String nonce = randomToken(16);
        String state = signState(new State(providerCode, verifier, nonce,
                Instant.now().plus(STATE_TTL).getEpochSecond()));
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", config.clientId());
        params.put("redirect_uri", config.redirectUri());
        params.put("scope", blankToDefault(config.scope(), "openid profile email"));
        params.put("state", state);
        params.put("nonce", nonce);
        params.put("code_challenge", base64Url(sha256(verifier)));
        params.put("code_challenge_method", "S256");
        return Map.entry(config.authorizationEndpoint() + (config.authorizationEndpoint().contains("?")
                ? "&" : "?") + formEncode(params), state);
    }

    /** 校验 state 并取出 PKCE verifier 与 nonce。 */
    public State verifyState(String state, String providerCode) {
        String[] parts = state == null ? new String[0] : state.split("\\.");
        if (parts.length != 2) {
            throw BizException.of(ErrorCode.SSO_STATE_INVALID);
        }
        String expected = hmacHex(stateKey, parts[0]);
        if (!constantTimeEquals(expected, parts[1])) {
            throw BizException.of(ErrorCode.SSO_STATE_INVALID);
        }
        JsonNode node;
        try {
            node = jsonMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SSO_STATE_INVALID);
        }
        if (node.path("exp").asLong() < Instant.now().getEpochSecond()) {
            throw BizException.of(ErrorCode.SSO_STATE_INVALID, "登录状态已过期，请重新发起");
        }
        if (!providerCode.equals(node.path("p").asText())) {
            throw BizException.of(ErrorCode.SSO_STATE_INVALID);
        }
        return new State(node.path("p").asText(), node.path("v").asText(),
                node.path("n").asText(), node.path("exp").asLong());
    }

    /** 用授权码换令牌，并校验 id_token（签名 + iss + aud + exp + nonce）。 */
    public Identity exchange(Config config, String authCode, String codeVerifier, String nonce) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", authCode);
        form.put("redirect_uri", config.redirectUri());
        form.put("client_id", config.clientId());
        if (!blank(config.clientSecret())) {
            form.put("client_secret", config.clientSecret());
        }
        form.put("code_verifier", codeVerifier);

        JsonNode token = postForm(config.tokenEndpoint(), form);
        String idToken = token.path("id_token").asText(null);
        if (blank(idToken)) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "身份提供方未返回 id_token");
        }
        JsonNode claims = verifyIdToken(idToken, config);
        if (!blank(nonce) && !nonce.equals(claims.path("nonce").asText())) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token nonce 不匹配");
        }
        String subject = claims.path("sub").asText(null);
        if (blank(subject)) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token 缺少 sub");
        }
        String username = text(claims, config.usernameClaim());
        return new Identity(subject, blank(username) ? subject : username,
                text(claims, config.displayNameClaim()), text(claims, config.emailClaim()));
    }

    /** id_token 验签与声明校验，返回 payload。 */
    public JsonNode verifyIdToken(String idToken, Config config) {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token 结构不合法");
        }
        JsonNode header = decodeJson(parts[0]);
        JsonNode payload = decodeJson(parts[1]);
        if (!"RS256".equals(header.path("alg").asText())) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID,
                    "仅支持 RS256 签名的 id_token，收到 " + header.path("alg").asText());
        }
        String kid = header.path("kid").asText("");
        JsonNode jwk = findKey(config.jwksEndpoint(), kid);
        boolean ok;
        try {
            byte[] modulus = Base64.getUrlDecoder().decode(jwk.path("n").asText());
            byte[] exponent = Base64.getUrlDecoder().decode(jwk.path("e").asText());
            var publicKey = KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            ok = signature.verify(Base64.getUrlDecoder().decode(parts[2]));
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token 签名校验异常");
        }
        if (!ok) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token 签名不匹配");
        }
        String issuer = payload.path("iss").asText("");
        if (!sameIssuer(issuer, config.issuer())) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token iss 与配置不一致");
        }
        JsonNode aud = payload.path("aud");
        boolean audienceOk = aud.isArray()
                ? containsText(aud, config.clientId())
                : config.clientId().equals(aud.asText());
        if (!audienceOk) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token aud 与 clientId 不一致");
        }
        long exp = payload.path("exp").asLong(0);
        if (exp > 0 && exp < Instant.now().getEpochSecond()) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token 已过期");
        }
        return payload;
    }

    /* ------------------------------------------------------------------ 内部 */

    private JsonNode postForm(String url, Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw BizException.of(ErrorCode.SSO_HANDSHAKE_FAILED,
                        "令牌端点返回 HTTP " + response.statusCode());
            }
            return jsonMapper.readTree(response.body());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SSO_HANDSHAKE_FAILED, "令牌端点不可达：" + ex.getMessage());
        }
    }

    private JsonNode findKey(String jwksEndpoint, String kid) {
        CachedJwks cached = jwksCache.get(jwksEndpoint);
        long now = Instant.now().getEpochSecond();
        if (cached == null || now - cached.fetchedAt() > JWKS_TTL.toSeconds()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(jwksEndpoint))
                    .timeout(HTTP_TIMEOUT).header("Accept", "application/json").GET().build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw BizException.of(ErrorCode.SSO_HANDSHAKE_FAILED,
                            "JWKS 端点返回 HTTP " + response.statusCode());
                }
                cached = new CachedJwks(jsonMapper.readTree(response.body()).path("keys"), now);
                jwksCache.put(jwksEndpoint, cached);
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ex) {
                throw BizException.of(ErrorCode.SSO_HANDSHAKE_FAILED, "JWKS 端点不可达：" + ex.getMessage());
            }
        }
        for (JsonNode key : cached.keys()) {
            if (blank(kid) || kid.equals(key.path("kid").asText())) {
                return key;
            }
        }
        throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "JWKS 中找不到签名公钥 kid=" + kid);
    }

    private String signState(State state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("p", state.providerCode());
        payload.put("v", state.codeVerifier());
        payload.put("n", state.nonce());
        payload.put("exp", state.expireAt());
        String body;
        try {
            body = base64Url(jsonMapper.writeValueAsBytes(payload));
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SSO_STATE_INVALID);
        }
        return body + "." + hmacHex(stateKey, body);
    }

    private JsonNode decodeJson(String part) {
        try {
            return jsonMapper.readTree(Base64.getUrlDecoder().decode(part));
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.SSO_TICKET_INVALID, "id_token 编码不合法");
        }
    }

    private static boolean sameIssuer(String actual, String expected) {
        return actual != null && expected != null
                && actual.replaceAll("/+$", "").equals(expected.replaceAll("/+$", ""));
    }

    private static boolean containsText(JsonNode array, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private static String text(JsonNode claims, String field) {
        if (blank(field)) {
            return null;
        }
        JsonNode node = claims.path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private static String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }

    private static String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        new java.security.SecureRandom().nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static byte[] sha256(String value) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hmacHex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("state 签名失败", ex);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length(); i++) {
            diff |= left.charAt(i) ^ right.charAt(i);
        }
        return diff == 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToDefault(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    /** 便于上层判断 state 是否过期用的当前时间。 */
    public OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}
