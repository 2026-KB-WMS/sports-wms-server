# SupplierController

`inbound/adapter/in/web/SupplierController.java` · `@RestController` · `/api/v1/suppliers` · Swagger 태그 "공급처"

## 의존

`SupplierUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerSupplier` | POST `/` | 201 | `SupplierRegisterRequest` | `SupplierResponse` | 등록 | HQ_ADMIN |
| `getSuppliers` | GET `/` | 200 | `keyword`, `isActive` | `ItemsResponse<SupplierSummaryResponse>` | 목록 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getSupplier` | GET `/{supplierId}` | 200 | - | `SupplierResponse` | 단건 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `updateSupplier` | PATCH `/{supplierId}` | 200 | `SupplierUpdateRequest` | `SupplierResponse` | 부분 수정 | HQ_ADMIN |
| `deactivateSupplier` | PATCH `/{supplierId}/deactivate` | 200 | - | `SupplierDeactivateResponse` | 비활성화 | HQ_ADMIN |
| `activateSupplier` | PATCH `/{supplierId}/activate` | 200 | - | `SupplierResponse` | 활성화 | HQ_ADMIN |

창고 관리자에게는 활성 공급처만 보이는 규칙(목록은 `isActive=true` 고정, 비활성 단건은 404)을 **서비스가** 처리한다. 컨트롤러는 `principal`만 넘긴다.

## 응답 차이

비활성화 응답만 `SupplierDeactivateResponse`(supplierId, supplierCode, supplierName, isActive, updatedAt)이고, 활성화·수정·등록·단건은 `SupplierResponse`다.
