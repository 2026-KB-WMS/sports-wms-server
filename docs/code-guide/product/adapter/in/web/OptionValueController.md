# OptionValueController

`product/adapter/in/web/OptionValueController.java` · `@RestController` · Swagger 태그 "상품 옵션"

## 의존

`OptionValueUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `registerOptionValue` | POST `/api/v1/products/option-groups/{optionGroupId}/values` | 201 | `OptionValueRegisterRequest` | `OptionValueResponse` | 옵션 그룹에 옵션 값 등록 |
