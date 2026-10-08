# Inventory API 명세

> 기준은 레포. 2026-10-02 Notion(최종 수정 2026-09-24)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/inventory`, `/api/v1/lots`. 목록 API는 `data.items`만 반환(페이지네이션 보류). 404는 도메인별 코드 사용.

## 구현 대비 메모

- Notion 명세의 `pageInfo`와 일반 `NOT_FOUND`는 현재 구현 기준(페이지네이션 보류, 도메인별 404)과 다름 → conventions.md 기준 따름.
- 목록 API는 `page`·`size`·`sort`를 받지 않고 고정 정렬을 쓴다(각 절의 "정렬" 참고).
- 확정 필요(미결): 안전 재고를 창고별로 둘지 여부, `InventoryTransaction.reference_id`가 문서 헤더(Inbound/Outbound)인지 항목(Line)인지, 로트 상태 변경 API(격리·폐기·만료 전환) 필요 여부.
- 인가(#170): 재고 조정 처리자는 토큰 주체이며 `userId` 쿼리 파라미터는 받지 않는다. 담당 창고 검사는 서비스에서 한다: 목록은 `warehouseId`를 생략하면 담당 창고로 좁히고(본사는 전체), 지정한 `warehouseId`·`sectionId`(구역의 창고)가 담당이 아니면 403이다. 재고 상세·재고별 이력·조정은 재고 행의 창고가 담당이 아니면 403이다. 로트 목록은 담당 창고에 재고가 있거나 입고 완료 이력이 있는 로트만 보이며, 입고 중(검수 중)인 로트는 완료 전까지 창고 관리자 목록에 나타나지 않는다. 로트 상세는 응답을 담당 창고 항목으로 좁히고, 보이는 항목이 없으면 403이다.
- 수량은 BIGINT(정수). Lot은 별도 생성 API 없이 입고 검수 트랜잭션에서 find-or-create(UNIQUE(sku_id, supplier_id, lot_number), ADR-004 — Notion 원문 기준).

## 엔드포인트 목록 (7 + 2)

| Method | Path | 권한 | 설명 |
|---|---|---|---|
| GET | /inventory | WAREHOUSE_MANAGER, HQ_ADMIN | SKU 집계 재고 목록 |
| POST | /inventory/adjustments | WAREHOUSE_MANAGER | 재고 수량 조정 |
| GET | /inventory/by-lot | WAREHOUSE_MANAGER, HQ_ADMIN | 로트 단위 재고 목록 |
| GET | /inventory/low-stock | WAREHOUSE_MANAGER, HQ_ADMIN | 안전 재고 미만 SKU |
| GET | /inventory/transactions | WAREHOUSE_MANAGER, HQ_ADMIN | 전체 재고 증감 이력 |
| GET | /inventory/{inventoryId} | WAREHOUSE_MANAGER, HQ_ADMIN | 재고(InventoryLot) 상세 |
| GET | /inventory/{inventoryId}/transactions | WAREHOUSE_MANAGER, HQ_ADMIN | 특정 재고 증감 이력 |
| GET | /lots | WAREHOUSE_MANAGER, HQ_ADMIN | 로트 마스터 목록 |
| GET | /lots/{lotId} | WAREHOUSE_MANAGER, HQ_ADMIN | 로트 상세 |

## 공통 규칙

- 점주(STORE_OWNER)는 전 API 호출 불가(403). 공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`.
- 데이터 범위: HQ_ADMIN은 전체 창고, WAREHOUSE_MANAGER는 본인이 배정된 창고(`WarehouseMember`)만. 비담당 창고·구역을 지정하면 403. `warehouseId` 생략 시 담당 창고 합산.
- 조회 API는 상태를 변경하지 않음.
- 품질 상태 `qualityStatus`: `AVAILABLE`(가용) / `DEFECTIVE`(불량). 검수 중은 `Inbound.status`, 할당은 `StockAllocation.status`/`allocated_quantity`로 관리(재고 행 상태에 두지 않음).
- 가용 수량 `availableQuantity`: `qualityStatus`와 로트 상태가 모두 `AVAILABLE`인 행만 `onHand - allocated`, 그 외 0.
- 코드 값: `transactionType`/`referenceType` = `INBOUND`/`OUTBOUND`/`ADJUSTMENT`; `Lot.status` = `AVAILABLE`/`EXPIRED`/`QUARANTINED`/`DISPOSED`(만료·격리·폐기 로트는 할당 불가).
- 원가(`unitCost`, `receivedUnitPrice`)는 HQ_ADMIN·WAREHOUSE_MANAGER에게만 제공.

## GET /inventory — SKU 집계 재고 (P1)

