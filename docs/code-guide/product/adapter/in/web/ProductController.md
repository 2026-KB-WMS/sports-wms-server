# ProductController

`product/adapter/in/web/ProductController.java` · `@RestController` · `/api/v1/products` · Swagger 태그 "상품"

## 의존

`ProductUseCase`, `BrandQueryUseCase`, `CategoryUseCase` (응답에 브랜드명·카테고리명을 채우기 위해)

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `registerProduct` | POST `/` | 201 | `ProductRegisterRequest` | `ProductDetailResponse` | 상품 등록 (코드는 서버 발급) |
| `getProducts` | GET `/` | 200 | 쿼리 `brandId`, `categoryId`, `keyword`, `isActive` | `ItemsResponse<ProductSummaryResponse>` | 목록 조회. `@AuthenticationPrincipal`로 점주 여부 전달 |
| `getProduct` | GET `/{productId}` | 200 | - | `ProductDetailResponse` | 단건 조회. 점주는 비활성이면 404 |
| `updateProduct` | PATCH `/{productId}` | 200 | `ProductUpdateRequest` | `ProductDetailResponse` | 브랜드·카테고리·이름·설명·활성 상태·스펙 수정 |

## private 헬퍼

| 메서드 | 설명 |
|---|---|
| `toSummaryResponse` | 상품 + 브랜드명 + 카테고리명 |
| `toDetailResponse` | 상품 + 브랜드명 + 카테고리명 + 스펙 목록 |
| `brandName`, `categoryName` | ID로 각각 조회해 이름만 반환 |

## 주의

- 목록에서 상품마다 브랜드·카테고리를 개별 조회하므로 상품 수만큼 쿼리가 나간다(N+1 가능성).
