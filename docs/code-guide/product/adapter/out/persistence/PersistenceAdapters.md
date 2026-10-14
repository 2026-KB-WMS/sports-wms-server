# 영속성 어댑터 (PersistenceAdapter)

`product/adapter/out/persistence/` · `application/port/out`의 Repository 인터페이스를 JPA로 구현한 `@Component`.

## 공통 패턴

- 도메인 객체 ↔ JPA 엔티티를 `XxxJpaEntity.fromDomain(...)` / `toDomain()`으로 변환한다. 서비스는 JPA 엔티티를 보지 못한다.
- `save`는 `fromDomain` → `JpaRepository.save` → `toDomain` 순서. ID가 있으면 UPDATE, 없으면 INSERT.
- 검색은 `SearchKeyword.normalize(keyword)`로 키워드를 정리하고 `ProductStatus.fromActiveFlag(isActive)`로 상태를 바꿔 JPQL에 넘긴다.
- `findAllByIds`, `findBySkuIdIn`, `findByCategoryIds`는 입력이 비어 있으면 쿼리 없이 빈 목록을 반환한다.

## 어댑터별

| 어댑터 | 구현 포트 | 특이 사항 |
|---|---|---|
| `BrandPersistenceAdapter` | `BrandRepository` | `findByIdForUpdate`는 비관적 락 쿼리를 호출한다 |
| `CategoryPersistenceAdapter` | `CategoryRepository` | |
| `CategoryOptionGroupPersistenceAdapter` | `CategoryOptionGroupRepository` | `replaceAll` = 벌크 `deleteByCategoryId` 후 `saveAll` |
| `OptionGroupPersistenceAdapter` | `OptionGroupRepository` | `findAllByIds`는 `findAllById` 사용 |
| `OptionValuePersistenceAdapter` | `OptionValueRepository` | |
| `ProductPersistenceAdapter` | `ProductRepository` | |
| `ProductSkuPersistenceAdapter` | `ProductSkuRepository` | `findAll(productId)`는 `findAllByFilter`(productId가 null이면 전체) |
| `ProductSpecPersistenceAdapter` | `ProductSpecRepository` | `replaceAll` = 벌크 `deleteByProductId` 후 `ProductSpec.assign`으로 만든 항목을 `saveAll` |
| `SkuOptionValuePersistenceAdapter` | `SkuOptionValueRepository` | 복합키 속성 경로 메서드(`findById_SkuId` 등) 사용 |
| `SkuPriceHistoryPersistenceAdapter` | `SkuPriceHistoryRepository` | 저장만 한다 |
| `ProductCodeSequencePersistenceAdapter` | `ProductCodeSequenceRepository` | 아래 참고 |
| `ProductUsageAdapter` | `ProductUsagePort` | 아래 참고 |

## ProductCodeSequencePersistenceAdapter.next

1. `findForUpdate(brandId, categoryId)`로 채번 행을 **비관적 락**으로 조회한다.
2. 행이 없으면 `saveAndFlush(new ProductCodeSequenceJpaEntity(...))`로 만든다(`lastNo=0`).
3. `sequence.next()`로 `lastNo`를 1 올려 반환한다. 영속 상태라 트랜잭션 커밋 때 UPDATE된다.

행이 아직 없을 때의 동시 생성은 이 어댑터가 막지 못해서, 호출자(`ProductCodeIssuer`)가 브랜드 행을 먼저 잠근다.

## ProductUsageAdapter

상품·SKU 비활성화 전 사용 여부를 확인하는 **읽기 전용** 어댑터. 진행 중 입고·창고 발주·출고·지점 발주는 각 소유 도메인의 `*UsageUseCase`(`InboundUsageUseCase`, `OutboundUsageUseCase`, `StoreOrderUsageUseCase`)가 판단해 결과만 돌려주고, 상품의 SKU ID 목록(`skuIdsOf`)으로 바꿔 질문한다. 진행 중 상태 집합은 소유 도메인이 갖는다. 재고 존재 여부만 `EntityManager`로 JPQL 직접 조회한다(ADR-007).

| 판단 | 방식 |
|---|---|
| 입고, 창고 발주 | 입고 `InboundUsageUseCase` |
| 출고 | 출고 `OutboundUsageUseCase` |
| 지점 발주 | 발주 `StoreOrderUsageUseCase` |
| 재고 | `onHandQuantity > 0` 또는 `allocatedQuantity > 0`인 재고 로트를 JPQL로 조회 |
