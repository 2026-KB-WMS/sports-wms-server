# 비활성화 가능/불가 조건 정리

> 기준: **코드(2026-10-10)**. 각 도메인 API 명세와 코드를 대조해 정리한 요약이며, 명세와 다른 부분은 맨 아래 "명세와 코드 차이"에 모았다. 재활성화 API와 SKU 사용 중 가드는 [ADR-014](../adr/014-reactivation-and-sku-deactivation-guard.md)로 추가했다.
> "진행 중 업무" 상태 집합은 창고·상품 가드가 같은 집합을 쓴다(아래 공통 정의).

## 한눈에 보기

| 대상 | 방법 | 권한 | 막는 조건 (409) | 재활성화 |
|---|---|---|---|---|
| 창고 | `PATCH /warehouses/{id}/deactivate` | HQ_ADMIN | `WAREHOUSE_IN_USE` | `PATCH /warehouses/{id}/activate` (조건 없음) |
| 구역 | `PATCH /warehouses/sections/{id}/deactivate` | HQ_ADMIN | `SECTION_HAS_INVENTORY`, `SECTION_HAS_CHILDREN` | `PATCH /warehouses/sections/{id}/activate` (창고·상위 구역이 활성일 때만) |
| 지점 | `PATCH /stores/{id}/deactivate` | HQ_ADMIN | `STORE_IN_USE` | `PATCH /stores/{id}/activate` (조건 없음) |
| 공급처 | `PATCH /suppliers/{id}/deactivate` | HQ_ADMIN | `SUPPLIER_IN_USE` | `PATCH /suppliers/{id}/activate` (조건 없음) |
| 상품 | `PATCH /products/{id}` `isActive=false` | HQ_ADMIN | `PRODUCT_IN_USE` | `isActive=true`로 가능 |
| SKU | `PATCH /products/skus/{id}/status` | HQ_ADMIN | `SKU_IN_USE` | 상품이 활성일 때만 |
| 계정 | `PATCH /users/{id}` `status=INACTIVE` | HQ_ADMIN | 본인 계정 불가, 상태 전이 규칙 | `status=ACTIVE`로 가능 |
| 브랜드·카테고리·옵션 값 | **비활성화 API 없음** | - | - | - |

공통: 이미 비활성인 창고·구역·지점·공급처를 다시 비활성화하거나, 이미 활성인 대상을 다시 활성화하면 409 `CONFLICT`(멱등 아님). 상품·SKU만 예외로 이미 같은 상태면 그대로 성공한다. 재활성화 결정 배경은 [ADR-014](../adr/014-reactivation-and-sku-deactivation-guard.md).

## 공통 정의: 진행 중 업무

| 업무 | 진행 중 상태 | 종결(막지 않음) |
|---|---|---|
| 입고 | `ARRIVED`, `INSPECTING` | 그 외 |
| 창고 발주 | `REQUESTED`, `CONFIRMED` | 그 외 |
| 출고 | `READY`, `PICKING`, `PICKED`, `SHIPPED` | 그 외 |
| 지점 발주 | `REQUESTED`, `APPROVED`, `ASSIGNED`, `ON_HOLD` | `COMPLETED`, `CANCELED`, `REJECTED` |
| 재고 | 보유 수량 > 0 **또는** 할당 수량 > 0 인 행 | 둘 다 0 |

## 대상별 조건

### 창고
- **불가**: 아래 중 하나라도 있으면 `WAREHOUSE_IN_USE`.
  - 창고의 구역에 재고(보유·할당 > 0)
  - 이 창고의 진행 중 입고, 창고 발주
  - 이 창고에 배정된 지점 발주에 딸린 진행 중 출고
  - 이 창고에 배정된 진행 중 지점 발주
