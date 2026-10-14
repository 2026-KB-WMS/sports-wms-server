# 응답 DTO (Response)

`storeorder/adapter/in/web/dto/response/` · 응답 `record`. 목록은 `ItemsResponse`(`data.items`).

## 조회

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `StoreOrderListItemResponse` | 목록 항목 | GET `/orders`, `/orders/my` | `from(StoreOrderListItem)` | storeOrderId, orderNo, 지점(id·이름), 창고(id·이름, 배정 전 null), status, **progressStage**, requestedAt, requestedDeliveryAt, lineCount, totalAmount, **latestOutboundStatus** |
| `StoreOrderResponse` | 헤더 단건 | GET `/orders/{id}` | `from(StoreOrderDetail)` | 목록 필드 + statusReason, note, createdBy·Name, createdAt, updatedAt |
| `StoreOrderDetailsResponse` | 상세 | GET `/orders/{id}/details` | `from(StoreOrderDetails)` | storeOrderId, orderNo, status, progressStage, `items`, `outbounds`, `statusHistory` |
| `StoreOrderLineResponse` | 항목 한 행 (등록 응답의 `lines`와 상세의 `items` 공용) | | `from(StoreOrderLineView)` | storeOrderLineId, SKU(id·코드·이름·단위), requested·allocated·shipped·remainingQuantity, requestedUnitSupplyPrice, lineAmount, status |

`StoreOrderDetailsResponse`의 내부 record:

| record | 필드 |
|---|---|
| `Outbound` | outboundId, outboundNo, status, shippedAt, deliveredAt (출고가 없으면 빈 배열) |
| `History` | fromStatus, toStatus, reason, changedBy, changedByName(처리자가 없으면 null), changedAt |

## 등록·상태 변경

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `StoreOrderRegisterResponse` | 등록된 발주 헤더 + 항목 | POST `/orders` | `of(view, lines)` | storeOrderId, orderNo, 지점(id·이름), warehouseId, status, requestedAt, requestedDeliveryAt, note, totalAmount, createdBy·Name, `lines`, createdAt |
| `StoreOrderApproveResponse` | 승인 결과 | PATCH `.../approve` | `from(StoreOrderStatusChange)` | storeOrderId, orderNo, status, updatedAt (**사유 없음**) |
| `StoreOrderStatusResponse` | 반려·보류·재개 결과 | PATCH `.../reject`, `hold`, `resume` | `from(StoreOrderStatusChange)` | storeOrderId, orderNo, status, statusReason, updatedAt. 재개는 `statusReason`이 null |
| `StoreOrderCancelResponse` | 취소 결과 | PATCH `.../cancel` | `from(StoreOrderCancelResult)` | + statusReason, **releasedAllocationCount**, **canceledOutboundCount** |
| `StoreOrderAssignResponse` | 배정 결과 | POST `/orders/assign` | `from(StoreOrderAssignResult)` | storeOrderId, orderNo, status, warehouseId, warehouseName, updatedAt |
| `StoreOrderCompletePartialResponse` | 부분 종결 결과 | PATCH `.../complete-partial` | `from(StoreOrderCompletePartialResult)` | storeOrderId, orderNo, status, statusReason, `items`(storeOrderLineId, skuCode, requested·shipped·shortageQuantity), releasedAllocationCount, updatedAt |

## 참고

- `progressStage`는 점주 화면용 **파생 값**이다(`StoreOrderProgressStage.resolve`). DB에 없다.
- 승인 전(REQUESTED) 취소는 정리할 할당·출고가 없어 두 건수가 0이다.
