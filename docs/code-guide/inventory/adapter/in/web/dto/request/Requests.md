# 요청 DTO (Request)

`inventory/adapter/in/web/dto/request/` · 처리자는 요청 바디에 포함하지 않고 **토큰 사용자**를 `toCommand(userId)`로 넘긴다.

## InventoryAdjustRequest

| 항목 | 내용 |
|---|---|
| 용도 | 재고 조정 요청 바디 |
| 엔드포인트 | POST `/api/v1/inventory/adjustments` |
| 변환 | `toCommand(userId)` → `InventoryAdjustCommand` |
| 필드 | inventoryLotId(필수), beforeQuantity(필수), afterQuantity(필수, 0 이상), reason(필수, 최대 500) |
| 주의 | `beforeQuantity`는 화면에서 본 수량이다. 서버 값과 다르면 409 `STALE_QUANTITY` |

## LotStatusChangeRequest

| 항목 | 내용 |
|---|---|
| 용도 | 로트 상태 변경 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/lots/{lotId}/status` |
| 변환 | `toCommand(userId)` → `LotStatusChangeCommand` |
| 필드 | status(필수, `LotStatus`), reason(필수, 최대 500) |
| 주의 | `EXPIRED`는 enum 값으로는 받지만 **서비스가 400으로 거절**한다 |
