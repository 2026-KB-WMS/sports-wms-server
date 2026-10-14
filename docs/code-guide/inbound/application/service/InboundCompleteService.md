# InboundCompleteService

`inbound/application/service/InboundCompleteService.java` · 구현: `InboundCompleteUseCase` · 클래스 기본 `@Transactional(readOnly = true)`, `completeInbound`는 `@Transactional`

입고 **완료**. 재고 반영부터 입고 상태 전환까지 **한 트랜잭션**이며, 하나라도 실패하면 아무것도 반영하지 않는다.

## 의존

`InboundRepository`, `PurchaseOrderRepository`, `InboundStockPort`, `StatusHistoryUseCase`

## 잠금 순서

**입고 행 → 발주 행 → 발주 항목 행 → (재고 도메인) 구역 행 → 재고 행**

## completeInbound(inboundId, actor)

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 입고 행 비관적 락 조회 | 404 `INBOUND_NOT_FOUND` |
| 2 | 담당 창고 확인 (입고 행을 잠근 직후) | 403 |
| 3 | 상태가 INSPECTING | 409 `CONFLICT` |
| 4 | 검수 항목이 있음 | 409 `INBOUND_HAS_NO_LINES` |
| 5 | 모든 항목에 필요한 구역이 지정됨 (`hasRequiredSections`) | 409 `SECTION_NOT_ASSIGNED` |
| 6 | 발주 행 비관적 락 조회, 발주가 CONFIRMED | 404 / 409 |
| 7 | 발주 항목 행을 ID 오름차순으로 잠금 | |
| 8 | **재고 반영** (`reflectStock`) | 재고·창고 도메인 409 |
| 9 | **발주 항목 입고 수량 누적** (`accumulateReceived`) | 409 |
| 10 | 모든 발주 항목이 COMPLETED면 **발주를 COMPLETED로 전환**하고 상태 이력 기록 | |
| 11 | 입고 완료(`complete`), 저장, 상태 이력(`INSPECTING → COMPLETED`) | |

처리자(`actor.userId()`)는 입고의 `receivedBy`와 재고 이력의 수행자, 상태 이력의 처리자가 된다.

## reflectStock

검수 항목을 입고 반영 단위(`StockReceipt`)로 바꿔 재고 도메인에 넘긴다.

| 수량 | 반영 대상 |
|---|---|
| 합격 수량 > 0 | **합격 구역**의 가용(AVAILABLE) 재고 |
| 불량 수량 > 0 | **불량 구역**의 불량(DEFECTIVE) 재고 |

반환된 재고 행 ID를 `(구역, 로트, 불량 여부)` 키로 모아 항목별 결과(`ReflectedInventory`)를 만든다. 수량이 0인 쪽의 재고 ID는 null이다.

## accumulateReceived

발주 항목별로 이 입고의 **입고 수량(합격 + 불량)** 합계를 누적하고 상태를 갱신한다.

| 검사 | 오류 |
|---|---|
| 검수 항목의 발주 항목이 입고의 발주에 없음 | 409 |
| 합계가 발주 **잔여 수량을 초과** | 409 (잔여·입고 수량 포함) |

## 자동 발주 완료

마지막 입고 완료로 모든 발주 항목이 전량 입고되면 발주가 자동으로 COMPLETED가 된다. 이 전이도 상태 이력에 남기며 **처리자는 입고를 완료한 사용자**다.

## 응답 (`InboundCompleteResult`)

완료된 입고, 항목별 반영된 재고 행, 발주 ID·상태, 갱신된 발주 항목의 누적 수량·상태.
