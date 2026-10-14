# CategoryOptionGroup

`product/domain/entity/CategoryOptionGroup.java`

카테고리에 지정한 옵션 그룹과 용도(SPEC·SKU). 하위 카테고리는 상위의 지정을 상속한다(ADR-025).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| categoryOptionGroupId | Long | PK |
| categoryId | Long | 카테고리 ID |
| optionGroupId | Long | 옵션 그룹 ID |
| optionType | OptionType | 용도. SPEC(상품 스펙) / SKU(SKU 옵션) |
| sortOrder | int | 표시 순서 |
| createdAt | LocalDateTime | 생성 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `assign(categoryId, optionGroupId, optionType, sortOrder)` | 카테고리에 옵션 그룹을 지정하는 매핑 생성 |

상속(상위 카테고리 지정 포함) 계산은 이 클래스가 아니라 `CategoryOptionResolver`가 한다.
