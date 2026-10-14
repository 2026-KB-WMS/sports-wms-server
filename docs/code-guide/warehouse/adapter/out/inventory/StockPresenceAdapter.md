# StockPresenceAdapter

`warehouse/adapter/out/inventory/StockPresenceAdapter.java` · `@Component` · 구현: `StockPresencePort`

창고 도메인의 "재고가 남아 있는가" 질문을 **재고 도메인의 조회 유스케이스로 연결**하는 어댑터. 창고 도메인이 재고 도메인의 내부 구조를 알지 않도록 포트 뒤에 둔다.

## 의존

`InventoryQueryUseCase`(inventory 도메인)

## 메서드

| 메서드 | 설명 |
|---|---|
| `hasStockInSection(sectionId)` | `inventoryQueryUseCase.hasStockInSection` 위임 |
| `hasStockInWarehouse(warehouseId)` | `inventoryQueryUseCase.hasStockInWarehouse` 위임 |

## 사용처

- `WarehouseSectionService.deactivateSection`, `deleteSection`: 구역 재고 확인
- `WarehouseService.deactivateWarehouse`: 창고 재고 확인
