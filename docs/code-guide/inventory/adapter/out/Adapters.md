# 다른 도메인 연결 어댑터

`inventory/adapter/out/product/`, `inventory/adapter/out/warehouse/` · 재고 도메인의 아웃바운드 포트를 **다른 도메인의 유스케이스에 연결**한다. 재고 도메인이 다른 도메인의 내부 구조를 알지 않도록 어댑터 한 곳으로 모은다.

## SkuStatusAdapter

| 항목 | 내용 |
|---|---|
| 위치 | `adapter/out/product/SkuStatusAdapter.java` |
| 구현 포트 | `SkuStatusPort` |
| 의존 | `ProductSkuUseCase`(상품 도메인) |
| 메서드 | `requireActive(skuId)`: `productSkuUseCase.getSkuView(skuId)`로 조회해 **비활성이면 `SKU_NOT_ACTIVE`**. 없는 SKU는 상품 도메인이 `SKU_NOT_FOUND`를 던진다 |
| 사용처 | `InventoryStockService.receive`, `allocate` |

## SectionCapacityAdapter

| 항목 | 내용 |
|---|---|
| 위치 | `adapter/out/warehouse/SectionCapacityAdapter.java` |
| 구현 포트 | `SectionCapacityPort` |
| 의존 | `WarehouseSectionCapacityUseCase`(창고 도메인) |
| 메서드 | `lock`, `occupy`, `vacate`를 그대로 위임 |
| 사용처 | `InventoryStockService`, `InventoryAdjustmentService` |
