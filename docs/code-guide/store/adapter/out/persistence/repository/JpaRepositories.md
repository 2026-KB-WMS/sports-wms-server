# Spring Data JPA 리포지토리

`store/adapter/out/persistence/repository/`

| 리포지토리 | 엔티티 | 직접 정의한 메서드 |
|---|---|---|
| `StoreJpaRepository` | `StoreJpaEntity` | `existsByStoreCode`, `search` |
| `StoreMemberJpaRepository` | `StoreMemberJpaEntity` | `existsByStoreIdAndUserId`, `findByUserId`, `search` |

## 검색 쿼리

| 대상 | keyword 매칭 | 정렬 |
|---|---|---|
| 지점 | name, storeCode | createdAt 내림차순, storeId 내림차순 |
| 배정 | 사용자 name, loginId | assignedAt 내림차순, storeMemberId 내림차순 |

`findByUserId`도 `assignedAt` 내림차순, `storeMemberId` 내림차순이다(창고 쪽은 정렬이 없다).

## 특이 쿼리

`StoreMemberJpaRepository.search`는 `UserJpaEntity`를 ID로 조인해 `new StoreMemberView(...)`를 직접 생성한다. 읽기 전용 크로스 도메인 조인(ADR-007)이다.
