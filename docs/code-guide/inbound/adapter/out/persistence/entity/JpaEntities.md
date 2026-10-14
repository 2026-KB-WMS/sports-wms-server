# JPA 엔티티

`inbound/adapter/out/persistence/entity/` · 엔티티끼리 연관관계 없이 `Long xxxId`로만 참조한다(ADR-005). 수량은 모두 `BIGINT`다.

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `InboundJpaEntity` | inbound | inbound_id | `uk_inbound_no`(inbound_no) | inbound_no(50), purchase_order_id, warehouse_id, status(20), arrived_at(**not null**), received_at, received_by, note(1000) |
| `InboundLineJpaEntity` | inbound_line | inbound_line_id | `uk_inbound_line_inbound_po_line_lot`(inbound_id, purchase_order_line_id, lot_id) | inbound_id, purchase_order_line_id, lot_id, accepted_section_id, defect_section_id, received/accepted/defective_quantity, received_unit_price(18,2), line_amount(18,2), price_change_reason(500), inspection_note(1000), received_at, received_by, created_at |
| `PurchaseOrderJpaEntity` | purchase_order | purchase_order_id | `uk_purchase_order_no`(purchase_order_no) | purchase_order_no(50), warehouse_id, supplier_id, status(20), expected_at, note(1000), created_by |
| `PurchaseOrderLineJpaEntity` | purchase_order_line | purchase_order_line_id | `uk_purchase_order_line_order_sku`(purchase_order_id, sku_id) | purchase_order_id, sku_id, expected_quantity, received_quantity, line_amount(18,2), ordered_unit_price(18,2), status(20) |
| `SupplierJpaEntity` | supplier | supplier_id | `uk_supplier_code`(supplier_code) | supplier_code(30), name(200), manager_name(100, not null), contact_number(30, not null), email(255), address(500), latitude·longitude(10,7), status(20) |

## 참고

- `InboundLineJpaEntity`는 **검수마다 항목을 통째로 교체하고 `updated_at`이 없어** `BaseTimeEntity`를 상속하지 않고 `@CreationTimestamp`로 `created_at`만 둔다. 나머지 엔티티는 `BaseTimeEntity`를 상속한다.
- 구역 참조 컬럼(`accepted_section_id`, `defect_section_id`)은 null을 허용한다(검수 때 미리 지정하지 않을 수 있다). 창고 도메인의 구역 삭제 가드는 이 컬럼을 보고 참조 여부를 판단한다.
