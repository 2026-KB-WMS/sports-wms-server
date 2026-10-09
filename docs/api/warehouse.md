# Warehouse API 명세

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/warehouses`. 목록 API는 `data.items`만 반환(페이지네이션 보류). 404는 도메인별 코드 사용.

## 구현 대비 메모

- `DELETE /warehouses/sections/{sectionId}`는 Notion 명세(상태 "시작 전")만 있고 **코드에는 미구현**. 구현 시 아래 명세 참고(구역 비활성화 `deactivate`로 대체 가능 여부 재검토).
- Notion 명세의 `pageInfo`와 일반 `NOT_FOUND`/`CONFLICT` 코드는 현재 구현 기준(페이지네이션 보류, 도메인별 404)과 다름 → conventions.md 기준 따름.
- 목록 API는 `page`·`size`·`sort`를 받지 않고 고정 정렬을 쓴다(각 절의 "정렬" 참고). `GET /warehouses/my`는 토큰 주체(`userId`)의 소속 창고를 돌려주며 `userId` 쿼리 파라미터는 받지 않는다(#170에서 인증 적용). 창고·구역 단건과 창고별 구역 목록의 담당 창고 검사는 서비스가 토큰의 `warehouseIds`로 한다.
- 확정 필요(미결): 창고/구역 재활성화 방법, `memberRole` 허용 값(예시 `MANAGER`), `sectionType` 허용 값(`DEFECT` 확정, `ZONE`/`RACK` 예시), 하위 구역 수용량 합 ↔ 상위 구역/창고 `totalCapacity` 관계 검증, 토큰의 소속 정보 갱신 시점, 관리자 0명이 되는 회수 허용 여부, "진행 중 업무" 범위.

## 엔드포인트 목록 (18)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | /warehouses | HQ_ADMIN | 창고 등록 |
| GET | /warehouses | HQ_ADMIN | 전체 창고 목록 |
| GET | /warehouses/{warehouseId} | HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고) | 창고 상세 |
| PATCH | /warehouses/{warehouseId} | HQ_ADMIN | 창고 정보 수정 |
| PATCH | /warehouses/{warehouseId}/deactivate | HQ_ADMIN | 창고 비활성화 |
| GET | /warehouses/my | WAREHOUSE_MANAGER | 내 소속 창고 |
| GET | /warehouses/management-types | 인증 사용자 | 관리자 역할 코드 목록 |
| GET | /warehouses/managers | HQ_ADMIN | 창고 관리자 배정 목록 |
| POST | /warehouses/managers | HQ_ADMIN | 창고 관리자 배정 |
| DELETE | /warehouses/managers/{warehouseMemberId} | HQ_ADMIN | 배정 회수(하드 삭제) |
| GET | /warehouses/section-types | 인증 사용자 | 구역 유형 코드 목록 |
| POST | /warehouses/sections | HQ_ADMIN | 구역 등록 |
| GET | /warehouses/sections | HQ_ADMIN | 전체 구역 목록 |
| GET | /warehouses/sections/{sectionId} | HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고) | 구역 상세 |
| PATCH | /warehouses/sections/{sectionId} | HQ_ADMIN | 구역 수정 |
| PATCH | /warehouses/sections/{sectionId}/deactivate | HQ_ADMIN | 구역 비활성화 |
| DELETE | /warehouses/sections/{sectionId} | HQ_ADMIN | 구역 삭제 (**미구현**) |
| GET | /warehouses/{warehouseId}/sections | HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고) | 창고별 구역 목록 |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`(권한 없는 역할, 또는 창고 관리자가 비담당 창고 접근).
`isActive` = `status == 'ACTIVE'`. 상태 전이는 ACTIVE→INACTIVE만, 이미 INACTIVE면 409.

## 창고

### POST /warehouses (P0)

- Body: `warehouseCode`(≤30, unique), `warehouseName`(≤100), `address`(≤500), `contactNumber`(선택, ≤30), `totalCapacity`(선택, ≥0, 소수 3자리, 기본 0)
- 201. 응답: `warehouseId, warehouseCode, warehouseName, address, contactNumber, totalCapacity, isActive, createdAt, updatedAt`
- 에러: `DUPLICATE_WAREHOUSE_CODE` 409 (DB unique 위반도 동일 매핑). 등록 시 status ACTIVE.

