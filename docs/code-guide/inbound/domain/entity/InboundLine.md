# InboundLine

`inbound/domain/entity/InboundLine.java`

입고 **검수 항목**. 발주 항목(`PurchaseOrderLine`) 하나에 대해 **로트별로** 실제 받은 수량, 합격·불량 수량, 확정 단가, 적치 구역을 기록한다.

한 발주 항목을 여러 로트로 나눠 받을 수 있어 같은 발주 항목이 로트를 달리해 여러 줄 나올 수 있다. 검수(inspect) 호출마다 입고의 검수 항목 **전체를 교체**하므로 생성 후 수정하지 않는다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| inboundLineId | Long | PK |
| inboundId | Long | 입고 |
| purchaseOrderLineId | Long | 대응하는 발주 항목 |
| lotId | Long | 로트 (검수 시 find-or-create) |
| acceptedSectionId | Long | 합격품을 둘 구역 (선택, 완료 전에는 필요) |
| defectSectionId | Long | 불량품을 둘 구역 (선택, 완료 전에는 필요) |
| receivedQuantity | Long | 입고(검수) 수량 |
| acceptedQuantity | Long | 합격 수량. null이면 0 |
| defectiveQuantity | Long | 불량 수량. null이면 0 |
| receivedUnitPrice | BigDecimal | 입고 확정 단가 |
| lineAmount | BigDecimal | 항목 금액 = 입고 수량 × 확정 단가 |
| priceChangeReason | String | 발주 단가와 다를 때의 사유 |
| inspectionNote | String | 검수 비고 |
| receivedAt, receivedBy | LocalDateTime, Long | 검수 처리 시각·처리자 |
| createdAt | LocalDateTime | |

(입고, 발주 항목, 로트)는 유니크(`uk_inbound_line_inbound_po_line_lot`)다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(...)` | 검수 항목 생성. 입고 수량 > 0, 합격·불량 ≥ 0, **합격 + 불량 = 입고 수량**, 단가 ≥ 0을 검증하고(`IllegalArgumentException`) `lineAmount`를 계산 |
| `hasRequiredSections()` | 합격 수량이 있으면 합격 구역이, 불량 수량이 있으면 불량 구역이 지정돼 있는지. **완료 전 확인 기준** |

발주 잔여 수량 초과, 단가 차이 사유, 구역 유효성처럼 다른 객체가 필요한 검증은 `InboundInspectService`가 한다.
