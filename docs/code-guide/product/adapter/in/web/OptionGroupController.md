# OptionGroupController

`product/adapter/in/web/OptionGroupController.java` · `@RestController` · Swagger 태그 "상품 옵션"

## 의존

`OptionGroupUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `registerOptionGroup` | POST `/api/v1/products/option-groups` | 201 | `OptionGroupRegisterRequest` | `OptionGroupResponse` | 옵션 그룹 등록 |
| `getOptionGroups` | GET `/api/v1/products/option-groups` | 200 | - | `ItemsResponse<ProductOptionGroupsResponse.Item>` | 전체 옵션 그룹과 옵션 값 |
| `getOptionGroupsByProduct` | GET `/api/v1/products/{productId}/option-groups` | 200 | - | `ProductOptionGroupsResponse` | 상품의 SKU에 실제 연결된 옵션 그룹 |
