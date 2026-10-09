# 출고(Outbound)·재고 할당(StockAllocation) 도메인 요약

> **기준은 레포다.** 출고 구현을 시작하면서 2026-10-05 Notion "업무 상태 전이도"의 재고 할당·출고 부분을 이 문서로 옮겼다. 이 부분의 상태 전이·권한·부수 효과는 이 문서를 고치고, Notion 전이도의 같은 부분은 참고용으로만 둔다.
> - API 명세: [`docs/api/outbound.md`](../api/outbound.md) (`/api/v1/outbounds` 8개, `/api/v1/allocations` 4개)
> - 구현 일정: WMS 개발 일정 DB의 "[구현] 출고 도메인". 추적 이슈·하위 이슈 번호는 아래 "구현 현황"에 적는다.
> - 지점 발주 쪽 규칙은 [`store-order.md`](store-order.md), 재고 수량 변경은 `InventoryStockUseCase`([ADR-006](../adr/006-inventory-pessimistic-lock.md))다.

## 범위

`outbound` 패키지 = StockAllocation, Outbound, OutboundLine (ERD "출고" 섹션).

- 재고 할당(예약)과 출고(피킹·배송)는 별개 엔티티다. 할당은 `StoreOrderLine`에 로트 재고를 예약하는 기록이고, 출고는 그 할당을 모아 실제로 피킹·배송하는 헤더다.
- 지점 발주(`storeorder`)는 이 도메인이 정의한 포트를 통해 출고 상태를 읽는다. 구현 전에 쓰던 임시 어댑터(`TemporaryStoreOrderOutboundAdapter`)는 실제 어댑터(`StoreOrderOutboundAdapter`)로 교체해 삭제했다.
- 재고 수량 변경(할당·해제·출고 차감)은 재고 도메인의 `InventoryStockUseCase`(`allocate`/`release`/`ship`)로만 한다. 이 도메인은 재고 행을 직접 수정하지 않는다.
- 반품·창고 간 이동·SKU 바코드 검증은 MVP 범위 밖이다.

## 권한

