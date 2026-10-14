# WarehouseSection

`warehouse/domain/entity/WarehouseSection.java`

창고 내부의 실제 보관 구역(구역·랙 등)을 **계층적으로** 관리한다. 상위 구역은 `Long parentSectionId`로만 참조한다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| sectionId | Long | PK |
| warehouseId | Long | 소속 창고 |
| parentSectionId | Long | 상위 구역. null이면 창고 최상위 |
| sectionCode | String | 구역 코드 (창고 안에서 유니크) |
| name | String | 구역명 |
| sectionType | String | 구역 유형 코드. 허용 값은 `WarehouseSectionType` (ZONE, RACK, DEFECT) |
| capacity | BigDecimal | 수용량. null이면 0 |
| currentCapacity | BigDecimal | 현재 사용 용량. 재고가 적치·반출될 때 증감. null이면 0 |
| status | WarehouseStatus | 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(warehouseId, parentSectionId, sectionCode, name, sectionType, capacity)` | ACTIVE로 생성 |
| `isRoot()` | `parentSectionId == null` |
| `availableCapacity()` | `capacity - currentCapacity`. **저장하지 않고 계산**한다 |
| `canOccupy(quantity)` | 적치해도 수용량을 넘지 않는지 |
| `occupy(quantity)` | 사용 용량 증가. 수용량 검증은 호출 전에 `canOccupy`로 한다 |
| `vacate(quantity)` | 사용 용량 감소. **0 아래로는 내려가지 않는다** |
| `changeSectionCode`, `changeName`, `changeSectionType`, `changeCapacity` | 필드 변경 |
| `activate()`, `deactivate()`, `isActive()` | 상태 변경·조회 |

수용량 합계·상위 한도 검증과 락은 `WarehouseSectionService`, 사용 용량 증감은 `WarehouseSectionCapacityService`가 맡는다.
