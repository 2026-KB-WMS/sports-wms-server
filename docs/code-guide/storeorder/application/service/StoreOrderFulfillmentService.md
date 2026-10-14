# StoreOrderFulfillmentService

`storeorder/application/service/StoreOrderFulfillmentService.java` · 구현: `StoreOrderFulfillmentUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

**출고 도메인이 지점 발주를 읽고 항목 수량·발주 상태를 바꿀 때 쓰는 서비스** (출고 → 지점 발주 방향). HTTP 엔드포인트는 없다.

## 왜 `StoreOrderService`와 분리했나

지점 발주 서비스가 출고 연동 포트(`StoreOrderOutboundPort`)를 거쳐 출고 어댑터를 부른다. 같은 서비스가 출고 도메인에서도 호출되면 **빈 순환 의존**이 생기므로 분리했다.

## 트랜잭션

쓰기·락 메서드는 모두 `Propagation.MANDATORY`다. 호출한 **출고 서비스의 트랜잭션에 반드시 참여**해야 하며, 없으면 예외다. 상태 확인과 변경은 호출 쪽이 발주·항목을 잠근 상태에서 한다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `getOrder(id)` | 발주 헤더 조회. 없으면 `STORE_ORDER_NOT_FOUND` |
| `getOrderForUpdate(id)` | 비관적 락 조회 (MANDATORY) |
| `getLines(id)` | 항목 조회 (ID 오름차순) |
| `getLinesForUpdate(id)` | 항목 락 조회 (MANDATORY) |
| `increaseAllocated(commands)` | 재고 할당에 따라 항목 할당 수량 증가. 항목을 ID 오름차순으로 잠그고 `increaseAllocated` 후 저장 |
| `decreaseAllocated(commands)` | 할당 해제·취소에 따라 할당 수량 감소 |
| `applyPicked(commands)` | 피킹 완료 반영: 할당 수량 감소 + 출고 수량 증가 |
| `refreshLineStatuses(ids)` | 배송 완료 후 항목 상태(PARTIALLY_SHIPPED·COMPLETED)를 출고 수량 기준으로 재계산 |
| `completeIfFulfilled(id, changedBy)` | **모든 항목이 전량 출고**됐고 발주가 ASSIGNED면 COMPLETED로 전환하고 상태 이력을 남긴다(시스템 자동 전이, 처리자는 요청자). 조건이 안 맞으면 바꾸지 않고 현재 발주를 돌려준다 |

## lockLines

항목 ID를 중복 제거·정렬해 비관적 락으로 조회한다. **없는 항목이 있으면 404가 아니라 `IllegalArgumentException`**(호출 오류)이다.

## 호출자

`completeIfFulfilled`는 호출 쪽이 **진행 중 출고가 없을 때만** 부른다. 호출 서비스: `OutboundService`, `OutboundFulfillmentService`, `StockAllocationService`.
