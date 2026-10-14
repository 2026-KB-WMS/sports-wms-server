# StoreOrder

`storeorder/domain/entity/StoreOrder.java`

**지점이 창고에 상품 보충을 요청하는 지점 발주 헤더.**

> 주의: `inbound` 도메인의 `PurchaseOrder`(창고 → 공급처 발주)와 다르다. ERD의 "발주"는 이 클래스다.

발주 항목(`StoreOrderLine`)은 `storeOrderId`로 참조한다. 권한·사유 필수 여부·출고 연동 검사는 서비스에서 하고, 이 객체는 **상태 전이 가드**만 맡는다.

## 상태 전이

```
REQUESTED ──approve()──▶ APPROVED ──assign()──▶ ASSIGNED ◀──resume()── ON_HOLD
    │                                              │   └───hold()────▶   │
    └─reject()─▶ REJECTED                          └──complete()──▶ COMPLETED
    
REQUESTED·APPROVED·ASSIGNED·ON_HOLD ──cancel()──▶ CANCELED
```

| 전이 | 메서드 | 주체 |
|---|---|---|
| REQUESTED → APPROVED | `approve()` | 본사 |
| REQUESTED → REJECTED | `reject()` | 본사 (사유 필수) |
| APPROVED → ASSIGNED | `assign(warehouseId)` | 본사 (창고 배정) |
| ASSIGNED의 창고 변경 | `reassign(warehouseId)` | 본사 (사유 필수) |
| ASSIGNED → ON_HOLD | `hold()` | 창고 관리자 (사유 필수) |
| ON_HOLD → ASSIGNED | `resume()` | 창고 관리자 (사유 필수) |
| ASSIGNED → COMPLETED | `complete()` | **전량 출고 시 시스템** 또는 부분 출고 종결(창고 관리자) |
| 진행 중 → CANCELED | `cancel()` | REQUESTED는 작성자 점주, 승인 이후는 본사 |

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| storeOrderId | Long | PK |
| orderNo | String | 주문 번호 `SO-YYYYMMDD-0001` (유니크) |
| storeId | Long | 발주 지점 |
| warehouseId | Long | 배정 창고. **ASSIGNED 전까지 null** |
| status | StoreOrderStatus | 생성 시 null이면 REQUESTED |
| requestedAt | LocalDateTime | 요청 일시 |
| requestedDeliveryAt | LocalDateTime | 요청 배송 일시 (선택) |
| note | String | 비고 |
| createdBy | Long | 작성자(점주) |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `register(orderNo, storeId, requestedAt, requestedDeliveryAt, note, createdBy)` | REQUESTED로 생성. `warehouseId`는 null | |
| `approve()`, `reject()` | REQUESTED에서만 | `IllegalStateException` |
| `assign(warehouseId)` | APPROVED에서만. warehouseId 필수 | `IllegalStateException`, `IllegalArgumentException` |
| `reassign(warehouseId)` | ASSIGNED에서만(**ON_HOLD는 불가**), 현재와 다른 창고여야 함 | 상태는 `IllegalStateException`, 같은 창고는 `IllegalArgumentException` |
| `hold()` | ASSIGNED에서만 | `IllegalStateException` |
| `resume()` | ON_HOLD에서만 | `IllegalStateException` |
| `complete()` | ASSIGNED에서만 | `IllegalStateException` |
| `cancel()` | 진행 중(`isInProgress`)에서만 | `IllegalStateException` |
| `isRequested()` | REQUESTED 여부 | |
| `isInProgress()` | REQUESTED·APPROVED·ASSIGNED·ON_HOLD. **취소 가능 여부와 지점 비활성화 가드의 기준** | |
| `isTerminal()` | CANCELED·REJECTED·COMPLETED | |
