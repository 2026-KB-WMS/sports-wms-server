# ADR-009 sku_option_value 복합키는 @Embeddable/@EmbeddedId로 구현

## 상태
`Accepted` (2026-09-24)

## 컨텍스트
프로젝트 전반의 엔티티 설계 컨벤션은 [ADR-005](005-entity-reference-by-id.md)에 따라 "모든 엔티티 참조는 도메인 구분 없이 전부 Long ID 참조"이며, 대부분의 엔티티도 auto-increment `Long id` 단일 PK를 갖는다.
하지만 Product 도메인의 `sku_option_value`는 SKU와 옵션값(OptionValue)을 연결하는 순수 조인 테이블 성격의 엔티티로, `(sku_id, option_value_id)` 조합 자체가 곧 비즈니스적 유일 식별자다. 이 테이블에 별도의 auto-increment PK를 추가할지, 아니면 조합 자체를 PK로 사용할지 결정이 필요했다.

## 결정
`sku_option_value`는 별도의 auto-increment PK 컬럼을 두지 않고, `(sku_id, option_value_id)` 복합키를 `@Embeddable` + `@EmbeddedId`로 구현한다.
- `SkuOptionValueId` — `@Embeddable`로 선언한 복합키 클래스. `skuId`, `optionValueId` 두 필드를 갖고, `equals`/`hashCode`를 필드값 기반으로 직접 오버라이드한다(JPA가 `@EmbeddedId`를 엔티티 식별자로 올바르게 다루려면 필수).
- `SkuOptionValueJpaEntity`는 `@EmbeddedId private SkuOptionValueId id` 필드로 이 복합키를 PK로 사용한다.
- `SkuOptionValueJpaRepository`는 `JpaRepository<SkuOptionValueJpaEntity, SkuOptionValueId>`로 선언되며, Spring Data가 중첩 프로퍼티 경로 기반 파생 쿼리(`findById_SkuId`, `existsById_SkuIdAndId_OptionValueId` 등)를 생성한다.
- 이 패턴은 JPA 영속성 계층에만 국한된다. 순수 도메인 엔티티(`SkuOptionValue`)는 `@Embeddable`을 전혀 알지 못하고 `skuId`/`optionValueId`를 일반 필드로 갖는다.

## 근거
- `(sku_id, option_value_id)` 조합이 DB 레벨에서 자연스럽게 PK 유일성 제약을 갖게 되어, 중복 삽입을 별도 UNIQUE 제약 없이 원천 차단할 수 있다.
- 조인 테이블 로우를 다른 테이블에서 FK로 참조할 필요가 없어, 복합키 참조로 인한 스키마 복잡도 증가 부담이 없다.
- 조인 테이블에 대한 조회는 대부분 "이 조합이 존재하는가"이므로, 복합키를 그대로 조회 키로 사용하는 것이 자연스럽고 별도 서로게이트 컬럼(및 그에 대한 인덱스)을 아낄 수 있다.

## 검토했던 대안
- **auto-increment `Long id` 단일 PK + `(sku_id, option_value_id)` UNIQUE 제약**: 프로젝트 전체 컨벤션(ADR-005, 다른 모든 엔티티)과 일관성을 유지할 수 있으나, 유일성 보장을 위해 PK와 별개로 UNIQUE 인덱스를 추가로 걸어야 하고 이 로우 자체는 어차피 다른 곳에서 FK로 참조되지 않아 서로게이트 키의 실익이 없다고 판단해 채택하지 않음.

## 영향
- `sku_option_value` 조회/존재 확인 쿼리는 `findById_SkuId`, `existsById_SkuIdAndId_OptionValueId`처럼 중첩 프로퍼티 경로를 사용해야 하며, 이는 단일 PK 엔티티의 `findBySkuId` 같은 파생 쿼리보다 다소 낯설 수 있다.
- 이 엔티티는 프로젝트에서 유일하게 복합키(`@EmbeddedId`)를 사용하는 예외 케이스이므로, 이 테이블만 PK 구조가 다르다는 점을 인지해야 한다.
- 향후 이 조인 테이블 로우를 다른 엔티티가 FK로 참조해야 하는 요구사항이 생기면, 복합키 참조 구조로 인해 스키마가 복잡해질 수 있어 재검토가 필요하다.
