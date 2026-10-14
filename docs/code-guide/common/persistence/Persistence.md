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

## SearchKeyword

| 항목 | 내용 |
|---|---|
| 용도 | 목록 조회 `keyword` 파라미터 정규화 |
| 메서드 | `normalize(keyword)`: 빈 문자열·공백뿐이면 `null`(조건 없음), 아니면 앞뒤 공백 제거 |
| 사용처 | 각 도메인 PersistenceAdapter의 `search`가 JPQL에 넘기기 전에 호출 |
