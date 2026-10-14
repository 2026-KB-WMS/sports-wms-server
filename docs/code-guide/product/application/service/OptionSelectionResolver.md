# OptionSelectionResolver

`product/application/service/OptionSelectionResolver.java` · `@Component`, **package-private**

요청에서 선택한 옵션 값이 카테고리 지정과 맞는지 검증하고, 지정 순서대로 정렬해 돌려준다(ADR-025).

## 의존

`OptionValueRepository`, `OptionGroupRepository`

## 메서드

| 메서드 | 설명 |
|---|---|
| `resolve(optionValueIds, mappings, type)` | 선택한 옵션 값을 검증하고 카테고리 지정 순서대로 정렬해 반환 |
| `describe(optionValueIds)` | 스펙 값 ID들을 옵션 그룹명까지 붙여 `ProductSpecSummary` 목록으로 반환(옵션 그룹 ID 오름차순) |

## resolve 검증 순서

`type`(SPEC 또는 SKU)에 해당하는 지정만 대상으로 한다. `optionValueIds`가 null이면 빈 목록으로 본다.

| 순서 | 검사 | 오류 | HTTP 분류 |
|---|---|---|---|
| 1 | 요청 안에 중복 ID | `DUPLICATE_OPTION_VALUE` | 409 |
| 2 | 존재하지 않는 값 | `OPTION_VALUE_NOT_FOUND` | 404 |
| 3 | 비활성 값 | `OPTION_VALUE_INACTIVE` | 409 |
| 4 | 카테고리에 없거나 용도(type)가 다른 그룹의 값 | `OPTION_NOT_ALLOWED_FOR_CATEGORY` | 400 |
| 5 | 같은 그룹에서 값 둘 이상 | `OPTION_GROUP_CONFLICT` | 409 |
| 6 | 지정된 그룹인데 값이 없음 | `REQUIRED_OPTION_MISSING` (누락 그룹명 포함) | 400 |

해당 type으로 지정된 그룹은 **모두 필수**다. 지정된 그룹이 없으면 빈 선택이 정상이다.
