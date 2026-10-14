# auth 코드 가이드

`src/main/java/com/kb/wms/auth/`의 패키지 구조를 따라 클래스별 설명을 모았다.

## 도메인 개요

**가입, 로그인(JWT), 계정 관리**를 맡는다. 가입 신청은 PENDING으로 만들어지고 본사 관리자가 승인해야 로그인할 수 있다. 창고 관리자·점주는 승인할 때 **창고·지점 소속을 함께 지정**한다. 요청마다 DB에서 역할·상태·소속을 읽어 인증 주체를 만든다.

```
가입(PENDING) ──본사 승인(+소속 지정)──▶ ACTIVE ──로그인──▶ 토큰(사용자 ID만) ──요청마다──▶ DB에서 역할·소속 조회
```

`common`의 `JwtProvider`, `JwtAuthenticationFilter`, `SecurityConfig`와 함께 동작한다. 흐름은 `docs/code-guide/common/security/Jwt.md`를 본다.

## 읽는 순서 추천

1. `domain/entity/User.md`, `domain/enums/Enums.md`
2. `application/service/AuthService.md` : 로그인
3. `application/service/UserService.md` : 가입·수정·승인 규칙
4. `application/service/UserManagementService.md`, `AuthenticatedUserService.md`
5. `adapter/in/`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | User |
| `domain/enums` | `Enums.md` (UserStatus, UserRole은 `common/security`) |
| `exception` | `Exceptions.md` |
| `application/service` | AuthService, UserService, UserManagementService, AuthenticatedUserService |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` |
| `adapter/in/web` | `Controllers.md`, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/in/startup` | `InitialHqAdminRunner.md` |
| `adapter/out/persistence` | `Persistence.md` |

## 핵심 규칙

- **가입은 PENDING**: 승인 전에는 로그인할 수 없다. HQ_ADMIN은 가입으로 만들 수 없다
- **승인 시 소속 필수**: 창고 관리자·점주를 소속 없이 ACTIVE로 만들 수 없다(`AFFILIATION_REQUIRED`). 승인과 소속 지정은 한 트랜잭션
- **상태는 PENDING으로 되돌릴 수 없다**. 상태 변경은 상태 이력(USER)에 기록
- **본인 계정의 역할·상태는 못 바꾼다** (마지막 관리자 잠김 방지)
- **소속이 있으면 역할을 못 바꾼다** (`AFFILIATION_ASSIGNED`)
- **로그인 보안**: 없는 아이디와 비밀번호 불일치는 같은 응답, 응답 시간도 맞춘다. 상태는 비밀번호가 맞은 뒤에만 알려 준다
- **토큰에는 사용자 ID만** 담고 역할·상태·소속은 요청마다 DB에서 읽는다(ADR-016). 비밀번호를 바꿔도 기존 토큰은 유효하다(ADR-011)
- **아이디는 소문자로 저장**하고 `^[a-z0-9_]{4,20}$`, 비밀번호는 8자 이상·영문·숫자·특수문자 포함·72바이트 이하
- **최초 본사 관리자**는 `WMS_ADMIN_*` 설정으로 시작 시 한 번 만든다(ADR-013). 설정이 잘못돼도 기동은 막지 않는다
- **순환 의존 회피**: 창고·지점 배정 서비스가 `UserUseCase`를 쓰므로 둘을 잇는 `UserManagementService`가 따로 있다