### GET /warehouses

- HQ_ADMIN 전용. Query: `keyword`(창고명·코드), `isActive`(생략 시 전체). `page`·`size`·`sort`는 받지 않는다(페이지네이션 보류).
- 정렬은 고정(등록 일시 `createdAt` 내림차순, 같으면 `warehouseId` 내림차순).
- 응답 항목: `warehouseId, warehouseCode, warehouseName, address, contactNumber, totalCapacity, isActive`. 구역별 수용량은 미포함.

### GET /warehouses/{warehouseId}

- HQ_ADMIN 전체, WAREHOUSE_MANAGER는 배정 창고만(그 외 403), STORE_OWNER 불가. 비활성 창고도 조회 가능. 구역·재고 미포함.
- 응답: 목록 항목 + `createdAt, updatedAt`

### PATCH /warehouses/{warehouseId}

- 부분 수정(≥1 필드): `warehouseName`, `address`, `contactNumber`(null이면 비움), `totalCapacity`
- `warehouseCode`, `isActive` 포함 시 400(상태는 `deactivate` 사용). 비활성 창고도 수정 가능.
- 에러: 창고 없음 404

### PATCH /warehouses/{warehouseId}/deactivate

- 응답: `warehouseId, warehouseCode, warehouseName, isActive=false, updatedAt`. 실제로는 창고 상세와 같은 전체 필드(`address, contactNumber, totalCapacity, createdAt`도 포함)를 내려준다(위 필드의 상위 집합).
- 에러: 이미 INACTIVE 409 `CONFLICT`, 재고 잔량(보유·할당) 또는 진행 중 입고·출고·발주 배정 존재 시 409 `WAREHOUSE_IN_USE`
- 비활성화 후 신규 구역 등록·관리자 배정·발주 배정 차단, 기존 이력 보존. 조건 확인과 상태 변경은 같은 트랜잭션.

### GET /warehouses/my

- WAREHOUSE_MANAGER 전용(그 외 403). 토큰 주체 본인의 소속 창고를 `items[]`로 반환(페이지네이션 없음, 미배정이면 빈 배열, 비활성 창고도 `isActive:false`로 포함).
- 응답 항목: 창고 필드 + `warehouseMemberId, memberRole, assignedAt`

## 창고 관리자 배정

### GET /warehouses/management-types

- 고정 코드 목록 `items[{code,name}]` (예: `MANAGER`/창고 관리자). 페이지네이션 없음. `POST managers`의 `memberRole` 검증 값과 동일해야 함.

### GET /warehouses/managers (P2)

- HQ_ADMIN. Query: `warehouseId`, `userId`. `keyword`(사용자 이름·로그인 ID, 부분 일치·대소문자 무시, 공백만 있으면 조건 없음)로 거른다.
- 정렬은 명시하지 않는다(현재 코드에 정렬 지정 없음).
- 응답 항목: `warehouseMemberId, warehouseId, warehouseName, userId, userName, loginId, memberRole, assignedAt`(password 등 민감 정보는 제외). 사용자 이름·로그인 ID는 사용자 테이블을 ID로 조인해 채운다.
- 같은 `userId`가 여러 창고로 여러 행 가능(UNIQUE는 warehouse_id+user_id). 에러: 존재하지 않는 필터 값 404

### POST /warehouses/managers (P2)

- Body: `warehouseId`, `userId`(role `WAREHOUSE_MANAGER`), `memberRole`(management-types의 code)
- 201. 응답: `warehouseMemberId, warehouseId, warehouseName, userId, userName, loginId, memberRole, assignedAt`
- 에러: 400(필수 누락/허용되지 않은 `memberRole`/대상이 WAREHOUSE_MANAGER 아님), 404(창고 없음 `WAREHOUSE_NOT_FOUND`, 사용자 없음 `USER_NOT_FOUND`), 409 `ALREADY_ASSIGNED`(같은 창고 중복, DB 제약도 동일 매핑), 409 `CONFLICT`(비활성 창고 또는 INACTIVE 사용자)
- PENDING 계정도 배정 가능(소속 배정 후 `PATCH /users/{userId}`로 ACTIVE 승인). 서로 다른 창고 배정은 허용. `assignedAt`은 서버 시각.

### DELETE /warehouses/managers/{warehouseMemberId} (P2)

