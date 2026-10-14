# ProductService

`product/application/service/ProductService.java` · 구현: `ProductUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

상품 등록·수정·조회와 스펙 조회.

## 의존

`ProductRepository`, `BrandRepository`, `CategoryRepository`, `ProductSkuRepository`, `ProductUsagePort`, `ProductSpecRepository`, `CategoryOptionResolver`, `OptionSelectionResolver`, `ProductCodeIssuer`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerProduct(command)` | 쓰기 | 브랜드·카테고리 존재와 활성 확인 → SPEC 옵션 값 검증(`resolve`, type=SPEC) → 상품 코드 발급 → 저장 → 스펙이 있으면 `replaceAll` |
| `getProducts(condition)` | 읽기 | `brandId`·`categoryId` 조건이 있으면 존재 확인 후 검색 |
| `getProducts(condition, actor)` | 읽기 | 점주(STORE_OWNER)면 `isActive=true`로 고정해 조회 |
| `getProduct(productId)` | 읽기 | 단건 조회. 없으면 `PRODUCT_NOT_FOUND` |
| `getProduct(productId, actor)` | 읽기 | 점주에게 비활성 상품은 `PRODUCT_NOT_FOUND`(404)로 숨긴다 |
| `getProductSpecs(productId)` | 읽기 | 상품에 지정된 스펙을 그룹명 포함 `ProductSpecSummary`로 반환. 없으면 빈 목록 |
| `updateProduct(command)` | 쓰기 | 아래 규칙 참고 |
| `isInUse(productId)` (private) | - | 재고, 진행 중 입고·발주(PurchaseOrder)·출고·지점 발주 중 하나라도 있으면 true (`ProductUsagePort`) |

## updateProduct 규칙

변경 필드가 없으면 `VALIDATION_ERROR`, 상품 없음은 `PRODUCT_NOT_FOUND`. 순서대로:

| 대상 | 규칙 |
|---|---|
| 브랜드 변경 | 새 브랜드 존재·활성 확인. 브랜드가 실제로 바뀌고 **SKU가 있으면** `PRODUCT_BRAND_LOCKED` (SKU 코드가 상품 코드에서 만들어지기 때문) |
| 카테고리 변경 | 새 카테고리 존재·활성 확인. 실제로 바뀌고 **스펙이나 SKU가 있으면** `PRODUCT_CATEGORY_LOCKED` |
| 스펙 | 카테고리가 바뀌거나 `specOptionValueIds`가 오면 새 카테고리 기준으로 재검증 후 전체 교체 |
| 상품 코드 | 브랜드나 카테고리 중 하나라도 바뀌면 새 조합으로 **재발급** |
| 이름·설명 | 값이 오면 변경 |
| 활성화(`isActive=true`) | 활성화 |
| 비활성화(`isActive=false`) | 이미 활성인 상품이면 `isInUse` 검사 → `PRODUCT_IN_USE`. 통과하면 상품과 **하위 활성 SKU를 같은 트랜잭션에서 함께 비활성화**(재고 도메인이 `sku.status`만 신뢰하므로). 이미 비활성인 상품은 검사 없이 통과 |
