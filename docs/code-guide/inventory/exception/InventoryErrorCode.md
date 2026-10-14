# InventoryErrorCode

`inventory/exception/InventoryErrorCode.java` · `DomainErrorCode` 구현 enum

| 분류 | 코드 | HTTP | 의미 |
|---|---|---|---|
| 미존재 | INVENTORY_NOT_FOUND | 404 | 재고를 찾을 수 없음 |
| 미존재 | LOT_NOT_FOUND | 404 | 로트를 찾을 수 없음 |
| 필터 대상 미존재 | SKU_NOT_FOUND, WAREHOUSE_NOT_FOUND, SECTION_NOT_FOUND, SUPPLIER_NOT_FOUND | 404 | 조회 필터에 준 ID가 없음 |
| 동시성 | STALE_QUANTITY | 409 | 조회 이후 수량이 바뀜 (조정의 `beforeQuantity` 불일치) |
| 수량 | BELOW_ALLOCATED_QUANTITY | 409 | 조정 후 수량이 할당 수량보다 작음 |
| 수량 | INSUFFICIENT_STOCK | 409 | 가용 재고 부족 (할당) |
| 로트 상태 | LOT_NOT_AVAILABLE | 409 | 가용이 아닌 로트(만료·격리·폐기)에 입고·할당 |
| 로트 상태 | INVALID_LOT_STATUS_TRANSITION | 409 | 허용되지 않는 로트 상태 변경 |
| 로트 상태 | LOT_HAS_ALLOCATION | 409 | 할당이 남은 로트는 격리 불가 |
| 로트 상태 | LOT_HAS_STOCK | 409 | 보유·할당이 남은 로트는 폐기 불가 |
| 로트 등록 | LOT_UNIT_COST_MISMATCH | 409 | 기존 로트와 원가가 다름 |
| 로트 등록 | LOT_DATE_MISMATCH | 409 | 기존 로트와 제조일·유통기한이 다름 |
| SKU | SKU_NOT_ACTIVE | 409 | 비활성 SKU는 입고·할당 불가 |

## 참고

조회 필터 대상(SKU·창고·구역·공급처) 미존재 404는 **다른 도메인의 에러 코드 enum을 import하지 않기 위해**(ADR-005) 해당 도메인과 같은 코드명으로 이 enum에 따로 둔다.
