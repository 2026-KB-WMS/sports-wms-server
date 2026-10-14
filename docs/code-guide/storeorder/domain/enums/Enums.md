# storeorder 도메인 enum

`storeorder/domain/enums/`

## StoreOrderStatus

| 값 | 의미 | 종결 |
|---|---|---|
| REQUESTED | 점주가 요청, 승인 대기 | |
| APPROVED | 본사 승인, 창고 배정 대기 | |
| ASSIGNED | 창고 배정됨 (출고 진행) | |
| ON_HOLD | 창고 관리자가 출고 보류 | |
| COMPLETED | 출고 종결 (전량 또는 부분) | ✔ |
| CANCELED | 취소 | ✔ |
| REJECTED | 반려 | ✔ |

전이: `REQUESTED → APPROVED → ASSIGNED ↔ ON_HOLD`, `ASSIGNED → COMPLETED`, `REQUESTED → REJECTED`, 진행 중 상태 → `CANCELED`.

## StoreOrderLineStatus

| 값 | 의미 |
|---|---|
| REQUESTED | 출고 전 |
| PARTIALLY_SHIPPED | 일부 출고 |
| COMPLETED | 전량 출고 |
| CANCELED | 발주 취소·반려로 취소 |

## StoreOrderOutboundStatus

지점 발주 쪽에서 읽는 **출고 상태 값**. 출고 도메인의 출고 진행 상태와 같은 이름이지만 **출고 패키지에 의존하지 않도록** 따로 둔다. 연동 어댑터가 출고 상태를 이 값으로 옮긴다.

| 값 | 의미 |
|---|---|
| READY | 출고 준비 |
| PICKING | 피킹 중 |
| PICKED | 피킹 완료 |
| SHIPPED | 배송 중 |
| DELIVERED | 배송 완료 |
| CANCELED | 취소 |

| 메서드 | 설명 |
|---|---|
| `isPickingStarted()` | PICKING·PICKED·SHIPPED·DELIVERED. 발주를 **취소·보류할 수 없게** 만드는 기준 (`ORDER_IN_PICKING`) |
| `isInProgress()` | READY·PICKING·PICKED·SHIPPED. 부분 출고 종결을 막는 기준 (`OUTBOUND_IN_PROGRESS`) |

## StoreOrderProgressStage

점주용 **진행 단계**. DB에 저장하지 않는 읽기 전용 파생값이다. 입력은 발주 상태 + 가장 최근 출고 상태 + 부족 수량 여부.

| 값 | 의미 |
|---|---|
| PENDING_APPROVAL | 승인 대기 |
| REJECTED | 반려됨 |
| CANCELED | 취소됨 |
| AWAITING_ASSIGNMENT | 승인됨 (창고 배정 중) |
| PREPARING | 상품 준비 중 (할당·출고 준비·피킹을 묶어서 표현) |
| IN_TRANSIT | 배송 중 |
| PARTIALLY_DELIVERED | 일부 배송 완료 (남은 수량 준비 중) |
| ON_HOLD | 재고 확보 중 |
| COMPLETED | 배송 완료 (부족 수량 없음) |
| COMPLETED_PARTIAL | 일부 수량만 배송 완료 (부족 수량 있음) |

### resolve(status, latestOutboundStatus, hasShortage)

| 발주 상태 | 결과 |
|---|---|
| REQUESTED | PENDING_APPROVAL |
| REJECTED | REJECTED |
| CANCELED | CANCELED |
| APPROVED | AWAITING_ASSIGNMENT |
| ON_HOLD | ON_HOLD |
| COMPLETED | `hasShortage`면 COMPLETED_PARTIAL, 아니면 COMPLETED |
| ASSIGNED | 최근 출고가 SHIPPED → IN_TRANSIT, **DELIVERED → PARTIALLY_DELIVERED**, 그 외 PREPARING |

ASSIGNED인데 최근 출고가 DELIVERED이면 "일부만 배송된 상태"다. 전량 배송이면 시스템이 발주를 COMPLETED로 바꾸기 때문이다. 매핑 표는 `docs/api/store-order.md`의 "공통 정의"에 있다.
