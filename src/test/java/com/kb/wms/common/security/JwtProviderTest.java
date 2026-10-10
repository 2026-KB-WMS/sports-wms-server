package com.kb.wms.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtProviderTest {

    private static final String SECRET = "test-only-jwt-secret-key-0123456789-0123456789";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600);
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    @DisplayName("발급한 토큰에서 사용자 ID를 그대로 복원한다")
    void createAndParse() {
        String token = jwtProvider.createAccessToken(12L);

        assertThat(jwtProvider.parseUserId(token)).contains(12L);
    }

    @Test
    @DisplayName("토큰에는 사용자 ID(sub)만 담고 역할·소속 클레임은 넣지 않는다")
    void tokenCarriesOnlySubject() {
        String token = jwtProvider.createAccessToken(12L);

        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("12");
        assertThat(claims).doesNotContainKeys("role", "warehouseIds", "storeIds");
    }

    @Test
    @DisplayName("만료된 토큰은 복원하지 않는다")
    void parse_expired() {
        JwtProvider alreadyExpired = new JwtProvider(SECRET, -1);

        assertThat(jwtProvider.parseUserId(alreadyExpired.createAccessToken(12L))).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명했거나 변조한 토큰은 복원하지 않는다")
    void parse_invalidSignature() {
        JwtProvider otherKey = new JwtProvider("other-jwt-secret-key-0123456789-0123456789-ab", 3600);
        String token = jwtProvider.createAccessToken(12L);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwtProvider.parseUserId(otherKey.createAccessToken(12L))).isEmpty();
        assertThat(jwtProvider.parseUserId(tampered)).isEmpty();
    }

    @Test
    @DisplayName("JWT 형식이 아닌 값은 복원하지 않는다")
    void parse_garbage() {
        assertThat(jwtProvider.parseUserId("not-a-jwt")).isEmpty();
        assertThat(jwtProvider.parseUserId("")).isEmpty();
    }

    @Test
    @DisplayName("서명은 유효해도 sub가 없거나 숫자가 아니면 예외 없이 복원하지 않는다")
    void parse_missingOrMalformedSubject() {
        String noSubject = Jwts.builder().claim("role", "HQ_ADMIN").signWith(key).compact();
        String badSubject = Jwts.builder().subject("abc").signWith(key).compact();

        assertThat(jwtProvider.parseUserId(noSubject)).isEmpty();
        assertThat(jwtProvider.parseUserId(badSubject)).isEmpty();
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
