package com.nexplm.platform.security;

import com.nexplm.common.api.Result;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 方法级鉴权拒绝（{@code @PreAuthorize} 不通过）返回 403，而不是 500。
 *
 * <p>{@code @PreAuthorize} 由 AOP 在 Controller 方法调用前抛出 {@code AccessDeniedException}，
 * 它会先进入 {@code @RestControllerAdvice} 的异常解析链；{@code com.nexplm.common} 里那个
 * {@code Exception} 兜底 handler 会把它当未知异常吞成 500——前端就会把「没权限」误判为服务端故障。
 * 这里用最高优先级单独处理，把语义纠正回 403。</p>
 *
 * <p>放在 platform 而非 common：common 是零依赖底座，不引 spring-security。</p>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessDeniedExceptionHandler {

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(ErrorCode.COMMON_FORBIDDEN.code(),
                        ErrorCode.COMMON_FORBIDDEN.message()));
    }
}
