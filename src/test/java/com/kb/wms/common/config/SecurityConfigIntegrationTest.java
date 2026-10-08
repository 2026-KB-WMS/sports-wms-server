package com.kb.wms.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.JwtProvider;

/**
 * 인증 필터가 붙은 실제 필터 체인에서도 기존 엔드포인트가 계속 열려 있는지 확인한다.
 * 인증 필수로 전환(#170)할 때 이 테스트를 함께 바꾼다.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // 다른 통합 테스트 컨텍스트와 인메모리 DB 스키마가 섞이지 않도록 전용 DB를 쓴다(Category.depth가 MySQL 전용 타입이라 MySQL 모드).
        "spring.datasource.url=jdbc:h2:mem:security_config;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    // 인가를 가장 마지막에 적용하는 도메인(출고)의 조회 API. 도메인별 인가 적용(#170) 순서가 바뀌면 아직 열려 있는 API로 옮긴다.
    private static final String OPEN_ENDPOINT = "/api/v1/outbounds";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("토큰이 없어도 기존 엔드포인트를 호출할 수 있다")
    void withoutToken() throws Exception {
        mockMvc.perform(get(OPEN_ENDPOINT)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효하지 않은 토큰이어도 기존 엔드포인트는 거절하지 않는다")
    void withInvalidToken() throws Exception {
        mockMvc.perform(get(OPEN_ENDPOINT).header("Authorization", "Bearer not-a-jwt")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 토큰이면 기존 엔드포인트를 호출할 수 있다")
    void withValidToken() throws Exception {
        String token = jwtProvider.createAccessToken(
                new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of()));

        mockMvc.perform(get(OPEN_ENDPOINT).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    private String bearer(UserRole role) {
        return "Bearer " + jwtProvider.createAccessToken(new AuthenticatedUser(1L, role, List.of(), List.of()));
    }

    @Test
    @DisplayName("/users는 토큰이 없으면 401, HQ_ADMIN이 아니면 403, HQ_ADMIN이면 통과한다")
    void usersRequireHqAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(UserRole.STORE_OWNER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users").header("Authorization", bearer(UserRole.HQ_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/auth/me는 토큰이 없으면 401이고, 유효한 토큰이면 토큰 주체로 조회한다")
    void meRequiresToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        // 토큰의 사용자(ID 1)가 DB에 없으면 404 USER_NOT_FOUND: 필터가 만든 인증 주체가 컨트롤러까지 전달됐다는 뜻이다.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(UserRole.WAREHOUSE_MANAGER)))
                .andExpect(status().isNotFound());
    }
}
