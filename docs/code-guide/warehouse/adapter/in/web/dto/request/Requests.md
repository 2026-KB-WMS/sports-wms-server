# 요청 DTO (Request)

`warehouse/adapter/in/web/dto/request/` · 요청 바디 `record`.

## 흐름

`요청 JSON` → `@Valid`(형식·길이) → `toCommand()` → Command → 서비스

- 업무 규칙(중복, 존재, 상태, 수용량)은 서비스에서 검증한다.
- 수정 요청에서 바꿀 수 없는 필드(`warehouseCode`, `isActive`, `warehouseId`, `parentSectionId`)는 검증 애노테이션 대신 `toCommand()`가 값이 있으면 **400 `VALIDATION_ERROR`**를 던진다(명세가 400을 요구).
- 위도·경도는 `@AssertTrue isCoordinatePairValid`로 **쌍으로만** 받는다.

## WarehouseRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 창고 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/warehouses` |
| 변환 | `toCommand()` → `WarehouseRegisterCommand` |
| 필드 | warehouseName(필수, 최대 100), address(필수, 최대 500), contactNumber(최대 30, `@PhoneNumber`), totalCapacity(필수, 0 이상, 소수 3자리까지), latitude(-90~90), longitude(-180~180) |
| 주의 | 창고 코드는 요청에 없다. 주소가 시·도로 시작하지 않으면 서비스에서 400 |

## WarehouseUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 창고 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/warehouses/{warehouseId}` |
| 변환 | `toCommand()` → `WarehouseUpdateCommand` |
| 필드 | warehouseName, address, contactNumber, totalCapacity, latitude, longitude, warehouseCode, isActive. 모두 선택 |
| 추가 검사 | `warehouseCode`나 `isActive`가 있으면 거부. 위도·경도는 쌍으로만 |
| 주의 | 활성 상태는 `/activate`, `/deactivate` 엔드포인트로만 바꾼다 |

## WarehouseSectionRegisterRequest

| 항목 | 내용 |
|---|---|
| 용도 | 구역 등록 요청 바디 |
| 엔드포인트 | POST `/api/v1/warehouses/sections` |
| 변환 | `toCommand()` → `WarehouseSectionRegisterCommand` |
| 필드 | warehouseId(필수), parentSectionId, sectionCode(필수, 최대 50), sectionName(필수, 최대 100), sectionType(필수), capacity(필수, 0 이상, 소수 3자리까지) |
| 주의 | JSON 필드 `sectionName`이 Command의 `name`이 된다. `sectionType` 허용 값 검사는 서비스가 한다 |

## WarehouseSectionUpdateRequest

| 항목 | 내용 |
|---|---|
| 용도 | 구역 부분 수정 요청 바디 |
| 엔드포인트 | PATCH `/api/v1/warehouses/sections/{sectionId}` |
| 변환 | `toCommand()` → `WarehouseSectionUpdateCommand` |
| 필드 | sectionCode(최대 50), sectionName(최대 100), sectionType, capacity(0 이상), warehouseId, parentSectionId, isActive. 모두 선택 |
| 추가 검사 | `warehouseId`, `parentSectionId`, `isActive`가 있으면 거부 |

## WarehouseMemberAssignRequest

| 항목 | 내용 |
|---|---|
| 용도 | 창고 관리자 배정 요청 바디 |
| 엔드포인트 | POST `/api/v1/warehouses/managers` |
| 변환 | `toCommand()` → `WarehouseMemberAssignCommand` |
| 필드 | warehouseId(필수), userId(필수), memberRole(필수) |
| 주의 | `memberRole` 허용 값(MANAGER) 검사는 서비스가 한다 |
