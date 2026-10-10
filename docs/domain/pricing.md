# 단가 기준 정리

> 기준: **코드(2026-10-10)**. 단가가 어디에 저장되고, 어느 시점에 어디서 가져오며, 누구에게 보이는지를 한 곳에 모았다. 각 도메인 API 명세와 Flyway SQL을 대조해 정리한 요약이며, 명세와 다르거나 결정이 필요한 부분은 맨 아래 "알려진 공백"에 모았다.

## 한눈에 보기

단가는 7곳에 있다. **매입 쪽 4개**와 **공급 쪽 3개**이며 두 흐름은 서로 연결되지 않는다.

```
매입(창고 ← 공급처)
  SKU 현재 매입 단가 ──스냅샷──▶ 발주 단가 ──(다르면 사유 필수)──▶ 입고 단가 ──▶ 로트 원가
  product_sku               purchase_order_line                  inbound_line     lot

공급(지점 ← 창고)
  SKU 현재 공급 단가 ──스냅샷──▶ 발주 요청 공급 단가 ──(없으면 SKU 현재값)──▶ 출고 확정 공급 단가
  product_sku                     store_order_line                               outbound_line
```

| # | 단가 | 컬럼 | 값이 정해지는 시점과 출처 | 이후 변경 | 없을 때 |
|---|---|---|---|---|---|
| 1 | SKU 현재 매입 단가 | `product_sku.current_purchase_price` | SKU 등록 요청 본문(선택) | 수정 API 없음 | 창고 발주 등록 409 `PURCHASE_PRICE_MISSING` |
| 2 | SKU 현재 공급 단가 | `product_sku.current_supply_price` | SKU 등록 요청 본문(선택) | 수정 API 없음 | 지점 발주 등록 409 `SUPPLY_PRICE_MISSING` |
| 3 | 발주 단가 | `purchase_order_line.ordered_unit_price` | 창고 발주 등록 시 #1 스냅샷. 요청에서 받지 않음 | 없음(스냅샷) | NOT NULL |
| 4 | 입고 단가 | `inbound_line.received_unit_price` | 검수(`PATCH /inbounds/{id}/inspect`) 요청 본문, 필수 | 검수 단계에서만 | NOT NULL |
| 5 | 로트 원가 | `lot.unit_cost` | 로트 find-or-create 때 입고 단가(#4) | 없음. 같은 로트에 다른 단가로 입고하면 409 `LOT_UNIT_COST_MISMATCH` | NOT NULL |
| 6 | 발주 요청 공급 단가 | `store_order_line.requested_unit_supply_price` | 지점 발주 등록 시 #2 스냅샷. 요청에서 받지 않음 | 없음(스냅샷) | DB는 NULL 허용이나 도메인이 단가 없는 항목 생성을 막음 |
| 7 | 출고 확정 공급 단가 | `outbound_line.confirmed_unit_supply_price` | 피킹 완료 시: #6 → 없으면 #2 현재값 → 둘 다 없으면 409 `SUPPLY_PRICE_MISSING` | 없음 | 피킹 완료 전에는 `null` |

공통 형식: `DECIMAL(18,2)`, 0 이상(CHECK), 입력은 소수 2자리까지. 통화 단위 컬럼은 없다.

## 금액 계산

금액은 컬럼으로 저장하지 않고 응답에서 계산한다.

| 금액 | 식 | 위치 |
|---|---|---|
| 창고 발주 항목 `lineAmount` | `expectedQuantity × orderedUnitPrice` | `api/inbound.md` |
| 창고 발주 `totalAmount` | 항목 금액 합 | `api/inbound.md` |
| 입고 항목 `lineAmount` | `receivedQuantity × receivedUnitPrice` (서버 계산) | `api/inbound.md` |
| 지점 발주 항목 `lineAmount` | `requestedQuantity × requestedUnitSupplyPrice` | `api/store-order.md` |
| 지점 발주 `totalAmount` | 항목 금액 합 | `api/store-order.md` |
| 출고 항목 `lineAmount` | `shippedQuantity × confirmedUnitSupplyPrice` (피킹 완료 후, 그 전 `null`) | `api/outbound.md` |

## 단계별 규칙

**→ 창고 발주 등록(매입 단가 스냅샷).** `SkuPurchasePriceAdapter`가 상품 도메인에서 SKU를 읽는다. 비활성 SKU는 409 `CONFLICT`, 매입 단가가 없으면 409 `PURCHASE_PRICE_MISSING`이다. 이후 SKU 단가가 바뀌어도 기존 발주 단가는 그대로다.

**→ 입고 검수(입고 단가 입력).** 입고 단가는 항목마다 필수이고 0 이상, 소수 2자리까지다. 발주 단가와 다르면 `priceChangeReason`(500자 이하)이 필수다. 이 단가가 새 로트의 `unit_cost`가 된다.

**→ 로트 원가 고정.** 로트는 SKU + 공급처 + 로트 번호로 찾고 없으면 만든다(ADR-004). 이미 있는 로트에 다른 단가로 입고하면 409 `LOT_UNIT_COST_MISMATCH`다. 단가가 다른 입고는 로트 번호를 새로 받아야 한다.

**→ 지점 발주 등록(공급 단가 스냅샷).** `SkuSupplyPriceAdapter`가 SKU를 읽는다. 비활성 SKU는 409 `CONFLICT`, 공급 단가가 없으면 409 `SUPPLY_PRICE_MISSING`이다. 승인 단계는 공급 단가를 다시 검사하지 않는다.

**→ 피킹 완료(출고 단가 확정).** `OutboundFulfillmentService.resolvePrices`가 항목마다 발주 항목의 스냅샷 → SKU 현재 공급 단가 순으로 정하고, 둘 다 없으면 피킹 전체를 409 `SUPPLY_PRICE_MISSING`으로 거절한다. 출고 항목 금액은 이때 확정된다.

## 노출 범위

| 단가 | HQ_ADMIN | WAREHOUSE_MANAGER | STORE_OWNER |
|---|---|---|---|
| SKU 매입 단가 (`currentPurchasePrice`) | 보임 | 보임 | **응답에서 생략** (`isPurchaseInfoVisibleTo`) |
| SKU 공급 단가 (`currentSupplyPrice`) | 보임 | 보임 | 보임 |
| 발주 단가·입고 단가·로트 원가 | 보임 | 보임(담당 창고) | 해당 API 자체가 403 (입고·재고 API는 점주 불가) |
| 지점 발주 `requestedUnitSupplyPrice`, `lineAmount` | 보임 | 보임(담당 창고 배정 발주) | 보임(담당 지점) |
| 출고 `confirmedUnitSupplyPrice`, `lineAmount` | 보임 | 보임(담당 창고) | 출고 API 자체가 403 |

점주에게는 매입 쪽 단가 전체와 출고 확정 단가가 노출되지 않는다. 별도의 필드 마스킹은 SKU 응답의 매입 단가에만 있고, 나머지는 API 접근 역할 규칙([`api/authorization.md`](../api/authorization.md))으로 막힌다.

## 알려진 공백

코드 기준 확인한 사실이다. 어느 쪽도 임의로 고치지 않았다.

**1 → SKU 단가를 바꾸는 API가 없다.** `ProductSku`에는 등록(`register`)과 활성/비활성만 있고 단가 수정 경로가 없다(`api/product.md`의 엔드포인트도 같다). 등록 때 단가를 잘못 넣었거나 비워 뒀다면 발주 등록이 `PURCHASE_PRICE_MISSING`·`SUPPLY_PRICE_MISSING`으로 막히고, 고치려면 DB를 직접 수정해야 한다.

**2 → SKU 현재 매입 단가는 입고 단가로 갱신되지 않는다.** 발주 단가와 다른 입고 단가는 로트 원가에만 반영되고 #1은 그대로다. "최근 입고가"를 SKU에 반영하는 로직은 없다.

**3 → 매입과 공급 사이에 연결이 없다.** 로트 원가(#5)는 공급 단가 계산이나 마진 검증에 쓰이지 않는다. 매입 단가보다 낮은 공급 단가도 막지 않는다.

**4 → 출고 확정 단가의 SKU 현재값 대체는 정상 경로에서 쓰이지 않는다.** 지점 발주 등록이 단가 없는 항목을 막으므로 #6은 비어 있지 않다. DB를 직접 수정한 데이터를 위한 안전망이다.

**5 → 지점 발주 항목의 단가 컬럼은 NULL을 허용한다.** 데이터 사전 기준이며 도메인이 막고 있다(V11 마이그레이션 주석). 컬럼을 NOT NULL로 바꿀지는 결정이 필요하다.

## 관련 문서

- 입고 단가·로트 원가: [`api/inbound.md`](../api/inbound.md), [`api/inventory.md`](../api/inventory.md), [ADR-004](../adr/004-lot-find-or-create-on-inspect.md)
- 공급 단가: [`api/store-order.md`](../api/store-order.md), [`api/outbound.md`](../api/outbound.md)
- SKU 단가 필드와 점주 노출 제한: [`api/product.md`](../api/product.md)
