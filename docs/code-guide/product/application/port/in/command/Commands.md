# Command

`product/application/port/in/command/` · 쓰기 유스케이스의 입력 `record`.

## 흐름

`요청 JSON` → `XxxRequest`(검증) → `toCommand(...)` → **Command** → `UseCase` 메서드(서비스) → 도메인 객체

Command는 HTTP를 모르는 순수 입력 객체다. 경로 변수(`brandId` 등)는 요청 바디가 아니라 `toCommand(id)`의 인자로 합쳐진다.

"부분 수정" 커맨드는 값이 있는(null이 아닌) 필드만 반영하고, `hasNoChanges()`가 true면 서비스가 `VALIDATION_ERROR`로 거절한다.

## 브랜드

### BrandRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드를 새로 등록한다 |
| 엔드포인트 | POST `/api/v1/products/brands` |
| 만드는 곳 | `BrandRegisterRequest.toCommand()` |
| 쓰는 곳 | `BrandService.registerBrand` |
| 필드 | brandCode(영문 3자, 서버가 대문자로 정규화), name, description |
| 쓰임 | 이름·코드 중복을 검사하고 `Brand.register(...)`로 만든다 |

### BrandUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 브랜드의 이름·설명을 수정하거나, 코드가 없는 기존 브랜드에 코드를 한 번 지정한다 |
| 엔드포인트 | PATCH `/api/v1/products/brands/{brandId}` |
| 만드는 곳 | `BrandUpdateRequest.toCommand(brandId)` |
| 쓰는 곳 | `BrandService.updateBrand` |
| 필드 | brandId, name, description, brandCode (뒤 셋은 선택) |
| 메서드 | `hasNoChanges()`: 세 선택 필드가 모두 null이면 true. 3인자 생성자는 brandCode를 null로 둔다 |
| 쓰임 | 이름은 자기 자신을 제외하고 중복 검사, 코드는 이미 있으면 거절 |

## 카테고리

### CategoryRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리를 새로 등록한다 (최상위 또는 하위) |
| 엔드포인트 | POST `/api/v1/products/categories` |
| 만드는 곳 | `CategoryRegisterRequest.toCommand()` (sortOrder가 null이면 0) |
| 쓰는 곳 | `CategoryService.registerCategory` |
| 필드 | parentCategoryId(null이면 최상위), categoryCode, name, sortOrder |
| 쓰임 | 상위 카테고리의 존재·활성 확인 후 `depth = 상위 depth + 1` 계산 |

### CategoryUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리의 이름·정렬 순서를 수정한다. 코드와 상위 카테고리는 불변이라 필드가 없다 |
| 엔드포인트 | PATCH `/api/v1/products/categories/{categoryId}` |
| 만드는 곳 | `CategoryUpdateRequest.toCommand(categoryId)` |
| 쓰는 곳 | `CategoryService.updateCategory` |
| 필드 | categoryId, name, sortOrder (뒤 둘은 선택) |
| 메서드 | `hasNoChanges()` |

### CategoryOptionGroupAssignCommand

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리가 쓰는 옵션 그룹(SPEC·SKU 용도)을 **통째로 교체**한다 |
| 엔드포인트 | PUT `/api/v1/products/categories/{categoryId}/option-groups` |
| 만드는 곳 | `CategoryOptionGroupAssignRequest.toCommand(categoryId)` (항목 sortOrder가 null이면 0) |
| 쓰는 곳 | `CategoryOptionGroupService.assignOptionGroups` |
| 필드 | categoryId, items(`Item`: optionGroupId, optionType, sortOrder) |
| 쓰임 | 빈 목록이면 해당 카테고리 자신의 지정을 모두 지운다. 상위에서 상속한 지정은 건드리지 않는다 |

## 옵션

### OptionGroupRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹(색상, 사이즈 등)을 등록한다 |
| 엔드포인트 | POST `/api/v1/products/option-groups` |
| 만드는 곳 | `OptionGroupRegisterRequest.toCommand()` |
| 쓰는 곳 | `OptionGroupService.registerOptionGroup` |
| 필드 | name |

### OptionValueRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹 안에 옵션 값(빨강, L 등)을 등록한다 |
| 엔드포인트 | POST `/api/v1/products/option-groups/{optionGroupId}/values` |
| 만드는 곳 | `OptionValueRegisterRequest.toCommand(optionGroupId)` (sortOrder가 null이면 0) |
| 쓰는 곳 | `OptionValueService.registerOptionValue` |
| 필드 | optionGroupId, optionCode, value, sortOrder |
| 쓰임 | optionCode는 이후 SKU 코드 자동 생성에 쓰인다 |

## 상품

### ProductRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 상품을 등록한다. 상품 코드는 서버가 발급하므로 입력에 없다 |
| 엔드포인트 | POST `/api/v1/products` |
| 만드는 곳 | `ProductRegisterRequest.toCommand()` |
| 쓰는 곳 | `ProductService.registerProduct` |
| 필드 | brandId, categoryId, name, description, specOptionValueIds |
| 쓰임 | specOptionValueIds는 카테고리의 SPEC 지정에 맞는지 검증 후 상품 스펙으로 저장 |

### ProductUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 상품의 이름·설명·브랜드·카테고리·활성 상태·스펙을 수정한다. 상품 코드는 직접 못 바꾼다 |
| 엔드포인트 | PATCH `/api/v1/products/{productId}` |
| 만드는 곳 | `ProductUpdateRequest.toCommand(productId)` |
| 쓰는 곳 | `ProductService.updateProduct` |
| 필드 | productId, productName, description, brandId, categoryId, isActive, specOptionValueIds (뒤 여섯은 선택) |
| 메서드 | `hasNoChanges()` |
| 쓰임 | specOptionValueIds가 있으면 빈 목록이어도 스펙 전체 교체. 브랜드·카테고리가 바뀌면 코드 재발급 |

## SKU

### ProductSkuRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | SKU를 등록한다. SKU 코드·이름은 서버가 항상 만든다(ADR-026) |
| 엔드포인트 | POST `/api/v1/products/skus` |
| 만드는 곳 | `ProductSkuRegisterRequest.toCommand()` |
| 쓰는 곳 | `ProductSkuService.registerSku` |
| 필드 | productId, barcode, weight, currentPurchasePrice, currentSupplyPrice, unit, safetyStockQuantity, optionValueIds |
| 쓰임 | optionValueIds로 옵션 조합을 검증하고 코드·이름 생성에 사용. unit이 null이면 EA |

### ProductSkuUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | SKU의 이름·바코드·무게·단가·안전 재고를 수정한다. skuCode·productId·unit·상태는 못 바꾼다 |
| 엔드포인트 | PATCH `/api/v1/products/skus/{skuId}` |
| 만드는 곳 | `ProductSkuUpdateRequest.toCommand(skuId)` |
| 쓰는 곳 | `ProductSkuService.updateSku` |
| 필드 | skuId, skuName, barcode, weight, currentPurchasePrice, currentSupplyPrice, safetyStockQuantity (뒤 여섯은 선택) |
| 메서드 | `hasNoChanges()` |
| 쓰임 | 단가가 바뀌면 `SkuPriceHistory`를 남긴다 |

### SkuOptionConnectCommand

| 항목 | 내용 |
|---|---|
| 용도 | 옵션이 연결되지 않은 기존 SKU에 옵션 값을 한 번 연결한다 |
| 엔드포인트 | POST `/api/v1/products/skus/{skuId}/options` |
| 만드는 곳 | `SkuOptionConnectRequest.toCommand(skuId)` |
| 쓰는 곳 | `ProductSkuService.connectOptions` |
| 필드 | skuId, optionValueIds |
| 쓰임 | 이미 옵션이 있으면 `SKU_OPTIONS_ALREADY_CONNECTED`. 등록 때와 같은 검증을 거친다 |

## 참고

- SKU 상태 변경(`PATCH /skus/{skuId}/status`)은 Command 없이 `changeSkuStatus(skuId, active)`로 값을 직접 넘긴다.
