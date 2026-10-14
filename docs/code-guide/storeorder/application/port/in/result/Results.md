# 조회 결과 (Result)

`storeorder/application/port/in/result/`

## 조회 쿼리가 만드는 읽기 전용 뷰

| 클래스 | 용도 | 엔드포인트 | 필드·설명 |
|---|---|---|---|
| `StoreOrderSummary` | 목록 한 행 | GET `/orders`, `/orders/my` | 발주·지점명·창고명·상태·요청 일시·`lineCount`·`totalAmount`(요청 수량 × 공급 단가 합계)·`shortageLineCount`. **창고는 배정 전 null**. `hasShortage()` 제공 |
| `StoreOrderView` | 헤더 단건 | GET `/orders/{id}`, 등록 응답 | Summary 필드 + note, createdBy·createdByName(작성자명), createdAt, updatedAt |
| `StoreOrderLineView` | 항목 한 행 | details의 `items`, 등록 응답의 `lines` | SKU(id·코드·이름·단위), 요청·할당·출고 수량, 공급 단가, 상태. `lineAmount()`(요청 × 단가), `remainingQuantity()`(`max(0, 요청 - 출고)`) |

`shortageLineCount`는 `shippedQuantity < requestedQuantity`인 항목 수다.

## 서비스가 조립하는 결과

| 클래스 | 용도 | 엔드포인트 | 구성 |
|---|---|---|---|
| `StoreOrderListItem` | 목록 항목 | GET `/orders`, `/orders/my` | `summary` + `latestOutboundStatus`(출고 연동 포트로 채움, 없으면 null) + `progressStage` |
| `StoreOrderDetail` | 헤더 + 사유·진행 단계 | GET `/orders/{id}` | `view` + `statusReason`(REJECTED·CANCELED·ON_HOLD일 때만 상태 이력에서 읽음) + `progressStage` |
| `StoreOrderDetails` | 상세 | GET `/orders/{id}/details` | 식별 정보, `items`, `outbounds`, `statusHistory`, `progressStage` |
| `StoreOrderOutboundView` | 발주에 딸린 출고 한 건 | details의 `outbounds` | outboundId, outboundNo, status, shippedAt, deliveredAt |
| `StoreOrderStatusHistoryView` | 상태 이력 한 건 | details의 `statusHistory` | fromStatus(최초 등록이면 null), toStatus, reason, changedBy, changedByName(처리자가 없으면 null), changedAt |

## 상태 변경 결과

| 클래스 | 용도 | 엔드포인트 | 필드 |
|---|---|---|---|
| `StoreOrderStatusChange` | 승인·반려·보류·재개 결과 | PATCH `.../approve`, `reject`, `hold`, `resume` | storeOrderId, orderNo, status, statusReason(승인·재개는 null), updatedAt |
| `StoreOrderCancelResult` | 취소 결과 | PATCH `.../cancel` | + `releasedAllocationCount`(해제한 재고 할당 수), `canceledOutboundCount`(취소한 READY 출고 수) |
| `StoreOrderAssignResult` | 배정 결과 | POST `/orders/assign` | + warehouseId, warehouseName |
| `StoreOrderCompletePartialResult` | 부분 종결 결과 | PATCH `.../complete-partial` | items(`Item`: storeOrderLineId, skuCode, requestedQuantity, shippedQuantity, shortageQuantity — SKU 코드 순), `releasedAllocationCount`(해제한 남은 ALLOCATED 할당 건수) |
| `StoreOrderFulfillmentCancelResult` | 출고 쪽에서 정리한 건수 | (내부) | releasedAllocationCount, canceledOutboundCount. 정리할 게 없으면 `NONE`(0, 0) |

## updatedAt 보정

저장 결과의 `updatedAt`은 flush 전이라 비어 있을 수 있어, 서비스가 조회 쿼리(JPQL 실행 전 flush)로 확정된 값을 다시 읽는다(`updatedAtOf`).
