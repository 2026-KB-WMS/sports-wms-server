# 인바운드 포트 (UseCase)

`storeorder/application/port/in/`

| UseCase | 구현 | 호출자 | 용도 |
|---|---|---|---|
| `StoreOrderUseCase` | `StoreOrderService` | `StoreOrderController` | 사용자 요청: 등록·조회·승인·반려·취소·배정·보류·재개·부분 종결 |
| `StoreOrderFulfillmentUseCase` | `StoreOrderFulfillmentService` | **출고 도메인** (`OutboundService`, `OutboundFulfillmentService`, `StockAllocationService`) | 출고 → 지점 발주 방향. 항목 수량·발주 상태 변경 |
| `StoreOrderPresenceUseCase` | `StoreOrderPresenceService` | **지점 도메인** (`StoreOrderPresenceAdapter`) | 진행 중인 발주가 있는지 읽기 전용 확인 |

## 왜 세 개로 나눴나

지점 ↔ 발주 ↔ 출고 도메인이 서로를 호출하므로, 한 서비스에 모으면 **빈 순환 의존**이 생긴다.

```
지점(store) ─ hasInProgressOrders ─▶ StoreOrderPresenceUseCase ──┐
출고(outbound) ─ 수량·상태 변경 ───▶ StoreOrderFulfillmentUseCase ┼─▶ 지점 발주(storeorder)
                                     StoreOrderUseCase ◀── 컨트롤러 │
지점 발주 ─ StoreOrderOutboundPort ─▶ OutboundOrderLinkUseCase(outbound)
```

## StoreOrderUseCase 메서드

| 메서드 | 시그니처 특징 |
|---|---|
| `registerStoreOrder(command, actor)` | 발주 **ID만** 반환. 응답은 컨트롤러가 조회 UseCase로 조립 |
| `getStoreOrders(condition)` | actor 없음 (본사 전용) |
| `getMyStoreOrders(condition, actor)` | 점주·창고 관리자용 |
| `getStoreOrder(id, actor)`, `getStoreOrderDetails(id, actor)` | 담당 범위 검사 |
| `approveStoreOrder(id, changedBy)` | **`Long changedBy`만** 받음 (본사 전용) |
| `rejectStoreOrder(command)` | actor 없음 (본사 전용) |
| `cancelStoreOrder(command, actor)` | 작성자·상태별 권한 검사에 actor 필요 |
| `assignStoreOrder(command)` | actor 없음 (본사 전용) |
| `holdStoreOrder`, `resumeStoreOrder`, `completePartialStoreOrder` (`command, actor`) | 담당 창고 검사에 actor 필요 |
