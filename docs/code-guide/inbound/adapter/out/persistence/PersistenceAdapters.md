# 영속성 어댑터 (PersistenceAdapter)

`inbound/adapter/out/persistence/`

| 어댑터 | 구현 포트 | 설명 |
|---|---|---|
| `InboundPersistenceAdapter` | `InboundRepository` | 헤더(`InboundJpaRepository`)와 항목(`InboundLineJpaRepository`)을 함께 다룬다. `deleteLinesByInboundId`는 벌크 삭제 |
| `InboundQueryPersistenceAdapter` | `InboundQueryRepository` | 조회 전용. 조건을 JPQL 파라미터로 바꾼다 |
| `PurchaseOrderPersistenceAdapter` | `PurchaseOrderRepository` | 헤더·항목 |
| `PurchaseOrderQueryPersistenceAdapter` | `PurchaseOrderQueryRepository` | 조회 전용 |
| `SupplierPersistenceAdapter` | `SupplierRepository` | 공급처 |

## 범위 변환

`InboundQueryPersistenceAdapter`, `PurchaseOrderQueryPersistenceAdapter`는 각각 `NO_WAREHOUSE = [-1]`과 `scopeIds(...)`를 갖는다(재고 도메인과 같은 패턴: 빈 목록을 `IN`에 넘기지 않으려는 자리 값). `scoped`는 `warehouseIds != null`로 계산한다.

## 검색 키워드

`SearchKeyword.normalize(keyword)`로 정리한 뒤 JPQL에 넘긴다.
