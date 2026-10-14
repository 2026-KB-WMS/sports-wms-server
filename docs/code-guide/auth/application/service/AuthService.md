# AuthService

`auth/application/service/AuthService.java` · 구현: `AuthUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

**로그인.** 아이디·비밀번호가 맞고 ACTIVE 계정이면 액세스 토큰을 발급하고 `lastLoginAt`을 갱신한다.

## 의존

`UserRepository`, `PasswordEncoder`, `JwtProvider`(common)

## login(command)

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 아이디·비밀번호가 공백이면 | 400 `VALIDATION_ERROR` |
| 2 | 아이디를 `trim().toLowerCase`로 맞춰 조회 (가입 때 소문자로 저장) | |
| 3 | **없는 아이디**: 더미 해시와 비교만 하고 거절 | 401 |
| 4 | 비밀번호 비교 | 틀리면 401 (3번과 **같은 메시지**) |
| 5 | **행을 잠그고 다시 읽는다** (비교하는 동안 관리자가 상태를 바꿨을 수 있음) | 401 |
| 6 | 상태 확인: PENDING | 403 `ACCOUNT_PENDING` |
| 7 | 상태 확인: INACTIVE | 403 `ACCOUNT_INACTIVE` |
| 8 | `recordLogin(now)` 저장, 토큰 발급 | |

## 보안 설계

| 설계 | 목적 |
|---|---|
| 없는 아이디와 비밀번호 불일치를 **같은 401·같은 메시지**로 응답 | 계정 존재 여부 노출 방지 |
| 없는 아이디도 **더미 해시와 `matches`** 를 수행 | 응답 시간 차이로 계정 존재 여부가 드러나지 않게 함 (`dummyHash`는 지연 생성) |
| 계정 **상태는 비밀번호가 맞은 뒤에만** 알려 줌 | 비밀번호를 모르는 사람에게 PENDING·INACTIVE 여부를 노출하지 않음 |
| 비밀번호 비교 뒤 행을 잠그고 재조회 | 낡은 값으로 덮어쓰지 않고 최신 상태로 판단 |

## 결과

`LoginResult(accessToken, expiresIn, user)`. 토큰에는 사용자 ID만 담기고(ADR-016), 리프레시 토큰은 없다.
