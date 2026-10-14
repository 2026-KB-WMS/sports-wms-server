# Spring Data JPA 리포지토리

`warehouse/adapter/out/persistence/repository/` · 영속성 어댑터만 사용한다.

| 리포지토리 | 엔티티 | 직접 정의한 메서드 |
|---|---|---|
| `WarehouseJpaRepository` | `WarehouseJpaEntity` | `existsByWarehouseCode`, `findByIdForUpdate`(`PESSIMISTIC_WRITE`), `search` |
| `WarehouseSectionJpaRepository` | `WarehouseSectionJpaEntity` | `existsByWarehouseIdAndSectionCode`, `existsByParentSectionIdAndStatus`, `existsByParentSectionId`, `findByIdForUpdate`, `search`, `findAllByWarehouseId`, `sumCapacity` |
| `WarehouseMemberJpaRepository` | `WarehouseMemberJpaEntity` | `existsByWarehouseIdAndUserId`, `findByUserId`, `search` |

## 검색 쿼리

모든 `search`는 `(:param is null or ...)` 형태라 null인 조건은 무시된다.

| 대상 | keyword 매칭 | 정렬 |
|---|---|---|
| 창고 | name, warehouseCode | createdAt 내림차순, warehouseId 내림차순 |
| 구역 | name, sectionCode (+ warehouseId, parentSectionId, sectionType, status 조건) | sectionCode, sectionId |
| 배정 | 사용자 name, loginId | (정렬 없음) |

## 특이 쿼리

| 메서드 | 설명 |
|---|---|
| `WarehouseSectionJpaRepository.sumCapacity` | 같은 상위 아래 `status` 일치 구역의 `capacity` 합(`coalesce(..., 0)`). `parentSectionId`가 null이면 최상위(`parentSectionId is null`) 합. `excludeSectionId`가 있으면 제외 |
| `WarehouseMemberJpaRepository.search` | `UserJpaEntity`를 ID로 조인해 `new WarehouseMemberView(...)`를 직접 생성. 읽기 전용 크로스 도메인 조인(ADR-007) |
