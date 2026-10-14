# 요청 DTO (Request)

`inbound/adapter/in/web/dto/request/` · 형식·길이는 `@Valid`, 업무 규칙은 서비스에서 검증한다. 처리자는 바디에 없고 `toCommand(userId)`로 합쳐진다.

## InboundRegisterRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/inbounds` |
| 변환 | `toCommand(userId)` → `InboundRegisterCommand` |
| 필드 | purchaseOrderId(필수), arrivedAt, note(최대 1000) |
| 주의 | 창고 ID는 없다(발주의 창고를 따른다) |

## InboundInspectRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/inbounds/{inboundId}/inspect` |
| 변환 | `toCommand(userId)` → `InboundInspectCommand` |
| 필드 | lines(필수, 1개 이상, 각 항목 `@Valid`) |
| 의미 | **호출할 때마다 기존 검수 항목을 이 목록으로 통째로 교체**한다 |

`Line` 검증:

| 필드 | 검증 |
|---|---|
| purchaseOrderLineId | 필수 |
| lotNumber | 필수, 최대 100 |
| manufacturedDate, expiryDate | 선택 |
| receivedQuantity | 필수, **양수** |
| acceptedQuantity, defectiveQuantity | 필수, 0 이상 |
| receivedUnitPrice | 필수, 0 이상, 정수 16자리·소수 2자리 |
| priceChangeReason | 최대 500 |
| inspectionNote | 최대 1000 |
| acceptedSectionId, defectSectionId | 선택 |

합격 + 불량 = 입고 수량 같은 항목 간 규칙은 서비스가 검증한다.

## InboundCancelRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/inbounds/{inboundId}/cancel` |
| 변환 | `toCommand(userId)` → `InboundCancelCommand` |
| 필드 | reason(**필수**, 최대 500) |

## PurchaseOrderRegisterRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/purchase-orders` |
| 변환 | `toCommand(userId)` → `PurchaseOrderRegisterCommand` (`userId`는 `createdBy`) |
| 필드 | warehouseId(필수), supplierId(필수), expectedAt, note(최대 1000), lines(필수, 1개 이상) |
| `Line` | skuId(필수), expectedQuantity(필수, 양수) |
| 주의 | **발주 단가는 받지 않는다** |

## PurchaseOrderCancelRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/purchase-orders/{purchaseOrderId}/cancel` |
| 변환 | `toCommand(userId)` → `PurchaseOrderCancelCommand` |
| 필드 | reason(최대 500) |
| 주의 | 확정(CONFIRMED) 발주 취소 때는 사유 필수, 요청(REQUESTED) 발주 취소 때는 선택. 상태별 필수 여부는 서비스가 검증 |

## SupplierRegisterRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | POST `/api/v1/suppliers` |
| 변환 | `toCommand()` → `SupplierRegisterCommand` |
| 필드 | supplierCode(필수, 최대 30), supplierName(필수, 최대 200), managerName(필수, 최대 100), contactNumber(필수, 최대 30, `@PhoneNumber`), email(`@Email`, 최대 255), address(최대 500), latitude, longitude |
| 검증 | 위도·경도는 쌍으로만 (`@AssertTrue`) |

## SupplierUpdateRequest

| 항목 | 내용 |
|---|---|
| 엔드포인트 | PATCH `/api/v1/suppliers/{supplierId}` |
| 변환 | `toCommand()` → `SupplierUpdateCommand` |
| 형태 | **record가 아니라 일반 클래스**. 필드마다 `@JsonSetter`로 "JSON에 키가 있었는지"를 기록 |

### 왜 일반 클래스인가

email·address는 `null`을 보내면 값을 **비우는** 의미라서 "키 없음(변경 없음)"과 "키는 있는데 null(비움)"을 구분해야 한다. Jackson은 키가 없을 때도 필드를 null/`Optional.empty()`로 두기 때문에 둘을 구분하지 못한다. 그래서 세터가 호출됐는지(= 키가 있었는지)를 `...Present` 플래그로 따로 기록한다.

### toCommand 규칙

| 규칙 | 오류 |
|---|---|
| `supplierCode` 또는 `isActive`가 있으면 거부 (수정 불가) | 400 |
| `supplierName`, `managerName`, `contactNumber`는 키가 있는데 null이거나 공백이면 거부 (비울 수 없는 필수 항목) | 400 |
| email·address는 `present && null`이면 `clearEmail`/`clearAddress=true` | |
