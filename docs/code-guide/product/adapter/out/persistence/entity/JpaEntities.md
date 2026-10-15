# JPA 엔티티

`product/adapter/out/persistence/entity/` · DB 테이블에 매핑되는 영속성 모델. 도메인 객체(`domain/entity`)와 분리되어 있고 `fromDomain(...)` / `toDomain()`으로 변환한다. 엔티티끼리 연관관계는 없고 모두 `Long xxxId`로만 참조한다(ADR-005).

`BaseTimeEntity`를 상속한 엔티티는 `createdAt`, `updatedAt`을 물려받는다. `status`는 `EnumType.STRING`(길이 20)이다.

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `BrandJpaEntity` | brand | brand_id (IDENTITY) | `uk_brand_name`(name), `uk_brand_code`(brand_code) | brand_code(길이 3, null 허용), name(100), description(500), status |
| `CategoryJpaEntity` | category | category_id | `uk_category_code`(category_code) | parent_category_id(null 허용), category_code(50), name(100), depth(TINYINT), sort_order, status |
| `CategoryOptionGroupJpaEntity` | category_option_group | category_option_group_id | `uk_category_option_group`(category_id, option_group_id) | category_id, option_group_id, option_type(길이 10), sort_order |
| `OptionGroupJpaEntity` | option_group | option_group_id | `uk_option_group_name`(name) | name(100) |
| `OptionValueJpaEntity` | option_value | option_value_id | `uk_option_value_group_value`(option_group_id, value) | option_group_id, option_code(길이 10, null 허용), value(100), sort_order, status |
| `ProductJpaEntity` | product | product_id | `uk_product_code`(product_code) | brand_id, category_id, product_code(50), name(200), description(1000), status |
| `ProductSkuJpaEntity` | product_sku | sku_id | `uk_product_sku_code`(sku_code), `uk_product_sku_barcode`(barcode) | product_id, sku_code(50), barcode(100), name(200), weight(12,3), current_purchase_price(18,2), current_supply_price(18,2), unit(20), safety_stock_quantity, status |
| `ProductSpecJpaEntity` | product_spec | product_spec_id | `uk_product_spec`(product_id, option_value_id) | product_id, option_value_id, created_at(`@CreationTimestamp`) |
| `SkuOptionValueJpaEntity` | sku_option_value | 복합키 `SkuOptionValueId`(sku_id, option_value_id) | PK 자체 | created_at(`@CreationTimestamp`) |
| `SkuPriceHistoryJpaEntity` | sku_price_history | sku_price_history_id | 없음 | sku_id, 이전·이후 매입가, 이전·이후 공급가(각 18,2), changed_by, changed_at |
| `ProductCodeSequenceJpaEntity` | product_code_sequence | 복합키 `Key`(brand_id, category_id) `@IdClass` | PK 자체 | last_no |

## 보조 클래스

| 클래스 | 설명 |
|---|---|
| `SkuOptionValueId` | `sku_option_value`의 `@Embeddable` 복합키. `equals`/`hashCode`는 skuId, optionValueId 기준(ADR-009) |
| `ProductCodeSequenceJpaEntity.Key` | 채번 테이블의 `@IdClass`용 키. `Serializable` |

## ProductCodeSequenceJpaEntity

브랜드+카테고리 조합별 마지막 발급 번호를 보관한다. 코드가 재발급되어도 번호가 되돌아가지 않도록, 기존 상품 개수가 아니라 이 값으로 채번한다.

| 메서드 | 설명 |
|---|---|
| 생성자 `(brandId, categoryId)` | `lastNo = 0`으로 시작 |
| `next()` | `lastNo`를 1 올리고 그 값을 반환 |

## 주의

- `ProductConstraintErrorCodes`에는 `uk_option_value_group_code`(DUPLICATE_OPTION_CODE)가 있지만, `OptionValueJpaEntity`의 `@UniqueConstraint`에는 해당 제약이 선언돼 있지 않다. 이 제약은 `V1__baseline.sql`에서 `(option_group_id, option_code)`로 추가된다. 엔티티 애노테이션만 보면 빠져 있으니, 제약 목록은 마이그레이션 기준으로 본다.
- `product_spec`의 유니크는 (상품, 옵션 값)이다. "그룹당 값 하나" 규칙은 DB가 아니라 서비스(`OptionSelectionResolver`)가 보장한다.
