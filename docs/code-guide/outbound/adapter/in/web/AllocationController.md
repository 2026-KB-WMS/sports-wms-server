# AllocationController

`outbound/adapter/in/web/AllocationController.java` · `@RestController` · `/api/v1/allocations` · Swagger 태그 "재고 할당"

처리 사용자는 토큰 주체다. 역할(생성·해제는 창고 관리자, 조회는 본사와 창고 관리자)은 `SecurityConfig`가, **담당 창고 범위(발주에 배정된 창고)**는 서비스가 검사한다. 목록은 페이지네이션 없이 전체를 반환한다.

## 의존

`StockAllocationUseCase`

## 엔드포인트

| 메서드 | 경로 | 상태 | 쿼리·바디 | 응답 | 설명 | 허용 역할 |
|---|---|---|---|---|---|---|
| `allocate` | POST `/` | 201 | `StockAllocateRequest` | `StockAllocateResponse` | FEFO 재고 할당 | WAREHOUSE_MANAGER |
| `getAllocations` | GET `/` | 200 | `storeOrderId`, `warehouseId`, `skuId`, `status`, `keyword`, `page`, `size`, `sort` | `PageResponse<StockAllocationItemResponse>` | 할당 목록(정렬 필드: `allocatedAt`, `status`, `orderNo`, `skuCode`, `allocatedQuantity`) | HQ_ADMIN, WAREHOUSE_MANAGER |
| `getAllocation` | GET `/{allocationId}` | 200 | - | `StockAllocationDetailResponse` | 할당 상세 | HQ_ADMIN, WAREHOUSE_MANAGER |
| `releaseAllocation` | PATCH `/{allocationId}/release` | 200 | `OutboundReasonRequest` | `StockAllocationReleaseResponse` | 할당 해제 | WAREHOUSE_MANAGER |

해제는 경로 ID, 바디의 `reason`, 토큰 사용자를 컨트롤러가 합쳐 `StockAllocationReleaseCommand`를 직접 만든다.
