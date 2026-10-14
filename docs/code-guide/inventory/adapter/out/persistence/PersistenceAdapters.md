# 영속성 어댑터 (PersistenceAdapter)

`inventory/adapter/out/persistence/`

| 어댑터 | 구현 포트 | 설명 |
|---|---|---|
| `InventoryLotPersistenceAdapter` | `InventoryLotRepository` | 락 조회 메서드를 `@Lock(PESSIMISTIC_WRITE)` 쿼리에 연결. `findAllByIdsForUpdate`는 입력이 비어 있으면 쿼리 없이 빈 목록 |
| `InventoryTransactionPersistenceAdapter` | `InventoryTransactionRepository` | 저장만 한다 |
| `LotPersistenceAdapter` | `LotRepository` | 락 조회, 유니크 키 조회 |
| `InventoryQueryPersistenceAdapter` | `InventoryQueryRepository` | 조회 전용. 조건 객체를 JPQL 파라미터로 바꿔 `InventoryLotJpaRepository`, `InventoryTransactionJpaRepository`, `LotJpaRepository`에 위임 |

## InventoryQueryPersistenceAdapter의 범위 변환

| 헬퍼 | 설명 |
|---|---|
| `scoped(warehouseIds)` | `warehouseIds != null`이면 true (범위 조건 사용) |
| `scopeIds(warehouseIds)` | null이거나 비어 있으면 `NO_WAREHOUSE = [-1]`, 아니면 그대로. JPQL `IN`에 빈 목록을 넘기지 않으려는 자리 값 |
| `keyword(value)` | 공백뿐인 검색어는 null(조건 없음), 아니면 `trim` |

범위 조건을 쓰지 않을 때(`scoped=false`)나 담당 창고가 없을 때(빈 목록 → `[-1]` → 아무것도 매칭 안 됨)에도 쿼리가 깨지지 않는다.
