# OutboundService

`outbound/application/service/OutboundService.java` · 구현: `OutboundUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

**출고 생성·피킹 시작·취소와 조회(서비스 B).** 피킹 완료·배송은 `OutboundFulfillmentService`(서비스 C)가 맡는다.

## 의존

`OutboundRepository`, `StockAllocationRepository`, `OutboundQueryRepository`, `StoreOrderFulfillmentUseCase`, `StatusHistoryUseCase`

## 잠금 규칙 (`lockOutbound`)

**발주 헤더 → 출고 헤더** 순으로 잠근다. 출고만 보고 잠그면 발주 취소(발주를 잠근 채 출고를 처리)와 순서가 어긋나기 때문이다.

1. 잠그지 않는 조회(`findOutboundView`)로 출고의 발주 ID를 얻는다.
2. 발주를 `getOrderForUpdate`로 잠그고 **담당 창고**를 확인한다.
3. 출고를 `findByIdForUpdate`로 잠근다.

그래서 **피킹 시작과 발주 취소가 동시에 들어와도 한쪽만 성공**한다.

## 메서드

| 메서드 | 트랜잭션 | 허용 역할 |
|---|---|---|
| `createOutbound(command, actor)` | 쓰기 | WAREHOUSE_MANAGER |
| `startPicking(outboundId, actor)` | 쓰기 | WAREHOUSE_MANAGER |
| `cancel(command, actor)` | 쓰기 | WAREHOUSE_MANAGER |
| `searchOutbounds(condition, page, actor)` | 읽기 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getOutbound(id, actor)` | 읽기 | HQ_ADMIN, WAREHOUSE_MANAGER |

## createOutbound

배정된 발주에서 **출고에 아직 묶이지 않은 ALLOCATED 할당**을 한 출고(READY)로 묶는다. **재고·할당·발주 수량은 바뀌지 않는다.**

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 처리 사용자, 발주 ID 필수, 비고 500자 이하 | 400 |
| 2 | 발주 비관적 락, 담당 창고 | 404 / 403 |
| 3 | 발주가 ASSIGNED | 409 |
| 4 | 취소되지 않은 출고에 연결되지 않은 ALLOCATED 할당을 락 조회. `skuIds`가 있으면 그 SKU의 항목에 속한 것만 | 빈 목록이면 400, 발주에 없는 SKU면 400 |
| 5 | 묶을 할당이 없음 | 409 `NO_ALLOCATION` |
| 6 | 출고 번호 채번, 출고·항목 저장, 상태 이력(`null → READY`) | |

`skuIds`를 생략하면 연결되지 않은 할당 **전부**, 주면 그 SKU의 할당만 묶는다. 이렇게 한 발주에 출고를 여러 건 만들 수 있다.

## startPicking

| 검사 | 오류 |
|---|---|
| 출고가 READY | 409 |
| 발주가 **ASSIGNED**(보류·취소 등이면 거절) | 409 `ORDER_NOT_ASSIGNED` |
| 연결된 할당 전부 존재하고 ALLOCATED | 409 `ALLOCATION_NOT_ACTIVE` |

통과하면 PICKING으로 전환하고 상태 이력을 남긴다. 이후 연결된 할당은 해제할 수 없고 발주도 취소할 수 없다.

## cancel

| 검사 | 오류 |
|---|---|
| 사유 필수, 500자 이하 | 400 |
| 출고가 **READY** | 409 |

재고·할당·발주 상태는 그대로 두고, 할당은 다시 묶거나 해제할 수 있게 된다. 사유와 처리자는 상태 이력에 남는다.

## 조회

| 메서드 | 규칙 |
|---|---|
| `searchOutbounds` | 범위 계산(`warehouseScope`), 기간 검증(400). 창고는 **출고가 아니라 발주에 배정된 창고** 기준 |
| `getOutbound` | 발주 창고가 담당 창고여야 함. **취소된 출고에만** 상태 이력에서 취소 사유를 채운다 |

## 출고 번호

`OB-YYYYMMDD-NNNN`. 당일 접두어 개수 + 1. 번호가 겹치면 유니크 제약이 409 `DUPLICATE_OUTBOUND_NO`로 응답한다.
