# StoreErrorCode / StoreConstraintErrorCodes

`store/exception/`

## StoreErrorCode

지점 도메인 전용 오류 코드 enum(`DomainErrorCode`).

| 코드 | HTTP | 기본 메시지 |
|---|---|---|
| STORE_NOT_FOUND | 404 | 지점을 찾을 수 없습니다. |
| DUPLICATE_STORE_CODE | 409 | 이미 사용 중인 지점 코드입니다. |
| STORE_IN_USE | 409 | 진행 중인 발주가 있어 비활성화할 수 없습니다. |
| MEMBER_NOT_FOUND | 404 | 지점 소속 정보를 찾을 수 없습니다. |
| ALREADY_ASSIGNED | 409 | 이미 해당 지점에 배정된 사용자입니다. |

이미 활성·비활성인 지점 등 일부 충돌은 도메인 코드 없이 공통 `CONFLICT`(409)를 쓴다.

## StoreConstraintErrorCodes

DB 유니크 제약 이름 → 오류 코드 대응표(`V1__baseline.sql` 기준).

| 제약 이름 | 오류 코드 |
|---|---|
| uk_store_code | DUPLICATE_STORE_CODE |
| uk_store_member_store_user | ALREADY_ASSIGNED |
