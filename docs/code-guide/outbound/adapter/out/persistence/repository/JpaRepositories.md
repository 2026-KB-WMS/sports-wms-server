# Spring Data JPA 리포지토리

`outbound/adapter/out/persistence/repository/`

| 리포지토리 | 상속 | 대상 | 주요 메서드 |
|---|---|---|---|
| `OutboundJpaRepository` | `JpaRepository` | `OutboundJpaEntity` | `findByIdForUpdate`, `existsByOutboundNo`, `countByOutboundNoStartingWith`, `findByStoreOrderIdOrderByOutboundIdAsc`, `findByStoreOrderIdInOrderByOutboundIdAsc`, `findByStoreOrderIdAndStatusForUpdate`, `existsByStoreOrderIdAndStatusIn`, `existsByStoreOrderIdAndStatusNot` |
| `OutboundLineJpaRepository` | `JpaRepository` | `OutboundLineJpaEntity` | `findByOutboundIdOrderByOutboundLineIdAsc`, `findByOutboundIdIn`, `existsLinkedToActiveOutbound` |
| `StockAllocationJpaRepository` | `JpaRepository` | `StockAllocationJpaEntity` | `findByIdForUpdate`, `findAllByIdForUpdate`, `findByStoreOrderId`, `findByStoreOrderIdAndStatusForUpdate`, `findUnlinkedAllocatedByStoreOrderIdForUpdate`, `existsByStoreOrderIdAndStatus` |
| `OutboundQueryJpaRepository` | `Repository` | (조회 전용) | `search`, `findView`, `findLineViews` |
| `StockAllocationQueryJpaRepository` | `Repository` | (조회 전용) | `search`, `findSummariesByIds`, `findView`, `findActiveOutboundIds`, `findFefoCandidates` |

## 발주 단위 조회

할당은 발주 항목 ID만 가지므로 발주 단위 조회는 모두 **서브쿼리**로 한다.

```
a.storeOrderLineId in (select l.storeOrderLineId from StoreOrderLineJpaEntity l where l.storeOrderId = :storeOrderId)
```

## 주요 쿼리

| 메서드 | 설명 |
|---|---|
| `findUnlinkedAllocatedByStoreOrderIdForUpdate` | 발주의 **ALLOCATED이면서** 출고 항목에 연결되지 않았거나 연결된 출고가 모두 CANCELED인 할당(`not exists`)을 락 조회. 출고 생성 때 묶을 대상 |
| `existsLinkedToActiveOutbound` | 취소(CANCELED)가 아닌 출고의 항목에 연결됐는지. 할당 해제 가드 |
| `OutboundQueryJpaRepository.search` | 출고 + 발주 + 지점, **창고는 발주의 창고**(left join), 처리자명(left join), 항목과 inner join해 `count(l)`. `Pageable`로 정렬·페이징하고 `group by` 쿼리라 `countQuery`를 따로 둔다. 정렬은 컨트롤러 `SortSpec`이 정한다(기본 생성 일시 내림차순) |
| `OutboundQueryJpaRepository.findLineViews` | 출고 항목 → 할당 → 발주 항목 → SKU, 재고 행 → 로트, 구역을 조인 |
| `StockAllocationQueryJpaRepository.search` | 할당 → 발주 항목 → 발주 → SKU → 재고 행 → 로트 → 구역. keyword는 발주 번호·SKU 코드·로트 번호. `Pageable`로 정렬·페이징하고 `countQuery`를 따로 둔다(기본 할당 일시 내림차순) |
| `StockAllocationQueryJpaRepository.findFefoCandidates` | **창고의 ACTIVE 구역**, 로트 AVAILABLE, 품질 AVAILABLE, 가용 > 0. `skuId`, 유통기한 빠른 순(없으면 뒤), 로트 생성 순, 재고 행 ID 순 |
| `StockAllocationQueryJpaRepository.findActiveOutboundIds` | 할당이 연결된, CANCELED가 아닌 출고 ID |
