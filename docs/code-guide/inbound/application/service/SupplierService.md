# SupplierService

`inbound/application/service/SupplierService.java` · 구현: `SupplierUseCase` · 클래스 기본 `@Transactional(readOnly = true)`

공급처 등록·조회·수정·활성화·비활성화.

## 의존

`SupplierRepository`, `PurchaseOrderRepository`

## 창고 관리자 가시성

창고 관리자는 발주 등록 시 선택용으로만 공급처를 본다.

| 요청 | 규칙 |
|---|---|
| 목록 | `isActive` 필터와 무관하게 **활성 공급처만** |
| 단건 | 비활성이면 **404** (없는 것으로 보인다) |

## 메서드

| 메서드 | 트랜잭션 | 설명 |
|---|---|---|
| `registerSupplier(command)` | 쓰기 | 코드 중복 → `DUPLICATE_SUPPLIER_CODE`. 생성 후 좌표 반영 |
| `getSuppliers(condition, actor)` | 읽기 | 창고 관리자면 `isActive=true`로 고정 |
| `getSupplier(id, actor)` | 읽기 | 창고 관리자에게 비활성이면 `SUPPLIER_NOT_FOUND` |
| `updateSupplier(id, command)` | 쓰기 | 아래 규칙 |
| `deactivateSupplier(id)` | 쓰기 | 이미 비활성 409, **진행 중인 발주(REQUESTED·CONFIRMED)가 있으면 `SUPPLIER_IN_USE`** |
| `activateSupplier(id)` | 쓰기 | 이미 활성이면 409 |

## updateSupplier 규칙

변경 필드가 없으면 `VALIDATION_ERROR`.

| 필드 | 규칙 |
|---|---|
| name, managerName, contactNumber | null이면 "변경 없음" (비울 수 없는 필수 항목) |
| email, address | 값이 있으면 변경. 값이 null이고 `clearEmail`/`clearAddress`가 true면 **비운다** |
| latitude, longitude | **둘 다** 있을 때만 변경 |

email·address는 "키 없음(변경 없음)"과 "키는 있는데 null(비움)"을 구분해야 해서 Command에 `clear` 플래그가 있다.
