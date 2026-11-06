package com.nexplm.platform.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.Result;
import com.nexplm.common.exception.BizException;
import com.nexplm.platform.integration.service.ApiKeyService;
import com.nexplm.platform.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 开放平台认证：{@code X-API-Key} → 客户端身份 + 作用域权限 + 每分钟限流。
 *
 * <p>与站内 JWT 的关系：两条路互不干扰。带 X-API-Key 的请求走这里（JWT 过滤器因无
 * Authorization 头会直接放行）；站内请求照旧。API 客户端拿到的权限是
 * {@code open.<scope>}，所以它只能访问挂了对应 {@code @PreAuthorize} 的开放接口，
 * 不会因为"有 key"就顺带能调站内管理接口。</p>
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    /** 虚拟用户 id：API 客户端不是真实用户，审计里用 0 明确标注来源。 */
    private static final long VIRTUAL_USER_ID = 0L;

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        ApiKeyService.ApiClient client;
        try {
            client = apiKeyService.authenticate(rawKey);
        } catch (BizException ex) {
            write(response, ex.getErrorCode().httpStatus(), ex.getErrorCode().code(),
                    ex.getMessage());
            return;
        }
        if (!apiKeyService.tryAcquire(client.id(), client.rateLimitPerMin())) {
            write(response, 429, "OPEN.RATE.LIMITED", "请求过于频繁，请稍后重试");
            return;
        }
        apiKeyService.touch(client.id());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        client.scopes().forEach(scope -> authorities.add(
                new SimpleGrantedAuthority("open." + scope)));
        UserPrincipal principal = new UserPrincipal(VIRTUAL_USER_ID,
                "apikey:" + client.code(), client.name(), client.companyId(),
                List.of("API"), client.scopes().stream().map(scope -> "open." + scope)
                        .collect(java.util.stream.Collectors.toSet()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void write(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
    }
}
