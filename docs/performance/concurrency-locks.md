# 동시성 제어 현황 (락 목록)

어디에 어떤 락이 걸려 있는지와, 락이 아닌 방식(유니크 제약)으로 막는 곳을 정리한다. 기술 결정의 배경은 [ADR-006](../adr/006-inventory-pessimistic-lock.md), 검증 계획은 [`scenario/performance.md`](../scenario/performance.md) S4에 있다.

- 기준은 코드다(2026-11-15 `develop` 기준). 락을 추가·제거하면 이 문서를 같이 고친다.
- 락 방식은 전부 JPA `@Lock(PESSIMISTIC_WRITE)`(`SELECT ... FOR UPDATE`)이다. 낙관적 락(`@Version`)은 쓰지 않는다.
- 순서는 2026-11-15에 서비스 코드를 읽어 채웠다. 호출 체인 끝까지 따라가지 않은 곳은 "잠금 없이 조회" 정도로만 적었다.

## 1. 락으로 막는 곳

잠그는 대상은 `*JpaRepository`의 `@Lock` 메서드이고, 서비스가 `findByIdForUpdate`류 포트로 호출한다.

| 도메인 | 서비스 | 잠그는 대상 | 순서 |
|---|---|---|---|
| storeorder | `StoreOrderService` (상태 변경 8곳 + 라인 2곳) | 발주 헤더, 발주 라인 | 헤더 → 라인 |
| storeorder | `StoreOrderFulfillmentService` | 발주 헤더, 라인 (출고 도메인이 포트로 호출) | 헤더 → 라인 |
| outbound | `StockAllocationService` (할당·취소) | 발주 헤더, 발주 라인, 할당 | 헤더 → 라인 → 할당 |
| outbound | `OutboundService` (출고 생성) | 발주 헤더, 미연결 할당 | 헤더 → 할당 |
| outbound | `OutboundFulfillmentService` (피킹 완료·배송 완료 등) | 발주 헤더, 출고, 할당(ID 오름차순 일괄), 발주 라인, 재고 | 헤더 → 출고 → 할당 → 라인 → 재고 |
| outbound | `OutboundOrderLinkService` (발주 취소·부분 종결 시 연결 정리) | READY 출고, ALLOCATED 할당, 이어서 재고·발주 라인 | 헤더(호출한 발주 서비스가 이미 잠금) → 출고 → 할당 → 재고·라인(ID 오름차순) |
| inbound | `InboundCompleteService` (입고 완료) | 입고, 발주서, 공급사 주문, 발주 라인 | 입고 → 발주서 → 공급사 주문 → 라인 |
| inbound | `InboundService` (입고 등록) | 발주서, 공급사 주문 | 발주서 → 공급사 주문 (입고는 아직 없음) |
| inbound | `InboundService` (입고 취소), `InboundInspectService` (검수) | 입고 1건만 (발주서·로트는 잠금 없이 조회) | - |
| inbound | `PurchaseOrderService` (확정·취소 등 4곳) | 발주서, 라인 또는 공급사 주문 | 발주서 → 라인 / 발주서 → 공급사 주문 (입고는 존재 여부만 읽고 잠그지 않음) |
| inventory | `InventoryStockService` (입고 반영) | 구역(ID 오름차순 일괄) → 재고 행(섹션+로트 단건) | **구역 → 재고** (§4-1) |
| inventory | `InventoryStockService` (예약·해제) | 재고 여러 건(ID 오름차순 일괄). 구역은 잠그지 않음 | ([ADR-006](../adr/006-inventory-pessimistic-lock.md)) |
| inventory | `InventoryStockService` (출고 `ship`) | 구역(ID 오름차순) → 재고 여러 건(ID 오름차순 일괄) | 구역 → 재고 (입고 반영과 같은 순서) |
| inventory | `InventoryAdjustmentService` | 구역 → 재고 1건 (수용량 증감) | 구역 → 재고 (#357에서 변경, §4-1) |
| inventory | `LotService` (상태 변경) | 로트 → 그 로트의 재고 행 전체(ID 오름차순) | 로트 → 재고 행 |
| warehouse | `WarehouseSectionService` (등록) | 창고만 (상위 구역은 잠금 없이 조회) | - |
| warehouse | `WarehouseSectionService` (수정·활성화) | 구역 → 창고 | 구역 → 창고 |
| warehouse | `WarehouseSectionService` (비활성화·삭제) | 구역만 | - |
| warehouse | `WarehouseService` (수정) | 창고만 | - |
| warehouse | `WarehouseService` (비활성화) | 소속 구역 전부(수용량 포트의 `lock`). 창고 행은 잠그지 않음 | - |
| warehouse | `WarehouseSectionCapacityService` | 구역 1건 (적치·반출 시 수용량 증감) | - |
| auth | `AuthService` (로그인), `UserService` (수정·비밀번호 변경) | 사용자 행 1건 | - |
| product | `ProductCodeIssuer` | 브랜드 행 (상품 코드 발급 직렬화) | - |
| common | `LocationCodeIssuer` | 위치 코드 시퀀스 행 | - |

여러 행을 한 번에 잠글 때는 ID 오름차순으로 잠가 데드락을 피한다(재고 일괄, 할당 일괄).

## 2. 락이 아닌 방식으로 막는 곳

| 대상 | 방식 | 검증 |
|---|---|---|
| 가입 (로그인 아이디·이메일 중복) | "중복 확인 후 저장"이라 동시 요청이 확인을 함께 통과할 수 있다. 최종 방어선은 DB 유니크 제약(`uk_users_login_id`, `uk_users_email`)이고 위반 오류를 409로 변환한다 | `SignupConcurrencyTest` |
| 최초 본사 관리자 생성 ([ADR-013](../adr/013-initial-hq-admin-from-environment.md)) | 유니크 제약 위반(`DataIntegrityViolationException`)을 `InitialHqAdminRunner`가 잡아 무시 | `InitialHqAdminConcurrencyTest` |

## 3. 동시성 테스트

| 테스트 | 대상 |
|---|---|
| `InventoryConcurrencyTest` | 재고 수량 변경 |
| `OutboundConcurrencyTest` | 할당·출고 |
| `InboundCompleteConcurrencyTest` | 입고 완료 |
| `StoreOrderConcurrencyTest` | 발주 상태 변경 |
| `SignupConcurrencyTest` | 가입 중복 (유니크 제약) |
| `InitialHqAdminConcurrencyTest` | 초기 관리자 (유니크 제약) |
| `InventoryLockOrderConcurrencyTest` | 출고 피킹 vs 재고 조정 락 순서, 동시 피킹 수량 유실 |
| `InboundRegisterStaleReadTest` | 입고 등록 vs 발주 취소 (낡은 읽기) |

락은 있지만 위 목록에 전용 테스트가 없는 곳(창고·구역, 로트, 사용자 상태 변경, 코드 발급, 발주서 상태 변경)은 S4 대조 때 채운다.

## 4. 순환 락(데드락) 점검

순환 락은 최대한 피한다. 아래는 코드를 읽어 찾은 후보와 재현 결과다.

### 4-1. 재고 조정만 재고 행 → 구역 순서였음 (재현 후 #357에서 수정)

| 경로 | 순서 |
|---|---|
| 입고 반영(`InventoryStockService.receive`), 출고 피킹(`ship`) | 구역 → 재고 행 |
| 재고 조정(`InventoryAdjustmentService.adjust`) | 재고 행 → 구역 (수정 전) → 구역 → 재고 행 (#357 이후) |

`ship`은 입고와 같은 순서를 지키려고 재고 행의 구역을 먼저 읽어 구역부터 잠근다(코드 주석). 순서가 반대인 건 `adjust` 하나다. 재고 행을 잠근 뒤 수용량을 늘리거나 줄이려고 구역을 잠그기 때문이다. 같은 구역·같은 재고에 `adjust`와 `ship`(또는 입고 반영)이 동시에 오면 서로 상대가 쥔 행을 기다린다.

**재현 결과 (#353).** `InventoryLockOrderConcurrencyTest`에서 `ship`과 `adjust`를 100라운드 동시에 보냈더니 H2에서 100라운드 모두 `Deadlock detected`로 실패했다(`adjust` 95번, `ship` 5번이 희생자, `CannotAcquireLockException`). 수량 충돌로 거절될 일이 없게 만든 테스트라 실패는 전부 락 충돌이다.

- 재현은 H2에서만 했다. 운영 DB인 MySQL(InnoDB)에서 같은 데드락이 나는지는 확인하지 않았다.
- **수정 (#357).** `adjust`가 구역 ID를 먼저 읽어 구역을 잠그고 재고 행을 잠그도록 바꿨다. 같은 테스트가 100라운드 모두 통과한다.
- **함정.** 구역 ID를 얻으려고 재고 행을 `findById`로 통째로 읽으면 그 객체가 영속성 컨텍스트에 남아, 뒤이은 `FOR UPDATE`가 락만 잡고 **낡은 값을 돌려준다.** 처음 시도에서 이 때문에 조정이 서로 덮어써 `STALE_QUANTITY` 검사가 뚫렸다(150과 80이 둘 다 성공). 그래서 구역 ID만 읽는 `findSectionIdById`를 따로 만들었다.
- **`ship`도 같은 함정이었다 (#360).** `ship`이 구역 ID를 얻으려고 재고 행을 `findById`로 읽은 뒤 `lockAll`로 잠갔다. 같은 재고에 피킹 20건을 동시에 보내면 오류 없이 전부 성공하지만 보유·할당 수량이 80이 아니라 99로 남았다(19건 유실). `findSectionIdById`로 바꾸자 80으로 맞는다(`InventoryLockOrderConcurrencyTest.concurrentShip_doesNotLoseUpdates`).
- **`registerInbound`도 같은 함정이었다 (#363).** 공급처 발주서를 `findById`로 읽은 뒤 발주 행 락을 기다리는 사이 발주가 취소돼도, 락을 얻은 뒤 `findByIdForUpdate`가 낡은 객체(진행 중)를 돌려줘 취소된 발주서에 입고가 만들어졌다. 발주 락을 쥔 채 공급처 발주서를 취소하는 동안 입고 등록을 대기시키는 테스트(`InboundRegisterStaleReadTest`)에서 입고가 1건 생기는 것을 확인했다. 발주 ID만 읽는 `findPurchaseOrderIdById`로 바꾸자 등록이 거절된다.

### 4-2. 문제 없다고 본 것

- **발주 단위 직렬화.** 출고·할당·취소는 모두 발주 헤더를 가장 먼저 잠근다. 헤더 안에서 라인과 할당의 순서가 서비스마다 다르지만(할당 서비스는 라인 → 할당, 피킹 완료는 할당 → 라인) 헤더가 이미 직렬화하므로 같은 발주 안에서는 순환하지 않는다.
- **입고.** 모든 경로가 발주서를 먼저 잠근다(입고 완료는 입고 → 발주서). 발주서 취소는 입고를 잠그지 않고 존재만 읽는다.
- **구역 ↔ 창고.** 수정·활성화는 구역 → 창고, 등록·창고 수정은 창고만, 창고 비활성화는 구역만 잠가서 반대 순서 쌍이 없다.
- **여러 행을 한 번에 잠글 때.** 재고, 할당, 구역, 공급사 주문은 ID 오름차순이다.

### 4-3. 아직 안 본 것

- 발주 헤더를 잠근 채 재고로 내려가는 출고 경로와, 다른 발주가 같은 재고를 잠그는 경로 사이의 교차. 출고 피킹은 구역 → 재고 순서라 4-1의 `adjust`를 제외하면 반대 순서 쌍은 찾지 못했다.
- 창고 비활성화는 구역을 잠근 뒤 여러 도메인을 조회만 하는데, 조회 쪽이 락을 거는지는 확인하지 않았다.

### 4-4. 후속 검증 후보

재고·발주 수량과 직결되는데 전용 테스트가 아직 없는 곳이다.

- 출고 피킹 완료·배송 완료 경합 (`OutboundFulfillmentService`)
- 발주 취소·부분 종결 때 출고·할당 정리 (`OutboundOrderLinkService`)
- 구역 비활성화·삭제 vs 입고 적치 (순차 요청은 `SECTION_HAS_INVENTORY`로 막히고, 락은 두 요청이 겹치는 순간을 막는다)
- 사용자 상태 변경 vs 로그인, 창고·구역 수정, 위치 코드 발급: 드물게 쓰이고 틀려도 수량이 어긋나지 않아 우선순위가 낮다. 상품 코드 발급은 H2 테스트 스키마가 `category`(`TINYINT`)와 `option_value`(`value` 예약어) 테이블을 만들지 못해 상품 등록 통합 테스트를 쓸 수 없다.

위 테스트와 4-1의 수정은 모두 H2에서만 확인했다. MySQL(InnoDB) 검증은 [`scenario/performance.md`](../scenario/performance.md) S4에서 한다.
