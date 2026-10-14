# Outbound

`outbound/domain/entity/Outbound.java`

**출고. 지점 발주 하나에 대한 출고 작업 단위.** 이미 만들어 둔 재고 할당(`StockAllocation`)들을 한 출고로 묶어 피킹·배송한다.

## 상태 전이

```
READY ──startPicking()──▶ PICKING ──completePicking()──▶ PICKED ──ship()──▶ SHIPPED ──deliver()──▶ DELIVERED
  │
  └── cancel() ──▶ CANCELED        (READY에서만 취소 가능)
```

| 전이 | 메서드 | 부수 효과 |
|---|---|---|
| (생성) READY | `create(outboundNo, storeOrderId, note)` | |
| READY → PICKING | `startPicking()` | 이후 연결된 할당은 해제할 수 없고 발주도 취소할 수 없다 |
| PICKING → PICKED | `completePicking()` | 재고 차감 완료 |
| PICKED → SHIPPED | `ship(shippedAt, shippedBy)` | 배송 시작 일시·처리자 기록 |
| SHIPPED → DELIVERED | `deliver(deliveredAt)` | 배송 완료 일시 기록 |
| READY → CANCELED | `cancel()` | |

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| outboundId | Long | PK |
| outboundNo | String | 출고 번호 `OB-YYYYMMDD-0001` (유니크) |
| storeOrderId | Long | 지점 발주 |
| status | OutboundStatus | |
| shippedAt, shippedBy | LocalDateTime, Long | 배송 시작 일시·처리자 |
| deliveredAt | LocalDateTime | 배송 완료 일시 |
| note | String | 비고 (500자 이하) |
| createdAt, updatedAt | LocalDateTime | |

창고는 출고가 직접 갖지 않고 **발주에 배정된 창고**를 따른다.

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `create(...)` | READY로 생성. 번호·발주 필수 | `IllegalArgumentException` |
| `startPicking`, `completePicking`, `ship`, `deliver`, `cancel` | 위 표의 전이 | 상태가 맞지 않으면 `IllegalStateException` |
| `isPickingStarted()` | READY도 CANCELED도 아닌 상태(PICKING 이후) | |
| `isInProgress()` | 종료 상태가 아님 | |
| `isTerminal()` | DELIVERED·CANCELED | |

한 발주에 출고를 **여러 건** 만들 수 있다(할당을 나눠 묶는 경우).
