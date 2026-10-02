# Inbound 도메인 API 명세 (공급처 · 창고 발주)

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24 ~ 10-02)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/suppliers`, `/api/v1/purchase-orders`. 목록 API는 `data.items`만 반환(페이지네이션 보류). 404는 도메인별 코드 사용.
> 상태 전이는 [docs/domain/inbound.md](../domain/inbound.md) 참고.

## 범위

- 이 문서는 **구현 완료된** Supplier(5) + PurchaseOrder(6)만 포함한다.
- Inbound 본체 API 9건(입고 등록·검수·완료·취소 등)은 아직 구현 전이라 **Notion에 남아 있다**. 구현 시 이 파일로 이전한다.

## 구현 대비 메모

- Notion 상태 컬럼은 "시작 전"이지만 코드는 구현됨(상태 컬럼이 오래됨, 코드 기준).
- Notion 명세의 `pageInfo`와 일반 `NOT_FOUND`는 현재 구현 기준과 다름 → conventions.md 기준 따름. 단 Supplier/PurchaseOrder 404는 도메인 코드(`SUPPLIER_NOT_FOUND`, `PURCHASE_ORDER_NOT_FOUND`).
- 확정 필요(미결): 공급처 재활성화 방법, 발주 번호 형식(예시 `PO-YYYYMMDD-일련번호`), 같은 공급처 중복 발주 허용 범위, 확정 단계에서 항목 조정 가능 여부, 취소 발주의 항목 상태 처리, `REQUESTED` 발주 반려는 MVP 이후.

## 엔드포인트 목록 (5 + 6)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| POST | /suppliers | HQ_ADMIN | 공급처 등록 |
| GET | /suppliers | HQ_ADMIN, WAREHOUSE_MANAGER | 공급처 목록 |
| GET | /suppliers/{supplierId} | HQ_ADMIN, WAREHOUSE_MANAGER | 공급처 상세 |
| PATCH | /suppliers/{supplierId} | HQ_ADMIN | 공급처 수정 |
| PATCH | /suppliers/{supplierId}/deactivate | HQ_ADMIN | 공급처 비활성화 |
| POST | /purchase-orders | WAREHOUSE_MANAGER | 창고 발주 등록(REQUESTED) |
| GET | /purchase-orders | HQ_ADMIN, WAREHOUSE_MANAGER | 발주 목록 |
| GET | /purchase-orders/{purchaseOrderId} | HQ_ADMIN, WAREHOUSE_MANAGER | 발주 헤더 조회 |
| GET | /purchase-orders/{purchaseOrderId}/details | HQ_ADMIN, WAREHOUSE_MANAGER | 발주 항목(SKU별) 조회 |
| PATCH | /purchase-orders/{purchaseOrderId}/confirm | HQ_ADMIN | 발주 확정 |
| PATCH | /purchase-orders/{purchaseOrderId}/cancel | WAREHOUSE_MANAGER(작성자) / HQ_ADMIN | 발주 취소 |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`. 점주(STORE_OWNER)는 전 API 호출 불가(403).

## 공급처 (Supplier)

`isActive` = `status == 'ACTIVE'`. 상태 전이 ACTIVE→INACTIVE만.

### POST /suppliers (P0)

- Body: `supplierCode`(≤30, unique), `supplierName`(≤200), `managerName`(≤100), `contactNumber`(≤30), `email`(선택, ≤255, 형식 검증), `address`(선택, ≤500)
- 201. 응답: `supplierId, supplierCode, supplierName, managerName, contactNumber, email, address, isActive, createdAt`
- 에러: `DUPLICATE_SUPPLIER_CODE` 409 (DB unique 위반도 동일 매핑). 등록 시 status ACTIVE.

### GET /suppliers (P0)

