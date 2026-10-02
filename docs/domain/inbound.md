# 입고(inbound) 도메인 요약

> **기준은 레포다.** 입고 도메인(공급처·발주·입고)이 구현 완료되어 2026-10-02 이 문서를 기준 문서로 승격했다. 입고 관련 상태 전이·권한·부수 효과는 이 문서를 고치고, Notion "업무 상태 전이도"의 입고 부분은 참고용으로만 둔다(출고·반품 부분은 계속 Notion이 기준).
> - API 명세: [`docs/api/inbound.md`](../api/inbound.md) (`/api/v1/suppliers`, `/purchase-orders`, `/inbounds` 20개)
> - 구현 일정: WMS 개발 일정 DB의 "[구현] 입고 → 공급처 / 창고 발주 / 입고", 추적 이슈 #71 (하위 #72~#88)

## 범위

`inbound` 패키지 = Supplier, PurchaseOrder, PurchaseOrderLine, Inbound, InboundLine (ERD "입고" 섹션).
지점→창고 요청인 StoreOrder/StoreOrderLine은 `purchaseorder` 패키지이며 이 문서의 범위가 아니다.

## 권한

- Supplier(공급처)는 본사 관리자(HQ_ADMIN)만 등록/수정/비활성화. 창고는 발주 등록 시 목록 조회만 가능.

## 발주(PurchaseOrder) 상태

| 현재 | 행위 | 다음 | 수행자 | 조건 |
|---|---|---|---|---|
| REQUESTED | 발주 확정 | CONFIRMED | 본사 관리자 | 공급처/품목/수량 확정 (반려는 MVP 이후) |
| REQUESTED | 취소 | CANCELED | 창고 관리자 | 본사 확정 전, 요청 작성자 |
| CONFIRMED | 최초 입고 등록 | (Inbound 생성, ARRIVED) | 창고 관리자 | 공급처로부터 상품 도착 |
| CONFIRMED | 발주 취소 | CANCELED | 본사 관리자 | 사유를 `StatusHistory.reason`에 기록 |
| CONFIRMED | 전량 입고 완료 | COMPLETED | 시스템(자동) | 모든 PurchaseOrderLine이 COMPLETED |

PurchaseOrderLine: `REQUESTED` → `PARTIALLY_RECEIVED`(received < expected) → `COMPLETED`(전량).
Inbound 완료 시점마다 InboundLine의 `accepted_quantity`/`defective_quantity` 합산으로 갱신한다. 한 발주에 부분 입고가 여러 번 걸칠 수 있다.

## 입고(Inbound) 상태

| 현재 | 행위 | 다음 | 조건 |
|---|---|---|---|
| ARRIVED | 검수 시작 | INSPECTING | 입고 대상 존재 |
| INSPECTING | 검수·적치 완료 | COMPLETED | 합격/불량 수량 확정, 위치 지정, 재고·이력 반영 성공 |
| ARRIVED | 입고 취소 | CANCELED | 사유 기록, 재고 반영 전 |
| INSPECTING | 입고 취소 | CANCELED | 사유 기록, 재고 반영 전 |

- **COMPLETED 이후 취소 불가** (Notion에서 확정). 완료 후 정정은 취소가 아니라 재고 조정 흐름으로 처리한다.
- 별도 "적치 상태"는 없다. `InboundLine.accepted_section_id` / `defect_section_id`가 채워지는 시점에 검수 완료와 함께 적치가 처리된다.
- 입고 `COMPLETED`는 업무 완료를 뜻하며 재고 분류(`AVAILABLE`/`DEFECTIVE`)와 다르다.

## 재고·부수 효과 (입고 완료 시 한 트랜잭션)

