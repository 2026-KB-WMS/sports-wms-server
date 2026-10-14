# 검색 조건 (SearchCondition)

`outbound/application/port/in/query/` · null인 조건은 무시한다. 마지막 필드 `warehouseIds`(담당 창고 범위, null이면 제한 없음)는 컨트롤러가 `unscoped(...)`로 만든 뒤 서비스가 채운다.

**창고 기준**: 출고·할당은 창고를 직접 갖지 않으므로 모든 창고 조건은 **발주에 배정된 창고**(`StoreOrder.warehouseId`)다.

## StockAllocationSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 재고 할당 목록 필터 |
| 엔드포인트 | GET `/api/v1/allocations` |
| 쓰는 곳 | `StockAllocationService.searchAllocations` → `OutboundQueryRepository.searchAllocations` |
| 정렬 | 할당 일시 내림차순, 할당 ID 내림차순 |

| 필드 | 설명 |
|---|---|
| storeOrderId | 발주 |
| warehouseId | 발주에 배정된 창고 |
| skuId | SKU |
| status | ALLOCATED / PICKED / RELEASED |
| keyword | **발주 번호·SKU 코드·로트 번호** 부분 일치 |
| warehouseIds | 담당 창고 범위 |

## OutboundSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 출고 목록 필터 |
| 엔드포인트 | GET `/api/v1/outbounds` |
| 쓰는 곳 | `OutboundService.searchOutbounds` → `OutboundQueryRepository.searchOutbounds` |
| 정렬 | 생성 일시 내림차순, 출고 ID 내림차순 |
| 검증 | `createdFrom`이 `createdTo`보다 늦으면 400 |

| 필드 | 설명 |
|---|---|
| status | 출고 상태 |
| warehouseId | 발주에 배정된 창고 |
| storeId | 지점 |
| storeOrderId | 발주 |
| keyword | **출고 번호·발주 번호** 부분 일치 |
| createdFrom, createdTo | 생성 일시 범위 |
| warehouseIds | 담당 창고 범위 |
