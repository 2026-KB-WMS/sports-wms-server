# store 도메인 enum

`store/domain/enums/`

## StoreStatus

지점 운영 상태. 지점은 삭제하지 않고 비활성화만 허용한다.

| 값 | 의미 |
|---|---|
| ACTIVE | 운영 중 |
| INACTIVE | 비활성 |

| 메서드 | 설명 |
|---|---|
| `fromActiveFlag(Boolean isActive)` | 목록 조회의 `isActive` 필터를 상태로 변환. null이면 null(조건 없음) |

## StoreManagementType

점주 배정 시 지정하는 지점 내 담당 역할(`StoreMember.memberRole`) 코드. `POST /stores/assign`의 `memberRole` 검증과 `GET /stores/management-types` 응답에 쓴다.

| 값 | 설명 |
|---|---|
| OWNER | 점주 |
| MANAGER | 지점 관리자 |

| 메서드 | 설명 |
|---|---|
| `getCode()`, `getDescription()` | 코드(enum 이름), 한글 설명 |
| `isValidCode(code)` | 허용 목록에 있는지 |

DB에는 문자열로 저장하므로 허용 여부는 서비스가 `isValidCode`로 검사한다.
