# 조회 결과 (Result)

`outbound/application/port/in/result/` · 발주·지점·창고·SKU·로트·구역·사용자 정보는 **ID 기준 읽기 전용 조인**으로 채운다(ADR-007).

## 조회 쿼리가 만드는 뷰

| 클래스 | 용도 | 엔드포인트 | 설명 |
|---|---|---|---|
| `StockAllocationSummary` | 할당 한 줄 | GET `/allocations`, 할당 생성 응답의 `items` | 할당 정보 + 발주·발주 항목·창고·SKU·재고 행·로트(번호·유통기한)·구역 |
| `StockAllocationView` | 할당 상세 | GET `/allocations/{id}` | Summary에 지점명·요청 수량·할당 처리자명·구역명을 더함 |
| `OutboundSummary` | 출고 목록 한 줄 | GET `/outbounds` | 발주·지점·**발주에 배정된 창고**·상태·`lineCount`·배송 시작 정보·처리자명 |
| `OutboundView` | 출고 헤더 | GET `/outbounds/{id}/details` | 목록 필드 + 배송 완료 일시, 비고, 수정 일시 |
| `OutboundLineView` | 출고 항목 | details의 `items`, 생성 응답의 `items` | 할당·발주 항목·SKU·재고 행·로트·구역·할당 수량·출고 수량·확정 단가. `lineAmount()`(출고 수량 × 확정 단가, **피킹 전 null**) |
| `FefoStockCandidate` | 할당 후보 재고 행 | (내부) | skuId, inventoryLotId, sectionId, lotId, availableQuantity(보유 - 할당, 양수). **조회 순서가 곧 할당 우선순위** |

## 서비스가 조립하는 결과

| 클래스 | 용도 | 구성 |
|---|---|---|
| `StockAllocateResult` | 할당 결과 | 발주 ID·번호, `items`(만들어진 할당), `shortages`(부족한 SKU, 없으면 빈 목록) |
| `StockAllocationShortage` | 부족한 SKU 한 건 | skuId, requestedQuantity(이번에 할당하려던 잔여 수량), allocatedQuantity(실제 할당), shortageQuantity |
| `StockAllocationDetail` | 할당 상세 | `view` + `outboundId`(취소되지 않은 출고에 연결된 경우만) |
| `StockAllocationReleaseResult` | 해제 결과 | 할당 정보 + 해제 뒤 재고 행의 보유·할당·가용 수량 |
| `OutboundCreateResult` | 출고 생성 결과 | `view` + `items`. `lineCount()` = 항목 수 |
| `OutboundDetail` | 출고 상세 | `view` + `items` + `cancelReason`(취소된 출고만) |
| `OutboundPickingStartResult` | 피킹 시작 | outboundId, outboundNo, status, updatedAt |
| `OutboundPickingCompleteResult` | 피킹 완료 | 상태, `hasShortage`, `items`(`PickedItem`: 할당·피킹·확정 단가·`InventoryState`) |
| `OutboundShipResult` | 배송 시작 | 상태, shippedAt, shippedBy·Name |
| `OutboundDeliverResult` | 배송 완료 | 상태, deliveredAt, 발주 ID, **발주 상태**(전량이면 COMPLETED, 아니면 ASSIGNED) |
| `OutboundCancelResult` | 취소 | 상태, cancelReason |

### OutboundPickingCompleteResult.PickedItem

| 메서드 | 설명 |
|---|---|
| `shortageQuantity()` | `allocated - picked` |
| `lineAmount()` | `확정 단가 × 피킹 수량` |

`InventoryState`(inventoryLotId, onHandQuantity, allocatedQuantity)는 **차감이 반영된 뒤** 재고 행의 수량이다.