- **가능**: 위가 모두 없을 때.
- 확인 전에 창고의 구역 행을 `section_id` 오름차순으로 잠가, 입고 적치와 경합하지 않게 한다.
- 비활성화 후: 신규 구역 등록, 입고 적치(`WAREHOUSE_INACTIVE`), 관리자 배정, 신규 발주·지점 발주 배정 차단. 조회·수정은 가능, 기존 이력 보존.
- **재활성화**: 조건 없음(이미 활성이면 409 `CONFLICT`). 구역은 자동으로 활성화되지 않는다.

### 구역
- **불가**: 재고가 있으면 `SECTION_HAS_INVENTORY`, 활성 하위 구역이 있으면 `SECTION_HAS_CHILDREN`.
- **가능**: 재고가 없고 활성 하위 구역이 없을 때. 비활성 하위 구역만 남아 있으면 통과.
- 구역 행을 잠가 입고 적치와 경합하지 않게 한다.
- 비활성화 후: 신규 하위 구역 등록, 입고 적치(`SECTION_INACTIVE`), 입고 검수의 합격·불량 구역 지정 차단. 수용량 변경은 현재 사용량 미만만 막는다(`CAPACITY_BELOW_USAGE`, 비활성 여부와 무관).
- **재활성화 불가**: 소속 창고가 비활성이거나 상위 구역이 비활성이면 409 `CONFLICT`. 이미 활성이어도 409. 구역 행을 잠근다.
- **재활성화 가능**: 창고가 활성이고 상위 구역이 없거나 활성일 때.

### 지점
- **불가**: 진행 중 지점 발주(`REQUESTED`·`APPROVED`·`ASSIGNED`·`ON_HOLD`)가 있으면 `STORE_IN_USE`.
- **가능**: 발주가 없거나 모두 종결(`COMPLETED`·`CANCELED`·`REJECTED`)일 때.
- 사유(선택, 최대 500자)는 상태 이력에 기록한다. 사유가 500자를 넘으면 400.
- 지점 행을 잠그지 않아, 같은 순간의 발주 등록과 경합할 수 있다(그렇게 생긴 발주는 승인할 수 없고 취소·반려만 가능).
- 비활성화 후: 신규 지점 발주 등록, 점주 배정 차단. 조회·수정은 가능.
- **재활성화**: 조건 없음(이미 활성이면 409 `CONFLICT`). 상태 이력에 `INACTIVE`→`ACTIVE`와 처리자를 기록한다(사유 없음).

### 공급처
- **불가**: 진행 중 창고 발주(`REQUESTED`·`CONFIRMED`)가 있으면 `SUPPLIER_IN_USE`.
- **가능**: 위가 없을 때. 입고 진행 여부는 보지 않는다(발주만 확인).
- 비활성화 후: 신규 발주 등록 불가(409 `CONFLICT`), `REQUESTED` 발주 확정 불가(`SUPPLIER_INACTIVE`). 창고 관리자 목록에서 숨고 단건 조회는 404. 수정은 가능, 기존 이력 보존.
- **재활성화**: 조건 없음(이미 활성이면 409 `CONFLICT`). 다시 발주 등록·확정이 가능하다.

### 상품
- **불가**: 상품의 SKU 기준으로 재고, 진행 중 입고·창고 발주·출고·지점 발주 중 하나라도 있으면 `PRODUCT_IN_USE`. 출고는 SKU를 직접 갖지 않아 출고가 속한 지점 발주의 항목으로 판단한다.
- **가능**: 위가 모두 없을 때. 이미 비활성인 상품에 다시 `false`를 보내면 검사 없이 성공.
- 부수 효과: 같은 트랜잭션에서 하위 ACTIVE SKU를 모두 비활성화한다. 상품을 다시 활성화해도 SKU는 자동 복구되지 않는다.
- 비활성화 후: 점주에게는 목록에서 빠지고 단건 404. 신규 SKU 등록은 `PRODUCT_INACTIVE`.
- 검사와 변경 사이에 새 재고·발주가 생기는 경합은 막지 않는다(행을 잠그지 않음).

