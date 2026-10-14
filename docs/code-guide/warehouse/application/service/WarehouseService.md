# WarehouseService

`warehouse/application/service/WarehouseService.java` · 구현: `WarehouseUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

창고 등록·조회·수정·활성화·비활성화와 내 창고 조회.

## 의존

`WarehouseRepository`, `WarehouseMemberRepository`, `WarehouseSectionRepository`, `WarehouseSectionCapacityUseCase`, `StockPresencePort`, `WarehouseUsagePort`, `LocationCodeIssuer`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerWarehouse(command)` | 쓰기 | 주소에서 시·도 판별(`RegionCode.fromAddress`, 실패 시 400) → 창고 코드 발급 → 생성 → 좌표 반영 → 저장 |
| `getWarehouses(condition)` | 읽기 | 검색 |
| `getWarehouse(id)` | 읽기 | 단건. 없으면 `WAREHOUSE_NOT_FOUND`. **다른 도메인의 내부 조회용**(권한 검사 없음) |
| `getWarehouse(id, actor)` | 읽기 | `actor.requireWarehouseAccess(id)` 후 조회. 담당 창고가 아니면 403 |
| `updateWarehouse(id, command)` | 쓰기 | 아래 규칙 |
| `deactivateWarehouse(id)` | 쓰기 | 아래 규칙 |
| `activateWarehouse(id)` | 쓰기 | 이미 활성이면 409 `CONFLICT`. 그 외 검사 없음 |
| `getMyWarehouses(userId)` | 읽기 | 사용자의 배정 목록을 창고 정보와 합쳐 `WarehouseMembershipSummary`로 반환 |

## updateWarehouse 규칙

변경 필드가 없으면 `VALIDATION_ERROR`. **창고 행을 비관적 락으로 잠근 뒤** 처리한다(구역 등록·수정과 겹치지 않게).

| 필드 | 규칙 |
|---|---|
| name, contactNumber | 값이 있으면 변경 |
| address | 시·도 판별(실패 시 400) 후 변경. **발급 형식 코드를 가진 창고가 다른 시·도로 옮기면 새 시·도의 다음 번호로 코드를 재발급**한다. 같은 시·도 안의 수정이나 직접 입력된 옛 코드는 바꾸지 않는다 |
| latitude, longitude | **둘 다** 있을 때만 변경 |
| totalCapacity | 활성 최상위 구역 수용량 합보다 작으면 `CAPACITY_BELOW_CHILDREN` |

## deactivateWarehouse 규칙

1. 창고 없음 → `WAREHOUSE_NOT_FOUND`, 이미 비활성 → 409 `CONFLICT`
2. 창고의 **모든 구역 행을 section_id 오름차순으로 잠근다**(입고 적치와 같은 잠금 순서로 교착 방지)
3. 재고가 남아 있거나, 진행 중인 입고·창고 발주·출고·지점 발주가 하나라도 있으면 `WAREHOUSE_IN_USE`
4. 통과하면 비활성화

구역은 같이 비활성화하지 않는다. 비활성 창고의 구역에는 적치가 막히고(`WAREHOUSE_INACTIVE`), 구역을 활성화하려면 창고가 먼저 활성이어야 한다.
