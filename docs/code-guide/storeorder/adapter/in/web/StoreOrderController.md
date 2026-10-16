# StoreOrderController

`storeorder/adapter/in/web/StoreOrderController.java` · `@RestController` · `/api/v1/orders` · Swagger 태그 "지점 발주"

처리 사용자는 토큰 주체다. 역할은 `SecurityConfig`가, 담당 지점·창고 범위와 취소 권한은 서비스가 검사한다. 목록은 페이지네이션·정렬(`page`, `size`, `sort`)을 지원한다.

## 의존

`StoreOrderUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerStoreOrder` | POST `/` | 201 | `StoreOrderRegisterRequest` | `StoreOrderRegisterResponse` | 발주 등록 | STORE_OWNER |
| `getStoreOrders` | GET `/` | 200 | `status`, `storeId`, `warehouseId`, `keyword`, `requestedFrom`, `requestedTo`, `page`, `size`, `sort` | `PageResponse<StoreOrderListItemResponse>` | 전체 목록(정렬 필드: `requestedAt`, `requestedDeliveryAt`, `status`, `orderNo`, `storeName`, `warehouseName`, `lineCount`, `totalAmount`) | HQ_ADMIN |
| `getMyStoreOrders` | GET `/my` | 200 | 위와 같음 | `PageResponse<StoreOrderListItemResponse>` | 내 발주 (점주: 담당 지점, 창고 관리자: 배정 창고) | STORE_OWNER, WAREHOUSE_MANAGER |
| `getStoreOrder` | GET `/{orderId}` | 200 | - | `StoreOrderResponse` | 헤더 단건 | HQ_ADMIN, STORE_OWNER, WAREHOUSE_MANAGER |
| `getStoreOrderDetails` | GET `/{orderId}/details` | 200 | - | `StoreOrderDetailsResponse` | 항목·출고·상태 이력 | 위와 같음 |
| `approveStoreOrder` | PATCH `/{orderId}/approve` | 200 | - | `StoreOrderApproveResponse` | 승인 | HQ_ADMIN |
| `rejectStoreOrder` | PATCH `/{orderId}/reject` | 200 | `StoreOrderReasonRequest` | `StoreOrderStatusResponse` | 반려 | HQ_ADMIN |
| `cancelStoreOrder` | PATCH `/{orderId}/cancel` | 200 | `StoreOrderCancelRequest`(선택) | `StoreOrderCancelResponse` | 취소 | HQ_ADMIN, STORE_OWNER |
| `assignStoreOrder` | POST `/assign` | 200 | `StoreOrderAssignRequest` | `StoreOrderAssignResponse` | 창고 배정·재배정 | HQ_ADMIN |
| `holdStoreOrder` | PATCH `/{orderId}/hold` | 200 | `StoreOrderReasonRequest` | `StoreOrderStatusResponse` | 출고 보류 | WAREHOUSE_MANAGER |
| `resumeStoreOrder` | PATCH `/{orderId}/resume` | 200 | `StoreOrderReasonRequest` | `StoreOrderStatusResponse` | 출고 재개 | WAREHOUSE_MANAGER |
| `completePartialStoreOrder` | PATCH `/{orderId}/complete-partial` | 200 | `StoreOrderReasonRequest` | `StoreOrderCompletePartialResponse` | 부분 출고 종결 | WAREHOUSE_MANAGER |

`/assign`은 **POST**이고 경로에 발주 ID가 없다(ID는 바디의 `storeOrderId`).

## 응답 조립

- **등록**: ID를 받아 `getStoreOrder(...).view()`와 `getStoreOrderDetails(...).items()`를 다시 조회해 헤더와 항목을 합친다.
- **취소**: 요청 바디가 `required = false`라서 없으면 `reason=null`.
- reject·hold·resume·complete-partial은 경로 ID, 바디의 `reason`, 토큰 사용자를 컨트롤러가 합쳐 Command를 직접 만든다.

## 경로 주의

`/my`는 `/{orderId}`보다 구체적인 리터럴 경로라서 먼저 매칭된다.
