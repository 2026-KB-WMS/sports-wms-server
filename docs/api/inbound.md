# Inbound 도메인 API 명세 (공급처 · 창고 발주 · 입고)

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24 ~ 10-02)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/suppliers`, `/api/v1/purchase-orders`, `/api/v1/inbounds`. 목록 API는 `data.items`만 반환(페이지네이션 보류). 404는 도메인별 코드 사용.
> 상태 전이는 [docs/domain/inbound.md](../domain/inbound.md) 참고.

## 범위

- 이 문서는 **구현 완료된** Supplier(5) + PurchaseOrder(6) + Inbound(9)를 포함한다.

## 구현 대비 메모

- Notion 상태 컬럼은 "시작 전"이지만 코드는 구현됨(상태 컬럼이 오래됨, 코드 기준).
- Notion 명세의 `pageInfo`와 일반 `NOT_FOUND`는 현재 구현 기준과 다름 → conventions.md 기준 따름. 단 Supplier/PurchaseOrder 404는 도메인 코드(`SUPPLIER_NOT_FOUND`, `PURCHASE_ORDER_NOT_FOUND`).
- 확정 필요(미결): 공급처 재활성화 방법, 같은 공급처 중복 발주 허용 범위, 확정 단계에서 항목 조정 가능 여부, 취소 발주의 항목 상태 처리, `REQUESTED` 발주 반려는 MVP 이후. 발주 번호는 `PO-yyyyMMdd-NNNN`으로 구현했다.
- **입고** 구현 대비 차이 (2026-10-02 이전 시점):
  - 입고 목록·구역 후보 조회는 `page`·`size`·`sort`와 `pageInfo`를 지원하지 않고 `data.items` 전체를 돌려준다(페이지네이션 보류). 구역 후보 응답의 가용 용량 필드명은 `availableCapacity`다(Notion의 `requiredQuantity` 설명에 적힌 `availableQuantity`와 다름).
  - 인증·인가가 없어 401/403과 역할·소속 창고 검사를 적용하지 않는다. 검수·완료의 처리 사용자는 쿼리 파라미터 `userId`(필수)로 받는다. 인증 연동 시 토큰의 사용자로 대체한다.
  - 404는 도메인 코드를 쓴다: `INBOUND_NOT_FOUND`(입고), `PURCHASE_ORDER_NOT_FOUND`(발주), `SECTION_NOT_FOUND`(구역). 발주 항목이 없을 때만 일반 `NOT_FOUND`다.
  - 목록 필터의 존재하지 않는 값(발주 목록의 `warehouseId`·`supplierId`, 입고 목록의 `warehouseId`·`purchaseOrderId`)은 404가 아니라 빈 목록을 돌려준다(보류). 공급처 목록에는 대상 ID 필터가 없다.
  - 응답의 `receivedByName`(단건)은 내려주지 않는다(회원 도메인 구현 후 반영). 입고 취소 사유(필수, 500자 이하)는 `StatusHistory`(`entity_type` `INBOUND`)에 저장하고, 입고 단건 응답과 취소 응답의 `cancelReason`은 이 이력에서 읽는다.
  - 입고 번호는 `IB-yyyyMMdd-NNNN`(일자별 일련번호)로 구현했다.
  - 명세에 없이 서비스에 넣은 규칙: 같은 입고 안에서 (발주 항목, 로트) 중복은 400, 입고 완료 시 발주가 `CONFIRMED`가 아니거나 발주 항목별 입고 수량이 남은 수량을 넘으면 409 `CONFLICT`.
  - 확정 필요(미결): 발주 수량 초과 입고 허용 여부(현재 거절), 하위 구역이 있는 상위 구역에도 적치할 수 있는지, 취소된 입고를 다시 여는 방법(현재는 새 입고 등록), 같은 발주에 진행 중 입고를 여러 건 허용할지(현재 1건만).

## 엔드포인트 목록 (5 + 6 + 9)

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
| POST | /inbounds | WAREHOUSE_MANAGER | 입고 등록(ARRIVED) |
| GET | /inbounds | HQ_ADMIN, WAREHOUSE_MANAGER | 입고 목록 |
| GET | /inbounds/{inboundId} | HQ_ADMIN, WAREHOUSE_MANAGER | 입고 헤더 조회 |
| GET | /inbounds/{inboundId}/details | HQ_ADMIN, WAREHOUSE_MANAGER | 검수 항목 조회 |
| GET | /inbounds/{inboundId}/assignable-sections | HQ_ADMIN, WAREHOUSE_MANAGER | 합격품 적치 가능 구역 |
| GET | /inbounds/{inboundId}/defect-sections | HQ_ADMIN, WAREHOUSE_MANAGER | 불량품 적치 가능 구역 |
| PATCH | /inbounds/{inboundId}/inspect | WAREHOUSE_MANAGER | 검수 진행(검수 항목 교체) |
| PATCH | /inbounds/{inboundId}/complete | WAREHOUSE_MANAGER | 입고 완료(재고 반영) |
| PATCH | /inbounds/{inboundId}/cancel | WAREHOUSE_MANAGER | 입고 취소 |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`. 점주(STORE_OWNER)는 전 API 호출 불가(403).

