# Outbound 도메인 API 명세 (출고·재고 할당)

> 기준은 레포. 2026-10-05 Notion(WMS API 명세, 최종 수정 2026-09-24)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/outbounds`, `/api/v1/allocations`. 구현 패키지는 `outbound`(ERD "출고": StockAllocation, Outbound, OutboundLine).
> 상태 전이·부수 효과는 [docs/domain/outbound.md](../domain/outbound.md) 참고.

## 범위

- 출고 8개 + 재고 할당 4개 = 12개. 지점 발주 도메인이 "출고 구현 때 함께 하기로" 미룬 `/allocations`를 포함한다([store-order.md](store-order.md) 범위 참고).
- Notion 개발 일정의 "[구현] 출고 도메인" 페이지는 처음에 7개(`/outbounds` 생성·목록, `details`, `picking/start`, `picking/complete`, `ship`, `deliver`)만 적고 있어 `PATCH /outbounds/{id}/cancel`과 `/allocations` 4개가 빠져 있었다. 지금은 이 문서와 같은 12개로 갱신되어 있다. 구현 범위도 이 문서의 12개다.

## 구현 대비 메모

이전 시점에 확인한 Notion 명세와의 차이와, 구현하면서 정하는 규칙을 적는다. 달라진 점은 이 목록에 추가한다.

- Notion 상태 컬럼은 "시작 전"이다.
- Notion 명세의 `pageInfo`(`page`·`size`·`sort`)와 일반 `NOT_FOUND`는 현재 구현 기준과 다름 → conventions.md 기준을 따른다. 목록 API는 `data.items`만 반환하고 `page`·`size`는 지원하지 않는다(페이지네이션 보류). 404는 도메인 전용 코드를 쓴다(`OUTBOUND_NOT_FOUND`, `ALLOCATION_NOT_FOUND`로 확정. 발주·창고·지점·SKU는 각 도메인 코드).
- 인증·인가는 #170에서 적용했다. 처리 사용자는 토큰 주체이며 `userId` 쿼리 파라미터는 받지 않는다. 역할은 보안 설정이, 담당 창고 범위(발주에 배정된 창고)는 서비스가 검사한다: 단건·쓰기는 비담당 창고면 403, 목록은 창고를 생략하면 담당 창고로 좁히고 비담당 창고를 지정하면 403이다.
- 모든 상태 변경과 사유는 `StatusHistory`에 기록한다(`entity_type`은 `OUTBOUND`, `STOCK_ALLOCATION`). 응답의 `cancelReason`은 이 이력에서 읽는다.
- 출고에는 창고 컬럼이 없다. 창고 기준 조회·권한은 지점 발주의 `warehouse_id`를 쓴다.
- 발주 항목 `shipped_quantity`는 피킹 완료에서 누적하고, 항목 상태(`PARTIALLY_SHIPPED`·`COMPLETED`)는 배송 완료에서 전환한다(2026-10-05 결정, [domain/outbound.md](../domain/outbound.md) "결정·미결" 1번). Notion은 이 시점을 문서마다 다르게 적고 있다.
- 락 순서와 할당·해제의 구역 잠금은 성능 개선 때 정한다(보류, [domain/outbound.md](../domain/outbound.md) "결정·미결" 2번).
- 출고 번호는 서버가 `OB-YYYYMMDD-일련번호(4자리)`로 채번한다(당일 출고 개수 + 1, 요청으로 받지 않음). 컬럼은 `outbound_no VARCHAR(30)`(UNIQUE)이고 출고 `note`는 `VARCHAR(500)`이다. 동시 생성으로 번호가 겹치면 UNIQUE 제약이 409 `DUPLICATE_OUTBOUND_NO`로 응답한다.
- 400 `errors`(필드별 사유)는 요청 바디 검증 실패에만 채워진다. 피킹 완료의 개수·ID 불일치·중복·할당 수량 초과와 `INSUFFICIENT_STOCK`의 SKU별 요청·가용 수량은 공통 `BusinessException`이 `errors`를 지원하지 않아 메시지로만 내려간다. 배열로 노출할지는 공통 예외 개선 때 정한다.
- 목록의 존재하지 않는 `storeOrderId`·`warehouseId`·`skuId` 필터는 404가 아니라 빈 목록을 돌려준다(보류).
- 확정 필요(미결): 배송 담당자·차량·운송장 기록 여부(ERD `Outbound`에는 `note`뿐), 배송 실패·수령 거부 처리, 부족 사유를 기록할 위치(ERD에 컬럼 없음), 취소한 출고를 다시 여는 방법(현재는 새 출고 생성), 재고 부족 시 부분 할당 허용 여부(현재 전체 실패), 세트 상품 구성품 동시 예약.

## 엔드포인트 목록 (12)

| Method | Path | 권한 | 우선순위 | 설명 |
|---|---|---|---|---|
| POST | /allocations | WAREHOUSE_MANAGER | P1 | 지점 발주 FEFO 재고 할당(ALLOCATED) |
| GET | /allocations | HQ_ADMIN, WAREHOUSE_MANAGER | P1 | 재고 할당 목록 |
| GET | /allocations/{allocationId} | HQ_ADMIN, WAREHOUSE_MANAGER | P1 | 재고 할당 상세 |
| PATCH | /allocations/{allocationId}/release | WAREHOUSE_MANAGER | P2 | 할당 해제(ALLOCATED → RELEASED) |
| POST | /outbounds | WAREHOUSE_MANAGER | P1 | 출고 생성(READY) |
| GET | /outbounds | HQ_ADMIN, WAREHOUSE_MANAGER | P1 | 출고 목록 |
| GET | /outbounds/{outboundId}/details | HQ_ADMIN, WAREHOUSE_MANAGER | P2 | 출고 상세(항목·피킹 위치) |
| PATCH | /outbounds/{outboundId}/picking/start | WAREHOUSE_MANAGER | P1 | 피킹 시작(READY → PICKING) |
| PATCH | /outbounds/{outboundId}/picking/complete | WAREHOUSE_MANAGER | P1 | 피킹 완료(PICKING → PICKED), 재고 차감 |
| PATCH | /outbounds/{outboundId}/ship | WAREHOUSE_MANAGER | P1 | 배송 시작(PICKED → SHIPPED) |
| PATCH | /outbounds/{outboundId}/deliver | WAREHOUSE_MANAGER | P1 | 배송 완료(SHIPPED → DELIVERED) |
| PATCH | /outbounds/{outboundId}/cancel | WAREHOUSE_MANAGER | P2 | 출고 취소(READY → CANCELED) |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 404(도메인 전용 코드). 상태 전이 API는 같은 요청을 다시 보내면 409 `CONFLICT`로 응답하고 부작용을 다시 실행하지 않는다. 상태 확인과 변경은 한 트랜잭션에서 대상 행을 잠그고 처리하며, `updated_at`을 갱신하고 `StatusHistory`에 기록한다. 알림 전송은 모든 API의 범위 밖이다.

## 공통 정의

- **데이터 범위**: HQ_ADMIN 전체, WAREHOUSE_MANAGER는 본인이 배정된 창고(`WarehouseMember`)에 배정된 발주의 할당·출고. 점주는 이 도메인 API를 호출할 수 없다(진행 상태는 지점 발주 조회의 `progressStage`로 본다). 범위 밖은 403.
- 할당 `status`: `ALLOCATED`(예약 중) → `PICKED`(피킹 완료, 재고 차감됨) 또는 `RELEASED`(해제).
- 출고 `status`: `READY` → `PICKING` → `PICKED` → `SHIPPED` → `DELIVERED`. `READY`에서만 `CANCELED`로 갈 수 있다.
- 출고 항목(`OutboundLine`)은 할당 한 건당 하나이며, 한 출고에는 취소되지 않은 출고에 아직 연결되지 않은 `ALLOCATED` 할당 전부가 묶인다.
- 공급 단가(`confirmedUnitSupplyPrice`, `lineAmount`)는 HQ_ADMIN과 WAREHOUSE_MANAGER에게만 제공한다.

## POST /allocations (P1)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고에 배정된 발주. 대상 창고는 발주의 `warehouse_id`이며 요청으로 받지 않는다.
- Body: `storeOrderId`(필수)
- 응답 201: `storeOrderId, orderNo, items[]`. `items[]`는 `GET /allocations`의 `data.items[]`와 같은 형식이다: `allocationId, storeOrderId, orderNo, storeOrderLineId, warehouseId, skuId, skuCode, skuName, inventoryLotId, lotId, lotNumber, expiryDate, sectionId, sectionCode, allocatedQuantity, pickedQuantity, status, allocatedAt, releasedAt(null)`
- 에러: 400, 403, 404(발주 없음), 409 `CONFLICT`(발주 상태가 `ASSIGNED`가 아님: 보류·취소). 창고가 아직 배정되지 않은 발주(`REQUESTED`·`APPROVED`)는 창고 관리자에게 403이다(담당 창고를 판별할 수 없고 조회도 할 수 없는 발주이므로 상태 409보다 소속 403이 먼저다), 409 `ALREADY_ALLOCATED`(할당할 잔여 수량 없음), 409 `INSUFFICIENT_STOCK`(한 항목이라도 가용 재고 부족, `errors`에 SKU별 요청·가용 수량), 409 `LOT_NOT_AVAILABLE`, 409 `SKU_NOT_ACTIVE`(재고 도메인이 거절)
- 규칙:
  - 발주 상태가 `ASSIGNED`일 때만. 보류(`ON_HOLD`) 중에는 재개 후 할당한다.
  - 항목별 할당 대상 수량은 `requestedQuantity - allocatedQuantity - shippedQuantity`(잔여 수량)다. 처음에는 요청 수량 전체, 부분 출고 뒤 다시 실행하면 남은 수량만이다.
  - FEFO: 대상 창고의 재고 행 중 품질 `AVAILABLE`, 로트 `AVAILABLE`, 구역 활성인 행에서 유통기한이 빠른 순(없으면 뒤로) → 로트 생성이 빠른 순 → `inventoryLotId` 순으로 가용 수량(`on_hand - allocated`)을 채운다. 한 항목이 여러 재고 행에 나뉠 수 있고 행마다 `StockAllocation` 하나가 생긴다.
  - 전체 항목을 한 번에 할당한다(all-or-nothing). 부족한 항목이 있으면 아무것도 할당하지 않는다.
  - 성공하면 `StockAllocation`(`ALLOCATED`, `picked_quantity` 0, 할당 사용자·일시)을 만들고 재고 행과 발주 항목의 `allocated_quantity`를 늘린다. 보유 수량은 바뀌지 않고 재고 이력(`InventoryTransaction`)은 남기지 않는다. `StatusHistory`에는 기록한다(`from_status` NULL → `ALLOCATED`).
  - 같은 요청을 다시 보내면 잔여 수량이 없어 409 `ALREADY_ALLOCATED`가 된다.
  - 재고 행과 발주 행을 비관적 락(ADR-006)으로 잠근다. 재고 쪽은 `InventoryStockUseCase.allocate`를 쓴다.

## GET /allocations (P1)

- 권한: HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고 발주의 할당만)
- Query: `storeOrderId`, `warehouseId`, `skuId`, `status`(`ALLOCATED`/`PICKED`/`RELEASED`), `keyword`(발주 번호·SKU 코드·로트 번호 부분 일치). `page`·`size`·`sort`는 페이지네이션 도입 때 적용(구현 대비 메모). 정렬은 고정(`allocatedAt` 내림차순, 같으면 `allocationId` 내림차순).
- 응답: `data.items[]`: `allocationId, storeOrderId, orderNo, storeOrderLineId, warehouseId, skuId, skuCode, skuName, inventoryLotId, lotId, lotNumber, expiryDate, sectionId, sectionCode, allocatedQuantity, pickedQuantity, status, allocatedAt, releasedAt`
- 에러: 400(`status` 값·필터 형식 오류), 403(점주, 또는 담당하지 않는 창고를 `warehouseId`로 지정), 404(존재하지 않는 `storeOrderId`·`warehouseId`·`skuId`로 필터링)
- 규칙: 조회 전용. 재고 행·로트·구역 정보는 `InventoryLot`·`Lot`·`WarehouseSection`을 ID 기준 읽기 전용 조인으로 가져온다(ADR-007). `warehouseId`를 생략한 창고 관리자는 담당 창고들의 할당을 받는다.

## GET /allocations/{allocationId} (P1)

- 권한: HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고 발주의 할당만)
- 응답: `allocationId, status, allocatedQuantity, pickedQuantity, allocatedAt, allocatedBy, allocatedByName, releasedAt, storeOrderId, orderNo, storeId, storeName, warehouseId, storeOrderLineId, requestedQuantity, skuId, skuCode, skuName, inventoryLotId, lotId, lotNumber, expiryDate, sectionId, sectionCode, sectionName, outboundId`
- 에러: 400, 403, 404(`ALLOCATION_NOT_FOUND`)
- 규칙: `outboundId`는 취소되지 않은 출고에 연결된 경우에만 값이 있고 아니면 `null`이다. 해제된 할당(`RELEASED`)도 조회할 수 있다. `allocatedByName`은 할당 처리자(`allocatedBy`)의 이름이고 사용자 테이블을 ID로 조인해 채운다. 처리자가 없으면 `null`이다.

## PATCH /allocations/{allocationId}/release (P2)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고 발주의 할당.
- Body: `reason`(필수, ≤500)
- 응답: `allocationId, status=RELEASED, allocatedQuantity, releasedAt, inventory{inventoryLotId, onHandQuantity, allocatedQuantity, availableQuantity}`
- 에러: 400, 403, 404, 409 `CONFLICT`(할당이 `ALLOCATED`가 아님: 이미 해제 또는 `PICKED`), 409 `ALLOCATION_IN_OUTBOUND`(취소되지 않은 출고에 연결됨)
- 규칙: `ALLOCATED` → `RELEASED`만, 피킹 시작 전에만(출고가 없거나 연결된 출고가 취소된 뒤). 해제하면 `released_at`을 채우고 재고 행과 발주 항목의 `allocated_quantity`를 이 할당 수량만큼 줄인다. 보유 수량은 불변이고 재고 이력은 남기지 않는다. 해제 후에도 발주는 `ASSIGNED`로 남는다. 재고 행·발주 행을 잠그고 한 트랜잭션으로 처리한다. 사유는 `StatusHistory.reason`에 저장한다.

## POST /outbounds (P1)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고에 배정된 발주. 출고 창고는 발주의 `warehouse_id`다.
- Body: `storeOrderId`(필수), `note`(선택, ≤500)
- 응답 201: `outboundId, outboundNo, storeOrderId, orderNo, status=READY, note, lineCount, items[], createdAt`. `items[]`: `outboundLineId, allocationId, skuId, skuCode, lotNumber, sectionCode, allocatedQuantity, shippedQuantity(0)`
- 에러: 400, 403, 404(발주 없음), 409 `CONFLICT`(발주 상태가 `ASSIGNED`가 아님. 창고 미배정 발주는 창고 관리자에게 403), 409 `NO_ALLOCATION`(취소되지 않은 출고에 연결되지 않은 `ALLOCATED` 할당이 없음)
- 규칙:
  - 연결되지 않은 `ALLOCATED` 할당마다 `OutboundLine` 하나를 만들고 `shipped_quantity`는 0, `confirmed_unit_supply_price`는 비워 둔다(피킹 완료 때 채움). 출고 번호는 서버가 만들며 UNIQUE다.
  - 재고 수량·할당·발주 상태는 바뀌지 않는다. 생성 이후 그 할당은 해제할 수 없다.
  - 부분 출고 뒤 남은 수량을 다시 할당하면 새 출고를 만들 수 있다(한 발주에 출고 여러 건).
  - 같은 할당이 두 출고에 묶이지 않도록 할당 행을 잠그고 단일 트랜잭션으로 처리한다. 같은 요청을 다시 보내면 409 `NO_ALLOCATION`이다.
  - `StatusHistory`에 기록한다(`from_status` NULL → `READY`).

## GET /outbounds (P1)

- 권한: HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고 발주의 출고만)
- Query: `status`(`READY`/`PICKING`/`PICKED`/`SHIPPED`/`DELIVERED`/`CANCELED`), `warehouseId`(발주에 배정된 창고 기준), `storeId`, `storeOrderId`, `keyword`(출고 번호·발주 번호 부분 일치), `createdFrom`, `createdTo`(ISO-8601, 양 끝 포함: `>= createdFrom`, `<= createdTo`). 정렬은 고정(`createdAt` 내림차순, 같으면 `outboundId` 내림차순). 페이지네이션은 보류.
- 응답: `data.items[]`: `outboundId, outboundNo, storeOrderId, orderNo, storeId, storeName, warehouseId, warehouseName, status, lineCount, shippedAt, shippedBy, createdAt`
- 에러: 400(`status` 값·일시 형식 오류, `createdFrom`이 `createdTo`보다 늦음), 403, 404(존재하지 않는 필터 대상)
- 규칙: 목록에는 헤더 정보와 `lineCount`만 담는다. `shippedAt`·`shippedBy`는 배송 시작 전 `null`이다. 조회 전용.

## GET /outbounds/{outboundId}/details (P2)

- 권한: HQ_ADMIN, WAREHOUSE_MANAGER(담당 창고 발주의 출고만)
- 응답: `outboundId, outboundNo, status, storeOrderId, orderNo, storeId, storeName, warehouseId, warehouseName, shippedAt, shippedBy, deliveredAt, note, cancelReason, items[], createdAt, updatedAt`. `items[]`: `outboundLineId, allocationId, storeOrderLineId, skuId, skuCode, skuName, unit, inventoryLotId, lotId, lotNumber, expiryDate, sectionId, sectionCode, allocatedQuantity, shippedQuantity, confirmedUnitSupplyPrice, lineAmount`
- 에러: 400, 403, 404(`OUTBOUND_NOT_FOUND`)
- 규칙: `shippedQuantity`는 피킹 완료 전 0이고 완료 후 실제 피킹 수량이다(`allocatedQuantity`보다 작으면 부족분). `confirmedUnitSupplyPrice`·`lineAmount`(`shippedQuantity × confirmedUnitSupplyPrice`)는 피킹 완료 후에 채워지고 그 전에는 `null`이다. `deliveredAt`은 배송 완료 전 `null`. 취소된 출고는 `cancelReason`을 함께 반환하고 `shippedQuantity`는 0이다. 조회 전용.

## PATCH /outbounds/{outboundId}/picking/start (P1)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고의 출고.
- Body 없음. 응답: `outboundId, outboundNo, status=PICKING, updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`READY`가 아님), 409 `ORDER_NOT_ASSIGNED`(연결된 발주가 `ASSIGNED`가 아님: 보류·취소), 409 `ALLOCATION_NOT_ACTIVE`(출고 항목의 할당이 `ALLOCATED`가 아님)
- 규칙: `READY` → `PICKING`만. 이 시점부터 해당 할당은 해제할 수 없고 본사가 발주를 취소할 수 없다(`ORDER_IN_PICKING`). 출고 행을 잠가 같은 출고를 두 번 시작하지 못하게 한다. 재고 수량·할당 수량·재고 이력은 바꾸지 않는다. `StatusHistory`(`READY` → `PICKING`)에 기록한다.

