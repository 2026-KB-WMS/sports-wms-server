# PurchaseOrder

`inbound/domain/entity/PurchaseOrder.java`

**창고가 공급업체에 상품을 주문하는 창고 발주 헤더.**

> 주의: `storeorder` 도메인의 `StoreOrder`(지점 → 창고 요청, ERD "발주")와 다르다. 이 클래스는 창고 → 공급처 발주다.

발주 항목(`PurchaseOrderLine`)은 `purchaseOrderId`로 참조한다.

## 상태 전이

```
REQUESTED ──confirm()──▶ CONFIRMED ──complete()──▶ COMPLETED
    │                        │
    └──────── cancel() ──────┴──▶ CANCELED
```

| 전이 | 주체 |
|---|---|
| REQUESTED 등록 | 창고 관리자 |
| → CONFIRMED | 본사 관리자 |
| → COMPLETED | **시스템 자동**: 마지막 입고 완료로 모든 항목이 전량 입고됐을 때 |
| → CANCELED | REQUESTED는 작성자 창고 관리자, CONFIRMED는 본사 관리자 |

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| purchaseOrderId | Long | PK |
| purchaseOrderNo | String | 발주 번호 `PO-YYYYMMDD-0001` (유니크) |
| warehouseId | Long | 입고 대상 창고 |
| supplierId | Long | 공급처 |
| status | PurchaseOrderStatus | 생성 시 null이면 REQUESTED |
| expectedAt | LocalDateTime | 입고 예정 일시 (선택) |
| note | String | 비고 |
| createdBy | Long | 작성자 |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `register(...)` | REQUESTED로 생성 | |
| `confirm()` | REQUESTED → CONFIRMED | `IllegalStateException` |
| `cancel()` | REQUESTED·CONFIRMED → CANCELED. **권한 검사는 서비스에서** | `IllegalStateException` |
| `complete()` | CONFIRMED → COMPLETED. 입고 완료 처리 시 시스템이 호출 | `IllegalStateException` |
| `isRequested()`, `isConfirmed()` | 상태 확인 | |
| `isInProgress()` | REQUESTED·CONFIRMED. 공급처 비활성화 가능 여부의 기준 | |