## 공급처 (Supplier)

`isActive` = `status == 'ACTIVE'`. 상태 전이 ACTIVE→INACTIVE만.

### POST /suppliers (P0)

- Body: `supplierCode`(≤30, unique), `supplierName`(≤200), `managerName`(≤100), `contactNumber`(≤30), `email`(선택, ≤255, 형식 검증), `address`(선택, ≤500)
- 201. 응답: `supplierId, supplierCode, supplierName, managerName, contactNumber, email, address, isActive, createdAt, updatedAt`
- 에러: `DUPLICATE_SUPPLIER_CODE` 409 (DB unique 위반도 동일 매핑). 등록 시 status ACTIVE.

### GET /suppliers (P0)

- Query: `keyword`(공급처명·코드·담당자명), `isActive`. `sort`는 받지 않는다.
- 정렬은 고정(`supplierName` 오름차순, 같으면 `supplierId` 오름차순).
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

- Query: `status`(REQUESTED/CONFIRMED/COMPLETED/CANCELED), `warehouseId`, `supplierId`, `keyword`(발주 번호), `createdFrom`, `createdTo`(ISO-8601, 양 끝 포함: `>= createdFrom`, `<= createdTo`). `sort`는 받지 않는다.
- 정렬은 고정(등록 일시 `createdAt` 내림차순, 같으면 `purchaseOrderId` 내림차순).
- 응답 항목: `purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, supplierId, supplierName, status, expectedAt, lineCount, totalAmount, createdBy, createdByName, createdAt` (항목별 수량·단가는 `/details`)
- 에러: 400(status 값/일시 형식/from>to), 403(비담당 창고). 존재하지 않는 `warehouseId`·`supplierId` 필터는 404가 아니라 빈 목록을 돌려준다.

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

## 입고 (Inbound)

- 입고 `status`: `ARRIVED`(도착) → `INSPECTING`(검수 중) → `COMPLETED`(입고 완료, 재고 반영). `ARRIVED`·`INSPECTING` → `CANCELED`. 완료 후 취소·정정 API는 없고 재고 조정으로 처리한다. 입고의 `COMPLETED`는 업무 완료를 뜻하며 재고 품질 상태(`AVAILABLE`/`DEFECTIVE`)와 다르다.
- 입고 흐름: 도착 등록(`POST`) → 검수(`inspect`, 재호출로 검수 항목 교체, 재고 미반영) → 완료(`complete`, 재고·발주 반영). 하나의 발주에 부분 입고가 여러 번 걸칠 수 있고, 같은 발주에는 완료되지 않은 입고를 한 번에 하나만 둘 수 있다.
- 데이터 범위(인증 연동 후): HQ_ADMIN 전체, WAREHOUSE_MANAGER는 본인이 배정된 창고(`WarehouseMember`)의 입고만(타 창고는 403).
- 입고 대상 창고는 발주의 창고를 따른다(요청으로 받지 않음).
- 불량 구역은 구역 유형 `DEFECT`로 구분한다. 합격품은 `DEFECT`가 아닌 구역에, 불량품은 `DEFECT` 구역에만 둘 수 있다.

### POST /inbounds (P1)

