# StockAllocationService

`outbound/application/service/StockAllocationService.java` · 구현: `StockAllocationUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

**재고 할당·해제(서비스 A).** 지점 발주 항목의 잔여 수량을 **FEFO 순서**로 재고 행에 예약한다. 상태 확인과 변경은 발주 헤더를 잠근 상태에서 한 트랜잭션으로 처리하고, 재고 행 잠금과 수량 변경은 재고 서비스에 맡긴다.

## 의존

`StockAllocationRepository`, `OutboundQueryRepository`, `OutboundStockPort`, `StoreOrderFulfillmentUseCase`, `StatusHistoryUseCase`

## 메서드

| 메서드 | 트랜잭션 | 허용 역할 |
|---|---|---|
| `allocate(command, actor)` | 쓰기 | WAREHOUSE_MANAGER |
| `release(command, actor)` | 쓰기 | WAREHOUSE_MANAGER |
| `searchAllocations(condition, actor)` | 읽기 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getAllocation(id, actor)` | 읽기 | HQ_ADMIN, WAREHOUSE_MANAGER |

## allocate

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 처리 사용자, 발주 ID 필수 | 400 |
| 2 | **발주 헤더 비관적 락** 조회 | 404 |
| 3 | 배정 창고가 담당 창고 | 403 |
| 4 | 발주가 ASSIGNED | 409 |
| 5 | 발주 항목을 락 조회, 요청 SKU(없으면 전체)로 대상 선택. 발주에 없는 SKU면 400, `skuIds`가 빈 목록이면 400 | 400 |
| 6 | 대상 중 `unallocatedQuantity > 0`인 항목만 | 하나도 없으면 409 `ALREADY_ALLOCATED` |
| 7 | **FEFO 배치** (`placeByFefo`) | 하나도 못 채우면 409 `INSUFFICIENT_STOCK` |
| 8 | 할당 저장 → **재고 행 예약**(`outboundStockPort.allocate`, 재고 행 ID 오름차순) → **발주 항목 할당 수량 증가**(`increaseAllocated`) | 재고 도메인 409 |
| 9 | 할당마다 상태 이력(`null → ALLOCATED`) | |

### FEFO 배치 (`placeByFefo`)

1. 대상 SKU의 후보 재고 행을 `findFefoCandidates`로 한 번에 조회한다. 품질·로트·구역이 모두 가용이고 `보유 - 할당 > 0`인 행이며, **유통기한이 빠른 순(없으면 뒤) → 로트 생성이 빠른 순 → 재고 행 ID 순**이다.
2. 항목별로 잔여 수량을 후보 행에 순서대로 채운다. **한 항목이 여러 행에 나뉠 수 있다.**
3. 같은 재고 행을 여러 항목이 쓸 수 있으므로 행별 남은 수량을 `availableByLot`에 누적해 차감한다.
4. 재고가 모자란 항목은 **가용 수량만큼만** 채우고 모자란 수량을 `shortages`에 담는다.

### 부분 할당

재고가 모자란 항목은 **가용 수량만큼만 할당**하고 부족분을 `shortages`로 돌려준다. **하나도 할당하지 못할 때만** `INSUFFICIENT_STOCK`으로 거절한다. `StockAllocationUseCase` Javadoc도 이 동작에 맞춰져 있다.

## release

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 사유 필수(공백뿐이면 거절), 500자 이하 | 400 |
| 2 | 잠그지 않는 조회로 발주를 먼저 찾고 **발주 → 할당 순**으로 잠금 | 404 |
| 3 | 담당 창고 | 403 |
| 4 | 할당이 ALLOCATED | 409 |
| 5 | 취소되지 않은 출고에 연결돼 있지 않음 | 409 `ALLOCATION_IN_OUTBOUND` |
| 6 | 해제 → 재고 행 할당 수량 감소(`release`) → 발주 항목 할당 수량 감소 → 상태 이력(사유) | |

보유 수량은 그대로이고 예약만 풀린다.

## 조회

| 메서드 | 규칙 |
|---|---|
| `searchAllocations` | `actor.warehouseScope(warehouseId)`로 범위 계산. 정렬은 할당 일시 내림차순 |
| `getAllocation` | 할당의 발주 창고가 담당 창고여야 한다. `outboundId`는 **취소되지 않은 출고에 연결된 경우에만** 채운다 |

## 잠금 순서

발주 헤더 → (할당 저장) → 발주 항목 → 재고 도메인의 구역 → 재고 행. 전역 락 순서는 성능 개선 단계에서 다룬다고 코드 주석에 적혀 있다.
