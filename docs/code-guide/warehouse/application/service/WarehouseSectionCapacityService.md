# WarehouseSectionCapacityService

`warehouse/application/service/WarehouseSectionCapacityService.java` · 구현: `WarehouseSectionCapacityUseCase` · 클래스 전체 `@Transactional`

구역의 **사용 용량(`currentCapacity`) 증감**. 재고 수량이 바뀔 때 **재고 도메인이 같은 트랜잭션 안에서 호출**하는 내부 서비스이며 HTTP 엔드포인트는 없다.

## 의존

`WarehouseSectionRepository`, `WarehouseRepository`

## 잠금 순서 (교착 방지)

재고 수량을 바꾸는 트랜잭션은 다음 순서를 지킨다.

1. `lock(sectionIds)`로 관련 **구역 행을 section_id 오름차순**으로 먼저 잠근다
2. 그다음 재고 행을 잠근다

모든 흐름이 같은 순서를 지켜 교착을 막는다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `lock(sectionIds)` | 중복을 제거하고 오름차순으로 정렬해 각각 `findByIdForUpdate`. 없는 구역이면 `SECTION_NOT_FOUND` |
| `occupy(sectionId, quantity)` | `quantity <= 0`이면 아무것도 안 함. 구역을 잠그고: 비활성 구역 → `SECTION_INACTIVE`, 비활성 창고 → `WAREHOUSE_INACTIVE`, 수용량 초과 → `SECTION_CAPACITY_EXCEEDED`(남은 용량을 메시지에 포함). 통과하면 사용 용량 증가 |
| `vacate(sectionId, quantity)` | `quantity <= 0`이면 아무것도 안 함. 구역을 잠그고 사용 용량 감소(0 아래로 내려가지 않음). 비활성 상태 검사는 하지 않는다 |
| `lock(sectionId)` (private) | 단건 비관적 락 조회 |

## 사용처

재고 도메인의 `SectionCapacityAdapter`(포트 `sectionCapacityPort`)가 이 유스케이스를 호출한다.

| 호출 서비스 | 사용 |
|---|---|
| `InventoryStockService` | 입고 적치 시 `lock` → `occupy`, 출고 피킹 차감 시 `lock` → `vacate` |
| `InventoryAdjustmentService` | 재고 조정의 증감분에 따라 `occupy` / `vacate` |

반출(`vacate`)은 비활성 구역·창고에서도 가능해야 하므로 상태 검사를 하지 않는다.
