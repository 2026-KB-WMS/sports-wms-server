package com.kb.wms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.json.JsonMapper;

class RestSecurityExceptionHandlerTest {

    private final RestSecurityExceptionHandler handler = new RestSecurityExceptionHandler(JsonMapper.builder().build());

    @Test
    @DisplayName("인증 실패는 공통 오류 포맷의 401 JSON으로 응답한다")
    void unauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"success\":false", "\"statusCode\":401", "\"errorCode\":\"UNAUTHORIZED\"", "\"errors\":[]");
    }

    @Test
    @DisplayName("권한 부족은 공통 오류 포맷의 403 JSON으로 응답한다")
    void forbidden() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"statusCode\":403", "\"errorCode\":\"FORBIDDEN\"");
    }
}
