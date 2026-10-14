# Command

`warehouse/application/port/in/command/` · 쓰기 유스케이스의 입력 `record`.

## 흐름

`요청 JSON` → `XxxRequest`(검증) → `toCommand()` → **Command** → `UseCase` → 도메인 객체

부분 수정 커맨드는 null이 아닌 필드만 반영하고, `hasNoChanges()`가 true면 서비스가 `VALIDATION_ERROR`로 거절한다. 경로 변수 ID는 Command에 담지 않고 UseCase 메서드의 첫 인자로 따로 넘긴다.

## WarehouseRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 창고를 등록한다. 창고 코드는 받지 않고 주소의 시·도로 서버가 발급한다 |
| 엔드포인트 | POST `/api/v1/warehouses` |
| 만드는 곳 | `WarehouseRegisterRequest.toCommand()` |
| 쓰는 곳 | `WarehouseService.registerWarehouse` |
| 필드 | name, address, contactNumber, totalCapacity, latitude, longitude |
| 생성자 | 좌표 없는 4인자 생성자(좌표는 null) |

## WarehouseUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 창고 정보를 부분 수정한다. 창고 코드와 운영 상태는 포함하지 않는다 |
| 엔드포인트 | PATCH `/api/v1/warehouses/{warehouseId}` |
| 만드는 곳 | `WarehouseUpdateRequest.toCommand()` |
| 쓰는 곳 | `WarehouseService.updateWarehouse(warehouseId, command)` |
| 필드 | name, address, contactNumber, totalCapacity, latitude, longitude (모두 선택) |
| 메서드 | `hasNoChanges()` |
| 쓰임 | 좌표는 쌍으로만 오며, 생략하면 기존 좌표를 유지한다 |

## WarehouseSectionRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 구역을 등록한다 |
| 엔드포인트 | POST `/api/v1/warehouses/sections` |
| 만드는 곳 | `WarehouseSectionRegisterRequest.toCommand()` |
| 쓰는 곳 | `WarehouseSectionService.registerSection` |
| 필드 | warehouseId, parentSectionId(null이면 최상위), sectionCode, name, sectionType, capacity |

## WarehouseSectionUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 구역의 코드·이름·유형·수용량을 부분 수정한다. 창고·상위 구역·운영 상태는 포함하지 않는다 |
| 엔드포인트 | PATCH `/api/v1/warehouses/sections/{sectionId}` |
| 만드는 곳 | `WarehouseSectionUpdateRequest.toCommand()` |
| 쓰는 곳 | `WarehouseSectionService.updateSection(sectionId, command)` |
| 필드 | sectionCode, name, sectionType, capacity (모두 선택) |
| 메서드 | `hasNoChanges()` |

## WarehouseMemberAssignCommand

| 항목 | 내용 |
|---|---|
| 용도 | 사용자를 창고 관리자로 배정한다 |
| 엔드포인트 | POST `/api/v1/warehouses/managers` |
| 만드는 곳 | `WarehouseMemberAssignRequest.toCommand()` |
| 쓰는 곳 | `WarehouseMemberService.assignManager` |
| 필드 | warehouseId, userId, memberRole |
