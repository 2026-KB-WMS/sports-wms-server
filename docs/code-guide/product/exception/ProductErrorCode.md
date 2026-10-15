# ProductErrorCode / ProductConstraintErrorCodes

`product/exception/`

## ProductErrorCode

상품 도메인 전용 오류 코드 enum(`DomainErrorCode` 구현). 404·409도 공통 코드 대신 이 도메인 코드를 쓴다. 각 값은 `(HTTP 분류 ErrorCode, 기본 메시지)`를 가진다.

| 분류 | 코드 |
|---|---|
| 브랜드 | BRAND_NOT_FOUND, BRAND_INACTIVE, DUPLICATE_BRAND_NAME, DUPLICATE_BRAND_CODE, BRAND_CODE_REQUIRED |
| 카테고리 | CATEGORY_NOT_FOUND, CATEGORY_INACTIVE, PARENT_CATEGORY_NOT_FOUND, PARENT_CATEGORY_INACTIVE, DUPLICATE_CATEGORY_CODE |
| 상품 | PRODUCT_NOT_FOUND, PRODUCT_INACTIVE, PRODUCT_IN_USE, DUPLICATE_PRODUCT_CODE, PRODUCT_BRAND_LOCKED, PRODUCT_CATEGORY_LOCKED |
| 옵션 그룹·값 | OPTION_GROUP_NOT_FOUND, DUPLICATE_OPTION_GROUP_NAME, OPTION_VALUE_NOT_FOUND, OPTION_VALUE_INACTIVE, DUPLICATE_OPTION_VALUE, DUPLICATE_OPTION_CODE |
| 카테고리 옵션 검증 | OPTION_GROUP_CONFLICT, OPTION_NOT_ALLOWED_FOR_CATEGORY, REQUIRED_OPTION_MISSING |
| SKU | SKU_NOT_FOUND, SKU_INACTIVE, SKU_IN_USE, DUPLICATE_SKU_CODE, DUPLICATE_BARCODE, DUPLICATE_SKU_OPTION_COMBINATION, SKU_OPTIONS_ALREADY_CONNECTED |

필드: `errorCode`(공통 분류: NOT_FOUND / CONFLICT / VALIDATION_ERROR), `defaultMessage`.

## ProductConstraintErrorCodes

DB 유니크 제약 이름을 도메인 오류 코드로 매핑하는 `@Component`(`ConstraintErrorCodeProvider` 구현). 저장 시 제약 위반이 나면 `GlobalExceptionHandler`가 이 맵으로 변환한다. 기준은 `V1__baseline.sql`.

| 제약 이름 | 오류 코드 |
|---|---|
| uk_brand_name | DUPLICATE_BRAND_NAME |
| uk_brand_code | DUPLICATE_BRAND_CODE |
| uk_category_code | DUPLICATE_CATEGORY_CODE |
| uk_product_code | DUPLICATE_PRODUCT_CODE |
| uk_product_sku_code | DUPLICATE_SKU_CODE |
| uk_product_sku_barcode | DUPLICATE_BARCODE |
| uk_option_group_name | DUPLICATE_OPTION_GROUP_NAME |
| uk_option_value_group_value | DUPLICATE_OPTION_VALUE |
| uk_option_value_group_code | DUPLICATE_OPTION_CODE |

새 유니크 제약을 추가하면 이 맵에도 넣어야 한다.
