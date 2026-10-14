# 조회 결과 (Result)

`warehouse/application/port/in/result/` · 서비스가 돌려주는 조회용 `record`.

## CodeItem

| 항목 | 내용 |
|---|---|
| 용도 | 고정 코드 목록의 항목 하나 |
| 엔드포인트 | GET `/warehouses/management-types`, GET `/warehouses/section-types` |
| 만드는 곳 | `WarehouseCodeService` |
| 쓰는 곳 | `CodeItemResponse.from` |
| 필드 | code, name |

## WarehouseMemberView

| 항목 | 내용 |
|---|---|
| 용도 | 창고 관리자 배정 한 건과 대상 사용자의 이름·로그인 아이디 |
| 엔드포인트 | POST·GET `/warehouses/managers` |
| 만드는 곳 | 목록은 `WarehouseMemberJpaRepository.search`가 사용자 테이블을 ID로 조인해 채우고, 배정 직후에는 `WarehouseMemberService.assignManager`가 `of(member, userName, loginId)`로 채운다 |
| 쓰는 곳 | `WarehouseMemberController.toResponse` → `WarehouseMemberResponse.of(view, warehouseName)` |
| 필드 | warehouseMemberId, warehouseId, userId, userName, loginId, memberRole, assignedAt |
| 쓰임 | **창고 이름은 포함하지 않는다.** 웹 어댑터가 창고를 조회해 붙인다 |

## WarehouseMembershipSummary

| 항목 | 내용 |
|---|---|
| 용도 | 로그인한 창고 관리자 본인의 소속 창고 정보. 창고 정보와 배정 정보를 함께 담는다 |
| 엔드포인트 | GET `/warehouses/my` |
| 만드는 곳 | `WarehouseService.getMyWarehouses` |
| 쓰는 곳 | `WarehouseMembershipResponse.from` |
| 필드 | warehouseId, warehouseCode, name, address, contactNumber, totalCapacity, isActive, warehouseMemberId, memberRole, assignedAt |
