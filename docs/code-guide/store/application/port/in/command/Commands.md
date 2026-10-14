# Command

`store/application/port/in/command/` · 쓰기 유스케이스의 입력 `record`.

흐름: `요청 JSON` → `XxxRequest`(검증) → `toCommand()` → **Command** → `UseCase`. 경로 변수 ID는 UseCase 메서드의 별도 인자로 넘긴다.

## StoreRegisterCommand

| 항목 | 내용 |
|---|---|
| 용도 | 지점을 등록한다. 지점 코드는 받지 않고 주소의 시·도로 서버가 발급한다. 점주 배정은 포함하지 않는다(`POST /stores/assign`으로 별도) |
| 엔드포인트 | POST `/api/v1/stores` |
| 만드는 곳 | `StoreRegisterRequest.toCommand()` |
| 쓰는 곳 | `StoreService.registerStore` |
| 필드 | name, address, contactName(선택), contactNumber, latitude, longitude |
| 생성자 | 좌표 없는 4인자 생성자 |

## StoreUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 지점 정보를 부분 수정한다. 지점 코드와 운영 상태는 포함하지 않는다 |
| 엔드포인트 | PATCH `/api/v1/stores/{storeId}` |
| 만드는 곳 | `StoreUpdateRequest.toCommand()` |
| 쓰는 곳 | `StoreService.updateStore(storeId, command)` |
| 필드 | name, address, contactName, contactNumber, latitude, longitude (모두 선택) |
| 메서드 | `hasNoChanges()` |

## StoreMemberAssignCommand

| 항목 | 내용 |
|---|---|
| 용도 | 사용자를 지점 관리자(점주)로 배정한다 |
| 엔드포인트 | POST `/api/v1/stores/assign` |
| 만드는 곳 | `StoreMemberAssignRequest.toCommand()` |
| 쓰는 곳 | `StoreMemberService.assignManager` |
| 필드 | storeId, userId, memberRole (`StoreManagementType`: OWNER, MANAGER) |

## 참고

지점 비활성화의 `reason`은 Command 없이 `deactivateStore(storeId, reason, userId)`로 값을 직접 넘긴다.
