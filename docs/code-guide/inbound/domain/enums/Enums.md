# inbound 도메인 enum

`inbound/domain/enums/`

## InboundStatus

| 값 | 의미 |
|---|---|
| ARRIVED | 도착 |
| INSPECTING | 검수 중 |
| COMPLETED | 입고 완료 (재고 반영됨) |
| CANCELED | 취소 (ARRIVED·INSPECTING에서만) |

전이: `ARRIVED → INSPECTING → COMPLETED`, `ARRIVED·INSPECTING → CANCELED`. 완료 후에는 취소할 수 없다(확정된 결정).

## PurchaseOrderStatus

| 값 | 의미 |
|---|---|
| REQUESTED | 창고 관리자가 등록, 본사 확정 대기 |
| CONFIRMED | 본사 관리자 확정 |
| COMPLETED | 전량 입고 시 시스템 자동 전환 |
| CANCELED | 취소 |

## PurchaseOrderLineStatus

| 값 | 의미 |
|---|---|
| REQUESTED | 입고 전 |
| PARTIALLY_RECEIVED | 일부 입고 (received < expected) |
| COMPLETED | 전량 입고 |

## SupplierStatus

| 값 | 의미 |
|---|---|
| ACTIVE | 거래 중 |
| INACTIVE | 비활성 |

| 메서드 | 설명 |
|---|---|
| `fromActiveFlag(Boolean isActive)` | 목록 조회 `isActive` 필터를 상태로 변환. null이면 null |

## 진행 중 상태 집합

다른 도메인의 비활성화 가드가 쓰는 "진행 중" 정의다.

| 대상 | 진행 중 상태 |
|---|---|
| 입고 | ARRIVED, INSPECTING |
| 창고 발주 | REQUESTED, CONFIRMED |
