# OutboundLine

`outbound/domain/entity/OutboundLine.java`

출고 항목. **재고 할당 하나와 1:1**로 연결된다. 피킹이 완료되면 출고 수량과 **확정 공급 단가**가 기록된다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| outboundLineId | Long | PK |
| outboundId | Long | 출고 |
| allocationId | Long | 연결된 재고 할당 (출고 안에서 유니크) |
| shippedQuantity | long | 출고(피킹) 수량. 피킹 확정 전에는 0 |
| confirmedUnitSupplyPrice | BigDecimal | 피킹 완료 때 확정한 공급 단가. 확정 전 null |
| createdAt | LocalDateTime | `updated_at` 없음 |

SKU, 로트, 구역, 할당 수량은 이 항목이 아니라 **연결된 할당·재고 행에서 ID 기준 조인**으로 가져온다.

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `create(outboundId, allocationId)` | 수량 0으로 생성. 할당 ID 필수 | `IllegalArgumentException` |
| `confirmPicking(picked, allocated, unitSupplyPrice)` | 피킹 확정. **한 번만** 가능, `0 <= picked <= allocated`, 단가 ≥ 0 | 이미 확정이면 `IllegalStateException`, 수량·단가는 `IllegalArgumentException` |
| `isPickingConfirmed()` | 단가가 확정됐는지 | |
| `lineAmount()` | `단가 × 출고 수량`. 확정 전에는 null | |
