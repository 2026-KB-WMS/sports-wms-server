# Brand

`product/domain/entity/Brand.java`

상품 브랜드 마스터. 순수 도메인 객체이며 JPA 엔티티는 `BrandJpaEntity`가 따로 있다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| brandId | Long | PK |
| brandCode | String | 브랜드 코드(영문 3자). 상품 코드 발급에 쓰인다. 코드 도입 전 브랜드는 null |
| name | String | 브랜드명 (유니크 `uk_brand_name`) |
| description | String | 설명 |
| status | ProductStatus | ACTIVE / INACTIVE. 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(name, description)` | 코드 없는 브랜드 생성. 코드 도입 전 브랜드와 같은 형태 |
| `register(brandCode, name, description)` | 코드를 포함해 ACTIVE 상태로 생성 |
| `assignBrandCode(brandCode)` | 코드가 없는 기존 브랜드에 **한 번만** 지정. 이미 있으면 `IllegalStateException` |
| `changeName(name)` / `changeDescription(description)` | 이름·설명 변경 |
| `activate()` / `deactivate()` | 상태를 ACTIVE / INACTIVE로 변경 |
| `isActive()` | ACTIVE 여부 |
