# Dashboard 도메인 API 명세 (초안)

> **초안.** 2026-10-23 작성, 아직 구현 전이며 기준 문서가 아니다. 공통 규칙은 [conventions.md](conventions.md), 역할 규칙은 [authorization.md](authorization.md) 참고.
> Base path: `/api/v1/dashboard`. 이 문서는 **본사(HQ_ADMIN) 홈**만 다룬다. 창고 관리자 홈은 별도 항목으로 추가한다.
> 읽기 전용 집계 API다. 다른 도메인 테이블을 서비스에서 조합하지 않고 읽기 전용 쿼리 조인(ID 기준)으로 집계한다.

## 엔드포인트

| 메서드 | 경로 | 역할 | 설명 |
|---|---|---|---|
| GET | /dashboard/hq/summary | HQ_ADMIN | 조치 대기 + 총 집계 (홈 진입 시 호출, 가볍게) |
| GET | /dashboard/hq/trends | HQ_ADMIN | 월간 추이 + 출고 상위 SKU (무거움, 별도 호출) |

- 두 API 모두 페이지네이션 없음. 목록처럼 보이는 `items`는 고정 개수이며 `page`·`size`·`sort`는 받지 않는다(보내도 무시).
- 창고 관리자·점주 호출은 403 `FORBIDDEN`.

## 집계 기준 (결정)

### 입고·출고 "진행중 / 완료" 구분

| 구분 | 입고 | 출고 |
|---|---|---|
| 진행중 | `INSPECTING` | `PICKING`, `PICKED`, `SHIPPED` |
| 완료 | `COMPLETED` | `DELIVERED` |
| 집계 제외 | `ARRIVED`(검수 시작 전), `CANCELED` | `READY`(피킹 대기), `CANCELED` |

- 시작 전 건(입고 `ARRIVED`, 출고 `READY`)은 집계에서 뺀다. 처리 대기는 `pending`에서 따로 보여준다.
- **수량 기준**
  - 입고: 입고 항목의 `receivedQuantity` 합.
  - 출고 완료·`PICKED`·`SHIPPED`: `shippedQuantity` 합. `PICKING`은 `shippedQuantity`가 아직 0이므로 `allocatedQuantity` 합.
- **기간 기준**
  - 완료: 입고 `receivedAt`, 출고 `deliveredAt`이 속한 달.
  - 진행중: 월과 무관하게 **현재 시점 전체**(상태 스냅샷). 지난달에 시작해 아직 안 끝난 건도 포함한다.
  - 월 경계는 서버 시간대(KST) 기준이다.

## GET /dashboard/hq/summary

- Query: `expiringWithinDays`(선택, 1~365, 기본 30), `month`(선택, `YYYY-MM`, 기본 현재 달)
- 응답 `data`:

```json
{
  "month": "2026-10",
  "pending": {
    "storeOrderRequestedCount": 4,
    "purchaseOrderRequestedCount": 2,
    "unassignedApprovedStoreOrderCount": 3
  },
  "inventory": {
    "totalQuantity": 184200,
    "quarantinedQuantity": 1200,
    "disposedQuantity": 300,
    "defectiveQuantity": 450,
    "activeSkuCount": 312,
    "lowStockSkuCount": 18,
    "expiringLotCount": 7
  },
  "inbound": {
    "completed": { "count": 21, "quantity": 9800 },
    "inProgress": { "count": 3, "quantity": 1200 },
    "previousMonthCompleted": { "count": 19, "quantity": 9100 }
  },
  "outbound": {
    "completed": { "count": 64, "quantity": 7300 },
    "inProgress": { "count": 9, "quantity": 950 },
    "previousMonthCompleted": { "count": 58, "quantity": 6900 }
  },
  "storeOrders": {
    "totalCount": 72,
    "byStatus": {
      "REQUESTED": 4, "APPROVED": 3, "ASSIGNED": 9, "ON_HOLD": 1,
      "COMPLETED": 51, "CANCELED": 3, "REJECTED": 1
    },
    "completionRate": 0.73
  },
  "warehouses": [
    {
      "warehouseId": 1, "warehouseName": "서울 센터",
      "inventoryQuantity": 98000, "lowStockSkuCount": 10,
      "inbound": { "completedQuantity": 5200, "inProgressQuantity": 600 },
      "outbound": { "completedQuantity": 3900, "inProgressQuantity": 450 }
    }
  ]
}
```

### 필드 정의

- `pending`
  - `storeOrderRequestedCount`: 지점 발주 `REQUESTED` (본사 승인 대기).
  - `purchaseOrderRequestedCount`: 창고 발주(PurchaseOrder) `REQUESTED` (본사 확정 대기).
  - `unassignedApprovedStoreOrderCount`: 지점 발주 `APPROVED` (창고 배정 전).
  - 기간 필터 없이 현재 시점 전체를 센다.
