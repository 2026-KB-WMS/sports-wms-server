# e2e 시나리오

실행 중인 서버에 HTTP로만 접근해 시드 데이터를 만들고 업무 흐름·예외·권한을 검증하는 시나리오다. 실행 스크립트는 [`scripts/e2e/e2e_scenarios.py`](../../scripts/e2e/e2e_scenarios.py)이며, 이 문서는 그 스크립트가 무엇을 검증하는지 설명한다. 기대 응답의 기준은 `docs/api/*.md`와 `docs/domain/*.md`이고, 이 문서와 다르면 API 명세가 기준이다.

- 마지막 실행: 2026-10-24, MySQL 8.4 임시 컨테이너 + `bootRun`, 121개 통과 / 0 실패 (같은 DB에서 세 번 연속 통과). 임시 앱은 `SPRING_DATASOURCE_URL`로 컨테이너를 가리켜 띄운다.
- 검증 항목 번호는 스크립트의 `check`/`expect` 이름과 같다.

## 실행

1. 서버를 최초 본사 관리자 환경변수와 함께 띄운다([ADR-013](../adr/013-initial-hq-admin-from-environment.md)). 값을 비우면 관리자가 만들어지지 않아 시드가 중단된다(종료 코드 2).

   | 환경변수 | 스크립트 기본값 |
   |---|---|
   | `WMS_ADMIN_LOGIN_ID` | `e2e_admin` |
   | `WMS_ADMIN_PASSWORD` | `E2eAdmin!234` |
   | `WMS_ADMIN_NAME`, `WMS_ADMIN_EMAIL`, `WMS_ADMIN_PHONE` | 아무 값 |

2. `python scripts/e2e/e2e_scenarios.py [--base-url http://localhost:8080] [--admin-id ...] [--admin-password ...]`
3. 종료 코드: `0` 전부 통과, `1` 하나 이상 실패, `2` 시드 단계 중단(관리자 로그인 실패 등).

매 실행마다 시각 기반 접미사(`RUN`)를 코드·로그인 ID·이메일에 붙여 새 데이터를 만들므로 같은 DB에 여러 번 돌려도 중복 오류가 나지 않는다. 영문 3자 코드인 브랜드·카테고리 코드는 무작위로 고르고, 이미 쓰는 코드(409)면 다른 코드로 다시 시도한다. 데이터를 지우지 않으므로 깨끗한 상태가 필요하면 DB를 새로 만든다.

## 시드 데이터

시나리오가 전제로 쓰는 데이터다. 의존 순서대로 만든다.

| 순서 | 데이터 | 비고 |
|---|---|---|
| 1 | 사용자 4명 가입: 창고 관리자 `wm1`·`wm2`, 점주 `own1`·`own2` | `POST /auth/signup`, 상태 `PENDING` |
| 2 | 창고 W1(수용량 10000), W2(1000) | 본사 관리자 |
| 3 | W1 구역: A(ZONE 3000), B(ZONE 3000), D(DEFECT 500) / W2 구역: A2(ZONE 500) | 하위 합이 창고 수용량 이하여야 함 |
| 4 | 지점 S1, S2 | |
| 5 | 소속 배정: `wm1`→W1, `wm2`→W2, `own1`→S1, `own2`→S2 | 승인 **전에** 배정해야 한다(소속 없는 `ACTIVE`는 409 `AFFILIATION_REQUIRED`) |
| 6 | 사용자 4명 승인(`ACTIVE`) 후 로그인 | `PATCH /users/{userId}` |
| 7 | 브랜드, 카테고리, 상품, 옵션 그룹·값, SKU1(매입 1000/공급 1500), SKU2(2000/3000), SKU1 옵션 연결 | SKU 단가는 등록 시 필수 |
| 8 | 공급처 2개: 공급처(SKU1·SKU2에 매핑), 공급처X(매핑 없음) | 매핑은 `POST /products/skus/{skuId}/suppliers`. 발주 확정은 라인 SKU에 매핑된 공급처만 지정할 수 있다 |

## 시나리오

각 시나리오는 앞 시나리오가 만든 재고 수량을 이어 쓴다. 순서를 바꾸면 수량 검증이 어긋난다.

### A. 창고 발주 → 입고 → 재고 반영

