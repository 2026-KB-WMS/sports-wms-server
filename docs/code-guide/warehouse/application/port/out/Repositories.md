# 아웃바운드 포트

`warehouse/application/port/out/` · 서비스가 의존하는 인터페이스. 구현은 `adapter/out/`.

## WarehouseRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `existsById`, `existsByWarehouseCode` | 기본 |
| `findByIdForUpdate(id)` | `SELECT ... FOR UPDATE`. 창고 아래 구역 수용량 관계를 확인·변경할 때 **트랜잭션 안에서만** 호출 |
| `search(condition)` | 조건이 null이면 무시. 생성 일시 내림차순 |

## WarehouseSectionRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `deleteById` | 기본 |
| `findByIdForUpdate(id)` | `FOR UPDATE`. 사용 용량을 바꿀 때 트랜잭션 안에서만 |
| `search(condition)` | 구역 코드 오름차순 |
| `findAllByWarehouseId(id)` | 창고의 구역 전체(필터 없음). 재고 잠금 대상용 |
| `existsByWarehouseIdAndSectionCode` | 창고 안 코드 중복 확인 |
| `sumActiveCapacity(warehouseId, parentSectionId, excludeSectionId)` | 같은 상위 아래 **활성** 직속 구역들의 수용량 합. `parentSectionId`가 null이면 창고 최상위 합. `excludeSectionId`가 있으면 그 구역은 뺀다(자기 수용량을 바꿀 때) |
| `existsActiveChild(parentId)` | 활성 직속 하위 구역이 있는지 (비활성화 판단) |
| `existsChild(parentId)` | 상태와 무관하게 직속 하위가 있는지 (삭제 판단) |

## WarehouseMemberRepository

| 메서드 | 설명 |
|---|---|
| `save`, `findById`, `deleteById` | 기본 |
| `search(warehouseId, userId, keyword, page)` | 사용자 이름·로그인 아이디를 붙인 `WarehouseMemberView`의 `PageResult`. keyword는 이름·로그인 아이디 부분 일치(대소문자 무시). 정렬은 페이지 조건을 따른다(기본 배정 일시 내림차순) |
| `findByUserId(userId)` | 사용자의 배정 전체 |
| `existsByWarehouseIdAndUserId` | 중복 배정 확인 |

## StockPresencePort

재고 도메인의 재고 존재 여부(읽기). 구역·창고 비활성화나 삭제 전에 남은 재고(보유·할당)를 확인한다.

| 메서드 | 설명 |
|---|---|
| `hasStockInSection(sectionId)` | 구역에 재고가 있는지 |
| `hasStockInWarehouse(warehouseId)` | 창고에 재고가 있는지 |

구현: `adapter/out/inventory/StockPresenceAdapter`가 재고 도메인의 `InventoryQueryUseCase`를 호출한다.

## WarehouseUsagePort

창고·구역을 참조하는 진행 중 업무 존재 여부(읽기 전용). 진행 중 업무는 소유 도메인의 `*UsageUseCase`가 판단하고, 구역 참조만 조회 조인(ADR-007)이다.

| 메서드 | 판단 기준 |
|---|---|
| `hasInProgressInbounds(warehouseId)` | 입고 ARRIVED·INSPECTING |
| `hasInProgressPurchaseOrders(warehouseId)` | 창고 발주 REQUESTED·CONFIRMED |
| `hasInProgressOutbounds(warehouseId)` | 출고 READY·PICKING·PICKED·SHIPPED. 출고는 창고를 직접 갖지 않아 배정된 **지점 발주의 창고**로 판단 |
| `hasInProgressStoreOrders(warehouseId)` | 배정된 지점 발주 REQUESTED·APPROVED·ASSIGNED·ON_HOLD |
| `isSectionReferenced(sectionId)` | 재고 로트 행(수량 0 포함)이나 입고 검수 항목의 합격·불량 구역 지정이 참조. **입고 상태와 무관**하게 세므로 완료된 입고가 참조한 구역도 삭제할 수 없다 |
