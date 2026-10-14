# WarehouseSectionController

`warehouse/adapter/in/web/WarehouseSectionController.java` · `@RestController` · Swagger 태그 "창고 구역"

클래스 레벨 `@RequestMapping`이 없고 **메서드마다 전체 경로를 명시**한다. 기준 경로가 둘(`/warehouses/sections`, `/warehouses/{warehouseId}/sections`)이기 때문이다.

## 의존

`WarehouseSectionUseCase`, `WarehouseUseCase`(응답에 창고명을 채우려고)

## 엔드포인트

| 메서드 | 경로 | 상태 | 요청 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `registerSection` | POST `/api/v1/warehouses/sections` | 201 | `WarehouseSectionRegisterRequest` | `WarehouseSectionResponse` | 구역 등록 | HQ_ADMIN |
| `getAllSections` | GET `/api/v1/warehouses/sections` | 200 | 쿼리 `warehouseId`, `parentSectionId`, `sectionType`, `keyword`, `isActive` | `ItemsResponse<WarehouseSectionResponse>` | 전체 구역 조회. 창고를 지정하지 않으면 본사만 | HQ_ADMIN |
| `getSectionsByWarehouse` | GET `/api/v1/warehouses/{warehouseId}/sections` | 200 | 쿼리 `parentSectionId`, `sectionType`, `keyword`, `isActive` | `ItemsResponse<WarehouseSectionResponse>` | 창고별 구역 조회 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getSection` | GET `/api/v1/warehouses/sections/{sectionId}` | 200 | - | `WarehouseSectionResponse` | 단건 조회 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `updateSection` | PATCH `/api/v1/warehouses/sections/{sectionId}` | 200 | `WarehouseSectionUpdateRequest` | `WarehouseSectionResponse` | 부분 수정 | HQ_ADMIN |
| `deactivateSection` | PATCH `.../sections/{sectionId}/deactivate` | 200 | - | `WarehouseSectionResponse` | 비활성화 | HQ_ADMIN |
| `activateSection` | PATCH `.../sections/{sectionId}/activate` | 200 | - | `WarehouseSectionResponse` | 활성화 | HQ_ADMIN |
| `deleteSection` | DELETE `/api/v1/warehouses/sections/{sectionId}` | 200 | - | `WarehouseSectionDeleteResponse` | 삭제 | HQ_ADMIN |

## 응답 조립

구역 응답은 창고명과 상위 구역 코드를 포함해야 해서 컨트롤러가 조회해 채운다.

| 헬퍼 | 설명 |
|---|---|
| `toResponse(section)` | 창고명과 상위 구역 코드(상위가 있을 때만)를 각각 조회해 `WarehouseSectionResponse.from`에 넘긴다 |
| `toResponses(sections)` | 창고·상위 구역당 **한 번만** 조회하도록 맵에 캐시하는 목록용 헬퍼 |

## 관찰

- `toResponses`는 정의돼 있지만 **어느 엔드포인트도 호출하지 않는다.** 목록 엔드포인트(`getAllSections`, `getSectionsByWarehouse`)는 `.map(this::toResponse)`를 써서 구역마다 창고와 상위 구역을 조회한다(N+1). 의도라면 목록에서 `toResponses`로 바꾸면 쿼리를 줄일 수 있다. 코드는 수정하지 않았다.
