# 지점 발주(StoreOrder) 도메인 요약

> **기준은 레포다.** 지점 발주 구현을 시작하면서 2026-10-03 Notion "업무 상태 전이도"의 지점 발주 부분을 이 문서로 옮겼다. 지점 발주 관련 상태 전이·권한·부수 효과는 이 문서를 고치고, Notion 전이도의 지점 발주 부분은 참고용으로만 둔다(출고·재고 할당·반품 부분은 계속 Notion이 기준).
> - API 명세: [`docs/api/store-order.md`](../api/store-order.md) (`/api/v1/orders` 12개)
> - 구현 일정: WMS 개발 일정 DB의 "[구현] 발주 도메인", 추적 이슈 #117 (하위 #118~#124, 선행 #116 StatusHistory)

## 범위

`purchaseorder` 패키지 = StoreOrder, StoreOrderLine (ERD "발주" 섹션). 지점 → 창고 상품 보충 요청이다.
창고 → 공급업체 요청인 PurchaseOrder/PurchaseOrderLine은 `inbound` 패키지이며 이 문서의 범위가 아니다([`inbound.md`](inbound.md)).
재고 할당(StockAllocation)과 출고(Outbound)는 ERD 출고 섹션이라 `outbound` 도메인에서 구현한다. 이 도메인은 연동 포트만 정의한다.

## 권한

- 발주 등록은 STORE_OWNER가 본인이 배정된 지점(`StoreMember`)에 대해서만 한다. 창고 관리자·본사 관리자는 대신 등록할 수 없다.
- 승인·반려·창고 배정·재배정·승인 이후 취소는 본사 관리자(HQ_ADMIN)만 한다.
- 출고 보류·재개·부분 출고 종결은 담당 창고(`WarehouseMember`)의 WAREHOUSE_MANAGER만 한다.
- 승인 전(`REQUESTED`) 취소는 발주를 작성한 STORE_OWNER만 한다. 본사 관리자는 승인 전 발주를 취소하지 않고 반려한다.
- 수량·품목 수정은 지원하지 않는다(취소 후 재등록).

## 발주(StoreOrder) 상태

| 현재 | 행위 | 다음 | 수행자 | 조건 |
|---|---|---|---|---|
| REQUESTED | 승인 | APPROVED | 본사 관리자 | 요청 품목·수량·지점 유효성(지점·SKU·상품 활성) |
| REQUESTED | 취소 | CANCELED | 점주 | 요청 지점의 작성자, 승인 전 |
| REQUESTED | 반려 | REJECTED | 본사 관리자 | 반려 사유 필수 |
| APPROVED | 창고 배정 | ASSIGNED | 본사 관리자 | 활성 창고 확정, `warehouse_id` 반영 |
| ASSIGNED | 창고 재배정 | ASSIGNED | 본사 관리자 | 재고 할당·출고가 없을 때만, 사유 기록, `warehouse_id` 변경 |
| ASSIGNED | 출고 보류 | ON_HOLD | 창고 관리자 | 재고 부족·운영 예외 사유 기록, 피킹 시작 전 |
| ON_HOLD | 출고 재개 | ASSIGNED | 창고 관리자 | 재고 확보·재개 사유 기록 |
| APPROVED, ASSIGNED, ON_HOLD | 승인 후 취소 | CANCELED | 본사 관리자 | 사유 필수, 할당 재고 해제(`RELEASED`), 피킹 시작 전, `READY` 출고는 함께 취소 |
| ASSIGNED | 전량 출고 완료 | COMPLETED | 시스템(배송 완료 시 자동) | 모든 항목 `shipped ≥ requested`이고 진행 중 출고 없음 |
| ASSIGNED | 부분 출고 종결 | COMPLETED | 창고 관리자 | 진행 중 출고 없음, 일부 항목 미충족, 사유 필수. 남은 수량은 자동 재발주하지 않음 |

