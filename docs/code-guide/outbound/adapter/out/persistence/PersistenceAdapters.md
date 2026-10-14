# 영속성 어댑터 (PersistenceAdapter)

`outbound/adapter/out/persistence/`

| 어댑터 | 구현 포트 | 설명 |
|---|---|---|
| `OutboundPersistenceAdapter` | `OutboundRepository` | 헤더(`OutboundJpaRepository`)와 항목(`OutboundLineJpaRepository`)을 함께 다룬다. `findByStoreOrderIds`는 빈 컬렉션이면 쿼리 없이 빈 목록 |
| `StockAllocationPersistenceAdapter` | `StockAllocationRepository` | `isLinkedToActiveOutbound`는 `OutboundLineJpaRepository`에 위임. `findAllByIdForUpdate`는 빈 컬렉션이면 빈 목록 |
| `OutboundQueryPersistenceAdapter` | `OutboundQueryRepository` | 조회 전용. 할당 조회는 `StockAllocationQueryJpaRepository`, 출고 조회는 `OutboundQueryJpaRepository`에 위임 |

## 조회 전용 리포지토리를 따로 둔 이유

`StockAllocationQueryJpaRepository`, `OutboundQueryJpaRepository`는 `JpaRepository`가 아니라 **`Repository`만 상속**한다. 엔티티 저장·삭제 메서드를 노출하지 않고 **읽기 쿼리만** 두려는 구조다(다른 도메인 테이블을 조인하므로 쓰기 경로와 분리).

## 범위 변환

`searchAllocations`, `searchOutbounds`는 `scoped = warehouseIds != null`과 `scopeIds(warehouseIds)`(null·빈 목록이면 `[-1]`)를 JPQL에 넘긴다. 키워드는 `SearchKeyword.normalize`로 정리한다.
