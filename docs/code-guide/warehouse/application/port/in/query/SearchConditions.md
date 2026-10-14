# 검색 조건 (SearchCondition)

`warehouse/application/port/in/query/` · 목록 조회 필터 `record`. **null인 조건은 무시**한다.

## 흐름

`쿼리 파라미터` → 컨트롤러가 `new XxxSearchCondition(...)` → `UseCase.get...(condition)` → 서비스가 참조 대상 확인 → `Repository.search` → JPQL

## WarehouseSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 창고 목록을 필터링한다 |
| 엔드포인트 | GET `/api/v1/warehouses` (본사 전용) |
| 만드는 곳 | `WarehouseController.getWarehouses` |
| 쓰는 곳 | `WarehouseService.getWarehouses` → `WarehouseRepository.search` |
| 정렬 | 생성 일시 내림차순, 같으면 `warehouseId` 내림차순 |

| 필드 | 설명 |
|---|---|
| keyword | 창고명·창고 코드 부분 일치 |
| isActive | 운영 상태 필터 (null이면 전체) |

## WarehouseSectionSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 구역 목록을 필터링한다 |
| 엔드포인트 | GET `/api/v1/warehouses/sections`, GET `/api/v1/warehouses/{warehouseId}/sections` |
| 만드는 곳 | `WarehouseSectionController.getAllSections`, `getSectionsByWarehouse` (후자는 경로의 `warehouseId`를 조건에 넣는다) |
| 쓰는 곳 | `WarehouseSectionService.getSections(condition, actor)` → `WarehouseSectionRepository.search` |
| 정렬 | 구역 코드 오름차순, 같으면 `sectionId` |
| 검증 | 창고·상위 구역이 없으면 `WAREHOUSE_NOT_FOUND`·`PARENT_SECTION_NOT_FOUND`, 허용되지 않은 구역 유형은 400 |

| 필드 | 설명 |
|---|---|
| warehouseId | 창고 |
| parentSectionId | 이 구역의 **직속** 하위만 |
| sectionType | 구역 유형 코드 (`GET /warehouses/section-types`의 code) |
| keyword | 구역명·구역 코드 부분 일치 |
| isActive | 사용 여부 필터 |

사용자 요청에서 `warehouseId`를 지정하면 접근 권한을 확인하고, **지정하지 않으면 본사만** 호출할 수 있다.
