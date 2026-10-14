# 응답 DTO (Response)

`inventory/adapter/in/web/dto/response/` · 응답 `record`. 대부분 `from(XxxView)`로 Result를 필드 그대로 옮긴다.

## 재고

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `InventorySkuSummaryResponse` | SKU 기준 집계 항목 | GET `/inventory` | `from(InventorySkuSummary)` | skuId, skuCode, skuName, unit, totalQuantity, availableQuantity, allocatedQuantity, defectiveQuantity |
| `InventoryLotViewResponse` | 로트·구역 단위 항목 | GET `/inventory/by-lot` | `from(InventoryLotView)` | inventoryLotId, lotId, lotNumber, skuId, skuCode, skuName, warehouseId, sectionId, sectionCode, sectionName, onHandQuantity, allocatedQuantity, availableQuantity, qualityStatus, expiryDate, lastCountedAt |
| `InventoryDetailResponse` | 재고 한 건 상세 | GET `/inventory/{id}` | `from(InventoryDetail)` | 위 항목 + warehouseName, unit, supplierId·supplierName, manufacturedDate, lotStatus, unitCost, createdAt, updatedAt |
| `LowStockItemResponse` | 안전 재고 미만 항목 | GET `/inventory/low-stock` | `from(LowStockItem)` | skuId, skuCode, skuName, unit, safetyStockQuantity, availableQuantity, shortageQuantity |

## 이력·조정

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `InventoryTransactionResponse` | 변동 이력 항목 | GET `/inventory/transactions`, `/{id}/transactions` | `from(InventoryTransactionView)` | transactionId, inventoryLotId, warehouseId, sectionId, sectionCode, skuId, skuCode, lotId, lotNumber, transactionType, quantityDelta, beforeQuantity, afterQuantity, referenceType, referenceId, reason, createdBy, createdByName, createdAt |
| `InventoryTransactionHistoryResponse` | 한 재고 행의 이력 래퍼 | GET `/inventory/{id}/transactions` | `of(inventoryLotId, items)` | inventoryLotId, items. 페이지 정보는 없다 |
| `InventoryAdjustmentResponse` | 조정 결과 | POST `/inventory/adjustments` | `from(InventoryAdjustmentResult)` | transactionId, inventoryLotId, transactionType, quantityDelta, beforeQuantity, afterQuantity, reason, createdBy, createdAt, `inventory`(onHandQuantity, allocatedQuantity, availableQuantity) |

`InventoryAdjustmentResponse.inventory`는 조정 후 재고 스냅샷이며 `availableQuantity`는 `inventoryLot.availableQuantity()`로 계산한다(품질·로트 상태를 반영하지 않은 단순 `on_hand - allocated`).

## 로트

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `LotSummaryResponse` | 로트 목록·상태 변경 결과 | GET `/lots`, PATCH `/lots/{id}/status` | `from(LotSummary)` | lotId, lotNumber, skuId, skuCode, skuName, supplierId, supplierName, manufacturedDate, expiryDate, status, unitCost |
| `LotDetailResponse` | 로트 상세 | GET `/lots/{id}` | `of(summary, inventory, inbounds)` | 로트 요약 + `inventory`(구역별 재고), `inbounds`(입고 이력), createdAt, updatedAt |

### LotDetailResponse 내부 record

| record | 필드 |
|---|---|
| `InventoryItem` | inventoryLotId, warehouseId, warehouseName, sectionId, sectionCode, sectionName, onHandQuantity, allocatedQuantity, qualityStatus |
| `InboundItem` | inboundId, inboundNo, warehouseId, receivedAt, receivedQuantity, acceptedQuantity, defectiveQuantity, receivedUnitPrice |

`inbounds`는 InboundLine의 `lot_id`가 이 로트인 입고 완료 건이며 없으면 빈 배열이다.
