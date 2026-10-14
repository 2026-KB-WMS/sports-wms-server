# WarehouseController

`warehouse/adapter/in/web/WarehouseController.java` · `@RestController` · `/api/v1/warehouses` · Swagger 태그 "창고"

## 의존

`WarehouseUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerWarehouse` | POST `/` | 201 | `WarehouseRegisterRequest` | `WarehouseResponse` | 창고 등록 (코드는 서버 발급) | HQ_ADMIN |
| `getWarehouses` | GET `/` | 200 | 쿼리 `keyword`, `isActive` | `ItemsResponse<WarehouseSummaryResponse>` | 전체 창고 목록 | HQ_ADMIN |
| `getMyWarehouses` | GET `/my` | 200 | - | `ItemsResponse<WarehouseMembershipResponse>` | 로그인한 창고 관리자의 소속 창고 | WAREHOUSE_MANAGER |
| `getWarehouse` | GET `/{warehouseId}` | 200 | - | `WarehouseResponse` | 단건 조회 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `updateWarehouse` | PATCH `/{warehouseId}` | 200 | `WarehouseUpdateRequest` | `WarehouseResponse` | 부분 수정 | HQ_ADMIN |
| `deactivateWarehouse` | PATCH `/{warehouseId}/deactivate` | 200 | - | `WarehouseResponse` | 비활성화 | HQ_ADMIN |
| `activateWarehouse` | PATCH `/{warehouseId}/activate` | 200 | - | `WarehouseResponse` | 활성화 | HQ_ADMIN |

허용 역할은 `SecurityConfig`의 경로 규칙이다. 단건 조회의 **담당 창고 범위 검사는 서비스**(`getWarehouse(id, actor)`)가 한다. `/my`는 토큰 주체(`principal.userId()`) 본인의 소속만 돌려준다.

## 경로 주의

`/my`는 `/{warehouseId}`보다 구체적인 리터럴 경로라서 먼저 매칭된다.