- Query: `skuId`, `warehouseId`, `keyword`(SKU 코드·명). `page`·`size`·`sort`는 받지 않는다(페이지네이션 보류).
- 정렬은 고정(`skuCode` 오름차순).
- 응답 항목: `skuId, skuCode, skuName, unit, totalQuantity, availableQuantity, allocatedQuantity, defectiveQuantity`
- 집계: `totalQuantity`=보유 합, `allocatedQuantity`=할당 합, `defectiveQuantity`=DEFECTIVE 보유 합, `availableQuantity`=AVAILABLE 재고의 (보유−할당) 합. 재고가 없는 SKU는 목록 제외.
- 에러: 404(존재하지 않는 skuId/warehouseId 필터)

## POST /inventory/adjustments — 재고 조정 (P1)

- 권한: WAREHOUSE_MANAGER만, 담당 창고의 재고만.
- Body: `inventoryLotId`, `beforeQuantity`(화면에서 본 현재 수량), `afterQuantity`(≥0 정수), `reason`(≤500)
- 201. 응답: `transactionId, inventoryLotId, transactionType=ADJUSTMENT, quantityDelta, beforeQuantity, afterQuantity, reason, createdBy, createdAt, inventory{onHandQuantity, allocatedQuantity, availableQuantity}`
- 에러: 400(필수 누락, 음수, reason 누락/초과, before==after), 403(WAREHOUSE_MANAGER 아님/비담당 창고), 404(inventoryLot 없음), 409 `STALE_QUANTITY`(beforeQuantity ≠ 서버 현재값), 409 `BELOW_ALLOCATED_QUANTITY`(after < 할당 수량), 409 `SECTION_CAPACITY_EXCEEDED`(증량 시 구역 수용량 초과), 409 `SECTION_INACTIVE`(증량 시 구역이 비활성), 409 `WAREHOUSE_INACTIVE`(증량 시 구역의 창고가 비활성). 세 코드는 창고 도메인의 `WarehouseErrorCode`로, 구역 용량 증가(`WarehouseSectionCapacityService`)에서 던진다.
- 규칙:
  - `quantityDelta = after - before`(서버 계산, 0이면 400).
  - 수량 변경 + 구역 `current_capacity` 증감 + `InventoryTransaction` 기록은 단일 트랜잭션. 이력의 `transaction_type`=`reference_type`=`ADJUSTMENT`, `reference_id`=null.
  - 동시성: 재고 행을 비관적 락으로 잠근 뒤 `beforeQuantity` 비교. 잠금 순서 구역(section_id 오름차순) → 재고 행(inventory_lot_id 오름차순) — 입고·출고 차감(`ship`)과 동일(ADR-006). 출고 할당·해제(`allocate`/`release`)는 재고 행만 잠근다(전역 락 순서는 [domain/outbound.md](../domain/outbound.md) "결정·미결" 2번, 성능 개선 때 확정).
  - 본사 승인 없이 창고 관리자가 즉시 반영. 성공 시 `last_counted_at`을 조정 시각으로 갱신(같은 트랜잭션).

## GET /inventory/by-lot — 로트 단위 재고

- Query: `skuId`, `warehouseId`, `sectionId`, `expiringBefore`(YYYY-MM-DD, 당일 포함), `qualityStatus`, `includeEmpty`(기본 false). `sort`는 받지 않는다.
- 정렬은 고정(`expiryDate` 오름차순, 유통기한 없는 로트는 뒤, 같으면 `inventoryLotId` 오름차순).
- 응답 항목: `inventoryLotId, lotId, lotNumber, skuId, skuCode, skuName, warehouseId, sectionId, sectionCode, sectionName, onHandQuantity, allocatedQuantity, availableQuantity, qualityStatus, expiryDate, lastCountedAt`
- Lot(마스터)과 InventoryLot(원장)을 조인. SKU 합산 없음. FEFO 피킹 기준 정렬. 기본적으로 보유 0 행 제외.
- 에러: 400(expiringBefore 형식 등), 404(필터 대상 없음)

## GET /inventory/low-stock — 안전 재고 미만 (P2)

- Query: `warehouseId`, `keyword`. `sort`는 받지 않는다.
- 정렬은 고정(`shortageQuantity` 내림차순, 같으면 `skuCode` 오름차순).
- 응답 항목: `skuId, skuCode, skuName, unit, safetyStockQuantity, availableQuantity, shortageQuantity`
- 조건: `availableQuantity < safetyStockQuantity`("미만", 2026-09-24 확정). `shortage = safety - available`. 안전 재고 0(미설정) SKU와 비활성 SKU 제외. 재고가 전혀 없어도 안전 재고가 설정된 SKU는 available 0으로 포함. 데이터 범위 내 창고 합산 가용 재고와 비교. 알림 생성은 범위 밖.

