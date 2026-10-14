# 요청 DTO (Request)

`outbound/adapter/in/web/dto/request/` · 형식·길이는 `@Valid`, 업무 규칙은 서비스에서 검증한다. 처리자는 바디에 없고 토큰 사용자로 채워진다.

## StockAllocateRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/allocations` |
| 변환 | `toCommand(userId)` → `StockAllocateCommand` |
| 필드 | storeOrderId(필수), skuIds(선택) |
| `skuIds` | **생략하면 발주 전체**, 주면 그 SKU만. 주려면 1개 이상이어야 한다(`@Size(min = 1)`), 항목은 null 불가 |

## OutboundCreateRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/outbounds` |
| 변환 | `toCommand(userId)` → `OutboundCreateCommand` |
| 필드 | storeOrderId(필수), note(최대 500), skuIds(선택) |
| `skuIds` | 생략하면 **연결되지 않은 할당 전부**, 주면 그 SKU의 할당만 |

## OutboundPickingCompleteRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/outbounds/{outboundId}/picking/complete` |
| 변환 | `toCommand(outboundId, userId)` → `OutboundPickingCompleteCommand` |
| 필드 | lines(필수, 비어 있으면 안 됨) |
| `Line` | outboundLineId(필수), pickedQuantity(필수, 0 이상) |
| 주의 | **항목 수·ID 일치, 중복, 할당 수량 초과는 서비스가 검증**한다 |

## OutboundReasonRequest

| 항목 | 내용 |
|---|---|
| 용도 | 사유가 필요한 요청 공용 바디 |
| 엔드포인트 | PATCH `/api/v1/allocations/{id}/release`, PATCH `/api/v1/outbounds/{id}/cancel` |
| 변환 | 없음. 컨트롤러가 `request.reason()`을 Command에 넣는다 |
| 필드 | reason(필수, 최대 500) |
