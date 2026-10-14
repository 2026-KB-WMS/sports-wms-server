# OutboundFulfillmentService

`outbound/application/service/OutboundFulfillmentService.java` · 구현: `OutboundFulfillmentUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

**피킹 완료·배송 시작·배송 완료(서비스 C).** 재고 차감(피킹 완료)과 발주 수량·상태 전환(피킹 완료·배송 완료)이 들어 있다. 허용 역할은 WAREHOUSE_MANAGER다.

## 의존

`OutboundRepository`, `StockAllocationRepository`, `OutboundQueryRepository`, `OutboundStockPort`, `SkuSupplyPricePort`, `StoreOrderFulfillmentUseCase`, `StatusHistoryUseCase`

## 잠금 순서

**발주 헤더 → 출고 헤더 → 할당 → 발주 항목**이고, 재고 행은 재고 서비스가 잠근다(`OutboundService`와 같다). 코드 주석에 따르면 전역 락 순서는 성능 개선 단계에서 다룬다.

## completePicking

PICKING 출고의 항목별 피킹 수량을 확정한다. 재고 차감, 할당 PICKED, 출고 항목 수량·확정 단가, 발주 항목 수량 갱신, 출고 PICKED를 **한 트랜잭션**으로 처리한다.

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 처리 사용자 필수, `lockOutbound`(발주 → 출고 락, 담당 창고) | 400 / 403 |
| 2 | 출고가 PICKING | 409 |
| 3 | **입력 검증**: `lines` 필수, **출고의 모든 항목을 입력**(개수 일치), 항목 ID가 이 출고의 것, 중복 불가, 수량 ≥ 0 | 400 |
| 4 | 할당을 ID 오름차순으로 락 조회, 전부 존재하고 ALLOCATED | 409 `ALLOCATION_NOT_ACTIVE` |
| 5 | 항목별 피킹 수량이 할당 수량을 넘지 않음 | 400 |
| 6 | **전부 0이면 거절** | 409 `NOTHING_PICKED` |
| 7 | 발주 항목을 락 조회하고 **확정 공급 단가**를 정한다 | `SUPPLY_PRICE_MISSING` |
| 8 | **재고 차감**(`outboundStockPort.ship`): 피킹 수량만큼 보유 차감, 할당 전체 해제(부족분 예약은 풀려 다시 가용 재고), 피킹 > 0이면 재고 이력 | 재고 도메인 409 |
| 9 | 할당 `pick`, 출고 항목 `confirmPicking`, 저장 | |
| 10 | **발주 항목 반영**(`applyPicked`): 할당 수량 감소 + 출고 수량 증가 | |
| 11 | 출고를 PICKED로, 상태 이력(할당 `ALLOCATED → PICKED`, 출고 `PICKING → PICKED`) | |

### 확정 공급 단가 결정 (`resolvePrices`)

1. **발주 항목의 공급 단가 스냅샷**(`requestedUnitSupplyPrice`)
2. 없으면 SKU의 **현재 공급 단가**(`SkuSupplyPricePort`)
3. 둘 다 없으면 `SUPPLY_PRICE_MISSING`

발주 항목을 못 찾으면 `ALLOCATION_NOT_ACTIVE`로 거절한다.

### 결과

항목별 할당·피킹·**부족 수량**, 확정 단가, 금액, 차감 뒤 재고 행 수량(`InventoryState`)과 `hasShortage`(부족분이 있는 항목이 하나라도 있는지)를 돌려준다. 부족분은 발주 항목의 미출고 수량으로 남아 **다시 할당**할 수 있다.

## ship

PICKED 출고를 SHIPPED로 바꾼다. **재고·할당은 바뀌지 않는다.**

| 검사 | 오류 |
|---|---|
| 출고가 PICKED | 409 |

배송 시작 일시(`now`)와 처리자를 기록하고 상태 이력을 남긴다.

## deliver

SHIPPED 출고를 DELIVERED로 바꾸고 발주를 갱신한다.

| 순서 | 동작 |
|---|---|
| 1 | 출고가 SHIPPED인지(409), `deliver(now)`, 상태 이력 |
| 2 | 출고 항목의 할당을 락 조회해 발주 항목 ID를 구하고 `refreshLineStatuses`로 **항목 상태를 출고 수량 기준으로 재계산**(PARTIALLY_SHIPPED·COMPLETED) |
| 3 | 같은 발주에 **진행 중 출고(READY·PICKING·PICKED·SHIPPED)가 남아 있으면** 발주 상태는 그대로 |
| 4 | 진행 중 출고가 없으면 `completeIfFulfilled`: 모든 항목이 전량 출고됐고 발주가 ASSIGNED면 **COMPLETED로 자동 전환** |

응답에는 배송 완료 반영 뒤 **발주 상태**(전량이면 COMPLETED, 아니면 ASSIGNED)가 담긴다.

## 관찰

- `completePicking`, `ship`, `deliver`는 발주 상태(ASSIGNED 여부)를 확인하지 않는다. 확인하는 곳은 `startPicking`(`ORDER_NOT_ASSIGNED`)뿐이다. 다만 발주를 취소·보류하려면 피킹이 시작되지 않아야 하므로(`ORDER_IN_PICKING`) 피킹 이후 단계의 발주는 상태가 바뀌지 않는다.
