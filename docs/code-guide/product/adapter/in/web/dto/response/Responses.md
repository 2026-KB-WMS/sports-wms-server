# 응답 DTO (Response)

`product/adapter/in/web/dto/response/` · 응답 `record`. JSON 필드는 camelCase다.

## 흐름

서비스가 돌려준 도메인 객체·Summary → 컨트롤러가 정적 팩토리(`from`, `of`, `listFrom`)로 **Response** 생성 → `ApiResponse`(envelope)로 감싸 반환

- 응답 DTO는 도메인 객체를 그대로 노출하지 않기 위한 변환 계층이다.
- 목록은 `ItemsResponse.of(items)`로 감싸 `data.items`가 된다.
- 브랜드명·카테고리명처럼 다른 도메인 객체의 값은 컨트롤러가 조회해서 인자로 넘긴다.

## 브랜드

### BrandCreateResponse

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드 등록 결과 |
| 엔드포인트 | POST `/products/brands` (201) |
| 만드는 곳 | `BrandController.registerBrand` → `from(Brand)` |
| 필드 | brandId, brandCode, brandName, description, isActive, createdAt |

### BrandResponse

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드 한 건. 수정 결과와 목록 항목에 공통으로 쓴다 |
| 엔드포인트 | PATCH `/products/brands/{brandId}`, GET `/products/brands` |
| 만드는 곳 | `BrandController` → `from(Brand)` |
| 필드 | brandId, brandCode, brandName, description, isActive |

## 카테고리

### CategoryResponse

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리 한 건. 등록·수정 결과와 목록 항목에 공통으로 쓴다 |
| 엔드포인트 | POST·PATCH·GET `/products/categories` |
| 만드는 곳 | `CategoryController` → `from(Category)` |
| 필드 | categoryId, parentCategoryId, categoryCode, categoryName, depth, sortOrder, isActive, createdAt |

### CategoryOptionGroupResponse

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리에 적용되는 옵션 그룹 하나(상속 포함) |
| 엔드포인트 | GET·PUT `/products/categories/{categoryId}/option-groups` |
| 만드는 곳 | `CategoryOptionGroupController.toResponse` → `of(summary, categoryId)` |
| 필드 | optionGroupId, name, optionType, sortOrder, definedCategoryId, inherited, values |
| 쓰임 | `inherited`는 `definedCategoryId != 요청 categoryId`로 계산한다. 클라이언트가 상속된 지정과 직접 지정을 구분하는 데 쓴다 |

## 옵션

### OptionGroupResponse

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹 등록 결과 |
| 엔드포인트 | POST `/products/option-groups` (201) |
| 만드는 곳 | `OptionGroupController.registerOptionGroup` → `from(OptionGroup)` |
| 필드 | optionGroupId, name, createdAt |

### OptionValueResponse

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 값 등록 결과 |
| 엔드포인트 | POST `/products/option-groups/{optionGroupId}/values` (201) |
| 만드는 곳 | `OptionValueController.registerOptionValue` → `from(OptionValue)` |
| 필드 | optionValueId, optionGroupId, optionCode, value, sortOrder, isActive, createdAt |

### OptionValueItemResponse

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹 응답들의 `values` 항목. 독립 엔드포인트는 없다 |
| 쓰이는 곳 | `ProductOptionGroupsResponse.Item`, `CategoryOptionGroupResponse` |
| 만드는 곳 | `from(OptionValueSummary)` |
| 필드 | optionValueId, optionCode(코드 도입 전 값이면 null), value, sortOrder, isActive |

### ProductOptionGroupsResponse

| 항목 | 내용 |
|---|---|
| 용도 | 상품별 옵션 그룹 조회 응답. 내부 `Item`은 전체 옵션 그룹 조회의 항목으로도 쓴다 |
| 엔드포인트 | GET `/products/{productId}/option-groups`(래퍼), GET `/products/option-groups`(`Item`) |
| 만드는 곳 | `OptionGroupController` → `of(productId, groups)`, `Item.from(summary)` |
| 필드 | 래퍼: productId, items. `Item`: optionGroupId, name, values |
| 주의 | 옵션 그룹 수가 적어 페이지 정보를 두지 않는다 |

## 상품

### ProductSummaryResponse

| 항목 | 내용 |
|---|---|
| 용도 | 상품 목록의 항목 |
| 엔드포인트 | GET `/products` |
| 만드는 곳 | `ProductController.toSummaryResponse` → `of(product, brandName, categoryName)` |
| 필드 | productId, productCode, productName, brandId, brandName, categoryId, categoryName, isActive |

### ProductDetailResponse

| 항목 | 내용 |
|---|---|
| 용도 | 상품 상세. 등록·수정 결과로도 같은 모양을 돌려준다 |
| 엔드포인트 | POST `/products`(201), GET·PATCH `/products/{productId}` |
| 만드는 곳 | `ProductController.toDetailResponse` → `of(product, brandName, categoryName, specs)` |
| 필드 | 요약 필드 + description, specs, createdAt, updatedAt |
| 쓰임 | 스펙이 없으면 `specs`는 빈 배열 |

