# 영속성 어댑터 (PersistenceAdapter)

`warehouse/adapter/out/persistence/` · `application/port/out`의 Repository 인터페이스를 JPA로 구현한 `@Component`.

## 공통 패턴

- 도메인 객체 ↔ JPA 엔티티를 `fromDomain(...)` / `toDomain()`으로 변환한다.
- 검색은 `SearchKeyword.normalize(keyword)`와 `WarehouseStatus.fromActiveFlag(isActive)`를 거쳐 JPQL에 넘긴다.
- `FOR UPDATE` 조회는 `@Lock(PESSIMISTIC_WRITE)` 쿼리를 쓴다.

## 어댑터별

| 어댑터 | 구현 포트 | 특이 사항 |
|---|---|---|
| `WarehousePersistenceAdapter` | `WarehouseRepository` | `findByIdForUpdate`, `existsById` |
| `WarehouseSectionPersistenceAdapter` | `WarehouseSectionRepository` | `sumActiveCapacity`는 `WarehouseStatus.ACTIVE`를 고정해 `sumCapacity` 호출. `deleteById` 제공 |
| `WarehouseMemberPersistenceAdapter` | `WarehouseMemberRepository` | `search`는 JPQL이 곧바로 `WarehouseMemberView`를 만들어 반환(엔티티 변환 없음) |
| `WarehouseUsageAdapter` | `WarehouseUsagePort` | 아래 참고 |

## WarehouseUsageAdapter

창고 비활성화·구역 삭제 전 진행 중 업무와 참조 여부를 확인하는 **읽기 전용** 어댑터. 진행 중 업무는 소유 도메인의 `*UsageUseCase`가 창고 ID 기준으로 판단해 결과만 돌려주고, 구역 참조 여부만 `EntityManager`로 JPQL 직접 조회한다(ADR-007).

| 메서드 | 판단 | 진행 중 상태(소유 도메인이 정함) |
|---|---|---|
| `hasInProgressInbounds` | 입고 `InboundUsageUseCase` | ARRIVED, INSPECTING |
| `hasInProgressPurchaseOrders` | 입고 `InboundUsageUseCase` | REQUESTED, CONFIRMED |
| `hasInProgressOutbounds` | 출고 `OutboundUsageUseCase`. 출고는 창고를 직접 갖지 않아 **지점 발주의 `warehouseId`**로 판단 | READY, PICKING, PICKED, SHIPPED |
| `hasInProgressStoreOrders` | 발주 `StoreOrderUsageUseCase` | REQUESTED, APPROVED, ASSIGNED, ON_HOLD |
| `isSectionReferenced` | JPQL: `InventoryLotJpaEntity.sectionId`가 있거나, `InboundLineJpaEntity`의 `acceptedSectionId`·`defectSectionId`가 이 구역 | (상태 무관) |
