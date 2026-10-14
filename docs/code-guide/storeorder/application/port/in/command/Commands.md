# Command

`storeorder/application/port/in/command/` · 처리자는 요청 바디가 아니라 **토큰 사용자**로, `changedBy`(또는 `createdBy`)에 채워진다.

## 사용자 요청에서 오는 Command

| Command | 엔드포인트 | 만드는 곳 | 쓰는 곳 | 필드 | 사유 |
|---|---|---|---|---|---|
| `StoreOrderRegisterCommand` | POST `/orders` | `StoreOrderRegisterRequest.toCommand(userId)` | `registerStoreOrder` | storeId, requestedDeliveryAt(선택, 현재 이후), note(선택), createdBy, lines(`Line`: skuId, requestedQuantity ≥ 1) | - |
| `StoreOrderRejectCommand` | PATCH `/orders/{id}/reject` | 컨트롤러가 직접 생성 | `rejectStoreOrder` | storeOrderId, reason, changedBy | 필수, 500자 이하 |
| `StoreOrderCancelCommand` | PATCH `/orders/{id}/cancel` | 컨트롤러가 직접 생성 | `cancelStoreOrder` | storeOrderId, reason, changedBy | 승인 이후 필수, 승인 전 점주 취소는 선택 |
| `StoreOrderAssignCommand` | POST `/orders/assign` | `StoreOrderAssignRequest.toCommand(userId)` | `assignStoreOrder` | storeOrderId, warehouseId(활성 창고), reason, changedBy | **재배정(ASSIGNED의 창고 변경)에서 필수** |
| `StoreOrderHoldCommand` | PATCH `/orders/{id}/hold` | 컨트롤러가 직접 생성 | `holdStoreOrder` | storeOrderId, reason, changedBy | 필수 |
| `StoreOrderResumeCommand` | PATCH `/orders/{id}/resume` | 컨트롤러가 직접 생성 | `resumeStoreOrder` | storeOrderId, reason, changedBy | 필수 |
| `StoreOrderCompletePartialCommand` | PATCH `/orders/{id}/complete-partial` | 컨트롤러가 직접 생성 | `completePartialStoreOrder` | storeOrderId, reason, changedBy | 필수 (재고 불가·공급 중단 등) |

승인(`approve`)은 사유가 없어 Command 없이 `approveStoreOrder(orderId, userId)`로 값을 직접 넘긴다.

reject·cancel·hold·resume·complete-partial은 경로 ID와 바디의 `reason`, 토큰 사용자를 컨트롤러가 합쳐 Command를 만든다(요청 DTO에 `toCommand`가 없다).

## 출고 도메인이 넘기는 Command

`StoreOrderFulfillmentUseCase`가 받는다. 사용자 요청과 무관한 내부 호출이다.

| Command | 쓰는 곳 | 필드 | 의미 |
|---|---|---|---|
| `StoreOrderLineQuantityCommand` | `increaseAllocated`, `decreaseAllocated` | storeOrderLineId, quantity(1 이상) | 재고 할당(증가)·할당 해제(감소) |
| `StoreOrderLinePickedCommand` | `applyPicked` | storeOrderLineId, allocatedQuantity(줄일 할당 수량), pickedQuantity(0 이상 할당 이하, 늘릴 출고 수량) | 피킹 완료 반영 |
