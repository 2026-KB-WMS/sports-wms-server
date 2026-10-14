# InventoryStockService

`inventory/application/service/InventoryStockService.java` · 구현: `InventoryStockUseCase` · 클래스 전체 `@Transactional`

**입고·출고 도메인이 재고 수량을 바꾸는 유일한 경로.** HTTP 엔드포인트는 없고, 호출자(입고·출고 서비스)의 트랜잭션에 참여한다.

## 의존

`InventoryLotRepository`, `InventoryTransactionRepository`, `LotRepository`, `SectionCapacityPort`, `SkuStatusPort`

## 원칙

- **all-or-nothing**: 명령을 순서대로 처리하다 하나라도 실패하면 예외로 트랜잭션 전체가 롤백된다.
- **잠금 순서**: 구역 행(section_id 오름차순) → 재고 행(inventory_lot_id 오름차순). 입고·출고 모두 고정해 교착을 막는다(ADR-006).
- 보유 수량이 바뀌는 `receive`, `ship`만 이력을 남기고, `allocate`, `release`는 남기지 않는다.

## 메서드

| 메서드 | 호출자 | 설명 |
|---|---|---|
| `receive(commands)` | 입고 완료 | 구역 + 로트 재고 행에 보유 수량을 더한다. 행이 없으면 만든다 |
| `allocate(commands)` | 출고 할당 | 가용 수량에서 할당 수량을 늘린다 |
| `release(commands)` | 출고 취소 등 | 할당 수량을 줄인다 |
| `ship(commands)` | 피킹 완료 | 할당 전체를 풀고 피킹 수량만큼 보유 수량을 차감한다 |

## receive 동작

1. 명령을 (구역, 로트) 순으로 정렬하고 **관련 구역을 먼저 잠근다**. 같은 (구역, 로트)의 첫 입고가 동시에 와도 재고 행이 하나만 만들어진다(구역 잠금을 기다린 두 번째 요청은 첫 요청이 만든 행을 찾는다).
2. 명령마다: 수량 > 0 확인 → 로트 존재(`LOT_NOT_FOUND`) → 로트가 AVAILABLE(`LOT_NOT_AVAILABLE`) → SKU 활성(`SKU_NOT_ACTIVE`) → 기존 행 조회(없으면 `InventoryLot.open`) → 기존 행과 품질 상태가 다르면 409 → `occupy` → `increase` → 저장 → `INBOUND` 이력

## allocate 동작

행들을 ID 오름차순으로 잠근 뒤 명령마다: 수량 > 0 → 로트 AVAILABLE(`LOT_NOT_AVAILABLE`) → SKU 활성 → `canAllocate`(불량이거나 가용 부족이면 `INSUFFICIENT_STOCK`, 요청·가용 수량을 메시지에 포함) → `allocate`. 한 행이라도 부족하면 **전체 실패**.

## release 동작

수량 > 0, 할당 수량보다 많이 해제하면 409. 로트·SKU 상태는 확인하지 않는다.

## ship 동작

1. 입고와 같은 잠금 순서를 지키려고, 재고 행의 구역을 **먼저 읽어 구역부터 잠근다**.
2. 행을 ID 오름차순으로 잠근다. 없으면 `INVENTORY_NOT_FOUND`(재고 ID 포함).
3. 명령마다: 할당 수량 > 0, `0 <= 피킹 수량 <= 할당 수량` → 재고의 할당 수량이 출고 할당 수량 이상인지(`canRelease`) → **부족분(`allocated - picked`)만큼 `release`**(다시 가용 재고가 된다) → 피킹 수량이 > 0이면 `ship`, 구역 `vacate`, `OUTBOUND` 이력.

## 관찰

- `ship`은 로트·SKU 상태를 확인하지 않는다(할당 시점에만 확인). 할당 후 로트가 격리돼도 피킹 반영은 진행된다. 다만 로트 격리는 할당이 남아 있으면 막힌다(`LOT_HAS_ALLOCATION`).
