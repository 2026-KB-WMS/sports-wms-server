# 인증·계정 도메인 API 명세 (가입 · 로그인 · 내 정보 · 계정 관리)

> 기준은 레포. 2026-10-08 Notion(최종 수정 2026-09-24 ~ 10-05)에서 이전. 공통 규칙은 [conventions.md](conventions.md), 역할별 호출 가능 여부는 [authorization.md](authorization.md) 참고.
> Base path: `/api/v1/auth`, `/api/v1/users`. 목록 API는 `data.items`만 반환(페이지네이션 보류). 404는 도메인 코드 `USER_NOT_FOUND`를 쓴다.
> 계정 상태 전이·토큰 정책은 [docs/domain/auth.md](../domain/auth.md), 결정 배경은 [ADR-011](../adr/011-jwt-access-token-only.md) 참고.

## 범위

- 이 문서는 **구현 완료된** 6개(가입·로그인·내 정보·비밀번호 변경·사용자 목록·사용자 수정)를 포함한다.
- Notion에 있던 `GET /users/{userId}`(사용자 단건 조회)는 **구현하지 않았다**. 아래 "구현 대비 메모" 참고.

## 구현 대비 메모

- Notion 상태 컬럼은 "시작 전"이지만 코드는 구현됨(상태 컬럼이 오래됨, 코드 기준).
- **`GET /users/{userId}`는 코드에 없다.** Notion 명세(P1, HQ_ADMIN 전용, 비활성 계정도 조회)는 남아 있지만 구현 범위(#162)에서 빠졌다. 사용자 상세 화면이 필요해지면 명세를 먼저 확정한 뒤 추가한다(미결).
- Notion의 `pageInfo`와 일반 `NOT_FOUND`는 현재 구현 기준과 다름 → conventions.md 기준 따름. 사용자가 없을 때 404 코드는 `USER_NOT_FOUND`다(`GET /auth/me`, `PATCH /users/{userId}`).
- **로그인 아이디 형식**: Notion은 "최대 50자"만 적었다. 구현은 서비스에서 앞뒤 공백을 자르고 **소문자로 바꾼 뒤** 영문 소문자·숫자·밑줄 4~20자만 허용한다(DB 컬럼은 `VARCHAR(50)`, 요청 DTO 검증은 50자 이하). 로그인도 같은 방식으로 소문자로 바꿔 조회하므로 대소문자를 구분하지 않는다.
- **비밀번호 규칙**(Notion에서 미결이던 항목, 구현에서 확정): 8자 이상, 영문·숫자·특수문자 각 1자 이상, UTF-8 72바이트 이하(BCrypt 한계). 해시는 BCrypt.
- **`PATCH /users/{userId}` 추가 규칙(명세에 없이 서비스에 넣음)**: 소속(창고·지점 멤버)이 있는 사용자의 `role` 변경은 409 `AFFILIATION_ASSIGNED`, 창고 관리자·점주를 소속 없이 `ACTIVE`로 만들면 409 `AFFILIATION_REQUIRED`, 허용되지 않는 상태 전이는 409 `INVALID_USER_STATUS_TRANSITION`. 상태가 바뀌면 `StatusHistory`(`entity_type` `USER`)에 이전·이후 상태와 처리자를 기록한다(사유는 받지 않는다).
- **상태 전이 허용 범위(Notion 미결을 구현에서 확정)**: 재활성화(`INACTIVE`→`ACTIVE`)와 가입 반려(`PENDING`→`INACTIVE`)를 둘 다 허용한다. `approve`/`deactivate` 별도 엔드포인트로 나누지 않고 `PATCH`의 `status` 하나로 처리한다. `PENDING`으로 되돌리기와 같은 상태로의 변경은 409.
- **토큰 정책(Notion 미결을 ADR-011로 확정)**: 액세스 토큰(JWT, HS256)만 발급, 만료 3600초, 리프레시 토큰·로그아웃 없음. 토큰에는 사용자 ID만 담고 역할·상태·소속은 요청마다 DB에서 읽으므로, 비활성화·역할·소속 변경이 다음 요청부터 반영된다([ADR-016](../adr/016-reload-user-per-request.md)). 로그인 실패 횟수 제한·rate limiting은 두지 않았다(보류).
- **최초 본사 관리자**: 가입 API는 `HQ_ADMIN`을 만들 수 없고 가입 승인은 `HQ_ADMIN`만 할 수 있으므로, 본사 관리자가 하나도 없을 때만 앱 시작 시 환경변수(`WMS_ADMIN_LOGIN_ID`, `WMS_ADMIN_PASSWORD`, `WMS_ADMIN_NAME`, `WMS_ADMIN_EMAIL`, `WMS_ADMIN_PHONE`)로 `ACTIVE` 관리자를 만든다([ADR-013](../adr/013-initial-hq-admin-from-environment.md), #189). 설정이 비었거나 잘못돼도 기동은 막지 않고 로그만 남긴다. 환경변수 비밀번호는 `PATCH /auth/me/password`(#193)로 로그인한 뒤 바꾼다. 관리자가 다른 사용자의 비밀번호를 재설정하는 API는 없다(보류).
- 확정 필요(미결): 로그인 실패 횟수 제한, 가입 시 소속 선택 필드 추가 여부(현재는 가입 후 본사가 배정), 가입 반려 사유 기록.

## 엔드포인트 목록 (6)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | /auth/signup | 없음 | 가입 신청(PENDING) |
| POST | /auth/login | 없음 | 로그인, 액세스 토큰 발급 |
| GET | /auth/me | 로그인한 모든 역할 | 내 정보 |
| PATCH | /auth/me/password | 로그인한 모든 역할 | 본인 비밀번호 변경 |
| GET | /users | HQ_ADMIN | 사용자 목록 |
| PATCH | /users/{userId} | HQ_ADMIN | 사용자 정보·역할·상태 수정 |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`(토큰 필요 API). 응답에 `password_hash`는 포함하지 않는다.

## POST /auth/signup (P0)

- 인증 없음. 점주·창고 관리자의 가입 신청이며, 계정은 `PENDING`으로 만들어져 본사 관리자가 승인(`ACTIVE`)해야 로그인할 수 있다.
- Body (전부 필수)

| 필드 | 타입 | 규칙 |
|---|---|---|
| loginId | string | 영문 소문자·숫자·밑줄 4~20자(대문자는 소문자로 저장), 중복 불가 |
| password | string | 8자 이상, 영문·숫자·특수문자 포함, 72바이트 이하 |
| name | string | 최대 100자 |
| email | string | 이메일 형식, 최대 255자, 중복 불가 |
| phone | string | 최대 30자 |
| role | string | `WAREHOUSE_MANAGER` / `STORE_OWNER`만. `HQ_ADMIN`은 400 |

- 201. 응답: `userId, loginId, name, email, phone, role, status(PENDING), createdAt`
- 소속(창고·지점)은 가입 때 받지 않는다. 승인 전에 본사가 `POST /warehouses/managers` 또는 `POST /stores/assign`으로 배정한다.
- 오류: 400 `VALIDATION_ERROR`(필드 누락·형식·허용되지 않는 역할), 409 `DUPLICATE_LOGIN_ID`, 409 `DUPLICATE_EMAIL`. 중복은 사전 조회와 DB 유니크 제약(`uk_users_login_id`, `uk_users_email`) 위반을 모두 같은 코드로 응답해 동시 가입 경쟁을 처리한다.

## POST /auth/login (P0)

- 인증 없음. Body: `loginId`, `password` (둘 다 필수, 공백 불가).
- 200. 응답

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "userId": 1, "loginId": "hq_admin01", "name": "홍길동", "role": "HQ_ADMIN", "status": "ACTIVE" }
}
```

- 토큰 클레임: `sub`(userId)만. 역할·소속은 요청마다 DB에서 읽는다. 로그인 성공 시 `lastLoginAt`을 갱신한다.
- 오류

| HTTP | 오류 코드 | 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 아이디·비밀번호 누락 |
| 401 | UNAUTHORIZED | 없는 아이디 또는 비밀번호 불일치(같은 메시지: "아이디 또는 비밀번호가 올바르지 않습니다.") |
| 403 | ACCOUNT_PENDING | 승인 대기 계정 |
| 403 | ACCOUNT_INACTIVE | 비활성 계정 |

- 계정 상태(403)는 **비밀번호가 맞은 뒤에만** 알려 준다. 비밀번호가 틀리면 상태와 관계없이 401이라 계정 존재·상태가 드러나지 않는다.

## GET /auth/me (P0)

- `Authorization: Bearer {accessToken}` 필수. 조회 대상은 토큰 주체 본인이다.
- 200. 응답: `userId, loginId, name, email, phone, role, status, lastLoginAt`. 창고·지점 소속은 포함하지 않는다(창고 관리자는 `GET /warehouses/my`, 점주는 `GET /stores/my`).
- 오류: 401 `UNAUTHORIZED`(토큰 없음·만료·위조), 404 `USER_NOT_FOUND`(토큰의 사용자가 삭제된 경우).
- 계정이 비활성화되면 이미 발급된 토큰도 다음 요청부터 401이다([ADR-016](../adr/016-reload-user-per-request.md)).

## PATCH /auth/me/password (P1)

- `Authorization: Bearer {accessToken}` 필수. 변경 대상은 토큰 주체 본인이다. 관리자가 다른 사용자의 비밀번호를 재설정하는 기능은 없다.
- Body (전부 필수)

| 필드 | 타입 | 규칙 |
|---|---|---|
| currentPassword | string | 공백 불가. 현재 비밀번호와 일치해야 한다 |
| newPassword | string | 가입과 같은 규칙(8자 이상, 영문·숫자·특수문자 포함, 72바이트 이하). 현재 비밀번호와 달라야 한다 |

- 200. 응답 `data`는 `null`이고 메시지는 "비밀번호가 변경되었습니다."이다.
- 오류

| HTTP | 오류 코드 | 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 필드 누락·공백, 새 비밀번호 형식 위반, 새 비밀번호가 현재와 같음 |
| 400 | CURRENT_PASSWORD_MISMATCH | 현재 비밀번호 불일치. 401이 아니라 400이다(클라이언트가 토큰 만료로 보고 로그아웃시키지 않도록) |
| 401 | UNAUTHORIZED | 토큰 없음·만료 |
| 403 | ACCOUNT_INACTIVE | 비활성화된 계정(만료 전 토큰이 남아 있어도 변경 불가) |
| 404 | USER_NOT_FOUND | 토큰의 사용자가 삭제된 경우 |

- 사용자 행을 잠그고 한 트랜잭션으로 처리한다. 새 비밀번호는 BCrypt로 저장한다.
- 이미 발급된 토큰은 비밀번호를 바꿔도 만료까지 유효하다([ADR-011](../adr/011-jwt-access-token-only.md)).

## GET /users (P0)

- `HQ_ADMIN` 전용. 본사 관리자가 계정 현황을 보거나 가입 승인 대기 목록(`status=PENDING`)을 찾을 때 쓴다.
- Query (전부 선택)

| 파라미터 | 설명 |
|---|---|
| role | `HQ_ADMIN` / `WAREHOUSE_MANAGER` / `STORE_OWNER`. 허용 값이 아니면 400 |
| status | `PENDING` / `ACTIVE` / `INACTIVE`. 허용 값이 아니면 400 |
| keyword | 이름·로그인 아이디·이메일 부분 일치(대소문자 무시) |

- 필터를 주지 않으면 모든 역할·상태를 반환한다. `page`·`size`·`sort`는 받지 않고(보내도 무시) 정렬은 고정이다(페이지네이션 보류).
- 200. `data.items[]`: `userId, loginId, name, email, phone, role, status, lastLoginAt, createdAt`. 창고·지점 소속은 포함하지 않는다(`GET /warehouses/managers`, `GET /stores/managers`).
- 오류: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`(HQ_ADMIN이 아님).

## PATCH /users/{userId} (P1)

- `HQ_ADMIN` 전용. 보낸 필드만 부분 수정하며 **최소 1개 필드가 필요**하다. 가입 승인은 `status`를 `ACTIVE`로 바꿔 처리한다.
- Body (전부 선택)

| 필드 | 타입 | 규칙 |
|---|---|---|
| name | string | 최대 100자, 공백만 불가 |
| email | string | 이메일 형식, 최대 255자, 다른 계정과 중복 불가(본인 값과 같으면 변경 아님) |
| phone | string | 최대 30자, 공백만 불가 |
| role | string | `HQ_ADMIN` / `WAREHOUSE_MANAGER` / `STORE_OWNER` |
| status | string | `PENDING` / `ACTIVE` / `INACTIVE` (허용 전이는 아래) |

- `loginId`·`password`는 수정할 수 없다. 포함해서 보내면 무시하지 않고 400으로 거절한다.
- 200. 응답: `userId, loginId, name, email, phone, role, status, updatedAt`
- 오류

| HTTP | 오류 코드 | 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 수정 필드 없음, 형식·길이 위반, `loginId`/`password` 포함, 본인 계정의 `role`·`status` 변경 시도(마지막 관리자 잠김 방지) |
| 401 | UNAUTHORIZED | 토큰 없음·만료 |
| 403 | FORBIDDEN | HQ_ADMIN이 아님 |
| 404 | USER_NOT_FOUND | 해당 사용자 없음 |
| 409 | DUPLICATE_EMAIL | 다른 계정이 쓰는 이메일 |
| 409 | INVALID_USER_STATUS_TRANSITION | 같은 상태로의 변경, `PENDING`으로 되돌리기 |
| 409 | AFFILIATION_REQUIRED | 창고 관리자·점주를 창고·지점 소속 없이 `ACTIVE`로 변경 |
| 409 | AFFILIATION_ASSIGNED | 소속이 배정된 사용자의 `role` 변경(소속을 먼저 회수) |

- 본인 계정은 `name`·`email`·`phone`만 바꿀 수 있다. 본인 값과 같은 `role`·`status`를 보내는 것은 변경이 아니므로 무시한다.
- 수정은 대상 행을 잠그고 한 트랜잭션으로 처리하며 `updated_at`을 갱신한다. 비밀번호 변경은 이 API가 아니라 `PATCH /auth/me/password`다.
- 상태 전이 규칙은 [docs/domain/auth.md](../domain/auth.md) 참고.
