# 영속성 어댑터 (PersistenceAdapter)

`store/adapter/out/persistence/` · `application/port/out`의 Repository를 JPA로 구현한 `@Component`.

## 공통 패턴

- 도메인 객체 ↔ JPA 엔티티를 `fromDomain(...)` / `toDomain()`으로 변환한다.
- 검색은 `SearchKeyword.normalize(keyword)`와 `StoreStatus.fromActiveFlag(isActive)`를 거쳐 JPQL에 넘긴다.

## 어댑터별

| 어댑터 | 구현 포트 | 특이 사항 |
|---|---|---|
| `StorePersistenceAdapter` | `StoreRepository` | 락 조회 없음 |
| `StoreMemberPersistenceAdapter` | `StoreMemberRepository` | `search`는 JPQL이 곧바로 `StoreMemberView`를 만들어 반환 |
