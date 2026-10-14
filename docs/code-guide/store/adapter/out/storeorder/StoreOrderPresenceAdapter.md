# StoreOrderPresenceAdapter

`store/adapter/out/storeorder/StoreOrderPresenceAdapter.java` · `@Component` · 구현: `StoreOrderPresencePort`

지점 도메인의 "진행 중인 발주가 있는가" 질문을 **지점 발주(storeorder) 도메인의 조회 유스케이스로 연결**한다. 지점 도메인이 발주 도메인의 내부를 알지 않도록 포트 뒤에 둔다.

## 의존

`StoreOrderPresenceUseCase`(storeorder 도메인)

## 메서드

| 메서드 | 설명 |
|---|---|
| `hasInProgressOrders(storeId)` | `storeOrderPresenceUseCase.hasInProgressOrders` 위임 |

## 사용처

`StoreService.deactivateStore`: 진행 중인 발주가 있으면 `STORE_IN_USE`로 비활성화를 막는다.
