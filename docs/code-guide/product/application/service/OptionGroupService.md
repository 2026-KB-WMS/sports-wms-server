# OptionGroupService

`product/application/service/OptionGroupService.java` · 구현: `OptionGroupUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

옵션 그룹 등록과 조회. 옵션 그룹은 특정 상품에 종속되지 않는 공용 마스터다.

## 의존

`OptionGroupRepository`, `OptionValueRepository`, `SkuOptionValueRepository`, `ProductSkuRepository`, `ProductRepository`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerOptionGroup(command)` | 쓰기 | 이름 중복 → `DUPLICATE_OPTION_GROUP_NAME`. 저장 |
| `getOptionGroups()` | 읽기 | 전체 그룹을 ID 오름차순으로, 각 그룹의 옵션 값(정렬 순서대로, 비활성 포함)과 함께 `ProductOptionGroupSummary`로 반환 |
| `getOptionGroupsByProduct(productId)` | 읽기 | 상품 존재 확인(`PRODUCT_NOT_FOUND`). 상품의 SKU들에 **실제로 연결된** 옵션 값을 모아 옵션 그룹 ID 오름차순으로 묶어 반환 |

## getOptionGroupsByProduct 경로

`상품 → SKU 목록 → SkuOptionValue → OptionValue → OptionGroup` 순으로 거슬러 올라간다. 연결된 옵션 값이 없으면 빈 목록이다. 그룹별 `TreeMap`으로 묶어 ID 순서를 보장한다.
