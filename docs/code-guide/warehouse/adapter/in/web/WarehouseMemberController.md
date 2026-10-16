# WarehouseMemberController

`warehouse/adapter/in/web/WarehouseMemberController.java` · `@RestController` · `/api/v1/warehouses/managers` · Swagger 태그 "창고 관리자"

## 의존

`WarehouseMemberUseCase`, `WarehouseUseCase`(응답에 창고명을 채우려고)

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `assignManager` | POST `/` | 201 | `WarehouseMemberAssignRequest` | `WarehouseMemberResponse` | 창고 관리자 배정 |
| `getManagers` | GET `/` | 200 | 쿼리 `warehouseId`, `userId`, `keyword`, `page`, `size`, `sort` (모두 선택) | `PageResponse<WarehouseMemberResponse>` | 배정 목록(정렬 필드: `assignedAt`, `userName`, `loginId`, `memberRole`) |
| `releaseManager` | DELETE `/{warehouseMemberId}` | 200 | - | `WarehouseMemberReleaseResponse` | 배정 해제 |

세 엔드포인트 모두 HQ_ADMIN 전용(`SecurityConfig`의 `/warehouses/**` 규칙: GET `/warehouses/managers`는 HQ_ADMIN, 그 외는 `/warehouses/**` 기본 규칙).

## 응답 조립

`toResponse(view)`가 `warehouseUseCase.getWarehouse(warehouseId).getName()`으로 창고명을 조회해 `WarehouseMemberResponse.of(view, warehouseName)`을 만든다. 목록에서는 배정마다 창고를 조회한다.
