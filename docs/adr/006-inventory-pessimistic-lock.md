# ADR-006 재고 수량 갱신에 비관적 락(PESSIMISTIC_WRITE) 사용

## 상태
`Accepted` (2026-09-24)

## 컨텍스트
재고(InventoryLot)는 여러 요청(입고/출고/조정)이 동시에 같은 로트의 on_hand/allocated 수량을 갱신할 수 있다. 동시성 제어 없이 read-modify-write를 하면 갱신 유실(lost update)이 발생해 실재고와 시스템 수량이 어긋날 수 있다. 낙관적 락(버전 컬럼 + 재시도)과 비관적 락(SELECT ... FOR UPDATE) 중 하나를 선택해야 했다.

## 결정
InventoryLot 조회 중 수량을 변경하는 모든 경로(findByIdForUpdate, findAllByIdInForUpdate, findBySectionIdAndLotIdForUpdate)는 JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`로 조회해 트랜잭션 종료까지 행 잠금을 건다. 조회 전용(재고 현황 조회 등) 쿼리는 잠금 없이 일반 조회로 유지한다.

## 근거
- 재고 도메인은 충돌 시 재시도보다 정확성이 우선이며(수량 오류는 실물 재고와 불일치를 낳음), 비관적 락은 충돌을 감지가 아니라 원천 차단한다.
- 입고/출고/조정 API는 트랜잭션이 짧고 잠금 경합이 크지 않을 것으로 예상돼(단일 로트 또는 소수 로트 단위) 비관적 락의 성능 비용이 감내할 만하다.
- 낙관적 락은 실패 시 클라이언트/서비스 계층에서 재시도 로직을 추가로 구현해야 하는데, 현재 단계에서는 복잡도를 늘리고 싶지 않았다.

## 검토했던 대안
- 낙관적 락(버전 컬럼) — 기각. 동시 갱신이 잦을 경우 재시도 로직이 필요해지고, 재시도 실패 시 사용자에게 어떻게 응답할지 별도 설계가 필요해 복잡도가 늘어난다.
- 애플리케이션 레벨 분산 락(Redis 등) — 기각. 아직 별도 인프라(Redis)를 도입하지 않았고, DB 트랜잭션 내 락으로 충분히 해결 가능한 규모다.

## 영향
- InventoryStockUseCase/InventoryAdjustmentUseCase 구현체는 반드시 findByIdForUpdate류를 통해 조회한 InventoryLot만 변경해야 하며, 잠금 없이 조회한 인스턴스를 수정·저장하면 안 된다.
- 여러 로트를 한 번에 다루는 로직(예: FIFO 할당)은 findAllByIdInForUpdate처럼 ID 오름차순 정렬로 잠그는 순서를 통일해 데드락을 예방한다.
- 트랜잭션 범위를 짧게 유지해야 하므로, 잠금을 건 상태에서 외부 API 호출 등 느린 작업을 하지 않는다.
