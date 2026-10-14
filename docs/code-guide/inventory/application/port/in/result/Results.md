# 조회 결과 (Result)

`inventory/application/port/in/result/` · 조회 쿼리가 **JPQL 생성자 표현식**(`select new ...`)으로 직접 만드는 읽기 전용 `record`가 대부분이다. 다른 도메인 테이블(SKU·구역·창고·공급처·사용자)을 ID로 조인해 채운다(ADR-007).

| 클래스 | 용도 | 엔드포인트 | 만드는 쿼리 | 쓰는 곳 |
|---|---|---|---|---|
| `InventorySkuSummary` | SKU 기준 재고 집계 | GET `/inventory` | `findSkuSummaries` | `InventorySkuSummaryResponse` |
| `InventoryLotView` | 로트·구역 단위 재고 행 | GET `/inventory/by-lot`, 로트 상세의 구역별 분포, 할당 후보 | `findLotViews`, `findAllocatableStocks` | `InventoryLotViewResponse`, `LotDetailResponse.InventoryItem`, 출고 할당 |
| `InventoryDetail` | 재고 한 건 상세 | GET `/inventory/{id}` | `findDetail` | `InventoryDetailResponse` |
| `LowStockItem` | 안전 재고 미만 SKU | GET `/inventory/low-stock` | `findLowStock` | `LowStockItemResponse` |
| `InventoryTransactionView` | 변동 이력 행 | GET `/inventory/transactions`, `/{id}/transactions` | `findViews` | `InventoryTransactionResponse` |
| `LotSummary` | 로트 마스터 + SKU + 공급처명 (수량 없음) | GET `/lots`, `/lots/{id}` | `findSummaries` | `LotSummaryResponse`, `LotDetailResponse` |
| `LotInboundView` | 로트가 입고된 이력 한 건 | GET `/lots/{id}`의 `inbounds[]` | `findInbounds` | `LotDetailResponse.InboundItem` |
| `InventoryAdjustmentResult` | 재고 조정 결과(이력 + 조정 후 재고 행). 도메인 객체를 담는다 | POST `/inventory/adjustments` | 서비스가 조립 | `InventoryAdjustmentResponse` |

## 필드 설명

### InventorySkuSummary

| 필드 | 설명 |
|---|---|
| totalQuantity | 보유 수량(on_hand) 합계 |
| availableQuantity | 품질·로트 상태가 모두 AVAILABLE인 행의 `on_hand - allocated` 합계 |
| allocatedQuantity | 할당 수량 합계 |
| defectiveQuantity | 불량(DEFECTIVE) 재고의 보유 수량 합계 |

### InventoryLotView

`availableQuantity`는 품질·로트 상태가 모두 AVAILABLE이면 `on_hand - allocated`, 아니면 **0**이다. 나머지 필드: inventoryLotId, lotId, lotNumber, skuId, skuCode, skuName, warehouseId, sectionId, sectionCode, sectionName, onHandQuantity, allocatedQuantity, qualityStatus, expiryDate, lastCountedAt.

### InventoryDetail

`InventoryLotView`의 정보에 창고명, SKU 단위, 공급처(id·이름), 제조일, 로트 상태, 원가, 생성·수정 시각을 더한 상세. 공급처명은 `left join`이라 공급처가 없으면 null일 수 있다.

### LowStockItem

| 필드 | 설명 |
|---|---|
| safetyStockQuantity | SKU의 안전 재고 |
| availableQuantity | 가용 수량 합계(재고가 없으면 0) |
| shortageQuantity | `safetyStockQuantity - availableQuantity` |

JPQL이 쓰는 6인자 보조 생성자가 부족 수량을 계산한다. 대상: **활성 SKU 중 안전 재고가 0보다 큰 것**. 재고가 없는 SKU도 가용 0으로 포함된다.

### InventoryTransactionView

처리자 이름(`createdByName`)은 사용자 테이블을 `left join`으로 읽어 채운다.

### LotInboundView

입고 완료(COMPLETED) 입고만 담는다. `receivedAt`은 입고 완료 일시다.
