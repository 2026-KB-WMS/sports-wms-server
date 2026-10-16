# LotService

`inventory/application/service/LotService.java` · 구현: `LotUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

로트 조회, 상태 변경, 입고 시 로트 find-or-create.

## 의존

`LotRepository`, `InventoryLotRepository`, `InventoryQueryRepository`, `StatusHistoryUseCase`

## 창고 관리자 가시성

창고 관리자(WAREHOUSE_MANAGER)에게는 **담당 창고에 재고 또는 입고 완료 이력이 있는 로트만** 보인다. 본사는 전체.

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `getLots(condition, page, actor)` | 읽기 | `skuId`·`supplierId` 필터 대상이 없으면 404(`SKU_NOT_FOUND`, `SUPPLIER_NOT_FOUND`). 범위는 `actor.warehouseScope(null)` |
| `getLot(lotId, actor)` | 읽기 | 없으면 `LOT_NOT_FOUND`. 담당 창고 범위에 보이지 않는 로트면 **403** (404가 아님) |
| `getLotInbounds(lotId, actor)` | 읽기 | 로트가 입고된 이력. 로트 없으면 `LOT_NOT_FOUND`. 창고 관리자에게는 담당 창고의 입고만 |
| `changeLotStatus(lotId, command)` | 쓰기 | 아래 규칙 |
| `findOrRegister(command)` | 쓰기 | 아래 규칙 |

## changeLotStatus 규칙

허용 역할은 HQ_ADMIN뿐이다(여러 창고가 공유하는 마스터라서).

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 목표 상태가 null이거나 EXPIRED | 400 |
| 2 | 사유가 비었거나 500자 초과 | 400 |
| 3 | 처리 사용자 null | 400 |
| 4 | **로트 행 비관적 락** 조회 | 404 `LOT_NOT_FOUND` |
| 5 | `canTransitionTo(target)` | 409 `INVALID_LOT_STATUS_TRANSITION` |
| 6 | AVAILABLE로 되돌리는데 유통기한이 지남 | 409 `INVALID_LOT_STATUS_TRANSITION` |
| 7 | 이 로트의 재고 행을 ID 오름차순으로 잠금 | |
| 8 | DISPOSED인데 보유·할당 수량이 남음 | 409 `LOT_HAS_STOCK` |
| 9 | QUARANTINED인데 할당 수량이 남음 | 409 `LOT_HAS_ALLOCATION` |
| 10 | 상태 변경·저장, **상태 이력(`STATUS LOT`)에 처리자와 사유 기록** | |

잠금 순서는 로트 행 → 재고 행이다. 할당·조정은 재고 행만 잠그므로, 재고 행을 잠근 상태에서 수량 0을 확인하면 그 사이 수량이 바뀌지 않는다.

## findOrRegister 규칙 (ADR-004)

입고 검수 트랜잭션에서 호출한다.

1. 제조일과 유통기한이 둘 다 있고 유통기한이 제조일보다 이르면 400
2. (SKU, 공급처, 로트 번호)로 조회해 있으면 `verifySameLot`: 로트가 AVAILABLE이 아니면 `LOT_NOT_AVAILABLE`, 원가가 다르면 `LOT_UNIT_COST_MISMATCH`, 제조일·유통기한이 다르면 `LOT_DATE_MISMATCH`. 통과하면 **재사용**
3. 없으면 AVAILABLE로 새로 만든다
