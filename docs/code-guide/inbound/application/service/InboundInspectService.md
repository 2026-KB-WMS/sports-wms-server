# InboundInspectService

`inbound/application/service/InboundInspectService.java` · 구현: `InboundInspectUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

입고 **검수**. 호출마다 검수 항목 **전체를 교체**하고, 항목마다 로트를 찾거나 만든다(ADR-004). **재고·구역 사용량·발주 항목은 바꾸지 않는다**(완료 처리에서 반영).

## 의존

`InboundRepository`, `PurchaseOrderRepository`, `LotPort`, `InboundSectionPort`, `StatusHistoryUseCase`

## inspectInbound(inboundId, command, actor)

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 요청 자체 검증 (`validateRequest`) | 400 |
| 2 | 입고 행 비관적 락 조회 | 404 `INBOUND_NOT_FOUND` |
| 3 | 담당 창고 확인 | 403 |
| 4 | ARRIVED·INSPECTING인지 | 409 `CONFLICT` |
| 5 | 발주 조회, 요청의 발주 항목 확인 (`resolvePurchaseOrderLines`) | 404 / 400 |
| 6 | 단가 차이 사유 확인 (`validatePrices`) | 400 |
| 7 | 발주 잔여 수량 확인 (`validateRemainingQuantities`) | 400 |
| 8 | 구역 확인 (`validateSections`) | 400 |
| 9 | 항목마다 로트 find-or-create → `InboundLine.register` | 로트 409 |
| 10 | `inbound.inspect()` 저장, 기존 항목 삭제 후 새 항목 저장 | |
| 11 | **상태가 바뀐 경우에만** 상태 이력(`ARRIVED → INSPECTING`) 기록 | |

검수 중에 항목만 다시 저장하는 호출은 상태가 바뀌지 않으므로 이력을 남기지 않는다.

## 검증 규칙

### validateRequest (DB 없이 판단)

| 항목 | 규칙 |
|---|---|
| 처리자 | 필수 |
| 항목 목록 | 1개 이상 |
| purchaseOrderLineId | 필수 |
| lotNumber | 필수, 100자 이하 |
| 유통기한 | 제조일 이후 |
| receivedQuantity | 0 초과 |
| acceptedQuantity, defectiveQuantity | 0 이상, **합격 + 불량 = 입고 수량** |
| receivedUnitPrice | 필수, 0 이상, 소수 2자리까지 |
| priceChangeReason | 500자 이하 |
| inspectionNote | 1000자 이하 |
| (발주 항목, 로트 번호) | 요청 안에서 중복 불가 |

오류 메시지에는 `lines[i].필드` 위치가 들어간다.

### 업무 규칙

| 규칙 | 설명 |
|---|---|
| 발주 항목 소속 | 항목이 없으면 404, **다른 발주의 항목이면 400** |
| 단가 변경 사유 | 발주 단가와 입고 단가가 **다르면 `priceChangeReason` 필수** |
| 잔여 수량 | 같은 발주 항목을 여러 로트로 나눠 받을 수 있으므로 **발주 항목별 합계**로 잔여 수량과 비교 |
| 구역 | 입고 창고의 **활성** 구역이어야 하고, 합격 구역은 DEFECT가 아니며 불량 구역은 DEFECT여야 한다 |
| 구역 지정 시점 | 검수 때 미리 지정할 수 있지만 **완료 전에는 반드시** 지정돼 있어야 한다 |

## 상수

`LOT_NUMBER_MAX_LENGTH=100`, `PRICE_CHANGE_REASON_MAX_LENGTH=500`, `INSPECTION_NOTE_MAX_LENGTH=1000`, `PRICE_MAX_SCALE=2`
