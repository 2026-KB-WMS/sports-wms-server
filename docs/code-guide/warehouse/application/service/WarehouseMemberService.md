# WarehouseMemberService

`warehouse/application/service/WarehouseMemberService.java` · 구현: `WarehouseMemberUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

창고 관리자 배정·조회·배정 해제.

## 의존

`WarehouseMemberRepository`, `WarehouseRepository`, `WarehouseMemberUserPort`(auth 도메인 연결은 `WarehouseMemberUserAdapter`)

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `assignManager(command)` | 쓰기 | 아래 검증 순서 |
| `getManagers(warehouseId, userId, keyword)` | 읽기 | `warehouseId`가 있으면 존재 확인(`WAREHOUSE_NOT_FOUND`) 후 검색. null인 조건은 무시 |
| `releaseManager(memberId)` | 쓰기 | 없으면 `MEMBER_NOT_FOUND`, 있으면 삭제 |

## assignManager 검증 순서

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | `memberRole`이 허용 값(MANAGER)인지 | 400 `VALIDATION_ERROR` |
| 2 | 창고 존재 | 404 `WAREHOUSE_NOT_FOUND` |
| 3 | 창고 활성 | 409 `CONFLICT` |
| 4 | 사용자 존재 (`warehouseMemberUserPort.getCandidate`) | 404 `USER_NOT_FOUND` |
| 5 | 사용자 역할이 `WAREHOUSE_MANAGER` | 400 `VALIDATION_ERROR` |
| 6 | 사용자 상태가 `ACTIVE` | 409 `CONFLICT` (승인 대기·비활성) |
| 7 | 같은 (창고, 사용자) 배정이 없음 | 409 `ALREADY_ASSIGNED` |

승인 전 사용자의 소속은 이 API가 아니라 **가입 승인 요청**(`PATCH /users/{userId}`)에서 함께 지정한다.

결과는 `WarehouseMemberView`로, 사용자 이름·로그인 아이디를 확인한 사용자 정보로 채운다.
