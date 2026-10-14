# StoreOrderLine

`storeorder/domain/entity/StoreOrderLine.java`

지점 발주의 **SKU별 요청·할당·출고 수량** 상세. 발주당 SKU는 한 줄만 둘 수 있다(`uk_store_order_line_order_sku`).

## 수량 3종

| 수량 | 의미 | 누가 바꾸나 |
|---|---|---|
| requestedQuantity | 요청한 수량 | 등록 시 고정 |
| allocatedQuantity | 재고를 **예약(할당)한** 수량 | 출고 도메인이 할당·해제·피킹 때 |
| shippedQuantity | 실제로 **피킹·출고된** 누적 수량 | 출고 도메인이 피킹 완료 때 |

## 상태

`REQUESTED`(출고 전) → `PARTIALLY_SHIPPED`(일부 출고) → `COMPLETED`(전량 출고). 발주가 취소·반려되면 REQUESTED 항목이 `CANCELED`가 된다. 할당·피킹 단계는 항목 상태가 아니라 `allocatedQuantity`와 StockAllocation·Outbound 상태로 확인한다.

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| storeOrderLineId | Long | PK |
| storeOrderId | Long | 발주 |
| skuId | Long | SKU |
| requestedQuantity | Long | 요청 수량 |
| allocatedQuantity | Long | 할당 수량. null이면 0 |
| shippedQuantity | Long | 출고 수량. null이면 0 |
| requestedUnitSupplyPrice | BigDecimal | **등록 시점의 SKU 공급 단가 스냅샷** |
| status | StoreOrderLineStatus | 생성 시 null이면 REQUESTED |
| createdAt, updatedAt | LocalDateTime | |

## 메서드

| 메서드 | 설명 | 위반 시 |
|---|---|---|
| `register(storeOrderId, skuId, requestedQuantity, supplyPrice)` | 요청 수량 ≥ 1, 단가 ≥ 0 검증 | `IllegalArgumentException` |
| `cancel()` | 출고 전(REQUESTED) 항목만 CANCELED | `IllegalStateException` |
| `lineAmount()` | 요청 수량 × 공급 단가. **저장하지 않고 계산** | |
| `isFulfilled()` | `shipped >= requested` | |
| `remainingQuantity()` | `max(0, requested - shipped)` (부족분) | |
| `unallocatedQuantity()` | `max(0, requested - allocated - shipped)` (새로 할당 가능한 수량) | |
| `increaseAllocated(q)` | 할당 증가. `unallocatedQuantity`를 넘길 수 없다 | `IllegalArgumentException` |
| `decreaseAllocated(q)` | 할당 감소. 현재 할당을 넘길 수 없다 | `IllegalArgumentException` |
| `applyPicked(allocated, picked)` | 피킹 완료 반영. 할당 수량을 `allocated`만큼 줄이고 출고 수량을 `picked`만큼 늘린다. `0 <= picked <= allocated`, `allocated <= 현재 할당` | `IllegalArgumentException` |
| `refreshStatusByShipped()` | 출고 수량 기준 상태 재계산. 전량이면 COMPLETED, 일부면 PARTIALLY_SHIPPED, 출고가 없으면 유지. **CANCELED·COMPLETED는 건드리지 않는다** | |
| `totalAmount(lines)` (static) | 항목 금액 합계 | |
| `requireDistinctSkus(lines)` (static) | 같은 SKU 중복 시 예외 | `IllegalArgumentException` |

피킹 부족분(`할당 - 피킹`)은 미출고 수량으로 남아 **다시 할당**할 수 있다.
