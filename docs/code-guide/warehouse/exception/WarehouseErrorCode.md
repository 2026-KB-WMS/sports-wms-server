# WarehouseErrorCode / WarehouseConstraintErrorCodes

`warehouse/exception/`

## WarehouseErrorCode

창고 도메인 전용 오류 코드 enum(`DomainErrorCode`). 각 값은 `(HTTP 분류, 기본 메시지)`를 가진다.

| 분류 | 코드 | HTTP |
|---|---|---|
| 창고 | WAREHOUSE_NOT_FOUND | 404 |
| 창고 | DUPLICATE_WAREHOUSE_CODE | 409 |
| 창고 | WAREHOUSE_INACTIVE (비활성 창고의 구역에 적치) | 409 |
| 창고 | WAREHOUSE_IN_USE (재고·진행 중 업무가 있어 비활성화 불가) | 409 |
| 구역 | SECTION_NOT_FOUND, PARENT_SECTION_NOT_FOUND | 404 |
| 구역 | DUPLICATE_SECTION_CODE | 409 |
| 구역 | SECTION_INACTIVE (비활성 구역에 적치) | 409 |
| 구역 | SECTION_HAS_INVENTORY (재고가 남아 비활성화·삭제 불가) | 409 |
| 구역 | SECTION_HAS_CHILDREN (하위 구역이 있어 비활성화·삭제 불가) | 409 |
| 구역 | SECTION_IN_USE (재고 로트·입고 검수 항목이 참조해 삭제 불가) | 409 |
| 수용량 | SECTION_CAPACITY_EXCEEDED (적치가 수용량 초과) | 409 |
| 수용량 | PARENT_CAPACITY_EXCEEDED (같은 상위 아래 구역 수용량 합이 상위 수용량 초과) | 409 |
| 수용량 | CAPACITY_BELOW_USAGE (수용량이 현재 사용량보다 작음) | 409 |
| 수용량 | CAPACITY_BELOW_CHILDREN (수용량이 활성 하위 구역 합보다 작음) | 409 |
| 소속 | MEMBER_NOT_FOUND | 404 |
| 소속 | ALREADY_ASSIGNED | 409 |

## WarehouseConstraintErrorCodes

DB 유니크 제약 이름 → 오류 코드 대응표(`V1__baseline.sql` 기준). 동시 요청이 사전 중복 검사를 통과해도 같은 오류 코드로 응답하게 한다.

| 제약 이름 | 오류 코드 |
|---|---|
| uk_warehouse_code | DUPLICATE_WAREHOUSE_CODE |
| uk_warehouse_section_code | DUPLICATE_SECTION_CODE |
| uk_warehouse_member_warehouse_user | ALREADY_ASSIGNED |
