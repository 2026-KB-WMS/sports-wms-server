# JPA 엔티티

`storeorder/adapter/out/persistence/entity/` · 엔티티끼리 연관관계 없이 `Long xxxId`로만 참조한다(ADR-005). 수량은 `BIGINT`다.

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `StoreOrderJpaEntity` | store_order | store_order_id (IDENTITY) | `uk_store_order_no`(order_no) | order_no(50), store_id, **warehouse_id(null 허용)**, status(20), requested_at(not null), requested_delivery_at, note(1000), created_by |
| `StoreOrderLineJpaEntity` | store_order_line | store_order_line_id | `uk_store_order_line_order_sku`(store_order_id, sku_id) | store_order_id, sku_id, requested_quantity, allocated_quantity, shipped_quantity, requested_unit_supply_price(18,2), status(20) |

두 엔티티 모두 `BaseTimeEntity`를 상속한다. 발주 금액·부족 수량은 컬럼이 아니라 쿼리에서 계산한다.
