# auth 도메인 enum

`auth/domain/enums/`

## UserRole

역할에 따라 호출 가능한 API와 접근 가능한 창고·지점 범위가 달라진다.

| 값 | 설명 | 소속 |
|---|---|---|
| HQ_ADMIN | 본사 관리자. 마스터 데이터·계정 관리 권한. **가입 API로 만들 수 없다** | 없음 (전체 접근) |
| WAREHOUSE_MANAGER | 창고 관리자. 배정된 창고의 입고·재고·출고 처리 | 창고(`WarehouseMember`) |
| STORE_OWNER | 점주. 배정된 지점의 발주 처리 | 지점(`StoreMember`) |

| 메서드 | 설명 |
|---|---|
| `isSelfSignupAllowed()` | 가입 신청으로 선택할 수 있는 역할인가 (HQ_ADMIN만 false) |
| `requiresAffiliation()` | 창고·지점 소속이 있어야 업무를 볼 수 있는가 (WAREHOUSE_MANAGER, STORE_OWNER). **승인 시 소속 배정이 필수** |

## UserStatus

| 값 | 의미 |
|---|---|
| PENDING | 가입 승인 대기 (가입 직후 기본값) |
| ACTIVE | 정상 |
| INACTIVE | 비활성 (가입 반려 또는 비활성화) |

전이: `PENDING → ACTIVE`(승인), `PENDING → INACTIVE`(반려), `ACTIVE ↔ INACTIVE`. PENDING으로 되돌리는 전이는 없다. **로그인과 인증 주체 생성은 ACTIVE 계정만** 가능하다.
