# InventoryController

`inventory/adapter/in/web/InventoryController.java` · `@RestController` · Swagger 태그 "재고"

클래스 레벨 `@RequestMapping`이 없고 메서드마다 전체 경로를 쓴다. 페이지네이션은 프로젝트 전체에 아직 없어 `page/size/sort`를 받지 않고 전체 목록을 반환한다.

## 의존

`InventoryQueryUseCase`, `InventoryAdjustmentUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 |
|---|---|---|---|---|---|
| `getInventories` | GET `/api/v1/inventory` | 200 | `skuId`, `warehouseId`, `keyword` | `ItemsResponse<InventorySkuSummaryResponse>` | SKU 기준 집계 |
| `getInventoriesByLot` | GET `/api/v1/inventory/by-lot` | 200 | `skuId`, `warehouseId`, `sectionId`, `expiringBefore`(날짜), `qualityStatus`, `includeEmpty` | `ItemsResponse<InventoryLotViewResponse>` | 로트·구역 단위 재고 |
| `getInventory` | GET `/api/v1/inventory/{inventoryId}` | 200 | - | `InventoryDetailResponse` | 재고 상세 |
| `getLowStock` | GET `/api/v1/inventory/low-stock` | 200 | `warehouseId`, `keyword` | `ItemsResponse<LowStockItemResponse>` | 안전 재고 미만 |
| `getTransactions` | GET `/api/v1/inventory/transactions` | 200 | `warehouseId`, `sectionId`, `skuId`, `lotId`, `transactionType`, `referenceType`, `referenceId`, `createdFrom`, `createdTo`(ISO 날짜시간) | `ItemsResponse<InventoryTransactionResponse>` | 변동 이력 목록 |
| `getTransactionsOf` | GET `/api/v1/inventory/{inventoryId}/transactions` | 200 | `transactionType`, `createdFrom`, `createdTo` | `InventoryTransactionHistoryResponse` | 한 재고 행의 이력 |
| `adjust` | POST `/api/v1/inventory/adjustments` | 201 | `InventoryAdjustRequest` | `InventoryAdjustmentResponse` | 재고 조정 |

## 허용 역할

| 엔드포인트 | 역할 |
|---|---|
| `POST /inventory/adjustments` | WAREHOUSE_MANAGER |
| 그 밖의 `/inventory/**` | HQ_ADMIN, WAREHOUSE_MANAGER (점주 불가) |

담당 창고 범위 검사는 서비스가 한다. 조정 처리자는 토큰 주체(`principal.userId()`)다.

## 경로 주의

`/by-lot`, `/low-stock`, `/transactions`는 `/{inventoryId}`보다 구체적인 리터럴 경로라서 먼저 매칭된다.
