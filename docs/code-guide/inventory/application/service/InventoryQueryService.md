# InventoryQueryService

`inventory/application/service/InventoryQueryService.java` · 구현: `InventoryQueryUseCase` · `@Transactional(readOnly = true)`

재고·재고 이력 조회. 사용자 요청용 조회는 모두 `AuthenticatedUser`를 받아 **담당 창고 범위**로 제한한다(본사는 전체). 범위 밖이면 403이다.

## 의존

`InventoryQueryRepository`, `InventoryLotRepository`, `LotRepository`

## 범위 처리

| 개념 | 설명 |
|---|---|
| 범위 계산 | `actor.warehouseScope(requestedWarehouseId)`: 창고를 지정하면 권한 확인 후 null(추가 제한 없음), 지정하지 않으면 본사는 null, 그 외는 담당 창고 목록 |
| 구역 지정 | `scopeOf(actor, warehouseId, sectionId)`: 구역을 지정하면 그 구역의 창고도 담당 창고여야 한다(본사 제외). 없는 구역은 이후 검증에서 404 |
| 조건 객체 | 컨트롤러가 `unscoped(...)`로 만든 조건에 서비스가 `warehouseIds`를 채워 새 조건을 만들어 리포지토리에 넘긴다 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `getInventories(condition, page, actor)` | SKU 기준 집계. SKU·창고 존재 검증(404) 후 `findSkuSummaries` |
| `getInventoriesByLot(condition, actor)` | 로트·구역 단위 행 전체(페이지 없음, 로트 상세용). SKU·창고·구역 존재 검증 |
| `searchInventoriesByLot(condition, page, actor)` | 같은 검증 후 페이지로 자른 로트·구역 단위 행 |
| `getInventory(inventoryLotId, actor)` | 상세. 없으면 `INVENTORY_NOT_FOUND`, 담당 창고가 아니면 403 |
| `getLowStock(condition, actor)` | 안전 재고 미만 SKU. 창고 존재 검증 |
| `getTransactions(condition, page, actor)` | 이력 목록. 기간 검증(`createdFrom`이 `createdTo`보다 뒤면 400), `referenceId`만 있고 `referenceType`이 없으면 400, SKU·창고·구역·로트 존재 검증 |
| `getTransactionsOf(inventoryLotId, condition, actor)` | 한 재고 행의 이력. 기간 검증 후 `getInventory`로 권한·존재 확인, 이후 해당 행으로 조회 |
| `getAllocatableStocks(warehouseId, skuId)` | 출고 할당 후보. **범위 검사 없는 내부 조회**이며 FEFO 순서. `src/main`에서 호출하는 곳이 없다(출고 도메인은 자체 FEFO 쿼리 `OutboundQueryRepository.findFefoCandidates`로 후보를 구한 뒤 `InventoryStockUseCase.allocate`를 쓴다) |
| `hasStockInSection(sectionId)`, `hasStockInWarehouse(warehouseId)` | 창고 도메인의 비활성화·삭제 가드용 |

## 오류 처리 순서

범위 계산(권한) → 기간·조합 검증(400) → 필터 대상 존재 검증(404) → 조회.
