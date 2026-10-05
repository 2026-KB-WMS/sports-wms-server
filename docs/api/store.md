# Store API 명세

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/stores`. 목록 API는 `data.items`만 반환(페이지네이션 보류). 404는 도메인별 코드 사용.

## 구현 대비 메모

- Notion의 상태 컬럼은 이 도메인 전부 "시작 전"이지만 코드는 구현되어 있음(상태 컬럼이 오래됨, 코드 기준으로 판단).
- Notion 명세의 `pageInfo`와 일반 `NOT_FOUND`/`CONFLICT`는 현재 구현 기준(페이지네이션 보류, 도메인별 404)과 다름 → conventions.md 기준 따름.
- 목록 API는 `page`·`size`·`sort`를 받지 않고 고정 정렬을 쓴다(각 절의 "정렬" 참고). 지점 담당자 배정 목록은 `keyword`를 지원하지 않고 응답에 `userName`·`loginId`도 없다(회원 도메인 구현 전). 인증 연동 전이라 `GET /stores/my`는 쿼리 파라미터 `userId`(필수)로 사용자를 받는다.
- 지점 비활성화의 `STORE_IN_USE` 검사는 구현했다(#141). 지점 행을 잠그지 않아 같은 순간의 발주 등록과는 경합할 수 있다(그렇게 생긴 발주는 승인할 수 없고 취소·반려만 가능).
- 확정 필요(미결): 지점 재활성화 방법, 토큰의 소속 정보 갱신 시점, 점주 0명이 되는 회수 허용 여부, 진행 중 발주가 있는 점주의 회수 차단 여부.

## 엔드포인트 목록 (10)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | /stores | HQ_ADMIN | 지점 등록 |
| GET | /stores | HQ_ADMIN | 전체 지점 목록 |
| GET | /stores/{storeId} | HQ_ADMIN, STORE_OWNER(담당 지점) | 지점 상세 |
| PATCH | /stores/{storeId} | HQ_ADMIN | 지점 정보 수정 |
| PATCH | /stores/{storeId}/deactivate | HQ_ADMIN | 지점 비활성화 |
| GET | /stores/my | STORE_OWNER | 내 소속 지점 |
| GET | /stores/management-types | 인증 사용자 | 지점 내 역할 코드 목록 |
| GET | /stores/managers | HQ_ADMIN | 지점 담당자 배정 목록 |
| POST | /stores/assign | HQ_ADMIN | 지점 담당자 배정 |
| DELETE | /stores/managers/{storeMemberId} | HQ_ADMIN | 배정 회수(하드 삭제) |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`. `isActive` = `status == 'ACTIVE'`. 상태 전이는 ACTIVE→INACTIVE만, 이미 INACTIVE면 409.

## POST /stores (P0)

- Body: `storeCode`(≤30, unique), `storeName`(≤100), `address`(≤500), `contactName`(선택, ≤100), `contactNumber`(≤30, 필수)
- 201. 응답: `storeId, storeCode, storeName, address, contactName, contactNumber, isActive, createdAt, updatedAt`
- 에러: `DUPLICATE_STORE_CODE` 409 (DB unique 위반도 동일 매핑). 등록 시 status ACTIVE. 점주 배정은 별도(`POST /stores/assign`).

## GET /stores (P0)

- HQ_ADMIN 전용(점주는 403, `GET /stores/my` 사용). Query: `keyword`(지점명·코드), `isActive`(생략 시 전체). `sort`는 받지 않는다.
- 정렬은 고정(등록 일시 `createdAt` 내림차순, 같으면 `storeId` 내림차순).
- 응답 항목: `storeId, storeCode, storeName, address, contactName, contactNumber, isActive`. 지점별 발주·재고 현황은 미포함.

## GET /stores/{storeId} (P1)