- `warehouse_id`는 요청 시점엔 NULL이고 `ASSIGNED`가 될 때 채워진다. 승인 시점에는 창고를 정하지 않는다.
- 취소·반려·`COMPLETED`는 종결 상태다. 같은 전이를 다시 요청하면 409 `CONFLICT`이며 부작용을 다시 실행하지 않는다.
- 피킹이 시작된 출고(`PICKING`·`PICKED`·`SHIPPED`·`DELIVERED`)가 있으면 취소할 수 없다(409 `ORDER_IN_PICKING`).
- 배송 실패·수령 거부 처리는 미결이다.

## 발주 항목(StoreOrderLine) 상태

| 상태 | 의미 |
|---|---|
| REQUESTED | 출고된 수량이 없음(등록 직후 기본값) |
| PARTIALLY_SHIPPED | `0 < shipped_quantity < requested_quantity` |
| COMPLETED | `shipped_quantity ≥ requested_quantity` |
| CANCELED | 발주 취소·반려로 종결 |

- `REQUESTED` → `PARTIALLY_SHIPPED` → `COMPLETED`는 출고 배송 완료 처리에서 `shipped_quantity`를 누적하며 갱신한다(출고 도메인 몫. 지점 발주 쪽은 초기값과 가드만 만든다).
- 발주가 취소·반려되면 모든 항목이 `CANCELED`가 된다. 피킹 시작 이후에는 발주를 취소할 수 없으므로 취소되는 항목은 항상 `REQUESTED`에서 전이한다(`PARTIALLY_SHIPPED` → `CANCELED`는 없다).
- 할당·피킹 단계(`ALLOCATED`, `PICKED` 등)는 항목 상태가 아니다. `allocated_quantity`와 `StockAllocation`·`Outbound` 상태로 확인한다(같은 정보를 상태에 중복해 맞추지 않는다).
- 부분 출고 종결(`complete-partial`)은 항목 상태를 바꾸지 않고 부족분은 `remainingQuantity`·`shortageQuantity`로 계산한다. 출고가 0건인 항목은 종결 후에도 `REQUESTED`로 남을 수 있어 항목 상태만 보지 말고 발주 상태·수량을 함께 읽는다.

## 점주용 진행 단계 (`progressStage`)

점주가 "지금 어느 단계인지"를 한 값으로 보도록 응답에 내려주는 읽기 전용 파생값이다. DB에 저장하지 않고 발주 상태 + 가장 최근 출고 상태 + 부족 수량 여부로 계산한다(순수 함수, 도메인에 둔다). 매핑 표는 [`api/store-order.md`](../api/store-order.md) "공통 정의"에 있다.

- 창고 내부 단계(할당·`READY`·`PICKING`·`PICKED`)는 점주에게 `PREPARING`("상품 준비 중")으로 묶는다. 창고 쪽 화면은 `latestOutboundStatus`를 그대로 쓴다.
- 출고 도메인 구현 전에는 최근 출고 상태가 항상 없으므로 `ASSIGNED`는 `PREPARING`으로만 나온다. 출고 연동 후에는 입력 값만 채워지면 되고 계산 코드는 바뀌지 않는다.
- 대안으로 검토한 것: DB 상태를 늘리는 방법(출고 상태와 동기화 부담), 클라이언트에서 계산하는 방법(클라이언트 두 곳과 백엔드가 규칙을 따로 가짐). 이 결정은 구현 PR에서 ADR로 남긴다.

## 재고·부수 효과