## GET /inventory/transactions — 전체 재고 이력 (P2)

- Query: `warehouseId`, `sectionId`, `skuId`, `lotId`, `transactionType`, `referenceType`, `referenceId`(referenceType과 함께), `createdFrom`, `createdTo`(ISO-8601, 양 끝 포함: `>= createdFrom`, `<= createdTo`). `sort`는 받지 않는다.
- 정렬은 고정(`createdAt` 내림차순, 같으면 `transactionId` 내림차순).
- 응답 항목: `transactionId, inventoryLotId, warehouseId, sectionId, sectionCode, skuId, skuCode, lotId, lotNumber, transactionType, quantityDelta, beforeQuantity, afterQuantity, referenceType, referenceId, reason, createdBy, createdByName, createdAt`
- 에러: 400(일시 형식, from>to, referenceId만 지정), 404(필터 대상 없음)
- 이력은 삭제·수정 불가. 보유 수량이 바뀐 경우(입고·출고·조정)만 기록하며 할당/해제는 기록하지 않음(2026-09-24 확정, 출고 감사 요구 시 재검토). 원천 문서 없으면 `referenceId`=null.

## GET /inventory/{inventoryId} — 재고 상세 (P1)

- `inventoryId` = `InventoryLot.inventory_lot_id` (= by-lot의 `inventoryLotId`). 보유 0 행도 조회 가능.
- 응답: `inventoryLotId, warehouseId, warehouseName, sectionId, sectionCode, sectionName, skuId, skuCode, skuName, unit, lotId, lotNumber, supplierId, supplierName, manufacturedDate, expiryDate, lotStatus, unitCost, onHandQuantity, allocatedQuantity, availableQuantity, qualityStatus, lastCountedAt, createdAt, updatedAt`
- 에러: 404(재고 없음), 403(비담당 창고)

## GET /inventory/{inventoryId}/transactions — 특정 재고 이력 (P2)

- Query: `transactionType`, `createdFrom`, `createdTo`(양 끝 포함). `sort`는 받지 않는다.
- 정렬은 고정(`createdAt` 내림차순, 같으면 `transactionId` 내림차순).
- 응답: `inventoryLotId`, `items[]`(`transactionId, transactionType, quantityDelta, beforeQuantity, afterQuantity, referenceType, referenceId, reason, createdBy, createdByName, createdAt`). 항상 `before + delta == after`. 이력이 없으면 빈 배열.
- 에러: 400(inventoryId 형식/일시 형식/from>to), 404(재고 없음), 403(비담당 창고)

## GET /lots — 로트 마스터 목록 (P2)

- Query: `skuId`, `supplierId`, `expiringBefore`(YYYY-MM-DD, 당일 포함), `keyword`(로트 번호). `sort`는 받지 않는다.
- 정렬은 고정(`expiryDate` 오름차순, 유통기한 없는 로트는 뒤, 같으면 `lotId` 오름차순).
- 응답 항목: `lotId, lotNumber, skuId, skuCode, skuName, supplierId, supplierName, manufacturedDate, expiryDate, status, unitCost` (수량 미포함)
- 범위: HQ_ADMIN 전체. WAREHOUSE_MANAGER는 담당 창고에 재고(InventoryLot) 또는 입고 이력이 있는 로트만.
- 에러: 400, 403(점주), 404(필터 대상 없음). 생성·수정 API 없음.

## GET /lots/{lotId} — 로트 상세 (P2)

- 응답: 로트 필드 + `inventory[]`(`inventoryLotId, warehouseId, warehouseName, sectionId, sectionCode, sectionName, onHandQuantity, allocatedQuantity, qualityStatus`), `inbounds[]`(`inboundId, inboundNo, warehouseId, receivedAt, receivedQuantity, acceptedQuantity, defectiveQuantity, receivedUnitPrice`), `createdAt, updatedAt`
- `inventory`는 구역별 InventoryLot, `inbounds`는 `InboundLine.lot_id`가 이 로트인 항목. 없으면 빈 배열.
- WAREHOUSE_MANAGER: 담당 창고의 재고·입고 이력이 있는 로트만(없으면 403), 응답의 `inventory`/`inbounds`는 담당 창고 항목만. 입고 이력(`inbounds`)만 따로 조회하는 API는 없으므로 403 판정은 항상 로트 단위(목록과 같은 기준)로 먼저 하고, 응답에서는 담당 창고 항목만 남긴다.
- 에러: 404(로트 없음), 403