- Query: `keyword`(공급처명·코드·담당자명), `isActive`, `sort`(기본 `supplierName,asc`)
- WAREHOUSE_MANAGER는 `isActive`와 무관하게 항상 활성 공급처만 조회(발주 등록 시 선택용). HQ_ADMIN은 `isActive`로 비활성도 조회. 소속 범위 없음(전체 조회).
- 응답 항목: `supplierId, supplierCode, supplierName, managerName, contactNumber, email, address, isActive`

### GET /suppliers/{supplierId} (P1)

- 응답: 목록 항목 + `createdAt, updatedAt`
- 에러: `SUPPLIER_NOT_FOUND` 404 (WAREHOUSE_MANAGER가 비활성 공급처를 조회해도 존재 여부를 숨기기 위해 404)

### PATCH /suppliers/{supplierId} (P1)

- 부분 수정(≥1 필드): `supplierName`, `managerName`, `contactNumber`, `email`(null이면 비움), `address`(null이면 비움)
- 필수 항목(`managerName`, `contactNumber`)을 null로 지정, `supplierCode`/`isActive` 포함 시 400. 비활성 공급처도 수정 가능. 기존 발주·입고 이력은 공급처 ID 참조라 유지.
- 에러: `SUPPLIER_NOT_FOUND` 404

### PATCH /suppliers/{supplierId}/deactivate (P2)

- 응답: `supplierId, supplierCode, supplierName, isActive=false, updatedAt`
- 에러: `SUPPLIER_NOT_FOUND` 404, 409 `CONFLICT`(이미 INACTIVE), 409 `SUPPLIER_IN_USE`(진행 중 발주 = `REQUESTED` 또는 `CONFIRMED` 존재; `COMPLETED`/`CANCELED` 제외)
- 비활성화 후 신규 발주 등록 불가, 창고 관리자 목록에서 숨김. 기존 발주·입고·`Lot.supplier_id` 이력 보존. 확인과 변경은 같은 트랜잭션.

## 창고 발주 (PurchaseOrder)

- 발주 `status`: `REQUESTED`(확정 대기) → `CONFIRMED`(확정) → `COMPLETED`(전량 입고) / `CANCELED`. 입고 진행 중 발주는 `CONFIRMED`이며, 항목 상태로 진행 확인.
- 항목(`PurchaseOrderLine`) `status`: `REQUESTED` → `PARTIALLY_RECEIVED`(`received < expected`) → `COMPLETED`. 입고 완료(`PATCH /inbounds/{id}/complete`) 시점마다 갱신.
- 데이터 범위: HQ_ADMIN 전체, WAREHOUSE_MANAGER는 본인이 배정된 창고(`WarehouseMember`)의 발주만(타 창고는 403). `warehouseId` 생략 시 담당 창고 합산.

### POST /purchase-orders (P1)

- 권한: WAREHOUSE_MANAGER, 본인 담당 창고만.
- Body: `warehouseId`, `supplierId`, `expectedAt`(선택, ISO-8601, 미래), `note`(선택, ≤1000), `lines[]`(≥1, 같은 SKU 중복 불가: `skuId`, `expectedQuantity`(>0 정수))
- 201. 응답: `purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, supplierId, supplierName, status=REQUESTED, expectedAt, note, totalAmount, createdBy, createdAt, lines[]`(`purchaseOrderLineId, skuId, skuCode, skuName, expectedQuantity, receivedQuantity(0), orderedUnitPrice, lineAmount, status`)
- 에러: 400(필수 누락, lines 비어 있음, 수량 ≤0, SKU 중복, expectedAt 과거), 403(비담당 창고), 404 `WAREHOUSE_NOT_FOUND`/`SUPPLIER_NOT_FOUND`/`SKU_NOT_FOUND`, 409 `CONFLICT`(비활성 창고·공급처·SKU), 409 `PURCHASE_PRICE_MISSING`(SKU 매입 단가 없음)
- 규칙:
  - 발주 + 항목을 단일 트랜잭션으로 생성. `purchase_order_no`는 서버 생성(UNIQUE), `created_by`는 토큰 사용자.
  - `orderedUnitPrice`는 요청에서 받지 않고 등록 시점의 `ProductSKU.current_purchase_price` 스냅샷(이후 SKU 단가 변경과 무관). `lineAmount = expectedQuantity × orderedUnitPrice`, `totalAmount` = 항목 금액 합. UNIQUE(purchase_order_id, sku_id).
  - 본사 알림 전송은 범위 밖.

