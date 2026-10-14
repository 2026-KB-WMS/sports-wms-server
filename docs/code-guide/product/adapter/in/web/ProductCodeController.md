# ProductCodeController

`product/adapter/in/web/ProductCodeController.java` · `@RestController` · `/api/v1/products/skus` · Swagger 태그 "상품 코드"

## 의존

`ProductCodeUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 응답 | 설명 |
|---|---|---|---|---|
| `getSkuUnits` | GET `/api/v1/products/skus/units` | 200 | `ItemsResponse<CodeItemResponse>` | SKU 수량 단위 허용 목록(EA·BOX·SET) |

경로가 `/skus/{skuId}`와 겹쳐 보이지만 `units`는 리터럴 경로라서 먼저 매칭된다.
