# 조회 결과 (Result / Summary)

`product/application/port/in/result/` · 서비스가 여러 엔티티를 조합해 돌려주는 조회용 `record`.

## 흐름

`Repository 조회` → **서비스가 Summary로 조립** → `UseCase` 반환 → 컨트롤러가 `XxxResponse.from/of(...)`로 응답 DTO 변환

도메인 엔티티만으로는 부족한 값(옵션 그룹명, 소속 그룹 등)을 서비스에서 미리 조인해 담는다. 웹 어댑터가 repository를 직접 부르지 않게 하려는 목적이다.

## CodeItem

| 항목 | 내용 |
|---|---|
| 용도 | DB가 아닌 고정 코드(enum) 목록의 항목 하나 |
| 엔드포인트 | GET `/api/v1/products/skus/units` |
| 만드는 곳 | `ProductCodeService.getSkuUnits` (`SkuUnit`의 code, description) |
| 쓰는 곳 | `CodeItemResponse.from` |
| 필드 | code(EA 등), name(개 등) |

## OptionValueSummary

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹 안의 옵션 값 하나. 다른 Summary의 `values` 항목으로 중첩된다 |
| 엔드포인트 | GET `/products/option-groups`, GET `/products/{productId}/option-groups`, GET `/products/categories/{categoryId}/option-groups` |
| 만드는 곳 | `OptionGroupService`(`getOptionGroups`, `getOptionGroupsByProduct`), `CategoryOptionGroupService.getOptionGroups` |
| 쓰는 곳 | `OptionValueItemResponse.from` |
| 필드 | optionValueId, optionCode(코드 도입 전 값이면 null), value, sortOrder, isActive |
| 쓰임 | 비활성 값도 담고 `isActive`로 구분한다. 항상 `sortOrder` 오름차순 |

## ProductOptionGroupSummary

| 항목 | 내용 |
|---|---|
| 용도 | 옵션 그룹 하나와 그 안의 옵션 값 목록 |
| 엔드포인트 | GET `/products/option-groups`(전체), GET `/products/{productId}/option-groups`(상품별) |
| 만드는 곳 | `OptionGroupService.getOptionGroups`, `getOptionGroupsByProduct` |
| 쓰는 곳 | `ProductOptionGroupsResponse.Item.from` |
| 필드 | optionGroupId, name, values(`OptionValueSummary` 목록) |
| 쓰임 | 전체 조회는 그룹의 **모든** 값, 상품별 조회는 그 상품의 SKU에 **실제 연결된** 값만 담는다 |

## CategoryOptionGroupSummary

| 항목 | 내용 |
|---|---|
| 용도 | 카테고리에 적용되는 옵션 그룹 하나(상위에서 상속한 것 포함)와 선택 가능한 값 |
| 엔드포인트 | GET·PUT `/products/categories/{categoryId}/option-groups` |
| 만드는 곳 | `CategoryOptionGroupService.getOptionGroups` (PUT도 저장 후 이 메서드로 결과를 만든다) |
| 쓰는 곳 | `CategoryOptionGroupResponse.of(summary, categoryId)` |
| 필드 | optionGroupId, name, optionType(SPEC/SKU), sortOrder, definedCategoryId, values |
| 쓰임 | `definedCategoryId`는 이 지정을 실제로 등록한 카테고리다. 응답 변환 시 요청 카테고리와 다르면 `inherited=true`가 된다 |

## ProductSpecSummary

| 항목 | 내용 |
|---|---|
| 용도 | 상품에 지정된 스펙 값 하나를 옵션 그룹명까지 조인해 담은 것 |
| 엔드포인트 | POST·GET·PATCH `/products`, `/products/{productId}`의 `specs` |
| 만드는 곳 | `ProductService.getProductSpecs` → `OptionSelectionResolver.describe` |
| 쓰는 곳 | `ProductSpecResponse.listFrom` |
| 필드 | optionGroupId, optionGroupName, optionValueId, optionCode, value |
| 쓰임 | 옵션 그룹 ID 오름차순. 스펙이 없으면 빈 목록 |

## SkuOptionSummary

| 항목 | 내용 |
|---|---|
| 용도 | SKU에 연결된 옵션 값 하나를 그룹명까지 조인해 담은 것 |
| 엔드포인트 | GET·POST `/products/skus`, GET `/products/skus/{skuId}`, POST `/products/skus/{skuId}/options`의 `optionValues` |
| 만드는 곳 | `ProductSkuService.getSkuOptions`, `getSkuOptionsBySkuIds` |
| 쓰는 곳 | `SkuOptionValueResponse.from/listFrom` |
| 필드 | optionGroupId, optionGroupName, optionValueId, value |
| 쓰임 | 목록 응답은 `getSkuOptionsBySkuIds`로 SKU 전체를 쿼리 3번(연결·옵션 값·옵션 그룹)에 조회한다 |
