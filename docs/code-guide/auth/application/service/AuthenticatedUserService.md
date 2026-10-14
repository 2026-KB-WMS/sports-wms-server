# AuthenticatedUserService

`auth/application/service/AuthenticatedUserService.java` · 구현: `AuthenticatedUserResolver`(common) · `@Transactional(readOnly = true)`

**요청마다** 사용자의 역할·상태·소속을 DB에서 읽어 인증 주체(`AuthenticatedUser`)를 만든다(ADR-016). `JwtAuthenticationFilter`가 호출한다.

## 의존

`UserRepository`, `UserQueryRepository`

## resolve(userId)

| 조건 | 결과 |
|---|---|
| 사용자가 없음 | 빈 Optional (인증 안 됨) |
| 상태가 ACTIVE가 아님 | 빈 Optional (**PENDING·INACTIVE는 기존 토큰이 있어도 막힌다**) |
| 역할이 HQ_ADMIN | `AuthenticatedUser(userId, role, [], [])`. 소속 조회를 건너뛴다 |
| 그 외 | `findAffiliation`으로 담당 창고·지점 ID를 읽어 채운다 |

## 의미

토큰 값이 아니라 DB 기준이라 **역할·상태·소속 변경이 다음 요청부터 바로 반영**된다. 대신 요청마다 사용자 조회(+소속 조회) 쿼리가 나간다.
