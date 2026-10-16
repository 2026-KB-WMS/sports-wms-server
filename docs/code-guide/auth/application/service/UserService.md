# UserService

`auth/application/service/UserService.java` · 구현: `UserUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

가입, 사용자 조회·수정, 비밀번호 변경, 최초 본사 관리자 생성.

## 의존

`UserRepository`, `UserQueryRepository`, `StatusHistoryUseCase`, `PasswordEncoder`

## 형식 규칙

| 항목 | 규칙 |
|---|---|
| 로그인 아이디 | `^[a-z0-9_]{4,20}$`. 입력은 `trim().toLowerCase`로 정규화한 뒤 검사 |
| 비밀번호 | 영문·숫자·특수문자를 **각각 하나 이상** 포함한 **8자 이상**. UTF-8 기준 **72바이트 이하**(BCrypt 한계) |

## 메서드

| 메서드 | 트랜잭션 | 허용 역할 |
|---|---|---|
| `signUp(command)` | 쓰기 | 모두(공개) |
| `getUser(userId)` | 읽기 | 내부 조회, `/auth/me` |
| `getUsers(condition, page)` | 읽기 | HQ_ADMIN |
| `updateUser(command)` | 쓰기 | HQ_ADMIN |
| `changePassword(command)` | 쓰기 | 인증된 본인 |
| `ensureInitialHqAdmin(command)` | 쓰기 | 시작 시 |

## signUp

| 검사 | 오류 |
|---|---|
| 역할이 null이거나 HQ_ADMIN | 400 |
| 로그인 아이디·비밀번호 형식 | 400 |
| 아이디 중복 / 이메일 중복 | 409 |

PENDING으로 생성한다. **창고·지점 소속은 가입 때 받지 않고** 승인 과정에서 배정한다.

## updateUser

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | `loginId`·`password`가 들어 있으면 거절 (수정 불가 필드를 무시하지 않고 명시적으로 거절) | 400 |
| 2 | 변경할 필드가 하나도 없음 | 400 |
| 3 | 사용자 행 비관적 락 조회 | 404 |
| 4 | **본인 계정**의 역할·상태 변경 시도 (같은 값은 무시) | 400 (마지막 관리자 계정 잠김 방지) |
| 5 | 이메일이 바뀌면 본인 제외 중복 확인 후 변경 | 409 |
| 6 | 이름·연락처 변경 | |
| 7 | 역할이 바뀌는데 **이미 소속이 있음** | 409 `AFFILIATION_ASSIGNED` |
| 8 | 소속 지정 요청이면 형식 검사 (`validateAffiliationAssignment`) | 400 / 409 |
| 9 | 상태 변경이면 전이 가능 여부 + 소속 필요 여부 (`validateStatusChange`) | 409 |
| 10 | 저장, **상태가 바뀌면 상태 이력(USER)에 기록** (사유 없음, 처리자 = 요청 관리자) | |

### 소속 지정 형식 검사 (`validateAffiliationAssignment`)

| 규칙 | 오류 |
|---|---|
| `warehouseId`와 `storeId`를 함께 보낼 수 없다 | 400 |
| **승인·재활성화(`status: ACTIVE`)와 함께만** 지정 가능하고, 이미 ACTIVE인 사용자는 불가 (이미 활성인 사용자의 소속은 배정 API로) | 400 |
| `warehouseId`는 창고 관리자에게만, `storeId`는 점주에게만 | 400 |
| 이미 소속이 있는 사용자 | 409 `AFFILIATION_ASSIGNED` |

이 메서드는 **검사만 하고 배정하지 않는다.** 실제 배정은 `UserManagementService`가 상태 변경 뒤 같은 트랜잭션에서 한다.

### 상태 변경 검사 (`validateStatusChange`)

| 규칙 | 오류 |
|---|---|
| `canChangeStatusTo`가 false (같은 상태, PENDING으로 되돌리기) | 409 `INVALID_USER_STATUS_TRANSITION` |
| ACTIVE로 바꾸는데 소속이 필요한 역할이고(창고 관리자·점주), 이번 요청에 소속 지정도 없고 기존 소속도 없음 | 409 `AFFILIATION_REQUIRED` |

## changePassword

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 현재 비밀번호 공백 | 400 |
| 2 | 새 비밀번호 형식 | 400 |
| 3 | 사용자 행 락 조회 | 404 |
| 4 | **ACTIVE 계정만** (만료 전 토큰이 남아 있어도 비활성 계정은 불가) | 403 `ACCOUNT_INACTIVE` |
| 5 | 현재 비밀번호 일치 | 400 `CURRENT_PASSWORD_MISMATCH` |
| 6 | 새 비밀번호가 현재와 다름 | 400 |

**이미 발급된 토큰은 그대로 유효하다**(ADR-011).

## ensureInitialHqAdmin

본사 관리자가 하나도 없을 때만 설정값으로 ACTIVE 관리자를 만든다(시작 시 초기화, ADR-013).

| 상황 | 결과 |
|---|---|
| 본사 관리자가 이미 있음 | `ALREADY_EXISTS` |
| 설정 5개가 모두 비어 있음 | `NOT_CONFIGURED` |
| 일부만 비었음 | 400 (비어 있는 항목 이름 포함) |
| 이름 100자·이메일 255자(@ 포함)·연락처 30자 초과 등 | 400 (컬럼 길이 오류가 동시 기동 중복과 구분되지 않아 미리 거른다) |
| 아이디·비밀번호 형식 위반 | 400 |
| 아이디·이메일 중복 | 409 |
| 성공 | `CREATED` |
