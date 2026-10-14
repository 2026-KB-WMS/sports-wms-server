# Category

`product/domain/entity/Category.java`

상품을 분류하는 계층형 카테고리 마스터. 부모는 `Long parentCategoryId`로만 참조한다(ADR-005).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| categoryId | Long | PK |
| parentCategoryId | Long | 상위 카테고리 ID. null이면 최상위 |
| categoryCode | String | 카테고리 코드 (유니크). 상품 코드 발급에 쓰인다 |
| name | String | 카테고리명 |
| depth | int | 계층 깊이 |
| sortOrder | int | 표시 순서 |
| status | ProductStatus | 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(parentCategoryId, categoryCode, name, depth, sortOrder)` | ACTIVE 상태로 생성. depth 계산은 서비스 몫 |
| `isRoot()` | `parentCategoryId == null` |
| `changeName(name)` / `changeSortOrder(sortOrder)` | 이름·순서 변경 |
| `activate()` / `deactivate()` / `isActive()` | 상태 변경·조회 |

코드·상위 카테고리·depth는 생성 후 바꾸는 메서드가 없다.
