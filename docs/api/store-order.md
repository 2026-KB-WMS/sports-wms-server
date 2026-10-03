# StoreOrder 도메인 API 명세 (지점 발주)

> 기준은 레포. 2026-10-03 Notion(최종 수정 2026-09-24 ~ 10-03)에서 이전. 공통 규칙은 [conventions.md](conventions.md) 참고.
> Base path: `/api/v1/orders`. 구현 패키지는 `storeorder`(ERD "발주": StoreOrder, StoreOrderLine). 창고 → 공급업체 발주(`/purchase-orders`)는 [inbound.md](inbound.md)이며 별개다.
> 상태 전이는 [docs/domain/store-order.md](../domain/store-order.md) 참고.

## 범위

- 이 문서는 지점 발주 12개를 포함한다. **아직 구현 전**이며 구현을 시작하면서 이전했다(트래킹 이슈 #117, 선행 #116).
- 재고 할당(`/allocations`, 4개)은 포함하지 않는다. `StockAllocation`은 ERD 출고 섹션이라 출고 도메인 구현 시 이전한다.

## 구현 대비 메모

구현 전이므로 "구현할 때 지킬 규칙과 알려진 차이"를 적는다. 구현하면서 달라진 점은 이 목록에 추가한다.

- Notion 상태 컬럼은 "시작 전"이다.
- Notion 명세의 `pageInfo`(`page`·`size`)와 일반 `NOT_FOUND`는 현재 구현 기준과 다름 → conventions.md 기준을 따른다. 목록 API는 `data.items`만 반환하고 `page`·`size`는 지원하지 않는다(페이지네이션 보류). 404는 도메인 전용 코드를 쓴다(`STORE_ORDER_NOT_FOUND` 등, 이름은 구현 시 확정. 지점·창고·SKU는 각 도메인 코드).
- 인증·인가는 입고와 같은 방식으로 보류한다. 처리 사용자는 쿼리 파라미터 `userId`로 받고, 401/403과 역할·소속 지점/창고·작성자 검사는 인증 연동 때 적용한다. `GET /orders/my`의 소속 판별도 인증 연동 때 처리한다.
- 모든 상태 변경과 사유는 `StatusHistory`에 기록한다. 응답의 `statusReason`과 `details.statusHistory`는 이 이력에서 읽는다(선행 이슈 #116).
- 출고·할당 의존 동작은 출고 도메인이 생길 때 완성한다. 지점 발주 쪽은 연동 포트만 정의하고 임시 구현을 둔다. 임시 구현 동안 `latestOutboundStatus`는 `null`, `outbounds`는 빈 배열이고, 진행 중 출고·피킹 시작 검사(`ORDER_IN_PICKING`, `OUTBOUND_IN_PROGRESS`, `ORDER_IN_FULFILLMENT`)는 "없음"으로 통과하며, 승인 후 취소 시 할당 해제·`READY` 출고 취소는 수행하지 않는다(`releasedAllocationCount`, `canceledOutboundCount`는 0).
- 2026-10-03에 명세에 반영한 결정: 발주 항목 상태 4개(`REQUESTED`/`PARTIALLY_SHIPPED`/`COMPLETED`/`CANCELED`), 점주용 진행 단계 `progressStage`(목록·단건·상세 응답).
- 확정 필요(미결): 주문 번호 형식(예시 `SO-YYYYMMDD-일련번호`), "주문 단위 검증"(화면 설계 ST-04)의 단위 기준(현재는 1 이상 정수만 검증), 요청 배송 일시의 최소 리드타임, 배송 실패·수령 거부 처리.

## 엔드포인트 목록 (12)

| Method | Path | 권한 | 우선순위 | 설명 |
|---|---|---|---|---|
| POST | /orders | STORE_OWNER | P1 | 지점 발주 등록(REQUESTED) |
| GET | /orders | HQ_ADMIN | P1 | 전체 발주 목록 |
| GET | /orders/my | STORE_OWNER, WAREHOUSE_MANAGER | P2 | 내 발주 목록 |
| GET | /orders/{orderId} | HQ_ADMIN, STORE_OWNER, WAREHOUSE_MANAGER | P1 | 발주 헤더 조회 |
| GET | /orders/{orderId}/details | HQ_ADMIN, STORE_OWNER, WAREHOUSE_MANAGER | P2 | 발주 항목·출고·상태 이력 조회 |
| PATCH | /orders/{orderId}/approve | HQ_ADMIN | P1 | 승인(REQUESTED → APPROVED) |
| PATCH | /orders/{orderId}/reject | HQ_ADMIN | P1 | 반려(REQUESTED → REJECTED) |
| PATCH | /orders/{orderId}/cancel | STORE_OWNER(작성자) / HQ_ADMIN | P1 | 취소 |
| POST | /orders/assign | HQ_ADMIN | P2 | 창고 배정·재배정 |
| PATCH | /orders/{orderId}/hold | WAREHOUSE_MANAGER | P2 | 출고 보류(ASSIGNED → ON_HOLD) |
| PATCH | /orders/{orderId}/resume | WAREHOUSE_MANAGER | P2 | 출고 재개(ON_HOLD → ASSIGNED) |
| PATCH | /orders/{orderId}/complete-partial | WAREHOUSE_MANAGER | P2 | 부분 출고 종결(ASSIGNED → COMPLETED) |

공통 에러: 400 `VALIDATION_ERROR`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`. 상태 전이 API는 같은 요청을 다시 보내면 409 `CONFLICT`로 응답하고 부작용을 다시 실행하지 않는다. 상태 확인과 변경은 한 트랜잭션에서 발주 행을 잠그고 처리하며, `updated_at`을 갱신하고 `StatusHistory`에 기록한다. 알림 전송은 모든 API의 범위 밖이다.

## 공통 정의

- **데이터 범위**(인증 연동 후): HQ_ADMIN 전체, STORE_OWNER는 본인이 배정된 지점(`StoreMember`)의 발주, WAREHOUSE_MANAGER는 본인이 배정된 창고(`WarehouseMember`)에 배정된 발주. 범위 밖은 403.
- 발주 `status`: `REQUESTED`(승인 대기) → `APPROVED`(승인) → `ASSIGNED`(창고 배정) ↔ `ON_HOLD`(출고 보류). 종결은 `COMPLETED`(전량 또는 부분 출고 종결) / `CANCELED` / `REJECTED`. `warehouse_id`는 요청 시점엔 NULL이고 `ASSIGNED`가 되며 채워진다.
- 항목(`StoreOrderLine`) `status`: `REQUESTED`(출고 전) → `PARTIALLY_SHIPPED`(`0 < shipped < requested`) → `COMPLETED`(`shipped ≥ requested`). 발주가 취소·반려되면 모든 항목이 `CANCELED`. `shipped_quantity` 누적과 `PARTIALLY_SHIPPED`·`COMPLETED` 전환은 출고 배송 완료 처리에서 갱신한다. 할당·피킹 단계는 항목 상태가 아니라 `allocated_quantity`와 `StockAllocation`·`Outbound` 상태로 확인한다.
- `statusReason`: 현재 상태(`REJECTED`, `CANCELED`, `ON_HOLD`)로 바뀔 때 `StatusHistory.reason`에 기록된 사유. 사유가 필요 없는 상태(재개 후 `ASSIGNED` 포함)는 `null`.
- `progressStage`: 점주용 진행 단계. DB에 저장하지 않는 읽기 전용 파생값으로, 발주 상태 + 가장 최근 출고 상태(`latestOutboundStatus`) + 부족 수량 여부로 계산한다. 목록·단건·상세 응답에 포함한다.

  | 발주 상태 | 가장 최근 출고 상태 | progressStage | 점주 표시 문구 |
  |---|---|---|---|
  | REQUESTED | - | PENDING_APPROVAL | 승인 대기 |
  | REJECTED | - | REJECTED | 반려됨 |
  | CANCELED | - | CANCELED | 취소됨 |
  | APPROVED | - | AWAITING_ASSIGNMENT | 승인됨 (창고 배정 중) |
  | ASSIGNED | 없음, CANCELED, READY, PICKING, PICKED | PREPARING | 상품 준비 중 |
  | ASSIGNED | SHIPPED | IN_TRANSIT | 배송 중 |
  | ASSIGNED | DELIVERED | PARTIALLY_DELIVERED | 일부 배송 완료 (남은 수량 준비 중) |
  | ON_HOLD | - | ON_HOLD | 재고 확보 중 |
  | COMPLETED | - | COMPLETED | 배송 완료 (부족 수량 없음) |
  | COMPLETED | - | COMPLETED_PARTIAL | 일부 수량만 배송 완료 (부족 수량 있음) |

  창고 내부 단계(할당·`READY`·`PICKING`·`PICKED`)는 점주에게 "상품 준비 중"으로 묶어 보여준다. 세분화가 필요한 화면은 `latestOutboundStatus`를 쓴다.

## POST /orders (P1)

- 권한: STORE_OWNER, 본인이 배정된 지점만. 창고 관리자·본사 관리자는 대신 등록할 수 없다(ST-03).
- Body: `storeId`(배정된 지점), `requestedDeliveryAt`(선택, ISO-8601, 현재 이후), `note`(선택, ≤1000), `lines[]`(≥1, 같은 SKU 중복 불가: `skuId`(활성 SKU), `requestedQuantity`(1 이상 정수))
- 201. 응답: `storeOrderId, orderNo, storeId, storeName, warehouseId(null), status=REQUESTED, requestedAt, requestedDeliveryAt, note, totalAmount, createdBy, lines[], createdAt`. `lines[]`: `storeOrderLineId, skuId, skuCode, skuName, unit, requestedQuantity, allocatedQuantity(0), shippedQuantity(0), requestedUnitSupplyPrice, lineAmount, status=REQUESTED`
- 에러: 400(필수 누락, `lines` 비어 있음, 수량 1 미만·정수 아님, 같은 SKU 중복, `requestedDeliveryAt` 과거, 길이·형식), 403(STORE_OWNER가 아니거나 배정되지 않은 지점), 404(존재하지 않는 `storeId`·`skuId`), 409 `CONFLICT`(비활성 지점, 비활성 SKU 또는 비활성 상품의 SKU), 409 `SUPPLY_PRICE_MISSING`(SKU `current_supply_price` 없음)
- 규칙:
  - 점주에게 노출되는 판매 가능 SKU(활성 상품의 활성 SKU)만 발주할 수 있다(ST-01). 발주(`StoreOrder`)와 항목(`StoreOrderLine`)을 단일 트랜잭션으로 만든다.
  - 상태 `REQUESTED`, `warehouse_id` NULL, `requested_at`은 서버 시각, 항목의 `allocated_quantity`·`shipped_quantity`는 0, 항목 상태 `REQUESTED`. `order_no`는 서버가 생성(UNIQUE), `created_by`는 토큰의 사용자.
  - `requestedUnitSupplyPrice`는 요청에서 받지 않고 등록 시점의 `ProductSKU.current_supply_price` 스냅샷(이후 단가가 바뀌어도 변하지 않음). `lineAmount = requestedQuantity × requestedUnitSupplyPrice`, `totalAmount` = 항목 금액 합. UNIQUE(store_order_id, sku_id).
  - `StatusHistory`에 기록한다(`NULL` → `REQUESTED`). 등록 후 수량·품목 수정은 지원하지 않는다. 잘못 등록했으면 승인 전에 취소하고 다시 등록한다.

## GET /orders (P1)

- 권한: HQ_ADMIN만. 점주·창고 관리자는 `GET /orders/my`를 쓴다(403).
- Query: `status`(REQUESTED/APPROVED/ASSIGNED/ON_HOLD/COMPLETED/CANCELED/REJECTED), `storeId`, `warehouseId`, `keyword`(주문 번호·지점명 부분 일치), `requestedFrom`, `requestedTo`(ISO-8601), `sort`(기본 `requestedAt,desc`)
- 응답 항목: `storeOrderId, orderNo, storeId, storeName, warehouseId, warehouseName, status, progressStage, requestedAt, requestedDeliveryAt, lineCount, totalAmount, latestOutboundStatus`
- 에러: 400(`status` 값·일시 형식 오류, `requestedFrom`이 `requestedTo`보다 늦음), 403, 404(존재하지 않는 `storeId`·`warehouseId` 필터)
- 규칙: 미배정 발주(`REQUESTED`, `APPROVED`)는 `warehouseId`·`warehouseName`이 `null`. `latestOutboundStatus`는 가장 최근 출고의 상태이며 출고가 없으면 `null`. 항목별 수량은 `/details`. 조회 전용.

## GET /orders/my (P2)

- 권한: STORE_OWNER, WAREHOUSE_MANAGER. 본사 관리자는 403(`GET /orders` 사용).
- Query: `status`, `storeId`(점주가 여러 지점에 배정된 경우), `warehouseId`(창고 관리자가 여러 창고에 배정된 경우), `keyword`(주문 번호), `requestedFrom`, `requestedTo`, `sort`(기본 `requestedAt,desc`)
- 응답 항목: `GET /orders`와 같다.
- 에러: 400, 403(본사 관리자가 호출, 배정되지 않은 지점·창고를 필터로 지정), 404(존재하지 않는 `storeId`·`warehouseId`)
- 규칙: STORE_OWNER는 본인 지점 발주, WAREHOUSE_MANAGER는 담당 창고에 배정된 발주만. 창고 관리자에게는 창고에 배정되기 전 발주(`REQUESTED`, `APPROVED`)와 반려·취소된 미배정 발주가 보이지 않고, 배정 이후(`ASSIGNED`, `ON_HOLD`, `COMPLETED`)와 배정 뒤 취소된 발주만 보인다. 점주는 `status`·`progressStage`·`latestOutboundStatus`로 승인·배정·출고·배송 상태를 확인한다. 조회 전용.

## GET /orders/{orderId} (P1)

- 응답: `storeOrderId, orderNo, storeId, storeName, warehouseId, warehouseName, status, statusReason, progressStage, requestedAt, requestedDeliveryAt, note, lineCount, totalAmount, createdBy, createdByName, createdAt, updatedAt`
- 에러: 400(`orderId` 형식), 403(점주가 자기 지점이 아닌 발주, 창고 관리자가 담당 창고에 배정되지 않은 발주), 404(발주 없음)
- 규칙: 데이터 범위는 위 "공통 정의"를 따른다. 항목별 수량·출고 현황·상태 이력은 `/details`. 조회 전용.

## GET /orders/{orderId}/details (P2)

- 응답: `storeOrderId, orderNo, status, progressStage, items[], outbounds[], statusHistory[]`
  - `items[]`: `storeOrderLineId, skuId, skuCode, skuName, unit, requestedQuantity, allocatedQuantity, shippedQuantity, remainingQuantity, requestedUnitSupplyPrice, lineAmount, status`
  - `outbounds[]`: `outboundId, outboundNo, status, shippedAt, deliveredAt`
  - `statusHistory[]`: `fromStatus, toStatus, reason, changedBy, changedByName, changedAt`
- 에러: 400, 403(`GET /orders/{orderId}`와 같은 범위), 404
- 규칙:
  - `remainingQuantity = requestedQuantity - shippedQuantity`(부분 출고 뒤 후속 출고가 필요한 수량). `allocatedQuantity`는 현재 예약 중인 수량이며 피킹이 끝나면 0으로 돌아간다.
  - `outbounds`는 이 발주의 출고를 생성 순으로 나열하며 취소된 출고도 `CANCELED`로 포함한다. `statusHistory`는 `entity_type = STORE_ORDER` 이력을 시간순으로 반환하며 보류·반려·취소 사유는 `reason`에 담긴다.
  - 항목 `status`는 위 "공통 정의"를 따른다. 조회 전용.

## PATCH /orders/{orderId}/approve (P1)

- 권한: HQ_ADMIN만(HQ-05).
- 응답: `storeOrderId, orderNo, status=APPROVED, updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`REQUESTED`가 아님: 이미 승인·반려·취소, 발주 지점이 비활성, 항목의 SKU 또는 상품이 비활성)
- 규칙: 전이는 `REQUESTED` → `APPROVED`만. 승인 조건은 지점·모든 항목의 SKU·상품이 활성인 것이다. 수량·품목은 이 API에서 수정하지 않는다(바꿔야 하면 반려하거나 취소 후 재등록). 승인 시점에는 창고를 정하지 않는다(`warehouse_id` NULL 유지). 같은 발주를 동시에 승인·반려·취소하면 한쪽만 성공한다. `StatusHistory`에 기록한다(처리자·시각).

## PATCH /orders/{orderId}/reject (P1)

- 권한: HQ_ADMIN만. 창고 발주의 반려는 MVP 이후지만 지점 발주의 반려는 MVP 범위다.
- Body: `reason`(필수, ≤500)
- 응답: `storeOrderId, orderNo, status=REJECTED, statusReason, updatedAt`
- 에러: 400(`reason` 누락·빈 값·길이 초과, `orderId` 형식), 403, 404, 409 `CONFLICT`(`REQUESTED`가 아님)
- 규칙: 전이는 `REQUESTED` → `REJECTED`만. 승인된 발주는 반려할 수 없고 취소(`PATCH /orders/{orderId}/cancel`)를 쓴다. 사유는 `StatusHistory.reason`에 저장한다. 반려된 발주는 종결 상태이며 수정·재제출할 수 없다(점주는 새 발주 등록). 모든 항목이 `CANCELED`가 된다.

## PATCH /orders/{orderId}/cancel (P1)

- 권한: `REQUESTED`는 작성자 STORE_OWNER만(요청 지점의 작성자), `APPROVED`·`ASSIGNED`·`ON_HOLD`는 HQ_ADMIN만. 창고 관리자는 불가. 본사 관리자가 `REQUESTED` 발주를 취소하려면 반려를 쓴다(403).
- Body: `reason`(≤500; 본사가 승인 이후 발주를 취소할 때 필수, 점주가 `REQUESTED`를 취소할 때 선택)
- 응답: `storeOrderId, orderNo, status=CANCELED, statusReason, releasedAllocationCount, canceledOutboundCount, updatedAt`
- 에러: 400(본사 취소 시 `reason` 누락·빈 값·초과, `orderId` 형식), 403(창고 관리자 호출, 점주가 작성자·자기 지점이 아니거나 승인 이후 발주를 취소, 본사가 `REQUESTED` 취소), 404, 409 `CONFLICT`(이미 `CANCELED`·`REJECTED`·`COMPLETED`), 409 `ORDER_IN_PICKING`(피킹 시작 이후 출고 `PICKING`·`PICKED`·`SHIPPED`·`DELIVERED`가 하나라도 있음, 배송 완료된 부분 출고 포함)
- 규칙:
  - 승인 이후 본사 취소는 한 트랜잭션으로 `READY` 출고를 `CANCELED`로(`PATCH /outbounds/{outboundId}/cancel`과 같은 처리), `ALLOCATED` 재고 할당을 `RELEASED`로 바꾸고 해당 재고 행과 발주 항목의 `allocated_quantity`를 줄인다. 보유 수량은 바뀌지 않는다. 함께 취소·해제된 출고·할당의 이력에는 "발주 취소로 인한 자동 처리"를 사유로 남긴다. 이 정리는 출고 도메인 연동 전에는 수행하지 않는다(구현 대비 메모).
  - 취소 사유는 `StatusHistory.reason`에 저장한다. 모든 항목이 `CANCELED`가 된다. 발주 행과 재고 행을 잠근다.
  - 부분 출고 뒤 남은 수량을 더 이상 출고하지 않고 종결하는 방법은 `complete-partial`이다.

## POST /orders/assign (P2)

- 권한: HQ_ADMIN만. 창고 관리자는 배정된 발주를 조회·처리만 한다.
- Body: `storeOrderId`, `warehouseId`(활성 창고), `reason`(재배정, 즉 이미 `ASSIGNED`인 발주의 창고 변경일 때 필수, ≤500)
- 응답: `storeOrderId, orderNo, status=ASSIGNED, warehouseId, warehouseName, updatedAt`
- 에러: 400(필드 누락·형식, 재배정인데 `reason` 누락, 길이 초과), 403, 404(존재하지 않는 `storeOrderId`·`warehouseId`), 409 `CONFLICT`(발주 상태가 `APPROVED`·`ASSIGNED`가 아님, 창고 비활성, 현재 배정된 창고와 같은 창고로 재배정), 409 `ORDER_IN_FULFILLMENT`(재고 할당 `ALLOCATED`가 남아 있거나 취소되지 않은 출고가 있는 발주를 재배정)
- 규칙:
  - 최초 배정: `APPROVED` → `ASSIGNED`, `StoreOrder.warehouse_id`를 채운다.
  - 재배정: `ASSIGNED`의 창고를 다른 창고로 바꾼다. 할당·출고가 없을 때만 가능하다(먼저 `PATCH /allocations/{allocationId}/release`, `PATCH /outbounds/{outboundId}/cancel`). `ON_HOLD` 발주는 재배정할 수 없다(재개 후 가능).
  - 창고 선택 기준(거리·재고·수용량)은 본사 관리자가 판단하며 이 API는 재고 충족 여부를 검증하지 않는다. 알고리즘 기반 자동 배정은 추후 확장 항목이다.
  - `StatusHistory`에 기록한다(최초는 `APPROVED` → `ASSIGNED`, 재배정은 `ASSIGNED` → `ASSIGNED`에 사유와 이전·이후 창고를 `reason`으로 남김).

## PATCH /orders/{orderId}/hold (P2)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고에 배정된 발주(본사 관리자 불가).
- Body: `reason`(필수, ≤500)
- 응답: `storeOrderId, orderNo, status=ON_HOLD, statusReason, updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`ASSIGNED`가 아님), 409 `ORDER_IN_PICKING`(피킹이 시작된 출고 `PICKING`·`PICKED`·`SHIPPED`가 진행 중)
- 규칙: 전이는 `ASSIGNED` → `ON_HOLD`만. `READY` 출고나 재고 할당이 있어도 보류할 수 있으며 예약과 출고는 유지된다. 보류 중에는 새 재고 할당(`POST /allocations`)·출고 생성(`POST /outbounds`)·피킹 시작이 막힌다(발주가 `ASSIGNED`가 아니므로 409). 재고·수량은 바뀌지 않는다. 필요한 재고 보충은 창고 발주(`POST /purchase-orders`)로 별도 요청한다(WH-07). 사유는 `StatusHistory.reason`에 저장한다.

## PATCH /orders/{orderId}/resume (P2)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고에 배정된 발주.
- Body: `reason`(필수, ≤500)
- 응답: `storeOrderId, orderNo, status=ASSIGNED, statusReason(null), updatedAt`
- 에러: 400, 403, 404, 409 `CONFLICT`(`ON_HOLD`가 아님: 보류 중이 아니거나 이미 재개·취소됨, 배정 창고가 비활성)
- 규칙: 전이는 `ON_HOLD` → `ASSIGNED`만. 재고 충분 여부는 검증하지 않는다(재개 후 `POST /allocations`에서 가용 재고가 부족하면 409 `INSUFFICIENT_STOCK`이므로 다시 보류할 수 있음). 보류 중 본사가 취소했으면 재개할 수 없다(409). 재개 사유는 `StatusHistory.reason`에 저장하고 재개 후 `statusReason`은 `null`이다.

## PATCH /orders/{orderId}/complete-partial (P2)

- 권한: WAREHOUSE_MANAGER만, 본인 담당 창고에 배정된 발주(본사 관리자 불가).
- Body: `reason`(필수, ≤500; 재고 불가·공급 중단 등)
- 응답: `storeOrderId, orderNo, status=COMPLETED, statusReason, items[], updatedAt`. `items[]`: `storeOrderLineId, skuCode, requestedQuantity, shippedQuantity, shortageQuantity`
- 에러: 400, 403, 404, 409 `CONFLICT`(`ASSIGNED`가 아님: 보류·완료·취소), 409 `OUTBOUND_IN_PROGRESS`(진행 중 출고 `READY`·`PICKING`·`PICKED`·`SHIPPED`가 있음), 409 `NO_SHORTAGE`(모든 항목의 `shipped_quantity ≥ requested_quantity`)
- 규칙: 전이는 `ASSIGNED` → `COMPLETED`만. 모든 출고가 `DELIVERED` 또는 `CANCELED`여야 하고 항목 중 하나라도 `shipped < requested`여야 한다. 전량 출고된 발주는 배송 완료 처리에서 시스템이 이미 `COMPLETED`로 전환한다. 재고·할당·항목 상태를 바꾸지 않으며(진행 중 출고가 없어 남은 예약도 없음) 부족 수량은 `shortageQuantity`로 계산해 내려준다. 남은 수량은 자동 재발주되지 않고 지점이 필요하면 `POST /orders`로 직접 재발주한다. 사유는 `StatusHistory.reason`에 저장한다.
