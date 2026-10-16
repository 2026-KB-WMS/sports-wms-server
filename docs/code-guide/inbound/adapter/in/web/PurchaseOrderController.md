# PurchaseOrderController

`inbound/adapter/in/web/PurchaseOrderController.java` · `@RestController` · `/api/v1/purchase-orders` · Swagger 태그 "창고 발주"

> `storeorder` 도메인의 지점 발주(`/api/v1/orders`)와 다르다. 이 컨트롤러는 **창고 → 공급처** 발주다.

## 의존

`PurchaseOrderUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerPurchaseOrder` | POST `/` | 201 | `PurchaseOrderRegisterRequest` | `PurchaseOrderRegisterResponse` | 발주 등록 | WAREHOUSE_MANAGER |
| `getPurchaseOrders` | GET `/` | 200 | `status`, `warehouseId`, `supplierId`, `keyword`, `createdFrom`, `createdTo`, `page`, `size`, `sort` | `PageResponse<PurchaseOrderSummaryResponse>` | 목록(정렬 필드: `createdAt`, `expectedAt`, `status`, `purchaseOrderNo`, `warehouseName`) | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getPurchaseOrder` | GET `/{id}` | 200 | - | `PurchaseOrderResponse` | 헤더 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getPurchaseOrderDetails` | GET `/{id}/details` | 200 | - | `PurchaseOrderDetailsResponse` | SKU별 항목 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `rejectPurchaseOrder` | PATCH `/{id}/reject` | 200 | `PurchaseOrderRejectRequest`(`reason` 필수) | `PurchaseOrderRejectResponse` | 반려 | **HQ_ADMIN** |
| `confirmPurchaseOrder` | PATCH `/{id}/confirm` | 200 | - | `PurchaseOrderConfirmResponse` | 확정(선택 바디 `lines[]`로 라인별 공급처 지정, 공급처 발주서 생성) | **HQ_ADMIN** |
| `cancelPurchaseOrder` | PATCH `/{id}/cancel` | 200 | `PurchaseOrderCancelRequest`(선택) | `PurchaseOrderStatusResponse` | 취소 | HQ_ADMIN, WAREHOUSE_MANAGER |

## 응답 조립

- **등록**: ID를 받아 `getPurchaseOrder` + `getPurchaseOrderDetails`를 다시 조회해 헤더와 항목을 합친다.
- **취소**: 요청 바디가 `required = false`라서 없으면 `reason=null`인 요청을 만든다. 취소 후 `getPurchaseOrder`로 취소 사유를 읽어 응답에 담는다.

## 취소 권한 (서비스 판단)

`SecurityConfig`는 두 역할에 열어 두고, 서비스가 상태별로 막는다. REQUESTED는 **작성자 창고 관리자**, CONFIRMED는 **본사 관리자**만 가능하다. 그 외는 403.