- 하드 삭제(단순 매핑 엔티티). 계정·과거 이력은 유지. 응답 200 `{warehouseMemberId}`.
- 에러: 404(없음/이미 회수됨), 400(숫자 아님). 회수 후 해당 창고 접근 불가(다른 창고 배정은 유지).

## 구역

### GET /warehouses/section-types (P0)

- 고정 코드 목록 `items[{code,name}]`: `ZONE`/구역, `RACK`/랙, `DEFECT`/불량 구역. `DEFECT`는 입고 검수의 불량품 보관 구역 구분에 사용(`GET /inbounds/{inboundId}/defect-sections`).

### POST /warehouses/sections (P0)

- Body: `warehouseId`, `parentSectionId`(선택, 같은 창고의 활성 구역), `sectionCode`(≤50, 창고 내 unique), `sectionName`(≤100), `sectionType`, `capacity`(선택, ≥0, 소수 3자리, 기본 0)
- 201. 응답: `sectionId, warehouseId, parentSectionId, sectionCode, sectionName, sectionType, capacity, currentCapacity(0), availableCapacity, isActive, createdAt`
- 에러: 400(형식/타 창고의 parent/허용되지 않은 type), 404(창고·상위 구역 없음), 409 `DUPLICATE_SECTION_CODE`, 409 `CONFLICT`(비활성 창고·상위 구역)
- `availableCapacity = capacity - currentCapacity`(저장하지 않음). 수용량 초과 적치는 불가(검증은 입고 검수 시점).

### GET /warehouses/sections

- HQ_ADMIN. Query: `warehouseId`, `parentSectionId`(직속 하위), `sectionType`, `keyword`(구역명·코드), `isActive`. `sort`는 받지 않는다.
- 정렬은 고정(`sectionCode` 오름차순, 같으면 `sectionId` 오름차순).
- 평면 목록(트리는 `parentSectionId`로 구성). 응답 항목: `sectionId, warehouseId, warehouseName, parentSectionId, sectionCode, sectionName, sectionType, capacity, currentCapacity, availableCapacity, isActive`

### GET /warehouses/{warehouseId}/sections

- HQ_ADMIN 전체, WAREHOUSE_MANAGER는 담당 창고만(그 외 403). Query는 위와 동일(`warehouseId` 제외). 비활성 창고의 구역도 조회 가능. 창고 없음 404.

### GET /warehouses/sections/{sectionId}

- HQ_ADMIN 전체, WAREHOUSE_MANAGER는 담당 창고만. 응답: 구역 필드 + `warehouseName, parentSectionCode(최상위면 null), createdAt, updatedAt`. 구역별 로트 재고는 `GET /inventory/by-lot`.

### PATCH /warehouses/sections/{sectionId}

- 부분 수정(≥1 필드): `sectionCode`, `sectionName`, `sectionType`, `capacity`(≥0, ≥ 현재 사용량)
- `warehouseId`, `parentSectionId` 포함 시 400. 자기 자신의 기존 코드와 같으면 변경 아님.
- 에러: 404, 409 `DUPLICATE_SECTION_CODE`, 409 `CAPACITY_BELOW_USAGE`. 사용량 확인과 수정은 같은 트랜잭션.

### PATCH /warehouses/sections/{sectionId}/deactivate

- 응답: `sectionId, warehouseId, sectionCode, sectionName, isActive=false, updatedAt`
- 에러: 409 `CONFLICT`(이미 INACTIVE), 409 `SECTION_HAS_INVENTORY`(보유·할당 수량 잔존), 409 `SECTION_HAS_CHILDREN`(활성 하위 구역 존재)
- 비활성 구역: 신규 하위 구역 등록·입고 적치·재고 증가 조정 차단. 이력은 보존. 구역 행 잠금 사용.

### DELETE /warehouses/sections/{sectionId} (P2, 미구현)

- 재고·이력·하위 구역이 전혀 없는 구역(잘못 등록)만 삭제 가능. 응답 200 `{sectionId}`.
- 에러: 404, 409 `SECTION_HAS_INVENTORY`, 409 `SECTION_HAS_CHILDREN`, 409 `SECTION_IN_USE`(InventoryLot·입고 검수 `accepted_section_id`/`defect_section_id` 참조 → 비활성화 사용)
- 재고 검사와 삭제는 같은 트랜잭션 + 구역 행 잠금.
