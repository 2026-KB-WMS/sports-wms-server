# 검색 조건 (SearchCondition)

`storeorder/application/port/in/query/`

## StoreOrderSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 지점 발주 목록 필터 |
| 엔드포인트 | GET `/api/v1/orders`(본사 전용), GET `/api/v1/orders/my`(점주·창고 관리자) |
| 만드는 곳 | `StoreOrderController`가 `unscoped(...)`로 생성, `getMyStoreOrders`가 범위를 채워 새 조건을 만든다 |
| 쓰는 곳 | `StoreOrderService.searchStoreOrders` → `StoreOrderQueryRepository.search` |
| 정렬 | 요청 일시 내림차순, 발주 ID 내림차순 |
| 검증 | 시작이 종료보다 늦으면 400. `storeId`·`warehouseId`가 있으면 존재 확인(404) |

| 필드 | 설명 |
|---|---|
| status | 발주 상태 |
| storeId, warehouseId | 지점, 배정 창고 |
| keyword | **주문 번호 또는 지점명** 부분 일치 |
| requestedFrom, requestedTo | 요청 일시 범위 |
| storeIds | 조회를 허용할 지점 범위(점주의 담당 지점). null이면 제한 없음 |
| warehouseIds | 조회를 허용할 창고 범위(창고 관리자의 담당 창고). null이면 제한 없음. **범위를 주면 창고가 배정되지 않은 발주는 제외된다** |

## 범위 변환

지점·창고 두 범위를 각각 `(...Scoped, ...Ids)` 쌍으로 JPQL에 넘긴다. 빈 목록은 `IN`에 넘기지 않으려고 자리 값 `[-1]`로 치환한다(`scopeIds`).

## 사용자별 처리 (`getMyStoreOrders`)

| 사용자 | 범위 |
|---|---|
| 점주 | `storeId`를 지정하면 담당 지점 확인(403), 아니면 `storeIds = 담당 지점` |
| 창고 관리자 | `warehouseId`를 지정하면 담당 창고 확인(403), 아니면 `warehouseIds = 담당 창고` |
| 본사 | **403** (이 조회를 쓰지 않는다) |
