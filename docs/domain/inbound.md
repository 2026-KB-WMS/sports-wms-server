# 입고(inbound) 도메인 요약

> **기준은 Notion이다.** 이 문서는 리뷰/구현 시 빠르게 참조하기 위한 요약 스냅샷(2026-10-02 기준)이며, 충돌하면 Notion을 따른다.
> - 업무 상태 전이도 (입고·출고·반품): WMS 문서 관리 → "업무 상태 전이도"
> - API 명세: WMS 문서 관리 → API 명세 → WMS API 명세 (`/api/v1/suppliers`, `/purchase-orders`, `/inbounds` 등 20개)
> - 구현 일정: WMS 개발 일정 DB의 "[구현] 입고 → 공급처 / 창고 발주 / 입고", 추적 이슈 #71

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
5. 모든 상태 변경은 `StatusHistory`에 기록 (취소는 사유 필수).
6. 적치 구역 수용량 초과 시 409 `SECTION_CAPACITY_EXCEEDED` (다른 구역 지정 후 재시도).
7. 재고 수량 변경은 비관적 락.

## 리뷰 시 자주 놓치는 지점

- 허용되지 않은 전이(예: COMPLETED→CANCELED, ARRIVED→COMPLETED 직행)가 서비스에서 막혀 있는가
- 재고 반영과 PurchaseOrder 상태 갱신이 같은 트랜잭션인가 (중간 실패 시 일부만 반영되지 않는가)
- 부분 입고 후 재입고 시 `received_quantity` 누적이 중복 합산되지 않는가
- 취소 시 사유 필수 검증과 StatusHistory 기록
- 권한: 확정/발주 취소는 본사, 입고 처리는 창고
- 존재하지 않는 공급처는 `SUPPLIER_NOT_FOUND` (generic NOT_FOUND 아님)
