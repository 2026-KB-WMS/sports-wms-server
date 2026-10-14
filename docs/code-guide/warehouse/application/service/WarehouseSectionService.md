# WarehouseSectionService

`warehouse/application/service/WarehouseSectionService.java` · 구현: `WarehouseSectionUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

구역 등록·조회·수정·활성화·비활성화·삭제. **수용량 계층 규칙**이 핵심이다.

## 의존

`WarehouseSectionRepository`, `WarehouseRepository`, `StockPresencePort`, `WarehouseUsagePort`

## 수용량 규칙

- 같은 상위(또는 창고 최상위) 아래 **활성** 구역들의 수용량 합은 상위 수용량을 넘을 수 없다 → `PARENT_CAPACITY_EXCEEDED`
- 상위는 `parentSectionId`가 있으면 그 구역의 `capacity`, 없으면 창고의 `totalCapacity`
- 비활성 구역은 합계에 포함하지 않는다
- 합계는 `sumActiveCapacity`로 구하며 `requireWithinUpperCapacity`(private)가 검사한다

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerSection(command)` | 쓰기 | 아래 규칙 |
| `getSections(condition)` | 읽기 | 필터의 창고·상위 구역 존재 확인(`WAREHOUSE_NOT_FOUND`, `PARENT_SECTION_NOT_FOUND`), 구역 유형 허용 값 검사 후 검색 |
| `getSections(condition, actor)` | 읽기 | 창고를 지정하면 `requireWarehouseAccess`. **지정하지 않으면 본사만** 가능(그 외 403) |
| `getSection(id)` | 읽기 | 단건. 없으면 `SECTION_NOT_FOUND`. 내부 조회용 |
| `getSection(id, actor)` | 읽기 | 구역이 속한 창고에 접근 권한이 없으면 403 |
| `updateSection(id, command)` | 쓰기 | 아래 규칙 |
| `deactivateSection(id)` | 쓰기 | 아래 규칙 |
| `activateSection(id)` | 쓰기 | 아래 규칙 |
| `deleteSection(id)` | 쓰기 | 아래 규칙 |

## registerSection 규칙

1. 구역 유형 허용 값 검사 (`VALIDATION_ERROR`)
2. **창고 행 잠금** 후 창고 확인(없음 `WAREHOUSE_NOT_FOUND`, 비활성 409)
3. 상위 구역이 있으면: 없음 `PARENT_SECTION_NOT_FOUND`, 다른 창고 소속 400, 비활성 409
4. 같은 창고 안 구역 코드 중복 → `DUPLICATE_SECTION_CODE`
5. 수용량 합 검증 → 저장

## updateSection 규칙

변경 필드가 없으면 `VALIDATION_ERROR`. **구역 행을 잠근 뒤** 처리한다.

| 필드 | 규칙 |
|---|---|
| sectionCode | 바뀌었을 때만 중복 검사 |
| name | 값이 있으면 변경 |
| sectionType | 허용 값 검사 후 변경 |
| capacity | 현재 사용 용량보다 작으면 `CAPACITY_BELOW_USAGE`. 창고 행을 잠근 뒤(구역 → 창고 순서) 활성 하위 합보다 작으면 `CAPACITY_BELOW_CHILDREN`. **늘리는 경우이고 활성 구역일 때만** 상위 한도 검사 |

## 상태·삭제 규칙

| 동작 | 규칙 |
|---|---|
| `deactivateSection` | 구역 행 잠금. 이미 비활성 409. 재고 있음 `SECTION_HAS_INVENTORY`. **활성** 하위 구역 있음 `SECTION_HAS_CHILDREN` |
| `activateSection` | 구역 행 잠금 후 창고 행 잠금. 이미 활성 409. 창고 비활성 409. 상위 구역 비활성 409. 활성화하면 형제 합에 자기 수용량이 더해지므로 상위 한도 재검사 |
| `deleteSection` | 구역 행 잠금. 재고 있음 `SECTION_HAS_INVENTORY`, **비활성 포함** 하위 구역 있음 `SECTION_HAS_CHILDREN`, 재고 로트·입고 검수 항목이 참조 `SECTION_IN_USE`. 이력이 남는 구역은 비활성화를 쓴다 |

## 잠금 순서

구역 → 창고 순서로 잠근다(등록은 창고만). 입고 적치가 구역을 먼저 잠그므로 같은 순서를 지켜 교착을 막는다.
