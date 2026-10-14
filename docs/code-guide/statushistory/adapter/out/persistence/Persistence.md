# 영속성 (adapter/out/persistence)

`statushistory/adapter/out/persistence/`

## StatusHistoryPersistenceAdapter

`StatusHistoryRepository`를 구현한 `@Component`. 저장·조회를 `StatusHistoryJpaRepository`에 위임하고 `fromDomain` / `toDomain`으로 변환한다.

## StatusHistoryJpaRepository

| 메서드 | 설명 |
|---|---|
| `findByEntityTypeAndEntityIdOrderByChangedAtAscStatusHistoryIdAsc` | 시간순 이력. **`changed_at`이 같은 이력(같은 트랜잭션에서 연속 기록)은 PK로 순서를 고정**한다 |
| `findFirstByEntityTypeAndEntityIdAndToStatusOrderByChangedAtDescStatusHistoryIdDesc` | 해당 상태로 가장 최근에 바뀐 이력 한 건 |
| `findViewsByEntity` | JPQL 생성자 표현식으로 `StatusHistoryView`를 만든다. **사용자를 `left join`**해 사용자가 없어도 이력 행이 사라지지 않게 한다 |

## StatusHistoryJpaEntity

| 항목 | 내용 |
|---|---|
| 테이블 | `status_history` |
| PK | `status_history_id` (IDENTITY) |
| 컬럼 | entity_type(30, enum 문자열), entity_id, from_status(20, null 허용), to_status(20), reason(500), changed_by, changed_at |
| 인덱스 | `idx_status_history_entity`(entity_type, entity_id, changed_at), `idx_status_history_changed_by`(changed_by) |
| 상속 | `BaseTimeEntity`를 상속하지 않는다. `changed_at`을 직접 관리하는 **추가 전용** 테이블 |

`entity_id`는 여러 테이블을 가리키는 **다형 참조**라 연관관계 매핑 없이 ID만 가진다. `changed_by`도 ID만 가진다.