1. 등록은 발주와 항목을 한 트랜잭션으로 저장하고, 공급 단가(`requested_unit_supply_price`)는 등록 시점 `ProductSKU.current_supply_price` 스냅샷이다(없으면 409 `SUPPLY_PRICE_MISSING`).
2. 모든 상태 변경은 `StatusHistory`에 기록한다(`entity_type = STORE_ORDER`, 처리자·시각, 사유가 필요한 전이는 사유 포함). 응답의 `statusReason`은 이 이력에서 읽는다. 재배정은 `ASSIGNED` → `ASSIGNED`에 사유와 이전·이후 창고를 남긴다.
3. 승인 이후 취소는 한 트랜잭션으로 `READY` 출고를 `CANCELED`로, `ALLOCATED` 재고 할당을 `RELEASED`로 바꾸고 재고 행과 발주 항목의 `allocated_quantity`를 줄인다(보유 수량은 불변). 함께 처리된 출고·할당 이력에는 "발주 취소로 인한 자동 처리"를 사유로 남긴다.
4. 보류 중에는 새 재고 할당·출고 생성·피킹 시작이 막힌다. 보류는 재고·수량을 바꾸지 않으며 재고 보충은 창고 발주(`POST /purchase-orders`)로 별도 요청한다.
5. 상태 확인과 변경은 한 트랜잭션에서 발주 행을 잠그고(필요하면 재고 행도) 처리해 같은 발주에 대한 동시 승인·반려·취소 중 하나만 성공하게 한다.
6. 알림 전송은 이 도메인의 범위가 아니다.

## 구현 현황 (2026-10-03)

구현 전. 트래킹 이슈 #117, 하위 이슈 #118 도메인 모델 + 마이그레이션, #119 포트 + 어댑터, #120 서비스(등록·조회), #121 서비스(승인·반려·취소), #122 서비스(배정·보류·재개·부분 출고 종결), #123 웹 어댑터, #124 테스트. 선행 #116 StatusHistory 공통 도메인.

### 설계 결정 (구현 시 따를 것)

- 출고·할당 의존 동작(승인 후 취소 시 할당 해제·`READY` 출고 취소, `ORDER_IN_PICKING`·`OUTBOUND_IN_PROGRESS`·`ORDER_IN_FULFILLMENT` 검사, `latestOutboundStatus`·`outbounds` 조회)은 출고 연동 포트만 정의하고 출고 도메인 구현 때 완성한다. 그 전에는 임시 구현이 "출고 없음"을 돌려준다. 보류 항목은 Notion "[보류]" 페이지에 기록한다.
- 인증은 입고와 같은 방식이다. 처리 사용자 `userId`는 `@RequestParam`으로 받고 역할·소속 지점/창고·작성자 검사는 인증 연동 때 처리한다.
- 다른 도메인 데이터(지점명·창고명·SKU 정보)는 조회 쿼리에서 ID 기준 읽기 전용 조인으로 가져온다(ADR-007 방식).
- 지점 도메인의 `STORE_IN_USE`(진행 중 발주가 있으면 비활성화 금지)는 이 도메인 완료 후 연결한다.
- 목록 페이지네이션은 전 도메인 일괄 적용 때까지 보류한다(`data.items`).

## 리뷰 시 자주 놓치는 지점

- 허용되지 않은 전이(예: `COMPLETED` → `CANCELED`, `REQUESTED` → `ASSIGNED` 직행, `ON_HOLD` 재배정)가 서비스에서 막혀 있는가
- 승인·반려·취소가 발주 행 잠금 아래에서 상태 확인과 변경을 같은 트랜잭션으로 처리하는가
- 사유가 필요한 전이(반려, 승인 후 취소, 재배정, 보류, 재개, 부분 종결)에서 사유 필수 검증과 `StatusHistory` 기록
- 권한: 승인·반려·배정은 본사, 보류·재개·부분 종결은 담당 창고, 승인 전 취소는 작성자 점주
- 취소·반려 시 항목이 모두 `CANCELED`로 바뀌는가, `complete-partial`은 항목 상태를 건드리지 않는가
- 공급 단가가 등록 시점 스냅샷인가(이후 SKU 단가 변경과 무관), `lineAmount`·`totalAmount` 계산
- 존재하지 않는 발주는 도메인 전용 404 코드(generic `NOT_FOUND` 아님)