- 권한: WAREHOUSE_MANAGER, 본인 담당 창고의 발주만.
- Body: `purchaseOrderId`(필수, `CONFIRMED` 발주), `arrivedAt`(선택, ISO-8601, 현재 이전. 생략하면 요청 시각), `note`(선택, ≤1000)
- 201. 응답: `inboundId, inboundNo, purchaseOrderId, purchaseOrderNo, warehouseId, warehouseName, status=ARRIVED, arrivedAt, receivedAt(null), receivedBy(null), note, createdAt`
- 에러: 400(`purchaseOrderId` 누락, `arrivedAt` 미래, 길이 초과), 403(비담당 창고), 404 `PURCHASE_ORDER_NOT_FOUND`, 409 `CONFLICT`(발주가 `CONFIRMED`가 아님: 확정 전·취소·완료), 409 `INBOUND_IN_PROGRESS`(같은 발주에 `ARRIVED`·`INSPECTING` 입고가 있음)
- 규칙: 입고 번호는 서버가 생성(`IB-yyyyMMdd-NNNN`, UNIQUE, 충돌 시 409 `DUPLICATE_INBOUND_NO`). 발주 행을 잠가 같은 발주에 대한 동시 등록·발주 취소가 어긋나지 않게 한다. 검수 항목과 재고 반영은 이 단계에서 하지 않는다.

### GET /inbounds (P1)

- Query: `status`(ARRIVED/INSPECTING/COMPLETED/CANCELED), `warehouseId`, `purchaseOrderId`, `keyword`(입고 번호·발주 번호, 대소문자 무시 부분 일치), `arrivedFrom`, `arrivedTo`(ISO-8601, 이상·이하)
- 정렬: 도착 일시 내림차순(같으면 입고 ID 내림차순). 검수 항목이 없는 입고도 포함한다.
- 응답 항목: `inboundId, inboundNo, purchaseOrderId, purchaseOrderNo, supplierId, supplierName, warehouseId, warehouseName, status, arrivedAt, receivedAt, receivedBy, lineCount`(검수 항목 수, `ARRIVED`면 0)
- 에러: 400(status 값·일시 형식 오류. `arrivedFrom`이 `arrivedTo`보다 늦음), 403(비담당 창고)

### GET /inbounds/{inboundId} (P1)

- 응답: 목록 항목 + `note, cancelReason(취소된 입고의 사유, 아니면 null), createdAt, updatedAt`
- 에러: 400(`inboundId` 형식), 403, `INBOUND_NOT_FOUND` 404
- 검수 완료 전이나 취소된 입고는 `receivedAt`, `receivedBy`가 `null`이다.

### GET /inbounds/{inboundId}/details (P2)

- 응답: `inboundId, inboundNo, status, items[]`(`inboundLineId, purchaseOrderLineId, skuId, skuCode, skuName, lotId, lotNumber, manufacturedDate, expiryDate, receivedQuantity, acceptedQuantity, defectiveQuantity, acceptedSectionId, acceptedSectionCode, defectSectionId, defectSectionCode, orderedUnitPrice, receivedUnitPrice, lineAmount, priceChangeReason, inspectionNote, receivedAt, receivedBy`)
- 정렬: SKU 코드, 로트 번호, 검수 항목 ID 순. `ARRIVED`는 `items`가 빈 배열. 구역이 아직 없는 항목은 구역 필드가 `null`.
- `acceptedQuantity + defectiveQuantity = receivedQuantity`, `lineAmount = receivedQuantity × receivedUnitPrice`. 입고 단가·금액은 HQ_ADMIN·WAREHOUSE_MANAGER에게만 제공(원가 정보).
- 에러: 400, 403, `INBOUND_NOT_FOUND` 404

### GET /inbounds/{inboundId}/assignable-sections, /defect-sections (P2)

- Query: `requiredQuantity`(선택, 이 수량 이상 여유가 있는 구역만), `keyword`(구역명·코드, 대소문자 무시 부분 일치)
- 응답: `inboundId, warehouseId, items[]`(`sectionId, parentSectionId, sectionCode, sectionName, sectionType, capacity, currentCapacity, availableCapacity`)
- 대상: 입고 창고의 활성(`ACTIVE`) 구역 중 여유 용량(`capacity - currentCapacity`)이 0보다 큰 구역. `assignable-sections`는 `DEFECT`가 아닌 구역, `defect-sections`는 `DEFECT` 구역. 구역 코드 오름차순. 입고 상태와 무관하게 조회할 수 있다.
- 후보만 보여줄 뿐 수용량을 예약하지 않는다. 실제 적치 가능 여부는 `complete`에서 다시 검사한다. 불량 구역이 없는 창고에서 불량 수량이 있는 항목은 완료할 수 없다(`SECTION_NOT_ASSIGNED`).
- 에러: 400(`inboundId` 형식, `requiredQuantity` 음수), 403, `INBOUND_NOT_FOUND` 404

