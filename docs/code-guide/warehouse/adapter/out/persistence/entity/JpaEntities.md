# JPA 엔티티

`warehouse/adapter/out/persistence/entity/` · 테이블 매핑 모델. 엔티티끼리 연관관계 없이 `Long xxxId`로만 참조한다(ADR-005).

| 엔티티 | 테이블 | PK | 유니크 제약 | 주요 컬럼 |
|---|---|---|---|---|
| `WarehouseJpaEntity` | warehouse | warehouse_id (IDENTITY) | `uk_warehouse_code`(warehouse_code) | warehouse_code(30), name(100), address(500), latitude·longitude(10,7, null 허용), contact_number(30), total_capacity(14,3), status(20) |
| `WarehouseSectionJpaEntity` | warehouse_section | section_id | `uk_warehouse_section_code`(warehouse_id, section_code) | warehouse_id, parent_section_id, section_code(50), name(100), section_type(20), capacity(14,3), current_capacity(14,3), status(20) |
| `WarehouseMemberJpaEntity` | warehouse_member | warehouse_member_id | `uk_warehouse_member_warehouse_user`(warehouse_id, user_id) | warehouse_id, user_id, member_role(30), assigned_at, created_at |

## 참고

- `WarehouseJpaEntity`, `WarehouseSectionJpaEntity`는 `BaseTimeEntity`를 상속해 `created_at`, `updated_at`을 갖는다.
- `WarehouseMemberJpaEntity`는 **`updated_at`이 없는 배정 이력성** 엔티티라 `BaseTimeEntity`를 상속하지 않고 `@CreationTimestamp`로 `created_at`만 둔다.
- `section_type`, `member_role`은 enum이 아니라 **문자열 컬럼**이다. 허용 값은 서비스가 `WarehouseSectionType`, `WarehouseManagementType`으로 검사한다.
- 용량 컬럼은 `DECIMAL(14,3)`이다.
