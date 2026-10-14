# 인바운드 포트 (UseCase)

`inventory/application/port/in/` · 컨트롤러뿐 아니라 **다른 도메인이 호출하는 진입점**이 많다.

| UseCase | 구현 | 메서드 | 호출자 |
|---|---|---|---|
| `InventoryQueryUseCase` | `InventoryQueryService` | `getInventories`, `getInventoriesByLot`, `getInventory`, `getLowStock`, `getTransactions`, `getTransactionsOf`, `getAllocatableStocks`, `hasStockInSection`, `hasStockInWarehouse` | `InventoryController`, `LotController`, 창고(`StockPresenceAdapter`). `getAllocatableStocks`는 현재 호출하는 곳이 없다 |
| `InventoryAdjustmentUseCase` | `InventoryAdjustmentService` | `adjust` | `InventoryController` |
| `InventoryStockUseCase` | `InventoryStockService` | `receive`, `allocate`, `release`, `ship` | 입고(`InboundStockAdapter`), 출고(`OutboundStockAdapter`) |
| `LotUseCase` | `LotService` | `getLots`, `getLot`, `getLotInbounds`, `changeLotStatus`, `findOrRegister` | `LotController`, 입고(`LotAdapter`) |

## 구분

| 구분 | 설명 |
|---|---|
| 사용자 요청용 | `AuthenticatedUser actor`를 받아 담당 창고 범위로 제한. 컨트롤러가 호출 |
| 내부 진입점 | `InventoryStockUseCase`, `getAllocatableStocks`, `has...`, `findOrRegister`. 호출자의 트랜잭션에 참여하고 권한 검사 없음 |

## 다른 도메인과의 경계

입고·출고·창고 도메인은 이 UseCase를 직접 호출하지 않고 **자기 도메인의 포트 + 어댑터**를 거친다(예: 입고의 `adapter/out/inventory/InboundStockAdapter`). 도메인 간 직접 의존을 어댑터 한 곳으로 모으기 위해서다.

## 하위 패키지

`command/Commands.md`, `query/SearchConditions.md`, `result/Results.md`
