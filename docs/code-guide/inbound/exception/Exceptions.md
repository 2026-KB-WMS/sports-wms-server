# 오류 코드

`inbound/exception/` · 입고·발주·공급처가 각자 `DomainErrorCode` enum과 `ConstraintErrorCodeProvider`를 가진다. 일반적인 상태 충돌(409)은 공통 `CONFLICT`를 쓴다.

## InboundErrorCode

| 코드 | HTTP | 의미 |
|---|---|---|
| INBOUND_NOT_FOUND | 404 | 입고를 찾을 수 없음 |
| INBOUND_IN_PROGRESS | 409 | 같은 발주에 완료되지 않은 입고가 있음 (새 입고 등록 불가) |
| DUPLICATE_INBOUND_NO | 409 | 입고 번호 중복 (동시 등록으로 채번이 겹침) |
| INBOUND_HAS_NO_LINES | 409 | 저장된 검수 항목이 없는데 완료 시도 |
| SECTION_NOT_ASSIGNED | 409 | 합격·불량 수량이 있는 항목에 구역이 없음 |

## PurchaseOrderErrorCode

| 코드 | HTTP | 의미 |
|---|---|---|
| PURCHASE_ORDER_NOT_FOUND | 404 | 창고 발주를 찾을 수 없음 |
| PURCHASE_PRICE_MISSING | 409 | SKU에 매입 단가가 없어 발주 단가를 확정할 수 없음 |
| SUPPLIER_INACTIVE | 409 | 확정 시점에 발주의 공급처가 비활성 |
| DUPLICATE_PURCHASE_ORDER_NO | 409 | 발주 번호 중복 |
| PURCHASE_ORDER_HAS_INBOUND | 409 | 취소되지 않은 입고가 있어 발주 취소 불가 |

## SupplierErrorCode

| 코드 | HTTP | 의미 |
|---|---|---|
| SUPPLIER_NOT_FOUND | 404 | 공급처를 찾을 수 없음 |
| DUPLICATE_SUPPLIER_CODE | 409 | 공급처 코드 중복 |
| SUPPLIER_IN_USE | 409 | 진행 중인 발주가 있어 비활성화 불가 |

## 제약 이름 → 오류 코드

| 클래스 | 제약 이름 | 오류 코드 | 기준 마이그레이션 |
|---|---|---|---|
| `InboundConstraintErrorCodes` | uk_inbound_no | DUPLICATE_INBOUND_NO | V9 |
| `PurchaseOrderConstraintErrorCodes` | uk_purchase_order_no | DUPLICATE_PURCHASE_ORDER_NO | V8 |
| `SupplierConstraintErrorCodes` | uk_supplier_code | DUPLICATE_SUPPLIER_CODE | V7 |

입고·발주 번호는 서버가 채번하므로 **동시 등록으로 번호가 겹친 경우에만** DB 제약이 409를 응답한다. 검수 항목 중복((입고, 발주 항목, 로트))과 발주 안의 SKU 중복은 서비스가 먼저 400으로 거른다.
