# PurchaseOrderLine

`inbound/domain/entity/PurchaseOrderLine.java`

창고 발주의 **SKU별 예정 수량** 상세. 입고가 완료될 때마다 `receive()`로 입고 수량을 누적한다. 발주당 SKU는 한 줄만 둘 수 있다(`uk_purchase_order_line_order_sku`).

## 상태

`REQUESTED`(입고 전) → `PARTIALLY_RECEIVED`(일부 입고, received < expected) → `COMPLETED`(전량 입고)

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| purchaseOrderLineId | Long | PK |
| purchaseOrderId | Long | 발주 |
| skuId | Long | SKU |
| expectedQuantity | Long | 발주(예정) 수량 |
| receivedQuantity | Long | 누적 입고 수량. null이면 0 |
| orderedUnitPrice | BigDecimal | **등록 시점의 SKU 매입 단가 스냅샷** |
| lineAmount | BigDecimal | 항목 금액 = 발주 수량 × 매입 단가 |
| status | PurchaseOrderLineStatus | 생성 시 null이면 REQUESTED |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `register(purchaseOrderId, skuId, expectedQuantity, orderedUnitPrice)` | 발주 수량 ≥ 1, 단가 ≥ 0 검증 후 `lineAmount` 계산 | `IllegalArgumentException` |
| `receive(quantity)` | 입고 수량 누적. 누적 ≥ 발주 수량이면 COMPLETED, 아니면 PARTIALLY_RECEIVED | 수량 ≤ 0은 `IllegalArgumentException`, 이미 COMPLETED면 `IllegalStateException` |
| `isCompleted()` | COMPLETED 여부 |
| `remainingQuantity()` | `max(0, expected - received)`. 초과 입고여도 음수가 되지 않는다 |

초과 입고를 허용할지는 입고 서비스가 판단한다(현재는 잔여 수량 초과를 거절한다).
