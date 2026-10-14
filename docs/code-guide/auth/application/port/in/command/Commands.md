# Command

`auth/application/port/in/command/`

## UserSignupCommand

| 항목 | 내용 |
|---|---|
| 용도 | 가입 신청. 소속은 받지 않고 승인 과정에서 배정한다 |
| 엔드포인트 | POST `/api/v1/auth/signup` |
| 만드는 곳 | `SignupRequest.toCommand()` |
| 쓰는 곳 | `UserService.signUp` |
| 필드 | loginId, password, name, email, phone, role |

## LoginCommand

| 항목 | 내용 |
|---|---|
| 용도 | 로그인 |
| 엔드포인트 | POST `/api/v1/auth/login` |
| 만드는 곳 | `LoginRequest.toCommand()` |
| 쓰는 곳 | `AuthService.login` |
| 필드 | loginId, password |

## ChangePasswordCommand

| 항목 | 내용 |
|---|---|
| 용도 | 본인 비밀번호 변경 |
| 엔드포인트 | PATCH `/api/v1/auth/me/password` |
| 만드는 곳 | `ChangePasswordRequest.toCommand(userId)` (`userId`는 **토큰 주체 본인**) |
| 쓰는 곳 | `UserService.changePassword` |
| 필드 | userId, currentPassword, newPassword |

## UserUpdateCommand

| 항목 | 내용 |
|---|---|
| 용도 | 계정을 부분 수정한다. 가입 승인·반려·비활성화·재활성화도 `status` 변경으로 처리 |
| 엔드포인트 | PATCH `/api/v1/users/{userId}` |
| 만드는 곳 | `UserUpdateRequest.toCommand(userId, actorUserId)` |
| 쓰는 곳 | `UserManagementService.updateUser` → `UserService.updateUser` |
| 필드 | userId, **actorUserId**(요청한 관리자. 본인 계정 보호와 상태 이력 처리자), loginId, password, name, email, phone, role, status, warehouseId, storeId |
| 메서드 | `hasNoChanges()`, `assignsAffiliation()`(`warehouseId` 또는 `storeId`가 있는지) |
| 주의 | `loginId`·`password`는 수정할 수 없지만 **무시하지 않고 거절**하려고 필드로 둔다. `warehouseId`·`storeId`는 승인·재활성화와 함께만, 둘을 함께 보낼 수 없다 |

## InitialHqAdminCommand

| 항목 | 내용 |
|---|---|
| 용도 | 시작 시 설정으로 받은 최초 본사 관리자 정보 |
| 엔드포인트 | 없음 (`InitialHqAdminRunner`가 설정값으로 생성) |
| 쓰는 곳 | `UserService.ensureInitialHqAdmin` |
| 필드 | loginId, password, name, email, phone |
| 메서드 | `isEmpty()`: 다섯 값이 모두 비어 있으면 설정하지 않은 것으로 본다 |
