package com.kb.wms.common.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Authorization 헤더의 Bearer 토큰이 유효하면 인증 주체를 SecurityContext에 올린다.
 * 토큰이 없거나 유효하지 않으면 아무것도 하지 않고 넘기므로, 거절 여부는 SecurityConfig의 경로 규칙이 정한다.
 * 필터 체인에 직접 넣어 쓰며 빈으로 등록하지 않는다(서블릿 필터로 중복 등록되지 않게).
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            jwtProvider.parse(header.substring(BEARER_PREFIX.length()).trim()).ifPresent(user ->
                    SecurityContextHolder.getContext().setAuthentication(
                            UsernamePasswordAuthenticationToken.authenticated(user, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())))));
        }
        filterChain.doFilter(request, response);
    }
}