| 단계 | 호출 | 기대 |
|---|---|---|
| 1 | `wm1`이 `POST /purchase-orders` (SKU1 100, SKU2 20, 공급처 없이 SKU·수량만) | 201, `REQUESTED` |
| 2 | `wm1`이 `PATCH .../confirm` | 403 (본사만 확정) |
| 3 | 본사가 라인 공급처 없이 `PATCH .../confirm` | 400 `LINE_SUPPLIER_REQUIRED` |
| 4 | 본사가 SKU에 매핑되지 않은 공급처X로 `confirm` | 409 `SKU_SUPPLIER_NOT_MAPPED` |
| 5 | 본사가 라인별로 공급처를 지정해 `confirm` | 200, `supplierOrders` 1건(`ISSUED`, 공급처 일치) |
| 6 | `wm1`이 `POST /inbounds` (`supplierOrderId`) | 201 `ARRIVED` |
| 7 | 같은 발주서로 `POST /inbounds` 재호출 | 409 `INBOUND_IN_PROGRESS` |
| 8 | 검수 전 `PATCH .../complete` | 409 |
| 9 | `PATCH .../inspect`: SKU1 로트 100개(합격 98→A, 불량 2→D), SKU2 로트 20개(합격 20→B) | 200 |
| 10 | 합격+불량≠입고 수량으로 `inspect` | 400 |
| 11 | `PATCH .../complete` | 200, 발주 `COMPLETED`, 공급처 발주서 `COMPLETED` |
| 12 | `PATCH .../complete` 재호출 | 409 (재고 이중 반영 없음) |
| 13 | `GET /inventory/by-lot` | SKU1 합격 98 / 불량 2, SKU2 합격 20 |

### B. 지점 발주 → 출고 완료 (정상 흐름)

`own1`이 S1에서 SKU1 30, SKU2 5를 발주한다. 승인·배정은 본사가 한다.

| 단계 | 호출 | 기대 |
|---|---|---|
| 1 | `own1`이 `PATCH /orders/{id}/cancel` (`ASSIGNED` 상태) | 403 (승인 이후 취소는 본사만) |
| 2 | `wm1`이 `POST /allocations` | 201, 할당 2건 |
| 3 | 같은 요청 재호출 | 409 `ALREADY_ALLOCATED` |
| 4 | `POST /outbounds` → `picking/start` | 200 |
| 5 | 본사가 발주 취소 | 409 `ORDER_IN_PICKING` |
| 6 | `picking/complete`(전량) → `ship` → `deliver` | 200, 발주 `COMPLETED` |
| 7 | `GET /inventory/by-lot` | SKU1 보유 68, SKU2 보유 15, 할당 0 |
| 8 | `own1`이 `GET /orders/{id}/details` | `statusHistory[].changedByName`이 모두 채워짐 |
| 9 | `own1`이 `GET /orders/my` | 200 |

### C. 재고 부족·보류·취소

| 단계 | 호출 | 기대 |
|---|---|---|
| 1 | SKU1 100개 발주(가용 68) 승인·배정 후 `POST /allocations` | 201, 68 할당, `shortages`에 요청 100·할당 68·부족 32 |
| 2 | `wm1`이 `hold` → 보류 중 `POST /allocations` → `resume` | 200 / 409 / 200 |
| 3 | 가용 0인 SKU1을 1개 발주 → `POST /allocations` | 409 `INSUFFICIENT_STOCK` |
| 4 | 본사가 취소(출고 전, 부분 할당분 있음) | 200 `CANCELED` |
| 5 | SKU2 2개 발주 → 할당 후 본사 취소 | 200, `releasedAllocationCount` 1 |
| 6 | SKU2 재고 조회 | 할당 0 (보유는 불변) |
| 7 | SKU1·SKU2 발주에 `skuIds`로 없는 SKU / 빈 배열 | 400 / 400 |
| 8 | `skuIds=[SKU2]`로 할당 → 같은 요청 재호출 | 201, 항목 1건 / 409 `ALREADY_ALLOCATED` |
| 9 | 남은 SKU1 할당 후 `POST /outbounds`에 `skuIds=[SKU1]` → `skuIds` 없이 재호출 | 201, SKU1 1건 / 201, SKU2 1건 |
| 10 | 출고 생성에 발주에 없는 SKU | 400 |

### D. 부분 피킹 → 부분 종결

| 단계 | 호출 | 기대 |
|---|---|---|
| 1 | SKU1 60개 발주 → 할당 → 출고 → 피킹 시작 | 200 |
| 2 | `picking/complete`에서 `pickedQuantity` = 할당+1 | 400 |
| 3 | `picking/complete`에서 50 | 200 |
| 4 | `ship` → `deliver` | 발주는 `ASSIGNED` 유지 |
| 5 | `wm1`이 `complete-partial` | 200, `shortageQuantity` 10 |
| 6 | SKU1 재고 조회 | 보유 18 |
| 7 | 반려(본사, 사유 필수), 점주의 승인 전 취소(사유 선택) | 각 200 |

### E. 로트 상태 변경·재고 조정

