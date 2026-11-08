package com.nexplm.platform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.Arrays;

/**
 * 审计切面：记录"谁在什么时间对什么做了什么"，成功与失败都记录。
 * <p>对象标识（object_id）用于「对象查看器 · 日志」页签按对象回溯，
 * 取值顺序：返回值中含 {@code id()} 的业务 DTO → 方法第一个 Long 参数（约定为路径中的对象 ID）。</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int MAX_DETAIL = 3800;
    /** 链式哈希的写入串行化（进程内）；多实例需换数据库行锁，见 docs/03 B22。 */
    private static final Object CHAIN_LOCK = new Object();

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditAspect(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String error = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            error = ex.getMessage();
            throw ex;
        } finally {
            try {
                write(audit, joinPoint, result, success, error, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("写审计日志失败: {}", ex.getMessage());
            }
        }
    }

    private void write(Audit audit, ProceedingJoinPoint joinPoint, Object result,
                       boolean success, String error, long costMs) {
        UserPrincipal principal = CurrentUser.orNull();
        String detail = truncate(buildDetail(joinPoint, audit.recordArgs(), error, costMs));
        HttpServletRequest request = currentRequest();
        String module = audit.module();
        String action = audit.action();
        String objectType = audit.objectType().isBlank() ? null : audit.objectType();
        Long objectId = objectType == null ? null : resolveObjectId(joinPoint, result);
        String rowResult = success ? "SUCCESS" : "FAIL";
        Long userId = principal == null ? null : principal.userId();
        String payload = AuditChain.payload(module, action, objectType, objectId, rowResult, userId, detail);

        // 链式哈希（docs/07 待整改 ⑤）：本行 hash = sha256(上一行 hash + 本行内容)。
        // 取"上一行"与插入必须在同一把锁里，否则并发写会取到同一个 prev 导致链分叉；
        // 多实例部署时这把进程内锁不够，需改用数据库行锁/advisory lock（见 03 B22）。
        synchronized (CHAIN_LOCK) {
            String prevHash = jdbcTemplate.query("""
                            SELECT hash FROM sys_audit_log
                             WHERE hash IS NOT NULL
                             ORDER BY id DESC
                             LIMIT 1
                            """,
                    rs -> rs.next() ? rs.getString(1) : null);
            String hash = AuditChain.hash(prevHash, payload);
            jdbcTemplate.update("""
                            INSERT INTO sys_audit_log
                              (trace_id, user_id, org_id, module, action, object_type, object_id,
                               result, detail, ip, ua, prev_hash, hash, created_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    MDC.get("traceId"), userId,
                    principal == null ? null : principal.companyId(),
                    module, action, objectType, objectId, rowResult, detail,
                    request == null ? null : clientIp(request),
                    request == null ? null : truncate(request.getHeader("User-Agent")),
                    prevHash, hash, OffsetDateTime.now());
        }
    }

    /** 对象 ID：优先取返回值 DTO 的 id，其次取方法第一个 Long 参数。 */
    private Long resolveObjectId(ProceedingJoinPoint joinPoint, Object result) {
        Long fromResult = extractId(result);
        if (fromResult != null) {
            return fromResult;
        }
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long value && value > 0) {
                return value;
            }
        }
        return null;
    }

    private Long extractId(Object result) {
        if (!(result instanceof Result<?> wrapper) || wrapper.data() == null) {
            return null;
        }
        Object data = wrapper.data();
        if (data instanceof Number) {
            return null;
        }
        try {
            Object id = data.getClass().getMethod("id").invoke(data);
            if (id instanceof Number number && number.longValue() > 0) {
                return number.longValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // 返回值不含 id（列表/布尔/无 id 的 DTO）时回退到参数
        }
        return null;
    }

    private String buildDetail(ProceedingJoinPoint joinPoint, boolean recordArgs, String error, long costMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("method=").append(joinPoint.getSignature().toShortString());
        sb.append(", costMs=").append(costMs);
        if (error != null) {
            sb.append(", error=").append(error);
        }
        if (recordArgs) {
            try {
                sb.append(", args=").append(objectMapper.writeValueAsString(
                        Arrays.stream(joinPoint.getArgs())
                                .filter(a -> a instanceof CharSequence || a instanceof Number || a instanceof Boolean)
                                .toList()));
            } catch (Exception ignored) {
                sb.append(", args=<unserializable>");
            }
        }
        return sb.toString();
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }
}
