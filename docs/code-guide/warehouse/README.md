# warehouse 코드 가이드

`src/main/java/com/kb/wms/warehouse/`의 패키지 구조를 따라 클래스별 설명을 모았다. 같은 폴더의 `.md`가 같은 폴더의 `.java`에 대응한다.

## 도메인 개요

창고, 창고 안의 **계층형 구역**, 창고 관리자 배정을 관리한다. 구역은 수용량과 현재 사용 용량을 가지며, 재고가 적치·반출될 때 재고 도메인이 용량 서비스를 호출해 증감시킨다.

## 읽는 순서 추천

1. `domain/entity/` → `domain/enums/`
2. `application/service/WarehouseSectionService.md` : 수용량 계층 규칙과 잠금 순서
3. `application/service/WarehouseService.md`, `WarehouseSectionCapacityService.md`
4. `adapter/in/web/` : 엔드포인트와 허용 역할
5. `application/port/out/Repositories.md` → `adapter/out/`

## 문서 위치

| 패키지 | 문서 |
|---|---|
| `domain/entity` | Warehouse, WarehouseSection, WarehouseMember |
| `domain/enums` | `Enums.md` (WarehouseStatus, WarehouseSectionType, WarehouseManagementType) |
| `exception` | `WarehouseErrorCode.md` |
| `application/service` | WarehouseService, WarehouseSectionService, WarehouseSectionCapacityService, WarehouseMemberService, WarehouseCodeService |
| `application/port/in` | `UseCases.md`, `command/Commands.md`, `query/SearchConditions.md`, `result/Results.md` |
| `application/port/out` | `Repositories.md` (Repository 3종 + StockPresencePort, WarehouseUsagePort) |
| `adapter/in/web` | 컨트롤러 4개, `dto/request/Requests.md`, `dto/response/Responses.md` |
| `adapter/out/inventory` | `StockPresenceAdapter.md` |
| `adapter/out/persistence` | `PersistenceAdapters.md`, `repository/JpaRepositories.md`, `entity/JpaEntities.md` |

## 핵심 규칙

- **코드는 서버가 만든다**: 창고 코드는 주소의 시·도로 발급(`WH-SU-001`). 시·도가 바뀌는 주소 수정이면 재발급
- **수용량 계층**: 같은 상위 아래 활성 구역의 수용량 합 ≤ 상위 수용량(창고 `totalCapacity` 또는 상위 구역 `capacity`)
- **잠금 순서**: 구역 → 창고 순. 재고 변경 트랜잭션은 구역 행을 section_id 오름차순으로 먼저 잠근 뒤 재고 행을 잠근다
- **비활성화 전 확인**: 재고와 진행 중 업무(입고·창고 발주·출고·지점 발주)가 있으면 `WAREHOUSE_IN_USE`
- **구역 삭제**는 재고·하위 구역·참조(로트·입고 검수 항목)가 없을 때만. 이력이 남는 구역은 비활성화
- **담당 창고 범위**는 서비스가 `AuthenticatedUser`로 검사한다. 구역 목록은 창고를 지정하지 않으면 본사만 가능
