# BrandController

`product/adapter/in/web/BrandController.java` · `@RestController` · `/api/v1/products/brands` · Swagger 태그 "브랜드"

## 의존

`BrandQueryUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 |
|---|---|---|---|---|---|
| `registerBrand` | POST `/` | 201 | `BrandRegisterRequest` | `BrandCreateResponse` | 브랜드 등록 |
| `updateBrand` | PATCH `/{brandId}` | 200 | `BrandUpdateRequest` | `BrandResponse` | 이름·설명·코드 부분 수정 |
| `getBrands` | GET `/` | 200 | 쿼리 `keyword`, `isActive` (모두 선택) | `ItemsResponse<BrandResponse>` | 목록 조회 |

요청 DTO는 `toCommand(...)`로 Command로 변환하고, 응답은 도메인 객체에서 `from(brand)`로 만든다. 권한 처리는 이 컨트롤러에 없다.
