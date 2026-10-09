package com.kb.wms.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.kb.wms.auth.domain.enums.UserRole;

/**
 * 컨트롤러 테스트에서 토큰 없이 인증 주체를 요청에 싣는다. 필터를 끈 {@code @WebMvcTest}에서도
 * {@code @AuthenticationPrincipal AuthenticatedUser}가 채워진다.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor as(UserRole role, Long userId, List<Long> warehouseIds, List<Long> storeIds) {
        AuthenticatedUser user = new AuthenticatedUser(userId, role, warehouseIds, storeIds);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        RequestPostProcessor withFilters = authentication(auth);
        // 필터를 끈 테스트(addFilters=false)에서는 SecurityContextHolderFilter가 없어 요청에 실은 인증이 읽히지 않으므로 홀더에도 직접 넣는다.
        // 테스트가 끝나면 spring-security-test의 리스너가 홀더를 비운다.
        return request -> {
            TestSecurityContextHolder.setAuthentication(auth);
            return withFilters.postProcessRequest(request);
        };
    }

    /** 필터를 끈 컨트롤러 테스트에서 기본 사용자를 지정한 역할·소속으로 둔다. {@code @BeforeEach}에서 호출한다. */
    public static void signInAs(UserRole role, Long userId, List<Long> warehouseIds, List<Long> storeIds) {
        TestSecurityContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, role, warehouseIds, storeIds), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    /** 필터를 끈 컨트롤러 테스트에서 기본 사용자를 본사 관리자로 둔다. {@code @BeforeEach}에서 호출한다. */
    public static void signInAsHqAdmin() {
        TestSecurityContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of()), null,
                List.of(new SimpleGrantedAuthority("ROLE_HQ_ADMIN"))));
    }

    public static RequestPostProcessor hqAdmin() {
        return as(UserRole.HQ_ADMIN, 1L, List.of(), List.of());
    }

    public static RequestPostProcessor warehouseManager(Long userId, Long... warehouseIds) {
        return as(UserRole.WAREHOUSE_MANAGER, userId, Arrays.asList(warehouseIds), List.of());
    }

    public static RequestPostProcessor storeOwner(Long userId, Long... storeIds) {
        return as(UserRole.STORE_OWNER, userId, List.of(), Arrays.asList(storeIds));
    }
}
