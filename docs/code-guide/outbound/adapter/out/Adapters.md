# 다른 도메인 연결 어댑터

`outbound/adapter/out/{inventory,product,storeorder}/`

## OutboundStockAdapter

| 항목 | 내용 |
|---|---|
| 위치 | `inventory/OutboundStockAdapter.java` |
| 구현 포트 | `OutboundStockPort` |
| 의존 | `InventoryStockUseCase`(재고 도메인) |
| 동작 | `allocate`/`release`: `StockQuantity` → `StockQuantityCommand`. `ship`: `StockPick` → `StockShipCommand`(재고 행, 할당 수량, 피킹 수량, 출고 ID, 처리자). 결과 `InventoryLot`을 `StockState`(재고 행 ID, 보유, 할당)로 변환 |

## OutboundSkuSupplyPriceAdapter

| 항목 | 내용 |
|---|---|
| 위치 | `product/OutboundSkuSupplyPriceAdapter.java` |
| 구현 포트 | `SkuSupplyPricePort` |
| 의존 | `ProductSkuUseCase` |
| 동작 | `findCurrentSupplyPrice(skuId)`: SKU의 현재 공급 단가를 `Optional`로 돌려준다(없으면 빈 값) |
| 사용처 | `OutboundFulfillmentService.completePicking`(발주 항목 스냅샷이 없을 때) |
