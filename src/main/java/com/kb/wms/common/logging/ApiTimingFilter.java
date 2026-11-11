package com.kb.wms.common.logging;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * 설정으로 실행한 SQL 목록({@code wms.api-log.show-queries}, 긴 select 컬럼 목록은 줄이고
 * {@code wms.api-log.full-queries}로 전체를 본다)과 화면 전환 구분선
 * ({@code wms.api-log.screen-separator}, 프런트가 보내는 {@code X-Screen} 헤더 기준)을 덧붙일 수 있다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiTimingFilter extends OncePerRequestFilter {

    static final String SCREEN_HEADER = "X-Screen";
    private static final int SCREEN_MAX_LENGTH = 50;
    private static final int SELECT_LIST_MAX_LENGTH = 60;
    private static final Pattern SELECT_FROM = Pattern.compile("^(select )(.+?)( from .*)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final boolean showQueries;
    private final boolean fullQueries;
    private final boolean screenSeparator;
    private final AtomicReference<String> lastScreen = new AtomicReference<>();

    public ApiTimingFilter(@Value("${wms.api-log.show-queries:false}") boolean showQueries,
                           @Value("${wms.api-log.full-queries:false}") boolean fullQueries,
                           @Value("${wms.api-log.screen-separator:false}") boolean screenSeparator) {
        this.showQueries = showQueries;
        this.fullQueries = fullQueries;
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
            String newScreen = changedScreen(request);
            if (newScreen != null) {
                // 구분선을 요약 줄과 합치면 시각 접두사가 구분선에 붙고 요약 줄이 밀려서 따로 남긴다.
                log.info("========== {} ==========", newScreen);
            }
            // 컨트롤러에 닿기 전에 막힌 요청(401 등)은 패턴이 없어 실제 URI를 쓴다.
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            StringBuilder message = new StringBuilder();
            message.append(request.getMethod()).append(' ')
                    .append(pattern != null ? pattern : request.getRequestURI())
                    .append(" -> ").append(response.getStatus())
                    .append(" (").append(ms).append("ms, queries=").append(QueryCountInspector.count()).append(')');
            if (showQueries) {
                appendQueries(message, QueryCountInspector.statements());
                message.append('\n'); // 로거가 줄바꿈을 한 번 더 붙여 요청 사이에 빈 줄이 생긴다.
            }
            log.info("{}", message);
        }
    }

    /** 직전에 구분선을 찍은 화면과 다를 때만 새 화면 이름을 돌려준다. 헤더가 없으면 구분하지 않는다(null). */
    private String changedScreen(HttpServletRequest request) {
        if (!screenSeparator) {
            return null;
        }
        String screen = sanitize(request.getHeader(SCREEN_HEADER));
        return !screen.isEmpty() && !screen.equals(lastScreen.getAndSet(screen)) ? screen : null;
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
    private void appendQueries(StringBuilder message, List<String> statements) {
        for (int i = 0; i < statements.size(); i++) {
            String sql = statements.get(i).replaceAll("\\s+", " ");
            message.append("\n  [").append(i + 1).append("] ").append(fullQueries ? sql : abbreviateSelect(sql));
        }
    }

    /** 컬럼이 수십 개인 select 목록은 어느 테이블을 조회하는지 가리므로 {@code select ... from}으로 줄인다. */
    static String abbreviateSelect(String sql) {
        Matcher matcher = SELECT_FROM.matcher(sql);
        if (matcher.matches() && matcher.group(2).length() > SELECT_LIST_MAX_LENGTH) {
            return matcher.group(1) + "..." + matcher.group(3);
        }
        return sql;
    }
}