### ProductSpecResponse

| 항목 | 내용 |
|---|---|
| 용도 | 상품 응답의 `specs` 항목 |
| 쓰이는 곳 | `ProductDetailResponse` |
| 만드는 곳 | `listFrom(List<ProductSpecSummary>)` |
| 필드 | optionGroupId, optionGroupName, optionValueId, optionCode, value |

## SKU

### 점주 마스킹 규칙 (목록·상세 공통)

- 점주(STORE_OWNER)에게는 `currentPurchasePrice`(매입 단가)와 `safetyStockQuantity`(안전 재고)를 숨긴다.
- 값을 `null`로 바꾸고 `@JsonInclude(NON_NULL)`로 **필드 자체를 JSON에서 생략**한다. 본사 응답에서 미설정 값(null)도 생략된다.
- 공급 단가(`currentSupplyPrice`)는 점주에게도 보인다.
- 판단은 `ProductSku.isPurchaseInfoVisibleTo(isStoreOwner)`가 하고, 결과를 `includePurchaseData` 인자로 넘긴다.

### ProductSkuCreateResponse

| 항목 | 내용 |
|---|---|
| 용도 | SKU 등록 결과 |
| 엔드포인트 | POST `/products/skus` (201) |
| 만드는 곳 | `ProductSkuController.registerSku` → `of(sku, options)` |
| 필드 | skuId, productId, skuCode, barcode, skuName, weight, 두 단가, unit, safetyStockQuantity, isActive, optionValues, createdAt |
| 쓰임 | 등록은 본사만 하므로 마스킹하지 않는다. 서버가 만든 skuCode·skuName을 클라이언트가 여기서 받는다 |

### ProductSkuListItemResponse

| 항목 | 내용 |
|---|---|
| 용도 | SKU 목록의 항목 |
| 엔드포인트 | GET `/products/skus` |
| 만드는 곳 | `ProductSkuController.getSkus` → `of(sku, productName, options, includePurchaseData)` |
| 필드 | skuId, skuCode, barcode, skuName, productId, productName, unit, weight, 두 단가, safetyStockQuantity, isActive, optionValues |
| 쓰임 | 점주 마스킹 적용 |

### ProductSkuDetailResponse

| 항목 | 내용 |
|---|---|
| 용도 | SKU 상세. 소속 상품·브랜드·카테고리 정보를 함께 담는다 |
| 엔드포인트 | GET `/products/skus/{skuId}` |
| 만드는 곳 | `ProductSkuController.getSku` → `of(sku, productCode, productName, brandId, brandName, categoryId, categoryName, options, includePurchaseData)` |
| 필드 | 목록 항목 필드 + productCode, brandId·brandName, categoryId·categoryName, createdAt, updatedAt |
| 쓰임 | 점주 마스킹 적용 |

### ProductSkuUpdateResponse

| 항목 | 내용 |
|---|---|
| 용도 | SKU 수정 결과 |
| 엔드포인트 | PATCH `/products/skus/{skuId}` |
| 만드는 곳 | `ProductSkuController.updateSku` → `from(ProductSku)` |
| 필드 | skuId, productId, skuCode, barcode, skuName, weight, 두 단가, unit, safetyStockQuantity, isActive, updatedAt |
| 쓰임 | 수정은 본사만 호출하므로 매입 단가를 그대로 담는다 |

### ProductSkuStatusResponse

| 항목 | 내용 |
|---|---|
| 용도 | SKU 활성 상태 변경 결과 |
| 엔드포인트 | PATCH `/products/skus/{skuId}/status` |
| 만드는 곳 | `ProductSkuController.changeSkuStatus` → `from(ProductSku)` |
| 필드 | skuId, isActive |

### SkuOptionValueResponse

| 항목 | 내용 |
|---|---|
| 용도 | SKU 응답들의 `optionValues` 항목. 독립 엔드포인트는 없다 |
| 쓰이는 곳 | SKU 등록·목록·상세 응답, `SkuOptionsConnectResponse` |
| 만드는 곳 | `from(SkuOptionSummary)`, `listFrom(list)` |
| 필드 | optionGroupId, optionGroupName, optionValueId, value |

### SkuOptionsConnectResponse

| 항목 | 내용 |
|---|---|
| 용도 | 기존 SKU에 옵션을 연결한 결과 |
| 엔드포인트 | POST `/products/skus/{skuId}/options` (201) |
| 만드는 곳 | `ProductSkuController.connectOptions` → `of(skuId, options)` |
| 필드 | skuId, optionValues |

## 기타

### CodeItemResponse

| 항목 | 내용 |
|---|---|
| 용도 | 고정 코드 목록의 항목 |
| 엔드포인트 | GET `/products/skus/units` |
| 만드는 곳 | `ProductCodeController.getSkuUnits` → `from(CodeItem)` |
| 필드 | code, name |