### PATCH /inbounds/{inboundId}/inspect (P1)

- 권한: WAREHOUSE_MANAGER, 본인 담당 창고만.
- Query: `userId`(필수, 처리 사용자. 인증 연동 전 임시)
- Body: `lines[]`(1개 이상, 이번 입고의 검수 항목 **전체**. 호출할 때마다 저장된 항목을 통째로 교체)
  - `purchaseOrderLineId`(필수, 이 입고의 발주에 속한 항목), `lotNumber`(필수, ≤100), `manufacturedDate`(선택, `YYYY-MM-DD`), `expiryDate`(선택, 제조일 이후)
  - `receivedQuantity`(필수, >0 정수), `acceptedQuantity`(≥0), `defectiveQuantity`(≥0). `accepted + defective = received`
  - `receivedUnitPrice`(필수, ≥0, 소수 2자리), `priceChangeReason`(발주 단가와 다르면 필수, ≤500), `inspectionNote`(선택, ≤1000)
  - `acceptedSectionId`, `defectSectionId`(선택. 완료 전에는 지정해야 함)
- 200. 응답: `inboundId, inboundNo, status=INSPECTING, items[]`(`inboundLineId, purchaseOrderLineId, skuId, skuCode, lotId, lotNumber, receivedQuantity, acceptedQuantity, defectiveQuantity, acceptedSectionId, defectSectionId, orderedUnitPrice, receivedUnitPrice, lineAmount`), `updatedAt`
- 에러:
  - 400: 필드 누락·형식, 합격+불량≠입고 수량, 입고 수량이 발주 항목 잔여 수량 초과(같은 발주 항목을 여러 로트로 나눈 경우 합계 기준), 발주 단가와 다른데 `priceChangeReason` 없음, 유통기한이 제조일 이전, 다른 발주의 `purchaseOrderLineId`, 같은 요청 안의 (발주 항목, 로트 번호) 중복, 지정 구역이 다른 창고 소속·비활성·유형 불일치(합격 구역이 `DEFECT`, 불량 구역이 `DEFECT`가 아님)
  - 403, 404(`INBOUND_NOT_FOUND`, `PURCHASE_ORDER_NOT_FOUND`, `SECTION_NOT_FOUND`, 발주 항목 없음은 `NOT_FOUND`)
  - 409 `CONFLICT`(입고가 `ARRIVED`·`INSPECTING`이 아님), 409 `LOT_UNIT_COST_MISMATCH`(기존 로트의 원가와 입고 단가가 다름), 409 `LOT_DATE_MISMATCH`(기존 로트의 제조일·유통기한과 다름), 409 `LOT_NOT_AVAILABLE`(기존 로트가 `AVAILABLE`이 아님)
- 규칙:
  - 처음 호출하면 `ARRIVED` → `INSPECTING`. `INSPECTING` 재호출은 상태를 유지한 채 검수 항목을 교체한다(같은 내용 재호출해도 항목이 늘지 않는 멱등).
  - 로트는 SKU + 공급처 + 로트 번호(`UNIQUE(sku_id, supplier_id, lot_number)`)로 찾고 없으면 만든다(ADR-004). SKU는 발주 항목, 공급처는 발주의 공급처를 따른다. 새 로트의 `unit_cost`는 입고 단가, 상태는 `AVAILABLE`. 로트 검사·생성과 검수 항목 저장은 한 트랜잭션.
  - `lineAmount = receivedQuantity × receivedUnitPrice`는 서버가 계산한다. 처리 사용자·시각은 호출자와 호출 시각으로 기록한다.
  - 재고 수량, 구역 사용량, 발주 항목 상태를 바꾸지 않는다. 모든 반영은 `complete`에서 한다.

### PATCH /inbounds/{inboundId}/complete (P1)

