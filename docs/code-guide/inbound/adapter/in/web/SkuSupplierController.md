# SkuSupplierController

`inbound/adapter/in/web/SkuSupplierController.java` · `@RestController` · `/api/v1/products/skus/{skuId}/suppliers` · Swagger 태그 "SKU 공급처 매핑"

## 의존

`SkuSupplierUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 바디 | 응답 | 허용 역할 |
|---|---|---|---|---|---|
| `getSuppliers` | GET `/` | 200 | - | `ItemsResponse<SkuSupplierResponse>` | HQ_ADMIN, WAREHOUSE_MANAGER |
| `registerSupplier` | POST `/` | 201 | `SkuSupplierRegisterRequest`(`supplierId`) | `SkuSupplierResponse` | HQ_ADMIN |
| `removeSupplier` | DELETE `/{supplierId}` | 200 | - | `SkuSupplierRemoveResponse`(skuId, supplierId) | HQ_ADMIN |

경로가 `/products/**` 아래라 SecurityConfig에서 상품 읽기 허용 규칙보다 먼저 매칭하도록 앞에 둔다(점주에게 공급처 정보를 열지 않는다).
