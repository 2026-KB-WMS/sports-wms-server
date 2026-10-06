package com.kb.wms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.auth.domain.enums.UserRole;

class JwtProviderTest {

    private static final String SECRET = "test-only-jwt-secret-key-0123456789-0123456789";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600);
    private final AuthenticatedUser user =
            new AuthenticatedUser(12L, UserRole.WAREHOUSE_MANAGER, List.of(1L, 2L), List.of());

    @Test
    @DisplayName("발급한 토큰에서 사용자 ID·역할·소속 ID를 그대로 복원한다")
    void createAndParse() {
        String token = jwtProvider.createAccessToken(user);

        Optional<AuthenticatedUser> parsed = jwtProvider.parse(token);

        assertThat(parsed).contains(user);
    }

    @Test
    @DisplayName("소속이 없으면 빈 목록으로 복원한다")
    void createAndParse_withoutAffiliation() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

        assertThat(jwtProvider.parse(jwtProvider.createAccessToken(admin))).contains(admin);
    }

    @Test
    @DisplayName("만료된 토큰은 복원하지 않는다")
    void parse_expired() {
        JwtProvider alreadyExpired = new JwtProvider(SECRET, -1);

        assertThat(jwtProvider.parse(alreadyExpired.createAccessToken(user))).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명했거나 변조한 토큰은 복원하지 않는다")
    void parse_invalidSignature() {
        JwtProvider otherKey = new JwtProvider("other-jwt-secret-key-0123456789-0123456789-ab", 3600);
        String token = jwtProvider.createAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwtProvider.parse(otherKey.createAccessToken(user))).isEmpty();
        assertThat(jwtProvider.parse(tampered)).isEmpty();
    }

    @Test
    @DisplayName("JWT 형식이 아닌 값은 복원하지 않는다")
    void parse_garbage() {
        assertThat(jwtProvider.parse("not-a-jwt")).isEmpty();
        assertThat(jwtProvider.parse("")).isEmpty();
    }

    @Test
    @DisplayName("서명은 유효해도 필수 클레임이 없거나 형식이 틀리면 예외 없이 복원하지 않는다")
    void parse_missingOrMalformedClaims() {
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String noRole = io.jsonwebtoken.Jwts.builder().subject("12").signWith(key).compact();
        String badIds = io.jsonwebtoken.Jwts.builder().subject("12").claim("role", "HQ_ADMIN")
                .claim("warehouseIds", List.of("a")).signWith(key).compact();
        String badSubject = io.jsonwebtoken.Jwts.builder().subject("abc").claim("role", "HQ_ADMIN")
                .signWith(key).compact();

        assertThat(jwtProvider.parse(noRole)).isEmpty();
        assertThat(jwtProvider.parse(badIds)).isEmpty();
        assertThat(jwtProvider.parse(badSubject)).isEmpty();
    }

    @Test
    @DisplayName("서명 키가 32바이트보다 짧으면 생성에 실패한다")
    void weakKey() {
        assertThatThrownBy(() -> new JwtProvider("short-key", 3600)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("유효 시간을 그대로 노출한다")
    void validitySeconds() {
        assertThat(jwtProvider.getAccessTokenValiditySeconds()).isEqualTo(3600);
    }
}