- 권한: WAREHOUSE_MANAGER, 본인 담당 창고만. `INSPECTING`에서만 호출할 수 있다.
- Query: `userId`(필수, 처리 사용자. 인증 연동 전 임시). Body 없음.
- 200. 응답: `inboundId, inboundNo, status=COMPLETED, receivedAt, receivedBy, inventory[]`(`inboundLineId, acceptedInventoryLotId, acceptedQuantity, defectiveInventoryLotId, defectiveQuantity`), `purchaseOrder`(`purchaseOrderId, status`), `purchaseOrderLines[]`(`purchaseOrderLineId, expectedQuantity, receivedQuantity, status`)
- 에러: 400(`inboundId`·`userId` 형식·누락), 403, `INBOUND_NOT_FOUND` 404, `PURCHASE_ORDER_NOT_FOUND` 404
  - 409 `CONFLICT`(입고가 `INSPECTING`이 아님: 이미 완료·취소·검수 전, 발주가 `CONFIRMED`가 아님, 항목별 입고 수량이 발주 잔여 수량 초과, 재고 상태가 기존 행과 달라 충돌), 409 `INBOUND_HAS_NO_LINES`(저장된 검수 항목 없음), 409 `SECTION_NOT_ASSIGNED`(합격 수량이 있는데 합격 구역이, 불량 수량이 있는데 불량 구역이 없음), 409 `SECTION_CAPACITY_EXCEEDED`(적치하면 구역 수용량 초과), 409 `SECTION_INACTIVE`(검수 이후 적치 구역이 비활성이 됨), 409 `WAREHOUSE_INACTIVE`(적치 구역의 창고가 비활성), 409 `LOT_NOT_AVAILABLE`(검수 이후 로트가 `AVAILABLE`이 아님), 409 `SKU_NOT_ACTIVE`(비활성 SKU). `SECTION_CAPACITY_EXCEEDED`·`SECTION_INACTIVE`·`WAREHOUSE_INACTIVE`는 창고 도메인의 `WarehouseErrorCode`로, 일반 `CONFLICT`가 아니다.
- 규칙(한 트랜잭션, 하나라도 실패하면 아무것도 반영하지 않음):
  - 검수 항목마다 합격 수량은 합격 구역의 `InventoryLot`(구역 + 로트, `UNIQUE(section_id, lot_id)`, `AVAILABLE`)에, 불량 수량은 불량 구역의 `InventoryLot`(`DEFECTIVE`)에 `on_hand_quantity`를 더한다(없으면 생성). 구역 `current_capacity`를 반영 수량만큼 늘리고, 반영한 행마다 `InventoryTransaction`(`INBOUND`, `reference_id`=입고 ID, +수량)을 남긴다. 재고 처리는 재고 도메인의 `receive`를 재사용한다.
  - 발주 항목의 `received_quantity`에 이 입고의 입고 수량 합계(합격+불량)를 더하고 합계가 `expected_quantity` 미만이면 `PARTIALLY_RECEIVED`, 이상이면 `COMPLETED`로 바꾼다. 발주의 모든 항목이 `COMPLETED`가 되면 발주도 `COMPLETED`, 아니면 `CONFIRMED` 유지.
  - 마지막으로 입고를 `COMPLETED`로 바꾸고 `received_at`, `received_by`를 기록한다. 이미 `COMPLETED`면 409라 재고가 두 번 반영되지 않는다.
  - 잠금 순서: 입고 행 → 발주 행 → 발주 항목 행 → 구역 행 → 재고 행. 같은 입고를 동시에 완료하면 한 건만 성공하고, 같은 로트·구역에 서로 다른 입고를 동시에 완료해도 수량이 합계와 일치한다.

### PATCH /inbounds/{inboundId}/cancel (P2)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고(본사 관리자 불가).
- Body: `reason`(필수, ≤500)
- 200. 응답: `inboundId, inboundNo, status=CANCELED, cancelReason, purchaseOrder`(`purchaseOrderId, status`), `updatedAt`. 취소 사유는 `StatusHistory.reason`에 저장하고 응답의 `cancelReason`은 이력에서 읽는다.
- 에러: 400(`reason` 누락·빈 값·길이 초과, `inboundId` 형식), 403, `INBOUND_NOT_FOUND` 404, 409 `CONFLICT`(입고가 `ARRIVED`·`INSPECTING`이 아님: 이미 완료·취소)
- 규칙: 허용 전이는 `ARRIVED` → `CANCELED`, `INSPECTING` → `CANCELED`뿐이다. 재고·구역 사용량·발주 항목 상태·재고 이력을 바꾸지 않고, 저장된 검수 항목과 새로 만든 로트는 기록으로 남는다. 취소하면 같은 발주에 새 입고를 등록할 수 있고 발주는 `CONFIRMED`로 유지되며, 취소되지 않은 입고가 없으면 발주 취소도 다시 할 수 있다. 취소와 완료가 동시에 들어오면 입고 행을 잠가 한쪽만 성공한다.
