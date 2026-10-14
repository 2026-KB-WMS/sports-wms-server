# StoreOrderPresenceService

`storeorder/application/service/StoreOrderPresenceService.java` · 구현: `StoreOrderPresenceUseCase` · `@Transactional(readOnly = true)`

다른 도메인이 "이 지점에 진행 중인 발주가 있는가"만 확인하는 **읽기 전용** 서비스. `StoreOrderService`와 분리해 지점 도메인과 발주 도메인 사이의 순환 의존을 막는다.

## 상수

`IN_PROGRESS_STATUSES = {REQUESTED, APPROVED, ASSIGNED, ON_HOLD}`. COMPLETED·CANCELED·REJECTED는 지점 비활성화를 막지 않는다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `hasInProgressOrders(storeId)` | 위 상태의 발주가 하나라도 있으면 true |

## 호출자

지점 도메인의 `StoreOrderPresenceAdapter` → `StoreService.deactivateStore`(`STORE_IN_USE` 판단).
