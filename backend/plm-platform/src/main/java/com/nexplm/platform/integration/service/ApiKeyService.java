package com.nexplm.platform.integration.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.PageResult;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.integration.api.OpenApiDtos;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key 管理 + 校验 + 限流（P9-3）。
 *
 * <p>几条安全上的取舍：</p>
 * <ul>
 *   <li><b>只存 sha256，明文只出现一次</b>：创建/轮换时返回一次，之后任何人都取不回来；</li>
 *   <li><b>走库校验而不是 JWT</b>：外部 key 泄漏必须能立刻失效，无状态令牌做不到；
 *       停用/过期改一行就生效；</li>
 *   <li><b>prefix 只做索引线索</b>：校验时先用前 12 位定位候选行，再比对 hash；
 *       没有 prefix 就只能全表逐行算 hash；</li>
 *   <li><b>scopes 映射成权限字面量</b>：{@code object.read} → {@code open.object.read}，
 *       交给 {@code @PreAuthorize} 判定，不另造一套鉴权；</li>
 *   <li><b>限流是单机内存计数</b>：多实例部署会各算各的，需要 Redis；这里不假装解决。</li>
 * </ul>
 */
@Service
public class ApiKeyService {

    public record ApiClient(Long id, Long companyId, String code, String name,
                            List<String> scopes, int rateLimitPerMin) {
    }

    /** 允许的作用域白名单：不在白名单里的字符串一律拒绝，避免写成 {@code *}。 */
    private static final Set<String> ALLOWED_SCOPES =
            Set.of("object.read", "file.read", "file.write");
    private static final String KEY_PREFIX = "nlk_";
    private static final int PREFIX_LENGTH = 12;
    private static final int MAX_RATE_LIMIT = 10000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();
    /** clientId → [分钟序号, 计数]，固定窗口即可满足"防止刷接口"，不需要令牌桶精度。 */
    private final ConcurrentHashMap<Long, long[]> buckets = new ConcurrentHashMap<>();

    public ApiKeyService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /* ================================================================ 管理 */

    public PageResult<OpenApiDtos.ApiClientRow> page(Long companyId, String keyword,
                                                     Integer page, Integer size) {
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
        int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 200);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM itg_api_client" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(pageSize);
        pageArgs.add((pageNo - 1) * pageSize);
        List<OpenApiDtos.ApiClientRow> items = jdbcTemplate.query("""
                        SELECT * FROM itg_api_client
                        """ + where + " ORDER BY code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new OpenApiDtos.ApiClientRow(rs.getLong("id"),
                        rs.getString("code"), rs.getString("name"), rs.getString("key_prefix"),
                        readScopes(rs.getString("scopes")), rs.getInt("rate_limit_per_min"),
                        rs.getBoolean("enabled"), textOf(rs.getObject("expires_at")),
                        rs.getObject("last_used_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class)),
                pageArgs.toArray());
        return PageResult.of(items, total == null ? 0 : total, pageNo, pageSize);
    }

    public OpenApiDtos.ApiClientDetail detail(Long id, Long companyId) {
        Map<String, Object> row = requireClient(id, companyId);
        return new OpenApiDtos.ApiClientDetail(id, str(row.get("code")), str(row.get("name")),
                str(row.get("key_prefix")), readScopes(str(row.get("scopes"))),
                intOf(row.get("rate_limit_per_min")), Boolean.TRUE.equals(row.get("enabled")),
                textOf(row.get("expires_at")),
                row.get("last_used_at") instanceof OffsetDateTime used ? used : null,
                str(row.get("remark")));
    }

