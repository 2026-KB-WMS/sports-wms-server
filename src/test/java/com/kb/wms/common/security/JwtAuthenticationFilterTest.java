package com.kb.wms.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

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

    private static final Long USER_ID = 12L;

    private final JwtProvider jwtProvider =
            new JwtProvider("test-only-jwt-secret-key-0123456789-0123456789", 3600);
    private final AuthenticatedUser user = new AuthenticatedUser(USER_ID, UserRole.STORE_OWNER, List.of(), List.of(3L));
    // 리졸버가 돌려주는 "현재 DB 기준 사용자"를 테스트에서 바꿀 수 있게 둔다. USER_ID만 존재한다.
    private AuthenticatedUser current = user;
    private final AuthenticatedUserResolver resolver =
            userId -> USER_ID.equals(userId) ? Optional.ofNullable(current) : Optional.empty();
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider, resolver);

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

    private AuthenticatedUser principal() {
        return (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 리졸버가 읽은 사용자와 역할 권한을 올린다")
    void validToken() throws Exception {
        MockFilterChain chain = run("Bearer " + jwtProvider.createAccessToken(USER_ID));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_STORE_OWNER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("같은 토큰이라도 사용자의 소속이 바뀌면 다음 요청부터 바뀐 값이 반영된다")
    void reflectsChangedAffiliation() throws Exception {
        String token = "Bearer " + jwtProvider.createAccessToken(USER_ID);
        run(token);
        assertThat(principal().storeIds()).containsExactly(3L);

        SecurityContextHolder.clearContext();
        current = new AuthenticatedUser(USER_ID, UserRole.STORE_OWNER, List.of(), List.of(7L));
        run(token);

        assertThat(principal().storeIds()).containsExactly(7L);
    }

    @Test
    @DisplayName("토큰은 유효해도 사용자를 찾을 수 없거나 비활성이면 인증 없이 그대로 넘긴다")
    void unresolvableUser() throws Exception {
        MockFilterChain unknown = run("Bearer " + jwtProvider.createAccessToken(99L));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(unknown.getRequest()).isNotNull();

        current = null;
        run("Bearer " + jwtProvider.createAccessToken(USER_ID));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
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
