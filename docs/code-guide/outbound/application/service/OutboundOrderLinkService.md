# OutboundOrderLinkService

`OutboundOrderLinkUseCase`(출고 `port.in`)의 구현이다. 지점 발주 도메인이 출고·재고 할당을 조회하고 정리할 때 발주 쪽 어댑터(`storeorder/adapter/out/outbound/StoreOrderOutboundAdapter`)가 이 유스케이스를 호출한다. 발주와 출고는 서로의 엔티티를 참조하지 않고 ID로만 연결하며(ADR-005), 상태는 이름 문자열로 내보낸다.

| 항목 | 내용 |
|---|---|
| 위치 | `outbound/application/service/OutboundOrderLinkService.java` |
| 구현 | `OutboundOrderLinkUseCase` |
| 의존 | `OutboundRepository`, `StockAllocationRepository`, `OutboundStockPort`, `StoreOrderPort`, `StatusHistoryUseCase` |

## 조회 메서드 (읽기 전용 트랜잭션)

| 메서드 | 설명 |
|---|---|
| `findLatestOutboundStatus(id)` | 발주의 출고를 생성 순으로 읽고 **마지막** 출고 상태 |
| `findLatestOutboundStatuses(ids)` | 여러 발주를 한 번에. 같은 발주의 뒤 출고가 앞 출고를 덮어쓴다 |
| `findOutbounds(id)` | 출고 목록(취소 포함)을 `OrderOutboundSummary`로 |
| `existsPickingStarted(id)` | PICKING·PICKED·SHIPPED·DELIVERED 출고가 있는지 |
| `existsInProgressOutbound(id)` | READY·PICKING·PICKED·SHIPPED 출고가 있는지 |
| `existsActiveFulfillment(id)` | ALLOCATED 할당이 있거나 **취소되지 않은** 출고가 있는지 |

## 쓰기 메서드 (`Propagation.MANDATORY`)

호출한 **발주 서비스의 트랜잭션에 참여**하며, 발주 헤더는 호출 쪽이 이미 잠근 상태다.

| 메서드 | 동작 |
|---|---|
| `cancelFulfillment(id, changedBy)` | **READY 출고를 먼저 취소**(상태 이력 사유 "발주 취소로 인한 자동 처리") → ALLOCATED 할당 해제(`releaseAllocated`). 출고를 먼저 취소하므로 그 출고에 묶였던 할당도 함께 해제된다. 반환: 해제한 할당 수, 취소한 출고 수 |
| `releaseUnlinkedAllocations(id, changedBy)` | 출고에 묶이지 않고 남은 ALLOCATED 할당을 해제(사유 "발주 부분 종결로 인한 자동 해제"). 출고는 건드리지 않는다 |

## releaseAllocated

1. 발주의 ALLOCATED 할당을 **잠그고** 조회한다(할당 ID 오름차순).
2. 각 할당을 `release`하고 상태 이력(`ALLOCATED → RELEASED`)을 남긴다.
3. 재고 행별·발주 항목별 수량을 `TreeMap`으로 합산해 **ID 오름차순**으로 `outboundStockPort.release`와 `storeOrderPort.decreaseAllocated`를 호출한다.
4. 해제한 할당 수를 반환한다.

잠금 순서: 할당 → 발주 항목, 재고 행은 재고 서비스가 잠근다.
