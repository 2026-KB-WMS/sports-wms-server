# 영속성 (adapter/out/persistence)

`auth/adapter/out/persistence/`

## 어댑터

| 어댑터 | 구현 포트 | 설명 |
|---|---|---|
| `UserPersistenceAdapter` | `UserRepository` | `save`는 **`saveAndFlush`**. flush해야 `@LastModifiedDate`가 채워져 응답의 `updatedAt`이 이번 수정 시각이 된다 |
| `UserQueryPersistenceAdapter` | `UserQueryRepository` | `findAffiliation`은 창고·지점 ID를 각각 조회해 `UserAffiliation`으로 묶는다. `findNamesByIds`는 ID → 이름 맵(중복 키는 첫 값) |

## JPA 리포지토리

| 리포지토리 | 상속 | 메서드 |
|---|---|---|
| `UserJpaRepository` | `JpaRepository` | `findByIdForUpdate`(`PESSIMISTIC_WRITE`), `findByLoginId`, `existsByLoginId`, `existsByEmail`, `existsByEmailAndUserIdNot`, `existsByRole`, `search` |
| `UserQueryJpaRepository` | `Repository` (조회 전용) | `findWarehouseIds`, `findStoreIds`, `findAllByIds` |

| 쿼리 | 설명 |
|---|---|
| `UserJpaRepository.search` | role·status 조건과 keyword(이름·로그인 아이디·이메일 부분 일치). 정렬·페이징은 `Pageable`이 정하고(기본 생성 일시 내림차순) 건수는 `countQuery`로 센다 |
| `UserQueryJpaRepository.findWarehouseIds` | `WarehouseMemberJpaEntity`에서 사용자의 창고 ID (오름차순). 읽기 전용 크로스 도메인 조회(ADR-007) |
| `UserQueryJpaRepository.findStoreIds` | `StoreMemberJpaEntity`에서 사용자의 지점 ID (오름차순) |

## UserJpaEntity

| 항목 | 내용 |
|---|---|
| 테이블 | **`users`** (`user`는 H2·PostgreSQL 예약어라 복수형을 쓴다) |
| PK | `user_id` (IDENTITY) |
| 유니크 | `uk_users_login_id`(login_id), `uk_users_email`(email) |
| 인덱스 | `idx_users_role_status`(role, status) |
| 컬럼 | login_id(50), password_hash(255), name(100), email(255), phone(30), role(30), status(20), last_login_at |
| 상속 | `BaseTimeEntity` |
