# PurchaseOrderService

`inbound/application/service/PurchaseOrderService.java` · 구현: `PurchaseOrderUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

창고 발주 등록·조회·확정·취소. 역할은 `SecurityConfig`가, **담당 창고 범위와 취소의 작성자·상태별 권한**은 이 서비스가 검사한다(ADR-012).

## 의존

`PurchaseOrderRepository`, `InboundRepository`, `PurchaseOrderQueryRepository`, `SupplierRepository`, `WarehouseAvailabilityPort`, `SkuPurchasePricePort`, `StatusHistoryUseCase`

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerPurchaseOrder(command, actor)` | 쓰기 | 발주와 항목을 한 트랜잭션으로 등록하고 발주 ID 반환 |
| `getPurchaseOrders(condition, actor)` | 읽기 | 범위 계산(403) → 기간 검증 → 창고·공급처 존재 확인(404) → 검색 |
| `getPurchaseOrder(id, actor)` | 읽기 | 담당 창고 확인. CANCELED면 상태 이력에서 취소 사유를 채운다 |
| `getPurchaseOrderDetails(id, actor)` | 읽기 | 헤더 + SKU별 항목 |
| `confirmPurchaseOrder(id, userId)` | 쓰기 | REQUESTED만 확정 |
| `cancelPurchaseOrder(id, command, actor)` | 쓰기 | 아래 규칙 |

## registerPurchaseOrder 규칙

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 작성자 필수, 항목 1개 이상, 항목 수량 > 0, **같은 SKU 중복 불가**, 예정 일시는 현재 이후, 비고 1000자 이하 | 400 |
| 2 | 담당 창고 확인 | 403 |
| 3 | 창고 존재·활성 | 404 / 409 |
| 4 | 공급처 존재·활성 | 404 / 409 |
| 5 | 항목마다 **SKU 현재 매입 단가를 스냅샷** (`skuPurchasePricePort`) | 비활성 SKU 409, 단가 없음 `PURCHASE_PRICE_MISSING` |
| 6 | 발주 번호 채번 후 헤더·항목 저장, 상태 이력(`null → REQUESTED`) | |

발주 단가는 **요청으로 받지 않는다.** 등록 시점의 SKU 매입 단가가 `orderedUnitPrice`로 고정되고, 이후 SKU 단가가 바뀌어도 발주 항목은 그대로다.

## confirmPurchaseOrder 규칙

처리 사용자 필수 → 발주 조회(404) → REQUESTED인지(409) → 공급처 조회, **활성인지**(`SUPPLIER_INACTIVE`) → 확정, 상태 이력 기록. 본사 전용 API라 담당 창고 검사는 하지 않는다.

## cancelPurchaseOrder 규칙

| 순서 | 검사 | 오류 |
|---|---|---|
| 1 | 처리 사용자 필수 | 400 |
| 2 | **발주 행 비관적 락** 조회 | 404 |
| 3 | 담당 창고 확인 (**먼저** 해서 다른 창고 사용자에게 상태가 드러나지 않게 한다) | 403 |
| 4 | REQUESTED·CONFIRMED | 409 |
| 5 | 취소 권한 (`requireCancelAuthority`) | 403 |
| 6 | CONFIRMED를 취소하면서 사유가 없음 | 400 |
| 7 | 사유 500자 초과 | 400 |
| 8 | **취소되지 않은 입고가 있음** | 409 `PURCHASE_ORDER_HAS_INBOUND` |
| 9 | 취소, 상태 이력에 사유·처리자 기록 | |

### 취소 권한

| 발주 상태 | 취소할 수 있는 사람 |
|---|---|
| REQUESTED | **작성자인** 창고 관리자 (담당 창고여야 함) |
| CONFIRMED | 본사 관리자 |

`SecurityConfig`는 취소를 HQ_ADMIN·WAREHOUSE_MANAGER에 열어 두고, 위 상세 권한은 서비스가 판단한다.

## 동시성

입고 등록도 같은 발주 행을 잠그므로, **취소와 입고 등록이 동시에 통과하지 못한다.**

## 발주 번호 채번

`PO-YYYYMMDD-NNNN`. 당일 접두어 개수 + 1. 번호가 겹치면 유니크 제약이 409 `DUPLICATE_PURCHASE_ORDER_NO`로 응답한다.
