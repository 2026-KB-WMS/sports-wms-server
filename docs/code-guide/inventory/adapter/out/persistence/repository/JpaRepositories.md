# Spring Data JPA 리포지토리

`inventory/adapter/out/persistence/repository/`

| 리포지토리 | 엔티티 | 메서드 요약 |
|---|---|---|
| `InventoryLotJpaRepository` | `InventoryLotJpaEntity` | 락 조회 4종, 존재 확인 5종, 재고 있음 2종, 집계·조회 5종 |
| `InventoryTransactionJpaRepository` | `InventoryTransactionJpaEntity` | `findViews` |
| `LotJpaRepository` | `LotJpaEntity` | `findBySkuIdAndSupplierIdAndLotNumber`, `findByIdForUpdate`, `findSummaries`, `findInbounds` |

## 가용 수량 기준

여러 쿼리가 공통으로 쓴다: **재고 품질 상태와 로트 상태가 모두 AVAILABLE**인 행의 `on_hand - allocated`. 아니면 0.

## InventoryLotJpaRepository

### 락 조회

| 메서드 | 설명 |
|---|---|
| `findByIdForUpdate` | 단건 |
| `findAllByIdInForUpdate` | `inventoryLotId in :ids`, **ID 오름차순** |
| `findBySectionIdAndLotIdForUpdate` | (구역, 로트) |
| `findAllByLotIdForUpdate` | 로트의 모든 행, ID 오름차순 |

### 조회 전용 (다른 도메인 테이블을 ID 조인으로 읽기만)

| 메서드 | 설명 |
|---|---|
| `existsSkuId`, `existsWarehouseId`, `existsSectionId`, `existsSupplierId` | 필터 ID 존재 확인 |
| `findWarehouseIdBySectionId` | 구역의 창고 |
| `existsStockInSection`, `existsStockInWarehouse` | 보유 또는 할당 수량 > 0인 행이 있는지 |
| `findSkuSummaries` | SKU 단위 `group by` 집계 (`select new InventorySkuSummary`). 로트·SKU·구역 조인, 창고·키워드 조건. `Pageable`로 정렬·페이징하고 `group by` 쿼리라 `countQuery`(SKU 수)를 따로 둔다 |
| `findLotViews` | 로트·구역 행. `includeEmpty=false`면 `onHandQuantity > 0`만. `Pageable`로 정렬·페이징한다(유통기한 정렬은 `coalesce(l.expiryDate, 먼 미래)` 식) |
| `findDetail` | 창고·SKU·로트·구역·공급처(`left join`) 조인 상세 |
| `findLowStock` | **SKU 기준 left join**. 창고·범위 필터를 가용 합계 `CASE` 안에 넣어, 다른 창고에만 재고가 있어도 가용 0으로 남긴다. 활성 SKU 중 안전 재고 > 0, 가용 < 안전 재고인 것만(`having`) |
| `findAllocatableStocks` | 출고 할당 후보. 품질·로트 AVAILABLE, **구역이 ACTIVE**, `on_hand > allocated`. FEFO 정렬 |

## InventoryTransactionJpaRepository

`findViews`: 이력 + 재고 행 + 로트 + SKU + 구역을 조인하고 처리자 이름을 `left join`으로 붙인다. 모든 조건이 `(:param is null or ...)`이고 `createdFrom <=`, `createdTo >=` 범위를 쓴다. 정렬은 생성 일시, 이력 ID 내림차순.

## LotJpaRepository

| 메서드 | 설명 |
|---|---|
| `findSummaries` | 로트 + SKU + 공급처명. `lotId`가 있으면 단건 조회에도 쓴다. `scoped=true`면 **담당 창고에 재고 행이 있거나 입고 완료(COMPLETED) 이력이 있는** 로트만 (`exists` 서브쿼리 두 개) |
| `findInbounds` | `InboundLine.lotId`가 이 로트이고 입고가 COMPLETED인 건. 완료 일시 내림차순 |
