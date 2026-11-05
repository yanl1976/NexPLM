package com.nexplm.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.api.Result;
import com.nexplm.common.exception.BizException;
import com.nexplm.platform.iam.domain.SysUser;
import com.nexplm.platform.iam.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 解析 Authorization: Bearer &lt;access token&gt;，加载用户权限，构建安全上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, SysUserMapper userMapper, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(PREFIX.length()).trim();
        try {
            Claims claims = jwtService.parseAndRequireType(token, JwtService.TYPE_ACCESS);
            Long userId = jwtService.userId(claims);
            SysUser user = userMapper.selectById(userId);
            if (user == null || !"ACTIVE".equals(user.getStatus())) {
                throw BizException.of(com.nexplm.common.exception.ErrorCode.AUTH_ACCOUNT_DISABLED);
            }
            List<String> roles = userMapper.selectRoleCodes(userId);
            Set<String> permissions = new HashSet<>(userMapper.selectPermissionCodes(userId));
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));

            UserPrincipal principal = new UserPrincipal(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getCompanyId(), roles, permissions);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (BizException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, ex);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, BizException ex) throws IOException {
        response.setStatus(ex.getErrorCode().httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(ex.getErrorCode().code(), ex.getMessage())));
    }
}
