# ProductSkuController

`product/adapter/in/web/ProductSkuController.java` · `@RestController` · `/api/v1/products/skus` · Swagger 태그 "SKU"

## 의존

`ProductSkuUseCase`, `ProductUseCase`, `BrandQueryUseCase`, `CategoryUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `registerSku` | POST `/` | 201 | `ProductSkuRegisterRequest` | `ProductSkuCreateResponse` | SKU 등록. 응답에 옵션 요약 포함 |
| `getSkus` | GET `/` | 200 | 쿼리 `productId`, `brandId`, `categoryId`, `keyword`, `isActive` | `ItemsResponse<ProductSkuListItemResponse>` | 목록 조회 |
| `getSku` | GET `/{skuId}` | 200 | - | `ProductSkuDetailResponse` | 상세 조회 (상품·브랜드·카테고리 정보와 옵션 포함) |
| `updateSku` | PATCH `/{skuId}` | 200 | `ProductSkuUpdateRequest` | `ProductSkuUpdateResponse` | 이름·바코드·무게·단가·안전 재고 부분 수정 |
| `changeSkuStatus` | PATCH `/{skuId}/status` | 200 | `ProductSkuStatusRequest` | `ProductSkuStatusResponse` | 활성/비활성 전환 |
| `connectOptions` | POST `/{skuId}/options` | 201 | `SkuOptionConnectRequest` | `SkuOptionsConnectResponse` | 옵션 없는 기존 SKU에 옵션 연결 |

## 구현 포인트

- **점주 마스킹**: 목록·상세에서 `sku.isPurchaseInfoVisibleTo(principal.isStoreOwner())` 값을 DTO 변환에 넘겨 매입 단가·안전 재고를 숨긴다.
- **N+1 회피**: 목록의 옵션은 `getSkuOptionsBySkuIds`로 한 번에 조회하고, 상품명은 `productNames` 맵으로 상품당 한 번만 조회한다.
- 상세 응답은 `ProductSkuDetailResponse.of(...)`에 상품 코드·상품명·브랜드·카테고리·옵션을 모두 넘겨 조립한다.
