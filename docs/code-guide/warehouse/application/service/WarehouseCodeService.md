# WarehouseCodeService

`warehouse/application/service/WarehouseCodeService.java` · 구현: `WarehouseCodeUseCase` · `@Transactional(readOnly = true)`

창고 도메인 고정 코드 목록 조회. DB 조회 없이 확정된 enum 값을 반환한다.

## 메서드

| 메서드 | 설명 |
|---|---|
| `getManagementTypes()` | `WarehouseManagementType` 전체를 `CodeItem(code, description)`으로 반환 (MANAGER) |
| `getSectionTypes()` | `WarehouseSectionType` 전체를 `CodeItem`으로 반환 (ZONE, RACK, DEFECT) |
