package com.nexplm.platform.iam.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.iam.api.AuthDtos;
import com.nexplm.platform.iam.api.SsoDtos;
import com.nexplm.platform.iam.domain.SysUser;
import com.nexplm.platform.iam.mapper.SysUserMapper;
import com.nexplm.platform.iam.sso.LdapAuthenticator;
import com.nexplm.platform.iam.sso.OidcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * P9-5 单点登录：身份提供方配置 + OIDC/LDAP 登录 + 账号绑定/自动建号。
 *
 * <p>账号匹配顺序（可预测，不搞"猜"）：</p>
 * <ol>
 *   <li>先按 {@code (provider_id, subject)} 找绑定——同一 IdP 的同一主体永远落到同一个账号；</li>
 *   <li>没绑定则按「同公司 + 用户名」匹配既有账号并建立绑定（老账号平滑接入）；</li>
 *   <li>仍没有且开了自动建号才建号，并按 {@code default_role_code} 授最小角色；否则拒绝登录。</li>
 * </ol>
 *
 * <p>自动建号的账号 {@code auth_provider='SSO'}、密码哈希为随机串（本地登录不可用），
 * 避免出现"SSO 建出来的账号还能用默认密码登"的口子。</p>
 */
@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);

    public static final String TYPE_OIDC = "OIDC";
    public static final String TYPE_LDAP = "LDAP";
    private static final Set<String> TYPES = Set.of(TYPE_OIDC, TYPE_LDAP);

    /** 各类型的密钥字段：只写不读回。 */
    private static final Map<String, List<String>> SECRET_KEYS = Map.of(
            TYPE_OIDC, List.of("clientSecret"),
            TYPE_LDAP, List.of("bindCredential"));

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;
    private final OidcClient oidcClient;
    private final LdapAuthenticator ldapAuthenticator;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper userMapper;

    public SsoService(JdbcTemplate jdbcTemplate, ObjectMapper jsonMapper, OidcClient oidcClient,
                      LdapAuthenticator ldapAuthenticator, AuthService authService,
                      PasswordEncoder passwordEncoder, SysUserMapper userMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.oidcClient = oidcClient;
        this.ldapAuthenticator = ldapAuthenticator;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    /* ============================================================ 配置维护 */

    public PageResult<SsoDtos.ProviderRow> providerPage(Long companyId, String keyword, Integer page,
                                                       Integer size) {
        StringBuilder where = new StringBuilder(" WHERE company_id = ? AND deleted = false");
        List<Object> args = new ArrayList<>();
        args.add(companyId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (code ILIKE ? OR name ILIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_sso_provider" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<SsoDtos.ProviderRow> items = jdbcTemplate.query("""
                        SELECT id, code, name, type, enabled, auto_provision, default_role_code,
                               remark, updated_at
                          FROM sys_sso_provider
                        """ + where + " ORDER BY code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new SsoDtos.ProviderRow(rs.getLong("id"), rs.getString("code"),
                        rs.getString("name"), rs.getString("type"), rs.getBoolean("enabled"),
                        rs.getBoolean("auto_provision"), rs.getString("default_role_code"),
                        rs.getString("remark"),
                        rs.getObject("updated_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public SsoDtos.ProviderDetail providerDetail(Long id, Long companyId) {
        return toDetail(requireProvider(id, companyId));
    }

    @Transactional
    public SsoDtos.ProviderDetail createProvider(SsoDtos.ProviderSaveReq req, Long companyId,
                                                 Long userId) {
        String type = requireType(req.type());
        Map<String, Object> config = validateConfig(type, req.config());
        long id = IdWorker.getId();
        try {
            jdbcTemplate.update("""
                            INSERT INTO sys_sso_provider (id, company_id, code, name, type, enabled,
                                                          config, auto_provision, default_role_code,
                                                          remark, created_by, updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?)
                            """,
                    id, companyId, req.code().trim(), req.name().trim(), type,
                    req.enabled() == null || req.enabled(), toJson(config),
                    req.autoProvision() == null || req.autoProvision(),
                    blankToNull(req.defaultRoleCode()), blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_DUPLICATED, "SSO 编码已存在：" + req.code());
        }
        return providerDetail(id, companyId);
    }

    @Transactional
    public SsoDtos.ProviderDetail updateProvider(Long id, SsoDtos.ProviderSaveReq req, Long companyId,
                                                 Long userId) {
        Map<String, Object> current = requireProvider(id, companyId);
        String type = requireType(req.type());
        Map<String, Object> config = validateConfig(type, req.config());
        // 密钥留空 = 保持不变（前端拿不到原值，只能这么表达"不改"）
        Map<String, Object> oldConfig = readConfig(current.get("config_text"));
        for (String secretKey : SECRET_KEYS.getOrDefault(type, List.of())) {
            Object incoming = config.get(secretKey);
            if (incoming == null || String.valueOf(incoming).isBlank()) {
                Object saved = oldConfig.get(secretKey);
                if (saved != null && !String.valueOf(saved).isBlank()) {
                    config.put(secretKey, saved);
                } else {
                    config.remove(secretKey);
                }
            }
        }
        try {
            jdbcTemplate.update("""
                            UPDATE sys_sso_provider
                               SET code = ?, name = ?, type = ?, enabled = ?, config = CAST(? AS jsonb),
                                   auto_provision = ?, default_role_code = ?, remark = ?,
                                   updated_by = ?, updated_at = now()
                             WHERE id = ? AND company_id = ? AND deleted = false
                            """,
                    req.code().trim(), req.name().trim(), type, req.enabled() == null || req.enabled(),
                    toJson(config), req.autoProvision() == null || req.autoProvision(),
                    blankToNull(req.defaultRoleCode()), blankToNull(req.remark()), userId, id, companyId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_DUPLICATED, "SSO 编码已存在：" + req.code());
        }
        return providerDetail(id, companyId);
    }

    @Transactional
    public void deleteProvider(Long id, Long companyId, Long userId) {
        requireProvider(id, companyId);
        jdbcTemplate.update("""
                UPDATE sys_sso_provider SET deleted = true, enabled = false, updated_by = ?,
                                            updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, userId, id, companyId);
    }

    /** 登录页用：只暴露编码/名称/类型，避免把 IdP 地址与配置泄漏给匿名访问者。 */
    public List<SsoDtos.PublicProvider> publicProviders() {
        return jdbcTemplate.query("""
                SELECT code, name, type FROM sys_sso_provider
                 WHERE enabled = true AND deleted = false ORDER BY code
                """, (rs, rowNum) -> new SsoDtos.PublicProvider(rs.getString("code"),
                rs.getString("name"), rs.getString("type")));
    }

    /* ============================================================ 登录握手 */

    public SsoDtos.AuthorizeResp authorize(String code) {
        Map<String, Object> provider = requireEnabled(code);
        if (!TYPE_OIDC.equals(provider.get("type"))) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID, "该配置不是 OIDC 类型");
        }
        OidcClient.Config config = oidcConfig(readConfig(provider.get("config_text")));
        Map.Entry<String, String> entry = oidcClient.authorizeUrl(config, code);
        return new SsoDtos.AuthorizeResp(entry.getKey(), entry.getValue());
    }

    public AuthDtos.TokenResp oidcCallback(String code, String authCode, String state) {
        Map<String, Object> provider = requireEnabled(code);
        if (!TYPE_OIDC.equals(provider.get("type"))) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID, "该配置不是 OIDC 类型");
        }
        OidcClient.Config config = oidcConfig(readConfig(provider.get("config_text")));
        OidcClient.State verified = oidcClient.verifyState(state, code);
        OidcClient.Identity identity = oidcClient.exchange(config, authCode,
                verified.codeVerifier(), verified.nonce());
        return loginWith(provider, identity);
    }

    public AuthDtos.TokenResp ldapLogin(String code, String username, String password) {
        Map<String, Object> provider = requireEnabled(code);
        if (!TYPE_LDAP.equals(provider.get("type"))) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID, "该配置不是 LDAP 类型");
        }
        LdapAuthenticator.Config config = ldapConfig(readConfig(provider.get("config_text")));
        LdapAuthenticator.Identity identity = ldapAuthenticator.authenticate(config, username, password);
        return loginWith(provider, new OidcClient.Identity(identity.subject(), identity.username(),
                identity.displayName(), identity.email()));
    }

    /** 绑定/建号 + 签发本站令牌。 */
    @Transactional
    public AuthDtos.TokenResp loginWith(Map<String, Object> provider, OidcClient.Identity identity) {
        long providerId = longOf(provider.get("id"));
        long companyId = longOf(provider.get("company_id"));
        boolean autoProvision = Boolean.TRUE.equals(provider.get("auto_provision"));
        String defaultRole = (String) provider.get("default_role_code");

        Long userId = findBoundUser(providerId, identity.subject());
        if (userId == null) {
            SysUser existing = findUserByUsername(companyId, identity.username());
            if (existing != null) {
                userId = existing.getId();
                linkUser(providerId, userId, identity);
            } else if (autoProvision) {
                userId = provision(companyId, identity, defaultRole);
                linkUser(providerId, userId, identity);
                log.info("SSO 自动建号：provider={} subject={} user={}", providerId,
                        identity.subject(), identity.username());
            } else {
                throw BizException.of(ErrorCode.SSO_ACCOUNT_NOT_MAPPED);
            }
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.of(ErrorCode.SSO_ACCOUNT_NOT_MAPPED);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BizException.of(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        jdbcTemplate.update("UPDATE sys_user SET last_login_at = now() WHERE id = ?", userId);
        jdbcTemplate.update("""
                UPDATE sys_user_sso SET last_login_at = now(), external_username = ?, updated_at = now()
                 WHERE provider_id = ? AND subject = ?
                """, identity.username(), providerId, identity.subject());
        return authService.issueToken(user);
    }

    /* ============================================================ 内部 */

    private Long findBoundUser(long providerId, String subject) {
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT s.user_id FROM sys_user_sso s
                  JOIN sys_user u ON u.id = s.user_id AND u.deleted = false
                 WHERE s.provider_id = ? AND s.subject = ?
                """, Long.class, providerId, subject);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private SysUser findUserByUsername(long companyId, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getCompanyId, companyId)
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
    }

    private void linkUser(long providerId, long userId, OidcClient.Identity identity) {
        jdbcTemplate.update("""
                        INSERT INTO sys_user_sso (id, user_id, provider_id, subject, external_username,
                                                  last_login_at)
                        VALUES (?, ?, ?, ?, ?, now())
                        ON CONFLICT (provider_id, subject) DO NOTHING
                        """,
                IdWorker.getId(), userId, providerId,
                truncate(identity.subject(), 200), truncate(identity.username(), 128));
    }

    private long provision(long companyId, OidcClient.Identity identity, String defaultRole) {
        long userId = IdWorker.getId();
        String username = truncate(sanitizeUsername(identity.username()), 64);
        jdbcTemplate.update("""
                        INSERT INTO sys_user (id, company_id, username, password_hash, display_name,
                                              email, security_level, status, must_change_pwd,
                                              auth_provider, version, deleted, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, 0, 'ACTIVE', false, 'SSO', 0, false, now(), now())
                        """,
                userId, companyId, username,
                // 随机密码：SSO 账号不应能用本地口令登录
                passwordEncoder.encode(UUID.randomUUID().toString()),
                truncate(blankToDefault(identity.displayName(), username), 100),
                truncate(identity.email(), 128));
        if (defaultRole != null && !defaultRole.isBlank()) {
            jdbcTemplate.update("""
                    INSERT INTO sys_user_role (user_id, role_id)
                    SELECT ?, id FROM sys_role WHERE code = ?
                    ON CONFLICT DO NOTHING
                    """, userId, defaultRole);
        }
        return userId;
    }

    private Map<String, Object> requireProvider(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *, config::text AS config_text FROM sys_sso_provider
                 WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_NOT_FOUND, "SSO 配置不存在：" + id);
        }
        return rows.get(0);
    }

    private Map<String, Object> requireEnabled(String code) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *, config::text AS config_text FROM sys_sso_provider
                 WHERE code = ? AND deleted = false ORDER BY company_id LIMIT 1
                """, code);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_NOT_FOUND, "SSO 配置不存在：" + code);
        }
        if (!Boolean.TRUE.equals(rows.get(0).get("enabled"))) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_DISABLED);
        }
        return rows.get(0);
    }

    private SsoDtos.ProviderDetail toDetail(Map<String, Object> row) {
        String type = (String) row.get("type");
        Map<String, Object> config = readConfig(row.get("config_text"));
        List<String> secretFields = new ArrayList<>();
        for (String key : SECRET_KEYS.getOrDefault(type, List.of())) {
            Object value = config.remove(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                secretFields.add(key);
            }
        }
        return new SsoDtos.ProviderDetail(longOf(row.get("id")), (String) row.get("code"),
                (String) row.get("name"), type, Boolean.TRUE.equals(row.get("enabled")),
                Boolean.TRUE.equals(row.get("auto_provision")), (String) row.get("default_role_code"),
                (String) row.get("remark"), config, secretFields,
                offset(row.get("created_at")), offset(row.get("updated_at")));
    }

    private String requireType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        if (!TYPES.contains(value)) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID,
                    "类型只能是 OIDC 或 LDAP，收到 " + type);
        }
        return value;
    }

    /** 配置合法性与安全校验：地址只允许 http/https（OIDC）或 ldap/ldaps（LDAP）。 */
    private Map<String, Object> validateConfig(String type, Map<String, Object> config) {
        Map<String, Object> value = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
        if (TYPE_OIDC.equals(type)) {
            for (String key : List.of("issuer", "authorizationEndpoint", "tokenEndpoint",
                    "jwksEndpoint", "clientId", "redirectUri")) {
                if (blank(str(value.get(key)))) {
                    throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID, "OIDC 缺少必填项：" + key);
                }
            }
            for (String key : List.of("authorizationEndpoint", "tokenEndpoint", "jwksEndpoint",
                    "redirectUri")) {
                requireHttpUrl(str(value.get(key)), key);
            }
            value.put("usernameClaim", blankToDefault(str(value.get("usernameClaim")),
                    "preferred_username"));
            value.put("displayNameClaim", blankToDefault(str(value.get("displayNameClaim")), "name"));
            value.put("emailClaim", blankToDefault(str(value.get("emailClaim")), "email"));
        } else {
            String url = str(value.get("url"));
            if (blank(url) || !(url.startsWith("ldap://") || url.startsWith("ldaps://"))) {
                throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID,
                        "LDAP 地址必须以 ldap:// 或 ldaps:// 开头");
            }
            if (blank(str(value.get("attrUsername")))) {
                throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID,
                        "LDAP 缺少必填项：attrUsername");
            }
            if (blank(str(value.get("userFilter"))) && blank(str(value.get("userDnPattern")))) {
                throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID,
                        "LDAP 需要 userFilter 或 userDnPattern 之一");
            }
            if (blank(str(value.get("subjectAttr")))) {
                value.put("subjectAttr", "entryDN");
            }
        }
        return value;
    }

    private OidcClient.Config oidcConfig(Map<String, Object> config) {
        return new OidcClient.Config(str(config.get("issuer")),
                str(config.get("authorizationEndpoint")), str(config.get("tokenEndpoint")),
                str(config.get("jwksEndpoint")), str(config.get("clientId")),
                str(config.get("clientSecret")), str(config.get("redirectUri")),
                str(config.get("scope")), blankToDefault(str(config.get("usernameClaim")),
                "preferred_username"), blankToDefault(str(config.get("displayNameClaim")), "name"),
                blankToDefault(str(config.get("emailClaim")), "email"));
    }

    private LdapAuthenticator.Config ldapConfig(Map<String, Object> config) {
        return new LdapAuthenticator.Config(str(config.get("url")), blankToDefault(
                str(config.get("baseDn")), ""), str(config.get("bindDn")),
                str(config.get("bindCredential")), blankToDefault(str(config.get("userFilter")),
                "(uid={0})"), str(config.get("userDnPattern")), str(config.get("attrUsername")),
                blankToDefault(str(config.get("attrDisplayName")), "cn"),
                blankToDefault(str(config.get("attrEmail")), "mail"),
                blankToDefault(str(config.get("subjectAttr")), "entryDN"));
    }

    private Map<String, Object> readConfig(Object json) {
        if (json == null) {
            return new LinkedHashMap<>();
        }
        try {
            return jsonMapper.readValue(String.valueOf(json), new com.fasterxml.jackson.core.type
                    .TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID, "配置序列化失败");
        }
    }

    private void requireHttpUrl(String value, String field) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || !Set.of("http", "https").contains(uri.getScheme())) {
                throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID,
                        field + " 只支持 http/https：" + value);
            }
        } catch (IllegalArgumentException ex) {
            throw BizException.of(ErrorCode.SSO_PROVIDER_INVALID, field + " 不是合法地址：" + value);
        }
    }

    private static String sanitizeUsername(String value) {
        if (value == null || value.isBlank()) {
            return "sso_user";
        }
        String cleaned = value.trim().replaceAll("[^A-Za-z0-9._@-]", "_");
        return cleaned.isBlank() ? "sso_user" : cleaned;
    }

    private static OffsetDateTime offset(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant().atOffset(java.time.ZoneOffset.ofHours(8));
        }
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static long longOf(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
