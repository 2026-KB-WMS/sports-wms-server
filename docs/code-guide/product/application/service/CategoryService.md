# CategoryService

`product/application/service/CategoryService.java` · 구현: `CategoryUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

카테고리 등록·수정·조회.

## 의존

`CategoryRepository`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerCategory(command)` | 쓰기 | 코드를 영문 3자·대문자로 정규화, 중복 → `DUPLICATE_CATEGORY_CODE`. 상위가 있으면 존재(`PARENT_CATEGORY_NOT_FOUND`)와 활성(`PARENT_CATEGORY_INACTIVE`) 확인 후 `depth = 부모 depth + 1`, 없으면 depth 1 |
| `updateCategory(command)` | 쓰기 | 변경 필드 없음 → `VALIDATION_ERROR`. 없는 카테고리 → `CATEGORY_NOT_FOUND`. **이름·정렬 순서만** 수정 가능 |
| `getCategories(condition)` | 읽기 | `parentCategoryId` 조건이 있으면 그 카테고리 존재 확인(`CATEGORY_NOT_FOUND`) 후 검색 |
| `getCategory(categoryId)` | 읽기 | 단건 조회. 없으면 `CATEGORY_NOT_FOUND` |

## 참고

- 코드·상위 카테고리·활성 상태는 이 서비스에서 수정할 수 없다.