1. 합격 수량은 `AVAILABLE`, 불량 수량은 `DEFECTIVE`로 `InventoryLot`에 반영 (구역별).
2. Lot은 `PATCH /inbounds/{inboundId}/inspect` 시점에 find-or-create.
3. 모든 재고 수량 변경은 `InventoryTransaction`을 함께 기록 (수행자, 시각, 원인 문서, 이전·이후 수량).
4. PurchaseOrderLine 상태/수량 갱신 → 전 항목 완료 시 PurchaseOrder 자동 COMPLETED.
5. 모든 상태 변경은 `StatusHistory`에 기록 (취소는 사유 필수). **미구현**: `StatusHistory` 도메인이 없어 현재는 취소 사유를 검증(필수, 500자 이하)만 하고 저장하지 않는다.
6. 적치 구역 수용량 초과 시 409 `SECTION_CAPACITY_EXCEEDED` (다른 구역 지정 후 재시도).
7. 재고 수량 변경은 비관적 락.

## 구현 현황 (2026-10-02)

트래킹 이슈 #71, 하위 이슈 #72~#88 모두 머지 완료 (공급처 #72~#76, 발주 #77~#81, 입고 #82~#88).

### 구현 방식

- 입고 완료는 재고 도메인의 `receive`를 호출해 로트·구역 수용량·재고 이력을 처리한다(입고 도메인에 재고 로직을 중복하지 않음).
- 락 순서: 입고 행 → 발주 행 → 발주 항목 행 → 구역 행(ID 오름차순) → 재고 행(ID 오름차순). 입고 등록과 발주 취소는 발주 행만 잠가 `INBOUND_IN_PROGRESS`·`PURCHASE_ORDER_HAS_INBOUND` 검사가 어긋나지 않게 한다.
- 로트는 검수(`inspect`) 시점에 find-or-create(ADR-004). 검수는 재고에 반영하지 않고 완료(`complete`)에서만 반영한다.
- 검수 호출마다 입고의 검수 항목 전체를 교체한다(멱등). 한 발주 항목을 여러 로트로 나눠 받을 수 있고 같은 요청 안의 (발주 항목, 로트) 중복은 400.
- 명세에 없이 서비스에 넣은 규칙(명세에도 반영함): 완료 시 발주가 `CONFIRMED`여야 하고 발주 항목별 입고 수량이 남은 수량을 넘으면 409 `CONFLICT`.
- 번호 형식: 입고 `IB-yyyyMMdd-NNNN`, 발주 `PO-yyyyMMdd-NNNN`.

### 명세와 다른 점·보류 (자세한 내용은 [`api/inbound.md`](../api/inbound.md) "구현 대비 메모")

- 인증·인가 없음: 처리 사용자 `userId`를 검수·완료의 쿼리 파라미터로 받고, 역할·소속 창고·작성자 검사를 적용하지 않는다.
- `StatusHistory` 없음: 취소 사유를 저장하지 않으며 응답에 `cancelReason`이 없다. 회원 도메인 연동 전이라 `receivedByName`도 없다.
- 목록 페이지네이션 없음, 목록 필터의 존재하지 않는 ID 404 미적용.
- 완료 후 취소·정정 API 없음(재고 조정으로 처리). 초과 입고는 거절, 상위 구역 적치 허용 여부는 미확정.
- 재고 로트 상세·목록 응답의 `supplierName`, 로트 상세의 `inbounds`는 아직 채우지 않았다.
- 창고 비활성화 가드 `WAREHOUSE_IN_USE`는 재고 잔량만 검사하고 진행 중 입고·발주 배정은 검사하지 않는다.

### 테스트

도메인 단위, 서비스 단위(Mockito), 컨트롤러(`@WebMvcTest`), 영속성 어댑터 조회 쿼리 통합, 입고 완료 동시성(같은 로트·구역 동시 완료 시 수량 합계 일치, 같은 입고 중복 완료는 1건만 반영).

## 리뷰 시 자주 놓치는 지점

- 허용되지 않은 전이(예: COMPLETED→CANCELED, ARRIVED→COMPLETED 직행)가 서비스에서 막혀 있는가
- 재고 반영과 PurchaseOrder 상태 갱신이 같은 트랜잭션인가 (중간 실패 시 일부만 반영되지 않는가)
- 부분 입고 후 재입고 시 `received_quantity` 누적이 중복 합산되지 않는가
- 취소 시 사유 필수 검증과 StatusHistory 기록
- 권한: 확정/발주 취소는 본사, 입고 처리는 창고
- 존재하지 않는 공급처는 `SUPPLIER_NOT_FOUND` (generic NOT_FOUND 아님)
