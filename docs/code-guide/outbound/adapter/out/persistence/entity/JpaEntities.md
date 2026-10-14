# JPA 엔티티

`outbound/adapter/out/persistence/entity/` · 엔티티끼리 연관관계 없이 `Long xxxId`로만 참조한다(ADR-005). 수량은 `BIGINT`다.

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `OutboundJpaEntity` | outbound | outbound_id (IDENTITY) | `uk_outbound_no`(outbound_no) | outbound_no(30), store_order_id, status(20), shipped_at, shipped_by, delivered_at, note(500) |
| `OutboundLineJpaEntity` | outbound_line | outbound_line_id | `uk_outbound_line_outbound_allocation`(outbound_id, allocation_id) | outbound_id, allocation_id, shipped_quantity, confirmed_unit_supply_price(18,2, 확정 전 null), created_at |
| `StockAllocationJpaEntity` | stock_allocation | allocation_id | 없음 | store_order_line_id, inventory_lot_id, allocated_quantity, picked_quantity, status(20), allocated_at, released_at, allocated_by |

## 참고

- `OutboundJpaEntity`, `StockAllocationJpaEntity`는 `BaseTimeEntity`를 상속한다.
- `OutboundLineJpaEntity`는 테이블에 `updated_at`이 없어 `BaseTimeEntity` 대신 `@EntityListeners(AuditingEntityListener)`와 `@CreatedDate`로 `created_at`만 직접 관리한다.
- 한 출고 안에서 같은 할당을 두 번 묶을 수 없다(`uk_outbound_line_outbound_allocation`). 다른 출고는 같은 할당을 묶을 수 있지만, 취소되지 않은 출고에 연결된 할당은 다시 묶이지 않는다(서비스 쿼리가 거른다).
