# Spring Data JPA 리포지토리

`product/adapter/out/persistence/repository/` · `JpaRepository`를 상속한 인터페이스. 영속성 어댑터만 사용한다.

| 리포지토리 | 엔티티 (PK) | 직접 정의한 메서드 |
|---|---|---|
| `BrandJpaRepository` | `BrandJpaEntity` (Long) | `findByIdForUpdate`(`PESSIMISTIC_WRITE`), `existsByName`, `existsByBrandCode`, `existsByBrandCodeAndBrandIdNot`, `existsByNameAndBrandIdNot`, `search` |
| `CategoryJpaRepository` | `CategoryJpaEntity` (Long) | `existsByCategoryCode`, `search` |
| `CategoryOptionGroupJpaRepository` | `CategoryOptionGroupJpaEntity` (Long) | `findByCategoryIdIn`, `deleteByCategoryId` |
| `OptionGroupJpaRepository` | `OptionGroupJpaEntity` (Long) | `existsByName` |
| `OptionValueJpaRepository` | `OptionValueJpaEntity` (Long) | `findByOptionGroupId`, `existsByOptionGroupIdAndValue`, `existsByOptionGroupIdAndOptionCode` |
| `ProductJpaRepository` | `ProductJpaEntity` (Long) | `existsByProductCode`, `search` |
| `ProductSkuJpaRepository` | `ProductSkuJpaEntity` (Long) | `existsBySkuCode`, `existsByBarcode`, `findAllByFilter`, `search` |
| `ProductSpecJpaRepository` | `ProductSpecJpaEntity` (Long) | `findByProductId`, `existsByProductId`, `deleteByProductId` |
| `SkuOptionValueJpaRepository` | `SkuOptionValueJpaEntity` (`SkuOptionValueId`) | `findById_SkuId`, `findById_SkuIdIn`, `existsById_SkuIdAndId_OptionValueId` |
| `SkuPriceHistoryJpaRepository` | `SkuPriceHistoryJpaEntity` (Long) | 없음 |
| `ProductCodeSequenceJpaRepository` | `ProductCodeSequenceJpaEntity` (`Key`) | `findForUpdate`(`PESSIMISTIC_WRITE`) |

## 검색 쿼리 정리

모든 `search`는 `(:param is null or ...)` 형태라 null인 조건은 무시된다. 키워드는 `lower(...) like lower('%keyword%')`다.

| 대상 | keyword 매칭 컬럼 | 정렬 |
|---|---|---|
| 브랜드 | **name, brandCode** | name, brandId |
| 카테고리 | name, categoryCode | sortOrder, categoryId |
| 상품 | name, productCode | createdAt 내림차순, productId 내림차순 |
| SKU | skuCode, barcode, name | createdAt 내림차순, skuId 내림차순 |

SKU 검색은 `ProductJpaEntity`를 ID로 조인해 브랜드·카테고리 조건을 건다(연관관계 없이 ID 조인, ADR-005·007).

## 주의

- 브랜드 검색은 이름뿐 아니라 **브랜드 코드도** 매칭한다. `BrandSearchCondition`의 javadoc은 "브랜드명 부분 일치"로만 적혀 있어 문서와 구현이 조금 다르다.
- `deleteByCategoryId`, `deleteByProductId`는 `@Modifying(flushAutomatically, clearAutomatically)` 벌크 삭제다. 엔티티 삭제는 flush 때 INSERT보다 늦게 실행되어 unique 제약에 걸리기 때문이다.
