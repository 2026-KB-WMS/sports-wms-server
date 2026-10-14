# 응답 DTO (Response)

`warehouse/adapter/in/web/dto/response/` · 응답 `record`. JSON 필드는 camelCase.

## 흐름

서비스가 돌려준 도메인 객체·Result → 컨트롤러가 `from` / `of`로 변환 → `ApiResponse`로 감싸 반환. 목록은 `ItemsResponse`(`data.items`).

## 창고

### WarehouseResponse

| 항목 | 내용 |
|---|---|
| 용도 | 창고 한 건의 전체 정보. 등록·수정·활성화·비활성화·단건 조회에 공통 |
| 엔드포인트 | POST `/warehouses`(201), GET·PATCH `/warehouses/{id}`, PATCH `.../activate`, `.../deactivate` |
| 만드는 곳 | `WarehouseController` → `from(Warehouse)` |
| 필드 | warehouseId, warehouseCode, warehouseName, address, latitude, longitude, contactNumber, totalCapacity, isActive, createdAt, updatedAt |

### WarehouseSummaryResponse

| 항목 | 내용 |
|---|---|
| 용도 | 전체 창고 목록의 항목 |
| 엔드포인트 | GET `/warehouses` |
| 만드는 곳 | `WarehouseController.getWarehouses` → `from(Warehouse)` |
| 필드 | warehouseId, warehouseCode, warehouseName, address, latitude, longitude, contactNumber, totalCapacity, isActive |

### WarehouseMembershipResponse

| 항목 | 내용 |
|---|---|
| 용도 | 로그인한 창고 관리자의 소속 창고 한 건(창고 정보 + 배정 정보) |
| 엔드포인트 | GET `/warehouses/my` |
| 만드는 곳 | `WarehouseController.getMyWarehouses` → `from(WarehouseMembershipSummary)` |
| 필드 | warehouseId, warehouseCode, warehouseName, address, contactNumber, totalCapacity, isActive, warehouseMemberId, memberRole, assignedAt |

## 구역

### WarehouseSectionResponse

| 항목 | 내용 |
|---|---|
| 용도 | 구역 한 건. 등록·수정·활성화·비활성화·조회에 공통 |
| 엔드포인트 | POST·GET·PATCH `/warehouses/sections...`, GET `/warehouses/{id}/sections` |
| 만드는 곳 | `WarehouseSectionController.toResponse` → `from(section, warehouseName, parentSectionCode)` |
| 필드 | sectionId, warehouseId, warehouseName, parentSectionId, parentSectionCode, sectionCode, sectionName, sectionType, capacity, currentCapacity, availableCapacity, isActive, createdAt, updatedAt |
| 주의 | `availableCapacity`는 `capacity - currentCapacity`를 **계산한 값**(저장 안 함). 창고명·상위 구역 코드는 컨트롤러가 조회해 채운다 |

### WarehouseSectionDeleteResponse

| 항목 | 내용 |
|---|---|
| 용도 | 구역 삭제 결과 |
| 엔드포인트 | DELETE `/warehouses/sections/{sectionId}` |
| 필드 | sectionId |

## 창고 관리자

### WarehouseMemberResponse

| 항목 | 내용 |
|---|---|
| 용도 | 창고 관리자 배정 한 건. 사용자 정보는 이름·로그인 아이디만 포함(비밀번호 등 민감 정보 제외) |
| 엔드포인트 | POST·GET `/warehouses/managers` |
| 만드는 곳 | `WarehouseMemberController.toResponse` → `of(view, warehouseName)` |
| 필드 | warehouseMemberId, warehouseId, warehouseName, userId, userName, loginId, memberRole, assignedAt |

### WarehouseMemberReleaseResponse

| 항목 | 내용 |
|---|---|
| 용도 | 배정 해제 결과 |
| 엔드포인트 | DELETE `/warehouses/managers/{warehouseMemberId}` |
| 필드 | warehouseMemberId |

## 코드

### CodeItemResponse

| 항목 | 내용 |
|---|---|
| 용도 | 고정 코드 목록 항목 |
| 엔드포인트 | GET `/warehouses/management-types`, GET `/warehouses/section-types` |
| 만드는 곳 | `WarehouseCodeController` → `from(CodeItem)` |
| 필드 | code, name |
