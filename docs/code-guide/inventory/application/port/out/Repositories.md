# 아웃바운드 포트

`inventory/application/port/out/`

## InventoryLotRepository

재고 행(`InventoryLot`) 영속성. `ForUpdate` 메서드는 `SELECT ... FOR UPDATE`(비관적 쓰기 락)로 조회하며 **트랜잭션 안에서만** 호출한다. 수량을 바꾸는 흐름은 반드시 `ForUpdate`로 조회한 뒤 변경·저장한다.

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `existsById` | 기본 |
| `findByIdForUpdate(id)` | 단건 락 조회 |
| `findAllByIdsForUpdate(ids)` | 여러 행을 **inventory_lot_id 오름차순**으로 잠근다(교착 방지). 없는 ID는 결과에서 빠진다 |
| `findBySectionIdAndLotIdForUpdate(sectionId, lotId)` | 유니크 키(구역, 로트)로 락 조회. 입고 반영 시 기존 행 찾기 |
| `findAllByLotIdForUpdate(lotId)` | 한 로트의 모든 재고 행을 ID 오름차순으로 잠근다. 로트 상태 변경 전 수량 확인용 |

## InventoryQueryRepository

재고 조회 전용. 재고 테이블에 SKU·구역·창고를 ID로 조인해 **필터·집계·정렬을 DB에서 한 번에** 처리한다. 다른 도메인 테이블은 읽기에만 쓴다. 목록 조회(SKU 집계·구역 집계·로트별 재고·이력·로트)는 `PageCondition`을 받아 `PageResult`를 돌려주고, 정렬은 페이지 조건을 따른다. 페이지 조건 없는 `findLotViews`·`findTransactions`는 전체를 읽는 용도(로트 상세, 재고별 이력)다.

| 메서드 | 반환 | 정렬·설명 |
|---|---|---|
| `findSkuSummaries(condition, page)` | `PageResult<InventorySkuSummary>` | 기본 SKU 코드 오름차순 |
| `findSectionSummaries(condition, page)` | `PageResult<InventorySectionSummary>` | 기본 창고, 구역 코드, SKU 코드 오름차순. 보유 0 행 제외 |
| `findLotViews(condition, page)` | `PageResult<InventoryLotView>` | 기본 유통기한 오름차순(없으면 뒤로), 재고 ID |
| `findLotViews(condition)` | `InventoryLotView` 목록 | 페이지 없이 전체. 유통기한 오름차순(없으면 뒤로), 재고 ID |
| `findDetail(inventoryLotId)` | `Optional<InventoryDetail>` | |
| `findLowStock(condition)` | `LowStockItem` 목록 | 부족 수량 내림차순. 안전 재고 0·비활성 SKU 제외, 재고 없는 SKU는 가용 0으로 포함 |
| `findTransactions(condition, page)` | `PageResult<InventoryTransactionView>` | 기본 생성 일시 내림차순 |
| `findTransactions(condition)` | `InventoryTransactionView` 목록 | 페이지 없이 전체. 생성 일시 내림차순, 이력 ID 내림차순 |
| `findLots(condition, page)` | `PageResult<LotSummary>` | 기본 유통기한 오름차순(없으면 뒤로), 로트 ID |
| `findLot(lotId)` | `Optional<LotSummary>` | 범위 없음 |
| `findLot(lotId, warehouseIds)` | `Optional<LotSummary>` | 로트가 `warehouseIds` 중 하나에 재고 또는 입고 완료 이력이 있을 때만 반환 |
| `findLotInbounds(lotId)` | `LotInboundView` 목록 | 입고 완료 건만. 완료 일시 내림차순 |
| `findAllocatableStocks(warehouseId, skuId)` | `InventoryLotView` 목록 | **FEFO**: 품질·로트 AVAILABLE, 활성 구역, 가용 > 0. 유통기한 빠른 순 → 로트 생성 순 → 재고 ID 순 |
| `existsStockInSection(sectionId)` / `existsStockInWarehouse(warehouseId)` | boolean | 보유 또는 할당 수량이 0보다 큰 행이 있는지 |
| `existsSku`, `existsWarehouse`, `existsSection`, `existsSupplier` | boolean | **필터 ID 존재 검증**(없는 ID로 필터링하면 404). 서비스를 거치지 않고 읽기 쿼리로 확인 |
| `findWarehouseIdOfSection(sectionId)` | `Optional<Long>` | 구역이 속한 창고. 구역이 없으면 비어 있다 |

## InventoryTransactionRepository

| 메서드 | 설명 |
|---|---|
| `save(transaction)` | 이력은 **추가만** 하고 수정·삭제하지 않는다 |

## LotRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `existsById` | 기본 |
| `findByIdForUpdate(lotId)` | 락 조회. 상태를 바꾸는 흐름에서만 |
| `findBySkuIdAndSupplierIdAndLotNumber(...)` | 유니크 키 조회 (로트 find-or-create) |

## SectionCapacityPort

창고 도메인의 구역 사용 용량 증감. 보유 수량이 바뀐 만큼 **같은 트랜잭션**에서 호출한다. 구현: `adapter/out/warehouse/SectionCapacityAdapter`.

| 메서드 | 설명 |
|---|---|
| `lock(sectionIds)` | 구역 행을 section_id 오름차순으로 잠근다. **재고 행을 잠그기 전에** 호출 |
| `occupy(sectionId, quantity)` | 수용량을 넘으면 `SECTION_CAPACITY_EXCEEDED` |
| `vacate(sectionId, quantity)` | 사용 용량 감소 |

## SkuStatusPort

상품 도메인의 SKU 상태 확인. 구현: `adapter/out/product/SkuStatusAdapter`.

| 메서드 | 설명 |
|---|---|
| `requireActive(skuId)` | 비활성이면 `SKU_NOT_ACTIVE`. 없는 SKU는 상품 도메인의 `SKU_NOT_FOUND` |
