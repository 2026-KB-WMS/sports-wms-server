# InventoryAdjustmentService

`inventory/application/service/InventoryAdjustmentService.java` · 구현: `InventoryAdjustmentUseCase` · 클래스 전체 `@Transactional`

실사 결과 등으로 **재고 보유 수량을 직접 조정**한다.

## 의존

`InventoryLotRepository`, `InventoryTransactionRepository`, `SectionCapacityPort`, `InventoryQueryRepository`

## adjust(command, actor)

수량 변경, 구역 사용 용량 증감, 이력 기록이 **한 트랜잭션**이다.

| 순서 | 동작 | 오류 |
|---|---|---|
| 1 | 입력 검증: `afterQuantity` 0 이상, `beforeQuantity` 필수, 전후 수량이 같으면 안 됨, 사유 필수 | 400 `VALIDATION_ERROR` |
| 2 | 재고 행을 **비관적 락**으로 조회 | 404 `INVENTORY_NOT_FOUND` |
| 3 | 구역의 창고가 담당 창고인지 확인(락을 잡은 뒤 같은 트랜잭션에서) | 403 `FORBIDDEN`, 구역이 없으면 404 `SECTION_NOT_FOUND` |
| 4 | `beforeQuantity`가 현재 보유 수량과 같은지 | 409 `STALE_QUANTITY` |
| 5 | `afterQuantity`가 할당 수량 이상인지 | 409 `BELOW_ALLOCATED_QUANTITY` |
| 6 | 증가면 `occupy` + `increase`, 감소면 `vacate` + `decrease` | `SECTION_CAPACITY_EXCEEDED` 등 |
| 7 | 마지막 실사 시각 갱신 | |
| 8 | 저장 후 `ADJUSTMENT` 이력 기록(사유는 `trim`) | |

## 중복 요청 방지

화면에서 본 수량(`beforeQuantity`)이 서버 값과 같을 때만 조정한다. 같은 요청이 중복 전송돼도 두 번째는 `STALE_QUANTITY`로 거절되어 **중복 반영되지 않는다**.

## 주의

- 조정 증가는 구역 수용량을 넘을 수 없다(`SectionCapacityPort.occupy`가 검증).
- 감소는 할당 수량 아래로 내려갈 수 없다(5번).
- 허용 역할은 WAREHOUSE_MANAGER뿐이다(`SecurityConfig`).
