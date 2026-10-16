# SupplierOrderController

`inbound/adapter/in/web/SupplierOrderController.java` · `@RestController` · `/api/v1/supplier-orders` · Swagger 태그 "공급처 발주서"

## 의존

`SupplierOrderUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리 | 응답 | 허용 역할 |
|---|---|---|---|---|---|
| `getSupplierOrders` | GET `/` | 200 | `status`, `warehouseId`, `supplierId`, `purchaseOrderId`, `keyword`, `createdFrom`, `createdTo`, `page`, `size`, `sort` | `PageResponse<SupplierOrderSummaryResponse>`(정렬 필드: `createdAt`, `status`, `supplierOrderNo`, `supplierName`, `warehouseName`, `lineCount`, `totalAmount`) | HQ_ADMIN, WAREHOUSE_MANAGER(담당) |
| `getSupplierOrder` | GET `/{supplierOrderId}` | 200 | - | `SupplierOrderResponse`(헤더 + `lines[]`) | HQ_ADMIN, WAREHOUSE_MANAGER(담당) |

쓰기 API는 없다. 담당 창고 범위는 서비스가 검사한다.
