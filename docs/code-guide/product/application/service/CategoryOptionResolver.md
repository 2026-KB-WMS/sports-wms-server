# CategoryOptionResolver

`product/application/service/CategoryOptionResolver.java` · `@Component`, **package-private**(같은 서비스 패키지 안에서만 사용)

카테고리에 실제로 적용되는 옵션 그룹 지정을 계산한다(ADR-025).

## 의존

`CategoryRepository`, `CategoryOptionGroupRepository`

## 메서드

| 메서드 | 설명 |
|---|---|
| `effectiveMappings(categoryId)` | 루트부터 해당 카테고리까지의 지정을 합쳐 돌려준다 |

## effectiveMappings 동작

1. 카테고리에서 부모를 따라 올라가며 체인을 만든다. 중간에 없는 카테고리가 있으면 `CATEGORY_NOT_FOUND`.
2. 체인을 루트 → 하위 순으로 뒤집는다.
3. 체인의 카테고리들의 지정을 한 번에 조회하고, 루트부터 순서대로 `optionGroupId` 기준으로 덮어쓴다. 같은 그룹이 여러 단계에 있으면 **가까운(하위) 카테고리가 우선**한다.
4. `sortOrder` 오름차순, 같으면 `optionGroupId` 오름차순으로 정렬해 반환한다.

사용처: `CategoryOptionGroupService`, `ProductService`, `ProductSkuService`.
