# SupplierOrderService

`inbound/application/service/SupplierOrderService.java` · 구현: `SupplierOrderUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

공급처 발주서 목록·상세 조회. 발주서는 발주 확정(`PurchaseOrderService.confirmPurchaseOrder`)으로만 만들어지고 수동 변경 API가 없다.

## 의존

`SupplierOrderQueryRepository`, `PurchaseOrderRepository`, `SupplierRepository`, `WarehouseAvailabilityPort`

## 메서드

| 메서드 | 설명 |
|---|---|
| `getSupplierOrders(condition, actor)` | 담당 창고 범위로 좁히고(비담당 창고 지정 시 403) 필터 대상(창고·공급처·발주) 존재를 확인한 뒤 조회. 등록 시작 일시가 종료 일시보다 늦으면 400 |
| `getSupplierOrder(id, actor)` | 헤더와 SKU별 항목. 없으면 `SUPPLIER_ORDER_NOT_FOUND`, 다른 창고 발주서면 403 |
