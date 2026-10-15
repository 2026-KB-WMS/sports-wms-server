# 인바운드 포트 (UseCase)

`inbound/application/port/in/`

| UseCase | 구현 | 메서드 | 호출자 |
|---|---|---|---|
| `InboundUseCase` | `InboundService` | `registerInbound`, `getInbounds`, `getInbound`, `getInboundDetails`, `cancelInbound`, `getAssignableSections`, `getDefectSections` | `InboundController` |
| `InboundInspectUseCase` | `InboundInspectService` | `inspectInbound` | `InboundController` |
| `InboundCompleteUseCase` | `InboundCompleteService` | `completeInbound` | `InboundController` |
| `PurchaseOrderUseCase` | `PurchaseOrderService` | `registerPurchaseOrder`, `getPurchaseOrders`, `getPurchaseOrder`, `getPurchaseOrderDetails`, `confirmPurchaseOrder`, `rejectPurchaseOrder`, `cancelPurchaseOrder` | `PurchaseOrderController` |
| `SupplierUseCase` | `SupplierService` | `registerSupplier`, `getSuppliers`, `getSupplier`, `updateSupplier`, `deactivateSupplier`, `activateSupplier` | `SupplierController` |

## 설계 포인트

- **입고를 세 UseCase로 쪼갰다**: 등록·조회·취소(`InboundUseCase`), 검수(`InboundInspectUseCase`), 완료(`InboundCompleteUseCase`). 검수와 완료는 잠금 순서·트랜잭션 성격이 달라서 서비스를 분리했다.
- **등록은 ID만 반환한다**: `registerInbound`, `registerPurchaseOrder`는 ID를 돌려주고, 응답 DTO는 컨트롤러가 조회 UseCase로 다시 조립한다.
- 모든 쓰기·조회 메서드가 `AuthenticatedUser actor`를 받아 **담당 창고 범위**를 서비스에서 검사한다. 예외: `confirmPurchaseOrder`(본사 전용이라 커맨드의 `userId`만), 공급처의 등록·수정·활성화(본사 전용이라 actor 없음).
