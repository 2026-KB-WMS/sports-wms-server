# InventoryTransaction

`inventory/domain/entity/InventoryTransaction.java`

모든 **보유 수량(on_hand) 변동**을 남기는 감사 이력. 생성 후 변경하지 않으며 추가만 한다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| transactionId | Long | PK |
| inventoryLotId | Long | 변동된 재고 행 |
| transactionType | TransactionType | INBOUND / OUTBOUND / ADJUSTMENT |
| quantityDelta | Long | 변동 수량 (`after - before`) |
| beforeQuantity, afterQuantity | Long | 변동 전·후 보유 수량 |
| referenceType | ReferenceType | 원천 문서 유형 (INBOUND / OUTBOUND / ADJUSTMENT) |
| referenceId | Long | 원천 문서 ID. 재고 조정처럼 문서가 없으면 null |
| reason | String | 사유 (조정에서만 채운다, 최대 500자) |
| createdBy | Long | 처리 사용자 |
| createdAt | LocalDateTime | 기록 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `record(inventoryLotId, type, before, after, referenceType, referenceId, reason, createdBy)` | 이력 생성. `quantityDelta`는 `after - before`로 계산 |

## 남기는 경우

| 동작 | 이력 |
|---|---|
| 입고 반영 (`receive`) | INBOUND, 행마다 |
| 출고 차감 (`ship`) | OUTBOUND, 피킹 수량이 0보다 클 때 |
| 재고 조정 (`adjust`) | ADJUSTMENT |
| 할당·할당 해제 | **남기지 않는다** (보유 수량이 그대로라서, API 명세 기준) |
