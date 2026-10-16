# 아웃바운드 포트

`storeorder/application/port/out/`

## 영속성

### StoreOrderRepository

지점 발주 헤더 + 항목. 한 트랜잭션에서 함께 저장한다.

| 메서드 | 설명 |
|---|---|
| `save(order)` | 새 발주 저장 또는 ID가 있으면 변경(상태·창고) 반영 |
| `saveLines(lines)` | 항목 저장. 항목의 `storeOrderId`는 호출 측이 채운다. 항목 상태 변경 반영에도 쓴다 |
| `findById`, `findLinesByStoreOrderId` | 조회 (항목은 ID 오름차순) |
| `findByIdForUpdate(id)` | 비관적 락. 승인·반려·취소·배정 등 상태를 확인하고 바꾸는 흐름. 동시 처리 중 하나만 성공 |
| `findLinesByStoreOrderIdForUpdate(id)` | 항목 락 조회 (ID 오름차순). 취소·반려에서 항목 상태를 바꿀 때 |
| `findLinesByIdsForUpdate(ids)` | 항목을 ID로 락 조회. 출고 연동에서 수량을 바꿀 때. 빈 컬렉션이면 빈 목록 |
| `existsByOrderNo`, `countByOrderNoPrefix(prefix)` | 주문 번호 채번 (`SO-YYYYMMDD-`) |
| `existsByStoreIdAndStatusIn(storeId, statuses)` | 지점의 발주 중 해당 상태가 있는지 (지점 비활성화 가드) |

### StoreOrderQueryRepository

지점(store)·창고(warehouse)·SKU(product_sku)를 ID로 조인해 이름·합계를 DB에서 한 번에 만든다(ADR-007). 창고는 배정 전에는 없으므로 left join이다.

| 메서드 | 설명 |
|---|---|
| `search(condition, page)` | `PageResult` 반환. 정렬은 페이지 조건을 따른다(기본 요청 일시 내림차순) |
| `findView(id)` | 헤더 뷰 |
| `findLineViews(id)` | 항목 뷰 (SKU 코드순) |

## 다른 도메인 연결

| 포트 | 메서드 | 연결 대상 | 구현 |
|---|---|---|---|
| `SkuSupplyPricePort` | `getOrderableSupplyPrice(skuId)` | 상품 `ProductSkuUseCase` | `SkuSupplyPriceAdapter` |
| `SkuAvailabilityPort` | `requireActive(skuId)` | 상품 `ProductSkuUseCase` | `SkuAvailabilityAdapter` |
| `StoreAvailabilityPort` | `requireExists`, `requireActive` | 지점 `StoreUseCase` | `StoreAvailabilityAdapter` |
| `WarehouseAvailabilityPort` | `requireExists`, `requireActive` | 창고 `WarehouseUseCase` | `StoreOrderWarehouseAvailabilityAdapter` |
| `StoreOrderOutboundPort` | 아래 | 출고 `OutboundOrderLinkUseCase` | `adapter/out/outbound/StoreOrderOutboundAdapter` |

### StoreOrderOutboundPort

**지점 발주 쪽이 정의하고, 어댑터가 출고의 `port.in`(`OutboundOrderLinkUseCase`)을 호출해 구현한다.** 출고가 내보낸 상태 이름은 어댑터가 `StoreOrderOutboundStatus`로 바꾼다. 발주와 출고는 서로의 엔티티를 참조하지 않고 ID로만 연결한다.

| 메서드 | 설명 | 용도 |
|---|---|---|
| `findLatestOutboundStatus(id)` | 가장 최근(생성 순 마지막) 출고의 상태. 없으면 비어 있음 | 진행 단계 계산 |
| `findLatestOutboundStatuses(ids)` | 위를 여러 발주에 대해 한 번에(목록용) | 목록 N+1 방지 |
| `findOutbounds(id)` | 발주에 딸린 출고를 생성 순으로. 취소된 출고 포함 | 상세 |
| `existsPickingStarted(id)` | PICKING·PICKED·SHIPPED·DELIVERED 출고가 있는지 | 취소·보류 가드 (`ORDER_IN_PICKING`) |
| `existsInProgressOutbound(id)` | READY·PICKING·PICKED·SHIPPED 출고가 있는지 | 부분 종결 가드 (`OUTBOUND_IN_PROGRESS`) |
| `existsActiveFulfillment(id)` | 해제되지 않은 ALLOCATED 할당이나 취소되지 않은 출고가 있는지 | 재배정 가드 (`ORDER_IN_FULFILLMENT`) |
| `cancelFulfillment(id, changedBy)` | ALLOCATED 할당을 RELEASED로, READY 출고를 CANCELED로 바꾸고 재고 행·발주 항목의 `allocated_quantity`를 줄인다. 호출 서비스의 트랜잭션에 참여. 이력 사유 "발주 취소로 인한 자동 처리" | 승인 이후 취소 |
| `releaseUnlinkedAllocations(id, changedBy)` | 출고에 묶이지 않고 남은 ALLOCATED 할당을 RELEASED로. 출고는 취소하지 않음. 이력 사유 "발주 부분 종결로 인한 자동 해제" | 부분 종결 |
