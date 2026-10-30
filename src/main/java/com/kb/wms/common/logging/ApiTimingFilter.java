package com.kb.wms.common.logging;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * API별 실행시간과 SQL 실행 횟수를 한 줄로 남긴다.
 * 보안 필터 체인보다 먼저 실행되어 JWT 검증 시간까지 포함한다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiTimingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        QueryCountInspector.reset();
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            // 컨트롤러에 닿기 전에 막힌 요청(401 등)은 패턴이 없어 실제 URI를 쓴다.
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            log.info("{} {} -> {} ({}ms, queries={})", request.getMethod(),
                    pattern != null ? pattern : request.getRequestURI(),
                    response.getStatus(), ms, QueryCountInspector.count());
        }
    }
}