- HQ_ADMIN 전체, STORE_OWNER는 배정된 지점만(그 외 403), WAREHOUSE_MANAGER 불가. 비활성 지점도 조회 가능.
- 응답: 목록 항목 + `createdAt, updatedAt`. 재고는 `GET /inventory`, 발주는 `GET /orders/my`.

## PATCH /stores/{storeId} (P1)

- 부분 수정(≥1 필드): `storeName`, `address`, `contactName`(null이면 비움), `contactNumber`
- `storeCode`, `isActive` 포함 시 400. 비활성 지점도 수정 가능. 에러: 404.

## PATCH /stores/{storeId}/deactivate (P2)

- Body(선택): `reason`(≤500) → `StatusHistory.reason`에 저장.
- 응답: `storeId, storeCode, storeName, isActive=false, updatedAt`. 실제로는 지점 상세와 같은 전체 필드(`address, contactName, contactNumber, createdAt`도 포함)를 내려준다(위 필드의 상위 집합).
- 에러: 409 `CONFLICT`(이미 INACTIVE), 409 `STORE_IN_USE`(진행 중 지점 발주 `REQUESTED`/`APPROVED`/`ASSIGNED`/`ON_HOLD` 존재; `COMPLETED`/`CANCELED`/`REJECTED`는 무시)
- 비활성화 후 신규 지점 발주 등록·점주 배정 차단. 기존 이력·배정 데이터 보존. 조건 확인과 상태 변경은 같은 트랜잭션.

## GET /stores/my (P2)

- STORE_OWNER 전용. 토큰 주체 본인의 소속 지점을 `items[]`로 반환(페이지네이션 없음, 미배정이면 빈 배열, 비활성 지점도 `isActive:false`로 포함).
- 응답 항목: 지점 필드 + `storeMemberId, memberRole, assignedAt`

## GET /stores/management-types (P0)

- 고정 코드 목록 `items[{code,name}]`: `OWNER`/점주, `MANAGER`/지점 관리자. 페이지네이션 없음. `POST /stores/assign`의 `memberRole` 검증 값과 동일.

## GET /stores/managers (P2)

- HQ_ADMIN. Query: `storeId`, `userId`. `keyword`(사용자 이름·로그인 ID)는 회원 도메인 구현 후 추가한다(현재 미지원). `sort`는 받지 않는다.
- 정렬은 고정(배정 일시 `assignedAt` 내림차순, 같으면 `storeMemberId` 내림차순).
- 응답 항목: `storeMemberId, storeId, storeName, userId, memberRole, assignedAt`. `userName`, `loginId`는 회원 도메인 구현 후 추가한다(현재 미제공, 민감 정보는 제외). 같은 `userId`가 여러 지점으로 여러 행 가능.
- 에러: 404(존재하지 않는 필터 값)

## POST /stores/assign (P2)

- Body: `storeId`, `userId`(role `STORE_OWNER`), `memberRole`(management-types의 code)
- 201. 응답: `storeMemberId, storeId, storeName, userId, memberRole, assignedAt`(`userName`은 회원 도메인 구현 후 추가)
- 에러: 400(필수 누락/허용되지 않은 `memberRole`/대상이 STORE_OWNER 아님), 404(지점·사용자 없음), 409 `ALREADY_ASSIGNED`(같은 지점 중복, DB 제약도 동일 매핑), 409 `CONFLICT`(비활성 지점 또는 INACTIVE 사용자)
- PENDING 계정도 배정 가능(소속 배정 후 `PATCH /users/{userId}`로 ACTIVE 승인). 서로 다른 지점 배정은 허용. `assignedAt`은 서버 시각, 단일 트랜잭션.

## DELETE /stores/managers/{storeMemberId} (P2)

- 하드 삭제(단순 매핑 엔티티). 계정·과거 발주 이력 유지. 응답 200 `{storeMemberId}`.
- 에러: 404(없음/이미 회수됨), 400(숫자 아님). 회수 후 해당 지점 접근 불가(다른 지점 배정은 유지).