### SKU
- **비활성화 불가**: 이 SKU에 재고(보유·할당 > 0)나 진행 중 입고·창고 발주·출고·지점 발주가 있으면 `SKU_IN_USE`. 같은 상품의 다른 SKU 사용 여부는 보지 않는다.
- **비활성화 가능**: 위가 없을 때. 이미 비활성인 SKU에 다시 `false`를 보내면 검사 없이 성공.
- **활성화**: 상품이 비활성이면 `PRODUCT_INACTIVE`로 불가.
- 비활성 SKU: 재고 변동(`SKU_NOT_ACTIVE`), 발주·지점 발주 항목 지정 불가, 점주에게 404.
- 비활성 옵션 값은 SKU에 연결할 수 없다(`OPTION_VALUE_INACTIVE`).

### 계정
- 전이 규칙(`PENDING`/`ACTIVE`/`INACTIVE`):

| 현재 → 목표 | 결과 |
|---|---|
| `PENDING` → `INACTIVE` | 가입 반려 |
| `ACTIVE` → `INACTIVE` | 비활성화 |
| `INACTIVE` → `ACTIVE` | 재활성화 (역할이 소속 필수면 소속 배정이 있어야 함, 없으면 `AFFILIATION_REQUIRED`) |
| `PENDING` → `ACTIVE` | 가입 승인 (같은 소속 조건) |
| 같은 상태, `PENDING`으로 되돌리기 | `INVALID_USER_STATUS_TRANSITION` |

- **불가**: 본인 계정의 상태·역할 변경(400, 마지막 관리자 잠김 방지).
- 진행 중 업무나 소속 배정이 있어도 비활성화는 막지 않는다.
- 비활성화 후: 로그인 불가, 비밀번호 변경 불가(`ACCOUNT_INACTIVE`, 403). 만료 전 토큰이 남아 있어도 변경은 막는다.
- 소속 배정(창고·지점 관리자)은 비활성 사용자에게 할 수 없다(409 `CONFLICT`).

### 브랜드·카테고리·옵션 값
- 엔티티에 `deactivate()`가 있지만 이를 호출하는 서비스·API가 없다. 브랜드·카테고리는 **비활성화 API를 두지 않기로 결정했다**(2026-10-10, #216). 이름·설명·정렬 순서는 수정 API로 바꾼다.
- 이미 비활성인 값에 대한 가드만 있다: 상품 등록·수정 시 `BRAND_INACTIVE`/`CATEGORY_INACTIVE`, 하위 카테고리 등록 시 `PARENT_CATEGORY_INACTIVE`, SKU 옵션 연결 시 `OPTION_VALUE_INACTIVE`.

## 명세와 코드 차이 (임의로 고치지 않고 알림)

- **`SECTION_IN_USE`**: 구역 **삭제**(`DELETE /warehouses/sections/{id}`, #214) 전용 에러다(`InventoryLot`·검수 구역 참조). 비활성화는 `SECTION_HAS_INVENTORY`·`SECTION_HAS_CHILDREN`만 검사하는 게 명세와 같다. 삭제는 비활성화와 달리 비활성 하위 구역도 `SECTION_HAS_CHILDREN`으로 막는다.
- 계정은 소속 배정이나 진행 중 업무가 있어도 비활성화되는데, 지점·창고와 달리 막는 조건이 없다. 의도인지 확인이 필요하다.

## 근거 코드

- `WarehouseService.deactivateWarehouse`·`activateWarehouse`, `WarehouseUsageAdapter`
- `WarehouseSectionService.deactivateSection`·`activateSection`
- `StoreService.deactivateStore`·`activateStore`, `StoreOrderPresenceService`
- `SupplierService.deactivateSupplier`·`activateSupplier`, `PurchaseOrderRepository.existsInProgressBySupplierId`
- `ProductService.updateProduct`, `ProductUsageAdapter`(`isSkuInUse` 포함), `ProductSkuService.changeSkuStatus`
- `UserService.updateUser`, `User.changeStatus`
