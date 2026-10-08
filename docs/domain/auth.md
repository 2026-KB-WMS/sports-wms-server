# 인증·계정(auth) 도메인 요약

> **기준은 레포다.** 인증·계정 도메인(#162)이 구현 완료되어 2026-10-08 이 문서를 기준 문서로 만들었다. 계정 상태 전이·권한·토큰 정책은 이 문서를 고치고, Notion "업무 상태 전이도"에는 User 상태 전이가 없어 따로 옮길 내용은 없다.
> - API 명세: [`docs/api/auth.md`](../api/auth.md) (`/api/v1/auth` 3개 + `/api/v1/users` 2개)
> - 역할·소속 검사 규칙: [`docs/api/authorization.md`](../api/authorization.md), [ADR-012](../adr/012-authorization-check-placement.md)
> - 토큰 정책: [ADR-011](../adr/011-jwt-access-token-only.md)
> - 구현 일정: WMS 개발 일정 DB의 "[구현] 인증·계정 도메인", 추적 이슈 #162

## 범위

`auth` 패키지 = User (ERD "회원" 섹션, 테이블 `users`). 창고·지점 소속은 `WarehouseMember`/`StoreMember`가 가지며 User에는 두지 않는다. JWT 발급·검증과 인증 필터는 `common.security`에 있다.

## 권한

- 가입·로그인은 토큰 없이 호출한다. 그 밖의 모든 API는 로그인이 필요하다(기본 인증 필수).
- `GET /auth/me`는 로그인한 모든 역할이 본인 정보만 조회한다.
- 사용자 목록·수정(`/users`)은 본사 관리자(HQ_ADMIN)만 한다. 가입 승인·반려·비활성화·재활성화·역할 변경이 모두 여기에 속한다.
- 가입으로 만들 수 있는 역할은 창고 관리자와 점주뿐이다. 본사 관리자는 가입으로 만들 수 없다.
- 본사 관리자는 본인 계정의 `role`·`status`를 바꿀 수 없다(마지막 관리자 계정 잠김 방지). 이름·이메일·연락처는 바꿀 수 있다.

## 계정(User) 상태

| 현재 | 행위 | 다음 | 수행자 | 조건 |
|---|---|---|---|---|
| (없음) | 가입 신청 | PENDING | 가입자(토큰 없음) | 역할은 창고 관리자·점주, 아이디·이메일 중복 없음 |
| PENDING | 승인 | ACTIVE | 본사 관리자 | 창고 관리자·점주는 창고·지점 소속이 하나 이상 배정돼 있어야 함(`AFFILIATION_REQUIRED`) |
| PENDING | 가입 반려 | INACTIVE | 본사 관리자 | 사유는 받지 않는다 |
| ACTIVE | 비활성화 | INACTIVE | 본사 관리자 | 본인 계정 불가 |
| INACTIVE | 재활성화 | ACTIVE | 본사 관리자 | 승인과 같은 소속 조건 |

- 모두 `PATCH /users/{userId}`의 `status`로 처리한다(별도 `approve`/`deactivate` 엔드포인트 없음).
- `PENDING`으로 되돌리는 전이와 같은 상태로의 변경은 409 `INVALID_USER_STATUS_TRANSITION`이다. 전이가 막힌 요청은 부작용(이력 기록)을 만들지 않는다.
- 로그인은 `ACTIVE` 계정만 된다. `PENDING`은 403 `ACCOUNT_PENDING`, `INACTIVE`는 403 `ACCOUNT_INACTIVE`이며, 둘 다 **비밀번호가 맞은 뒤에만** 알려 준다.
- 역할 변경은 소속이 하나도 없을 때만 된다(`AFFILIATION_ASSIGNED`). 소속은 각 배정·회수 API(`/warehouses/managers`, `/stores/assign`, `/stores/managers`)로 먼저 정리한다.
- 승인 시 소속 검사는 역할 변경과 함께 올 때 **변경 후 역할** 기준이다.

## 부수 효과

- 상태가 바뀌면 같은 트랜잭션에서 `StatusHistory`(`entity_type` `USER`)에 이전·이후 상태와 처리자를 기록한다. 사유는 받지 않는다.
- 로그인 성공 시 `lastLoginAt`을 갱신한다. 로그인과 수정은 대상 사용자 행을 비관적 락으로 잠그고 최신 상태를 다시 읽어 판단한다(비밀번호 비교 중 관리자가 상태를 바꾼 경우 낡은 값으로 덮어쓰지 않는다).
- 가입의 아이디·이메일 중복은 사전 조회와 DB 유니크 제약 위반(`uk_users_login_id`, `uk_users_email`)을 모두 409로 변환한다.

## 토큰 정책 (ADR-011)

- 액세스 토큰(JWT, HS256)만 발급하고 만료는 3600초(`wms.jwt.access-token-validity-seconds`)다. 클레임은 `sub`(userId), `role`, `warehouseIds`, `storeIds`다.
- 리프레시 토큰과 로그아웃은 없다. 비활성화·역할·소속 변경 뒤에도 발급된 토큰은 만료까지 유효하며, 관리자 API도 마찬가지다.
- 서명 키는 `wms.jwt.secret`(32바이트 이상)이고 prod에서는 `JWT_SECRET` 환경변수로만 받는다.
- 비밀번호는 BCrypt로 저장하고, 없는 아이디로 로그인해도 해시 비교만큼의 시간을 쓰게 해 응답 시간으로 계정 존재 여부가 드러나지 않게 한다.

## 구현 현황과 보류

- 구현 완료: 도메인 모델·마이그레이션(V14, FK는 V15), 포트·어댑터, 서비스, 웹 어댑터, 모든 도메인의 인증·인가 적용(#170), 점주 응답 차등(#182), 검사 순서·403/404 정리(#183), 통합·동시성 테스트(#171).
- **최초 본사 관리자 생성 수단이 없다**(미결). 시작 시 초기화나 시드가 없어 새 환경에서는 DB에 직접 넣어야 한다. 방식(환경변수 기반 시작 시 초기화, 마이그레이션 시드, 별도 스크립트)을 정해야 한다.
- `GET /users/{userId}` 미구현(Notion 명세만 있음).
- 보류: 토큰 즉시 무효화(리프레시 토큰 또는 토큰 버전), 로그아웃, 로그인 실패 횟수 제한·rate limiting, 가입 반려·비활성화 사유 기록, 비밀번호 변경·재설정.
- 별도 저장소(wms-client)의 로그인 연동은 이 도메인 범위가 아니다.
