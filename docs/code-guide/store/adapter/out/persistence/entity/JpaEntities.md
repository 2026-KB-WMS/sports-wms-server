# JPA 엔티티

`store/adapter/out/persistence/entity/` · 엔티티끼리 연관관계 없이 `Long xxxId`로만 참조한다(ADR-005).

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `StoreJpaEntity` | store | store_id (IDENTITY) | `uk_store_code`(store_code) | store_code(30), name(100), address(500), latitude·longitude(10,7, null 허용), contact_name(100, null 허용), contact_number(30, **not null**), status(20) |
| `StoreMemberJpaEntity` | store_member | store_member_id | `uk_store_member_store_user`(store_id, user_id) | store_id, user_id, member_role(30), assigned_at, created_at |

## 참고

- `StoreJpaEntity`는 `BaseTimeEntity`를 상속해 `created_at`, `updated_at`을 갖는다.
- `StoreMemberJpaEntity`는 **`updated_at`이 없는 배정 이력성** 엔티티라 `BaseTimeEntity`를 상속하지 않고 `@CreationTimestamp`로 `created_at`만 둔다.
- `member_role`은 문자열 컬럼이다. 허용 값은 서비스가 `StoreManagementType`으로 검사한다.
