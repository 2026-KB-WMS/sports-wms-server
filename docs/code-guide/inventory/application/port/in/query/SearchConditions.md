# 검색 조건 (SearchCondition)

`inventory/application/port/in/query/`

## 공통 구조

모든 조건 `record`는 마지막 필드로 **`warehouseIds`(조회를 허용할 창고 범위)**를 가진다.

| 값 | 의미 |
|---|---|
| null | 제한 없음 (본사, 또는 창고를 직접 지정해 권한을 확인한 경우) |
| 목록 | 이 창고들만 (창고 관리자의 담당 창고). 비어 있을 수 있음 → 아무것도 보이지 않음 |

### 흐름

1. 컨트롤러가 `XxxSearchCondition.unscoped(...)`로 범위 없는 조건을 만든다.
2. 서비스가 `actor.warehouseScope(...)`로 범위를 계산해 `warehouseIds`를 채운 **새 조건**을 만든다.
3. `InventoryQueryPersistenceAdapter`가 JPQL 파라미터 `scoped`(범위 사용 여부), `warehouseIds`로 바꾼다. 빈 목록은 `IN` 절 오류를 피하려고 자리 값 `[-1]`로 치환한다.

`unscoped`는 **서비스 안에서 범위를 채우기 전의 값**이라는 뜻이며, 사용자 요청을 범위 없이 실행한다는 뜻이 아니다.

## InventorySearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | SKU 기준 재고 집계 필터 |
| 엔드포인트 | GET `/api/v1/inventory` |
| 필드 | skuId, warehouseId, keyword(SKU 코드·SKU명 부분 일치), warehouseIds |
| 정렬 | SKU 코드 오름차순 |

## InventoryLotSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 로트·구역 단위 재고 행 필터. 로트 상세의 구역별 분포 조회에도 쓴다 |
| 엔드포인트 | GET `/api/v1/inventory/by-lot`, GET `/api/v1/lots/{lotId}`(내부) |
| 필드 | skuId, warehouseId, sectionId, lotId, expiringBefore(이 날짜 당일 포함 이전 만료), qualityStatus, includeEmpty(보유 0 행 포함, null이면 false), warehouseIds |
| 정렬 | 유통기한 오름차순(없으면 뒤로), 재고 ID |
| 팩토리 | `unscoped(...)`, `ofLot(lotId)`(로트 상세용, `includeEmpty=true`), `ofLot(lotId, warehouseIds)` |

## LowStockSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 안전 재고 미만 SKU 필터 |
| 엔드포인트 | GET `/api/v1/inventory/low-stock` |
| 필드 | warehouseId(생략하면 모든 창고의 가용 재고를 합산해 비교), keyword, warehouseIds |
| 정렬 | 부족 수량 내림차순, SKU 코드 |

## InventoryTransactionSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 재고 변동 이력 필터 |
| 엔드포인트 | GET `/api/v1/inventory/transactions`, GET `/api/v1/inventory/{inventoryId}/transactions` |
| 필드 | inventoryLotId, warehouseId, sectionId, skuId, lotId, transactionType, referenceType, referenceId, createdFrom, createdTo, warehouseIds |
| 정렬 | 생성 일시 내림차순, 이력 ID 내림차순 |
| 검증 | `createdFrom`이 `createdTo`보다 뒤면 400. `referenceId`만 있고 `referenceType`이 없으면 400 |
| 쓰임 | `/{inventoryId}/transactions`는 `inventoryLotId`로 고정하고 나머지 대부분을 null로 둔다 |

## LotSearchCondition

| 항목 | 내용 |
|---|---|
| 용도 | 로트 목록 필터 |
| 엔드포인트 | GET `/api/v1/lots` |
| 필드 | skuId, supplierId, expiringBefore, keyword(로트 번호 부분 일치), warehouseIds |
| 범위 의미 | `warehouseIds`는 "이 창고들에 재고 또는 입고 완료 이력이 있는 로트만" |
| 정렬 | 유통기한 오름차순(없으면 뒤로), 로트 ID |