## PATCH /outbounds/{outboundId}/picking/complete (P1)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고의 출고.
- Body: `lines[]`(필수, 이 출고의 모든 항목, 중복 불가) — `outboundLineId`, `pickedQuantity`(0 이상 정수, 해당 항목 할당 수량 이하)
- 응답: `outboundId, outboundNo, status=PICKED, hasShortage, items[], updatedAt`. `items[]`: `outboundLineId, allocationId, allocatedQuantity, pickedQuantity, shortageQuantity, confirmedUnitSupplyPrice, lineAmount, inventory{inventoryLotId, onHandQuantity, allocatedQuantity}`
- 에러: 400(`lines` 누락·개수·ID 불일치·중복, `pickedQuantity` 음수 또는 할당 수량 초과, `errors`에 필드별), 403, 404, 409 `CONFLICT`(`PICKING`이 아님), 409 `NOTHING_PICKED`(모든 항목 0), 409 `SUPPLY_PRICE_MISSING`(발주 항목·SKU 모두 공급 단가 없음)
- 규칙: `PICKING` → `PICKED`만. 재고 차감 시점은 배송 시작이 아니라 이 피킹 완료다. 다음을 **한 트랜잭션**으로 처리하고 하나라도 실패하면 반영하지 않는다.
  - 항목마다 재고 행의 `on_hand_quantity`를 `pickedQuantity`만큼, `allocated_quantity`를 할당 수량 전체만큼 줄이고 구역의 `current_capacity`를 `pickedQuantity`만큼 줄인다(`InventoryStockUseCase.ship`). `pickedQuantity`가 0보다 크면 `InventoryTransaction`(`OUTBOUND`, `reference_id`는 출고 ID, `quantity_delta`는 −`pickedQuantity`)을 남긴다.
  - 항목마다 `StockAllocation.picked_quantity`와 `status=PICKED`, `OutboundLine.shipped_quantity=pickedQuantity`, `confirmed_unit_supply_price`(발주 항목 `requested_unit_supply_price` 스냅샷, 없으면 SKU 현재 공급 단가)를 채우고, 발주 항목의 `allocated_quantity`를 할당 수량만큼 줄이고 `shipped_quantity`를 `pickedQuantity`만큼 늘린다.
  - 마지막으로 출고를 `PICKED`로 바꾼다.
  - 부족분(`shortageQuantity` = 할당 − 피킹)의 예약은 풀려 다시 가용 재고가 되고, 발주 항목에는 미출고 수량이 남아 `POST /allocations`로 다시 할당해 후속 출고로 이어갈 수 있다. 실물 부족·파손의 재고 반영은 `POST /inventory/adjustments`로 정리한다.
  - 락 순서는 구역(`section_id` 오름차순) → 재고 행(`inventory_lot_id` 오름차순)이며 발주 행 위치는 [domain/outbound.md](../domain/outbound.md) 참고. `StatusHistory`에 출고(`PICKING` → `PICKED`)와 할당(`ALLOCATED` → `PICKED`)을 기록한다.

