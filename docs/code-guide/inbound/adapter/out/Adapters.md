# 다른 도메인 연결 어댑터

`inbound/adapter/out/{inventory,product,warehouse}/` · 입고 도메인의 아웃바운드 포트를 **다른 도메인의 유스케이스에 연결**한다. 도메인 간 직접 의존을 어댑터 한 곳으로 모은다.

| 어댑터 | 위치 | 구현 포트 | 의존 | 동작 |
|---|---|---|---|---|
| `InboundStockAdapter` | `inventory/` | `InboundStockPort` | `InventoryStockUseCase` | `StockReceipt` → `StockReceiveCommand`(입고의 `StockQuality`를 재고 품질 상태 이름 문자열로 `switch` 매핑)로 바꿔 `receive`. 반환된 `StockLotView`를 `ReceivedStock`(구역, 로트, `StockQuality`, 재고 행 ID)으로 변환. 모르는 품질 상태 이름은 거절 |
| `LotAdapter` | `inventory/` | `LotPort` | `LotUseCase` | `findOrRegisterLot`을 `LotRegisterCommand`로 바꿔 `findOrRegister`, 로트 ID 반환 |
| `SkuPurchasePriceAdapter` | `product/` | `SkuPurchasePricePort` | `ProductSkuUseCase` | `getPurchasablePrice(skuId)`: SKU 조회 → **비활성이면 409 `CONFLICT`**("비활성 SKU는 발주할 수 없습니다") → 매입 단가가 null이면 `PURCHASE_PRICE_MISSING` → 현재 매입 단가 반환 |
| `InboundSectionAdapter` | `warehouse/` | `InboundSectionPort` | `WarehouseSectionUseCase` | `getSection(sectionId)`: 구역 `WarehouseSectionView`를 `SectionInfo`(sectionId, warehouseId, `SectionKind`, active)로 변환. 구역 유형 문자열은 `DEFECT`면 `SectionKind.DEFECT`, 아니면 `OTHER` |
| `WarehouseAvailabilityAdapter` | `warehouse/` | `WarehouseAvailabilityPort` | `WarehouseUseCase` | `requireExists`: 조회만(없으면 창고 도메인이 404). `requireActive`: 비활성이면 409 "비활성 창고에는 발주를 등록할 수 없습니다" |

## 사용처

| 어댑터 | 호출하는 서비스 |
|---|---|
| `InboundStockAdapter` | `InboundCompleteService` |
| `LotAdapter`, `InboundSectionAdapter` | `InboundInspectService` |
| `SkuPurchasePriceAdapter`, `WarehouseAvailabilityAdapter(requireActive)` | `PurchaseOrderService.registerPurchaseOrder` |
| `WarehouseAvailabilityAdapter(requireExists)` | `InboundService.getInbounds`, `PurchaseOrderService.getPurchaseOrders` |
