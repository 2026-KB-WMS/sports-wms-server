# 아웃바운드 포트

`outbound/application/port/out/`

## 영속성

### OutboundRepository

출고 헤더 + 항목. 한 트랜잭션에서 함께 저장한다.

| 메서드 | 설명 |
|---|---|
| `save`, `saveLines`, `findById`, `findLinesByOutboundId` | 기본. `saveLines`는 피킹 확정 반영에도 쓴다 |
| `findByIdForUpdate(id)` | 비관적 락. 같은 출고의 동시 처리 중 하나만 성공 |
| `findByStoreOrderId(id)`, `findByStoreOrderIds(ids)` | 발주의 출고를 **생성 순**(출고 ID 오름차순)으로. **취소된 출고도 포함**. 후자는 목록용 |
| `findByStoreOrderIdAndStatusForUpdate(id, status)` | 해당 상태 출고를 잠그고 조회. 발주 취소 때 READY 출고 일괄 취소 |
| `existsByStoreOrderIdAndStatusIn(id, statuses)` | 해당 상태 출고가 있는지 |
| `existsNotCanceledByStoreOrderId(id)` | 취소되지 않은 출고가 있는지 |
| `existsByOutboundNo`, `countByOutboundNoPrefix(prefix)` | 번호 채번 (`OB-YYYYMMDD-`) |

### StockAllocationRepository

발주는 ID로만 참조하므로 **발주 단위 조회는 발주 항목 ID를 거쳐 읽기 전용**으로 한다(ADR-005, ADR-007).

| 메서드 | 설명 |
|---|---|
| `save`, `saveAll`, `findById` | 기본 |
| `findByIdForUpdate(id)` | 해제처럼 상태를 확인하고 바꾸는 흐름 |
| `findAllByIdForUpdate(ids)` | **할당 ID 오름차순**으로 락. 피킹 완료·출고 항목 처리 |
| `findByStoreOrderId(id)` | 발주의 할당(상태 무관) |
| `findByStoreOrderIdAndStatusForUpdate(id, status)` | 발주의 해당 상태 할당을 잠그고 조회. 발주 취소에 따른 일괄 해제 |
| `findUnlinkedAllocatedByStoreOrderIdForUpdate(id)` | **취소되지 않은 출고에 연결되지 않은** ALLOCATED 할당을 잠그고 조회. 출고 생성 때 묶을 대상 |
| `existsByStoreOrderIdAndStatus(id, status)` | 해당 상태 할당이 있는지 |
| `isLinkedToActiveOutbound(allocationId)` | 취소되지 않은 출고 항목에 연결됐는지 |

### OutboundQueryRepository

조회 전용. 지점·창고·SKU·로트·구역 정보를 ID 읽기 전용 조인으로 가져온다.

| 메서드 | 설명 |
|---|---|
| `searchAllocations(condition)` | 할당 일시 내림차순 |
| `findAllocationSummaries(ids)` | 할당 ID 목록 요약(ID 오름차순). 할당 생성 응답에 사용. 빈 컬렉션이면 빈 목록 |
| `findAllocationView(id)` | 할당 상세 |
| `findActiveOutboundIdByAllocationId(id)` | 할당이 연결된 **취소되지 않은** 출고 ID |
| `searchOutbounds(condition)` | 생성 일시 내림차순 |
| `findOutboundView(id)`, `findOutboundLineViews(id)` | 출고 헤더·항목 (항목 ID 오름차순) |
| `findFefoCandidates(warehouseId, skuIds)` | SKU별 FEFO 후보. 품질 AVAILABLE, 로트 AVAILABLE, 구역 ACTIVE, 가용 > 0. 유통기한 빠른 순(없으면 뒤) → 로트 생성 순 → 재고 행 ID 순. **잠그지 않는 조회**이므로 실제 예약은 재고 유스케이스가 잠그고 다시 검증 |

## 다른 도메인 연결

| 포트 | 메서드 | 연결 대상 | 구현 |
|---|---|---|---|
| `OutboundStockPort` | `allocate(quantities)`, `release(quantities)`, `ship(outboundId, userId, picks)` | 재고 `InventoryStockUseCase` | `OutboundStockAdapter` |
| `SkuSupplyPricePort` | `findCurrentSupplyPrice(skuId)` → `Optional<BigDecimal>` | 상품 `ProductSkuUseCase` | `OutboundSkuSupplyPriceAdapter` |

### OutboundStockPort

재고 수량 변경은 **재고 도메인의 유스케이스로만** 하며 호출한 출고 서비스의 트랜잭션 안에서 처리한다. 재고 행은 재고 서비스가 ID 오름차순으로 락을 건다.

| 메서드 | 설명 |
|---|---|
| `allocate` | 할당 수량 증가(예약). 한 행이라도 가용이 부족하면 전체 실패 |
| `release` | 할당 수량 감소(해제). 보유 수량은 그대로 |
| `ship` | 피킹 완료: 보유를 피킹 수량만큼, 할당을 할당 전체만큼 감소. 피킹 > 0이면 재고 이력(OUTBOUND) |

반환 `StockState`(재고 행 ID, 보유, 할당)는 반영 뒤 수량이고 `availableQuantity() = 보유 - 할당`이다.

## 발주 도메인 연동

발주가 출고·할당을 조회하고 정리하는 기능은 이 포트가 아니라 출고의 `port.in`(`OutboundOrderLinkUseCase`)으로 제공한다. 자세한 내용은 `application/service/OutboundOrderLinkService.md`를 본다.
