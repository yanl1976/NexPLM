package com.nexplm.platform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p>P0 只落基础字段；哈希链与分区归档在 P1 随审计模块完善。</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private static final int MAX_DETAIL = 3800;

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
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            success = false;
            error = ex.getMessage();
            throw ex;
        } finally {
            try {
                write(audit, joinPoint, success, error, System.currentTimeMillis() - start);
            } catch (Exception ex) {
                log.warn("写审计日志失败: {}", ex.getMessage());
            }
        }
    }

    private void write(Audit audit, ProceedingJoinPoint joinPoint, boolean success, String error, long costMs) {
        UserPrincipal principal = CurrentUser.orNull();
        String detail = buildDetail(joinPoint, audit.recordArgs(), error, costMs);
        HttpServletRequest request = currentRequest();
        jdbcTemplate.update("""
                        INSERT INTO sys_audit_log
                          (trace_id, user_id, org_id, module, action, object_type, object_id,
                           result, detail, ip, ua, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                MDC.get("traceId"),
                principal == null ? null : principal.userId(),
                principal == null ? null : principal.companyId(),
                audit.module(),
                audit.action(),
                audit.objectType().isBlank() ? null : audit.objectType(),
                null,
                success ? "SUCCESS" : "FAIL",
                truncate(detail),
                request == null ? null : clientIp(request),
                request == null ? null : truncate(request.getHeader("User-Agent")),
                OffsetDateTime.now());
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