- 재고 할당 생성·해제, 출고 생성·피킹·배송·취소는 담당 창고(`WarehouseMember`)의 WAREHOUSE_MANAGER만 한다. 대상 창고는 발주의 `warehouse_id`다.
- 조회는 HQ_ADMIN과 담당 창고의 WAREHOUSE_MANAGER만 한다. 점주는 호출할 수 없고 진행 상태는 지점 발주 조회의 `progressStage`로 본다.
- 본사 관리자는 출고를 직접 취소하지 않는다. 승인 이후 발주 취소로 `READY` 출고가 함께 취소될 뿐이다.
- 인증·인가 적용됨(#170): 처리 사용자는 토큰 주체이고, 역할은 보안 설정이, 담당 창고 범위는 서비스가 발주를 잠근 직후에 검사한다([`api/authorization.md`](../api/authorization.md), ADR-012).

## 재고 할당(StockAllocation) 상태

| 현재 | 행위 | 다음 | 수행자 | 조건 |
|---|---|---|---|---|
| (없음) | 재고 할당 | ALLOCATED | 창고 관리자(FEFO 자동) | 발주 `ASSIGNED`, 가용 재고 ≥ 잔여 수량, 전체 항목 all-or-nothing |
| ALLOCATED | 할당 해제 | RELEASED | 창고 관리자 | 취소되지 않은 출고에 연결되지 않음, 사유 필수, `released_at` 기록 |
| ALLOCATED | 피킹 완료 | PICKED | 창고 관리자(피킹 완료 처리 시 자동) | 실제 피킹 수량 확정(`picked_quantity`), 재고 차감, 미피킹 예약분 해제 |

- `RELEASED`와 `PICKED`는 종결 상태다. 같은 전이를 다시 요청하면 409 `CONFLICT`이며 부작용을 다시 실행하지 않는다.
- 취소되지 않은 출고(`OutboundLine`)에 연결된 할당은 해제할 수 없다(409 `ALLOCATION_IN_OUTBOUND`). 출고가 취소되면 할당은 `ALLOCATED` 그대로 다시 연결되지 않은 상태가 된다.

## 출고(Outbound) 상태

| 현재 | 행위 | 다음 | 수행자 | 조건 |
|---|---|---|---|---|
| (없음) | 출고 생성 | READY | 창고 관리자 | 발주 `ASSIGNED`, 취소되지 않은 출고에 연결되지 않은 `ALLOCATED` 할당 ≥ 1 |
| READY | 피킹 시작 | PICKING | 창고 관리자 | 발주 `ASSIGNED`, 항목 할당이 모두 `ALLOCATED` |
| PICKING | 피킹 완료 | PICKED | 창고 관리자 | 전 항목 피킹 수량 확정(0 이상, 할당 수량 이하), 전부 0이면 거절 |
| PICKED | 배송 시작 | SHIPPED | 창고 관리자 | 인계 완료, `shipped_at`·`shipped_by` 기록 |
| SHIPPED | 배송 완료 | DELIVERED | 창고 관리자 | 수령 확인, `delivered_at` 기록 |
| READY | 출고 취소 | CANCELED | 창고 관리자 | 사유 필수, 피킹 시작 전에만, 할당은 유지 |

- `DELIVERED`와 `CANCELED`는 종결 상태다.
- 피킹이 시작된 출고(`PICKING`·`PICKED`·`SHIPPED`·`DELIVERED`)가 있으면 발주를 취소·보류할 수 없다(발주 쪽 409 `ORDER_IN_PICKING`).
- 진행 중 출고는 `READY`·`PICKING`·`PICKED`·`SHIPPED`다. 있으면 부분 출고 종결을 막는다(발주 쪽 409 `OUTBOUND_IN_PROGRESS`).

## 재고·부수 효과

1. **할당**은 재고 행과 발주 항목의 `allocated_quantity`만 늘린다. 보유 수량·재고 이력은 건드리지 않는다. FEFO 순서와 all-or-nothing은 API 명세 `POST /allocations`가 기준이다.
2. **해제**는 재고 행과 발주 항목의 `allocated_quantity`를 줄인다. 보유 수량·재고 이력은 불변이다.
3. **피킹 완료**가 재고를 차감하는 유일한 시점이다. 한 트랜잭션으로 재고 행(보유·할당), 구역 사용 용량, `InventoryTransaction`(`OUTBOUND`), 할당(`PICKED`), 출고 항목(`shipped_quantity`, 확정 공급 단가), 발주 항목(`allocated_quantity` 감소, `shipped_quantity` 증가), 출고 상태를 함께 바꾼다. 부족분의 예약은 풀려 가용 재고로 돌아간다.
4. **배송 시작·배송 완료**는 재고·할당을 바꾸지 않는다. 배송 완료는 조건이 맞으면 발주를 `COMPLETED`로 자동 전환한다.
5. **출고 취소**는 재고·할당·발주를 바꾸지 않는다.
6. **발주 취소(승인 이후)**: 지점 발주 서비스가 `StoreOrderOutboundPort.cancelFulfillment`로 `READY` 출고를 `CANCELED`, `ALLOCATED` 할당을 `RELEASED`로 바꾸고 `allocated_quantity`를 줄인다. 이력 사유는 "발주 취소로 인한 자동 처리"다.
7. **부분 출고 종결(`complete-partial`)**: 지점 발주 서비스가 `StoreOrderOutboundPort.releaseUnlinkedAllocations`로 출고에 묶이지 않고 남은 `ALLOCATED` 할당(출고를 만들지 않은 것, 취소된 `READY` 출고의 것, 배송 완료 뒤 추가로 할당한 것)을 `RELEASED`로 바꾸고 `allocated_quantity`를 줄인다. 출고는 취소하지 않는다(진행 중 출고가 없는 것은 서비스 가드가 보장). 이력 사유는 "발주 부분 종결로 인한 자동 해제"다. 취소 연쇄와 같은 내부 해제 로직을 쓴다.
8. 모든 상태 변경은 `StatusHistory`에 기록한다(`entity_type`은 `OUTBOUND`, `STOCK_ALLOCATION`, 시스템 자동 전이는 트리거한 사용자가 처리자). 출고 취소 사유와 할당 해제 사유는 `reason`에 저장하고 응답의 `cancelReason`은 이 이력에서 읽는다.
9. 상태 확인과 변경은 한 트랜잭션에서 대상 행을 잠그고 처리한다. 알림 전송은 이 도메인의 범위가 아니다.

## 지점 발주 연동 교체

`StoreOrderOutboundPort`는 실제 어댑터(`StoreOrderOutboundAdapter`)가 구현하며 아래가 실제로 동작한다. 포트 시그니처는 `storeorder` 쪽이 정의한 그대로 쓴다.

| 포트 메서드 | 실제 구현 | 쓰는 곳 |
|---|---|---|
| `findLatestOutboundStatus`, `findLatestOutboundStatuses` | 발주별 가장 최근(생성 순 마지막) 출고 상태 | `latestOutboundStatus`, `progressStage` |
| `findOutbounds` | 발주에 딸린 출고 목록(취소 포함, 생성 순) | 발주 상세의 `outbounds` |
| `existsPickingStarted` | `PICKING`·`PICKED`·`SHIPPED`·`DELIVERED` 출고 존재 | 취소·보류 가드 `ORDER_IN_PICKING` |
| `existsInProgressOutbound` | `READY`·`PICKING`·`PICKED`·`SHIPPED` 출고 존재 | 부분 종결 가드 `OUTBOUND_IN_PROGRESS` |
| `existsActiveFulfillment` | `ALLOCATED` 할당 또는 취소되지 않은 출고 존재 | 재배정 가드 `ORDER_IN_FULFILLMENT` |
| `cancelFulfillment` | READY 출고 취소 + ALLOCATED 할당 해제 + 수량 감소 | 승인 이후 취소(`releasedAllocationCount`, `canceledOutboundCount`) |
| `releaseUnlinkedAllocations` | 남은 ALLOCATED 할당 해제 + 수량 감소(출고는 그대로) | 부분 종결(`releasedAllocationCount`) |

- 포트가 `storeorder`에 있고 어댑터가 `outbound`에 있는 방향(의존성 역전)을 유지한다. 두 도메인은 서로의 엔티티를 참조하지 않고 ID로만 연결한다([ADR-005](../adr/005-entity-reference-by-id.md)).
- 반대 방향도 필요하다. 할당은 발주 항목의 요청·할당·출고 수량을, 피킹 완료·해제·배송 완료는 발주 항목 수량과 발주 상태를 바꿔야 한다. 이 변경은 `storeorder`가 소유하므로 출고 쪽이 쓸 인바운드 유스케이스(발주·항목 조회, 수량 갱신, `COMPLETED` 전환)를 `storeorder`에 추가해야 한다. 시그니처는 포트·어댑터 이슈에서 정한다.
- 연동 교체를 마쳐 임시 어댑터를 지웠고, 발주 취소 연쇄·`ORDER_IN_PICKING`·`OUTBOUND_IN_PROGRESS`·`progressStage` 실제 값은 `OutboundFlowIntegrationTest`로 검증한다. 보류 항목은 Notion "[보류]" 표 #17이다.
- 지점 발주 쪽 `shipped_quantity` 누적과 항목 상태(`PARTIALLY_SHIPPED`·`COMPLETED`) 전환은 이 도메인이 호출해 완성한다.

## 결정·미결 (2026-10-05)

1. **발주 항목 `shipped_quantity` 누적 시점 (확정).** 문서마다 달랐다. API 명세 `picking/complete`는 피킹 완료 때 발주 항목의 `allocated_quantity`를 줄이고 `shipped_quantity`를 늘린다고 했고, 업무 상태 전이도와 `store-order.md`는 배송 완료 처리에서 갱신한다고 했다. 잔여 수량 `requested - allocated - shipped`로 다음 할당 수량을 계산하므로 `allocated`가 줄 때 `shipped`도 같이 늘어야 그 사이 같은 수량이 다시 할당되지 않는다. **결정**: 수량(`allocated`·`shipped`)은 피킹 완료에서 갱신하고, 항목 상태(`PARTIALLY_SHIPPED`·`COMPLETED`)와 발주 `COMPLETED` 전환은 배송 완료에서 처리한다. 배송 완료 때는 이 출고가 다룬 발주 항목의 상태를 `shipped_quantity`로 다시 계산한다. `store-order.md`·`api/store-order.md`의 해당 문구는 이에 맞춰 고쳤다.
2. **락 순서와 할당의 구역 잠금 (보류, 성능 개선 때 다룬다).** `POST /allocations` 명세의 락 순서는 구역 → 재고 행이지만 `InventoryStockService.allocate`·`release`는 재고 행만 잠그고(`receive`·`ship`은 구역부터 잠근다), 발주 행의 위치는 정해지지 않았다. 지금은 확정하지 않고 기존 재고 서비스 동작을 그대로 쓴다. 출고 서비스는 상태 확인과 변경을 출고·발주 행 잠금 아래에서 하고, 재고 행 잠금은 재고 서비스에 맡긴다. 전역 락 순서 확정과 할당·해제의 구역 잠금 추가 여부는 성능 개선 단계에서 판단하며 Notion "[보류]"에 기록한다.
3. **Notion 개발 일정 페이지의 범위는 12개로 갱신됐다.** 처음에는 12개 중 7개만 적혀 있어 `PATCH /outbounds/{id}/cancel`과 `/allocations` 4개가 빠져 있었다.
4. 배송 실패·수령 거부, 배송 담당자·운송장 기록, 부족 사유 기록 위치, 취소한 출고 재개, 부분 할당, 세트 상품 구성품 예약은 [`api/outbound.md`](../api/outbound.md) "확정 필요"에 있다(출고 번호 형식은 `OB-YYYYMMDD-NNNN`으로 확정, `outbound_no VARCHAR(30)`).

## 구현 현황

트래킹 이슈 #143(Outbound 마일스톤), 하위 이슈 #144 도메인 모델 + 마이그레이션, #145 포트 + 어댑터(발주 연동 교체 포함), #146 서비스 A(재고 할당·해제), #149 서비스 B(출고 생성·피킹 시작·취소), #150 서비스 C(피킹 완료·배송 시작·배송 완료), #147 웹 어댑터, #148 테스트. 서비스는 A → B → C 순으로 구현한다. #144~#150(도메인·포트·서비스 A·B·C)과 #147(웹 어댑터)은 구현·머지를 마쳤고(2026-10-05), #148(테스트)은 동시성·전체 흐름·발주 연동 통합 테스트까지 넣었다. 새 JPQL·서비스·동시성 테스트는 H2와 MySQL 8.4(Hibernate `create-drop` 스키마, V13 마이그레이션은 별도 검증)에서 모두 통과했다. 남은 것: 피킹 완료·재고 부족의 필드별 `errors` 배열, 목록 필터의 존재하지 않는 `warehouseId`·`skuId`·`storeOrderId` 404, 전역 락 순서(성능 개선).

### 설계 결정 (구현 시 따를 것)

- 도메인 모델 폴더는 `domain/entity/`(`StockAllocation`, `Outbound`, `OutboundLine`), `domain/enums/`(`AllocationStatus`, `OutboundStatus`)로 나눈다. 모든 엔티티 참조는 `Long xxxId`만 쓴다(ADR-005).
- 사용자에게 보이는 오류 메시지는 도메인별 `OutboundErrorCode` enum에서 서비스가 던지고, 도메인 모델의 가드 예외는 개발자용 메시지를 인라인으로 둔다.
- 다른 도메인 데이터(지점명·창고명·SKU·로트·구역)는 조회 쿼리에서 ID 기준 읽기 전용 조인으로 가져온다(ADR-007).
- 목록 페이지네이션은 전 도메인 일괄 적용 때까지 보류한다(`data.items`).
- 수량 컬럼은 BIGINT, 단가는 DECIMAL(18,2)다. `created_by`·`allocated_by`·`shipped_by`처럼 회원 도메인이 없어 가리킬 수 없던 FK는 인덱스만 두었다가 V15에서 ALTER TABLE로 추가했다.

## 리뷰 시 자주 놓치는 지점

- 허용되지 않은 전이(예: `PICKED` → `CANCELED`, `READY` → `SHIPPED` 직행, `RELEASED` 할당의 재해제)가 서비스에서 막혀 있는가
- 상태 확인과 변경이 대상 행 잠금 아래 같은 트랜잭션인가, 락 순서가 입고와 같은가(구역 → 재고 행)
- 재고 수량 변경이 `InventoryStockUseCase`를 거치는가(출고 서비스가 재고 행을 직접 수정하지 않는가), 피킹 완료의 부족분 예약 해제와 `OUTBOUND` 이력
- 같은 할당이 두 출고에 묶이지 않는가, 취소된 출고의 할당만 다시 묶이거나 해제되는가
- 사유가 필요한 전이(출고 취소, 할당 해제)의 사유 필수 검증과 `StatusHistory` 기록
- 권한: 쓰기는 담당 창고 관리자만, 조회는 본사·담당 창고, 공급 단가·금액 노출 범위
- 존재하지 않는 출고·할당은 도메인 전용 404 코드(generic `NOT_FOUND` 아님)
- 발주가 `ASSIGNED`가 아닐 때(보류·취소) 할당·출고 생성·피킹 시작이 막히는가
- 임시 어댑터가 제거되고 포트 6개가 모두 실제 어댑터로 연결됐는가
