# InboundService

`inbound/application/service/InboundService.java` · 구현: `InboundUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

입고 등록·조회·취소와 검수 구역 후보 조회. 검수(`inspect`)와 완료(`complete`)는 **각각 별도 서비스**다. 역할은 `SecurityConfig`가, **담당 창고 범위는 이 서비스**가 검사한다(ADR-012).

## 의존

`InboundRepository`, `InboundQueryRepository`, `PurchaseOrderRepository`, `WarehouseAvailabilityPort`, `StatusHistoryUseCase`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerInbound(command, actor)` | 쓰기 | 아래 규칙. 입고 ID 반환 |
| `getInbounds(condition, actor)` | 읽기 | 아래 규칙 |
| `getInbound(id, actor)` | 읽기 | 조회 뷰 → 담당 창고 확인. CANCELED면 상태 이력에서 **취소 사유**를 읽어 채운다 |
| `getInboundDetails(id, actor)` | 읽기 | `getInbound`로 권한·존재 확인 후 검수 항목 뷰 목록 |
| `cancelInbound(id, command, actor)` | 쓰기 | 아래 규칙 |
| `getAssignableSections(id, condition, actor)` | 읽기 | 합격품을 둘 수 있는 구역 후보 |
| `getDefectSections(id, condition, actor)` | 읽기 | 불량품을 둘 수 있는 구역 후보 |

## registerInbound 규칙

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 발주 ID·처리 사용자 필수 | 400 |
| 2 | 도착 일시가 현재보다 미래 | 400 |
| 3 | 비고 1000자 초과 | 400 |
| 4 | **발주 행을 비관적 락**으로 조회 | 404 `PURCHASE_ORDER_NOT_FOUND` |
| 5 | 발주의 창고가 담당 창고 | 403 |
| 6 | 발주가 CONFIRMED | 409 `CONFLICT` |
| 7 | 같은 발주에 ARRIVED·INSPECTING 입고가 없음 | 409 `INBOUND_IN_PROGRESS` |
| 8 | 입고 번호 채번, ARRIVED로 저장, 상태 이력(`null → ARRIVED`) | |

- 입고 창고는 요청이 아니라 **발주의 창고**를 따른다.
- 발주 행을 잠그므로 같은 발주의 동시 입고 등록·발주 취소와 어긋나지 않는다.
- 도착 일시를 생략하면 요청 시각을 쓴다.

## 입고 번호 채번

`IB-YYYYMMDD-NNNN`. `당일 접두어로 시작하는 입고 개수 + 1`이다. 동시 등록으로 번호가 겹치면 유니크 제약이 409 `DUPLICATE_INBOUND_NO`로 응답한다.

## getInbounds 규칙

1. `actor.warehouseScope(warehouseId)`로 범위 계산(권한 403이 먼저)
2. 도착 시작이 종료보다 늦으면 400
3. 권한을 통과한 뒤에만 창고·발주 존재 확인(404). **비담당자에게 존재 여부를 흘리지 않기 위해서**다
4. 범위를 채워 검색

## cancelInbound 규칙

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 처리 사용자 필수, 사유 필수·500자 이하 | 400 |
| 2 | 입고 행 비관적 락 조회 | 404 `INBOUND_NOT_FOUND` |
| 3 | 담당 창고 확인 | 403 |
| 4 | ARRIVED·INSPECTING | 409 `CONFLICT` |
| 5 | 취소, 상태 이력에 사유·처리자 기록 | |

- 검수 항목은 재고에 반영되기 전이라 **그대로 둔다**.
- 취소해도 **발주 상태는 바뀌지 않는다**(응답에 발주 상태를 함께 내려준다).
