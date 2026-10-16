# Spring Data JPA 리포지토리

`storeorder/adapter/out/persistence/repository/`

| 리포지토리 | 엔티티 | 직접 정의한 메서드 |
|---|---|---|
| `StoreOrderJpaRepository` | `StoreOrderJpaEntity` | `existsByOrderNo`, `countByOrderNoStartingWith`, `existsByStoreIdAndStatusIn`, `findByIdForUpdate`, `search`, `findView` |
| `StoreOrderLineJpaRepository` | `StoreOrderLineJpaEntity` | `findByStoreOrderIdOrderByStoreOrderLineIdAsc`, `findByStoreOrderIdForUpdate`, `findByIdsForUpdate`, `findLineViews` |

## 주요 쿼리

| 메서드 | 설명 |
|---|---|
| `StoreOrderJpaRepository.search` | 지점은 inner join, **창고는 left join**(배정 전 null), 항목은 inner join(발주는 항목이 1개 이상). `count(l)`로 항목 수, `sum(requestedQuantity * requestedUnitSupplyPrice)`로 금액, `sum(case when shipped < requested then 1 else 0 end)`로 부족 항목 수를 집계. keyword는 주문 번호·지점명. 지점·창고 범위 조건 각각. `Pageable`로 정렬·페이징하고 `countQuery`는 `count(distinct 발주 ID)`(`group by` 쿼리는 건수 쿼리를 만들 수 없음). 기본 요청 일시 내림차순 |
| `StoreOrderJpaRepository.findView` | 위와 같은 조인에 작성자명(`left join` 사용자) 추가, 한 건 |
| `StoreOrderJpaRepository.findByIdForUpdate` | `PESSIMISTIC_WRITE` |
| `StoreOrderLineJpaRepository.findByStoreOrderIdForUpdate` | 발주의 항목 전체를 ID 오름차순으로 잠금 |
| `StoreOrderLineJpaRepository.findByIdsForUpdate` | 항목 ID 목록을 ID 오름차순으로 잠금 |
| `StoreOrderLineJpaRepository.findLineViews` | `product_sku`를 ID로 조인해 SKU 코드·이름·단위를 붙임. SKU 코드순 |

**금액은 저장하지 않고** 요청 수량 × 공급 단가로 계산한다.
