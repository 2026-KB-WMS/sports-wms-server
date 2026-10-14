# CategoryOptionGroupController

`product/adapter/in/web/CategoryOptionGroupController.java` · `@RestController` · Swagger 태그 "카테고리"

## 의존

`CategoryOptionGroupUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `assignOptionGroups` | PUT `/api/v1/products/categories/{categoryId}/option-groups` | 200 | `CategoryOptionGroupAssignRequest` | `ItemsResponse<CategoryOptionGroupResponse>` | 카테고리 자신의 지정을 통째로 교체. 결과는 상속분을 합친 값 |
| `getOptionGroups` | GET `/api/v1/products/categories/{categoryId}/option-groups` | 200 | - | `ItemsResponse<CategoryOptionGroupResponse>` | 상속을 반영한 지정 조회 |

`toResponse`(private)가 `CategoryOptionGroupSummary` 목록을 `CategoryOptionGroupResponse.of(summary, categoryId)`로 변환한다.
