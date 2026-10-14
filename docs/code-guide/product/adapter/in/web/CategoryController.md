# CategoryController

`product/adapter/in/web/CategoryController.java` · `@RestController` · `/api/v1/products/categories` · Swagger 태그 "카테고리"

## 의존

`CategoryUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `registerCategory` | POST `/` | 201 | `CategoryRegisterRequest` | `CategoryResponse` | 카테고리 등록 |
| `updateCategory` | PATCH `/{categoryId}` | 200 | `CategoryUpdateRequest` | `CategoryResponse` | 이름·정렬 순서 수정 |
| `getCategories` | GET `/` | 200 | 쿼리 `parentCategoryId`, `depth`, `keyword`, `isActive` (모두 선택) | `ItemsResponse<CategoryResponse>` | 목록 조회 |

페이지네이션·역할별 응답 차등은 아직 적용하지 않는다(코드 주석 기준, #20 1단계).
