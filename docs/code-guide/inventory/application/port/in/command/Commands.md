# Command

`inventory/application/port/in/command/`

사용자 요청에서 만들어지는 Command(`InventoryAdjustCommand`, `LotStatusChangeCommand`)와 **다른 도메인이 넘기는 Command**(`LotRegisterCommand`, `Stock...Command`)가 섞여 있다.

## InventoryAdjustCommand

| 항목 | 내용 |
|---|---|
| 용도 | 재고 보유 수량을 조정한다 |
| 엔드포인트 | POST `/api/v1/inventory/adjustments` |
| 만드는 곳 | `InventoryAdjustRequest.toCommand(userId)` (처리자는 토큰 사용자) |
| 쓰는 곳 | `InventoryAdjustmentService.adjust` |
| 필드 | inventoryLotId, beforeQuantity(화면에서 본 현재 수량, 서버 값과 다르면 `STALE_QUANTITY`), afterQuantity(0 이상, 할당 수량 이상), reason, userId |

## LotStatusChangeCommand

| 항목 | 내용 |
|---|---|
| 용도 | 로트 상태를 격리·해제·폐기로 바꾼다 |
| 엔드포인트 | PATCH `/api/v1/lots/{lotId}/status` |
| 만드는 곳 | `LotStatusChangeRequest.toCommand(userId)` |
| 쓰는 곳 | `LotService.changeLotStatus(lotId, command)` |
| 필드 | status(AVAILABLE·QUARANTINED·DISPOSED), reason(필수, 500자 이하, 상태 이력에 기록), userId(상태 이력 `changed_by`) |

## LotRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 입고 검수 시 로트를 찾거나 만든다(find-or-create, ADR-004) |
| 엔드포인트 | 없음 (입고 검수 트랜잭션 내부) |
| 만드는 곳 | 입고 도메인 `LotAdapter` |
| 쓰는 곳 | `LotService.findOrRegister` |
| 필드 | skuId, supplierId, lotNumber, manufacturedDate, expiryDate, unitCost |
| 쓰임 | SKU + 공급처 + 로트 번호가 같으면 기존 로트를 재사용 |

## StockReceiveCommand

| 항목 | 내용 |
|---|---|
| 용도 | 입고 완료 시 한 재고 행(구역 + 로트)에 보유 수량을 더한다. 행이 없으면 만든다 |
| 만드는 곳 | 입고 도메인 `InboundStockAdapter` |
| 쓰는 곳 | `InventoryStockService.receive` |
| 필드 | sectionId, lotId, qualityStatus(합격품 AVAILABLE, 불량품 DEFECTIVE), quantity, inboundId(이력 `reference_id`), userId |

## StockQuantityCommand

| 항목 | 내용 |
|---|---|
| 용도 | 재고 행 하나와 수량. 할당·할당 해제에 쓴다 |
| 만드는 곳 | 출고 도메인 `OutboundStockAdapter` |
| 쓰는 곳 | `InventoryStockService.allocate`, `release` |
| 필드 | inventoryLotId, quantity |

## StockShipCommand

| 항목 | 내용 |
|---|---|
| 용도 | 피킹 완료 시 한 재고 행의 출고 반영 |
| 만드는 곳 | 출고 도메인 `OutboundStockAdapter` |
| 쓰는 곳 | `InventoryStockService.ship` |
| 필드 | inventoryLotId, allocatedQuantity(할당 전체), pickedQuantity, outboundId(이력 `reference_id`), userId |
| 쓰임 | 보유 수량은 `pickedQuantity`만큼, 할당 수량은 `allocatedQuantity`만큼 줄인다. 부족분(`allocated - picked`)의 예약은 풀려 다시 가용 재고가 된다 |