- `inventory`
  - `totalQuantity`: 전 창고 정상 재고 수량 합. `QUARANTINED`·`DISPOSED` 로트와 품질 상태 `DEFECTIVE` 재고는 제외하고 `EXPIRED` 로트는 포함한다.
  - `quarantinedQuantity`·`disposedQuantity`: 격리·폐기 로트의 재고 수량 합. 로트 상태가 우선이라 이 로트의 불량 재고도 여기에 센다.
  - `defectiveQuantity`: 격리·폐기가 아닌 로트의 `DEFECTIVE` 재고 수량 합. 세 값은 서로 겹치지 않는다.
  - `activeSkuCount`: 활성 SKU 수.
  - `lowStockSkuCount`: `GET /inventory/low-stock`과 같은 조건(`availableQuantity < safetyStockQuantity`, 안전재고 0·비활성 SKU 제외)의 SKU 수.
  - `expiringLotCount`: 재고가 남은 `AVAILABLE` 로트 중 `expiryDate <= 오늘 + expiringWithinDays`인 로트 수(당일 포함).
- `inbound`, `outbound`: 위 집계 기준 표를 따른다. `count`는 입고·출고 건수, `quantity`는 수량 합.
  - `previousMonthCompleted`는 전월 대비 증감 표시용이며 완료 기준만 제공한다.
- `storeOrders`
  - 집계 대상: 요청 시각(`createdAt`)이 `month`에 속한 지점 발주.
  - `byStatus`: 모든 `StoreOrderStatus`를 키로 항상 내려준다(0건이어도 0).
  - `completionRate`: `COMPLETED / (totalCount - CANCELED - REJECTED)`, 소수 둘째 자리 반올림. 분모가 0이면 `null`.
- `warehouses[]`: 활성 창고 전체, `warehouseId` 오름차순 고정. `inbound`·`outbound`는 위 기준과 동일하되 수량만 제공한다.
- 수량 필드는 모두 BIGINT 범위 정수다(DECIMAL 아님).

## GET /dashboard/hq/trends

- Query: `months`(선택, 1~12, 기본 6), `warehouseId`(선택, 창고 필터. 없는 창고는 404 `WAREHOUSE_NOT_FOUND`)
- 응답 `data`:

```json
{
  "items": [
    { "month": "2026-05", "inboundCompletedQuantity": 8800, "outboundCompletedQuantity": 6500,
      "inboundInProgressQuantity": null, "outboundInProgressQuantity": null },
    { "month": "2026-10", "inboundCompletedQuantity": 9800, "outboundCompletedQuantity": 7300,
      "inboundInProgressQuantity": 1200, "outboundInProgressQuantity": 950 }
  ],
  "topOutboundSkus": [
    { "skuId": 12, "skuCode": "SKU-0012", "skuName": "축구공 5호", "quantity": 840 }
  ]
}
```

- `items`: 과거 달부터 현재 달 순서로 `months`개를 내려준다. 데이터가 없는 달도 0으로 채운다.
- `*InProgressQuantity`는 **현재 달 항목에만** 값이 있고 지난 달은 `null`이다(진행중은 스냅샷이므로).
- `topOutboundSkus`: 현재 달 출고 **완료** 수량 상위 5개 SKU, 수량 내림차순(같으면 `skuId` 오름차순). 5개 미만이면 있는 만큼만. `warehouseId` 필터를 따른다.

## 에러

| 상태 | 코드 | 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | `month` 형식 오류, `expiringWithinDays`·`months` 범위 오류 |
| 401 | UNAUTHORIZED | 토큰 없음·만료 |
| 403 | FORBIDDEN | HQ_ADMIN이 아님 |
| 404 | WAREHOUSE_NOT_FOUND | `warehouseId`가 없는 창고 (trends) |

## 결정 사항 (2026-10-23)

- `totalQuantity`는 격리(`QUARANTINED`)·폐기(`DISPOSED`) 로트와 불량(`DEFECTIVE`) 재고를 제외하고, 세 가지를 각각 `quarantinedQuantity`·`disposedQuantity`·`defectiveQuantity`로 따로 내려준다. `warehouses[].inventoryQuantity`도 같은 정상 재고 기준이다. 유통기한 경과(`EXPIRED`) 로트는 별도 언급이 없어 포함으로 두었으며, 빼려면 사용자 확인이 필요하다.
- `month`는 과거 달 조회를 허용한다.
- `storeOrders`는 요청 시각(`createdAt`) 기준으로 월을 나눈다. 완료 시각 기준은 미완료 상태를 월에 배치할 수 없어 쓰지 않는다.
- 지연 건은 이번 범위에서 뺐다. 후보였던 기준은 출고 `READY`가 `createdAt` 기준 5일 초과였다.

## 구현 시 참고

- 새 도메인(`dashboard`)이라 `docs/README.md` 표 추가, 이슈·서브이슈 생성, 집계 방식(읽기 전용 쿼리 조인)을 정하는 ADR 추가가 필요하다.
- 집계 쿼리는 대상 테이블이 늘어 느려질 수 있으므로 `summary`와 `trends`를 분리했다. 필요하면 인덱스(`inbound.received_at`, `outbound.delivered_at`, 상태 컬럼)를 V2 이후 마이그레이션으로 추가한다.
