# 인바운드 포트 (UseCase)

`outbound/application/port/in/`

출고 도메인은 UseCase를 **세 개**로 나눠 서비스 A·B·C가 각각 구현한다.

| UseCase | 구현 | 호출자 | 메서드 |
|---|---|---|---|
| `StockAllocationUseCase` | `StockAllocationService` (A) | `AllocationController` | `allocate`, `release`, `searchAllocations`, `getAllocation` |
| `OutboundUseCase` | `OutboundService` (B) | `OutboundController` | `createOutbound`, `startPicking`, `cancel`, `searchOutbounds`, `getOutbound` |
| `OutboundFulfillmentUseCase` | `OutboundFulfillmentService` (C) | `OutboundController` | `completePicking`, `ship`, `deliver` |

## 업무 순서와 UseCase

```
(발주 ASSIGNED)
  ① 재고 할당           StockAllocationUseCase.allocate     ALLOCATED
  ② 출고 생성(묶기)     OutboundUseCase.createOutbound      READY
  ③ 피킹 시작           OutboundUseCase.startPicking        PICKING
  ④ 피킹 완료(재고 차감) OutboundFulfillmentUseCase.completePicking   PICKED
  ⑤ 배송 시작           OutboundFulfillmentUseCase.ship     SHIPPED
  ⑥ 배송 완료           OutboundFulfillmentUseCase.deliver  DELIVERED → 발주 COMPLETED(전량)
  
  취소: READY에서만 OutboundUseCase.cancel / 해제: ALLOCATED 할당을 StockAllocationUseCase.release
```

## 시그니처 특징

- 모든 메서드가 `AuthenticatedUser actor`를 받아 **발주에 배정된 창고** 기준으로 담당 창고를 검사한다.
- 상태를 바꾸는 흐름은 응답을 위해 처리 후 **뷰를 다시 조회**한다(`updatedAt` 확정 등).
- 호출자는 컨트롤러뿐이다. 발주 도메인이 출고 쪽을 부를 때는 이 UseCase가 아니라 발주의 `StoreOrderOutboundPort`를 통해 출고의 `StoreOrderOutboundAdapter`가 처리한다.
