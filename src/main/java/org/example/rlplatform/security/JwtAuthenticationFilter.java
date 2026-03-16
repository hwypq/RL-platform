package org.example.rlplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.rlplatform.utils.JwtUtil;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 JWT 的认证过滤器：
 * - 从请求头中解析 token
 * - 校验 token，有效则把用户信息和角色放入 SecurityContext
 * - 同时兼容原有的 ThreadLocalUtil 用法
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // 登录、注册接口不需要解析 token
        if (path.startsWith("/user/login") || path.startsWith("/user/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }


        try {
            if (token != null && !token.isBlank()) {
                Map<String, Object> claims = JwtUtil.parseToken(token);
                // 兼容原有代码：把业务数据放入 ThreadLocal
                ThreadLocalUtil.set(claims);

                String username = (String) claims.get("username");
                Object roleObj = claims.get("role");
                String role = roleObj != null ? roleObj.toString() : null;

                List<GrantedAuthority> authorities = Collections.emptyList();
                if (role != null && !role.isBlank()) {
                    // Spring Security 里角色名需要以 ROLE_ 前缀
                    authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                }

                Integer userId = (Integer) claims.get("id");
                String tokenVersion = (String) claims.get("tokenVersion");

                if (userId != null || tokenVersion != null) {
                    String key = "login:version:" + userId;
                    String currentVersion = stringRedisTemplate.opsForValue().get(key);
                    if (currentVersion == null || !currentVersion.equals(tokenVersion)) {
                        SecurityContextHolder.clearContext();
                        throw new RuntimeException("token version is invalid");
                    }
                }

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            // token 非法时，清理上下文，但不直接终止链路，让后续处理返回 401
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } finally {
            // 防止 ThreadLocal 内存泄漏
            ThreadLocalUtil.remove();
        }
    }
}

