# CategoryOptionGroupService

`product/application/service/CategoryOptionGroupService.java` · 구현: `CategoryOptionGroupUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

카테고리에 옵션 그룹(SPEC·SKU 용도)을 지정하고, 실제 적용되는 지정을 조회한다(ADR-025).

## 의존

`CategoryRepository`, `OptionGroupRepository`, `OptionValueRepository`, `CategoryOptionGroupRepository`, `CategoryOptionResolver`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `assignOptionGroups(command)` | 쓰기 | 카테고리 존재 확인(`CATEGORY_NOT_FOUND`) → 요청 항목 검증(같은 그룹 중복 → `VALIDATION_ERROR`, 없는 그룹 → `OPTION_GROUP_NOT_FOUND`) → 해당 카테고리의 지정을 **전부 교체**(`replaceAll`) → 결과를 `getOptionGroups`로 반환 |
| `getOptionGroups(categoryId)` | 읽기 | `CategoryOptionResolver.effectiveMappings`로 상위 상속을 반영한 지정을 구하고, 그룹명과 옵션 값 목록(정렬 순서대로)을 붙여 `CategoryOptionGroupSummary`로 반환 |

## 참고

- 결과의 `definedCategoryId`는 **그 지정이 실제로 설정된 카테고리**다(상속받은 경우 상위 카테고리 ID).
- 옵션 값 목록에는 비활성 값도 포함되고 `isActive`로 구분한다.
