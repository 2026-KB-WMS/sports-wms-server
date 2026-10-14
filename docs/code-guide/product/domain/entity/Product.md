# Product

`product/domain/entity/Product.java`

브랜드·카테고리 단위의 상품 공통 정보. 옵션 조합까지 확정된 재고·입출고 단위는 `ProductSku`가 따로 관리한다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| productId | Long | PK |
| brandId | Long | 브랜드 ID |
| categoryId | Long | 카테고리 ID |
| productCode | String | 상품 코드. 서버가 자동 발급 (유니크) |
| name | String | 상품명 |
| description | String | 설명 |
| status | ProductStatus | 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(brandId, categoryId, productCode, name, description)` | ACTIVE 상태로 생성 |
| `changeName(name)` / `changeDescription(description)` | 이름·설명 변경 |
| `changeProductCode(productCode)` | 브랜드·카테고리가 바뀌어 코드를 **재발급할 때만** 사용 |
| `changeBrand(brandId)` / `changeCategory(categoryId)` | 브랜드·카테고리 변경. 변경 가능 여부(`PRODUCT_BRAND_LOCKED`, `PRODUCT_CATEGORY_LOCKED`)는 서비스에서 검사 |
| `activate()` / `deactivate()` / `isActive()` | 상태 변경·조회. 재고·진행 업무가 있으면 비활성화 불가(`PRODUCT_IN_USE`, 서비스에서 검사) |
