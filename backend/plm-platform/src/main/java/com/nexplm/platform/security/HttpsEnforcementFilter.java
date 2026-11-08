package com.nexplm.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * HTTPS 强制（`docs/07` 待整改 ⑥）：应用层兜底，确保部署侧真走 TLS。
 *
 * <p>默认<b>关闭</b>（{@code nexplm.security.https.enforce=false}）：开发环境走 http，
 * 强开会把本地调试全重定向到不存在的 https。生产在前置 nginx 终止 TLS 时开启，
 * 未带 {@code X-Forwarded-Proto: https} 的请求一律 308 到 https 地址。</p>
 *
 * <p>健康检查与 info 端点跳过：探针通常走内网 http，重定向会让探针判死。</p>
 */
@Component
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    /** 探针/指标不强制 https（内网 http 探活是常态）。 */
    static final Set<String> SKIP_PREFIXES = Set.of("/actuator/health", "/actuator/info");
    private static final String HSTS = "max-age=31536000; includeSubDomains";

    private final boolean enforce;

    public HttpsEnforcementFilter(@Value("${nexplm.security.https.enforce:false}") boolean enforce) {
        this.enforce = enforce;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!enforce) {
            chain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        boolean secured = isSecure(request.isSecure(), request.getHeader("X-Forwarded-Proto"));
        if (secured || isSkipped(uri)) {
            if (secured) {
                response.setHeader("Strict-Transport-Security", HSTS);
            }
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.PERMANENT_REDIRECT.value());
        response.setHeader("Location", httpsUrl(request.getServerName(),
                request.getHeader("X-Forwarded-Host"), uri, request.getQueryString()));
    }

    /** {@code X-Forwarded-Proto} 由 nginx 注入；容器侧 {@code isSecure()} 在未配 forward-headers 时为 false。 */
    static boolean isSecure(boolean servletSecure, String forwardedProto) {
        return servletSecure || "https".equalsIgnoreCase(forwardedProto == null ? null : forwardedProto.trim());
    }

    static boolean isSkipped(String uri) {
        if (uri == null) {
            return false;
        }
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 目标地址：优先 {@code X-Forwarded-Host}（外部域名），否则用容器看到的 serverName。 */
    static String httpsUrl(String serverName, String forwardedHost, String uri, String query) {
        String host = forwardedHost == null || forwardedHost.isBlank() ? serverName : forwardedHost.trim();
        String path = uri == null || uri.isBlank() ? "/" : uri;
        String qs = query == null || query.isBlank() ? "" : "?" + query;
        return "https://" + host + path + qs;
    }
}