### GET /purchase-orders (P1)

- Query: `status`(REQUESTED/CONFIRMED/COMPLETED/CANCELED), `warehouseId`, `supplierId`, `keyword`(발주 번호), `createdFrom`, `createdTo`(ISO-8601), `sort`(기본 `createdAt,desc`)
- 응답 항목: `purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, supplierId, supplierName, status, expectedAt, lineCount, totalAmount, createdBy, createdByName, createdAt` (항목별 수량·단가는 `/details`)
- 에러: 400(status 값/일시 형식/from>to), 403(비담당 창고), 404(필터 대상 없음)

### GET /purchase-orders/{purchaseOrderId} (P1)

- 응답: 목록 항목 + `note, cancelReason(취소 시 사유, 아니면 null), updatedAt`
- 에러: `PURCHASE_ORDER_NOT_FOUND` 404, 403(비담당 창고)

### GET /purchase-orders/{purchaseOrderId}/details (P2)

- 응답: `purchaseOrderId, purchaseOrderNo, status, items[]`(`purchaseOrderLineId, skuId, skuCode, skuName, unit, expectedQuantity, receivedQuantity, remainingQuantity, orderedUnitPrice, lineAmount, status`)
- `remainingQuantity = expected - received`. 입고 검수 시 이 수량을 넘는 입고 등록 불가. 취소된 발주의 항목도 조회 가능.
- 에러: `PURCHASE_ORDER_NOT_FOUND` 404, 403

### PATCH /purchase-orders/{purchaseOrderId}/confirm (P1)

- 권한: HQ_ADMIN만(창고 관리자 403). 요청·확정 분리 원칙(HQ-06).
- 응답: `purchaseOrderId, purchaseOrderNo, status=CONFIRMED, updatedAt`
- 에러: `PURCHASE_ORDER_NOT_FOUND` 404, 409 `CONFLICT`(REQUESTED가 아님, 부작용 재실행 없음), 409 `SUPPLIER_INACTIVE`(공급처 비활성)
- 상태만 전이(공급처·품목·수량 수정 불가, 변경 필요 시 취소 후 재등록). 확정된 발주에 대해서만 입고(`POST /inbounds`) 등록 가능. 창고 알림은 범위 밖.

### PATCH /purchase-orders/{purchaseOrderId}/cancel (P2)

- Body: `reason`(≤500; `CONFIRMED` 발주를 HQ_ADMIN이 취소할 때 필수, `REQUESTED` 취소 시 선택)
- 응답: `purchaseOrderId, purchaseOrderNo, status=CANCELED, cancelReason, updatedAt`
- 권한: `REQUESTED`는 작성자 WAREHOUSE_MANAGER만(본사 포함 타인 403), `CONFIRMED`는 HQ_ADMIN만.
- 에러: 400(CONFIRMED 취소 시 reason 누락/초과), 403, `PURCHASE_ORDER_NOT_FOUND` 404, 409 `CONFLICT`(이미 CANCELED 또는 COMPLETED), 409 `PURCHASE_ORDER_HAS_INBOUND`(취소되지 않은 입고가 하나라도 있음; 입고를 모두 취소한 발주는 다시 취소 가능)
- 허용 전이: REQUESTED→CANCELED, CONFIRMED→CANCELED. 상태 변경과 입고 존재 확인은 같은 트랜잭션(동시 취소·입고 등록 시 한쪽만 성공). 사유는 `StatusHistory.reason`에 저장, 응답의 `cancelReason`은 이력에서 읽음.
