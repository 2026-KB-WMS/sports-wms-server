# AuthenticatedUser

`common/security/AuthenticatedUser.java` · `record`

현재 요청의 **인증 주체(현재 사용자 컨텍스트)**. 컨트롤러가 `@AuthenticationPrincipal`로 받아 서비스에 넘긴다.

요청마다 DB에서 새로 만들므로 소속·역할 변경이 다음 요청부터 반영된다(ADR-016).

## 필드

| 필드 | 타입 | 설명 |
|---|---|---|
| userId | Long | 사용자 ID |
| role | UserRole | `HQ_ADMIN`, `WAREHOUSE_MANAGER`, `STORE_OWNER` |
| warehouseIds | List&lt;Long&gt; | 배정된 창고 ID (생성 시 불변 복사) |
| storeIds | List&lt;Long&gt; | 배정된 지점 ID (생성 시 불변 복사) |

## 메서드

| 메서드 | 설명 |
|---|---|
| `isHqAdmin()` / `isWarehouseManager()` / `isStoreOwner()` | 역할 확인 |
| `canAccessWarehouse(warehouseId)` | 본사는 모든 창고, 그 외는 배정된 창고만. null이면 본사만 true |
| `canAccessStore(storeId)` | 본사는 모든 지점, 그 외는 배정된 지점만 |
| `requireWarehouseAccess(warehouseId)` | 접근 불가면 `FORBIDDEN`(403) |
| `requireStoreAccess(storeId)` | 접근 불가면 `FORBIDDEN`(403) |
| `warehouseScope(requestedWarehouseId)` | 목록 조회에 적용할 창고 범위를 계산한다. 아래 표 참고 |

## warehouseScope 결과

| 상황 | 반환 |
|---|---|
| 창고 ID를 지정함 | 접근 권한 확인(없으면 403) 후 **null**(추가 제한 없음) |
| 지정 안 함 + 본사 | **null**(전체) |
| 지정 안 함 + 그 외 | 담당 창고 ID 목록(**비어 있을 수 있음**) |

null이면 제한 없음이고, 빈 목록이면 "볼 수 있는 창고가 없음"이다. 둘을 구분해서 쿼리에 넘겨야 한다.

## 사용 규칙

- 소속 범위 검사는 서비스가 **대상 리소스를 읽은 뒤** 호출한다(SecurityConfig는 역할만 본다, ADR-012).
