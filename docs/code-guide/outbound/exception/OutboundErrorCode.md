# OutboundErrorCode / OutboundConstraintErrorCodes

`outbound/exception/`

일반 상태 충돌(409)은 공통 `CONFLICT`를 쓰고, 이 도메인에서만 의미가 있는 충돌과 404만 여기에 둔다. 재고 도메인이 거절하는 `LOT_NOT_AVAILABLE`, `SKU_NOT_ACTIVE` 등은 **재고 쪽 코드 그대로** 전달된다.

## OutboundErrorCode

| 코드 | HTTP | 의미 | 발생 |
|---|---|---|---|
| ALLOCATION_NOT_FOUND | 404 | 재고 할당을 찾을 수 없음 | 할당 조회·해제 |
| OUTBOUND_NOT_FOUND | 404 | 출고를 찾을 수 없음 | 출고 조회·상태 변경 |
| ALREADY_ALLOCATED | 409 | 할당할 잔여 수량이 없음 | 할당 |
| INSUFFICIENT_STOCK | 409 | 가용 재고가 부족해 **하나도** 할당할 수 없음 | 할당 |
| ALLOCATION_IN_OUTBOUND | 409 | 출고에 연결된 할당은 해제할 수 없음 | 할당 해제 |
| ORDER_NOT_ASSIGNED | 409 | 배정 상태가 아닌 발주 | 피킹 시작 |
| ALLOCATION_NOT_ACTIVE | 409 | ALLOCATED가 아닌 할당이 포함됨 | 피킹 시작·완료 |
| NO_ALLOCATION | 409 | 출고에 묶을 재고 할당이 없음 | 출고 생성 |
| DUPLICATE_OUTBOUND_NO | 409 | 출고 번호 중복 (동시 생성) | 출고 생성 |
| NOTHING_PICKED | 409 | 피킹한 수량이 전부 0 | 피킹 완료 |
| SUPPLY_PRICE_MISSING | 409 | 발주 항목과 SKU 모두 공급 단가가 없음 | 피킹 완료 |

## OutboundConstraintErrorCodes

| 제약 이름 | 오류 코드 | 기준 |
|---|---|---|
| uk_outbound_no | DUPLICATE_OUTBOUND_NO | `V1__baseline.sql` |

출고 번호는 서버가 채번하므로 동시 생성으로 번호가 겹친 경우에만 DB 제약이 409를 응답한다.
