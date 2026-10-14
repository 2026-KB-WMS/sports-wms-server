# 요청 DTO (Request)

`storeorder/adapter/in/web/dto/request/` · 형식·길이는 `@Valid`, 업무 규칙은 서비스에서 검증한다. 처리자는 바디에 없고 토큰 사용자로 채워진다.

## StoreOrderRegisterRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/orders` |
| 변환 | `toCommand(userId)` → `StoreOrderRegisterCommand` |
| 필드 | storeId(필수), requestedDeliveryAt, note(최대 1000), lines(필수, 1개 이상) |
| `Line` | skuId(필수), requestedQuantity(필수, 양수) |
| 주의 | **공급 단가는 받지 않는다.** 등록 시점의 SKU 공급 단가를 서비스가 스냅샷한다 |

## StoreOrderAssignRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/orders/assign` |
| 변환 | `toCommand(userId)` → `StoreOrderAssignCommand` |
| 필드 | storeOrderId(필수), warehouseId(필수), reason(최대 500) |
| 주의 | 재배정(이미 ASSIGNED인 발주의 창고 변경)은 사유가 필수이며 서비스가 검증 |

## StoreOrderReasonRequest

| 항목 | 내용 |
|---|---|
| 용도 | **사유가 필수**인 상태 변경 요청 공용 바디 |
| 엔드포인트 | PATCH `/api/v1/orders/{id}/reject`, `hold`, `resume`, `complete-partial` |
| 변환 | 없음. 컨트롤러가 `request.reason()`을 Command에 넣는다 |
| 필드 | reason(필수, 최대 500) |

## StoreOrderCancelRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/orders/{id}/cancel` |
| 변환 | 없음. 컨트롤러가 `request.reason()` 사용, 바디가 없으면 null |
| 필드 | reason(최대 500) |
| 주의 | 승인 이후(APPROVED·ASSIGNED·ON_HOLD) 취소는 사유 필수, 승인 전(REQUESTED) 취소는 선택. **상태별 필수 여부는 서비스가 검증** |
