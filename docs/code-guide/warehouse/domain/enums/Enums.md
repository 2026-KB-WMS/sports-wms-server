# warehouse 도메인 enum

`warehouse/domain/enums/`

## WarehouseStatus

창고 도메인 공통 사용 여부 상태. `Warehouse`, `WarehouseSection`이 함께 쓴다.

| 값 | 의미 |
|---|---|
| ACTIVE | 사용 중 |
| INACTIVE | 비활성 |

| 메서드 | 설명 |
|---|---|
| `fromActiveFlag(Boolean isActive)` | 목록 조회의 `isActive` 필터를 상태로 변환. null이면 null(조건 없음) |

## WarehouseSectionType

구역 유형(`WarehouseSection.sectionType`) 코드. 구역 등록·수정의 `sectionType` 검증과 `GET /warehouses/section-types` 응답에 쓴다.

| 값 | 설명 |
|---|---|
| ZONE | 구역 |
| RACK | 랙 |
| DEFECT | 불량 구역 (확정 코드. ZONE·RACK은 ERD 예시를 그대로 반영) |

| 메서드 | 설명 |
|---|---|
| `getCode()`, `getDescription()` | 코드(enum 이름), 한글 설명 |
| `isValidCode(code)` | 허용 목록에 있는지 |

DB에는 enum이 아니라 **문자열 코드**로 저장하므로, 허용 여부는 서비스가 `isValidCode`로 검사한다.

## WarehouseManagementType

창고 관리자 배정 시 지정하는 담당 역할(`WarehouseMember.memberRole`) 코드. `POST /warehouses/managers`의 `memberRole` 검증과 `GET /warehouses/management-types` 응답에 쓴다.

| 값 | 설명 |
|---|---|
| MANAGER | 창고 관리자 |

ERD상 허용 값이 확정되지 않아 현재는 `MANAGER`만 정의돼 있다. 메서드는 `getCode()`, `getDescription()`, `isValidCode(code)`.
