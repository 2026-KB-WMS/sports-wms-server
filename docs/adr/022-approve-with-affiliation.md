# ADR-022: 가입 승인은 소속 지정과 함께 하고, 승인 전 배정은 허용하지 않는다

- 상태: Accepted
- 날짜: 2026-10-13

## 컨텍스트
창고 관리자·점주는 소속이 있어야 `ACTIVE`가 된다. 지금까지는 승인 대기(PENDING) 사용자에게 `POST /warehouses/managers`, `POST /stores/assign`으로 먼저 배정한 뒤 `PATCH /users/{userId}`로 승인했다. 승인 화면에서 소속을 고르며 승인하는 흐름과 맞지 않고, 배정만 하고 승인하지 않은 사용자가 남을 수 있다.

## 결정
- **승인 요청에 소속 포함**: `PATCH /users/{userId}`에 선택 필드 `warehouseId`(창고 관리자), `storeId`(점주)를 추가한다. 승인·재활성화(`status: ACTIVE`)와 함께 오면 같은 트랜잭션에서 배정하고 승인한다. 배정이 실패하면 승인도 되돌려진다.
- **배정 API는 ACTIVE 사용자만**: `POST /warehouses/managers`, `POST /stores/assign`은 `ACTIVE`가 아닌 사용자(PENDING·INACTIVE)를 409로 거절한다. 승인된 사용자의 추가 배정·회수 용도로만 남는다.
- **검증**: 두 필드를 함께 보내거나, 승인·재활성화가 아닌 요청 또는 이미 ACTIVE인 사용자에게 보내거나, 역할(같은 요청의 `role` 변경 후 기준)과 맞지 않으면 400이다. 이미 소속이 있는 사용자에게 보내면 409 `AFFILIATION_ASSIGNED`이며, 이때는 필드 없이 승인한다.
- **담당 역할**: 승인과 함께 만드는 배정의 `memberRole`은 창고 `MANAGER`, 지점 `OWNER`로 고정한다(각 도메인의 현재 기본 값).
- **구현 위치**: 승인과 배정을 묶는 `UserManagementService`(auth)가 `UserUseCase.updateUser`와 창고·지점 배정 유스케이스를 순서대로 호출한다. 배정 서비스가 사용자 조회에 `UserUseCase`를 쓰므로 `UserService`가 직접 배정 서비스를 부르면 순환 의존이 생긴다.

## 근거
- 승인은 "누구를 어디에 배치하며 승인한다"는 한 번의 결정이다. 배정과 승인을 따로 호출하면 중간 상태(소속은 있는데 PENDING)가 남는다.
- 상태를 `ACTIVE`로 바꾼 뒤 배정하므로 배정 서비스의 "활성 사용자만" 규칙을 예외 없이 유지할 수 있다.
- 한 트랜잭션이므로 존재하지 않거나 비활성인 창고·지점을 지정하면 승인도 일어나지 않는다.

## 검토했던 대안
- 소속이 있는 사용자의 소속 필드를 조용히 무시: 기각. 요청한 소속이 반영되지 않은 채 성공해 혼동된다.
- 소속이 있는 사용자에게 추가 배정: 기각. 승인 화면은 최초 소속 지정용이고, 추가 배정은 배정 API가 맡는다.
- PENDING 배정을 계속 허용하고 승인 요청에도 소속을 받기: 기각. 같은 일을 하는 경로가 둘이 되고 배정만 된 PENDING 사용자가 남는다.
- `UserService`가 배정 서비스를 직접 호출(`@Lazy` 등으로 순환 의존 회피): 기각. 빈 연결을 우회하는 장치가 필요하다.

## 영향
- 코드: `UserUpdateCommand`/`UserUpdateRequest`에 `warehouseId`·`storeId`, `UserManagementUseCase`·`UserManagementService`, `UserService`의 소속 검증, 창고·지점 배정 서비스의 상태 검사.
- 명세: `api/auth.md`, `api/warehouse.md`, `api/store.md`, `domain/auth.md`.
- 프론트: 승인 화면에서 창고/지점을 선택해 승인 요청에 담는다. 승인 전 배정 경로는 사라진다. 소속이 있는 사용자는 필드 없이 승인한다.
