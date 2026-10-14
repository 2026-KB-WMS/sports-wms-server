# Command

`inbound/application/port/in/command/` · 처리자는 요청 바디가 아니라 **토큰 사용자**로, `toCommand(userId)`가 Command의 `userId`(또는 `createdBy`)에 채운다.

## InboundRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 확정된 발주에 대한 도착(ARRIVED) 입고를 등록한다 |
| 엔드포인트 | POST `/api/v1/inbounds` |
| 만드는 곳 | `InboundRegisterRequest.toCommand(userId)` |
| 쓰는 곳 | `InboundService.registerInbound` |
| 필드 | purchaseOrderId(CONFIRMED 발주), arrivedAt(선택, 현재 이전. 생략하면 요청 시각), note(선택, 1000자 이하), userId(상태 이력 처리자) |
| 주의 | 입고 창고는 발주의 창고를 따르므로 받지 않는다 |

## InboundInspectCommand

| 항목 | 내용 |
|---|---|
| 용도 | 검수 항목 **전체**를 이 목록으로 교체한다 |
| 엔드포인트 | PATCH `/api/v1/inbounds/{inboundId}/inspect` |
| 만드는 곳 | `InboundInspectRequest.toCommand(userId)` |
| 쓰는 곳 | `InboundInspectService.inspectInbound` |
| 필드 | userId(검수 처리자), lines(`Line` 목록) |

`Line` 필드:

| 필드 | 설명 |
|---|---|
| purchaseOrderLineId | 대응하는 발주 항목 (이 입고의 발주에 속해야 함) |
| lotNumber | 로트 번호 (100자 이하). 같은 SKU·공급처·번호가 있으면 재사용 |
| manufacturedDate, expiryDate | 제조일, 유통기한(제조일 이후) |
| receivedQuantity | 입고 수량 (0 초과) |
| acceptedQuantity, defectiveQuantity | 합격·불량 수량 (0 이상, **합 = 입고 수량**) |
| receivedUnitPrice | 입고 확정 단가 (0 이상, 소수 2자리까지) |
| priceChangeReason | 발주 단가와 다를 때 필수 (500자 이하) |
| inspectionNote | 검수 비고 (1000자 이하) |
| acceptedSectionId, defectSectionId | 합격·불량 구역 (선택, 완료 전에는 지정 필요) |

## InboundCancelCommand

| 항목 | 내용 |
|---|---|
| 용도 | 완료 전 입고를 사유와 함께 취소한다 |
| 엔드포인트 | PATCH `/api/v1/inbounds/{inboundId}/cancel` |
| 만드는 곳 | `InboundCancelRequest.toCommand(userId)` |
| 쓰는 곳 | `InboundService.cancelInbound` |
| 필드 | reason(필수, 500자 이하), userId(상태 이력 처리자) |

## PurchaseOrderRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 창고 발주와 항목을 등록한다 |
| 엔드포인트 | POST `/api/v1/purchase-orders` |
| 만드는 곳 | `PurchaseOrderRegisterRequest.toCommand(userId)` |
| 쓰는 곳 | `PurchaseOrderService.registerPurchaseOrder` |
| 필드 | warehouseId, supplierId, expectedAt(선택), note(선택), createdBy, lines(`Line`: skuId, expectedQuantity) |
| 주의 | **발주 단가는 받지 않는다.** 등록 시점의 SKU 매입 단가를 서비스가 스냅샷한다 |

## PurchaseOrderCancelCommand

| 항목 | 내용 |
|---|---|
| 용도 | 요청·확정 발주를 취소한다 |
| 엔드포인트 | PATCH `/api/v1/purchase-orders/{purchaseOrderId}/cancel` |
| 만드는 곳 | `PurchaseOrderCancelRequest.toCommand(userId)` |
| 쓰는 곳 | `PurchaseOrderService.cancelPurchaseOrder` |
| 필드 | reason(500자 이하. **CONFIRMED 취소 때 필수**, REQUESTED 취소 때는 선택), userId |

## SupplierRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 공급처를 등록한다 |
| 엔드포인트 | POST `/api/v1/suppliers` |
| 만드는 곳 | `SupplierRegisterRequest.toCommand()` |
| 쓰는 곳 | `SupplierService.registerSupplier` |
| 필드 | supplierCode, name, managerName, contactNumber, email(선택), address(선택), latitude, longitude |

## SupplierUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 공급처를 부분 수정한다. 코드와 거래 상태는 포함하지 않는다 |
| 엔드포인트 | PATCH `/api/v1/suppliers/{supplierId}` |
| 만드는 곳 | `SupplierUpdateRequest.toCommand()` |
| 쓰는 곳 | `SupplierService.updateSupplier(supplierId, command)` |
| 필드 | name, managerName, contactNumber, email, address, **clearEmail, clearAddress**, latitude, longitude |
| 메서드 | `hasNoChanges()` |
| 쓰임 | name·managerName·contactNumber는 null이면 "변경 없음". email·address는 값이 있으면 변경, 값이 null이고 clear 플래그가 true면 **비움**. 값이 있으면 clear보다 값이 우선 |