    @Transactional
    public OpenApiDtos.ApiClientCreated create(OpenApiDtos.ApiClientSaveReq req, Long companyId,
                                               Long userId) {
        String code = req.code().trim();
        String secret = newSecret();
        String prefix = secret.substring(0, PREFIX_LENGTH);
        try {
            jdbcTemplate.update("""
                            INSERT INTO itg_api_client (id, company_id, code, name, key_prefix,
                                                        key_hash, scopes, rate_limit_per_min,
                                                        enabled, expires_at, remark, created_by,
                                                        updated_by)
                            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, CAST(? AS date), ?, ?, ?)
                            """,
                    IdWorker.getId(), companyId, code, req.name().trim(), prefix, sha256(secret),
                    scopesJson(req.scopes()), rateLimit(req.rateLimitPerMin()),
                    req.enabled() == null || req.enabled(), normalizeDate(req.expiresAt()),
                    blankToNull(req.remark()), userId, userId);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.ITG_CLIENT_INVALID,
                    "API 客户端编码已存在：" + code);
        }
        Long id = jdbcTemplate.queryForObject("""
                SELECT id FROM itg_api_client WHERE company_id = ? AND code = ? AND deleted = false
                """, Long.class, companyId, code);
        return new OpenApiDtos.ApiClientCreated(detail(id, companyId), secret);
    }

    @Transactional
    public OpenApiDtos.ApiClientDetail update(Long id, OpenApiDtos.ApiClientSaveReq req,
                                              Long companyId, Long userId) {
        requireClient(id, companyId);
        jdbcTemplate.update("""
                        UPDATE itg_api_client
                           SET name = ?, scopes = ?::jsonb, rate_limit_per_min = ?, enabled = ?,
                               expires_at = CAST(? AS date), remark = ?, updated_by = ?,
                               updated_at = now()
                         WHERE id = ? AND company_id = ? AND deleted = false
                        """,
                req.name().trim(), scopesJson(req.scopes()), rateLimit(req.rateLimitPerMin()),
                req.enabled() == null || req.enabled(), normalizeDate(req.expiresAt()),
                blankToNull(req.remark()), userId, id, companyId);
        return detail(id, companyId);
    }

    /** 轮换：旧 key 立即失效（hash 被覆盖），返回新明文一次。 */
    @Transactional
    public OpenApiDtos.ApiClientCreated rotate(Long id, Long companyId, Long userId) {
        requireClient(id, companyId);
        String secret = newSecret();
        jdbcTemplate.update("""
                        UPDATE itg_api_client
                           SET key_prefix = ?, key_hash = ?, updated_by = ?, updated_at = now()
                         WHERE id = ? AND company_id = ?
                        """,
                secret.substring(0, PREFIX_LENGTH), sha256(secret), userId, id, companyId);
        return new OpenApiDtos.ApiClientCreated(detail(id, companyId), secret);
    }

    @Transactional
    public void delete(Long id, Long companyId) {
        requireClient(id, companyId);
        jdbcTemplate.update("""
                UPDATE itg_api_client SET deleted = true, enabled = false, updated_at = now()
                 WHERE id = ? AND company_id = ?
                """, id, companyId);
    }

    /* ================================================================ 校验 */

    public ApiClient authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank() || !rawKey.startsWith(KEY_PREFIX)) {
            throw BizException.of(ErrorCode.OPEN_KEY_INVALID, "API Key 无效");
        }
        String key = rawKey.trim();
        if (key.length() < PREFIX_LENGTH) {
            throw BizException.of(ErrorCode.OPEN_KEY_INVALID, "API Key 无效");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_api_client WHERE key_prefix = ? AND deleted = false
                """, key.substring(0, PREFIX_LENGTH));
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.OPEN_KEY_INVALID, "API Key 无效");
        }
        Map<String, Object> row = rows.get(0);
        // 定长比较：hash 不同长度也要走完，避免用长度差异做侧信道
        if (!constantTimeEquals(str(row.get("key_hash")), sha256(key))) {
            throw BizException.of(ErrorCode.OPEN_KEY_INVALID, "API Key 无效");
        }
        if (!Boolean.TRUE.equals(row.get("enabled"))) {
            throw BizException.of(ErrorCode.OPEN_KEY_INVALID, "API Key 已停用");
        }
        // 过期时间用 SQL 里查出来的值判定（不同 JDBC 驱动可能给 Date/LocalDate/String，
        // 统一转成 yyyy-MM-dd 再比，避免因返回类型不同把过期判断漏掉）
        String expires = textOf(row.get("expires_at"));
        if (expires != null && !expires.isBlank()) {
            if (LocalDate.parse(expires.substring(0, 10)).isBefore(LocalDate.now())) {
                throw BizException.of(ErrorCode.OPEN_KEY_EXPIRED, "API Key 已于 " + expires + " 过期");
            }
        }
        return new ApiClient(longOf(row.get("id")), longOf(row.get("company_id")),
                str(row.get("code")), str(row.get("name")),
                readScopes(str(row.get("scopes"))), intOf(row.get("rate_limit_per_min")));
    }

    /** 按编码取客户端（供 whoami 展示限流配额），不存在返回 null。 */
    public ApiClient byCode(Long companyId, String code) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_api_client
                 WHERE company_id = ? AND code = ? AND deleted = false
                """, companyId, code);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        return new ApiClient(longOf(row.get("id")), longOf(row.get("company_id")),
                str(row.get("code")), str(row.get("name")),
                readScopes(str(row.get("scopes"))), intOf(row.get("rate_limit_per_min")));
    }

    public void touch(Long clientId) {
        try {
            jdbcTemplate.update("UPDATE itg_api_client SET last_used_at = now() WHERE id = ?",
                    clientId);
        } catch (RuntimeException ignored) {
            // 记录使用时间失败不影响请求
        }
    }

    /** 固定窗口限流：同 client 一分钟内超过配额即拒绝。 */
    public boolean tryAcquire(Long clientId, int limit) {
        long minute = System.currentTimeMillis() / 60000L;
        long[] cell = buckets.compute(clientId, (key, current) -> {
            if (current == null || current[0] != minute) {
                return new long[]{minute, 1};
            }
            current[1] = current[1] + 1;
            return current;
        });
        return cell[1] <= Math.max(1, limit);
    }

    /* ================================================================ 内部 */

    private Map<String, Object> requireClient(Long id, Long companyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM itg_api_client WHERE id = ? AND company_id = ? AND deleted = false
                """, id, companyId);
        if (rows.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_CLIENT_NOT_FOUND, "API 客户端不存在：" + id);
        }
        return rows.get(0);
    }

    private String newSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private String scopesJson(List<String> scopes) {
        List<String> normalized = new ArrayList<>();
        for (String scope : scopes) {
            String value = scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_SCOPES.contains(value)) {
                throw BizException.of(ErrorCode.ITG_CLIENT_INVALID,
                        "未知作用域：" + scope + "（可选 " + String.join("/", ALLOWED_SCOPES) + "）");
            }
            if (!normalized.contains(value)) {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty()) {
            throw BizException.of(ErrorCode.ITG_CLIENT_INVALID, "至少需要一个作用域");
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw BizException.of(ErrorCode.ITG_CLIENT_INVALID, "作用域序列化失败");
        }
    }

    private List<String> readScopes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static int rateLimit(Integer value) {
        int limit = value == null ? 60 : value;
        if (limit < 1 || limit > MAX_RATE_LIMIT) {
            throw BizException.of(ErrorCode.ITG_CLIENT_INVALID,
                    "限流需在 1~" + MAX_RATE_LIMIT + " 次/分钟之间");
        }
        return limit;
    }

    private static String normalizeDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate.parse(date.trim());
        } catch (RuntimeException ex) {
            throw BizException.of(ErrorCode.ITG_CLIENT_INVALID, "日期格式应为 YYYY-MM-DD：" + date);
        }
        return date.trim();
    }

    private static String textOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        return String.valueOf(value);
    }

    private static int intOf(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long longOf(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
