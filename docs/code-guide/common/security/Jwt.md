# JWT 인증 (security)

`common/security/` · `AuthenticatedUser`는 별도 문서(`AuthenticatedUser.md`)를 본다.

## 요청 흐름

```
Authorization: Bearer <token>
   → JwtAuthenticationFilter
   → JwtProvider.parseUserId(token)      (서명·만료·형식 검증)
   → AuthenticatedUserResolver.resolve(userId)   (DB에서 역할·소속·상태 조회)
   → SecurityContext에 AuthenticatedUser 올림
   → SecurityConfig 경로 규칙이 허용 여부 결정
```

토큰에는 **사용자 ID(sub)만** 담는다. 역할·상태·소속은 요청마다 DB에서 다시 읽는다(ADR-016).

## JwtProvider

| 항목 | 내용 |
|---|---|
| 용도 | 액세스 토큰(HS256 JWT) 발급과 검증. 리프레시 토큰은 없다 |
| 설정 | `wms.jwt.secret`, `wms.jwt.access-token-validity-seconds` |
| 제약 | 서명 키가 32바이트보다 짧으면 시작 시점에 실패한다 |

| 메서드 | 설명 |
|---|---|
| `createAccessToken(userId)` | sub=userId, 발급·만료 시각을 넣어 서명 |
| `parseUserId(token)` | 유효하면 사용자 ID, 그 외(서명·만료·형식·sub 누락)는 이유를 구분하지 않고 빈 Optional |
| `getAccessTokenValiditySeconds()` | 로그인 응답의 만료 시간 안내용 |

## JwtAuthenticationFilter

| 항목 | 내용 |
|---|---|
| 용도 | Bearer 토큰이 유효하면 인증 주체를 SecurityContext에 올린다 |
| 상속 | `OncePerRequestFilter`. 빈이 아니라 `SecurityConfig`에서 `new`로 체인에 직접 넣는다(서블릿 필터 중복 등록 방지) |
| 권한 | `ROLE_` + `UserRole.name()` |
| 실패 시 | 아무것도 하지 않고 다음 필터로 넘긴다. **거절은 SecurityConfig 경로 규칙**이 한다 |

## AuthenticatedUserResolver

| 항목 | 내용 |
|---|---|
| 용도 | 토큰의 사용자 ID로 현재 인증 주체를 읽어 오는 **포트** |
| 구현 | auth 도메인 |
| 메서드 | `resolve(userId)`: 사용자가 없거나 `ACTIVE`가 아니면 빈 Optional |
| 의미 | 승인 대기·비활성 사용자의 기존 토큰도 다음 요청부터 막힌다 |

## RestSecurityExceptionHandler

| 항목 | 내용 |
|---|---|
| 용도 | 필터 체인에서 나는 인증·인가 실패를 공통 오류 JSON으로 응답 |
| 구현 | `AuthenticationEntryPoint` + `AccessDeniedHandler` |
| 이유 | 컨트롤러 밖이라 `GlobalExceptionHandler`가 처리하지 못해 같은 포맷을 직접 쓴다 |

| 상황 | 응답 |
|---|---|
| 토큰 없음·만료·위조 (`commence`) | 401 `UNAUTHORIZED` |
| 인증됐지만 권한 없음 (`handle`) | 403 `FORBIDDEN` |
