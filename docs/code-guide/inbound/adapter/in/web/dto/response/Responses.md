# 응답 DTO (Response)

`inbound/adapter/in/web/dto/response/` · 응답 `record`. 목록은 `ItemsResponse`(`data.items`)이고, 페이지네이션을 적용한 목록(입고, 창고 발주, 공급처 발주서)은 `PageResponse`(`data.items` + `data.pageInfo`)다.

## 입고

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `InboundRegisterResponse` | 등록된 입고 헤더 | POST `/inbounds` | `from(InboundView)` | inboundId, inboundNo, purchaseOrderId·No, warehouseId·Name, status, arrivedAt, receivedAt, receivedBy·Name, note, createdAt |
| `InboundSummaryResponse` | 목록 항목 | GET `/inbounds` | `from(InboundSummary)` | 등록 응답 필드 + supplierId·Name, lineCount (처리자명은 검수 전·취소 시 null) |
| `InboundResponse` | 헤더 단건 | GET `/inbounds/{id}` | `from(InboundView)` | 목록 필드 + note, createdAt, updatedAt, **cancelReason**(취소 상태일 때 상태 이력에서 읽은 값) |
| `InboundDetailsResponse` | 검수 항목 포함 | GET `/inbounds/{id}/details` | `from(InboundDetails)` | inboundId, inboundNo, status, items(`InboundLineResponse`) |
| `InboundLineResponse` | 검수 항목 한 줄 | details의 `items` | `from(InboundLineView)` | inboundLineId, purchaseOrderLineId, SKU(id·코드·이름), 로트(id·번호·제조일·유통기한), 수량 3종, 구역(합격·불량 id·코드), 발주 단가·입고 단가·금액, 사유, 비고, 처리 시각·처리자 |
| `InboundInspectResponse` | 검수 결과 | PATCH `.../inspect` | `of(inbound, lines)` | inboundId, inboundNo, status, items, updatedAt. 내부 `Item`은 `InboundLineView`의 요약(구역 코드·SKU명은 제외) |
| `InboundCompleteResponse` | 완료 결과 | PATCH `.../complete` | `from(InboundCompleteResult)` | 입고 정보 + `inventory`(항목별 합격·불량 재고 행 ID와 수량), `purchaseOrder`(id·상태), `purchaseOrderLines`(누적 수량·상태) |
| `InboundCancelResponse` | 취소 결과 | PATCH `.../cancel` | `of(inbound, view)` | inboundId, inboundNo, status, `purchaseOrder`(id·상태), cancelReason, updatedAt |
| `SectionCandidatesResponse` | 구역 후보 | GET `.../assignable-sections`, `.../defect-sections` | `of(inboundId, warehouseId, candidates)` | inboundId, warehouseId, items(sectionId, parentSectionId, 코드, 이름, 유형, capacity, currentCapacity, availableCapacity) |

취소 응답에 발주 상태를 담는 이유: **입고를 취소해도 발주 상태는 바뀌지 않는다**는 것을 함께 보여주기 위해서다.

## 창고 발주

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `PurchaseOrderRegisterResponse` | 등록된 발주 헤더 + 항목 | POST `/purchase-orders` | `of(view, lines)` | 발주 정보, warehouse·supplier 이름, totalAmount, createdBy·Name, `lines`(`PurchaseOrderLineResponse`) |
| `PurchaseOrderSummaryResponse` | 목록 항목 | GET `/purchase-orders` | `from(PurchaseOrderSummary)` | 발주 번호, 창고·공급처, 상태, expectedAt, lineCount, totalAmount, 작성자, createdAt |
| `PurchaseOrderResponse` | 헤더 단건 | GET `/purchase-orders/{id}` | `from(PurchaseOrderView)` | 목록 필드 + note, updatedAt, **cancelReason** |
| `PurchaseOrderDetailsResponse` | SKU별 항목 | GET `.../details` | `from(PurchaseOrderDetails)` | purchaseOrderId, purchaseOrderNo, status, items |
| `PurchaseOrderLineResponse` | 항목 한 줄 (등록 응답의 `lines`와 details의 `items` 공용) | | `from(PurchaseOrderLineView)` | purchaseOrderLineId, SKU(id·코드·이름·단위), expectedQuantity, receivedQuantity, remainingQuantity, orderedUnitPrice, lineAmount, status |
| `PurchaseOrderStatusResponse` | 확정·취소 상태 전이 결과 | PATCH `.../confirm`, `.../cancel` | `from(po)`, `of(po, cancelReason)` | purchaseOrderId, purchaseOrderNo, status, cancelReason(취소 응답에서만), updatedAt |

## 공급처

| 클래스 | 용도 | 엔드포인트 | 변환 | 필드 |
|---|---|---|---|---|
| `SupplierResponse` | 한 건 (등록·단건·수정·활성화 공통) | POST, GET `/{id}`, PATCH, `.../activate` | `from(Supplier)` | supplierId, supplierCode, supplierName, managerName, contactNumber, email, address, latitude, longitude, isActive, createdAt, updatedAt |
| `SupplierSummaryResponse` | 목록 항목 | GET `/suppliers` | `from(Supplier)` | 위 필드에서 createdAt·updatedAt 제외 |
| `SupplierDeactivateResponse` | 비활성화 결과 | PATCH `.../deactivate` | `from(Supplier)` | supplierId, supplierCode, supplierName, isActive, updatedAt |
