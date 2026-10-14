# WarehouseMember

`warehouse/domain/entity/WarehouseMember.java`

사용자와 창고의 소속·업무 역할을 연결하는 매핑. 한 사용자가 여러 창고에 배정될 수 있다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| warehouseMemberId | Long | PK |
| warehouseId | Long | 창고 |
| userId | Long | 사용자 (auth 도메인) |
| memberRole | String | 담당 역할 코드. 허용 값은 `WarehouseManagementType` (현재 MANAGER) |
| assignedAt | LocalDateTime | 배정 시각 |
| createdAt | LocalDateTime | 생성 시각 (`updated_at` 없음) |

## 메서드

| 메서드 | 설명 |
|---|---|
| `assign(warehouseId, userId, memberRole, assignedAt)` | 배정 매핑 생성 |

이 매핑이 `AuthenticatedUser.warehouseIds`(담당 창고 범위)의 근거가 된다. 같은 (창고, 사용자) 조합은 유니크(`uk_warehouse_member_warehouse_user`)다.
