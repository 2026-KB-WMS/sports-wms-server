# 다른 도메인 연결 어댑터

`storeorder/adapter/out/{product,store,warehouse,outbound}/` · 지점 발주 도메인의 아웃바운드 포트를 다른 도메인의 유스케이스에 연결한다. (출고 도메인 연결은 `outbound/` 폴더의 `StoreOrderOutboundAdapter`가 출고의 `port.in`(`OutboundOrderLinkUseCase`)을 호출해 `StoreOrderOutboundPort`를 구현한다.)

| 어댑터 | 위치 | 구현 포트 | 의존 | 동작 |
|---|---|---|---|---|
| `SkuSupplyPriceAdapter` | `product/` | `SkuSupplyPricePort` | `ProductSkuUseCase` | `getOrderableSupplyPrice(skuId)`: SKU 조회 → **비활성이면 409** "비활성 SKU는 발주할 수 없습니다" → 공급 단가가 null이면 `SUPPLY_PRICE_MISSING` → 현재 공급 단가 반환 |
| `SkuAvailabilityAdapter` | `product/` | `SkuAvailabilityPort` | `ProductSkuUseCase` | `requireActive(skuId)`: 비활성이면 409 "비활성 SKU가 포함된 발주는 승인할 수 없습니다". 상품을 비활성화하면 하위 SKU도 함께 비활성화되므로 SKU 상태만 확인하면 된다 |
| `StoreAvailabilityAdapter` | `store/` | `StoreAvailabilityPort` | `StoreUseCase` | `requireExists`: 조회만(없으면 지점 도메인이 404). `requireActive`: 비활성이면 409 "비활성 지점의 발주는 등록하거나 승인할 수 없습니다" |
| `StoreOrderWarehouseAvailabilityAdapter` | `warehouse/` | `WarehouseAvailabilityPort` | `WarehouseUseCase` | `requireExists`: 조회만. `requireActive`: 비활성이면 409 "비활성 창고는 배정하거나 재개할 수 없습니다" |

## 사용처

| 어댑터 | 호출하는 곳 |
|---|---|
| `SkuSupplyPriceAdapter` | `registerStoreOrder` |
| `SkuAvailabilityAdapter` | `approveStoreOrder` |
| `StoreAvailabilityAdapter` | `registerStoreOrder`(active), `approveStoreOrder`(active), 목록 필터(exists) |
| `StoreOrderWarehouseAvailabilityAdapter` | `assignStoreOrder`(active), `resumeStoreOrder`(active), 목록 필터(exists) |
