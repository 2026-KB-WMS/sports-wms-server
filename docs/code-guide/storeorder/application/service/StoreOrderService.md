# StoreOrderService

`storeorder/application/service/StoreOrderService.java` · 구현: `StoreOrderUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

지점 발주의 등록·조회와 **모든 상태 전이**(승인·반려·취소·배정·보류·재개·부분 종결). 역할은 `SecurityConfig`가, 담당 지점·창고 범위와 취소 권한은 이 서비스가 검사한다(ADR-012).

## 의존

`StoreOrderRepository`, `StoreOrderQueryRepository`, `StoreOrderOutboundPort`, `StoreAvailabilityPort`, `WarehouseAvailabilityPort`, `SkuSupplyPricePort`, `SkuAvailabilityPort`, `StatusHistoryUseCase`

## 공통 패턴

- **상태를 바꾸는 모든 메서드는 발주 행을 비관적 락(`findByIdForUpdate`)으로 잡고** 상태 확인과 변경을 한 트랜잭션에서 처리한다. 같은 발주에 대한 동시 처리 중 하나만 성공한다.
- 모든 전이는 상태 이력(`StatusHistory`)에 `from → to`, 사유, 처리자를 기록한다.
- 사유는 `normalizeReason`으로 앞뒤 공백을 제거하고, 공백뿐이면 null, **500자 초과면 400**이다.
- 처리자(`changedBy`)는 필수다. 토큰 사용자가 채운다.

## 메서드 한눈에

| 메서드 | 전이 | 허용 역할 | 사유 | 범위 검사 |
|---|---|---|---|---|
| `registerStoreOrder` | null → REQUESTED | STORE_OWNER | - | 요청 지점이 담당 지점 |
| `approveStoreOrder` | REQUESTED → APPROVED | HQ_ADMIN | 없음 | - |
| `rejectStoreOrder` | REQUESTED → REJECTED | HQ_ADMIN | **필수** | - |
| `cancelStoreOrder` | 진행 중 → CANCELED | HQ_ADMIN, STORE_OWNER | 승인 이후 **필수**, 승인 전 선택 | 지점 접근 + 작성자·상태별 권한 |
| `assignStoreOrder` | APPROVED → ASSIGNED, ASSIGNED 창고 변경 | HQ_ADMIN | 재배정 **필수** | - |
| `holdStoreOrder` | ASSIGNED → ON_HOLD | WAREHOUSE_MANAGER | **필수** | 배정 창고가 담당 창고 |
| `resumeStoreOrder` | ON_HOLD → ASSIGNED | WAREHOUSE_MANAGER | **필수** | 배정 창고가 담당 창고 |
| `completePartialStoreOrder` | ASSIGNED → COMPLETED | WAREHOUSE_MANAGER | **필수** | 배정 창고가 담당 창고 |

## 등록 (registerStoreOrder)

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 입력 검증: 지점·작성자 필수, 항목 1개 이상, SKU 필수·수량 ≥ 1·**SKU 중복 불가**, 요청 배송 일시는 현재 이후, 비고 1000자 이하 | 400 |
| 2 | 요청 지점이 담당 지점 | 403 |
| 3 | 지점 활성 | 404 / 409 |
| 4 | 항목마다 **SKU 공급 단가 스냅샷** | 비활성 SKU 409, 단가 없음 `SUPPLY_PRICE_MISSING` |
| 5 | 주문 번호 채번 후 헤더·항목 저장, 상태 이력(`null → REQUESTED`) | |

공급 단가는 요청으로 받지 않는다. 등록 시점의 `current_supply_price`가 고정된다.

## 승인·반려

| | 검사 | 부수 효과 |
|---|---|---|
| 승인 | REQUESTED인지(409), **지점 활성**, 항목 행 잠금 후 **모든 항목 SKU 활성**(409) | 상태 이력 |
| 반려 | 사유 필수(400), REQUESTED인지(409) | **항목을 모두 CANCELED로**, 상태 이력(사유) |

## 취소 (cancelStoreOrder)

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 발주 행 락 조회 | 404 |
| 2 | **지점 접근 확인을 먼저** (다른 지점 사용자에게 상태가 드러나지 않게). 본사는 전체 지점 가능 | 403 |
| 3 | 종결 상태가 아님 | 409 |
| 4 | 권한: 승인 전(REQUESTED)은 **작성자 점주**, 승인 이후는 **본사** | 403 |
| 5 | 승인 이후면 사유 필수 | 400 |
| 6 | 피킹이 시작된 출고가 있음 | 409 `ORDER_IN_PICKING` |
| 7 | 승인 이후면 출고 연동 포트로 **READY 출고 취소·재고 할당 해제**를 같은 트랜잭션에서 처리 | |
| 8 | 취소, 항목을 모두 CANCELED, 상태 이력 | |

응답에는 해제한 할당 수와 취소한 READY 출고 수가 담긴다. 승인 전 취소는 정리할 할당·출고가 없어 `NONE`(0, 0)이다.

## 창고 배정·재배정 (assignStoreOrder)

한 API가 최초 배정과 재배정을 모두 처리한다.

| 현재 상태 | 동작 | 추가 검사 |
|---|---|---|
| APPROVED | `assign` → ASSIGNED | |
| ASSIGNED | `reassign` (창고 변경) | **사유 필수**, 현재와 **다른** 창고, **재고 할당·취소되지 않은 출고가 없어야** 함(`ORDER_IN_FULFILLMENT`) |
| 그 외(ON_HOLD 포함) | | 409 |

공통: 배정할 창고는 존재·**활성**이어야 한다. 재배정 이력의 사유에는 `창고 변경 {이전} → {이후}: {사유}`가 남고, 500자를 넘으면 사용자 사유 끝을 `…`로 줄인다.

## 보류·재개

| | 검사 |
|---|---|
| 보류 | 담당 창고 → ASSIGNED인지(409) → 피킹 시작된 출고 없음(`ORDER_IN_PICKING`). **READY 출고나 재고 할당이 있어도 보류할 수 있고** 재고·수량은 바뀌지 않는다 |
| 재개 | 담당 창고 → ON_HOLD인지(409) → 배정 창고가 **활성**인지(409). **재고 충분 여부는 검증하지 않는다.** 응답 `statusReason`은 null |

## 부분 출고 종결 (completePartialStoreOrder)

전량이 아니더라도 남은 수량을 포기하고 발주를 종결한다.

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 사유 필수 | 400 |
| 2 | 담당 창고, ASSIGNED인지 | 403 / 409 |
| 3 | 진행 중 출고(READY·PICKING·PICKED·SHIPPED)가 없음 | 409 `OUTBOUND_IN_PROGRESS` |
| 4 | 부족한 항목이 있음 (전량 출고면 거절) | 409 `NO_SHORTAGE` |
| 5 | **출고에 묶이지 않고 남은 ALLOCATED 재고 할당을 모두 해제** (재고 행·발주 항목 할당 수량 감소) | |
| 6 | COMPLETED로 전환, 상태 이력 | |

출고·항목 상태는 바꾸지 않고, 항목별 부족 수량만 계산해 돌려준다. **남은 수량은 자동 재발주하지 않는다.** 락 순서(할당 → 발주 항목)를 지키려고 항목은 잠그지 않고 읽는다(헤더 락과 "진행 중 출고 없음"으로 수량이 고정된다).

## 조회

| 메서드 | 규칙 |
|---|---|
| `getStoreOrders(condition)` | 시작·종료 일시 검증(400), 지점·창고 존재 확인(404), 검색 후 **출고 연동 포트로 발주별 최근 출고 상태를 한 번에 조회**해 진행 단계를 붙인다. actor가 없는 본사 전용 조회 |
| `getMyStoreOrders(condition, actor)` | 점주는 담당 지점, 창고 관리자는 배정 창고(**배정 전 발주 제외**). 지점·창고를 지정하면 담당 범위여야 한다(403). 본사는 이 조회를 쓸 수 없다(403) |
| `getStoreOrder(id, actor)` | 점주는 담당 지점, 창고 관리자는 배정 창고, 본사는 전체. **배정 전 발주는 창고 관리자에게 403**. `statusReason`은 REJECTED·CANCELED·ON_HOLD일 때만 이력에서 읽는다 |
| `getStoreOrderDetails(id, actor)` | 항목, 출고 목록, 상태 이력(처리자명 포함)을 한 번에 |

## 관찰

- `StoreOrderCancelResult`, `StoreOrderDetails`, `StoreOrderFulfillmentCancelResult.NONE`의 주석에는 "출고 도메인 연동 전에는 …"이라고 적혀 있다. 지금은 `outbound` 도메인의 `StoreOrderOutboundAdapter`가 연동돼 있어, **주석이 구현보다 오래됐다.** 코드는 수정하지 않았다.
