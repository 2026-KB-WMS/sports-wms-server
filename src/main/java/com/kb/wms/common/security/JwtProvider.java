package com.kb.wms.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kb.wms.auth.domain.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 액세스 토큰(HS256 JWT) 발급과 검증. 리프레시 토큰은 발급하지 않는다.
 * 서명 키가 32바이트보다 짧으면 시작 시점에 실패한다.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_WAREHOUSE_IDS = "warehouseIds";
    private static final String CLAIM_STORE_IDS = "storeIds";

    private final SecretKey key;
    private final long accessTokenValiditySeconds;

    public JwtProvider(@Value("${wms.jwt.secret}") String secret,
                       @Value("${wms.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
    }

    public String createAccessToken(AuthenticatedUser user) {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_WAREHOUSE_IDS, user.warehouseIds())
                .claim(CLAIM_STORE_IDS, user.storeIds())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(nowMillis + accessTokenValiditySeconds * 1000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 서명·만료·형식이 유효한 토큰만 인증 주체로 복원한다. 그 외에는 이유를 구분하지 않고 비어 있다. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    readIds(claims, CLAIM_WAREHOUSE_IDS),
                    readIds(claims, CLAIM_STORE_IDS)));
        } catch (RuntimeException e) {
            // 서명·만료 오류(JwtException)뿐 아니라 필수 클레임 누락·형식 오류도 같은 "유효하지 않은 토큰"으로 본다.
            return Optional.empty();
        }
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    private List<Long> readIds(Claims claims, String name) {
        List<?> values = claims.get(name, List.class);
        if (values == null) {
            return List.of();
        }
        return values.stream().map(value -> ((Number) value).longValue()).toList();
    }
}
