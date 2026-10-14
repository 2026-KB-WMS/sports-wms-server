# 검색 조건 (SearchCondition)

`inbound/application/port/in/query/` · null인 조건은 무시한다. `InboundSearchCondition`, `PurchaseOrderSearchCondition`은 마지막 필드 `warehouseIds`(담당 창고 범위, null이면 제한 없음)를 가지며, 컨트롤러가 `unscoped(...)`로 만든 뒤 서비스가 범위를 채운다(`inventory`와 같은 방식).

## InboundSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 입고 목록 필터 |
| 엔드포인트 | GET `/api/v1/inbounds` |
| 쓰는 곳 | `InboundService.getInbounds` → `InboundQueryRepository.search` |
| 정렬 | 도착 일시 내림차순, 입고 ID 내림차순 |

| 필드 | 설명 |
|---|---|
| status | 입고 상태 |
| warehouseId | 창고 |
| purchaseOrderId | 발주 |
| keyword | 입고 번호·발주 번호 부분 일치(대소문자 무시) |
| arrivedFrom, arrivedTo | 도착 일시 범위 (이 시각 이후 / 이전) |
| warehouseIds | 담당 창고 범위 |

## PurchaseOrderSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 창고 발주 목록 필터 |
| 엔드포인트 | GET `/api/v1/purchase-orders` |
| 쓰는 곳 | `PurchaseOrderService.getPurchaseOrders` → `PurchaseOrderQueryRepository.search` |
| 정렬 | 등록 일시 내림차순, 발주 ID 내림차순 |

| 필드 | 설명 |
|---|---|
| status | 발주 상태 |
| warehouseId, supplierId | 창고, 공급처 |
| keyword | 발주 번호 부분 일치 |
| createdFrom, createdTo | 등록 일시 범위 |
| warehouseIds | 담당 창고 범위 |

## SupplierSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 공급처 목록 필터 |
| 엔드포인트 | GET `/api/v1/suppliers` |
| 쓰는 곳 | `SupplierService.getSuppliers` → `SupplierRepository.search` |
| 정렬 | 공급처명 오름차순, 공급처 ID |

| 필드 | 설명 |
|---|---|
| keyword | 공급처명·공급처 코드·담당자명 부분 일치 |
| isActive | 거래 상태. **창고 관리자는 서비스가 항상 true로 강제** |

## SectionCandidateCondition

| 항목 | 내용 |
|---|---|
| 용도 | 검수 구역 후보 조회 필터 |
| 엔드포인트 | GET `/api/v1/inbounds/{id}/assignable-sections`, `/defect-sections` |
| 쓰는 곳 | `InboundService.getAssignableSections`, `getDefectSections` |
| 정렬 | 구역 코드 |

| 필드 | 설명 |
|---|---|
| requiredQuantity | 이 수량 이상 가용 용량(`capacity - currentCapacity`)이 있는 구역만 (선택, 0 이상) |
| keyword | 구역명·구역 코드 부분 일치 |
