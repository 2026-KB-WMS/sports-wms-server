# 애플리케이션 계층 (UseCase · Service · Port)

`statushistory/application/` · 다른 도메인이 상태를 바꿀 때 **호출하는 공용 도메인**이다. HTTP 엔드포인트와 컨트롤러는 없다.

## StatusHistoryUseCase / StatusHistoryService

`StatusHistoryService`가 구현한다. 클래스 기본은 `@Transactional(readOnly = true)`다.

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `record(entityType, entityId, fromStatus, toStatus, reason, changedBy)` | **`Propagation.MANDATORY`** | 이력을 기록한다. 호출한 도메인 서비스의 트랜잭션에 **반드시 참여**하며, 진행 중인 트랜잭션이 없으면 예외다. 호출 쪽이 롤백되면 이력도 함께 롤백된다. 최초 생성이면 `fromStatus`에 null |
| `findHistory(entityType, entityId)` | 읽기 | 엔티티별 이력을 **시간순(오래된 것부터)**으로 조회 |
| `findHistoryViews(entityType, entityId)` | 읽기 | 위와 같되 **처리자 이름을 함께** 담은 응답용 조회 |
| `findStatusReason(entityType, entityId, currentStatus)` | 읽기 | `currentStatus`로 **가장 최근에 바뀔 때** 기록된 사유. 이력이나 사유가 없으면 비어 있다 |

### MANDATORY의 이유

이력만 단독으로 커밋되면 "상태는 롤백됐는데 이력만 남는" 불일치가 생긴다. 항상 상태 변경과 **같은 트랜잭션**에 묶으려고 MANDATORY로 강제한다.

### findStatusReason 활용

취소·반려·보류 사유처럼 **현재 상태에 도달한 사유**를 읽을 때 쓴다. 예: `getInbound`가 CANCELED일 때 `findStatusReason(INBOUND, id, "CANCELED")`로 취소 사유를 채운다. 같은 상태로 여러 번 바뀌었다면(보류 → 재개 → 보류) **가장 최근** 기록을 쓴다.

## StatusHistoryView

| 항목 | 내용 |
|---|---|
| 용도 | 처리자 이름을 함께 담은 상태 이력 조회 결과 |
| 만드는 곳 | `StatusHistoryJpaRepository.findViewsByEntity`가 `users`를 ID로 조인해 생성(ADR-007) |
| 필드 | fromStatus(최초 생성이면 null), toStatus, reason, changedBy, changedByName(처리자가 없으면 null), changedAt |
| 쓰는 곳 | 지점 발주 상세의 `statusHistory`(`StoreOrderStatusHistoryView`로 다시 옮겨 담는다) |

## StatusHistoryRepository (아웃바운드 포트)

| 메서드 | 설명 |
|---|---|
| `save(history)` | 저장 |
| `findByEntity(type, id)` | 시간순 이력 |
| `findViewsByEntity(type, id)` | 처리자 이름 포함, 조회 전용 |
| `findLatestByToStatus(type, id, toStatus)` | 해당 상태로 가장 최근에 바뀐 이력. 한 번도 그 상태가 된 적이 없으면 비어 있음 |
