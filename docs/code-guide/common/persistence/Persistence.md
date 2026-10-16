# 공통 영속성 (persistence)

`common/persistence/`

## BaseTimeEntity

| 항목 | 내용 |
|---|---|
| 용도 | JPA 엔티티의 `created_at`, `updated_at`을 공통 관리하는 베이스 클래스 |
| 어노테이션 | `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)` |
| 필드 | `createdAt`(`@CreatedDate`, 수정 불가), `updatedAt`(`@LastModifiedDate`) |
| 전제 | `JpaAuditingConfig`의 `@EnableJpaAuditing` |
| 사용처 | 각 도메인의 `XxxJpaEntity`가 상속. `toDomain()`에서 `getCreatedAt()`·`getUpdatedAt()`으로 도메인 객체에 옮긴다 |

`@CreationTimestamp`를 직접 쓰는 일부 엔티티(예: `SkuOptionValueJpaEntity`)는 이 클래스를 상속하지 않는다.

## PageAdapter

| 항목 | 내용 |
|---|---|
| 용도 | 포트의 `PageCondition`·`PageResult`와 Spring Data의 `Pageable`·`Page`를 오가는 변환 (영속성 어댑터 전용) |
| 메서드 | `toPageable(condition)`: 정렬 경로를 그대로 `Sort`에 넘긴다 (page는 0부터로 바꿈). `toPageResult(page, condition[, mapper])` |
| 전제 | 쿼리가 별칭 경로 정렬(`i.createdAt`, 조인한 `w.name`)과 집계식 정렬(`count(l)`, `sum(...)`, `JpaSort.unsafe`로 변환)을 받는 JPQL이어야 한다. `group by`·집계가 있으면 `countQuery`를 직접 적는다 |
| 검증 | `PageAdapterIntegrationTest` + 테스트 전용 `PagingProbeRepository`가 기준 예시(생성자 프로젝션 + 조인 + 별칭 정렬 + `countQuery`) |

## SearchKeyword

| 항목 | 내용 |
|---|---|
| 용도 | 목록 조회 `keyword` 파라미터 정규화 |
| 메서드 | `normalize(keyword)`: 빈 문자열·공백뿐이면 `null`(조건 없음), 아니면 앞뒤 공백 제거 |
| 사용처 | 각 도메인 PersistenceAdapter의 `search`가 JPQL에 넘기기 전에 호출 |
