# StoreOrderErrorCode / StoreOrderConstraintErrorCodes

`storeorder/exception/`

## StoreOrderErrorCode

상태 충돌(409)은 대부분 공통 `CONFLICT`를 쓰고, 아래 코드는 클라이언트가 구분해야 하는 경우만 둔다.

| 코드 | HTTP | 의미 | 발생 |
|---|---|---|---|
| STORE_ORDER_NOT_FOUND | 404 | 지점 발주를 찾을 수 없음 | 조회·상태 변경 전반 |
| SUPPLY_PRICE_MISSING | 409 | SKU에 공급 단가가 없어 발주 단가를 확정할 수 없음 | 등록 |
| DUPLICATE_STORE_ORDER_NO | 409 | 주문 번호 중복 (동시 등록으로 채번이 겹침) | 등록 |
| ORDER_IN_PICKING | 409 | 피킹이 시작된 출고가 있어 취소·보류할 수 없음 | 취소, 보류 |
| ORDER_IN_FULFILLMENT | 409 | 재고 할당이나 취소되지 않은 출고가 남아 재배정할 수 없음 | 재배정 |
| OUTBOUND_IN_PROGRESS | 409 | 진행 중 출고가 있어 부분 출고로 종결할 수 없음 | 부분 종결 |
| NO_SHORTAGE | 409 | 부족 수량이 없어 부분 출고로 종결할 수 없음 | 부분 종결 |

## StoreOrderConstraintErrorCodes

| 제약 이름 | 오류 코드 | 기준 |
|---|---|---|
| uk_store_order_no | DUPLICATE_STORE_ORDER_NO | `V11__create_store_order_tables.sql` |

주문 번호는 서버가 채번하므로 동시 등록으로 번호가 겹친 경우에만 DB 제약이 409를 응답한다. 같은 발주 안의 SKU 중복(`uk_store_order_line_order_sku`)은 서비스가 먼저 400으로 거른다.
