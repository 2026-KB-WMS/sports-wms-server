package com.kb.wms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.kb.wms.auth.domain.enums.UserRole;

class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider =
            new JwtProvider("test-only-jwt-secret-key-0123456789-0123456789", 3600);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);
    private final AuthenticatedUser user = new AuthenticatedUser(12L, UserRole.STORE_OWNER, List.of(), List.of(3L));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain run(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 인증 주체와 역할 권한을 올린다")
    void validToken() throws Exception {
        MockFilterChain chain = run("Bearer " + jwtProvider.createAccessToken(user));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_STORE_OWNER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 없으면 인증 없이 그대로 넘긴다")
    void noToken() throws Exception {
        MockFilterChain chain = run(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("유효하지 않은 토큰이나 Bearer가 아닌 헤더면 인증 없이 그대로 넘긴다")
    void invalidToken() throws Exception {
        MockFilterChain bad = run("Bearer not-a-jwt");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(bad.getRequest()).isNotNull();

        MockFilterChain basic = run("Basic abc");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(basic.getRequest()).isNotNull();
    }
}