## PATCH /outbounds/{outboundId}/ship (P1)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고의 출고.
- Body 없음. 응답: `outboundId, outboundNo, status=SHIPPED, shippedAt, shippedBy, updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`PICKED`가 아님)
- 규칙: `PICKED` → `SHIPPED`만. `shipped_at`을 현재 시각, `shipped_by`를 요청자로 기록한다. 재고·할당·재고 이력은 바꾸지 않는다(차감은 피킹 완료에서 끝). 출고 행을 잠가 처리하고 `StatusHistory`(`PICKED` → `SHIPPED`)에 기록한다.

## PATCH /outbounds/{outboundId}/deliver (P1)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고의 출고.
- Body 없음. 응답: `outboundId, outboundNo, status=DELIVERED, deliveredAt, storeOrder{storeOrderId, status}, updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`SHIPPED`가 아님)
- 발주 항목 상태: 배송 완료 때 이 출고가 다룬 발주 항목의 상태를 `shipped_quantity`로 다시 계산한다(`0 < shipped < requested` → `PARTIALLY_SHIPPED`, `shipped ≥ requested` → `COMPLETED`). 수량은 피킹 완료에서 이미 반영돼 있다.
- 규칙: `SHIPPED` → `DELIVERED`만. `delivered_at`을 현재 시각으로 기록하고 재고·할당·재고 이력은 바꾸지 않는다. 이 출고로 발주의 모든 항목이 `shippedQuantity ≥ requestedQuantity`가 되고 진행 중 출고(`READY`/`PICKING`/`PICKED`/`SHIPPED`)가 없으면 발주를 `COMPLETED`로 바꾼다(시스템 자동 전이, 처리자는 요청자). 남은 수량이 있으면 `ASSIGNED`를 유지하고 `POST /allocations`로 다시 할당해 후속 출고를 진행한다. 출고 행을 잠가 처리하고 `StatusHistory`(출고 `SHIPPED` → `DELIVERED`, 발주 `ASSIGNED` → `COMPLETED`)에 기록한다. 배송 실패·수령 거부는 미결이다.

## PATCH /outbounds/{outboundId}/cancel (P2)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고의 출고. 본사 관리자는 호출할 수 없고 발주 취소를 통해 간접적으로만 취소된다.
- Body: `reason`(필수, ≤500)
- 응답: `outboundId, outboundNo, status=CANCELED, cancelReason, updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`READY`가 아님: 피킹 시작 이후이거나 이미 `CANCELED`)
- 규칙: `READY` → `CANCELED`만. 사유는 `StatusHistory.reason`에 저장하고 `cancelReason`은 이 이력에서 읽는다. 재고 수량·`allocated_quantity`·할당 상태·발주 상태는 바꾸지 않는다. 취소된 출고의 `OutboundLine`은 이력으로 남고, 그 항목이 참조하던 할당은 "취소되지 않은 출고에 연결되지 않은" 상태가 되어 새 출고에 다시 묶거나 해제할 수 있다. 취소와 피킹 시작이 동시에 들어오면 출고 행을 잠가 한쪽만 성공하게 한다. 발주 취소(승인 이후)가 `READY` 출고를 함께 취소할 때도 같은 처리를 쓴다([store-order.md](store-order.md) `PATCH /orders/{orderId}/cancel`).
