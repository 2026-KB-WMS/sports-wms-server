# inventory 도메인 enum

`inventory/domain/enums/`

## QualityStatus

재고 행(`InventoryLot`)의 품질 상태.

| 값 | 의미 |
|---|---|
| AVAILABLE | 가용 |
| DEFECTIVE | 불량. 할당할 수 없다 |

## LotStatus

로트 마스터(`Lot`)의 상태.

| 값 | 의미 |
|---|---|
| AVAILABLE | 가용 |
| EXPIRED | 만료 |
| QUARANTINED | 격리 |
| DISPOSED | 폐기 |

전이 규칙은 `Lot.canTransitionTo`를 본다.

## TransactionType

재고 변동 이력의 유형.

| 값 | 의미 |
|---|---|
| INBOUND | 입고 |
| OUTBOUND | 출고 |
| ADJUSTMENT | 조정 |

## ReferenceType

이력이 참조하는 원천 문서 유형.

| 값 | 의미 |
|---|---|
| INBOUND | 입고 (`referenceId` = inboundId) |
| OUTBOUND | 출고 (`referenceId` = outboundId) |
| ADJUSTMENT | 재고 조정 (원천 문서 없음, `referenceId` null) |

## 가용 수량 기준

여러 조회가 공통으로 쓰는 정의: **재고 품질 상태와 로트 상태가 모두 AVAILABLE인 행의 `on_hand - allocated`**. 하나라도 AVAILABLE이 아니면 가용 수량은 0으로 본다.
