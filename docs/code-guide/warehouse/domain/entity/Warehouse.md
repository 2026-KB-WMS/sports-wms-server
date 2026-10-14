# Warehouse

`warehouse/domain/entity/Warehouse.java`

재고를 보관하고 입고·출고를 수행하는 물류 거점 마스터. 순수 도메인 객체이며 JPA 엔티티는 `WarehouseJpaEntity`다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| warehouseId | Long | PK |
| warehouseCode | String | 창고 코드. 서버가 주소의 시·도로 발급(예: `WH-SU-001`) |
| name | String | 창고명 |
| address | String | 주소. 시·도로 시작해야 한다 |
| latitude, longitude | BigDecimal | 위도·경도. 소수 7자리로 반올림해 저장. null 허용 |
| contactNumber | String | 연락처 |
| totalCapacity | BigDecimal | 전체 수용량. 생성 시 null이면 0 |
| status | WarehouseStatus | ACTIVE / INACTIVE. 생성 시 null이면 ACTIVE |
| createdAt, updatedAt | LocalDateTime | 생성·수정 시각 |

## 메서드

| 메서드 | 설명 |
|---|---|
| `register(code, name, address, contactNumber, totalCapacity)` | ACTIVE 상태로 생성. 좌표는 따로 `changeCoordinates`로 넣는다 |
| `reissueCode(code)` | 주소의 시·도가 바뀌어 코드를 **다시 발급할 때만** 사용 |
| `changeName`, `changeAddress`, `changeContactNumber`, `changeTotalCapacity` | 필드 변경 |
| `changeCoordinates(lat, lon)` | 소수 7자리로 맞춰 쌍으로 변경. 주소만 바꿔도 기존 좌표는 유지된다 |
| `activate()`, `deactivate()`, `isActive()` | 상태 변경·조회 |

비활성화 가능 여부(재고·진행 중 업무 없음)는 `WarehouseService`가 검사한다.
