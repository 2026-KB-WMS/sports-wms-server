# Command

`outbound/application/port/in/command/` · 처리자는 요청 바디가 아니라 **토큰 사용자**로, `userId`에 채워진다.

## StockAllocateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 발주의 잔여 수량을 FEFO로 재고에 예약한다 |
| 엔드포인트 | POST `/api/v1/allocations` |
| 만드는 곳 | `StockAllocateRequest.toCommand(userId)` |
| 쓰는 곳 | `StockAllocationService.allocate` |
| 필드 | storeOrderId, userId, skuIds |
| `skuIds` | **null이면 발주 전체**, 지정하면 그 SKU만, **빈 목록이면 거절**(400) |
| 생성자 | `(storeOrderId, userId)` = 발주 전체 |
| 주의 | 대상 창고는 요청이 아니라 **발주에 배정된 창고**다 |

## StockAllocationReleaseCommand

| 항목 | 내용 |
|---|---|
| 용도 | ALLOCATED 할당을 해제한다 |
| 엔드포인트 | PATCH `/api/v1/allocations/{allocationId}/release` |
| 만드는 곳 | 컨트롤러가 직접 생성 (`OutboundReasonRequest`의 사유 + 토큰 사용자) |
| 쓰는 곳 | `StockAllocationService.release` |
| 필드 | allocationId, reason(필수, 500자 이하, 상태 이력에 저장), userId |

## OutboundCreateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 출고에 아직 묶이지 않은 ALLOCATED 할당을 한 출고(READY)로 묶는다 |
| 엔드포인트 | POST `/api/v1/outbounds` |
| 만드는 곳 | `OutboundCreateRequest.toCommand(userId)` |
| 쓰는 곳 | `OutboundService.createOutbound` |
| 필드 | storeOrderId(필수), note(선택, 500자 이하), userId, skuIds |
| `skuIds` | **null이면 연결되지 않은 할당 전부**, 지정하면 그 SKU의 할당만, 빈 목록이면 거절 |
| 생성자 | `(storeOrderId, note, userId)` = 전부 묶기 |

## OutboundPickingCompleteCommand

| 항목 | 내용 |
|---|---|
| 용도 | 출고 항목별 피킹 수량을 확정한다 |
| 엔드포인트 | PATCH `/api/v1/outbounds/{outboundId}/picking/complete` |
| 만드는 곳 | `OutboundPickingCompleteRequest.toCommand(outboundId, userId)` |
| 쓰는 곳 | `OutboundFulfillmentService.completePicking` |
| 필드 | outboundId, lines(`PickedLine`: outboundLineId, pickedQuantity), userId |
| 규칙 | **이 출고의 모든 항목**을 입력해야 하고 중복 불가. 수량은 0 이상 할당 수량 이하 |

## OutboundCancelCommand

| 항목 | 내용 |
|---|---|
| 용도 | READY 출고를 취소한다 |
| 엔드포인트 | PATCH `/api/v1/outbounds/{outboundId}/cancel` |
| 만드는 곳 | 컨트롤러가 직접 생성 |
| 쓰는 곳 | `OutboundService.cancel` |
| 필드 | outboundId, reason(필수, 500자 이하), userId |

## Command가 없는 요청

`picking/start`, `ship`, `deliver`는 본문이 없어 Command 없이 `(outboundId, actor)`만 넘긴다.
