# 요청 DTO (Request)

`product/adapter/in/web/dto/request/` · 요청 바디 `record`.

## 흐름

`요청 JSON` → `@Valid`로 형식 검증(Bean Validation) → `toCommand(...)` → Command → 서비스

- 형식·길이·필수 여부는 여기서, 업무 규칙(중복, 존재, 상태)은 서비스에서 검증한다.
- 경로 변수는 바디에 없고 `toCommand(id)`의 인자로 합쳐진다.
- **변경 불가 필드 규칙**: 수정(PATCH) 요청에서 바꿀 수 없는 필드(`categoryCode`, `productCode`, `skuCode` 등)는 검증 애노테이션 대신 `toCommand()`가 값이 있으면 `VALIDATION_ERROR`(400)를 던진다. 명세가 403·409가 아닌 400을 요구하기 때문이다.
- 서비스에도 같은 규칙(코드 형식, 변경 필드 없음 등)이 있다. 웹 검증을 거치지 않는 호출도 지키게 하는 이중 방어다.

## BrandRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/products/brands` |
| 변환 | `toCommand()` → `BrandRegisterCommand` |
| 필드 | brandCode(필수, 영문 3자), brandName(필수, 최대 100), description(최대 500) |
| 주의 | JSON 필드는 `brandName`, Command 필드는 `name`이다 |

## BrandUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/products/brands/{brandId}` |
| 변환 | `toCommand(brandId)` → `BrandUpdateCommand` |
| 필드 | brandName(최대 100), description(최대 500), brandCode(영문 3자). 모두 선택 |
| 추가 검사 | brandName이 공백 문자열이면 `VALIDATION_ERROR` |

## CategoryRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/products/categories` |
| 변환 | `toCommand()` → `CategoryRegisterCommand` (sortOrder가 null이면 0) |
| 필드 | parentCategoryId(선택), categoryCode(필수, 영문 3자), categoryName(필수, 최대 100), sortOrder(0 이상) |

## CategoryUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/products/categories/{categoryId}` |
| 변환 | `toCommand(categoryId)` → `CategoryUpdateCommand` |
| 필드 | categoryName(최대 100), sortOrder(0 이상), categoryCode, parentCategoryId |
| 추가 검사 | categoryCode·parentCategoryId가 있으면 거부(불변). categoryName이 공백이면 거부 |

## CategoryOptionGroupAssignRequest

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리의 옵션 그룹 지정을 통째로 교체하는 요청 바디 |
| 엔드포인트 | PUT `/api/v1/products/categories/{categoryId}/option-groups` |
| 변환 | `toCommand(categoryId)` → `CategoryOptionGroupAssignCommand` (항목 sortOrder가 null이면 0) |
| 필드 | items(필수). 항목: optionGroupId(필수), optionType(필수, SPEC/SKU), sortOrder(0 이상) |
| 주의 | 빈 목록도 유효하며 자기 지정을 모두 지운다 |

## OptionGroupRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/products/option-groups` |
| 변환 | `toCommand()` → `OptionGroupRegisterCommand` |
| 필드 | name(필수, 최대 100) |

## OptionValueRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 값 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/products/option-groups/{optionGroupId}/values` |
| 변환 | `toCommand(optionGroupId)` → `OptionValueRegisterCommand` (sortOrder가 null이면 0) |
| 필드 | optionCode(필수, 영문·숫자 1~10자), value(필수, 최대 100), sortOrder(0 이상) |

## ProductRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 상품 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/products` |
| 변환 | `toCommand()` → `ProductRegisterCommand` |
| 필드 | brandId(필수), categoryId(필수), productName(필수, 최대 200), description(최대 1000), specOptionValueIds |
| 주의 | 상품 코드는 요청에 없다(서버 발급) |

## ProductUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 상품 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/products/{productId}` |
| 변환 | `toCommand(productId)` → `ProductUpdateCommand` |
| 필드 | productName(최대 200), description(최대 1000), brandId, categoryId, isActive, productCode, specOptionValueIds |
| 추가 검사 | productCode가 있으면 거부(불변) |

## ProductSkuRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | SKU 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/products/skus` |
| 변환 | `toCommand()` → `ProductSkuRegisterCommand` |
| 필드 | productId(필수), barcode(최대 100), weight(0 이상), currentPurchasePrice(필수, 0 이상), currentSupplyPrice(필수, 0 이상), unit(최대 20), safetyStockQuantity(0 이상), optionValueIds |
| 주의 | SKU 코드·이름은 요청에 없다(서버가 상품·옵션으로 생성). `unit`의 허용 값 검사는 서비스(`SkuUnit`)가 한다 |

## ProductSkuUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | SKU 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/products/skus/{skuId}` |
| 변환 | `toCommand(skuId)` → `ProductSkuUpdateCommand` |
| 필드 | skuName(1~200), barcode(최대 100), weight, currentPurchasePrice, currentSupplyPrice, safetyStockQuantity(각 0 이상), skuCode, productId, unit, isActive |
| 추가 검사 | skuCode·productId·unit·isActive가 있으면 거부. 상태 변경은 `/status`로 안내 |

## ProductSkuStatusRequest

| 항목 | 내용 |
|---|---|
| 용도 | SKU 활성/비활성 전환 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/products/skus/{skuId}/status` |
| 변환 | 없음. 컨트롤러가 `request.isActive()`를 `changeSkuStatus`에 직접 넘긴다 |
| 필드 | isActive(필수) |

## SkuOptionConnectRequest

| 항목 | 내용 |
|---|---|
| 용도 | 기존 SKU에 옵션 값을 연결하는 요청 바디 |
| 엔드포인트 | POST `/api/v1/products/skus/{skuId}/options` |
| 변환 | `toCommand(skuId)` → `SkuOptionConnectCommand` |
| 필드 | optionValueIds(1개 이상) |
