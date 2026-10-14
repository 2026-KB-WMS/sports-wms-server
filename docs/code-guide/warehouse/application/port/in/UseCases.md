# 인바운드 포트 (UseCase)

`warehouse/application/port/in/` · 컨트롤러(또는 다른 도메인)가 호출하는 인터페이스. 상세 동작은 각 서비스 문서를 본다.

| UseCase | 구현 | 메서드 | 호출자 |
|---|---|---|---|
| `WarehouseUseCase` | `WarehouseService` | `registerWarehouse`, `getWarehouses`, `getWarehouse`(2종), `updateWarehouse`, `deactivateWarehouse`, `activateWarehouse`, `getMyWarehouses` | `WarehouseController`, 다른 도메인(주체 없는 `getWarehouse`) |
| `WarehouseSectionUseCase` | `WarehouseSectionService` | `registerSection`, `getSections`(2종), `getSection`(2종), `updateSection`, `deactivateSection`, `activateSection`, `deleteSection` | `WarehouseSectionController` |
| `WarehouseMemberUseCase` | `WarehouseMemberService` | `assignManager`, `getManagers`, `releaseManager` | `WarehouseMemberController` |
| `WarehouseCodeUseCase` | `WarehouseCodeService` | `getManagementTypes`, `getSectionTypes` | `WarehouseCodeController` |
| `WarehouseSectionCapacityUseCase` | `WarehouseSectionCapacityService` | `lock`, `occupy`, `vacate` | **재고 도메인**(`SectionCapacityAdapter`). 컨트롤러 없음 |

## 오버로드 규칙

`getWarehouse`, `getSection`, `getSections`는 `AuthenticatedUser actor`를 받는 버전과 받지 않는 버전이 있다.

| 버전 | 용도 |
|---|---|
| `actor` 있음 | 사용자 요청용. 담당 창고(본사는 전체)가 아니면 403 |
| `actor` 없음 | 다른 도메인·컨트롤러의 내부 조회용. 권한 검사 없음 |

## 하위 패키지

- `command/`: 쓰기 요청 입력 (`Commands.md`)
- `query/`: 검색 조건 (`SearchConditions.md`)
- `result/`: 조회 결과 (`Results.md`)
