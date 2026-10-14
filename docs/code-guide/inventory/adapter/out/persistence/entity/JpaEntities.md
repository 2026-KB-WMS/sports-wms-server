# JPA 엔티티

`inventory/adapter/out/persistence/entity/` · 엔티티끼리 연관관계 없이 `Long xxxId`로만 참조한다(ADR-005). 수량 컬럼은 모두 `BIGINT`(`Long`)다.

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `InventoryLotJpaEntity` | inventory_lot | inventory_lot_id (IDENTITY) | `uk_inventory_lot_section_lot`(section_id, lot_id) | section_id, lot_id, on_hand_quantity, allocated_quantity, quality_status(20), last_counted_at |
| `LotJpaEntity` | lot | lot_id | `uk_lot_sku_supplier_number`(sku_id, supplier_id, lot_number) | sku_id, supplier_id, lot_number(100), manufactured_date, expiry_date, status(20), unit_cost(18,2) |
| `InventoryTransactionJpaEntity` | inventory_transaction | transaction_id | 없음 | inventory_lot_id, transaction_type(30), quantity_delta, before_quantity, after_quantity, reference_type(30, **not null**), reference_id(null 허용), reason(500), created_by, created_at |

## 참고

- `InventoryLotJpaEntity`, `LotJpaEntity`는 `BaseTimeEntity`를 상속한다.
- `InventoryTransactionJpaEntity`는 **변경 없는 감사 이력**이라 `updated_at`이 없어 `BaseTimeEntity`를 상속하지 않고 `@CreationTimestamp`로 `created_at`만 둔다.
- `reference_type`은 not null이지만 `reference_id`는 null을 허용한다(재고 조정은 원천 문서가 없다).
- `quality_status`, `status`, `transaction_type`, `reference_type`은 모두 `EnumType.STRING`이다.
