# 영속성 어댑터 (PersistenceAdapter)

`storeorder/adapter/out/persistence/`

| 어댑터 | 구현 포트 | 설명 |
|---|---|---|
| `StoreOrderPersistenceAdapter` | `StoreOrderRepository` | 헤더(`StoreOrderJpaRepository`)와 항목(`StoreOrderLineJpaRepository`)을 함께 다룬다. `findLinesByIdsForUpdate`는 빈 컬렉션이면 쿼리 없이 빈 목록 |
| `StoreOrderQueryPersistenceAdapter` | `StoreOrderQueryRepository` | 조회 전용. 조건을 JPQL 파라미터로 변환 |

## 범위 변환

`StoreOrderQueryPersistenceAdapter.search`는 지점·창고 범위를 각각 변환한다.

| 파라미터 | 값 |
|---|---|
| `storeScoped` | `condition.storeIds() != null` |
| `storeIds` | `scopeIds(condition.storeIds())` |
| `warehouseScoped` | `condition.warehouseIds() != null` |
| `warehouseIds` | `scopeIds(condition.warehouseIds())` |

`scopeIds`는 null·빈 목록을 자리 값 `[-1]`로 바꾼다(`IN`에 빈 목록을 넘기지 않으려는 용도). 키워드는 `SearchKeyword.normalize`로 정리한다.