| 단계 | 호출 | 기대 |
|---|---|---|
| 1 | `wm1`이 `PATCH /lots/{id}/status` | 403 (본사만) |
| 2 | `status=EXPIRED` | 400 |
| 3 | 본사가 `QUARANTINED` | 200 |
| 4 | 격리 로트로 발주 할당 | 409 |
| 5 | 보유 수량이 있는 로트를 `DISPOSED` | 409 `LOT_HAS_STOCK` |
| 6 | `AVAILABLE`로 해제 | 200 |
| 7 | `POST /inventory/adjustments` (SKU2 15→14) | 201 |
| 8 | 같은 `beforeQuantity`로 재호출 | 409 `STALE_QUANTITY` |
| 9 | `GET /inventory/{id}/transactions` | 200 |

### F. 권한·소속 범위

| 호출 | 기대 |
|---|---|
| 토큰 없이 `GET /warehouses` | 401 |
| 점주가 `GET /warehouses`, `GET /inventory` | 403 |
| 창고 관리자가 `POST /orders` | 403 |
| `own2`가 S1로 `POST /orders` | 403 |
| `wm2`가 W1의 출고 상세·입고 조회 | 403 |
| 창고 관리자가 `PATCH /products/brands/{id}` | 403 |
| 점주의 `GET /products/skus` 응답 | `currentPurchasePrice` 필드 없음 |
| 존재하지 않는 `warehouseId`로 `GET /purchase-orders` | 404 `WAREHOUSE_NOT_FOUND` |
| `GET /warehouses/my`, `GET /stores/my` | 200 |

### G. 마스터 데이터 수정·삭제·비활성화 가드

| 호출 | 기대 |
|---|---|
| 브랜드 수정, 카테고리 수정, SKU 공급 단가 수정 | 200 |
| 카테고리 수정에 `categoryCode` 포함 | 400 |
| 빈 구역 삭제 | 200 |
| 재고가 있는 구역 삭제 | 409 `SECTION_HAS_INVENTORY` |
| 재고가 있는 상품 비활성화 | 409 `PRODUCT_IN_USE` |
| 재고가 있는 창고 비활성화 | 409 `WAREHOUSE_IN_USE` |
| 공급처 비활성화 → 창고 관리자 단건 조회 → 재활성화 | 200 / 404 `SUPPLIER_NOT_FOUND` / 200 |
| 이미 쓰는 로그인 ID로 가입 | 409 `DUPLICATE_LOGIN_ID` |
| 하이픈 없는 전화번호로 가입 | 400 `VALIDATION_ERROR` |

### H. 본사 대시보드

같은 DB에 이전 실행 데이터가 남을 수 있어 수치는 보지 않고 응답 구조·권한·오류만 확인한다. 수치는 `DashboardApiIntegrationTest`가 검증한다.

| 호출 | 기대 |
|---|---|
| 본사가 `GET /dashboard/hq/summary` | 200, 최상위 필드(`pending`·`inventory`·`inbound`·`outbound`·`storeOrders`·`warehouses`), 재고 분류 필드 5개, `byStatus` 7개 상태, W1이 `warehouses`에 포함 |
| 본사가 `GET /dashboard/hq/trends?months=3` | 200, `items` 3개, 지난 달 `*InProgressQuantity`는 `null` |
| `warehouseId=W1`로 `trends` | 200 |
| 없는 `warehouseId`로 `trends` | 404 `WAREHOUSE_NOT_FOUND` |
| `expiringWithinDays=366`으로 `summary` | 400 `VALIDATION_ERROR` |
| 창고 관리자·점주가 대시보드 호출 | 403 |

## 범위 밖

- **동시성**: 순차 호출만 한다. 같은 로트·구역에 대한 동시 입고·할당·폐기 경합은 성능 개선 단계에서 부하 테스트로 다룬다(알려진 한계: [`api/inventory.md`](../api/inventory.md) 로트 상태 변경 절, [`domain/outbound.md`](../domain/outbound.md) 결정·미결 2번).
- **미결 정책**: 배송 실패·수령 거부, 세트 상품, 입고 완료 후 취소·정정.
- **미구현 기능**: 관리자 비밀번호 재설정, 리프레시 토큰·로그아웃, 로그인 실패 제한, `GET /users/{userId}`.
- **비활성 상태 전이 일부**: 지점 비활성화(`STORE_IN_USE`), 재활성화(창고·구역·지점), SKU 비활성화(`SKU_IN_USE`), 창고 발주 취소·입고 취소는 이 스크립트가 다루지 않는다. 필요하면 시나리오 G에 추가한다.
- **프론트엔드 연동·응답 시간**: 서버 API 응답의 상태 코드와 주요 필드만 확인한다.
