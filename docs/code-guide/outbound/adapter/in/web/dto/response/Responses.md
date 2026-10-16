# 응답 DTO (Response)

`outbound/adapter/in/web/dto/response/` · 응답 `record`. 목록은 `PageResponse`(`data.items` + `data.pageInfo`, 페이지네이션 적용).

## 재고 할당

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `StockAllocationItemResponse` | 할당 한 건 (생성 응답의 `items`와 목록 항목 공용) | POST·GET `/allocations` | `from(StockAllocationSummary)` | allocationId, 발주(id·번호·항목 id), warehouseId, SKU(id·코드·이름), inventoryLotId, 로트(id·번호·유통기한), 구역(id·코드), allocatedQuantity, pickedQuantity, status, allocatedAt, releasedAt |
| `StockAllocateResponse` | 할당 결과 | POST `/allocations` | `from(StockAllocateResult)` | storeOrderId, orderNo, `items`, **`shortages`** |
| `StockAllocationShortageResponse` | 재고가 모자라 일부만 할당한 SKU | 위 응답의 `shortages` | `from(StockAllocationShortage)` | skuId, requestedQuantity, allocatedQuantity, shortageQuantity |
| `StockAllocationDetailResponse` | 할당 상세 | GET `/allocations/{id}` | `from(StockAllocationDetail)` | 목록 필드 + 처리자(id·이름), 지점(id·이름), 요청 수량, 구역명, **outboundId**(취소되지 않은 출고에 연결된 경우만) |
| `StockAllocationReleaseResponse` | 해제 결과 | PATCH `.../release` | `from(StockAllocationReleaseResult)` | allocationId, status, allocatedQuantity, releasedAt, `inventory`(inventoryLotId, onHandQuantity, allocatedQuantity, availableQuantity) |

## 출고

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `OutboundCreateResponse` | 생성된 출고 | POST `/outbounds` | `from(OutboundCreateResult)` | outboundId, outboundNo, 발주(id·번호), status, note, lineCount, `items`(outboundLineId, allocationId, skuId·코드, 로트 번호, 구역 코드, allocatedQuantity, shippedQuantity), createdAt |
| `OutboundListItemResponse` | 목록 항목 | GET `/outbounds` | `from(OutboundSummary)` | 출고 번호, 발주·지점·창고(id·이름), status, lineCount, shippedAt, shippedBy·Name, createdAt |
| `OutboundDetailResponse` | 상세 | GET `/outbounds/{id}/details` | `from(OutboundDetail)` | 목록 필드 + deliveredAt, note, **cancelReason**, `items`(할당·발주 항목·SKU·재고 행·로트·구역·수량·`confirmedUnitSupplyPrice`·`lineAmount`), createdAt, updatedAt |
| `OutboundPickingStartResponse` | 피킹 시작 | PATCH `.../picking/start` | `from(...)` | outboundId, outboundNo, status, updatedAt |
| `OutboundPickingCompleteResponse` | 피킹 완료 | PATCH `.../picking/complete` | `from(...)` | outboundId, outboundNo, status, **hasShortage**, `items`(outboundLineId, allocationId, allocated·picked·**shortage**Quantity, 확정 단가, 금액, `inventory`: inventoryLotId·onHandQuantity·allocatedQuantity), updatedAt |
| `OutboundShipResponse` | 배송 시작 | PATCH `.../ship` | `from(...)` | status, shippedAt, shippedBy·Name, updatedAt |
| `OutboundDeliverResponse` | 배송 완료 | PATCH `.../deliver` | `from(...)` | status, deliveredAt, **`storeOrder`**(storeOrderId, status), updatedAt |
| `OutboundCancelResponse` | 취소 | PATCH `.../cancel` | `from(...)` | status, cancelReason, updatedAt |

## 참고

- **확정 공급 단가·금액은 피킹 완료 전에는 null**이다. 이 API는 HQ_ADMIN·WAREHOUSE_MANAGER만 호출하므로 점주용 마스킹은 필요 없다.
- 배송 완료 응답의 `storeOrder.status`로 클라이언트가 발주가 전량 출고돼 COMPLETED가 됐는지 알 수 있다.
