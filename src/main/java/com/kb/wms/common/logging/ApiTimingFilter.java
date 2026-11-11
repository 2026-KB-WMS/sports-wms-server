package com.kb.wms.common.logging;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
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
 * 설정으로 실행한 SQL 목록({@code wms.api-log.show-queries})과 화면 전환 구분선
 * ({@code wms.api-log.screen-separator}, 프런트가 보내는 {@code X-Screen} 헤더 기준)을 덧붙일 수 있다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiTimingFilter extends OncePerRequestFilter {

    static final String SCREEN_HEADER = "X-Screen";
    private static final int SCREEN_MAX_LENGTH = 50;

    private final boolean showQueries;
    private final boolean screenSeparator;
    private final AtomicReference<String> lastScreen = new AtomicReference<>();

    public ApiTimingFilter(@Value("${wms.api-log.show-queries:false}") boolean showQueries,
                           @Value("${wms.api-log.screen-separator:false}") boolean screenSeparator) {
        this.showQueries = showQueries;
        this.screenSeparator = screenSeparator;
    }

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
            StringBuilder message = new StringBuilder();
            appendScreenSeparator(message, request);
            message.append(request.getMethod()).append(' ')
                    .append(pattern != null ? pattern : request.getRequestURI())
                    .append(" -> ").append(response.getStatus())
                    .append(" (").append(ms).append("ms, queries=").append(QueryCountInspector.count()).append(')');
            if (showQueries) {
                appendQueries(message, QueryCountInspector.statements());
            }
            log.info("{}", message);
        }
    }

    /** 직전에 구분선을 찍은 화면과 다를 때만 찍는다. 헤더가 없으면 구분하지 않는다. */
    private void appendScreenSeparator(StringBuilder message, HttpServletRequest request) {
        if (!screenSeparator) {
            return;
        }
        String screen = sanitize(request.getHeader(SCREEN_HEADER));
        if (!screen.isEmpty() && !screen.equals(lastScreen.getAndSet(screen))) {
            message.append("========== ").append(screen).append(" ==========\n");
        }
    }

    /** 로그 줄을 깨뜨리지 않도록 식별자에 쓰는 문자만 남긴다. */
    private static String sanitize(String header) {
        if (header == null) {
            return "";
        }
        String cleaned = header.replaceAll("[^A-Za-z0-9._:/-]", "");
        return cleaned.length() > SCREEN_MAX_LENGTH ? cleaned.substring(0, SCREEN_MAX_LENGTH) : cleaned;
    }

    /** 바인딩 값은 들어 있지 않고 {@code ?}로 나온다. */
    private static void appendQueries(StringBuilder message, List<String> statements) {
        for (int i = 0; i < statements.size(); i++) {
            message.append("\n  [").append(i + 1).append("] ").append(statements.get(i).replaceAll("\s+", " "));
        }
    }
}
