# 아웃바운드 포트

`inbound/application/port/out/`

## 영속성

### InboundRepository

입고 헤더 + 검수 항목. 한 트랜잭션에서 함께 저장한다.

| 메서드 | 설명 |
|---|---|
| `save`, `findById` | 헤더 |
| `findByIdForUpdate(id)` | 비관적 락. 취소·완료·검수가 동시에 와도 한쪽만 성공 |
| `saveLines(lines)`, `findLinesByInboundId(id)` | 검수 항목 (ID 오름차순). `inboundId`는 호출 측이 채운다 |
| `deleteLinesByInboundId(id)` | 검수 호출마다 기존 항목을 통째로 교체하려고 모두 삭제 |
| `existsByInboundNo`, `countByInboundNoPrefix(prefix)` | 입고 번호 채번 |
| `existsInProgressByPurchaseOrderId(id)` | 발주에 ARRIVED·INSPECTING 입고가 있는지 (`INBOUND_IN_PROGRESS` 기준) |
| `existsNotCanceledByPurchaseOrderId(id)` | 발주에 취소되지 않은 입고가 있는지 (`PURCHASE_ORDER_HAS_INBOUND` 기준) |

### PurchaseOrderRepository

발주 헤더 + 항목.

| 메서드 | 설명 |
|---|---|
| `save`, `saveLines`, `findById`, `findLineById`, `findLinesByPurchaseOrderId` | 기본 |
| `findByIdForUpdate(id)` | 발주 상태를 기준으로 판단하는 흐름(입고 등록·발주 취소·입고 완료)이 어긋나지 않게 락 |
| `findLinesByPurchaseOrderIdForUpdate(id)` | 항목 락 조회(ID 오름차순). 입고 완료에서 수량 누적 시 |
| `existsByPurchaseOrderNo`, `countByPurchaseOrderNoPrefix(prefix)` | 발주 번호 채번 |
| `existsInProgressBySupplierId(id)` | 공급처에 REQUESTED·CONFIRMED 발주가 있는지 (공급처 비활성화 가드) |

### SupplierRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `existsById`, `existsBySupplierCode` | 기본 |
| `search(condition)` | 공급처명 오름차순 |

## 조회 전용

### InboundQueryRepository

발주·공급처·창고·SKU·로트·구역 정보를 ID로 조인한 조회 결과(ADR-007).

| 메서드 | 설명 |
|---|---|
| `search(condition)` | 도착 일시 최신순. **검수 항목이 없는 입고도 포함** |
| `findView(inboundId)` | 입고 헤더 뷰 |
| `findLineViews(inboundId)` | 검수 항목 뷰 (SKU 코드·로트 번호 순) |
| `findAssignableSections(warehouseId, condition)` | 활성, **DEFECT가 아님**, 가용 용량 > 0 (구역 코드순) |
| `findDefectSections(warehouseId, condition)` | 활성, **DEFECT**, 가용 용량 > 0 |

### PurchaseOrderQueryRepository

| 메서드 | 설명 |
|---|---|
| `search(condition)` | 등록 일시 내림차순. 항목 수·금액 합계를 DB에서 집계 |
| `findView(id)` | 헤더 뷰 |
| `findLineViews(id)` | 항목 뷰 (SKU 코드 오름차순) |

## 다른 도메인 연결 포트

| 포트 | 메서드 | 연결 대상 | 구현 |
|---|---|---|---|
| `InboundStockPort` | `receive(inboundId, userId, receipts)` → `ReceivedStock` 목록 | 재고 `InventoryStockUseCase.receive` | `InboundStockAdapter` |
| `LotPort` | `findOrRegisterLot(...)` → 로트 ID | 재고 `LotUseCase.findOrRegister` | `LotAdapter` |
| `InboundSectionPort` | `getSection(sectionId)` → `SectionInfo`(sectionId, warehouseId, `SectionKind`, active, `isDefect()`) | 창고 `WarehouseSectionUseCase.getSectionView` | `InboundSectionAdapter` |
| `WarehouseAvailabilityPort` | `requireExists(id)`, `requireActive(id)` | 창고 `WarehouseUseCase.getWarehouseView` | `WarehouseAvailabilityAdapter` |
| `SkuPurchasePricePort` | `getPurchasablePrice(skuId)` | 상품 `ProductSkuUseCase.getSkuView` | `SkuPurchasePriceAdapter` |
