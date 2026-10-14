# outbound 도메인 enum

`outbound/domain/enums/`

## OutboundStatus

| 값 | 의미 |
|---|---|
| READY | 출고 생성, 피킹 대기 |
| PICKING | 피킹 진행 중 |
| PICKED | 피킹 완료 (재고 차감 완료) |
| SHIPPED | 출고(상차)·배송 시작 |
| DELIVERED | 배송 완료 |
| CANCELED | 취소 (**READY에서만**) |

전이: `READY → PICKING → PICKED → SHIPPED → DELIVERED`, `READY → CANCELED`.

지점 발주 도메인의 `StoreOrderOutboundStatus`는 이 enum과 같은 이름을 가진 **별도 enum**이다(발주 도메인이 출고 패키지에 의존하지 않기 위해). 연동 어댑터가 `valueOf(status.name())`으로 옮긴다.

## AllocationStatus

| 값 | 의미 |
|---|---|
| ALLOCATED | 재고가 예약된 상태 |
| PICKED | 피킹 완료로 재고가 차감된 상태 |
| RELEASED | 할당이 해제되어 예약 재고가 반환된 상태 |

전이: `ALLOCATED → PICKED`, `ALLOCATED → RELEASED`.
