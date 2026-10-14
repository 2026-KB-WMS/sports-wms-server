# Inbound

`inbound/domain/entity/Inbound.java`

발주에 대한 **실제 입고(도착) 처리를 기록하는 입고 헤더**. 검수 항목(`InboundLine`)은 `inboundId`로 참조하며 이 객체가 직접 들고 있지 않는다.

하나의 발주에 부분 입고가 여러 번 걸칠 수 있어 **같은 발주에 입고를 여러 건** 둘 수 있다.

## 상태 전이

```
ARRIVED ──inspect()──▶ INSPECTING ──complete()──▶ COMPLETED
   │                        │
   └────── cancel() ────────┴──▶ CANCELED
```

`INSPECTING`에서 `inspect()`를 다시 호출하면 상태는 유지되고 검수 항목만 교체된다. `COMPLETED` 이후에는 취소할 수 없다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| inboundId | Long | PK |
| inboundNo | String | 입고 번호 `IB-YYYYMMDD-0001` (유니크) |
| purchaseOrderId | Long | 대상 발주 |
| warehouseId | Long | 입고 창고. **발주의 창고를 따른다** |
| status | InboundStatus | 생성 시 null이면 ARRIVED |
| arrivedAt | LocalDateTime | 도착 일시 |
| receivedAt | LocalDateTime | 입고 완료 일시 (완료 전 null) |
| receivedBy | Long | 입고 완료 처리자 (완료 전 null) |
| note | String | 비고 |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `register(inboundNo, purchaseOrderId, warehouseId, arrivedAt, note)` | ARRIVED로 생성. 도착 일시가 없으면 호출 측이 현재 시각을 채운다 | |
| `inspect()` | ARRIVED → INSPECTING. 이미 INSPECTING이면 유지 | `IllegalStateException` (COMPLETED·CANCELED) |
| `complete(receivedBy, receivedAt)` | INSPECTING → COMPLETED, 처리자·완료 시각 기록 | `IllegalStateException` |
| `cancel()` | ARRIVED·INSPECTING → CANCELED | `IllegalStateException` |
| `isArrived()`, `isInspecting()` | 상태 확인 | |
| `isInspectable()` | 검수 항목을 저장·교체할 수 있는 상태(ARRIVED·INSPECTING) | |
| `isCancelable()` | `isInspectable()`과 같다 | |

서비스가 먼저 상태를 검사해 409로 응답하고, 도메인의 예외는 최후 방어선이다(ADR-008).
